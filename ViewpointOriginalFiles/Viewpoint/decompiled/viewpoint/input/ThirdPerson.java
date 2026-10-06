/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.LosUtil$TestResults
 *  zombie.vehicles.BaseVehicle
 */
package viewpoint.input;

import viewpoint.core.CameraSquares;
import viewpoint.core.Frame;
import viewpoint.core.View;
import viewpoint.input.Look;
import viewpoint.platform.Keys;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.LosUtil;
import zombie.vehicles.BaseVehicle;

public final class ThirdPerson {
    public static volatile boolean active;
    private static final float HEAD_CLEAR = 0.35f;
    public static float offsetX;
    public static float offsetY;
    public static float offsetUp;
    public static boolean headShown;
    private static final float[] placed;
    static final float BOOM = 2.6f;
    static final float SHOULDER = 0.45f;
    static final float LIFT = 0.2f;
    static final float SEATED_BOOM = 5.0f;
    static final float SEATED_LIFT = 0.9f;
    static final float CLEARANCE = 0.15f;
    static final float CORNER_MARGIN = 0.05f;
    static final float MAX_CLEARANCE = 0.45f;
    private static final float SHORT = 0.001f;

    public static void poll() {
        if (!Keys.THIRD_PERSON.pressed()) {
            return;
        }
        active = !active;
        System.out.println("[Viewpoint] third person " + (active ? "on" : "off"));
    }

    public static void capture(Frame frame, IsoCell isoCell, IsoGameCharacter isoGameCharacter) {
        CameraSquares cameraSquares = frame.cameraSquares;
        cameraSquares.on = active && !frame.onCamera;
        offsetUp = 0.0f;
        offsetY = 0.0f;
        offsetX = 0.0f;
        headShown = false;
        if (!cameraSquares.on) {
            return;
        }
        BaseVehicle baseVehicle = isoGameCharacter.getVehicle();
        cameraSquares.seated = baseVehicle != null;
        cameraSquares.nearCorner = 0.05f / (float)Math.cos(View.halfDiagonal(true));
        float f = cameraSquares.seated ? baseVehicle.getX() : frame.camX - frame.eyeX;
        float f2 = cameraSquares.seated ? baseVehicle.getY() : frame.camY - frame.eyeZ;
        int n = (int)Math.ceil(ThirdPerson.boom(cameraSquares) + 0.45f) + 2;
        cameraSquares.side = n * 2 + 1;
        cameraSquares.x = (int)Math.floor(f) - n;
        cameraSquares.y = (int)Math.floor(f2) - n;
        cameraSquares.vehicleX = f - (float)cameraSquares.x;
        cameraSquares.vehicleY = f2 - (float)cameraSquares.y;
        cameraSquares.level = (int)Math.floor(frame.camZ + (frame.eyeY + ThirdPerson.lift(cameraSquares)) / 2.4494896f);
        ThirdPerson.squares(cameraSquares, isoCell);
        float f3 = frame.camX - (float)cameraSquares.x;
        float f4 = frame.camY - (float)cameraSquares.y;
        float f5 = (frame.camZ - (float)cameraSquares.level) * 2.4494896f;
        float f6 = f3 - frame.eyeX;
        float f7 = f4 - frame.eyeZ;
        float f8 = f5 + frame.eyeY;
        ThirdPerson.place(cameraSquares, f6, f7, f8, Look.yaw, Look.pitch, placed);
        offsetX = placed[0] - f6;
        offsetY = placed[1] - f7;
        offsetUp = placed[2] - f8;
        float f9 = offsetUp - ThirdPerson.lift(cameraSquares);
        headShown = offsetX * offsetX + offsetY * offsetY + f9 * f9 >= 0.122499995f;
    }

    private static void squares(CameraSquares cameraSquares, IsoCell isoCell) {
        int n = cameraSquares.side * cameraSquares.side * 2;
        if (cameraSquares.blocks.length < n) {
            cameraSquares.blocks = new byte[n];
        }
        int n2 = 0;
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < cameraSquares.side; ++j) {
                for (int k = 0; k < cameraSquares.side; ++k) {
                    IsoGridSquare isoGridSquare = isoCell.getGridSquare(cameraSquares.x + k, cameraSquares.y + j, cameraSquares.level + i);
                    cameraSquares.blocks[n2++] = isoGridSquare == null ? (byte)0 : ThirdPerson.blocks(isoGridSquare);
                }
            }
        }
    }

    private static byte blocks(IsoGridSquare isoGridSquare) {
        int n = ThirdPerson.seen(isoGridSquare.testVisionAdjacent(0, -1, 0, false, false)) ? 0 : 1;
        n |= ThirdPerson.seen(isoGridSquare.testVisionAdjacent(-1, 0, 0, false, false)) ? 0 : 2;
        return (byte)(n |= isoGridSquare.isSolidFloor() || isoGridSquare.TreatAsSolidFloor() || isoGridSquare.hasRainBlockingTile() ? 4 : 0);
    }

    private static boolean seen(LosUtil.TestResults testResults) {
        return testResults == LosUtil.TestResults.Clear || testResults == LosUtil.TestResults.ClearThroughOpenDoor;
    }

    public static boolean view(Frame frame, float f, float f2, float[] fArray) {
        CameraSquares cameraSquares = frame.cameraSquares;
        if (!cameraSquares.on) {
            return false;
        }
        float f3 = frame.camX - (float)cameraSquares.x;
        float f4 = frame.camY - (float)cameraSquares.y;
        float f5 = (frame.camZ - (float)cameraSquares.level) * 2.4494896f;
        ThirdPerson.place(cameraSquares, f3 - frame.eyeX, f4 - frame.eyeZ, f5 + frame.eyeY, f, f2, fArray);
        float f6 = fArray[0];
        float f7 = fArray[1];
        float f8 = fArray[2];
        fArray[0] = f3 - f6;
        fArray[1] = f8 - f5;
        fArray[2] = f4 - f7;
        return true;
    }

    static void place(CameraSquares cameraSquares, float f, float f2, float f3, float f4, float f5, float[] fArray) {
        float f6 = (float)Math.cos(f4);
        float f7 = (float)Math.sin(f4);
        float f8 = (float)Math.cos(f5);
        float f9 = (float)Math.sin(f5);
        float f10 = cameraSquares.seated ? cameraSquares.vehicleX : f;
        float f11 = cameraSquares.seated ? cameraSquares.vehicleY : f2;
        float f12 = f3 + ThirdPerson.lift(cameraSquares);
        float f13 = cameraSquares.seated ? 0.0f : 0.45f;
        float f14 = ThirdPerson.reach(cameraSquares, f10, f11, f12, f10 - f7 * f13, f11 + f6 * f13, f12);
        float f15 = f10 - f7 * f13 * f14;
        float f16 = f11 + f6 * f13 * f14;
        float f17 = ThirdPerson.boom(cameraSquares);
        float f18 = f15 - f6 * f8 * f17;
        float f19 = f16 - f7 * f8 * f17;
        float f20 = f12 - f9 * f17;
        f14 = ThirdPerson.reach(cameraSquares, f15, f16, f12, f18, f19, f20);
        fArray[0] = f15 + (f18 - f15) * f14;
        fArray[1] = f16 + (f19 - f16) * f14;
        fArray[2] = f12 + (f20 - f12) * f14;
        ThirdPerson.keepClear(cameraSquares, fArray);
    }

    static float reach(CameraSquares cameraSquares, float f, float f2, float f3, float f4, float f5, float f6) {
        float f7;
        boolean bl;
        float f8 = f4 - f;
        float f9 = f5 - f2;
        float f10 = f6 - f3;
        float f11 = (float)Math.sqrt(f8 * f8 + f9 * f9 + f10 * f10);
        if (f11 < 0.001f) {
            return 1.0f;
        }
        int n = (int)Math.floor(f);
        int n2 = (int)Math.floor(f2);
        int n3 = (int)Math.floor(f3 / 2.4494896f);
        int n4 = f8 > 0.0f ? 1 : -1;
        int n5 = f9 > 0.0f ? 1 : -1;
        int n6 = f10 > 0.0f ? 1 : -1;
        float f12 = ThirdPerson.crossing(f, f8, n, 1.0f);
        float f13 = ThirdPerson.crossing(f2, f9, n2, 1.0f);
        float f14 = ThirdPerson.crossing(f3, f10, n3, 2.4494896f);
        do {
            if ((f7 = Math.min(f12, Math.min(f13, f14))) >= 1.0f) {
                return 1.0f;
            }
            if (f7 == f12) {
                bl = ThirdPerson.blocked(cameraSquares, n, n2, n3, n4, 0, 0, 2);
                n += n4;
                f12 += 1.0f / Math.abs(f8);
                continue;
            }
            if (f7 == f13) {
                bl = ThirdPerson.blocked(cameraSquares, n, n2, n3, 0, n5, 0, 1);
                n2 += n5;
                f13 += 1.0f / Math.abs(f9);
                continue;
            }
            bl = ThirdPerson.blocked(cameraSquares, n, n2, n3, 0, 0, n6, 4);
            n3 += n6;
            f14 += 2.4494896f / Math.abs(f10);
        } while (!bl);
        return Math.max(0.0f, f7 - 0.001f / f11);
    }

    private static float crossing(float f, float f2, int n, float f3) {
        if (f2 == 0.0f) {
            return Float.POSITIVE_INFINITY;
        }
        return ((float)(f2 > 0.0f ? n + 1 : n) * f3 - f) / f2;
    }

    private static boolean blocked(CameraSquares cameraSquares, int n, int n2, int n3, int n4, int n5, int n6, int n7) {
        int n8 = cameraSquares.at(n + n4, n2 + n5, n3 + n6);
        int n9 = n4 + n5 + n6 > 0 ? n8 : cameraSquares.at(n, n2, n3);
        return n8 < 0 || (n9 & n7) != 0;
    }

    private static void keepClear(CameraSquares cameraSquares, float[] fArray) {
        float f = Math.min(0.45f, Math.max(0.15f, cameraSquares.nearCorner + 0.05f));
        int n = (int)Math.floor(fArray[0]);
        int n2 = (int)Math.floor(fArray[1]);
        int n3 = (int)Math.floor(fArray[2] / 2.4494896f);
        float f2 = (float)n3 * 2.4494896f;
        if (ThirdPerson.blocked(cameraSquares, n, n2, n3, -1, 0, 0, 2)) {
            fArray[0] = Math.max(fArray[0], (float)n + f);
        }
        if (ThirdPerson.blocked(cameraSquares, n, n2, n3, 1, 0, 0, 2)) {
            fArray[0] = Math.min(fArray[0], (float)(n + 1) - f);
        }
        if (ThirdPerson.blocked(cameraSquares, n, n2, n3, 0, -1, 0, 1)) {
            fArray[1] = Math.max(fArray[1], (float)n2 + f);
        }
        if (ThirdPerson.blocked(cameraSquares, n, n2, n3, 0, 1, 0, 1)) {
            fArray[1] = Math.min(fArray[1], (float)(n2 + 1) - f);
        }
        if (ThirdPerson.blocked(cameraSquares, n, n2, n3, 0, 0, -1, 4)) {
            fArray[2] = Math.max(fArray[2], f2 + f);
        }
        if (ThirdPerson.blocked(cameraSquares, n, n2, n3, 0, 0, 1, 4)) {
            fArray[2] = Math.min(fArray[2], f2 + 2.4494896f - f);
        }
    }

    private static float boom(CameraSquares cameraSquares) {
        return cameraSquares.seated ? 5.0f : 2.6f;
    }

    private static float lift(CameraSquares cameraSquares) {
        return cameraSquares.seated ? 0.9f : 0.2f;
    }

    private ThirdPerson() {
    }

    static {
        placed = new float[3];
    }
}

