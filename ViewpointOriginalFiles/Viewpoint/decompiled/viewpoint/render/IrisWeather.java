/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Random;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import viewpoint.render.SceneData;

final class IrisWeather {
    private static final int RADIUS = 6;
    private static final int FLOATS = 12;
    private static final int SIDE = 13;
    private static final float BELOW = 6.0f;
    private static final float ABOVE = 10.0f;
    private static final float RAIN_FALL = 2.2f;
    private static final float SNOW_FALL = 0.25f;
    private final FloatBuffer data = BufferUtils.createFloatBuffer((int)24336);
    private int vao;
    private int vbo;
    private int rainTexture;
    private int snowTexture;
    private int rainCount;
    private int snowCount;

    IrisWeather() {
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
        GL13.glActiveTexture((int)34079);
        this.rainTexture = IrisWeather.streaks(false);
        this.snowTexture = IrisWeather.streaks(true);
        GL11.glBindTexture((int)3553, (int)0);
        GL13.glActiveTexture((int)33984);
    }

    boolean update(SceneData sceneData, float f, float f2, double d) {
        this.snowCount = 0;
        this.rainCount = 0;
        if (sceneData.rain <= 0.0f && sceneData.snow <= 0.0f) {
            return false;
        }
        this.data.clear();
        this.rainCount = this.columns(sceneData.rain, f, f2, d * (double)2.2f, false);
        this.snowCount = this.columns(sceneData.snow, f, f2, d * 0.25, true);
        this.data.flip();
        GL15.glBindBuffer((int)34962, (int)this.vbo);
        GL15.glBufferSubData((int)34962, (long)0L, (FloatBuffer)this.data);
        GL15.glBindBuffer((int)34962, (int)0);
        return this.rainCount + this.snowCount > 0;
    }

    private int columns(float f, float f2, float f3, double d, boolean bl) {
        if (f <= 0.0f) {
            return 0;
        }
        int n = this.data.position();
        int n2 = (int)Math.floor(-f2);
        int n3 = (int)Math.floor(-f3);
        float f4 = -f2 - (float)n2;
        float f5 = -f3 - (float)n3;
        for (int i = -6; i <= 6; ++i) {
            for (int j = -6; j <= 6; ++j) {
                float f6 = (float)j + 0.5f - f4;
                float f7 = (float)i + 0.5f - f5;
                float f8 = (float)Math.sqrt(f6 * f6 + f7 * f7);
                if (f8 > 6.0f || f8 < 0.01f) continue;
                long l = (long)(n2 + j) * 3121L + (long)(n3 + i) * 45238971L;
                float f9 = (float)((l * 418711L ^ l >> 7) & 0x3FFL) / 1024.0f;
                float f10 = f * ((1.0f - f8 * f8 / 36.0f) * 0.5f + 0.5f);
                this.quad(f6, f7, f8, (float)(d * (bl ? 1.0 : 1.0 + (double)f9)) + f9, f10, bl ? f9 : 0.0f);
            }
        }
        return (this.data.position() - n) / 12;
    }

    private void quad(float f, float f2, float f3, float f4, float f5, float f6) {
        int[] nArray;
        float f7 = f2 / f3 * 0.5f;
        float f8 = -f / f3 * 0.5f;
        float f9 = f4;
        float f10 = f4 + 4.0f;
        float[][] fArrayArray = new float[][]{{-1.0f, -6.0f, 0.0f, f10}, {1.0f, -6.0f, 1.0f, f10}, {1.0f, 10.0f, 1.0f, f9}, {-1.0f, 10.0f, 0.0f, f9}};
        for (int n : nArray = new int[]{0, 1, 2, 0, 2, 3}) {
            float[] fArray = fArrayArray[n];
            this.data.put(f + f7 * fArray[0]).put(fArray[1]).put(f2 + f8 * fArray[0]);
            this.data.put(1.0f).put(1.0f).put(1.0f).put(f5);
            this.data.put(fArray[2] + f6).put(fArray[3]);
            this.data.put(f).put(0.0f).put(f2);
        }
    }

    void draw() {
        GL30.glBindVertexArray((int)this.vao);
        GL13.glActiveTexture((int)33984);
        if (this.rainCount > 0) {
            GL11.glBindTexture((int)3553, (int)this.rainTexture);
            GL11.glDrawArrays((int)4, (int)0, (int)this.rainCount);
        }
        if (this.snowCount > 0) {
            GL11.glBindTexture((int)3553, (int)this.snowTexture);
            GL11.glDrawArrays((int)4, (int)this.rainCount, (int)this.snowCount);
        }
        GL30.glBindVertexArray((int)0);
    }

    private static int streaks(boolean bl) {
        int n = 64;
        int n2 = 256;
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)(n * n2 * 4));
        Random random = new Random(bl ? 11L : 7L);
        byte[] byArray = new byte[n * n2];
        for (int i = 0; i < (bl ? 180 : 90); ++i) {
            int n3 = random.nextInt(n);
            int n4 = random.nextInt(n2);
            int n5 = bl ? 2 : 6 + random.nextInt(10);
            for (int j = 0; j < n5; ++j) {
                byArray[(n4 + j) % n2 * n + n3] = (byte)(bl ? 230 : 110 + random.nextInt(80));
                if (!bl) continue;
                byArray[(n4 + j) % n2 * n + (n3 + 1) % n] = -56;
            }
        }
        for (byte by : byArray) {
            byteBuffer.put((byte)-1).put((byte)-1).put((byte)-1).put(by);
        }
        byteBuffer.flip();
        int n6 = GL11.glGenTextures();
        GL11.glBindTexture((int)3553, (int)n6);
        GL11.glTexImage2D((int)3553, (int)0, (int)32856, (int)n, (int)n2, (int)0, (int)6408, (int)5121, (ByteBuffer)byteBuffer);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
        return n6;
    }

    void release() {
        GL30.glDeleteVertexArrays((int)this.vao);
        GL15.glDeleteBuffers((int)this.vbo);
        GL11.glDeleteTextures((int)this.rainTexture);
        GL11.glDeleteTextures((int)this.snowTexture);
    }
}

