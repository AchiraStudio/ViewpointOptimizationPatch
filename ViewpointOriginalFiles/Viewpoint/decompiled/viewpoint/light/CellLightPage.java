/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.util.Arrays;
import viewpoint.light.LightFlood;

public final class CellLightPage {
    public static final int TEXELS = 128;
    public static final int LAMP_INTS = 4;
    public static final int ROOM_INTS = 5;
    public static final int ROOM_LEVELS = 8;

    public static byte[] build(int n, int n2, int[] nArray, int[] nArray2) {
        byte[] byArray = new byte[65536];
        int n3 = n * 256;
        int n4 = n2 * 256;
        int n5 = 2;
        int n6 = 0;
        while (n6 + 4 <= nArray.length) {
            CellLightPage.lamp(byArray, n3, n4, n5, nArray, n6);
            n6 += 4;
        }
        n6 = 0;
        while (n6 + 5 <= nArray2.length) {
            CellLightPage.room(byArray, n3, n4, n5, nArray2, n6);
            n6 += 5;
        }
        return byArray;
    }

    public static int[] cards(int n, int n2, int[] nArray) {
        int[] nArray2 = new int[nArray.length / 4 * 4];
        int n3 = 0;
        int n4 = n * 256;
        int n5 = n2 * 256;
        int n6 = 0;
        while (n6 + 4 <= nArray.length) {
            int n7 = nArray[n6];
            int n8 = nArray[n6 + 1];
            int n9 = nArray[n6 + 3];
            int n10 = (int)CellLightPage.engine(n9) | (int)CellLightPage.engine(n9 >> 8) << 8 | (int)CellLightPage.engine(n9 >> 16) << 16;
            if (n7 >= n4 && n8 >= n5 && n7 < n4 + 256 && n8 < n5 + 256 && n10 != 0) {
                nArray2[n3] = n7;
                nArray2[n3 + 1] = n8;
                nArray2[n3 + 2] = nArray[n6 + 2];
                nArray2[n3 + 3] = n10;
                n3 += 4;
            }
            n6 += 4;
        }
        return Arrays.copyOf(nArray2, n3);
    }

    private static void lamp(byte[] byArray, int n, int n2, int n3, int[] nArray, int n4) {
        int n5 = nArray[n4];
        int n6 = nArray[n4 + 1];
        int n7 = nArray[n4 + 2];
        int n8 = nArray[n4 + 3];
        int n9 = Math.min(n8 >>> 24 & 0x3F, 20);
        double d = 3.0 * (double)Math.max(0, n7);
        if (n9 <= 0 || d >= (double)n9) {
            return;
        }
        double d2 = CellLightPage.engine(n8);
        double d3 = CellLightPage.engine(n8 >> 8);
        double d4 = CellLightPage.engine(n8 >> 16);
        int n10 = Math.max(0, Math.floorDiv(n5 - n9 - n, n3));
        int n11 = Math.min(127, (n5 + n9 - n) / n3);
        int n12 = Math.max(0, Math.floorDiv(n6 - n9 - n2, n3));
        int n13 = Math.min(127, (n6 + n9 - n2) / n3);
        for (int i = n12; i <= n13; ++i) {
            for (int j = n10; j <= n11; ++j) {
                double d5;
                double d6 = Math.abs((double)n + ((double)j + 0.5) * (double)n3 - ((double)n5 + 0.5));
                double d7 = Math.max(d6, d5 = Math.abs((double)n2 + ((double)i + 0.5) * (double)n3 - ((double)n6 + 0.5))) + (LightFlood.DIAGONAL - 1.0) * Math.min(d6, d5) + d;
                if (!(d7 < (double)n9)) continue;
                double d8 = 1.0 - d7 / (double)n9;
                int n14 = (i * 128 + j) * 4;
                CellLightPage.brighten(byArray, n14, (int)Math.floor(d2 * d8));
                CellLightPage.brighten(byArray, n14 + 1, (int)Math.floor(d3 * d8));
                CellLightPage.brighten(byArray, n14 + 2, (int)Math.floor(d4 * d8));
            }
        }
    }

    private static void room(byte[] byArray, int n, int n2, int n3, int[] nArray, int n4) {
        int n5 = nArray[n4] - n;
        int n6 = nArray[n4 + 1] - n2;
        int n7 = nArray[n4 + 2];
        int n8 = nArray[n4 + 3];
        int n9 = nArray[n4 + 4];
        if (n7 < 0 || n7 >= 8) {
            return;
        }
        for (int i = Math.max(0, Math.floorDiv(n6, n3)); i <= Math.min(127, (n6 + n9 - 1) / n3); ++i) {
            for (int j = Math.max(0, Math.floorDiv(n5, n3)); j <= Math.min(127, (n5 + n8 - 1) / n3); ++j) {
                int n10 = (i * 128 + j) * 4 + 3;
                byArray[n10] = (byte)(byArray[n10] | (byte)(1 << n7));
            }
        }
    }

    private static double engine(int n) {
        return 255.0 * Math.min(1.0, (double)(n & 0xFF) / 255.0 * 2.0);
    }

    private static void brighten(byte[] byArray, int n, int n2) {
        byArray[n] = (byte)Math.max(byArray[n] & 0xFF, Math.min(255, n2));
    }

    private CellLightPage() {
    }
}

