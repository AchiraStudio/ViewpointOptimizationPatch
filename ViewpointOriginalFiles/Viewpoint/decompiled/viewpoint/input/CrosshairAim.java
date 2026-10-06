/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 *  zombie.iso.IsoCell
 */
package viewpoint.input;

import viewpoint.core.Frame;
import viewpoint.input.Controls;
import viewpoint.input.FreeCam;
import viewpoint.input.Look;
import viewpoint.input.ThirdPerson;
import viewpoint.render.MousePick;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.iso.IsoCell;
import zombie.iso.IsoMovingObject;

public final class CrosshairAim {
    static final float NEAREST = 1.0f;
    static final float REACH = 0.35f;
    static final float TALL = 1.8f;
    static final float LYING = 0.6f;
    private static final double NONE = Double.POSITIVE_INFINITY;
    private static float turn;
    private static float lift;
    private static double cosYaw;
    private static double sinYaw;
    private static double level;
    private static double rise;
    private static double cameraX;
    private static double cameraY;
    private static double cameraUp;
    private static double chestUp;

    public static void snapshot(Frame frame, IsoCell isoCell, IsoGameCharacter isoGameCharacter) {
        double d;
        boolean bl;
        boolean bl2 = bl = Look.captured && !FreeCam.active && Controls.owns(isoGameCharacter);
        if (!bl) {
            CrosshairAim.off();
            return;
        }
        int n = Core.getInstance().getScreenWidth();
        int n2 = Core.getInstance().getScreenHeight();
        MousePick.Ask ask = MousePick.aim;
        if (ask == null || ask.screenWidth() != n || ask.screenHeight() != n2) {
            MousePick.aim = ask = new MousePick.Ask(n / 2, n2 / 2, n, n2);
        }
        double d2 = isoGameCharacter.getAimOriginPosX();
        double d3 = isoGameCharacter.getAimOriginPosY();
        CrosshairAim.sight(frame.viewYaw, frame.viewPitch, (double)(frame.camX - frame.eyeX + ThirdPerson.offsetX) - d2, (double)(frame.camY - frame.eyeZ + ThirdPerson.offsetY) - d3, frame.camZ * 2.4494896f + frame.eyeY + ThirdPerson.offsetUp, isoGameCharacter.getAimOriginPosZ() * 2.4494896f);
        MousePick.Hit hit = MousePick.aimHit;
        double d4 = d = hit == null || hit.ask() != ask ? Double.POSITIVE_INFINITY : CrosshairAim.along(hit.x() - d2, hit.y() - d3, hit.z() * 2.4494895935058594);
        for (IsoMovingObject isoMovingObject : isoCell.getObjectList()) {
            IsoGameCharacter isoGameCharacter2;
            if (!(isoMovingObject instanceof IsoGameCharacter) || (isoGameCharacter2 = (IsoGameCharacter)isoMovingObject) == isoGameCharacter || isoGameCharacter2.isDead()) continue;
            double d5 = isoGameCharacter2.getZ() * 2.4494896f;
            d4 = Math.min(d4, CrosshairAim.passed((double)isoGameCharacter2.getX() - d2, (double)isoGameCharacter2.getY() - d3, d5, isoGameCharacter2.isProne() ? (double)0.6f : (double)1.8f, d));
        }
        turn = CrosshairAim.turn(d4);
        lift = CrosshairAim.lift(d4);
    }

    public static void off() {
        MousePick.aim = null;
        lift = 0.0f;
        turn = 0.0f;
    }

    static float yaw(IsoGameCharacter isoGameCharacter) {
        return turn != 0.0f && isoGameCharacter.isAiming() ? Look.wrap(Look.yaw + turn) : Look.yaw;
    }

    static float pitch(IsoGameCharacter isoGameCharacter) {
        return lift != 0.0f && isoGameCharacter.isAiming() ? Look.pitch + lift : Look.pitch;
    }

    static void sight(float f, float f2, double d, double d2, double d3, double d4) {
        cosYaw = Math.cos(f);
        sinYaw = Math.sin(f);
        level = Math.cos(f2);
        rise = Math.sin(f2);
        cameraX = d;
        cameraY = d2;
        cameraUp = d3;
        chestUp = d4;
    }

    static double along(double d, double d2, double d3) {
        double d4 = ((d - cameraX) * cosYaw + (d2 - cameraY) * sinYaw) * level + (d3 - cameraUp) * rise;
        return d4 > 0.0 ? d4 : Double.POSITIVE_INFINITY;
    }

    static double passed(double d, double d2, double d3, double d4, double d5) {
        double d6 = (d - cameraX) * cosYaw + (d2 - cameraY) * sinYaw;
        double d7 = d6 / level;
        if (d6 <= 0.0 || d7 >= d5) {
            return Double.POSITIVE_INFINITY;
        }
        double d8 = (d2 - cameraY) * cosYaw - (d - cameraX) * sinYaw;
        if (Math.abs(d8) < (double)0.35f && CrosshairAim.over(cameraUp + d7 * rise, d3, d4)) {
            return d7;
        }
        double d9 = cameraX / d5 + level * cosYaw;
        double d10 = cameraY / d5 + level * sinYaw;
        double d11 = (cameraUp - chestUp) / d5 + rise;
        double d12 = Math.hypot(d9, d10);
        double d13 = (d * d9 + d2 * d10) / d12;
        double d14 = (d2 * d9 - d * d10) / d12;
        return d13 > 0.0 && Math.abs(d14) < (double)0.35f && CrosshairAim.over(chestUp + d11 * d13 / d12, d3, d4) ? d7 : Double.POSITIVE_INFINITY;
    }

    static float turn(double d) {
        return (float)Math.atan2(CrosshairAim.right(), CrosshairAim.ahead(d));
    }

    static float lift(double d) {
        if (d == Double.POSITIVE_INFINITY) {
            return 0.0f;
        }
        double d2 = CrosshairAim.ahead(d);
        double d3 = cameraX * cosYaw + cameraY * sinYaw;
        double d4 = cameraUp + (d2 - d3) / level * rise;
        return (float)(Math.atan2(d4 - chestUp, Math.hypot(CrosshairAim.right(), d2)) - Math.atan2(rise, level));
    }

    private static double right() {
        return cameraY * cosYaw - cameraX * sinYaw;
    }

    private static double ahead(double d) {
        return Math.max(1.0, cameraX * cosYaw + cameraY * sinYaw + d * level);
    }

    private static boolean over(double d, double d2, double d3) {
        return d >= d2 && d <= d2 + d3;
    }

    private CrosshairAim() {
    }
}

