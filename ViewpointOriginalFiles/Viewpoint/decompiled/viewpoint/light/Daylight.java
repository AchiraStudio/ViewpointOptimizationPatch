/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import viewpoint.render.SceneData;
import zombie.GameTime;

public final class Daylight {
    public static void snapshot(SceneData sceneData) {
        int n = Daylight.skyLevel();
        float f = (float)(n & 0xFF) / 255.0f;
        float f2 = (float)(n >> 8 & 0xFF) / 255.0f;
        float f3 = (float)(n >> 16 & 0xFF) / 255.0f;
        sceneData.daylightKnown = true;
        sceneData.daylightR = f;
        sceneData.daylightG = f2;
        sceneData.daylightB = f3;
        sceneData.skyR = f;
        sceneData.skyG = f2;
        sceneData.skyB = f3;
    }

    static int skyLevel() {
        return GameTime.getInstance().getSkyLightLevel();
    }

    private Daylight() {
    }
}

