/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.SandboxOptions
 */
package viewpoint.far;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import viewpoint.core.Frame;
import viewpoint.far.BlockLight;
import viewpoint.far.FarWorkers;
import viewpoint.far.FarWorld;
import viewpoint.far.Lotpacks;
import viewpoint.far.ShellMesher;
import viewpoint.light.CellLightPage;
import viewpoint.light.CellLights;
import viewpoint.platform.LongMap;
import viewpoint.render.CellLightPages;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.iso.IsoWorld;

public final class CellLight {
    private static final float REACH = 1024.0f;
    private static final float HOLD = 128.0f;
    private static final long DECIDE_NANOS = 1000000000L;
    private static final int JOBS = 2;
    private static final long BUDGET_NANOS = 500000L;
    private static final LongMap<Cell> cells = new LongMap(128);
    private static final ArrayList<Cell> wanted = new ArrayList();
    private static int running;
    private static long frames;
    private static long decided;
    private static boolean broken;
    private static boolean grid;
    private static boolean lampGrid;
    private static boolean night;
    private static final ConcurrentLinkedQueue<Done> DONE;

    public static void update(Frame frame) {
        if (broken) {
            return;
        }
        try {
            CellLight.updateCells(frame);
        }
        catch (RuntimeException runtimeException) {
            broken = true;
            System.out.println("[Viewpoint] far world's light pages off after an error: " + String.valueOf(runtimeException));
            runtimeException.printStackTrace(System.out);
            CellLight.clear();
        }
    }

    private static void updateCells(Frame frame) {
        if (FarWorld.reach() <= 0.0f) {
            if (!cells.isEmpty()) {
                CellLight.clear();
            }
            return;
        }
        ++frames;
        CellLight.takeIn();
        CellLight.visit(frame.camX, frame.camY);
        long l = System.nanoTime();
        long l2 = l + 500000L;
        if (CellLight.turned() || l - decided > 1000000000L) {
            decided = l;
            CellLight.decide();
        }
        for (Cell cell : wanted) {
            if (running >= 2) break;
            if (cell.lights == null && !cell.scanning) {
                CellLight.scan(cell, l2);
                continue;
            }
            if (cell.next == null || cell.making != null) continue;
            CellLight.page(cell);
        }
        CellLight.letGo();
    }

    private static boolean turned() {
        boolean bl = IsoWorld.instance.isHydroPowerOn();
        boolean bl2 = SandboxOptions.instance.doesPowerGridExist();
        boolean bl3 = GameTime.getInstance().getNight() >= 0.5f;
        boolean bl4 = bl != grid || bl2 != lampGrid || bl3 != night;
        grid = bl;
        lampGrid = bl2;
        night = bl3;
        return bl4;
    }

    private static void decide() {
        CellLights.Cell[] cellArray = new CellLights.Cell[9];
        for (Cell cell : wanted) {
            int n;
            if (cell.lights == null) continue;
            for (int i = -1; i <= 1; ++i) {
                for (n = -1; n <= 1; ++n) {
                    Cell cell2 = cells.get(FarWorld.key(cell.cellX + n, cell.cellY + i));
                    cellArray[(i + 1) * 3 + n + 1] = cell2 == null ? null : cell2.lights;
                }
            }
            CellLights.Inputs inputs = CellLights.decide(cell.lights, cellArray);
            n = (cell.making != null ? inputs.same(cell.making) : inputs.same(cell.made)) ? 1 : 0;
            cell.next = n != 0 ? null : inputs;
        }
    }

    private static void visit(float f, float f2) {
        int n = 256;
        int n2 = Math.floorDiv((int)Math.floor(f), n);
        int n3 = Math.floorDiv((int)Math.floor(f2), n);
        int n4 = (int)Math.ceil(1152.0f / (float)n);
        wanted.clear();
        for (int i = n3 - n4; i <= n3 + n4; ++i) {
            for (int j = n2 - n4; j <= n2 + n4; ++j) {
                float f3 = Math.max(0.0f, Math.max((float)(j * n) - f, f - (float)((j + 1) * n)));
                float f4 = Math.max(0.0f, Math.max((float)(i * n) - f2, f2 - (float)((i + 1) * n)));
                float f5 = (float)Math.sqrt(f3 * f3 + f4 * f4);
                Cell cell3 = cells.get(FarWorld.key(j, i));
                if (cell3 == null && f5 > 1024.0f || cell3 != null && f5 > 1152.0f) continue;
                if (cell3 == null) {
                    cell3 = new Cell(j, i);
                    cells.put(FarWorld.key(j, i), cell3);
                }
                cell3.seen = frames;
                cell3.distance = f5;
                wanted.add(cell3);
            }
        }
        wanted.sort((cell, cell2) -> Float.compare(cell.distance, cell2.distance));
    }

    private static void scan(Cell cell, long l) {
        FarWorld.Slot slot = FarWorld.slots.get(FarWorld.key(cell.cellX, cell.cellY));
        if (slot == null || slot.legend == null || slot.lotpack == null) {
            return;
        }
        long[] lArray = BlockLight.table(slot.legend, l);
        if (lArray == null) {
            return;
        }
        String string = slot.lotpack;
        byte[] byArray = Lotpacks.get(string);
        int n = slot.minLevel;
        int n2 = slot.maxLevel;
        int n3 = cell.cellX;
        int n4 = cell.cellY;
        cell.scanning = true;
        ++running;
        FarWorkers.jobs.incrementAndGet();
        FarWorkers.POOL.execute(() -> {
            int[] nArray = new int[]{};
            try {
                int[][] nArrayArray = new int[][]{new int[64]};
                int[] nArray2 = new int[]{0};
                int n9 = n3 * 256;
                int n10 = n4 * 256;
                ShellMesher.read(Lotpacks.read(string, byArray), n, n2, 0, 0, 32, (n3, n4, n5, n6) -> {
                    int n7;
                    int n8 = n7 = n3 < lArray.length ? (int)(lArray[n3] >>> 32) : 0;
                    if (n7 != 0) {
                        if (nArray2[0] + 4 > nArrayArray[0].length) {
                            nArray2[0] = Arrays.copyOf(nArrayArray[0], nArrayArray[0].length * 2);
                        }
                        int n9 = nArray2[0];
                        nArray2[0][n9] = n9 + n4;
                        nArray2[0][n9 + 1] = n10 + n5;
                        nArray2[0][n9 + 2] = n6;
                        nArray2[0][n9 + 3] = n7;
                        nArray[0] = nArray2[0] + 4;
                    }
                });
                nArray = Arrays.copyOf(nArrayArray[0], nArray2[0]);
            }
            catch (Throwable throwable) {
                System.out.println("[Viewpoint] far world: the lamps of cell " + n3 + "," + n4 + " failed: " + String.valueOf(throwable));
            }
            finally {
                DONE.add(new Done(cell, nArray, null, null, null));
                FarWorkers.jobs.decrementAndGet();
            }
        });
    }

    private static void page(Cell cell) {
        CellLights.Inputs inputs = cell.next;
        int n = cell.cellX;
        int n2 = cell.cellY;
        cell.next = null;
        cell.making = inputs;
        ++running;
        FarWorkers.jobs.incrementAndGet();
        FarWorkers.POOL.execute(() -> {
            byte[] byArray = null;
            int[] nArray = null;
            try {
                byArray = CellLightPage.build(n, n2, inputs.lamps(), inputs.rooms());
                nArray = CellLightPage.cards(n, n2, inputs.lamps());
                DONE.add(new Done(cell, null, inputs, byArray, nArray));
            }
            catch (Throwable throwable) {
                try {
                    System.out.println("[Viewpoint] far world: the light page of cell " + n + "," + n2 + " failed: " + String.valueOf(throwable));
                    DONE.add(new Done(cell, null, inputs, byArray, nArray));
                }
                catch (Throwable throwable2) {
                    DONE.add(new Done(cell, null, inputs, byArray, nArray));
                    FarWorkers.jobs.decrementAndGet();
                    throw throwable2;
                }
                FarWorkers.jobs.decrementAndGet();
            }
            FarWorkers.jobs.decrementAndGet();
        });
    }

    private static void takeIn() {
        Done done;
        while ((done = DONE.poll()) != null) {
            --running;
            Cell cell = done.cell;
            if (cells.get(FarWorld.key(cell.cellX, cell.cellY)) != cell) continue;
            if (done.lamps != null) {
                cell.scanning = false;
                cell.lights = CellLights.cell(cell.cellX, cell.cellY, done.lamps);
                decided = 0L;
                continue;
            }
            cell.making = null;
            cell.made = done.inputs;
            if (done.page == null) continue;
            CellLightPages.show(cell.cellX, cell.cellY, done.page, done.cards);
        }
    }

    private static void letGo() {
        Iterator<Cell> iterator = cells.values().iterator();
        while (iterator.hasNext()) {
            Cell cell = iterator.next();
            if (cell.seen == frames) continue;
            CellLightPages.hide(cell.cellX, cell.cellY);
            iterator.remove();
        }
    }

    public static void clear() {
        cells.clear();
        CellLightPages.clear();
    }

    private CellLight() {
    }

    static {
        DONE = new ConcurrentLinkedQueue();
    }

    private static final class Cell {
        final int cellX;
        final int cellY;
        CellLights.Cell lights;
        CellLights.Inputs made;
        CellLights.Inputs making;
        CellLights.Inputs next;
        boolean scanning;
        long seen;
        float distance;

        Cell(int n, int n2) {
            this.cellX = n;
            this.cellY = n2;
        }
    }

    private record Done(Cell cell, int[] lamps, CellLights.Inputs inputs, byte[] page, int[] cards) {
    }
}

