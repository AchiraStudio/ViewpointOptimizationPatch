/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import viewpoint.core.Frame;
import viewpoint.far.BiomeTrees;
import viewpoint.far.CellRooms;
import viewpoint.far.Erosion;
import viewpoint.far.FarTiles;
import viewpoint.far.FarTreeCheck;
import viewpoint.far.FarTrees;
import viewpoint.far.FarWorkers;
import viewpoint.far.FarWorld;
import viewpoint.far.Lotpacks;
import viewpoint.far.ShellBlock;
import viewpoint.far.ShellInteriors;
import viewpoint.far.ShellMesher;
import viewpoint.far.TreeSeasons;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.LongMap;
import viewpoint.platform.Pool;
import viewpoint.platform.Profile;
import viewpoint.render.FarGpu;
import viewpoint.render.GroundPage;
import viewpoint.render.GroundTiles;
import viewpoint.render.SceneData;
import viewpoint.render.ShellGround;
import viewpoint.render.TreeAtlas;
import viewpoint.world.Cook;

public final class FarShell {
    private static final LiveSettings.Number FULL_BLOCKS = LiveSettings.number("lod.shellFullBlocks", "Full shell up to (blocks)", "World/Levels of detail", 0.0f, 12.0f, 1.0f, 3.0f);
    private static final LiveSettings.Number LITE_BLOCKS = LiveSettings.number("lod.shellLiteBlocks", "Light shell up to (blocks)", "World/Levels of detail", 0.0f, 24.0f, 1.0f, 10.0f);
    private static final LiveSettings.Number JOBS = LiveSettings.number("far.shellJobs", "Shell blocks built at once", "World/Far world", 1.0f, 8.0f, 1.0f, 2.0f);
    static final float HOLD = 32.0f;
    static final byte MASK_LITE = 1;
    static final byte MASK_FULL = 2;
    static final byte MASK_LITE_GROUND = 3;
    private static final float TREE_HEIGHT = 4.0f;
    private static final double RELAX = 0.6;
    private static int vertices;
    private static long heldBytes;
    private static int groundsFull;
    private static int groundsLite;
    private static int groundMode;
    private static float allowedReach;
    private static float allowedFullReach;
    private static final LongMap<ShellBlock> blocks;
    private static final LongMap<ShellBlock> leaving;
    private static final ArrayList<ShellBlock> wanted;
    private static int running;
    private static int generation;
    private static final ConcurrentLinkedQueue<Done> DONE;
    private static final ConcurrentLinkedQueue<Facades> FACADES;

    static void update(Frame frame, float f, float f2, long l, long l2) {
        SceneData sceneData = frame.scene;
        long l3 = System.nanoTime();
        FarShell.takeIn();
        int n = 64;
        int n2 = 256 / n;
        int n3 = Math.floorDiv((int)Math.floor(f), n);
        int n4 = Math.floorDiv((int)Math.floor(f2), n);
        sceneData.shellMaskX = n3 - 26;
        sceneData.shellMaskY = n4 - 26;
        Arrays.fill(sceneData.shellMask, (byte)0);
        float f3 = sceneData.nearEnd() - 8.0f;
        float f4 = LITE_BLOCKS.get() * (float)n;
        float f5 = Math.min(FULL_BLOCKS.get(), LITE_BLOCKS.get()) * (float)n;
        wanted.clear();
        int n5 = (int)Math.ceil((f4 + 32.0f) / (float)n);
        for (int i = n4 - n5; i <= n4 + n5; ++i) {
            for (int j = n3 - n5; j <= n3 + n5; ++j) {
                FarShell.visit(frame, j, i, f3, f4, f5, l, l3);
            }
        }
        FarShell.leave(sceneData, frame, l, l3);
        FarShell.makeRoom(f5);
        FarShell.startJobs(n2, l2);
    }

    private static void visit(Frame frame, int n, int n2, float f, float f2, float f3, long l, long l2) {
        FarWorld.Slot slot;
        ShellBlock shellBlock;
        int n3 = 64;
        int n4 = 256 / n3;
        float f4 = frame.camX;
        float f5 = frame.camY;
        float f6 = (float)(n * n3) - f4;
        float f7 = f6 + (float)n3;
        float f8 = (float)(n2 * n3) - f5;
        float f9 = f8 + (float)n3;
        float f10 = Math.max(0.0f, Math.max(f6, -f7));
        float f11 = Math.max(0.0f, Math.max(f8, -f9));
        float f12 = Math.max(Math.abs(f6), Math.abs(f7));
        float f13 = Math.max(Math.abs(f8), Math.abs(f9));
        float f14 = (float)Math.sqrt(f10 * f10 + f11 * f11);
        float f15 = (float)Math.sqrt(f12 * f12 + f13 * f13);
        ShellBlock shellBlock2 = blocks.get(FarWorld.key(n, n2));
        ShellBlock shellBlock3 = shellBlock = shellBlock2 == null ? leaving.get(FarWorld.key(n, n2)) : null;
        if (shellBlock2 != null) {
            FarShell.settle(shellBlock2, l2);
        }
        boolean bl = shellBlock2 != null ? shellBlock2.piece != null || shellBlock2.coming != null : shellBlock != null;
        FarWorld.Slot slot2 = slot = FarShell.shelled(f14, f15, bl, f, allowedReach, f2) ? FarWorld.readCell(Math.floorDiv(n, n4), Math.floorDiv(n2, n4)) : null;
        if (slot == null) {
            return;
        }
        if (shellBlock2 == null) {
            shellBlock2 = shellBlock != null ? FarShell.back(n, n2, l2) : new ShellBlock(n, n2);
            blocks.put(FarWorld.key(n, n2), shellBlock2);
        }
        shellBlock2.seen = l;
        shellBlock2.distance = f14;
        shellBlock2.top = slot.maxHeight + 4.0f;
        shellBlock2.want = FarShell.tier(f14, shellBlock2.builtTier(), allowedFullReach, f3);
        shellBlock2.wantInteriors = FarShell.interiors(shellBlock2, slot, f4, f5, frame.scene.nearEnd());
        if (!(shellBlock2.busy || bl && shellBlock2.builtTier() == shellBlock2.want && shellBlock2.generation == generation && shellBlock2.wantInteriors == shellBlock2.interiors)) {
            wanted.add(shellBlock2);
        }
        if (bl || shellBlock2.previous != null) {
            FarShell.add(frame.scene, shellBlock2, frame, l2);
        }
    }

    static boolean shelled(float f, float f2, boolean bl, float f3, float f4, float f5) {
        float f6 = bl ? 32.0f : 0.0f;
        return f <= f5 + f6 && f < f4 && f2 >= f3 - f6;
    }

    static int tier(float f, int n, float f2, float f3) {
        float f4 = n == 0 ? f3 + 32.0f : f3;
        return f < f4 && f < f2 ? 0 : 1;
    }

    private static int[] interiors(ShellBlock shellBlock, FarWorld.Slot slot, float f, float f2, float f3) {
        if (shellBlock.want != 0) {
            return ShellInteriors.NONE;
        }
        if (slot.buildingBounds == null) {
            slot.buildingBounds = ShellInteriors.bounds(CellRooms.of(slot), slot.buildings);
        }
        int n = 4;
        return ShellInteriors.select(slot.buildingBounds, slot.cellX, slot.cellY, shellBlock.bx - slot.cellX * n, shellBlock.by - slot.cellY * n, f, f2, f3, shellBlock.interiors);
    }

    private static void makeRoom(float f) {
        Pool.SHELL.use(FarShell.poolBytes());
        if (!Pool.SHELL.over(0.9)) {
            if (!Pool.SHELL.over(0.6)) {
                allowedReach = Float.MAX_VALUE;
                allowedFullReach = Float.MAX_VALUE;
            }
            return;
        }
        ArrayList<ShellBlock> arrayList = new ArrayList<ShellBlock>();
        boolean bl = false;
        for (ShellBlock shellBlock3 : blocks.values()) {
            if (shellBlock3.piece == null) continue;
            arrayList.add(shellBlock3);
            bl |= shellBlock3.tier() == 0;
        }
        arrayList.sort((shellBlock, shellBlock2) -> Float.compare(shellBlock2.distance, shellBlock.distance));
        for (int i = 0; i < arrayList.size() && Pool.SHELL.over(0.8); ++i) {
            ShellBlock shellBlock3;
            shellBlock3 = (ShellBlock)arrayList.get(i);
            if (shellBlock3.tier() == 0) {
                allowedFullReach = Math.min(allowedFullReach, shellBlock3.distance);
                break;
            }
            if (!(shellBlock3.distance >= f) && bl) continue;
            allowedReach = Math.min(allowedReach, shellBlock3.distance);
            FarShell.drop(shellBlock3);
            blocks.remove(FarWorld.key(shellBlock3.bx, shellBlock3.by));
            Pool.SHELL.use(FarShell.poolBytes());
        }
    }

    private static void takeIn() {
        Record record;
        while ((record = DONE.poll()) != null) {
            FarShell.install(record);
        }
        Lotpacks.takeIn();
        while ((record = FACADES.poll()) != null) {
            --running;
            ((Facades)record).slot.facades = ((Facades)record).facades;
            ((Facades)record).slot.facading = false;
        }
        int n = (ShellGround.BAKED.get() ? 1 : 0) | (ShellGround.TEXELS_LITE.getInt() > 0 ? 2 : 0) | (ShellInteriors.FURNITURE.get() ? 4 : 0) | FarTrees.liteKeep() << 3;
        if (TreeSeasons.update() | Erosion.update() | n != groundMode) {
            groundMode = n;
            ++generation;
        }
        FarTreeCheck.update();
    }

    private static boolean bakesGround(int n) {
        return ShellGround.BAKED.get() && (n == 0 ? ShellGround.TEXELS_FULL.getInt() > 0 : ShellGround.TEXELS_LITE.getInt() > 0);
    }

    private static long poolBytes() {
        return heldBytes + TreeAtlas.bytes() + (long)groundsFull * ShellGround.pageBytes(ShellGround.TEXELS_FULL.getInt()) + (long)groundsLite * ShellGround.pageBytes(ShellGround.TEXELS_LITE.getInt());
    }

    private static void startJobs(int n, long l) {
        wanted.sort((shellBlock, shellBlock2) -> Float.compare(shellBlock.distance, shellBlock2.distance));
        for (ShellBlock shellBlock3 : wanted) {
            if (running >= JOBS.getInt() || System.nanoTime() > l) break;
            if (blocks.get(FarWorld.key(shellBlock3.bx, shellBlock3.by)) != shellBlock3) continue;
            FarWorld.Slot slot = FarWorld.readCell(Math.floorDiv(shellBlock3.bx, n), Math.floorDiv(shellBlock3.by, n));
            if (slot.facades == null) {
                if (slot.facading) continue;
                FarShell.prepare(slot);
                continue;
            }
            FarShell.start(shellBlock3, slot, shellBlock3.want);
        }
    }

    private static void drop(ShellBlock shellBlock) {
        FarShell.hold(shellBlock.piece, -1);
        FarShell.hold(shellBlock.previous, -1);
        FarShell.hold(shellBlock.coming, -1);
        shellBlock.coming = null;
        shellBlock.previous = null;
        shellBlock.piece = null;
    }

    private static void hold(ShellBlock.Piece piece, int n) {
        if (piece == null) {
            return;
        }
        vertices += n * piece.vertices();
        heldBytes += (long)n * piece.bytes();
        if (piece.ground != null && piece.tier == 0) {
            groundsFull += n;
        } else if (piece.ground != null) {
            groundsLite += n;
        }
        Profile.shellVertices = vertices;
        if (n < 0) {
            piece.retire();
        }
    }

    private static void leave(SceneData sceneData, Frame frame, long l, long l2) {
        ShellBlock shellBlock;
        Iterator<ShellBlock> iterator = blocks.values().iterator();
        while (iterator.hasNext()) {
            shellBlock = iterator.next();
            if (shellBlock.seen == l) continue;
            iterator.remove();
            if (shellBlock.piece == null) {
                FarShell.drop(shellBlock);
                continue;
            }
            FarShell.hold(shellBlock.previous, -1);
            FarShell.hold(shellBlock.coming, -1);
            shellBlock.previous = shellBlock.piece;
            shellBlock.coming = null;
            shellBlock.piece = null;
            shellBlock.fade.begin(l2, 600000000L, 0.0f);
            leaving.put(FarWorld.key(shellBlock.bx, shellBlock.by), shellBlock);
        }
        iterator = leaving.values().iterator();
        while (iterator.hasNext()) {
            shellBlock = iterator.next();
            if (shellBlock.fade.progress(l2) >= 1.0f) {
                FarShell.drop(shellBlock);
                iterator.remove();
                continue;
            }
            FarShell.add(sceneData, shellBlock, frame, l2);
        }
    }

    private static ShellBlock back(int n, int n2, long l) {
        ShellBlock shellBlock = leaving.remove(FarWorld.key(n, n2));
        if (shellBlock != null) {
            shellBlock.piece = shellBlock.previous;
            shellBlock.previous = null;
            shellBlock.fade.begin(l, 600000000L, 1.0f - shellBlock.fade.progress(l));
        }
        return shellBlock;
    }

    private static void add(SceneData sceneData, ShellBlock shellBlock, Frame frame, long l) {
        float f = shellBlock.fade.progress(l);
        if (f >= 1.0f && shellBlock.previous != null) {
            FarShell.hold(shellBlock.previous, -1);
            shellBlock.previous = null;
        }
        shellBlock.emit(sceneData, frame, f);
    }

    private static void prepare(FarWorld.Slot slot) {
        short[][] sArray = CellRooms.of(slot);
        String string = slot.lotpack;
        byte[] byArray = Lotpacks.get(string);
        int n = slot.minLevel;
        int n2 = slot.maxLevel;
        int n3 = slot.buildings;
        FarTiles.Tile[] tileArray = slot.tiles;
        slot.facading = true;
        ++running;
        FarWorkers.jobs.incrementAndGet();
        FarWorkers.POOL.execute(() -> {
            int[] nArray = new int[]{};
            try {
                nArray = ShellMesher.facades(Lotpacks.read(string, byArray), n, n2, tileArray, sArray, n3);
            }
            catch (Throwable throwable) {
                System.out.println("[Viewpoint] far world: the facades of cell " + slot.cellX + "," + slot.cellY + " failed: " + String.valueOf(throwable));
            }
            finally {
                FACADES.add(new Facades(slot, nArray));
                FarWorkers.jobs.decrementAndGet();
            }
        });
    }

    private static void start(ShellBlock shellBlock, FarWorld.Slot slot, int n) {
        short[][] sArray = CellRooms.of(slot);
        String string = slot.lotpack;
        byte[] byArray = Lotpacks.get(string);
        int n2 = slot.minLevel;
        int n3 = slot.maxLevel;
        int n4 = 4;
        FarTiles.Tile[] tileArray = slot.tiles;
        int[] nArray = slot.facades.length == 0 ? null : slot.facades;
        byte[] byArray2 = slot.biomes;
        BiomeTrees.Legend legend = slot.legend;
        BiomeTrees.Table table = BiomeTrees.table();
        byte[][] byArray3 = TreeSeasons.displays();
        if (slot.groundTiles == null && FarShell.bakesGround(n)) {
            slot.groundTiles = FarTiles.ground(tileArray);
        }
        GroundTiles groundTiles = FarShell.bakesGround(n) ? slot.groundTiles : null;
        int n5 = slot.cellX;
        int n6 = slot.cellY;
        int n7 = shellBlock.bx - n5 * n4;
        int n8 = shellBlock.by - n6 * n4;
        int n9 = generation;
        int[] nArray2 = shellBlock.wantInteriors;
        FarTrees.Thinning thinning = FarTrees.Thinning.of(n, n5, n6);
        byte[] byArray4 = ShellInteriors.mask(nArray2, ShellInteriors.FURNITURE.get());
        shellBlock.busy = true;
        ++running;
        FarWorkers.jobs.incrementAndGet();
        FarWorkers.POOL.execute(() -> {
            try {
                byte[] byArray5 = Lotpacks.read(string, byArray);
                ShellMesher.Result result = ShellMesher.mesh(n5, n6, n7, n8, byArray5, n2, n3, tileArray, sArray, nArray, byArray2, table, legend, byArray3, n, groundTiles, byArray4, thinning);
                DONE.add(new Done(shellBlock, n, n9, nArray2, FarShell.gpu(result.shell()), result.inside(), result.ground(), false));
            }
            catch (Throwable throwable) {
                System.out.println("[Viewpoint] far world: the shell of block " + shellBlock.bx + "," + shellBlock.by + " failed: " + String.valueOf(throwable));
                DONE.add(new Done(shellBlock, n, n9, nArray2, null, null, null, true));
            }
            finally {
                FarWorkers.jobs.decrementAndGet();
            }
        });
    }

    private static void install(Done done) {
        --running;
        ShellBlock shellBlock = done.block;
        if (done.failed) {
            return;
        }
        shellBlock.busy = false;
        FarGpu.Mesh mesh = new FarGpu.Mesh(1, done.inside);
        if (blocks.get(FarWorld.key(shellBlock.bx, shellBlock.by)) != shellBlock) {
            FarGpu.retire(done.shell);
            FarGpu.retire(mesh);
            return;
        }
        FarShell.hold(shellBlock.coming, -1);
        shellBlock.coming = new ShellBlock.Piece(done.shell, mesh, done.ground, done.tier);
        shellBlock.generation = done.generation;
        shellBlock.interiors = done.interiors;
        FarShell.hold(shellBlock.coming, 1);
        FarGpu.upload(done.shell);
        FarGpu.upload(mesh);
    }

    private static void settle(ShellBlock shellBlock, long l) {
        ShellBlock.Piece piece = shellBlock.coming;
        if (piece == null || !piece.uploaded()) {
            return;
        }
        FarShell.hold(shellBlock.previous, -1);
        shellBlock.previous = shellBlock.piece;
        shellBlock.fade.begin(l, shellBlock.previous == null ? 600000000L : 400000000L, 0.0f);
        shellBlock.piece = piece;
        shellBlock.coming = null;
    }

    static void clear() {
        for (ShellBlock shellBlock : blocks.values()) {
            FarShell.drop(shellBlock);
        }
        for (ShellBlock shellBlock : leaving.values()) {
            FarShell.drop(shellBlock);
        }
        blocks.clear();
        leaving.clear();
        Lotpacks.clear();
        vertices = 0;
        heldBytes = 0L;
        groundsLite = 0;
        groundsFull = 0;
        Profile.shellVertices = 0;
        Pool.SHELL.use(0L);
        allowedReach = Float.MAX_VALUE;
        allowedFullReach = Float.MAX_VALUE;
    }

    private static FarGpu.Shell gpu(Cook.Cooked cooked) {
        return cooked == null ? null : new FarGpu.Shell(cooked.pages(), cooked.first(), cooked.count(), cooked.verts());
    }

    private FarShell() {
    }

    static {
        groundMode = -1;
        allowedReach = Float.MAX_VALUE;
        allowedFullReach = Float.MAX_VALUE;
        blocks = new LongMap(256);
        leaving = new LongMap(64);
        wanted = new ArrayList();
        DONE = new ConcurrentLinkedQueue();
        FACADES = new ConcurrentLinkedQueue();
    }

    private record Done(ShellBlock block, int tier, int generation, int[] interiors, FarGpu.Shell shell, FloatBuffer inside, GroundPage ground, boolean failed) {
    }

    private record Facades(FarWorld.Slot slot, int[] facades) {
    }
}

