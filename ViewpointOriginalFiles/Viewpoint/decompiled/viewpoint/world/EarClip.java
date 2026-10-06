/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.world;

import java.util.Arrays;

final class EarClip {
    static int[] triangulate(float[] fArray) {
        int n;
        int n2 = fArray.length / 2;
        int[] nArray = new int[n2];
        boolean bl = EarClip.signedArea(fArray) > 0.0f;
        for (int i = 0; i < n2; ++i) {
            nArray[i] = bl ? i : n2 - 1 - i;
        }
        int[] nArray2 = new int[(n2 - 2) * 3];
        int n3 = 0;
        int n4 = n2;
        int n5 = 0;
        int n6 = 0;
        while (n4 > 3 && n6 < n4) {
            n = nArray[(n5 + n4 - 1) % n4];
            int n7 = nArray[n5 % n4];
            int n8 = nArray[(n5 + 1) % n4];
            if (EarClip.isEar(fArray, nArray, n4, n, n7, n8)) {
                nArray2[n3++] = n;
                nArray2[n3++] = n7;
                nArray2[n3++] = n8;
                int n9 = n5 % n4;
                System.arraycopy(nArray, n9 + 1, nArray, n9, n4 - n9 - 1);
                --n4;
                n6 = 0;
                continue;
            }
            ++n5;
            ++n6;
        }
        n = 1;
        while (n + 1 < n4) {
            nArray2[n3++] = nArray[0];
            nArray2[n3++] = nArray[n];
            nArray2[n3++] = nArray[n + 1];
            ++n;
        }
        return n3 == nArray2.length ? nArray2 : Arrays.copyOf(nArray2, n3);
    }

    private static boolean isEar(float[] fArray, int[] nArray, int n, int n2, int n3, int n4) {
        if (EarClip.cross(fArray, n2, n3, n4) <= 0.0f) {
            return false;
        }
        for (int i = 0; i < n; ++i) {
            int n5 = nArray[i];
            if (n5 == n2 || n5 == n3 || n5 == n4 || !(EarClip.cross(fArray, n2, n3, n5) >= 0.0f) || !(EarClip.cross(fArray, n3, n4, n5) >= 0.0f) || !(EarClip.cross(fArray, n4, n2, n5) >= 0.0f)) continue;
            return false;
        }
        return true;
    }

    private static float cross(float[] fArray, int n, int n2, int n3) {
        return (fArray[n2 * 2] - fArray[n * 2]) * (fArray[n3 * 2 + 1] - fArray[n * 2 + 1]) - (fArray[n2 * 2 + 1] - fArray[n * 2 + 1]) * (fArray[n3 * 2] - fArray[n * 2]);
    }

    private static float signedArea(float[] fArray) {
        float f = 0.0f;
        int n = fArray.length / 2;
        int n2 = 0;
        int n3 = n - 1;
        while (n2 < n) {
            f += fArray[n3 * 2] * fArray[n2 * 2 + 1] - fArray[n2 * 2] * fArray[n3 * 2 + 1];
            n3 = n2++;
        }
        return f;
    }

    private EarClip() {
    }
}

