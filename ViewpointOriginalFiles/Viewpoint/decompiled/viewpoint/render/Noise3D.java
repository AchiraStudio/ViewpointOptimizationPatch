/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL12
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

final class Noise3D {
    static final int SIZE = 64;

    static int create() {
        float[] fArray = new float[262144];
        float f = 0.015625f;
        int n = 0;
        for (int i = 0; i < 64; ++i) {
            for (int j = 0; j < 64; ++j) {
                for (int k = 0; k < 64; ++k) {
                    float f2 = (float)k * f;
                    float f3 = (float)j * f;
                    float f4 = (float)i * f;
                    float f5 = Noise3D.perlinFbm(f2, f3, f4, 4, 3);
                    float f6 = Noise3D.worleyFbm(f2, f3, f4, 4);
                    fArray[n++] = Noise3D.remap(f5, -(1.0f - f6), 1.0f, 0.0f, 1.0f);
                }
            }
        }
        float[] fArray2 = (float[])fArray.clone();
        Arrays.sort(fArray2);
        float f7 = fArray2[(int)((double)fArray2.length * 0.03)];
        float f8 = fArray2[(int)((double)fArray2.length * 0.97)];
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)fArray.length);
        for (float f6 : fArray) {
            byteBuffer.put((byte)Math.round(Noise3D.clamp((f6 - f7) / Math.max(f8 - f7, 1.0E-4f)) * 255.0f));
        }
        byteBuffer.flip();
        int n2 = GL11.glGenTextures();
        GL11.glBindTexture((int)32879, (int)n2);
        GL11.glPixelStorei((int)3317, (int)1);
        GL12.glTexImage3D((int)32879, (int)0, (int)33321, (int)64, (int)64, (int)64, (int)0, (int)6403, (int)5121, (ByteBuffer)byteBuffer);
        GL11.glPixelStorei((int)3317, (int)4);
        GL30.glGenerateMipmap((int)32879);
        GL11.glTexParameteri((int)32879, (int)10241, (int)9987);
        GL11.glTexParameteri((int)32879, (int)10240, (int)9729);
        GL11.glTexParameteri((int)32879, (int)10242, (int)10497);
        GL11.glTexParameteri((int)32879, (int)10243, (int)10497);
        GL11.glTexParameteri((int)32879, (int)32882, (int)10497);
        GL11.glBindTexture((int)32879, (int)0);
        return n2;
    }

    private static float remap(float f, float f2, float f3, float f4, float f5) {
        return f4 + (f - f2) / (f3 - f2) * (f5 - f4);
    }

    private static float clamp(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    private static float perlinFbm(float f, float f2, float f3, int n, int n2) {
        float f4 = 0.0f;
        float f5 = 0.5f;
        float f6 = 0.0f;
        for (int i = 0; i < n2; ++i) {
            f4 += f5 * Noise3D.perlin(f, f2, f3, n);
            f6 += f5;
            f5 *= 0.5f;
            n *= 2;
        }
        return Noise3D.clamp(0.5f + 0.5f * f4 / f6);
    }

    private static float perlin(float f, float f2, float f3, int n) {
        float f4 = f * (float)n;
        float f5 = f2 * (float)n;
        float f6 = f3 * (float)n;
        int n2 = (int)Math.floor(f4);
        int n3 = (int)Math.floor(f5);
        int n4 = (int)Math.floor(f6);
        float f7 = f4 - (float)n2;
        float f8 = f5 - (float)n3;
        float f9 = f6 - (float)n4;
        float f10 = Noise3D.fade(f7);
        float f11 = Noise3D.fade(f8);
        float f12 = Noise3D.fade(f9);
        float f13 = 0.0f;
        for (int i = 0; i < 8; ++i) {
            int n5 = i & 1;
            int n6 = i >> 1 & 1;
            int n7 = i >> 2 & 1;
            int n8 = Noise3D.hash(Math.floorMod(n2 + n5, n), Math.floorMod(n3 + n6, n), Math.floorMod(n4 + n7, n));
            float f14 = (n8 & 1) == 0 ? 1.0f : -1.0f;
            float f15 = (n8 & 2) == 0 ? 1.0f : -1.0f;
            float f16 = (n8 & 4) == 0 ? 1.0f : -1.0f;
            int n9 = n8 >> 3 & 3;
            if (n9 == 0) {
                f14 = 0.0f;
            } else if (n9 == 1) {
                f15 = 0.0f;
            } else {
                f16 = 0.0f;
            }
            float f17 = f14 * (f7 - (float)n5) + f15 * (f8 - (float)n6) + f16 * (f9 - (float)n7);
            float f18 = (n5 == 0 ? 1.0f - f10 : f10) * (n6 == 0 ? 1.0f - f11 : f11) * (n7 == 0 ? 1.0f - f12 : f12);
            f13 += f17 * f18;
        }
        return f13;
    }

    private static float fade(float f) {
        return f * f * f * (f * (f * 6.0f - 15.0f) + 10.0f);
    }

    private static float worleyFbm(float f, float f2, float f3, int n) {
        return Noise3D.clamp(0.625f * Noise3D.worley(f, f2, f3, n) + 0.25f * Noise3D.worley(f, f2, f3, n * 2) + 0.125f * Noise3D.worley(f, f2, f3, n * 4));
    }

    private static float worley(float f, float f2, float f3, int n) {
        float f4 = f * (float)n;
        float f5 = f2 * (float)n;
        float f6 = f3 * (float)n;
        int n2 = (int)Math.floor(f4);
        int n3 = (int)Math.floor(f5);
        int n4 = (int)Math.floor(f6);
        float f7 = 1.0E9f;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                for (int k = -1; k <= 1; ++k) {
                    int n5 = n2 + k;
                    int n6 = n3 + j;
                    int n7 = n4 + i;
                    int n8 = Noise3D.hash(Math.floorMod(n5, n), Math.floorMod(n6, n), Math.floorMod(n7, n));
                    float f8 = (float)n5 + (float)(n8 & 0xFF) / 255.0f;
                    float f9 = (float)n6 + (float)(n8 >> 8 & 0xFF) / 255.0f;
                    float f10 = (float)n7 + (float)(n8 >> 16 & 0xFF) / 255.0f;
                    float f11 = (f8 - f4) * (f8 - f4) + (f9 - f5) * (f9 - f5) + (f10 - f6) * (f10 - f6);
                    f7 = Math.min(f7, f11);
                }
            }
        }
        return Noise3D.clamp(1.0f - (float)Math.sqrt(f7));
    }

    private static int hash(int n, int n2, int n3) {
        int n4 = n * 374761393 + n2 * 668265263 + n3 * Integer.MAX_VALUE;
        n4 = (n4 ^ n4 >>> 13) * 1274126177;
        return n4 ^ n4 >>> 16;
    }

    private Noise3D() {
    }
}

