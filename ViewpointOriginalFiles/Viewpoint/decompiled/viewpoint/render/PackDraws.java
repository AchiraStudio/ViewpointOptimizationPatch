/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL33
 *  org.lwjgl.opengl.GL43
 *  zombie.core.textures.Texture
 */
package viewpoint.render;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL43;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.Profile;
import viewpoint.render.ChunkMeshData;
import viewpoint.render.FrameStream;
import viewpoint.render.MeshArena;
import viewpoint.render.Meshes;
import viewpoint.render.PackArena;
import viewpoint.render.PackArt;
import viewpoint.render.PackModel;
import viewpoint.render.PackModels;
import viewpoint.render.SceneData;
import viewpoint.render.TextureFilter;
import zombie.core.textures.Texture;

final class PackDraws {
    private static final int COMMAND_INTS = 5;
    private static final ArrayList<Bucket> buckets = new ArrayList();
    private static final HashMap<Long, Bucket> byPages = new HashMap();
    private static int bucketCount;
    private static int runCount;
    private static int[] runs;
    private static IntBuffer upload;

    static void draw(SceneData sceneData, GlProgram glProgram, byte by, int n, boolean bl) {
        if (!glProgram.has("uModels") || PackModels.table() == 0) {
            return;
        }
        PackDraws.begin();
        for (int i = 0; i < sceneData.meshCount; ++i) {
            if (!Meshes.drawn(sceneData, i, by, n)) continue;
            PackDraws.collect(sceneData, i, bl ? sceneData.meshModelSquares[i] : -1L);
        }
        PackDraws.issue(glProgram);
    }

    static void drawTarget(SceneData sceneData, GlProgram glProgram, int n, long l) {
        if (!glProgram.has("uModels") || PackModels.table() == 0 || sceneData.meshes[n].arenaFirst < 0) {
            return;
        }
        PackDraws.begin();
        PackDraws.collect(sceneData, n, l);
        PackDraws.issue(glProgram);
    }

    private static void begin() {
        bucketCount = 0;
        runCount = 0;
        byPages.clear();
    }

    private static void issue(GlProgram glProgram) {
        int n;
        if (bucketCount == 0) {
            return;
        }
        PackDraws.room(runCount * 4);
        upload.put(runs, 0, runCount * 4).flip();
        long l = FrameStream.put(upload, 16);
        int n2 = FrameStream.buffer();
        int n3 = 0;
        for (n = 0; n < bucketCount; ++n) {
            n3 += PackDraws.buckets.get((int)n).count * 5;
        }
        PackDraws.room(n3);
        for (n = 0; n < bucketCount; ++n) {
            upload.put(PackDraws.buckets.get((int)n).commands, 0, PackDraws.buckets.get((int)n).count * 5);
        }
        long l2 = FrameStream.put(upload.flip(), 4);
        GL15.glBindBuffer((int)36671, (int)FrameStream.buffer());
        PackDraws.multiDraw(glProgram, n2, l, l2);
        GL15.glBindBuffer((int)36671, (int)0);
    }

    private static void multiDraw(GlProgram glProgram, int n, long l, long l2) {
        boolean bl = glProgram.has("uSpriteFilter");
        glProgram.setInt("uModels", 1);
        if (bl) {
            glProgram.setInt("uSpriteFilter", TextureFilter.MODELS.get());
        }
        glProgram.sampler("uPackTable", 88);
        glProgram.sampler("uMeshRecords", 89);
        glProgram.sampler("uPlantSlots", 48);
        MeshArena.bindForModels(89, 48);
        GL13.glActiveTexture((int)34072);
        GL11.glBindTexture((int)35882, (int)PackModels.table());
        GL13.glActiveTexture((int)33984);
        long l3 = l2;
        for (int i = 0; i < bucketCount; ++i) {
            Bucket bucket = buckets.get(i);
            PackArena.bind(bucket.meshPage, n, l);
            int n2 = PackArt.texture(bucket.artPage);
            GL11.glBindTexture((int)3553, (int)n2);
            Texture.lastTextureID = -1;
            GL33.glBindSampler((int)0, (int)TextureFilter.MODELS.sampler(n2));
            GL43.glMultiDrawElementsIndirect((int)Gl.triangles(), (int)5125, (long)l3, (int)bucket.count, (int)20);
            l3 += (long)bucket.count * 5L * 4L;
            ++Profile.draws;
            Profile.meshDraws += bucket.count;
        }
        GL30.glBindVertexArray((int)0);
        GL33.glBindSampler((int)0, (int)0);
        GL13.glActiveTexture((int)34072);
        GL11.glBindTexture((int)35882, (int)0);
        GL13.glActiveTexture((int)33984);
        MeshArena.unbindForModels(89, 48);
        glProgram.setInt("uModels", 0);
        if (bl) {
            TextureFilter.SPRITES.apply(glProgram);
        }
    }

    private static void collect(SceneData sceneData, int n, long l) {
        ChunkMeshData chunkMeshData = sceneData.meshes[n];
        if (chunkMeshData.modelRuns == null || l == 0L) {
            return;
        }
        for (int i = 0; i < chunkMeshData.modelRuns.length; i += 3) {
            PackModel packModel = PackModels.get(chunkMeshData.modelRuns[i]);
            if (packModel == null || packModel.state() != 3) continue;
            int n2 = chunkMeshData.arenaFirst + chunkMeshData.modelBase + chunkMeshData.modelRuns[i + 1];
            PackDraws.bucket(packModel).add(packModel, chunkMeshData.modelRuns[i + 2], PackDraws.run(n, n2, (int)l, (int)(l >>> 32)));
        }
    }

    private static int run(int n, int n2, int n3, int n4) {
        if ((runCount + 1) * 4 > runs.length) {
            runs = Arrays.copyOf(runs, runs.length * 2);
        }
        int n5 = runCount * 4;
        PackDraws.runs[n5] = n;
        PackDraws.runs[n5 + 1] = n2;
        PackDraws.runs[n5 + 2] = n3;
        PackDraws.runs[n5 + 3] = n4;
        return runCount++;
    }

    private static Bucket bucket(PackModel packModel) {
        long l = (long)packModel.meshPage << 32 | (long)packModel.artPage;
        Bucket bucket = byPages.get(l);
        if (bucket == null) {
            if (bucketCount == buckets.size()) {
                buckets.add(new Bucket());
            }
            bucket = buckets.get(bucketCount++);
            bucket.meshPage = packModel.meshPage;
            bucket.artPage = packModel.artPage;
            bucket.count = 0;
            byPages.put(l, bucket);
        }
        return bucket;
    }

    private static void room(int n) {
        if (upload.capacity() < n) {
            upload = BufferUtils.createIntBuffer((int)(n * 2));
        }
        upload.clear();
    }

    private PackDraws() {
    }

    static {
        runs = new int[1024];
        upload = BufferUtils.createIntBuffer((int)4096);
    }

    private static final class Bucket {
        int meshPage;
        int artPage;
        int count;
        int[] commands = new int[320];

        private Bucket() {
        }

        void add(PackModel packModel, int n, int n2) {
            if ((this.count + 1) * 5 > this.commands.length) {
                this.commands = Arrays.copyOf(this.commands, this.commands.length * 2);
            }
            int n3 = this.count * 5;
            this.commands[n3] = packModel.indices;
            this.commands[n3 + 1] = n;
            this.commands[n3 + 2] = packModel.firstIndex;
            this.commands[n3 + 3] = packModel.firstVertex;
            this.commands[n3 + 4] = n2;
            ++this.count;
        }
    }
}

