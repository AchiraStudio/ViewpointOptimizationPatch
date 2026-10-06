/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoLot
 *  zombie.iso.MapFiles
 */
package viewpoint.far;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import viewpoint.core.Frame;
import viewpoint.far.BiomeTrees;
import viewpoint.far.BlockRooms;
import viewpoint.far.FarWorkers;
import viewpoint.far.FarWorld;
import viewpoint.far.Lotpacks;
import viewpoint.far.ShellMesher;
import viewpoint.light.MapLayout;
import viewpoint.light.MapLight;
import viewpoint.light.MapSquares;
import viewpoint.light.TileLight;
import viewpoint.platform.Caches;
import viewpoint.platform.LongMap;
import zombie.iso.IsoLot;
import zombie.iso.IsoWorld;
import zombie.iso.MapFiles;
import zombie.iso.RoomDef;

public final class BlockLight
implements Caches.Cache {
    private static final float REACH = 293.0f;
    private static final float SQUARES_REACH = 389.0f;
    private static final float HOLD = 32.0f;
    private static final int JOBS = 2;
    private static final long BUDGET_NANOS = 1000000L;
    private static final LongMap<Block> blocks = new LongMap(256);
    private static final ArrayList<Block> wanted = new ArrayList();
    private static final IdentityHashMap<BiomeTrees.Legend, long[]> tables = new IdentityHashMap();
    private static final IdentityHashMap<BiomeTrees.Legend, Integer> filled = new IdentityHashMap();
    private static final LongMap<Boolean> mapLacks = new LongMap(64);
    private static int running;
    private static int reading;
    private static int generation;
    private static long frames;
    private static boolean broken;
    private static final ConcurrentLinkedQueue<Done> DONE;

    public static void update(Frame frame) {
        if (broken) {
            return;
        }
        try {
            BlockLight.updateBlocks(frame);
        }
        catch (RuntimeException runtimeException) {
            broken = true;
            System.out.println("[Viewpoint] far world's light off after an error: " + String.valueOf(runtimeException));
            runtimeException.printStackTrace(System.out);
            BlockLight.clear();
        }
    }

    private static void updateBlocks(Frame frame) {
        if (FarWorld.reach() <= 0.0f) {
            if (!blocks.isEmpty()) {
                BlockLight.clear();
            }
            return;
        }
        ++frames;
        if (MapLight.generation() != generation) {
            generation = MapLight.generation();
            for (Block block : blocks.values()) {
                block.installed = false;
            }
        }
        BlockLight.takeIn();
        long l = System.nanoTime() + 1000000L;
        BlockLight.visit(frame.camX, frame.camY);
        BlockLight.startJobs(l);
        BlockLight.install(l);
        BlockLight.letGo();
        MapLight.update(IsoWorld.instance.getCell());
    }

    private static void visit(float f, float f2) {
        int n = 64;
        int n2 = Math.floorDiv((int)Math.floor(f), n);
        int n3 = Math.floorDiv((int)Math.floor(f2), n);
        int n4 = (int)Math.ceil(421.0f / (float)n);
        wanted.clear();
        for (int i = n3 - n4; i <= n3 + n4; ++i) {
            for (int j = n2 - n4; j <= n2 + n4; ++j) {
                float f3 = (float)(j * n) - f;
                float f4 = f3 + (float)n;
                float f5 = (float)(i * n) - f2;
                float f6 = f5 + (float)n;
                float f7 = Math.max(0.0f, Math.max(f3, -f4));
                float f8 = Math.max(0.0f, Math.max(f5, -f6));
                float f9 = (float)Math.sqrt(f7 * f7 + f8 * f8);
                Block block3 = blocks.get(FarWorld.key(j, i));
                if (block3 == null && f9 > 389.0f || block3 != null && f9 > 421.0f) continue;
                if (block3 == null) {
                    block3 = new Block(j, i);
                    blocks.put(FarWorld.key(j, i), block3);
                }
                block3.seen = frames;
                block3.distance = f9;
                wanted.add(block3);
            }
        }
        wanted.sort((block, block2) -> Float.compare(block.distance, block2.distance));
    }

    private static void startJobs(long l) {
        for (Block block : wanted) {
            if (running >= 2 || System.nanoTime() > l) {
                return;
            }
            if (block.squares == null && !block.squaresBusy) {
                BlockLight.squares(block, l);
                continue;
            }
            if (block.layout != null || block.layoutBusy || block.squares == null || !(block.distance <= 293.0f) || BlockLight.neighbours(block) == null) continue;
            BlockLight.layout(block);
        }
    }

    private static void squares(Block block, long l) {
        int n;
        int n2 = 4;
        int n3 = Math.floorDiv(block.bx, n2);
        FarWorld.Slot slot = FarWorld.slots.get(FarWorld.key(n3, n = Math.floorDiv(block.by, n2)));
        if (slot == null || slot.legend == null || slot.lotpack == null) {
            return;
        }
        long[] lArray = BlockLight.table(slot.legend, l);
        if (lArray == null) {
            return;
        }
        String string = slot.lotpack;
        byte[] byArray = Lotpacks.get(string);
        if (byArray == null && reading > 0) {
            return;
        }
        boolean bl = byArray == null;
        reading += bl ? 1 : 0;
        int n4 = slot.minLevel;
        int n5 = slot.maxLevel;
        int n6 = block.bx;
        int n7 = block.by;
        int n8 = Math.max(0, Math.min(n5, 31)) + 1;
        block.squaresBusy = true;
        ++running;
        FarWorkers.jobs.incrementAndGet();
        FarWorkers.POOL.execute(() -> {
            MapSquares mapSquares = null;
            try {
                MapSquares mapSquares2 = new MapSquares(n6, n7, n8);
                int n13 = n3 * 256;
                int n14 = n * 256;
                int n15 = 8;
                ShellMesher.read(Lotpacks.read(string, byArray), n4, n5, (n6 - n3 * n2) * n15, (n7 - n * n2) * n15, n15, (n3, n4, n5, n6) -> mapSquares2.tile(n13 + n4, n14 + n5, n6, n3 < lArray.length ? lArray[n3] : 0L));
                mapSquares2.finish();
                mapSquares = mapSquares2;
                DONE.add(new Done(block, false, bl, mapSquares, null));
            }
            catch (Throwable throwable) {
                try {
                    System.out.println("[Viewpoint] far world: the light of block " + n6 + "," + n7 + " failed: " + String.valueOf(throwable));
                    mapSquares = new MapSquares(n6, n7, 1);
                    mapSquares.finish();
                    DONE.add(new Done(block, false, bl, mapSquares, null));
                }
                catch (Throwable throwable2) {
                    DONE.add(new Done(block, false, bl, mapSquares, null));
                    FarWorkers.jobs.decrementAndGet();
                    throw throwable2;
                }
                FarWorkers.jobs.decrementAndGet();
            }
            FarWorkers.jobs.decrementAndGet();
        });
    }

    private static void layout(Block block) {
        MapSquares[] mapSquaresArray = BlockLight.neighbours(block);
        BlockRooms blockRooms = BlockRooms.of(block.bx, block.by, block.squares.levels());
        long[][] lArray = blockRooms.ids;
        block.lit = blockRooms.lit;
        block.layoutBusy = true;
        ++running;
        FarWorkers.jobs.incrementAndGet();
        FarWorkers.POOL.execute(() -> {
            MapLayout.Block block2 = null;
            try {
                block2 = MapLayout.build(mapSquaresArray, lArray);
            }
            catch (Throwable throwable) {
                System.out.println("[Viewpoint] far world: the layouts of block " + block.bx + "," + block.by + " failed: " + String.valueOf(throwable));
            }
            finally {
                DONE.add(new Done(block, true, false, null, block2));
                FarWorkers.jobs.decrementAndGet();
            }
        });
    }

    private static MapSquares[] neighbours(Block block) {
        MapSquares[] mapSquaresArray = new MapSquares[9];
        int n = 4;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                Block block2 = blocks.get(FarWorld.key(block.bx + j, block.by + i));
                if (block2 != null && block2.squares != null) {
                    mapSquaresArray[(i + 1) * 3 + j + 1] = block2.squares;
                    continue;
                }
                if (BlockLight.mapLacks(Math.floorDiv(block.bx + j, n), Math.floorDiv(block.by + i, n))) continue;
                return null;
            }
        }
        return mapSquaresArray;
    }

    private static boolean mapLacks(int n, int n2) {
        long l = FarWorld.key(n, n2);
        Boolean bl = mapLacks.get(l);
        if (bl == null) {
            String string = "world_" + n + "_" + n2 + ".lotpack";
            bl = Boolean.TRUE;
            for (MapFiles mapFiles : IsoLot.MapFiles) {
                bl = bl != false && !mapFiles.infoFileNames.containsKey(string);
            }
            mapLacks.put(l, bl);
        }
        return bl;
    }

    private static void takeIn() {
        Done done;
        while ((done = DONE.poll()) != null) {
            --running;
            reading -= done.read ? 1 : 0;
            Block block = done.block;
            if (blocks.get(FarWorld.key(block.bx, block.by)) != block) continue;
            if (done.layoutJob) {
                block.layoutBusy = done.layout == null;
                block.layout = done.layout;
                continue;
            }
            block.squaresBusy = false;
            block.squares = done.squares;
        }
    }

    private static void install(long l) {
        for (Block block : wanted) {
            if (System.nanoTime() > l) {
                return;
            }
            if (block.layout == null || block.installed) continue;
            MapLight.install(block.layout, block.lit);
            block.installed = true;
        }
    }

    private static void letGo() {
        Iterator<Block> iterator = blocks.values().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (block.seen == frames) continue;
            if (block.installed) {
                MapLight.drop(block.bx, block.by);
            }
            iterator.remove();
        }
    }

    static long[] table(BiomeTrees.Legend legend, long l) {
        int n;
        long[] lArray = tables.get(legend);
        Integer n2 = filled.get(legend);
        if (lArray != null && n2 == null) {
            return lArray;
        }
        String[] stringArray = legend.names();
        if (lArray == null) {
            lArray = new long[stringArray.length];
            tables.put(legend, lArray);
        }
        int n3 = n = n2 == null ? 0 : n2;
        while (n < lArray.length) {
            lArray[n] = TileLight.of(stringArray[n]);
            if ((++n & 0x1F) != 0 || n >= lArray.length || System.nanoTime() <= l) continue;
            filled.put(legend, n);
            return null;
        }
        filled.remove(legend);
        return lArray;
    }

    public static void clear() {
        MapLight.off();
        for (Block block : blocks.values()) {
            if (!block.installed) continue;
            MapLight.drop(block.bx, block.by);
        }
        blocks.clear();
        tables.clear();
        filled.clear();
        mapLacks.clear();
        TileLight.forget();
    }

    @Override
    public long bytes() {
        long l = 0L;
        for (Block block : blocks.values()) {
            l += block.squares == null ? 0L : block.squares.bytes();
        }
        return l;
    }

    @Override
    public void list(Caches.Survey survey) {
    }

    @Override
    public void evictBefore(long l) {
    }

    private BlockLight() {
    }

    static {
        generation = -1;
        Caches.register(new BlockLight());
        DONE = new ConcurrentLinkedQueue();
    }

    private static final class Block {
        final int bx;
        final int by;
        MapSquares squares;
        MapLayout.Block layout;
        ArrayList<RoomDef> lit;
        boolean squaresBusy;
        boolean layoutBusy;
        boolean installed;
        long seen;
        float distance;

        Block(int n, int n2) {
            this.bx = n;
            this.by = n2;
        }
    }

    private record Done(Block block, boolean layoutJob, boolean read, MapSquares squares, MapLayout.Block layout) {
    }
}

