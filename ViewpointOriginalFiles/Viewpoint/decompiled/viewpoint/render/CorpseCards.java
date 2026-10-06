/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL33
 *  org.lwjgl.opengl.GL43
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL43;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.render.FrameStream;
import viewpoint.render.MeshArena;
import viewpoint.render.Meshes;
import viewpoint.render.SceneData;
import zombie.core.textures.TextureID;

public final class CorpseCards {
    private static final byte VIEW = 5;
    private static final int CARD_VERTICES = 6;
    private static final int[] CORNERS = new int[]{0, 1, 2, 0, 2, 3};
    private static final ArrayList<Bucket> buckets = new ArrayList();
    private static final IdentityHashMap<Object, Bucket> byPage = new IdentityHashMap();
    private static final Object NO_PAGE = new Object();
    private static int bucketCount;
    private static int texture;
    private static FloatBuffer upload;

    public static float[] baked(float[] fArray) {
        float[] fArray2 = new float[84];
        int n = 0;
        for (int n2 : CORNERS) {
            int n3 = n2 * 4;
            fArray2[n++] = fArray[n3];
            fArray2[n++] = fArray[16];
            fArray2[n++] = fArray[n3 + 1];
            fArray2[n++] = fArray[n3 + 2];
            fArray2[n++] = fArray[n3 + 3];
            System.arraycopy(fArray, 20, fArray2, n, 4);
            n += 4;
            fArray2[n++] = 0.0f;
            fArray2[n++] = 1.0f;
            fArray2[n++] = 0.0f;
            fArray2[n++] = 0.0f;
            fArray2[n++] = fArray[17];
        }
        return fArray2;
    }

    static void draw(SceneData sceneData, GlProgram glProgram, byte by, int n, boolean bl) {
        if (!CorpseCards.builds(glProgram) || (by & 5) == 0 || sceneData.cardCount == 0) {
            return;
        }
        CorpseCards.begin();
        for (int i = 0; i < sceneData.meshCount; ++i) {
            if (!Meshes.drawn(sceneData, i, by, n)) continue;
            CorpseCards.collect(sceneData, i, bl ? sceneData.meshModelSquares[i] : -1L, false);
        }
        CorpseCards.issue(sceneData, glProgram);
    }

    static void drawTarget(SceneData sceneData, GlProgram glProgram, int n, long l) {
        if (!CorpseCards.builds(glProgram) || sceneData.meshCardCount[n] == 0) {
            return;
        }
        CorpseCards.begin();
        CorpseCards.collect(sceneData, n, l, true);
        CorpseCards.issue(sceneData, glProgram);
    }

    private static boolean builds(GlProgram glProgram) {
        if (!glProgram.has("uCards")) {
            return false;
        }
        glProgram.sampler("uCardTexels", 90);
        return true;
    }

    private static void begin() {
        bucketCount = 0;
        byPage.clear();
    }

    private static void collect(SceneData sceneData, int n, long l, boolean bl) {
        for (int i = sceneData.meshCardFirst[n]; i < sceneData.meshCardFirst[n] + sceneData.meshCardCount[n]; ++i) {
            boolean bl2;
            int n2 = i * 28;
            int n3 = Float.floatToRawIntBits(sceneData.cards[n2 + 19]);
            boolean bl3 = bl2 = sceneData.cards[n2 + 24] >= 1.0f;
            if ((l >>> n3 & 1L) == 0L || bl && bl2) continue;
            CorpseCards.bucket(sceneData.cardPages[i]).add(i, n);
        }
    }

    private static void issue(SceneData sceneData, GlProgram glProgram) {
        int n = 0;
        for (int i = 0; i < bucketCount; ++i) {
            n += CorpseCards.buckets.get((int)i).count;
        }
        if (n == 0) {
            return;
        }
        CorpseCards.fill(sceneData.cards, n);
        long l = FrameStream.put(upload, FrameStream.textureAlignment());
        if (texture == 0) {
            texture = GL11.glGenTextures();
        }
        GL13.glActiveTexture((int)34074);
        GL11.glBindTexture((int)35882, (int)texture);
        GL43.glTexBufferRange((int)35882, (int)34836, (int)FrameStream.buffer(), (long)l, (long)((long)n * 28L * 4L));
        GL13.glActiveTexture((int)33984);
        glProgram.sampler("uMeshRecords", 89);
        MeshArena.bindForModels(89, 48);
        glProgram.setInt("uCards", 1);
        int n2 = 0;
        for (int i = 0; i < bucketCount; ++i) {
            Bucket bucket = buckets.get(i);
            if (bucket.page != null) {
                Meshes.bindPage(bucket.page);
            }
            Gl.generated(n2 * 6, bucket.count * 6);
            n2 += bucket.count;
        }
        glProgram.setInt("uCards", 0);
        MeshArena.unbindForModels(89, 48);
        GL13.glActiveTexture((int)34074);
        GL11.glBindTexture((int)35882, (int)0);
        GL13.glActiveTexture((int)33984);
        GL33.glBindSampler((int)0, (int)0);
    }

    private static void fill(float[] fArray, int n) {
        if (upload.capacity() < n * 28) {
            upload = BufferUtils.createFloatBuffer((int)(n * 28 * 2));
        }
        upload.clear();
        for (int i = 0; i < bucketCount; ++i) {
            Bucket bucket = buckets.get(i);
            for (int j = 0; j < bucket.count; ++j) {
                int n2 = upload.position();
                upload.put(fArray, bucket.cards[j * 2] * 28, 28);
                upload.put(n2 + 18, Float.intBitsToFloat(bucket.cards[j * 2 + 1]));
            }
        }
        upload.flip();
    }

    private static Bucket bucket(TextureID textureID) {
        Object object = textureID == null ? NO_PAGE : textureID;
        Bucket bucket = byPage.get(object);
        if (bucket == null) {
            if (bucketCount == buckets.size()) {
                buckets.add(new Bucket());
            }
            bucket = buckets.get(bucketCount++);
            bucket.page = textureID;
            bucket.count = 0;
            byPage.put(object, bucket);
        }
        return bucket;
    }

    private CorpseCards() {
    }

    static {
        upload = BufferUtils.createFloatBuffer((int)1792);
    }

    private static final class Bucket {
        TextureID page;
        int[] cards = new int[128];
        int count;

        private Bucket() {
        }

        void add(int n, int n2) {
            if (this.count * 2 == this.cards.length) {
                this.cards = Arrays.copyOf(this.cards, this.cards.length * 2);
            }
            this.cards[this.count * 2] = n;
            this.cards[this.count * 2 + 1] = n2;
            ++this.count;
        }
    }
}

