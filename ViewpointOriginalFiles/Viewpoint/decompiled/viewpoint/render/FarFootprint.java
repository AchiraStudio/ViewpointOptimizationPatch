/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import viewpoint.render.FarGpu;
import viewpoint.render.SceneData;

final class FarFootprint {
    private static final float GROWN = 8.0f;
    private static final int BUILT = 128;
    private static final int BLOCK_CHUNKS = 8;
    private static final int CELL_BLOCKS = 4;
    float x0;
    float z0;
    float x1;
    float z1;

    FarFootprint() {
    }

    boolean block(SceneData sceneData, FarGpu.BlockDraw blockDraw) {
        int n;
        int n2;
        int n3 = blockDraw.worldX + blockDraw.blockX;
        int n4 = blockDraw.worldY + blockDraw.blockY;
        int n5 = Math.floorDiv(n3, 8);
        int n6 = Math.floorDiv(n4, 8);
        int n7 = Integer.MAX_VALUE;
        int n8 = Integer.MAX_VALUE;
        int n9 = Integer.MIN_VALUE;
        int n10 = Integer.MIN_VALUE;
        for (n2 = 0; n2 < 8; ++n2) {
            for (n = 0; n < 8; ++n) {
                if (FarFootprint.built(sceneData, n5 + n, n6 + n2)) continue;
                n7 = Math.min(n7, n);
                n8 = Math.min(n8, n2);
                n9 = Math.max(n9, n);
                n10 = Math.max(n10, n2);
            }
        }
        if (n9 < 0) {
            return false;
        }
        n2 = blockDraw.blockX;
        n = blockDraw.blockY;
        this.set(blockDraw.originX, blockDraw.originZ, n2 + n7 * 8, n + n8 * 8, n2 + (n9 + 1) * 8, n + (n10 + 1) * 8, n2, n, n2 + 64, n + 64);
        return true;
    }

    boolean cell(SceneData sceneData, FarGpu.CellDraw cellDraw) {
        int n = Math.floorDiv(cellDraw.worldX, 64);
        int n2 = Math.floorDiv(cellDraw.worldY, 64);
        int n3 = Integer.MAX_VALUE;
        int n4 = Integer.MAX_VALUE;
        int n5 = Integer.MIN_VALUE;
        int n6 = Integer.MIN_VALUE;
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                if (FarFootprint.shellStands(sceneData, n + j, n2 + i) || FarFootprint.withinNear(sceneData, cellDraw, j, i)) continue;
                n3 = Math.min(n3, j);
                n4 = Math.min(n4, i);
                n5 = Math.max(n5, j);
                n6 = Math.max(n6, i);
            }
        }
        if (n5 < 0) {
            return false;
        }
        this.set(cellDraw.originX, cellDraw.originZ, n3 * 64, n4 * 64, (n5 + 1) * 64, (n6 + 1) * 64, 0, 0, 256, 256);
        return true;
    }

    private void set(float f, float f2, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8) {
        float f3 = Math.max((float)n5, (float)n - 8.0f);
        float f4 = Math.max((float)n6, (float)n2 - 8.0f);
        float f5 = Math.min((float)n7, (float)n3 + 8.0f);
        float f6 = Math.min((float)n8, (float)n4 + 8.0f);
        this.x0 = f - f5;
        this.x1 = f - f3;
        this.z0 = f2 - f6;
        this.z1 = f2 - f4;
    }

    static boolean reaches(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        if (f <= 0.0f) {
            return true;
        }
        float f9 = (f4 + f6) * 0.5f;
        float f10 = (f5 + f7) * 0.5f;
        float f11 = f8 * f2;
        float f12 = f8 * f3;
        float f13 = (f6 - f4) * 0.5f + f;
        float f14 = (f7 - f5) * 0.5f + f;
        return Math.max(FarFootprint.enters(f9, f11, f13), FarFootprint.enters(f10, f12, f14)) <= Math.min(FarFootprint.leaves(f9, f11, f13), FarFootprint.leaves(f10, f12, f14));
    }

    private static float enters(float f, float f2, float f3) {
        if (f2 == 0.0f) {
            return Math.abs(f) <= f3 ? 0.0f : 2.0f;
        }
        return Math.max(0.0f, Math.min((-f3 - f) / f2, (f3 - f) / f2));
    }

    private static float leaves(float f, float f2, float f3) {
        if (f2 == 0.0f) {
            return Math.abs(f) <= f3 ? 1.0f : -1.0f;
        }
        return Math.min(1.0f, Math.max((-f3 - f) / f2, (f3 - f) / f2));
    }

    private static boolean built(SceneData sceneData, int n, int n2) {
        int n3 = n - sceneData.farMaskX;
        int n4 = n2 - sceneData.farMaskY;
        int n5 = sceneData.farMaskSize;
        return n3 >= 0 && n4 >= 0 && n3 < n5 && n4 < n5 && (sceneData.farMask[n4 * n5 + n3] & 0xFF) >= 128;
    }

    private static boolean shellStands(SceneData sceneData, int n, int n2) {
        int n3 = n - sceneData.shellMaskX;
        int n4 = n2 - sceneData.shellMaskY;
        if (n3 < 0 || n4 < 0 || n3 >= 53 || n4 >= 53) {
            return false;
        }
        int n5 = (n4 * 53 + n3) * 3;
        return sceneData.shellMask[n5] != 0 || sceneData.shellMask[n5 + 2] != 0;
    }

    private static boolean withinNear(SceneData sceneData, FarGpu.CellDraw cellDraw, int n, int n2) {
        float f = sceneData.nearReach - 8.0f;
        float f2 = 0.0f;
        for (int i = 0; i < 4; ++i) {
            float f3 = cellDraw.originX - (float)((n + (i & 1)) * 64);
            float f4 = cellDraw.originZ - (float)((n2 + (i >> 1)) * 64);
            f2 = Math.max(f2, f3 * f3 + f4 * f4);
        }
        return f > 0.0f && f2 < f * f;
    }
}

