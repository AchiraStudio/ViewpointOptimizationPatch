/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.environment;

import java.util.Arrays;

final class ShoreField {
    static final float STEPS_PER_SQUARE = 16.0f;
    private static final float DIAGONAL = (float)Math.sqrt(2.0);
    private static final float FAR = 1000000.0f;
    private static final float[] distance = new float[4096];

    static void compute(byte[] byArray, byte[] byArray2) {
        int n;
        int n2;
        int n3 = 64;
        boolean bl = false;
        for (n2 = 0; n2 < distance.length; ++n2) {
            ShoreField.distance[n2] = byArray[n2] == 0 ? 0.0f : 1000000.0f;
            bl |= byArray[n2] != 0;
        }
        if (!bl) {
            Arrays.fill(byArray2, (byte)0);
            return;
        }
        for (n2 = 0; n2 < n3; ++n2) {
            for (n = 0; n < n3; ++n) {
                ShoreField.relax(n, n2, n - 1, n2, 1.0f);
                ShoreField.relax(n, n2, n, n2 - 1, 1.0f);
                ShoreField.relax(n, n2, n - 1, n2 - 1, DIAGONAL);
                ShoreField.relax(n, n2, n + 1, n2 - 1, DIAGONAL);
            }
        }
        for (n2 = n3 - 1; n2 >= 0; --n2) {
            for (n = n3 - 1; n >= 0; --n) {
                ShoreField.relax(n, n2, n + 1, n2, 1.0f);
                ShoreField.relax(n, n2, n, n2 + 1, 1.0f);
                ShoreField.relax(n, n2, n + 1, n2 + 1, DIAGONAL);
                ShoreField.relax(n, n2, n - 1, n2 + 1, DIAGONAL);
            }
        }
        for (n2 = 0; n2 < distance.length; ++n2) {
            byArray2[n2] = (byte)Math.min(255, Math.round(distance[n2] * 16.0f));
        }
    }

    private static void relax(int n, int n2, int n3, int n4, float f) {
        int n5 = 64;
        if (n3 < 0 || n4 < 0 || n3 >= n5 || n4 >= n5) {
            return;
        }
        int n6 = n2 * n5 + n;
        ShoreField.distance[n6] = Math.min(distance[n6], distance[n4 * n5 + n3] + f);
    }

    private ShoreField() {
    }
}

