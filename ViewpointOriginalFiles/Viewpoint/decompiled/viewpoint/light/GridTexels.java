/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.nio.ByteBuffer;

public final class GridTexels {
    private static final int[] COL = new int[]{0, 1, 1, 0};
    private static final int[] ROW = new int[]{0, 0, 1, 1};

    static void light(ByteBuffer byteBuffer, int n, int n2, int[] nArray, boolean bl) {
        for (int i = 0; i < 4; ++i) {
            GridTexels.put(byteBuffer, n * 2 + COL[i], n2 * 2 + ROW[i], nArray[i], bl ? 255 : 0);
        }
    }

    static void sky(ByteBuffer byteBuffer, int n, int n2, int[] nArray) {
        for (int i = 0; i < 4; ++i) {
            GridTexels.put(byteBuffer, n * 2 + COL[i], n2 * 2 + ROW[i] + 20, nArray[i], 0);
        }
    }

    static void noSky(ByteBuffer byteBuffer, int n, int n2) {
        for (int i = 0; i < 4; ++i) {
            GridTexels.put(byteBuffer, n * 2 + COL[i], n2 * 2 + ROW[i] + 20, 255, 0);
        }
    }

    public static void uniform(ByteBuffer byteBuffer, float f, float f2, float f3, boolean bl) {
        int n = GridTexels.pack(f, f2, f3);
        int[] nArray = new int[]{n, n, n, n};
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                GridTexels.light(byteBuffer, j, i, nArray, bl);
                GridTexels.noSky(byteBuffer, j, i);
            }
        }
    }

    public static void flatSquare(ByteBuffer byteBuffer, int n, int n2, float f, float f2, float f3, boolean bl, int n3) {
        int n4 = GridTexels.pack(f, f2, f3);
        GridTexels.light(byteBuffer, n, n2, new int[]{n4, n4, n4, n4}, bl);
        GridTexels.sky(byteBuffer, n, n2, new int[]{n3, n3, n3, n3});
    }

    public static void openSky(ByteBuffer byteBuffer) {
        int[] nArray = new int[]{0, 0, 0, 0};
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                GridTexels.sky(byteBuffer, j, i, nArray);
            }
        }
    }

    static int pack(float f, float f2, float f3) {
        return GridTexels.clamp(f) | GridTexels.clamp(f2) << 8 | GridTexels.clamp(f3) << 16;
    }

    private static int clamp(float f) {
        return Math.max(0, Math.min(255, Math.round(f * 255.0f)));
    }

    private static void put(ByteBuffer byteBuffer, int n, int n2, int n3, int n4) {
        int n5 = (n2 * 20 + n) * 4;
        byteBuffer.put(n5, (byte)(n3 & 0xFF));
        byteBuffer.put(n5 + 1, (byte)(n3 >> 8 & 0xFF));
        byteBuffer.put(n5 + 2, (byte)(n3 >> 16 & 0xFF));
        byteBuffer.put(n5 + 3, (byte)n4);
    }

    private GridTexels() {
    }
}

