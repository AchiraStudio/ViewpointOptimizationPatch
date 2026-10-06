/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import viewpoint.core.View;

public final class Visibility {
    public static boolean lightingUpdate;

    public static float viewDistance(float f) {
        return View.enabled && lightingUpdate ? Math.max(f, Visibility.drawDistance()) : f;
    }

    public static float drawDistance() {
        return (View.radiusChunks() + 1) * 8;
    }

    private Visibility() {
    }
}

