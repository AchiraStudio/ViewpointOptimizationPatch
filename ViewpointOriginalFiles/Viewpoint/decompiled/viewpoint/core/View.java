/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 */
package viewpoint.core;

import viewpoint.platform.LiveSettings;
import zombie.core.Core;
import zombie.iso.IsoChunkMap;

public final class View {
    private static final String SECTION = "Controls/View";
    private static final float MIN_FOV = 30.0f;
    private static final float MAX_FOV = 140.0f;
    private static final float DEFAULT_FOV = 103.0f;
    private static final float DEFAULT_THIRD_PERSON_FOV = 60.0f;
    private static final LiveSettings.Number FOV = LiveSettings.number("view.verticalFov", "Field of view", "Controls/View", 30.0f, 140.0f, 1.0f, 103.0f);
    private static final LiveSettings.Number THIRD_PERSON_FOV = LiveSettings.number("view.thirdPersonVerticalFov", "Field of view in third person", "Controls/View", 30.0f, 140.0f, 1.0f, 60.0f);
    public static volatile boolean enabled;

    public static float fovY(boolean bl) {
        return (float)Math.toRadians((bl ? THIRD_PERSON_FOV : FOV).get());
    }

    public static double halfDiagonal(boolean bl) {
        double d = (double)Core.getInstance().getScreenWidth() / (double)Math.max(1, Core.getInstance().getScreenHeight());
        return Math.atan(Math.tan((double)View.fovY(bl) / 2.0) * Math.sqrt(1.0 + d * d));
    }

    public static int radiusChunks() {
        return Math.min(IsoChunkMap.chunkGridWidth, 19) / 2;
    }

    private View() {
    }

    static {
        FOV.describe("How tall the first-person view is, in degrees from the screen's bottom to its top.");
        THIRD_PERSON_FOV.describe("How tall the third-person view is, in degrees from the screen's bottom to its top.");
    }
}

