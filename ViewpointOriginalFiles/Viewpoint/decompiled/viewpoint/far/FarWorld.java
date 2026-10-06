/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import viewpoint.core.Frame;
import viewpoint.far.BiomeTrees;
import viewpoint.far.BlockLight;
import viewpoint.far.CellJobs;
import viewpoint.far.CellLight;
import viewpoint.far.CellMemory;
import viewpoint.far.Dissolve;
import viewpoint.far.FarColours;
import viewpoint.far.FarShell;
import viewpoint.far.FarTiles;
import viewpoint.far.FarTrees;
import viewpoint.far.FarWorkers;
import viewpoint.platform.Caches;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.LongMap;
import viewpoint.platform.Pool;
import viewpoint.render.FarGpu;
import viewpoint.render.GroundTiles;
import viewpoint.render.SceneData;

public final class FarWorld {
    static final String LODS = "World/Levels of detail";
    static final String WORK = "World/Far world";
    private static final LiveSettings.Number REACH = LiveSettings.number("lod.farBlocks", "Far world up to (blocks)", "World/Levels of detail", 0.0f, 64.0f, 1.0f, 24.0f);
    private static final LiveSettings.Number DETAIL_1 = LiveSettings.number("lod.boxesFineBlocks", "Boxes of 1 square up to (blocks)", "World/Levels of detail", 0.0f, 64.0f, 1.0f, 3.0f);
    private static final LiveSettings.Number DETAIL_2 = LiveSettings.number("lod.boxesMidBlocks", "Boxes of 2 squares up to (blocks)", "World/Levels of detail", 0.0f, 64.0f, 1.0f, 6.0f);
    private static final LiveSettings.Number DETAIL_4 = LiveSettings.number("lod.boxesCoarseBlocks", "Boxes of 4 squares up to (blocks)", "World/Levels of detail", 0.0f, 64.0f, 1.0f, 12.0f);
    private static final LiveSettings.Number[] DETAIL_REACH = new LiveSettings.Number[]{DETAIL_1, DETAIL_2, DETAIL_4};
    private static final int[] DETAIL = new int[]{1, 2, 4, 8};
    private static final LiveSettings.Number JOBS = LiveSettings.number("far.cellJobs", "Cells read at once", "World/Far world", 1.0f, 16.0f, 1.0f, 4.0f);
    private static final LiveSettings.Number BUDGET = LiveSettings.number("far.budgetMs", "Main thread budget (ms)", "World/Far world", 0.25f, 10.0f, 0.25f, 1.0f);
    private static final float KEEP_MARGIN = 256.0f;
    private static final double RELAX = 0.6;
    private static float reachLimit = Float.MAX_VALUE;
    static final LongMap<Slot> slots = new LongMap(256);
    private static final HashSet<Long> missing;
    private static final ArrayList<Slot> wanted;
    private static long frameCounter;
    private static long now;
    private static boolean broken;

    public static float reach() {
        return REACH.get() * 64.0f;
    }

    public static void update(Frame frame) {
        frame.scene.farCount = 0;
        if (broken) {
            return;
        }
        try {
            FarWorld.updateWorld(frame);
        }
        catch (RuntimeException runtimeException) {
            broken = true;
            frame.scene.farCount = 0;
            System.out.println("[Viewpoint] far world off after an error: " + String.valueOf(runtimeException));
            runtimeException.printStackTrace(System.out);
            FarWorld.clear();
        }
    }

    private static void updateWorld(Frame frame) {
        CellJobs.Done done;
        SceneData sceneData = frame.scene;
        float f = FarWorld.reach();
        if (f <= 0.0f) {
            if (!slots.isEmpty()) {
                FarWorld.clear();
            }
            return;
        }
        FarWorkers.resize();
        FarColours.load(FarWorkers.POOL);
        if (!FarColours.ready()) {
            return;
        }
        ++frameCounter;
        now = System.nanoTime();
        while ((done = CellJobs.poll()) != null) {
            FarWorld.install(done);
        }
        float f2 = frame.camX;
        float f3 = frame.camY;
        int n = (int)Math.floor((f2 - f) / 256.0f);
        int n2 = (int)Math.floor((f2 + f) / 256.0f);
        int n3 = (int)Math.floor((f3 - f) / 256.0f);
        int n4 = (int)Math.floor((f3 + f) / 256.0f);
        wanted.clear();
        for (int i = n3; i <= n4; ++i) {
            for (int j = n; j <= n2; ++j) {
                float f4;
                float f5 = Math.max(0.0f, Math.max((float)(j * 256) - f2, f2 - (float)((j + 1) * 256)));
                float f6 = (float)Math.sqrt(f5 * f5 + (f4 = Math.max(0.0f, Math.max((float)(i * 256) - f3, f3 - (float)((i + 1) * 256)))) * f4);
                if (!(f6 <= f) || !(f6 < reachLimit)) continue;
                FarWorld.visit(sceneData, frame, j, i, f6);
            }
        }
        wanted.sort((slot, slot2) -> Float.compare(slot.distance, slot2.distance));
        long l = System.nanoTime() + (long)(BUDGET.get() * 1000000.0f);
        FarWorld.startJobs(l);
        FarShell.update(frame, f2, f3, frameCounter, l);
        FarWorld.letGo(f2, f3, f);
        FarWorld.makeRoom();
    }

    private static void visit(SceneData sceneData, Frame frame, int n, int n2, float f) {
        long l = FarWorld.key(n, n2);
        Slot slot = slots.get(l);
        if (slot == null) {
            if (missing.contains(l)) {
                return;
            }
            if (CellJobs.mapFiles(n, n2) == null) {
                missing.add(l);
                return;
            }
            slot = new Slot(n, n2);
            slots.put(l, slot);
        }
        slot.seen = frameCounter;
        slot.distance = f;
        FarWorld.settle(slot);
        if (!slot.busy && (slot.mesh == null || slot.mesh.detail != FarWorld.detail(f) || slot.height == null && slot.wantsData)) {
            wanted.add(slot);
        }
        if (slot.mesh != null && slot.colours != null) {
            FarWorld.add(sceneData, slot, frame);
        }
    }

    /*
     * WARNING - void declaration
     */
    private static void makeRoom() {
        void var3_5;
        long l = 0L;
        for (Slot object : slots.values()) {
            l += FarWorld.gpuBytes(object);
        }
        Pool.FAR.use(l);
        if (!Pool.FAR.over(0.9)) {
            if (!Pool.FAR.over(0.6)) {
                reachLimit = Float.MAX_VALUE;
            }
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (Slot slot3 : slots.values()) {
            if (FarWorld.gpuBytes(slot3) <= 0L) continue;
            arrayList.add(slot3);
        }
        arrayList.sort((slot, slot2) -> Float.compare(slot2.distance, slot.distance));
        boolean bl = false;
        while (var3_5 < arrayList.size() && Pool.FAR.over(0.8)) {
            Slot slot3;
            slot3 = (Slot)arrayList.get((int)var3_5);
            reachLimit = Math.min(reachLimit, slot3.distance);
            FarWorld.retire(slot3);
            slots.remove(FarWorld.key(slot3.cellX, slot3.cellY));
            Pool.FAR.use(l -= FarWorld.gpuBytes(slot3));
            ++var3_5;
        }
    }

    private static long gpuBytes(Slot slot) {
        return (slot.mesh == null ? 0L : slot.mesh.bytes()) + (slot.colours == null ? 0L : 699050L) + (slot.trees == null ? 0L : slot.trees.bytes()) + (slot.oldMesh == null ? 0L : slot.oldMesh.bytes()) + (slot.oldColours == null ? 0L : 699050L) + (slot.oldTrees == null ? 0L : slot.oldTrees.bytes()) + (slot.coming == null ? 0L : FarWorld.bytes(slot.coming));
    }

    private static long bytes(CellJobs.Done done) {
        return done.mesh().bytes() + (done.colours() == null ? 0L : 699050L) + (done.trees() == null ? 0L : done.trees().bytes());
    }

    private static void retire(Slot slot) {
        FarGpu.retire(slot.mesh);
        FarGpu.retire(slot.colours);
        FarGpu.retire(slot.trees);
        FarWorld.retireOld(slot);
        FarWorld.retireComing(slot);
    }

    private static void startJobs(long l) {
        for (Slot slot : wanted) {
            if (FarWorkers.jobs.get() < JOBS.getInt() && CellJobs.start(slot, l)) continue;
            break;
        }
    }

    private static void letGo(float f, float f2, float f3) {
        Iterator<Slot> iterator = slots.values().iterator();
        while (iterator.hasNext()) {
            float f4;
            float f5;
            Slot slot = iterator.next();
            if (slot.seen == frameCounter || !((f5 = Math.max(0.0f, Math.max((float)(slot.cellX * 256) - f, f - (float)((slot.cellX + 1) * 256)))) * f5 + (f4 = Math.max(0.0f, Math.max((float)(slot.cellY * 256) - f2, f2 - (float)((slot.cellY + 1) * 256)))) * f4 > (f3 + 256.0f) * (f3 + 256.0f))) continue;
            FarWorld.retire(slot);
            iterator.remove();
        }
    }

    static int detail(float f) {
        for (int i = 0; i < DETAIL_REACH.length; ++i) {
            if (!(f < DETAIL_REACH[i].get() * 64.0f)) continue;
            return DETAIL[i];
        }
        return DETAIL[DETAIL.length - 1];
    }

    private static void add(SceneData sceneData, Slot slot, Frame frame) {
        float f = slot.fade.progress(now);
        if (f >= 1.0f) {
            FarWorld.retireOld(slot);
        }
        FarWorld.add(sceneData, slot, frame, slot.mesh, slot.colours, slot.trees, 0.0f, Dissolve.top(f));
        if (slot.oldMesh != null) {
            FarWorld.add(sceneData, slot, frame, slot.oldMesh, slot.oldColours != null ? slot.oldColours : slot.colours, slot.oldTrees != null ? slot.oldTrees : slot.trees, f, 2.0f);
        }
    }

    private static void add(SceneData sceneData, Slot slot, Frame frame, FarGpu.Mesh mesh, FarGpu.Colours colours, FarGpu.Trees trees, float f, float f2) {
        FarGpu.CellDraw cellDraw;
        if (sceneData.farCount == sceneData.far.length) {
            sceneData.far = Arrays.copyOf(sceneData.far, sceneData.far.length * 2);
        }
        if ((cellDraw = sceneData.far[sceneData.farCount]) == null) {
            cellDraw = sceneData.far[sceneData.farCount] = new FarGpu.CellDraw();
        }
        ++sceneData.farCount;
        cellDraw.mesh = mesh;
        cellDraw.colours = colours;
        cellDraw.trees = trees;
        cellDraw.treeKeep = FarTrees.boxKeep(mesh.detail);
        cellDraw.treeGrow = FarTrees.grow(cellDraw.treeKeep);
        cellDraw.fadeLo = f;
        cellDraw.fadeHi = f2;
        cellDraw.worldX = slot.cellX * 256;
        cellDraw.worldY = slot.cellY * 256;
        cellDraw.originX = (float)((double)frame.camX - (double)cellDraw.worldX);
        cellDraw.originY = -frame.camZ * 2.4494896f;
        cellDraw.originZ = (float)((double)frame.camY - (double)cellDraw.worldY);
        cellDraw.maxHeight = slot.maxHeight;
    }

    private static void install(CellJobs.Done done) {
        Slot slot = done.slot();
        if (done.failed()) {
            return;
        }
        if (slots.get(FarWorld.key(slot.cellX, slot.cellY)) != slot) {
            FarWorld.retire(done);
            return;
        }
        FarWorld.retireComing(slot);
        slot.coming = done;
        FarGpu.upload(done.mesh());
        FarGpu.upload(done.colours());
        FarGpu.upload(done.trees());
    }

    private static void settle(Slot slot) {
        CellJobs.Done done = slot.coming;
        if (done == null || !done.uploaded()) {
            return;
        }
        slot.coming = null;
        slot.busy = false;
        FarWorld.retireOld(slot);
        slot.fade.begin(now, slot.mesh == null ? 600000000L : 400000000L, 0.0f);
        slot.oldMesh = slot.mesh;
        if (done.cell() != null) {
            slot.wantsData = false;
            slot.used = Caches.tick();
            slot.height = done.cell().height;
            slot.maxHeight = done.cell().maxHeight;
            slot.biomes = done.cell().biomes;
            slot.oldColours = slot.colours;
            slot.colours = done.colours();
            slot.oldTrees = slot.trees;
            slot.trees = done.trees();
        }
        slot.mesh = done.mesh();
    }

    private static void retireComing(Slot slot) {
        if (slot.coming != null) {
            FarWorld.retire(slot.coming);
            slot.coming = null;
        }
    }

    private static void retire(CellJobs.Done done) {
        FarGpu.retire(done.mesh());
        FarGpu.retire(done.colours());
        FarGpu.retire(done.trees());
    }

    private static void retireOld(Slot slot) {
        FarGpu.retire(slot.oldMesh);
        FarGpu.retire(slot.oldColours);
        FarGpu.retire(slot.oldTrees);
        slot.oldMesh = null;
        slot.oldColours = null;
        slot.oldTrees = null;
    }

    static long key(int n, int n2) {
        return (long)n << 32 | (long)n2 & 0xFFFFFFFFL;
    }

    static Slot readCell(int n, int n2) {
        Slot slot = slots.get(FarWorld.key(n, n2));
        if (slot == null) {
            return null;
        }
        if (slot.height == null || slot.tiles == null) {
            slot.wantsData = true;
            return null;
        }
        slot.used = Caches.tick();
        return slot;
    }

    public static void clear() {
        FarShell.clear();
        BlockLight.clear();
        CellLight.clear();
        for (Slot slot : slots.values()) {
            FarWorld.retire(slot);
        }
        slots.clear();
        missing.clear();
        FarTiles.forget();
        reachLimit = Float.MAX_VALUE;
        Pool.FAR.use(0L);
    }

    private FarWorld() {
    }

    static {
        Caches.register(new CellMemory());
        missing = new HashSet();
        wanted = new ArrayList();
    }

    static final class Slot {
        final int cellX;
        final int cellY;
        short[] height;
        float maxHeight;
        FarGpu.Mesh mesh;
        FarGpu.Colours colours;
        FarGpu.Trees trees;
        FarGpu.Mesh oldMesh;
        FarGpu.Colours oldColours;
        FarGpu.Trees oldTrees;
        CellJobs.Done coming;
        final Dissolve fade = new Dissolve();
        boolean busy;
        long seen;
        float distance;
        String lotpack;
        int minLevel;
        int maxLevel;
        FarTiles.Tile[] tiles;
        byte[] biomes;
        BiomeTrees.Legend legend;
        short[][] rooms;
        int buildings;
        int[] facades;
        GroundTiles groundTiles;
        int[] buildingBounds;
        boolean facading;
        long used;
        boolean wantsData;

        Slot(int n, int n2) {
            this.cellX = n;
            this.cellY = n2;
        }
    }
}

