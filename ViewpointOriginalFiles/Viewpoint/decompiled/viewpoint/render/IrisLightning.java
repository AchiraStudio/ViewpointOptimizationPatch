/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import java.util.Random;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import viewpoint.render.SceneData;

final class IrisLightning {
    private static final int FLOATS = 12;
    private static final int SEGMENTS = 8;
    private static final int LAYERS = 4;
    private static final int QUADS = 4;
    private static final float HEIGHT = 128.0f;
    private static final float STEP = 16.0f;
    private static final float WANDER = 4.0f;
    private static final float[] COLOUR = new float[]{0.45f, 0.45f, 0.5f, 0.3f};
    private final FloatBuffer data = BufferUtils.createFloatBuffer((int)9216);
    private int vao;
    private int vbo;
    private int count;

    IrisLightning() {
    }

    void init() {
        this.vao = GL30.glGenVertexArrays();
        this.vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray((int)this.vao);
        GL15.glBindBuffer((int)34962, (int)this.vbo);
        GL15.glBufferData((int)34962, (long)((long)this.data.capacity() * 4L), (int)35040);
        int n = 48;
        GL20.glVertexAttribPointer((int)0, (int)3, (int)5126, (boolean)false, (int)n, (long)0L);
        GL20.glVertexAttribPointer((int)1, (int)4, (int)5126, (boolean)false, (int)n, (long)12L);
        GL20.glVertexAttribPointer((int)2, (int)2, (int)5126, (boolean)false, (int)n, (long)28L);
        GL20.glVertexAttribPointer((int)3, (int)3, (int)5126, (boolean)false, (int)n, (long)36L);
        for (int i = 0; i < 4; ++i) {
            GL20.glEnableVertexAttribArray((int)i);
        }
        GL30.glBindVertexArray((int)0);
        GL15.glBindBuffer((int)34962, (int)0);
    }

    boolean update(SceneData sceneData, float f, float f2, float f3) {
        int n;
        this.count = 0;
        if (sceneData.lightning <= 0.0f) {
            return false;
        }
        float f4 = -(sceneData.lightningX - f);
        float f5 = sceneData.lightningY - f2;
        float f6 = -(sceneData.lightningZ - f3);
        Random random = new Random((long)Float.floatToIntBits(sceneData.lightningX) * 31L + (long)Float.floatToIntBits(sceneData.lightningZ));
        float[] fArray = new float[9];
        float[] fArray2 = new float[9];
        for (n = 7; n >= 0; --n) {
            fArray[n] = fArray[n + 1] + (random.nextFloat() - 0.5f) * 4.0f;
            fArray2[n] = fArray2[n + 1] + (random.nextFloat() - 0.5f) * 4.0f;
        }
        this.data.clear();
        for (n = 0; n < 4; ++n) {
            float f7 = 0.1f + (float)n * 0.1f;
            for (int i = 0; i < 8; ++i) {
                float f8 = f5 + 128.0f - (float)i * 16.0f;
                float f9 = f8 - 16.0f;
                this.column(f4 + fArray[i], f8, f6 + fArray2[i], f4 + fArray[i + 1], f9, f6 + fArray2[i + 1], f7);
            }
        }
        this.data.flip();
        this.count = this.data.remaining() / 12;
        GL15.glBindBuffer((int)34962, (int)this.vbo);
        GL15.glBufferSubData((int)34962, (long)0L, (FloatBuffer)this.data);
        GL15.glBindBuffer((int)34962, (int)0);
        return true;
    }

    private void column(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        float[][] fArrayArray = new float[][]{{-f7, -f7}, {f7, -f7}, {f7, f7}, {-f7, f7}};
        for (int i = 0; i < 4; ++i) {
            float[] fArray = fArrayArray[i];
            float[] fArray2 = fArrayArray[(i + 1) % 4];
            this.vertex(f + fArray[0], f2, f3 + fArray[1]);
            this.vertex(f + fArray2[0], f2, f3 + fArray2[1]);
            this.vertex(f4 + fArray2[0], f5, f6 + fArray2[1]);
            this.vertex(f + fArray[0], f2, f3 + fArray[1]);
            this.vertex(f4 + fArray2[0], f5, f6 + fArray2[1]);
            this.vertex(f4 + fArray[0], f5, f6 + fArray[1]);
        }
    }

    private void vertex(float f, float f2, float f3) {
        this.data.put(f).put(f2).put(f3).put(COLOUR).put(0.0f).put(0.0f).put(0.0f).put(1.0f).put(0.0f);
    }

    void draw() {
        if (this.count == 0) {
            return;
        }
        GL30.glBindVertexArray((int)this.vao);
        GL11.glDrawArrays((int)4, (int)0, (int)this.count);
        GL30.glBindVertexArray((int)0);
    }

    void release() {
        GL30.glDeleteVertexArrays((int)this.vao);
        GL15.glDeleteBuffers((int)this.vbo);
    }
}

