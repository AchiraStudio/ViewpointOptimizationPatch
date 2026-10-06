/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.interact;

final class LootAim {
    static final int BOX = 9;
    static final int FLAT = 8;
    static final float SOLID = -1.0f;
    static final float KEEP = 0.08f;
    static final float TAKE_OVER = 0.25f;
    private static final float GRAZE = 1000.0f;
    private static final float[] turned = new float[6];

    static float enter(float[] fArray, float[] fArray2, int n, float f) {
        int n2 = n * 9;
        float[] fArray3 = LootAim.unturn(fArray, fArray2, n2);
        float f2 = 0.0f;
        float f3 = Float.MAX_VALUE;
        for (int i = 0; i < 3; ++i) {
            float f4;
            float f5;
            float f6 = fArray3[i];
            float f7 = fArray3[i + 3];
            float f8 = fArray2[n2 + i] - f;
            float f9 = fArray2[n2 + 3 + i] + f;
            if (!(Math.abs(f7) < 1.0E-6f ? f6 < f8 || f6 > f9 : (f2 = Math.max(f2, Math.min(f5 = (f8 - f6) / f7, f4 = (f9 - f6) / f7))) > (f3 = Math.min(f3, Math.max(f5, f4))))) continue;
            return -1.0f;
        }
        return f2;
    }

    static int pick(float[] fArray, float[] fArray2, int n, int n2, float f) {
        float f2;
        int n3 = -1;
        float f3 = Float.MAX_VALUE;
        for (int i = 0; i < n; ++i) {
            float f4 = LootAim.rank(fArray, fArray2, i, 0.0f, f);
            if (!(f4 >= 0.0f) || !(f4 < f3)) continue;
            n3 = i;
            f3 = f4;
        }
        if (n2 >= 0 && n2 < n && n2 != n3 && (f2 = LootAim.rank(fArray, fArray2, n2, 0.08f, f)) >= 0.0f && (n3 < 0 || f2 <= f3 + 0.25f)) {
            return n2;
        }
        return n3;
    }

    private static float rank(float[] fArray, float[] fArray2, int n, float f, float f2) {
        float f3 = LootAim.enter(fArray, fArray2, n, f);
        if (f3 < 0.0f) {
            return -1.0f;
        }
        int n2 = n * 9;
        int n3 = (int)fArray2[n2 + 8];
        if (n3 < 0) {
            return f3 >= f2 ? f3 : -1.0f;
        }
        float f4 = LootAim.crossing(fArray, fArray2, n2, n3, f);
        if (f4 >= f2) {
            return f4;
        }
        return f3 >= f2 ? f3 + 1000.0f : -1.0f;
    }

    private static float crossing(float[] fArray, float[] fArray2, int n, int n2, float f) {
        float[] fArray3 = LootAim.unturn(fArray, fArray2, n);
        float f2 = fArray3[n2 + 3];
        if (Math.abs(f2) < 1.0E-6f) {
            return -1.0f;
        }
        float f3 = ((fArray2[n + n2] + fArray2[n + 3 + n2]) * 0.5f - fArray3[n2]) / f2;
        for (int i = 0; i < 3 && f3 >= 0.0f; ++i) {
            float f4 = fArray3[i] + f3 * fArray3[i + 3];
            if (i == n2 || !(f4 < fArray2[n + i] - f) && !(f4 > fArray2[n + 3 + i] + f)) continue;
            return -1.0f;
        }
        return f3;
    }

    private static float[] unturn(float[] fArray, float[] fArray2, int n) {
        float f = fArray2[n + 6];
        float f2 = fArray2[n + 7];
        if (f2 == 0.0f && f == 1.0f) {
            return fArray;
        }
        float f3 = (fArray2[n] + fArray2[n + 3]) * 0.5f;
        float f4 = (fArray2[n + 1] + fArray2[n + 4]) * 0.5f;
        float f5 = fArray[0] - f3;
        float f6 = fArray[1] - f4;
        LootAim.turned[0] = f3 + f5 * f + f6 * f2;
        LootAim.turned[1] = f4 - f5 * f2 + f6 * f;
        LootAim.turned[2] = fArray[2];
        LootAim.turned[3] = fArray[3] * f + fArray[4] * f2;
        LootAim.turned[4] = -fArray[3] * f2 + fArray[4] * f;
        LootAim.turned[5] = fArray[5];
        return turned;
    }

    private LootAim() {
    }
}

