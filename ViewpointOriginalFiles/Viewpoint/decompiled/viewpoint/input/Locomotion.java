/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.input;

import viewpoint.input.Ease;

final class Locomotion {
    static final int TURN_TO_MOVE = 0;
    static final int STRAFE = 1;
    static final float BACKPEDAL_IN = (float)Math.toRadians(110.0);
    static final float BACKPEDAL_OUT = (float)Math.toRadians(95.0);
    static final float STRAFE_IN = (float)Math.toRadians(25.0);
    static final float STRAFE_OUT = (float)Math.toRadians(15.0);
    static final float SIDEWAYS = (float)Math.toRadians(89.0);
    static final float WIDE_TURN = (float)Math.toRadians(150.0);
    static final float SWITCH_TURN = (float)Math.toRadians(60.0);
    static final float SWITCH_SECONDS = 0.4f;
    private boolean moving;
    private boolean strafing;
    private boolean backwards;
    private boolean switched;

    Locomotion() {
    }

    static float turn(float f, float f2, float f3, float f4) {
        if (Math.abs(Ease.between(f, f2)) > WIDE_TURN) {
            return Ease.wrap(f2);
        }
        return Ease.angleToward(f, f2, f3, f4);
    }

    static boolean steersAfterSwitch(float f, float f2) {
        return Math.abs(Ease.between(f, f2)) >= SWITCH_TURN;
    }

    void update(int n, boolean bl, float f) {
        boolean bl2 = this.moving;
        boolean bl3 = this.strafing;
        this.moving = bl;
        if (!bl) {
            this.switched = false;
            this.backwards = false;
            this.strafing = false;
            return;
        }
        float f2 = Math.abs(f);
        if (n == 0) {
            this.backwards = this.strafing = f2 > (this.strafing ? BACKPEDAL_OUT : BACKPEDAL_IN);
        } else {
            this.strafing = f2 > (this.strafing ? STRAFE_OUT : STRAFE_IN);
            this.backwards = f2 >= SIDEWAYS;
        }
        this.switched = bl2 && this.strafing != bl3;
    }

    boolean switched() {
        return this.switched;
    }

    boolean strafing() {
        return this.strafing;
    }

    boolean backwards() {
        return this.backwards;
    }
}

