/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.models;

import viewpoint.core.Frame;
import viewpoint.render.SceneData;

final class CorpseView {
    private static final double MARGIN = Math.toRadians(10.0);
    private static float apexX;
    private static float apexY;
    private static float levelX;
    private static float levelY;
    private static float eyeX;
    private static float eyeY;
    private static float eyeUp;
    private static float forwardX;
    private static float forwardY;
    private static float forwardUp;
    private static float rightX;
    private static float rightY;
    private static float upX;
    private static float upY;
    private static float upUp;
    private static float sinX;
    private static float cosX;
    private static float sinY;
    private static float cosY;

    static void begin(Frame frame) {
        SceneData sceneData = frame.scene;
        CorpseView.set(frame.camX - sceneData.viewerX, frame.camY - sceneData.viewerZ, frame.camZ * 2.4494896f + sceneData.viewerY, frame.viewYaw, frame.viewPitch, 1.0f / sceneData.projection.m00(), 1.0f / sceneData.projection.m11());
        CorpseView.apex(frame.camX, frame.camY);
    }

    static void set(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        eyeX = f;
        eyeY = f2;
        eyeUp = f3;
        float f8 = (float)Math.cos(f5);
        levelX = (float)Math.cos(f4);
        levelY = (float)Math.sin(f4);
        forwardX = levelX * f8;
        forwardY = levelY * f8;
        forwardUp = (float)Math.sin(f5);
        float f9 = (float)Math.sqrt(forwardX * forwardX + forwardY * forwardY);
        rightX = f9 > 1.0E-6f ? forwardY / f9 : 1.0f;
        rightY = f9 > 1.0E-6f ? -forwardX / f9 : 0.0f;
        upX = rightY * forwardUp;
        upY = -rightX * forwardUp;
        upUp = rightX * forwardY - rightY * forwardX;
        double d = Math.atan(f6) + MARGIN;
        double d2 = Math.atan(f7) + MARGIN;
        sinX = (float)Math.sin(d);
        cosX = (float)Math.cos(d);
        sinY = (float)Math.sin(d2);
        cosY = (float)Math.cos(d2);
    }

    static void apex(float f, float f2) {
        apexX = f;
        apexY = f2;
    }

    static boolean sees(float f, float f2, float f3) {
        float f4 = -1.2f;
        float f5 = f - apexX;
        float f6 = f2 - apexY;
        float f7 = f5 * levelX + f6 * levelY;
        float f8 = Math.abs(f5 * levelY - f6 * levelX);
        if (f7 <= f4 || f7 * sinX - f8 * cosX <= f4) {
            return false;
        }
        float f9 = f - eyeX;
        float f10 = f2 - eyeY;
        float f11 = f3 * 2.4494896f + 0.2f - eyeUp;
        float f12 = f9 * forwardX + f10 * forwardY + f11 * forwardUp;
        float f13 = Math.abs(f9 * rightX + f10 * rightY);
        float f14 = Math.abs(f9 * upX + f10 * upY + f11 * upUp);
        return f12 > f4 && f12 * sinX - f13 * cosX > f4 && f12 * sinY - f14 * cosY > f4;
    }

    private CorpseView() {
    }
}

