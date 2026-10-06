/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import viewpoint.platform.GlProgram;
import viewpoint.platform.LiveSettings;
import viewpoint.render.Wireframe;

public final class LodView {
    private static final LiveSettings.Toggle SHOWN = LiveSettings.toggle("debug.lod", "Colour by level of detail", "Debug/Views", false);
    private static final int OFF = 0;
    private static final int TINTED = 1;
    private static final int PLAIN = 2;

    public static void flip() {
        SHOWN.set(!SHOWN.get());
        System.out.println("[Viewpoint] level of detail view " + (SHOWN.get() ? "on" : "off"));
    }

    static void apply(GlProgram glProgram) {
        if (glProgram.has("uLodView")) {
            glProgram.setInt("uLodView", Wireframe.drawing() == 2 ? 2 : (SHOWN.get() ? 1 : 0));
        }
    }

    private LodView() {
    }
}

