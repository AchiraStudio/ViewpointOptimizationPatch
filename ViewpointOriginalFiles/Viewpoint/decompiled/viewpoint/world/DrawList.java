/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 */
package viewpoint.world;

import java.util.Arrays;
import org.joml.Matrix4f;
import viewpoint.platform.LongMap;
import viewpoint.render.ChunkMeshData;
import viewpoint.render.FloorPage;
import viewpoint.render.SceneData;
import viewpoint.render.WorldRenderer;
import viewpoint.visibility.Rooms;
import viewpoint.world.BodyCards;
import viewpoint.world.FloorPages;

final class DrawList {
    private static final int MASK = 48;
    private static final Matrix4f lightView = new Matrix4f();
    private static final boolean[] mask = new boolean[2304];
    private static ChunkMeshData[] candMesh = new ChunkMeshData[1024];
    private static float[] candOffset = new float[3072];
    private static float[] candDistance = new float[1024];
    private static float[] candFoot = new float[4096];
    private static float[] candFade = new float[2048];
    private static byte[] candFlags = new byte[1024];
    private static int[] candChunk = new int[2048];
    private static BodyCards[] candBodies = new BodyCards[1024];
    private static int candidates;
    private static final LongMap<ChunkMeshData> handed;

    static void begin(SceneData sceneData, boolean bl) {
        if (bl) {
            boolean bl2 = Math.abs(sceneData.sunY) > 0.99f;
            lightView.setLookAt(sceneData.sunX, sceneData.sunY, sceneData.sunZ, 0.0f, 0.0f, 0.0f, 0.0f, bl2 ? 0.0f : 1.0f, bl2 ? 1.0f : 0.0f);
        }
        candidates = 0;
    }

    static void candidate(ChunkMeshData chunkMeshData, float f, float f2, float f3, byte by, float f4, boolean bl, float f5, float f6, int n, float f7, float f8, float f9, int n2, int n3, BodyCards bodyCards) {
        if (candidates == candMesh.length) {
            candChunk = Arrays.copyOf(candChunk, candidates * 4);
            candMesh = Arrays.copyOf(candMesh, candidates * 2);
            candBodies = Arrays.copyOf(candBodies, candidates * 2);
            candOffset = Arrays.copyOf(candOffset, candidates * 6);
            candDistance = Arrays.copyOf(candDistance, candidates * 2);
            candFoot = Arrays.copyOf(candFoot, candidates * 8);
            candFade = Arrays.copyOf(candFade, candidates * 4);
            candFlags = Arrays.copyOf(candFlags, candidates * 2);
        }
        DrawList.candMesh[DrawList.candidates] = chunkMeshData;
        DrawList.candBodies[DrawList.candidates] = bodyCards;
        DrawList.candOffset[DrawList.candidates * 3] = f;
        DrawList.candOffset[DrawList.candidates * 3 + 1] = f2;
        DrawList.candOffset[DrawList.candidates * 3 + 2] = f3;
        DrawList.candDistance[DrawList.candidates] = f4;
        DrawList.candFlags[DrawList.candidates] = by;
        DrawList.candFade[DrawList.candidates * 2] = f8;
        DrawList.candFade[DrawList.candidates * 2 + 1] = f9;
        DrawList.candChunk[DrawList.candidates * 2] = n2;
        DrawList.candChunk[DrawList.candidates * 2 + 1] = n3;
        if (bl) {
            float f10 = -(f5 + 4.0f);
            float f11 = -(f5 - 4.0f);
            float f12 = -(f6 + 4.0f);
            float f13 = -(f6 - 4.0f);
            float f14 = (float)n * 2.4494896f - (f7 - 1.5f);
            float f15 = f14 + 2.4494896f + 2.0f;
            float f16 = Float.MAX_VALUE;
            float f17 = -3.4028235E38f;
            float f18 = Float.MAX_VALUE;
            float f19 = -3.4028235E38f;
            for (int i = 0; i < 8; ++i) {
                float f20 = (i & 1) == 0 ? f10 : f11;
                float f21 = (i & 2) == 0 ? f14 : f15;
                float f22 = (i & 4) == 0 ? f12 : f13;
                float f23 = lightView.m00() * f20 + lightView.m10() * f21 + lightView.m20() * f22;
                float f24 = lightView.m01() * f20 + lightView.m11() * f21 + lightView.m21() * f22;
                f16 = Math.min(f16, f23);
                f17 = Math.max(f17, f23);
                f18 = Math.min(f18, f24);
                f19 = Math.max(f19, f24);
            }
            DrawList.candFoot[DrawList.candidates * 4] = f16;
            DrawList.candFoot[DrawList.candidates * 4 + 1] = f17;
            DrawList.candFoot[DrawList.candidates * 4 + 2] = f18;
            DrawList.candFoot[DrawList.candidates * 4 + 3] = f19;
        }
        ++candidates;
    }

    static void commit(SceneData sceneData, boolean bl, int n) {
        int n2;
        if (bl) {
            int n3;
            Arrays.fill(mask, false);
            float f = sceneData.shadowReach + 24.0f;
            float f2 = 2.0f * f / 48.0f;
            for (n3 = 0; n3 < candidates; ++n3) {
                if ((candFlags[n3] & 1) == 0) continue;
                DrawList.stamp(n3, f, f2);
            }
            for (n3 = 0; n3 < candidates; ++n3) {
                if (!DrawList.covered(n3, f, f2)) continue;
                byte by = candFlags[n3];
                if (DrawList.overlaps(n3, WorldRenderer.cascadeRadius(0, sceneData) + 2.0f)) {
                    by = (byte)(by | 2);
                }
                if (n > 1 && DrawList.overlaps(n3, WorldRenderer.cascadeRadius(1, sceneData) + 2.0f)) {
                    by = (byte)(by | 0x10);
                }
                if (n > 2 && DrawList.overlaps(n3, WorldRenderer.cascadeRadius(2, sceneData) + 2.0f)) {
                    by = (byte)(by | 8);
                }
                DrawList.candFlags[n3] = by;
            }
        }
        for (n2 = 0; n2 < candidates; ++n2) {
            if (candFlags[n2] == 0) continue;
            handed.put(FloorPage.key(candChunk[n2 * 2], candChunk[n2 * 2 + 1], DrawList.candMesh[n2].level), candMesh[n2]);
        }
        for (n2 = 0; n2 < candidates; ++n2) {
            if (candFlags[n2] != 0) {
                DrawList.hand(sceneData, n2);
            }
            DrawList.candMesh[n2] = null;
            DrawList.candBodies[n2] = null;
        }
        handed.clear();
        sceneData.sortFrontToBack();
    }

    private static void hand(SceneData sceneData, int n) {
        byte by = candFlags[n];
        int n2 = sceneData.meshCount;
        sceneData.addMesh(candMesh[n], candOffset[n * 3], candOffset[n * 3 + 1], candOffset[n * 3 + 2], by, candDistance[n], candFade[n * 2], candFade[n * 2 + 1]);
        sceneData.meshFloor[n2] = DrawList.candMesh[n].floorPage < 0 ? null : FloorPages.page(candChunk[n * 2], candChunk[n * 2 + 1], DrawList.candMesh[n].level);
        sceneData.meshBelow[n2] = handed.get(FloorPage.key(candChunk[n * 2], candChunk[n * 2 + 1], DrawList.candMesh[n].level - 1));
        if ((by & 5) != 0) {
            Rooms.plan(sceneData, n2, candMesh[n], candChunk[n * 2], candChunk[n * 2 + 1]);
            if (candBodies[n] != null) {
                candBodies[n].into(sceneData, n2);
            }
        }
    }

    private static boolean overlaps(int n, float f) {
        return candFoot[n * 4] < f && candFoot[n * 4 + 1] > -f && candFoot[n * 4 + 2] < f && candFoot[n * 4 + 3] > -f;
    }

    private static void stamp(int n, float f, float f2) {
        int n2 = DrawList.cellOf(candFoot[n * 4], f, f2);
        int n3 = DrawList.cellOf(candFoot[n * 4 + 1], f, f2);
        int n4 = DrawList.cellOf(candFoot[n * 4 + 2], f, f2);
        int n5 = DrawList.cellOf(candFoot[n * 4 + 3], f, f2);
        for (int i = n4; i <= n5; ++i) {
            for (int j = n2; j <= n3; ++j) {
                DrawList.mask[i * 48 + j] = true;
            }
        }
    }

    private static boolean covered(int n, float f, float f2) {
        int n2 = DrawList.cellOf(candFoot[n * 4], f, f2);
        int n3 = DrawList.cellOf(candFoot[n * 4 + 1], f, f2);
        int n4 = DrawList.cellOf(candFoot[n * 4 + 2], f, f2);
        int n5 = DrawList.cellOf(candFoot[n * 4 + 3], f, f2);
        for (int i = n4; i <= n5; ++i) {
            for (int j = n2; j <= n3; ++j) {
                if (!mask[i * 48 + j]) continue;
                return true;
            }
        }
        return false;
    }

    private static int cellOf(float f, float f2, float f3) {
        return Math.max(0, Math.min(47, (int)((f + f2) / f3)));
    }

    private DrawList() {
    }

    static {
        handed = new LongMap();
    }
}

