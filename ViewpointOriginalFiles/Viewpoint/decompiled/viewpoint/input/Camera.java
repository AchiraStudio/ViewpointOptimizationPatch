/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.input;

import viewpoint.core.Frame;
import viewpoint.input.FreeCam;

public final class Camera {
    private static final float EYE_FORWARD = 0.12f;

    public static void direction(float f, float f2, float[] fArray) {
        float f3 = (float)Math.cos(f2);
        fArray[0] = -((float)Math.cos(f)) * f3;
        fArray[1] = (float)Math.sin(f2);
        fArray[2] = -((float)Math.sin(f)) * f3;
    }

    public static void eye(Frame frame, float f, float[] fArray) {
        float f2 = 0.12f + frame.eyeLean;
        fArray[0] = frame.eyeX - (float)Math.cos(f) * f2;
        fArray[1] = frame.eyeY;
        fArray[2] = frame.eyeZ - (float)Math.sin(f) * f2;
    }

    public static void at(Frame frame, FreeCam.Place place, float[] fArray, float[] fArray2) {
        fArray[0] = (float)((double)frame.camX - place.x());
        fArray[1] = (float)((place.z() - (double)frame.camZ) * 2.4494895935058594);
        fArray[2] = (float)((double)frame.camY - place.y());
        Camera.direction(place.yaw(), place.pitch(), fArray2);
    }

    private Camera() {
    }
}

