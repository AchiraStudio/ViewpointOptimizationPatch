/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.util.Arrays;

final class ShadowLamps {
    static final float LINK = 12.0f;
    static final float STICK = 3.0f;
    static final float HANDOVER = 2.0f;
    private static final int MAX = 48;
    private static final int[] group = new int[48];
    private static final int[] order = new int[48];
    private static final float[] groupDistance = new float[48];
    private static final long[] cast = new long[48];
    private static int castCount;

    static int choose(int n, int[] nArray, int[] nArray2, int[] nArray3, float[] fArray, long[] lArray, float[] fArray2, int n2, boolean[] blArray, float[] fArray3) {
        int n3;
        int n4;
        Arrays.fill(blArray, 0, n, false);
        Arrays.fill(fArray3, 0, n, 0.0f);
        ShadowLamps.group(n, nArray, nArray2, nArray3, lArray, fArray2);
        ShadowLamps.sort(n, fArray, lArray);
        int n5 = 0;
        int n6 = -1;
        for (n4 = 0; n4 < n && n5 < n2; ++n5, ++n4) {
            n3 = order[n4];
            blArray[n3] = true;
            fArray3[n3] = 1.0f;
            n6 = group[n3];
        }
        ShadowLamps.handOver(n, n4, n6, n5 == n2, blArray, fArray3);
        castCount = 0;
        for (n3 = 0; n3 < n; ++n3) {
            if (!blArray[n3] || ShadowLamps.castLast(lArray[group[n3]])) continue;
            ShadowLamps.cast[ShadowLamps.castCount++] = lArray[group[n3]];
        }
        return n5;
    }

    private static void group(int n, int[] nArray, int[] nArray2, int[] nArray3, long[] lArray, float[] fArray) {
        int n2;
        int n3;
        for (n3 = 0; n3 < n; ++n3) {
            ShadowLamps.group[n3] = -1;
            ShadowLamps.order[n3] = n3;
        }
        for (n3 = 1; n3 < n; ++n3) {
            for (n2 = n3; n2 > 0 && lArray[order[n2]] < lArray[order[n2 - 1]]; --n2) {
                ShadowLamps.swap(n2, n2 - 1);
            }
        }
        for (n3 = 0; n3 < n; ++n3) {
            n2 = order[n3];
            if (group[n2] >= 0) continue;
            ShadowLamps.groupDistance[n2] = Float.MAX_VALUE;
            for (int i = 0; i < n; ++i) {
                if (group[i] >= 0 || !ShadowLamps.linked(n2, i, nArray, nArray2, nArray3)) continue;
                ShadowLamps.group[i] = n2;
                ShadowLamps.groupDistance[n2] = Math.min(groupDistance[n2], fArray[i]);
            }
            if (!ShadowLamps.castLast(lArray[n2])) continue;
            int n4 = n2;
            groupDistance[n4] = groupDistance[n4] - 3.0f;
        }
    }

    private static void sort(int n, float[] fArray, long[] lArray) {
        for (int i = 1; i < n; ++i) {
            for (int j = i; j > 0 && ShadowLamps.before(order[j], order[j - 1], fArray, lArray); --j) {
                ShadowLamps.swap(j, j - 1);
            }
        }
    }

    private static boolean before(int n, int n2, float[] fArray, long[] lArray) {
        if (group[n] != group[n2]) {
            float f = groupDistance[group[n]];
            float f2 = groupDistance[group[n2]];
            return f != f2 ? f < f2 : group[n] < group[n2];
        }
        return fArray[n] != fArray[n2] ? fArray[n] > fArray[n2] : lArray[n] < lArray[n2];
    }

    private static void handOver(int n, int n2, int n3, boolean bl, boolean[] blArray, float[] fArray) {
        if (!bl || n3 < 0) {
            return;
        }
        for (int i = n2; i < n; ++i) {
            int n4 = group[order[i]];
            if (n4 == n3) continue;
            float f = Math.min(1.0f, (groupDistance[n4] - groupDistance[n3]) / 2.0f);
            for (int j = 0; j < n; ++j) {
                if (!blArray[j] || group[j] != n3) continue;
                fArray[j] = Math.max(0.0f, f);
            }
            return;
        }
    }

    private static boolean linked(int n, int n2, int[] nArray, int[] nArray2, int[] nArray3) {
        float f = nArray[n] - nArray[n2];
        float f2 = nArray2[n] - nArray2[n2];
        float f3 = (float)(nArray3[n] - nArray3[n2]) * 2.0f;
        return f * f + f2 * f2 + f3 * f3 <= 144.0f;
    }

    private static boolean castLast(long l) {
        for (int i = 0; i < castCount; ++i) {
            if (cast[i] != l) continue;
            return true;
        }
        return false;
    }

    private static void swap(int n, int n2) {
        int n3 = order[n];
        ShadowLamps.order[n] = order[n2];
        ShadowLamps.order[n2] = n3;
    }

    static void reset() {
        castCount = 0;
    }

    private ShadowLamps() {
    }
}

