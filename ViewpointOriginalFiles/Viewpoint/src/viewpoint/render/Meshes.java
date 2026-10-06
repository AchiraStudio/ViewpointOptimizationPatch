/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL33
 *  zombie.core.textures.Texture
 *  zombie.core.textures.TextureID
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL33;
import viewpoint.platform.GlProgram;
import viewpoint.render.ChunkMeshData;
import viewpoint.render.CorpseCards;
import viewpoint.render.FloorBakes;
import viewpoint.render.FloorFilter;
import viewpoint.render.LodView;
import viewpoint.render.MeshArena;
import viewpoint.render.PackDraws;
import viewpoint.render.SceneData;
import viewpoint.render.TextureFilter;
import viewpoint.render.Wireframe;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;

public final class Meshes {
    private static final float[] BANDS = new float[]{12.0f, 32.0f, Float.MAX_VALUE};
    static FloorBakes floors;
    private static final ArrayList<Bucket> buckets;
    private static final IdentityHashMap<Object, Bucket> byPage;
    private static final Object NO_PAGE;
    private static int bucketCount;
    private static TextureID[] segmentPages;
    private static int[] segmentFirst;
    private static int[] segmentLength;
    private static int[] segmentFloor;
    private static int segmentCount;
    private static int plantSegments;

    static void init() {
        MeshArena.init();
    }

    static void prepare(SceneData sceneData) {
        int n;
        FloatBuffer floatBuffer = MeshArena.records(sceneData.meshCount);
        for (n = 0; n < sceneData.meshCount; ++n) {
            sceneData.meshes[n].prepare();
        }
        for (n = 0; n < sceneData.meshCount; ++n) {
            ChunkMeshData chunkMeshData = sceneData.meshes[n];
            ChunkMeshData chunkMeshData2 = sceneData.meshBelow[n];
            floatBuffer.put(sceneData.meshOffsets[n * 3]).put(sceneData.meshOffsets[n * 3 + 1]).put(sceneData.meshOffsets[n * 3 + 2]).put((float)chunkMeshData.level * 2.4494896f).put(sceneData.meshFade[n * 2]).put(sceneData.meshFade[n * 2 + 1]).put(chunkMeshData.lightSlot).put(Meshes.floorSlice(n) < 0 ? 0.0f : FloorBakes.recordLayer(Meshes.floorSlice(n), floors.meshShare(n))).put(chunkMeshData2 != null && chunkMeshData2.lightSlot >= 0 ? (float)chunkMeshData2.lightSlot : (float)chunkMeshData.lightSlot).put(0.0f).put(0.0f).put(0.0f);
        }
        MeshArena.uploadRecords();
    }

    static void draw(SceneData sceneData, GlProgram glProgram, byte by) {
        Meshes.draw(sceneData, glProgram, by, Integer.MAX_VALUE, false, true);
    }

    static void drawPlanned(SceneData sceneData, GlProgram glProgram, byte by, boolean bl) {
        Meshes.draw(sceneData, glProgram, by, Integer.MAX_VALUE, true, bl);
    }

    static void draw(SceneData sceneData, GlProgram glProgram, byte by, int n, boolean bl, boolean bl2) {
        int n2;
        int n3 = 0;
        for (int i = 0; i < sceneData.meshCount; ++i) {
            if (!Meshes.drawn(sceneData, i, by, n)) continue;
            ChunkMeshData chunkMeshData = sceneData.meshes[i];
            n3 += bl && sceneData.meshPlan[i] >= 0 ? sceneData.meshPlanLength[i] : chunkMeshData.pages.length;
            n3 += bl2 && chunkMeshData.plantFirst != null ? chunkMeshData.pages.length : 0;
            n3 += chunkMeshData.hasModels() ? 1 : 0;
        }
        if (n3 == 0) {
            return;
        }
        IntBuffer intBuffer = MeshArena.commands(n3);
        segmentCount = 0;
        if (meshDist.length < sceneData.meshCount) {
            meshDist = new float[Math.max(meshDist.length * 2, sceneData.meshCount)];
        }
        for (int i = 0; i < sceneData.meshCount; ++i) {
            if (Meshes.drawn(sceneData, i, by, n)) {
                float f4 = sceneData.meshOffsets[i * 3] - 4.0f;
                float f3 = sceneData.meshOffsets[i * 3 + 2] - 4.0f;
                meshDist[i] = (float)Math.sqrt(f4 * f4 + f3 * f3);
            }
        }
        for (n2 = 0; n2 < BANDS.length; ++n2) {
            float f = n2 == 0 ? -1.0f : BANDS[n2 - 1];
            float f2 = BANDS[n2];
            bucketCount = 0;
            byPage.clear();
            for (int i = 0; i < sceneData.meshCount; ++i) {
                if (!Meshes.drawn(sceneData, i, by, n)) continue;
                float f5 = meshDist[i];
                if (f5 <= f || f5 > f2) continue;
                Meshes.collect(sceneData, i, bl);
            }
            Meshes.flushBuckets(intBuffer);
        }
        plantSegments = segmentCount;
        if (bl2) {
            Meshes.collectPlants(sceneData, by, n);
            Meshes.flushBuckets(intBuffer);
        }
        MeshArena.beginDraws();
        n2 = glProgram.has("uLight") ? 1 : 0;
        if (n2 != 0) {
            MeshArena.bindLight(5, glProgram);
        }
        Meshes.issue(glProgram);
        MeshArena.endDraws();
        PackDraws.draw(sceneData, glProgram, by, n, bl);
        CorpseCards.draw(sceneData, glProgram, by, n, bl);
        if (n2 != 0) {
            MeshArena.unbindLight(5);
        }
        GL33.glBindSampler((int)0, (int)0);
        Arrays.fill(segmentPages, 0, segmentCount, null);
    }

    private static void flushBuckets(IntBuffer intBuffer) {
        for (int i = 0; i < bucketCount; ++i) {
            Bucket bucket = buckets.get(i);
            Meshes.segment(bucket.page, bucket.floorArray, intBuffer.position() / 4, bucket.count);
            intBuffer.put(bucket.commands, 0, bucket.count * 4);
        }
    }

    private static void collectPlants(SceneData sceneData, byte by, int n) {
        bucketCount = 0;
        byPage.clear();
        for (int i = 0; i < sceneData.meshCount; ++i) {
            ChunkMeshData chunkMeshData = sceneData.meshes[i];
            if (!Meshes.drawn(sceneData, i, by, n) || chunkMeshData.plantFirst == null) continue;
            for (int j = 0; j < chunkMeshData.pages.length; ++j) {
                if (chunkMeshData.plantCount[j] <= 0) continue;
                int n2 = chunkMeshData.arenaFirst + chunkMeshData.plantBase + chunkMeshData.plantFirst[j];
                Meshes.bucket(chunkMeshData.pages[j], chunkMeshData.pages[j], 0).add(chunkMeshData.plantCount[j] * 6, n2 * 6, i);
            }
        }
    }

    private static void issue(GlProgram glProgram) {
        Wireframe.apply(glProgram);
        LodView.apply(glProgram);
        TextureFilter.SPRITES.apply(glProgram);
        if (glProgram.has("uFloorPages")) {
            glProgram.sampler("uFloorPages", 41);
            FloorFilter.apply(glProgram);
        }
        TextureID textureID = null;
        int n = 0;
        for (int i = 0; i < segmentCount; ++i) {
            TextureID textureID2 = segmentPages[i];
            if (i == plantSegments) {
                MeshArena.beginPlants(48);
                glProgram.sampler("uPlantSlots", 48);
                glProgram.setInt("uPlants", 1);
            }
            if (segmentFloor[i] != 0 && segmentFloor[i] != n) {
                n = segmentFloor[i];
                Meshes.bindFloorArray(n);
            } else if (textureID2 != null && textureID2 != textureID) {
                textureID = textureID2;
                Meshes.bindPage(textureID2);
            }
            MeshArena.multiDraw(segmentFirst[i], segmentLength[i]);
        }
        if (n != 0) {
            Meshes.bindFloorArray(0);
        }
        if (plantSegments < segmentCount) {
            glProgram.setInt("uPlants", 0);
            MeshArena.endPlants(48);
        }
    }

    private static void collect(SceneData sceneData, int n, boolean bl) {
        ChunkMeshData chunkMeshData = sceneData.meshes[n];
        if (bl && sceneData.meshPlan[n] >= 0) {
            int[] nArray = sceneData.plan;
            int n2 = -1;
            Bucket bucket = null;
            for (int i = sceneData.meshPlan[n]; i < sceneData.meshPlan[n] + sceneData.meshPlanLength[n]; ++i) {
                if (nArray[i * 3] != n2) {
                    n2 = nArray[i * 3];
                    bucket = Meshes.bucket(chunkMeshData, n, n2);
                }
                if (bucket == null) continue;
                bucket.add(nArray[i * 3 + 2], chunkMeshData.arenaFirst + nArray[i * 3 + 1], n);
            }
            return;
        }
        for (int i = 0; i < chunkMeshData.pages.length; ++i) {
            Bucket bucket = Meshes.bucket(chunkMeshData, n, i);
            if (bucket == null) continue;
            bucket.add(chunkMeshData.count[i], chunkMeshData.arenaFirst + chunkMeshData.first[i], n);
        }
    }

    private static Bucket bucket(ChunkMeshData chunkMeshData, int n, int n2) {
        if (n2 != chunkMeshData.floorPage) {
            return Meshes.bucket(chunkMeshData.pages[n2] == null ? NO_PAGE : chunkMeshData.pages[n2], chunkMeshData.pages[n2], 0);
        }
        int n3 = Meshes.floorSlice(n);
        return n3 < 0 ? null : Meshes.bucket(floors.array(n3), null, floors.texture(n3));
    }

    private static int floorSlice(int n) {
        return floors == null ? -1 : floors.meshSlice(n);
    }

    static void bindFloorArray(int n) {
        GL13.glActiveTexture((int)34025);
        GL11.glBindTexture((int)35866, (int)n);
        FloorFilter.bind(n);
        GL13.glActiveTexture((int)33984);
    }

    public static void bindPage(TextureID textureID) {
        if (textureID.getID() == -1) {
            Texture.lastTextureID = -1;
            textureID.bind();
        } else {
            GL11.glBindTexture((int)3553, (int)textureID.getID());
        }
        Texture.lastTextureID = -1;
        GL33.glBindSampler((int)0, (int)TextureFilter.SPRITES.sampler(textureID.getID()));
    }

    private static Bucket bucket(Object object, TextureID textureID, int n) {
        Bucket bucket = byPage.get(object);
        if (bucket == null) {
            if (bucketCount == buckets.size()) {
                buckets.add(new Bucket());
            }
            bucket = buckets.get(bucketCount++);
            bucket.page = textureID;
            bucket.floorArray = n;
            bucket.count = 0;
            byPage.put(object, bucket);
        }
        return bucket;
    }

    static boolean drawn(SceneData sceneData, int n, byte by, int n2) {
        ChunkMeshData chunkMeshData = sceneData.meshes[n];
        return (sceneData.meshFlags[n] & by) != 0 && chunkMeshData.level <= n2 && chunkMeshData.arenaFirst >= 0;
    }

    private static void segment(TextureID textureID, int n, int n2, int n3) {
        if (segmentCount == segmentPages.length) {
            segmentPages = Arrays.copyOf(segmentPages, segmentCount * 2);
            segmentFirst = Arrays.copyOf(segmentFirst, segmentCount * 2);
            segmentLength = Arrays.copyOf(segmentLength, segmentCount * 2);
            segmentFloor = Arrays.copyOf(segmentFloor, segmentCount * 2);
        }
        Meshes.segmentPages[Meshes.segmentCount] = textureID;
        Meshes.segmentFloor[Meshes.segmentCount] = n;
        Meshes.segmentFirst[Meshes.segmentCount] = n2;
        Meshes.segmentLength[Meshes.segmentCount] = n3;
        ++segmentCount;
    }

    private Meshes() {
    }

    private static float[] meshDist = new float[1024];

    static {
        buckets = new ArrayList();
        byPage = new IdentityHashMap();
        NO_PAGE = new Object();
        segmentPages = new TextureID[256];
        segmentFirst = new int[256];
        segmentLength = new int[256];
        segmentFloor = new int[256];
    }

    private static final class Bucket {
        TextureID page;
        int floorArray;
        int[] commands = new int[256];
        int count;

        private Bucket() {
        }

        void add(int n, int n2, int n3) {
            if (this.count * 4 == this.commands.length) {
                this.commands = Arrays.copyOf(this.commands, this.commands.length * 2);
            }
            int n4 = this.count * 4;
            this.commands[n4] = n;
            this.commands[n4 + 1] = 1;
            this.commands[n4 + 2] = n2;
            this.commands[n4 + 3] = n3;
            ++this.count;
        }
    }
}
