/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.visibility;

import viewpoint.core.Frame;
import viewpoint.input.ThirdPerson;
import viewpoint.platform.LiveSettings;
import viewpoint.render.SceneData;
import viewpoint.visibility.Portals;

public final class Glass {
    public static final int REACH = 48;
    private static final String SECTION = "World/Glass";
    private static final LiveSettings.Toggle ON = LiveSettings.toggle("glass.far", "Glass opaque far away, hiding the rooms behind it", "World/Glass", true);
    private static final LiveSettings.Number START = LiveSettings.number("glass.fadeStart", "Glass starts turning opaque at (squares)", "World/Glass", 1.0f, 48.0f, 1.0f, 16.0f);
    private static final LiveSettings.Number OPAQUE = LiveSettings.number("glass.opaqueDistance", "Glass opaque from (squares)", "World/Glass", 2.0f, 48.0f, 1.0f, 48.0f);
    private static final LiveSettings.Number STOREYS = LiveSettings.number("glass.opaqueStoreys", "Glass opaque from storeys above or below", "World/Glass", 1.0f, 32.0f, 1.0f, 6.0f);
    private static final float DISTANCE_MARGIN = 1.0f;
    private static final float STOREY_MARGIN = 0.25f;

    public static void snapshot(Frame frame) {
        SceneData sceneData = frame.scene;
        sceneData.viewerX = frame.onCamera ? 0.0f : frame.eyeX - ThirdPerson.offsetX;
        sceneData.viewerY = frame.onCamera ? 0.0f : frame.eyeY + ThirdPerson.offsetUp;
        sceneData.viewerZ = frame.onCamera ? 0.0f : frame.eyeZ - ThirdPerson.offsetY;
        sceneData.glassFar = ON.get();
        sceneData.glassOpaque = OPAQUE.get();
        sceneData.glassStart = Math.min(START.get(), sceneData.glassOpaque - 1.0f);
        sceneData.glassStoreys = STOREYS.get();
    }

    static boolean opaque(SceneData sceneData, float[] fArray, int n, Portals.View view) {
        float f;
        float f2;
        if (!sceneData.glassFar) {
            return false;
        }
        float f3 = fArray[n + 4] * 2.4494896f;
        float f4 = Glass.outside(view.x, Math.min(fArray[n], fArray[n + 2]), Math.max(fArray[n], fArray[n + 2]));
        return f4 * f4 + (f2 = Glass.outside(view.y, Math.min(fArray[n + 1], fArray[n + 3]), Math.max(fArray[n + 1], fArray[n + 3]))) * f2 + (f = Glass.outside(view.h, f3, f3 + 2.4494896f)) * f >= Glass.square(sceneData.glassOpaque + 1.0f) || f >= sceneData.glassStoreys * 2.4494896f + 0.25f;
    }

    private static float outside(float f, float f2, float f3) {
        return Math.max(Math.max(f2 - f, f - f3), 0.0f);
    }

    private static float square(float f) {
        return f * f;
    }

    private Glass() {
    }
}

