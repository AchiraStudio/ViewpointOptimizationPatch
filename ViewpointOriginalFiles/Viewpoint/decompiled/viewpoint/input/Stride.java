/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.input;

import viewpoint.input.Ease;

final class Stride {
    static final int WALK = 0;
    static final int SNEAK = 1;
    static final float WALK_TAU = 0.3f;
    static final float FACTOR_TAU = 0.1f;
    static final float MOST = 5.0f;
    private final float[] walk = new float[2];
    private float factor = 1.0f;

    Stride() {
    }

    void walked(int n, float f, float f2) {
        this.walk[n] = this.walk[n] == 0.0f ? f : Ease.toward(this.walk[n], f, f2, 0.3f);
    }

    float strafed(int n, float f, float f2, float f3, float f4) {
        if (this.walk[n] > 0.0f && f > 0.0f && f2 > 0.0f) {
            float f5 = Math.max(1.0f, Math.min(5.0f, f4 * this.walk[n] * f2 / f));
            this.factor = Ease.toward(this.factor, f5, f3, 0.1f);
        }
        return this.factor;
    }

    void stop() {
        this.factor = 1.0f;
    }

    float factor() {
        return this.factor;
    }
}

