/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.system.MemoryUtil
 */
package viewpoint.far;

import java.nio.FloatBuffer;
import java.util.Arrays;
import org.lwjgl.system.MemoryUtil;

public final class FarMesher {
    private static final ThreadLocal<float[]> SCRATCH = ThreadLocal.withInitial(() -> new float[65536]);
    private static final int STANDING = 4;
    private float[] out;
    private int n;
    private float x0;
    private float y0;
    private float inset;

    public static FloatBuffer mesh(short[] sArray, int n) {
        int n2 = 256 / n;
        int n3 = n * n;
        int n4 = n3 - Math.max(1, n3 / 4);
        short[] sArray2 = new short[n2 * n2];
        short[] sArray3 = new short[n3];
        for (int i = 0; i < n2; ++i) {
            for (int j = 0; j < n2; ++j) {
                int n5 = 0;
                for (int k = i * n; k < (i + 1) * n; ++k) {
                    for (int i2 = j * n; i2 < (j + 1) * n; ++i2) {
                        sArray3[n5++] = sArray[k * 256 + i2];
                    }
                }
                Arrays.sort(sArray3);
                sArray2[i * n2 + j] = sArray3[n4];
            }
        }
        return FarMesher.boxes(sArray2, n2, n, 0.0f, 0.0f, 0.0f, true);
    }

    static FloatBuffer boxes(short[] sArray, int n, int n2, float f, float f2, float f3, boolean bl) {
        FarMesher farMesher = new FarMesher();
        farMesher.out = SCRATCH.get();
        farMesher.x0 = f;
        farMesher.y0 = f2;
        farMesher.inset = f3;
        farMesher.tops(sArray, n, n2, bl);
        farMesher.sides(sArray, n, n2);
        SCRATCH.set(farMesher.out);
        return MemoryUtil.memAllocFloat((int)farMesher.n).put(farMesher.out, 0, farMesher.n).flip();
    }

    private void tops(short[] sArray, int n, int n2, boolean bl) {
        boolean[] blArray = new boolean[n * n];
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                int n3;
                int n4;
                if (blArray[i * n + j]) continue;
                short s = sArray[i * n + j];
                int n5 = 1;
                while (j + n5 < n && !blArray[i * n + j + n5] && sArray[i * n + j + n5] == s) {
                    ++n5;
                }
                int n6 = 1;
                block3: while (i + n6 < n) {
                    for (n4 = 0; n4 < n5; ++n4) {
                        n3 = (i + n6) * n + j + n4;
                        if (blArray[n3] || sArray[n3] != s) break block3;
                    }
                    ++n6;
                }
                for (n4 = 0; n4 < n6; ++n4) {
                    for (n3 = 0; n3 < n5; ++n3) {
                        blArray[(i + n4) * n + j + n3] = true;
                    }
                }
                if (s == 0 && !bl) continue;
                float f = (float)s / 8.0f;
                float f2 = this.x0 + (float)(j * n2);
                float f3 = this.x0 + (float)((j + n5) * n2);
                float f4 = this.y0 + (float)(i * n2);
                float f5 = this.y0 + (float)((i + n6) * n2);
                this.quad(f2, f4, f, f3, f4, f, f3, f5, f, f2, f5, f, 0);
            }
        }
    }

    private void sides(short[] sArray, int n, int n2) {
        float f;
        float f2;
        float f3;
        float f4;
        float f5;
        short s;
        short s2;
        int n3;
        int n4;
        float f6;
        int n5;
        for (n5 = -1; n5 < n; ++n5) {
            f6 = this.x0 + (float)((n5 + 1) * n2);
            for (n3 = 0; n3 < n; n3 += n4) {
                s2 = n5 >= 0 ? sArray[n3 * n + n5] : (short)0;
                s = n5 + 1 < n ? sArray[n3 * n + n5 + 1] : (short)0;
                n4 = 1;
                while (n3 + n4 < n && (n5 >= 0 ? sArray[(n3 + n4) * n + n5] : (short)0) == s2 && (n5 + 1 < n ? sArray[(n3 + n4) * n + n5 + 1] : (short)0) == s) {
                    ++n4;
                }
                if (s2 == s) continue;
                f5 = (float)Math.min(s2, s) / 8.0f;
                f4 = (float)Math.max(s2, s) / 8.0f;
                f3 = this.y0 + (float)(n3 * n2);
                f2 = this.y0 + (float)((n3 + n4) * n2);
                f = s2 > s ? f6 - this.inset : f6 + this.inset;
                this.quad(f, f3, f5, f, f2, f5, f, f2, f4, f, f3, f4, s2 > s ? 2 : 1);
            }
        }
        for (n5 = -1; n5 < n; ++n5) {
            f6 = this.y0 + (float)((n5 + 1) * n2);
            for (n3 = 0; n3 < n; n3 += n4) {
                s2 = n5 >= 0 ? sArray[n5 * n + n3] : (short)0;
                s = n5 + 1 < n ? sArray[(n5 + 1) * n + n3] : (short)0;
                n4 = 1;
                while (n3 + n4 < n && (n5 >= 0 ? sArray[n5 * n + n3 + n4] : (short)0) == s2 && (n5 + 1 < n ? sArray[(n5 + 1) * n + n3 + n4] : (short)0) == s) {
                    ++n4;
                }
                if (s2 == s) continue;
                f5 = (float)Math.min(s2, s) / 8.0f;
                f4 = (float)Math.max(s2, s) / 8.0f;
                f3 = this.x0 + (float)(n3 * n2);
                f2 = this.x0 + (float)((n3 + n4) * n2);
                f = s2 > s ? f6 - this.inset : f6 + this.inset;
                this.quad(f3, f, f5, f2, f, f5, f2, f, f4, f3, f, f4, s2 > s ? 4 : 3);
            }
        }
    }

    private void quad(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, int n) {
        if (this.n + 24 > this.out.length) {
            this.out = Arrays.copyOf(this.out, this.out.length * 2);
        }
        this.vertex(f, f2, f3, n);
        this.vertex(f4, f5, f6, n);
        this.vertex(f7, f8, f9, n);
        this.vertex(f, f2, f3, n);
        this.vertex(f7, f8, f9, n);
        this.vertex(f10, f11, f12, n);
    }

    private void vertex(float f, float f2, float f3, int n) {
        this.out[this.n++] = f;
        this.out[this.n++] = f2;
        this.out[this.n++] = f3;
        this.out[this.n++] = n;
    }
}

