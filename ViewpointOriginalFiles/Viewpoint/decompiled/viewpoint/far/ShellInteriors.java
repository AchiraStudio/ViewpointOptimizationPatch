/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.util.Arrays;
import viewpoint.platform.LiveSettings;

final class ShellInteriors {
    static final int[] NONE = new int[0];
    static final byte WALLS = 1;
    static final byte FURNISHED = 2;
    static final LiveSettings.Toggle FURNITURE = LiveSettings.toggle("lod.interiorFurniture", "Furniture in buildings across the grid's edge", "World/Levels of detail", false);
    private static int[] scratch = new int[64];

    static int[] bounds(short[][] sArray, int n) {
        int[] nArray = new int[(n + 1) * 4];
        for (int i = 0; i <= n; ++i) {
            nArray[i * 4 + 1] = Integer.MAX_VALUE;
            nArray[i * 4] = Integer.MAX_VALUE;
            nArray[i * 4 + 3] = Integer.MIN_VALUE;
            nArray[i * 4 + 2] = Integer.MIN_VALUE;
        }
        for (short[] sArray2 : sArray) {
            for (int i = 0; sArray2 != null && i < sArray2.length; ++i) {
                short s = sArray2[i];
                if (s <= 0 || s > n) continue;
                int n2 = i % 256;
                int n3 = i / 256;
                nArray[s * 4] = Math.min(nArray[s * 4], n2);
                nArray[s * 4 + 1] = Math.min(nArray[s * 4 + 1], n3);
                nArray[s * 4 + 2] = Math.max(nArray[s * 4 + 2], n2);
                nArray[s * 4 + 3] = Math.max(nArray[s * 4 + 3], n3);
            }
        }
        return nArray;
    }

    static int[] select(int[] nArray, int n, int n2, int n3, int n4, float f, float f2, float f3, int[] nArray2) {
        int n5 = 0;
        int n6 = n3 * 64;
        int n7 = n4 * 64;
        int n8 = n6 + 64 - 1;
        int n9 = n7 + 64 - 1;
        for (int i = 1; i < nArray.length / 4; ++i) {
            int n10 = i * 4;
            if (nArray[n10 + 2] < n6 || nArray[n10] > n8 || nArray[n10 + 3] < n7 || nArray[n10 + 1] > n9) continue;
            float f4 = (float)(n * 256 + nArray[n10]) - f;
            float f5 = f4 + (float)nArray[n10 + 2] + 1.0f - (float)nArray[n10];
            float f6 = (float)(n2 * 256 + nArray[n10 + 1]) - f2;
            float f7 = f6 + (float)nArray[n10 + 3] + 1.0f - (float)nArray[n10 + 1];
            float f8 = Math.max(0.0f, Math.max(f4, -f5));
            float f9 = Math.max(0.0f, Math.max(f6, -f7));
            float f10 = Math.max(Math.abs(f4), Math.abs(f5));
            float f11 = Math.max(Math.abs(f6), Math.abs(f7));
            float f12 = Arrays.binarySearch(nArray2, i) >= 0 ? 32.0f : 0.0f;
            float f13 = f3 + f12;
            float f14 = Math.max(0.0f, f3 - f12);
            if (!(f8 * f8 + f9 * f9 < f13 * f13) || !(f10 * f10 + f11 * f11 > f14 * f14)) continue;
            if (n5 == scratch.length) {
                scratch = Arrays.copyOf(scratch, n5 * 2);
            }
            ShellInteriors.scratch[n5++] = i;
        }
        return Arrays.equals(scratch, 0, n5, nArray2, 0, nArray2.length) ? nArray2 : (n5 == 0 ? NONE : Arrays.copyOf(scratch, n5));
    }

    static byte[] mask(int[] nArray, boolean bl) {
        if (nArray.length == 0) {
            return null;
        }
        byte[] byArray = new byte[nArray[nArray.length - 1] + 1];
        for (int n : nArray) {
            byArray[n] = bl ? 2 : 1;
        }
        return byArray;
    }

    private ShellInteriors() {
    }
}

