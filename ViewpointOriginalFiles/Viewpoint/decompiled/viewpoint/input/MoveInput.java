/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.input;

import viewpoint.input.Ease;

final class MoveInput {
    static final float REVERSAL = (float)Math.toRadians(135.0);
    private boolean moving;
    private float direction;

    MoveInput() {
    }

    void update(float f, float f2, float f3, float f4) {
        if (f2 == 0.0f && f3 == 0.0f) {
            this.moving = false;
            return;
        }
        float f5 = (float)Math.atan2(f3, f2);
        boolean bl = !this.moving || Math.abs(Ease.between(this.direction, f5)) > REVERSAL;
        this.direction = bl ? f5 : Ease.angleToward(this.direction, f5, f, f4);
        this.moving = true;
    }

    boolean moving() {
        return this.moving;
    }

    float direction() {
        return this.direction;
    }
}

