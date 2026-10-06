/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.input;

final class Ease {
    private static final float TWO_PI = (float)Math.PI * 2;
    private static final float ARRIVED = 0.001f;

    static float toward(float f, float f2, float f3, float f4) {
        if (f4 <= 0.0f) {
            return f2;
        }
        return f + (f2 - f) * (1.0f - (float)Math.exp(-f3 / f4));
    }

    static float step(float f, float f2, float f3, float f4) {
        float f5 = f4 * f3;
        return f + Math.max(-f5, Math.min(f5, f2 - f));
    }

    static float between(float f, float f2) {
        return Ease.wrap(f2 - f);
    }

    static float angleToward(float f, float f2, float f3, float f4) {
        float f5 = Ease.between(f, f2);
        if (Math.abs(f5) < 0.001f) {
            return Ease.wrap(f2);
        }
        return Ease.wrap(Ease.toward(f, f + f5, f3, f4));
    }

    static float wrap(float f) {
        float f2 = f % ((float)Math.PI * 2);
        return (double)f2 > Math.PI ? f2 - (float)Math.PI * 2 : ((double)f2 < -Math.PI ? f2 + (float)Math.PI * 2 : f2);
    }

    private Ease() {
    }
}

