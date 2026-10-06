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

final class IrisSky {
    private static final int FLOATS = 12;
    private static final int DOME_RINGS = 16;
    private static final int DOME_SEGMENTS = 32;
    private static final int STARS = 1500;
    private int vao;
    private int vbo;
    private int domeCount;
    private int starFirst;
    private int starCount;
    private int sunFirst;
    private int sunTexture;
    private int moonTexture;

    IrisSky() {
    }

    void init() {
        int n;
        GL13.glActiveTexture((int)34079);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer((int)145008);
        IrisSky.dome(floatBuffer);
        this.starFirst = this.domeCount = floatBuffer.position() / 12;
        IrisSky.stars(floatBuffer);
        this.starCount = floatBuffer.position() / 12 - this.starFirst;
        this.sunFirst = floatBuffer.position() / 12;
        for (n = 0; n < 144; ++n) {
            floatBuffer.put(0.0f);
        }
        floatBuffer.flip();
        this.vao = GL30.glGenVertexArrays();
        this.vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray((int)this.vao);
        GL15.glBindBuffer((int)34962, (int)this.vbo);
        GL15.glBufferData((int)34962, (FloatBuffer)floatBuffer, (int)35048);
        n = 48;
        GL20.glVertexAttribPointer((int)0, (int)3, (int)5126, (boolean)false, (int)n, (long)0L);
        GL20.glVertexAttribPointer((int)1, (int)4, (int)5126, (boolean)false, (int)n, (long)12L);
        GL20.glVertexAttribPointer((int)2, (int)2, (int)5126, (boolean)false, (int)n, (long)28L);
        GL20.glVertexAttribPointer((int)3, (int)3, (int)5126, (boolean)false, (int)n, (long)36L);
        for (int i = 0; i < 4; ++i) {
            GL20.glEnableVertexAttribArray((int)i);
        }
        GL30.glBindVertexArray((int)0);
        GL15.glBindBuffer((int)34962, (int)0);
        this.sunTexture = IrisSky.disc(1.0f, 0.95f, 0.85f, false);
        this.moonTexture = IrisSky.disc(0.85f, 0.88f, 0.95f, true);
        GL13.glActiveTexture((int)33984);
    }

    private static void dome(FloatBuffer floatBuffer) {
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 32; ++j) {
                float[][] fArrayArray;
                for (float[] fArray : fArrayArray = new float[][]{{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j}, {i + 1, j + 1}, {i, j + 1}}) {
                    double d = Math.PI * ((double)(fArray[0] / 16.0f) - 0.5);
                    double d2 = Math.PI * 2 * (double)fArray[1] / 32.0;
                    float f = (float)(Math.cos(d) * Math.cos(d2));
                    float f2 = (float)Math.sin(d);
                    float f3 = (float)(Math.cos(d) * Math.sin(d2));
                    IrisSky.vertex(floatBuffer, f * 100.0f, f2 * 100.0f, f3 * 100.0f, 1.0f, 1.0f, 1.0f, 1.0f, fArray[1] / 32.0f, fArray[0] / 16.0f, -f, -f2, -f3);
                }
            }
        }
    }

    private static void stars(FloatBuffer floatBuffer) {
        Random random = new Random(10842L);
        for (int i = 0; i < 1500; ++i) {
            float[][] fArrayArray;
            double d = random.nextFloat() * 2.0f - 1.0f;
            double d2 = random.nextFloat() * 2.0f - 1.0f;
            double d3 = random.nextFloat() * 2.0f - 1.0f;
            double d4 = 0.15f + random.nextFloat() * 0.1f;
            double d5 = d * d + d2 * d2 + d3 * d3;
            if (d5 >= 1.0 || d5 <= 0.01) continue;
            d5 = 1.0 / Math.sqrt(d5);
            double[] dArray = IrisSky.perpendicular(d *= d5, d2 *= d5, d3 *= d5);
            double[] dArray2 = IrisSky.cross(d, d2, d3, dArray);
            float f = 0.5f + random.nextFloat() * 0.5f;
            for (float[] fArray : fArrayArray = new float[][]{{-1.0f, -1.0f}, {1.0f, -1.0f}, {1.0f, 1.0f}, {-1.0f, -1.0f}, {1.0f, 1.0f}, {-1.0f, 1.0f}}) {
                double d6 = d * 100.0 + (dArray[0] * (double)fArray[0] + dArray2[0] * (double)fArray[1]) * d4 * 2.0;
                double d7 = d2 * 100.0 + (dArray[1] * (double)fArray[0] + dArray2[1] * (double)fArray[1]) * d4 * 2.0;
                double d8 = d3 * 100.0 + (dArray[2] * (double)fArray[0] + dArray2[2] * (double)fArray[1]) * d4 * 2.0;
                IrisSky.vertex(floatBuffer, (float)d6, (float)d7, (float)d8, f, f, f, f, 0.5f + fArray[0] * 0.5f, 0.5f + fArray[1] * 0.5f, (float)(-d), (float)(-d2), (float)(-d3));
            }
        }
    }

    void update(double[] dArray, double[] dArray2) {
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer((int)144);
        IrisSky.quad(floatBuffer, dArray, 30.0);
        IrisSky.quad(floatBuffer, dArray2, 20.0);
        floatBuffer.flip();
        GL15.glBindBuffer((int)34962, (int)this.vbo);
        GL15.glBufferSubData((int)34962, (long)((long)this.sunFirst * 12L * 4L), (FloatBuffer)floatBuffer);
        GL15.glBindBuffer((int)34962, (int)0);
    }

    private static void quad(FloatBuffer floatBuffer, double[] dArray, double d) {
        float[][] fArrayArray;
        double[] dArray2 = IrisSky.perpendicular(dArray[0], dArray[1], dArray[2]);
        double[] dArray3 = IrisSky.cross(dArray[0], dArray[1], dArray[2], dArray2);
        for (float[] fArray : fArrayArray = new float[][]{{-1.0f, -1.0f}, {1.0f, -1.0f}, {1.0f, 1.0f}, {-1.0f, -1.0f}, {1.0f, 1.0f}, {-1.0f, 1.0f}}) {
            double d2 = d * 0.5;
            IrisSky.vertex(floatBuffer, (float)(dArray[0] * 100.0 + (dArray2[0] * (double)fArray[0] + dArray3[0] * (double)fArray[1]) * d2), (float)(dArray[1] * 100.0 + (dArray2[1] * (double)fArray[0] + dArray3[1] * (double)fArray[1]) * d2), (float)(dArray[2] * 100.0 + (dArray2[2] * (double)fArray[0] + dArray3[2] * (double)fArray[1]) * d2), 1.0f, 1.0f, 1.0f, 1.0f, 0.5f + fArray[0] * 0.5f, 0.5f + fArray[1] * 0.5f, (float)(-dArray[0]), (float)(-dArray[1]), (float)(-dArray[2]));
        }
    }

    void bind() {
        GL30.glBindVertexArray((int)this.vao);
    }

    void drawDome() {
        GL11.glDrawArrays((int)4, (int)0, (int)this.domeCount);
    }

    void drawStars() {
        GL11.glDrawArrays((int)4, (int)this.starFirst, (int)this.starCount);
    }

    void drawSun() {
        GL11.glBindTexture((int)3553, (int)this.sunTexture);
        GL11.glDrawArrays((int)4, (int)this.sunFirst, (int)6);
    }

    void drawMoon() {
        GL11.glBindTexture((int)3553, (int)this.moonTexture);
        GL11.glDrawArrays((int)4, (int)(this.sunFirst + 6), (int)6);
    }

    void unbind() {
        GL30.glBindVertexArray((int)0);
    }

    private static void vertex(FloatBuffer floatBuffer, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12) {
        floatBuffer.put(f).put(f2).put(f3).put(f4).put(f5).put(f6).put(f7).put(f8).put(f9).put(f10).put(f11).put(f12);
    }

    private static double[] perpendicular(double d, double d2, double d3) {
        double[] dArray = Math.abs(d2) < 0.9 ? IrisSky.cross(d, d2, d3, new double[]{0.0, 1.0, 0.0}) : IrisSky.cross(d, d2, d3, new double[]{1.0, 0.0, 0.0});
        double d4 = Math.sqrt(dArray[0] * dArray[0] + dArray[1] * dArray[1] + dArray[2] * dArray[2]);
        return new double[]{dArray[0] / d4, dArray[1] / d4, dArray[2] / d4};
    }

    private static double[] cross(double d, double d2, double d3, double[] dArray) {
        return new double[]{d2 * dArray[2] - d3 * dArray[1], d3 * dArray[0] - d * dArray[2], d * dArray[1] - d2 * dArray[0]};
    }

    private static int disc(float f, float f2, float f3, boolean bl) {
        int n;
        int n2 = 64;
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)(n2 * n2 * 4));
        Random random = new Random(3L);
        for (n = 0; n < n2; ++n) {
            for (int i = 0; i < n2; ++i) {
                double d = ((double)i + 0.5) / (double)n2 * 2.0 - 1.0;
                double d2 = ((double)n + 0.5) / (double)n2 * 2.0 - 1.0;
                double d3 = Math.sqrt(d * d + d2 * d2);
                float f4 = (float)Math.max(0.0, Math.min(1.0, (0.62 - d3) * 20.0));
                float f5 = bl ? 0.8f + 0.2f * random.nextFloat() : 1.0f;
                byteBuffer.put((byte)(255.0f * f * f5)).put((byte)(255.0f * f2 * f5)).put((byte)(255.0f * f3 * f5)).put((byte)(255.0f * f4));
            }
        }
        byteBuffer.flip();
        n = GL11.glGenTextures();
        GL11.glBindTexture((int)3553, (int)n);
        GL11.glTexImage2D((int)3553, (int)0, (int)32856, (int)n2, (int)n2, (int)0, (int)6408, (int)5121, (ByteBuffer)byteBuffer);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        GL11.glBindTexture((int)3553, (int)0);
        return n;
    }

    void release() {
        GL30.glDeleteVertexArrays((int)this.vao);
        GL15.glDeleteBuffers((int)this.vbo);
        GL11.glDeleteTextures((int)this.sunTexture);
        GL11.glDeleteTextures((int)this.moonTexture);
    }
}

