/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.input;

import viewpoint.input.Ease;

final class EyeMotion {
    static final float FOLLOW = 0.04f;
    static final float STEADY = 0.15f;
    static final float BLEND = 0.2f;
    static final float RESTART = 0.25f;
    float offsetX;
    float offsetZ;
    float height;
    float lean;
    private float steadied;
    private float headX;
    private float headZ;
    private float followHeight;
    private float steadyHeight;
    private float steadyLean;
    private boolean started;

    EyeMotion() {
    }

    void update(float f, float f2, float f3, float f4, float f5, boolean bl, float f6) {
        if (!this.started || f > 0.25f) {
            this.started = true;
            this.steadied = bl ? 1.0f : 0.0f;
            this.headX = f2;
            this.headZ = f3;
            this.followHeight = this.steadyHeight = f4;
            this.steadyLean = f5;
        } else {
            this.steadied = Ease.step(this.steadied, bl ? 1.0f : 0.0f, f, 5.0f);
            this.headX = Ease.toward(this.headX, f2, f, 0.04f);
            this.headZ = Ease.toward(this.headZ, f3, f, 0.04f);
            this.followHeight = Ease.toward(this.followHeight, f4, f, 0.04f);
            this.steadyHeight = Ease.toward(this.steadyHeight, f4, f, 0.15f);
            this.steadyLean = Ease.toward(this.steadyLean, f5, f, 0.15f);
        }
        float f7 = this.steadied * (1.0f - f6);
        this.offsetX = (1.0f - f7) * this.headX;
        this.offsetZ = (1.0f - f7) * this.headZ;
        this.height = f7 * this.steadyHeight + (1.0f - f7) * this.followHeight;
        this.lean = f7 * this.steadyLean;
    }
}

