/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.world;

import java.util.Arrays;
import viewpoint.platform.Pool;
import viewpoint.render.BandLight;
import viewpoint.render.ChunkMeshData;
import viewpoint.world.ChunkCache;

final class ChunkBudget {
    private static final long VERTEX_BYTES = 56L;
    private static long vertices;
    private static int meshes;
    static boolean tight;
    private static ChunkCache.Level[] outOfView;
    private static long[] keys;
    private static int count;

    static void begin() {
        Arrays.fill(outOfView, 0, count, null);
        count = 0;
    }

    static void hold(ChunkMeshData chunkMeshData) {
        if (chunkMeshData != null) {
            vertices += (long)chunkMeshData.vertices();
            ++meshes;
        }
    }

    static void letGo(ChunkMeshData chunkMeshData) {
        if (chunkMeshData != null) {
            vertices -= (long)chunkMeshData.vertices();
            --meshes;
            chunkMeshData.retire();
        }
    }

    static void outOfView(ChunkCache.Level level, float f) {
        if (count == outOfView.length) {
            outOfView = Arrays.copyOf(outOfView, count * 2);
            keys = Arrays.copyOf(keys, count * 2);
        }
        ChunkBudget.keys[ChunkBudget.count] = (long)Float.floatToIntBits(Math.max(0.0f, f)) << 32 | (long)count;
        ChunkBudget.outOfView[ChunkBudget.count++] = level;
    }

    static boolean full() {
        ChunkBudget.count();
        return Pool.NEAR_MESHES.over(0.9) || Pool.LIGHT_GRIDS.over(0.9);
    }

    static void makeRoom() {
        if (ChunkBudget.full()) {
            Arrays.sort(keys, 0, count);
            for (int i = count - 1; i >= 0 && (Pool.NEAR_MESHES.over(0.8) || Pool.LIGHT_GRIDS.over(0.8)); --i) {
                ChunkBudget.unbuild(outOfView[(int)keys[i]]);
                ChunkBudget.count();
            }
        }
        tight = ChunkBudget.full();
    }

    private static void count() {
        Pool.NEAR_MESHES.use(vertices * 56L);
        Pool.LIGHT_GRIDS.use((long)(meshes + BandLight.grids()) * 3200L);
    }

    private static void unbuild(ChunkCache.Level level) {
        ChunkBudget.letGo(level.data);
        ChunkBudget.letGo(level.fading);
        level.fading = null;
        level.data = null;
        level.fadeNanos = 0L;
        level.fadingModelled = null;
        level.modelled = null;
        level.fadingModelledItems = null;
        level.modelledItems = null;
        level.fadingBodies = null;
        level.bodies = null;
        level.fingerprint = 0L;
        ++level.version;
        level.dirty = true;
        level.reason = 0;
        level.retryFrame = 0L;
        level.retryDelay = 0;
    }

    private ChunkBudget() {
    }

    static {
        outOfView = new ChunkCache.Level[256];
        keys = new long[256];
    }
}

