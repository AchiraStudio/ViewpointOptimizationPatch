/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.environment;

final class SkyClock {
    private static final float SMOOTHING = 0.5f;
    private static final float MAX_PACE = 250.0f;
    private static final float MAX_FRAME = 0.1f;
    private static final float HOURS_A_DAY = 24.0f;
    private float pace = 1.0f;
    private float lastTimeOfDay = Float.NaN;
    private double seconds;

    SkyClock() {
    }

    float advance(float f, float f2, boolean bl, float f3) {
        float f4;
        float f5;
        float f6 = f2 - this.lastTimeOfDay;
        boolean bl2 = Float.isNaN(this.lastTimeOfDay);
        this.lastTimeOfDay = f2;
        if (bl || bl2 || f <= 0.0f) {
            return 0.0f;
        }
        if (f6 < -12.0f) {
            f6 += 24.0f;
        }
        if ((f5 = f6 / (f4 = f * 24.0f / (60.0f * f3))) >= 0.0f && f5 <= 250.0f) {
            this.pace += (f5 - this.pace) * Math.min(1.0f, f / 0.5f);
        }
        float f7 = Math.min(f, 0.1f) * this.pace;
        this.seconds += (double)f7;
        return f7;
    }

    double seconds() {
        return this.seconds;
    }
}

