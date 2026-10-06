/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 */
package viewpoint.render;

import org.lwjgl.opengl.GL11;
import viewpoint.platform.GlProgram;
import viewpoint.platform.LiveSettings;

final class Wireframe {
    static final String SECTION = "Debug/Views";
    private static final LiveSettings.Toggle LINES = LiveSettings.toggle("debug.wireframe", "Wireframe", "Debug/Views", false);
    private static final LiveSettings.Toggle BARE = LiveSettings.toggle("debug.wireframeOnly", "Wireframe only (untextured)", "Debug/Views", false);
    static final int OFF = 0;
    static final int TEXTURED = 1;
    static final int UNTEXTURED = 2;
    private static int drawing;

    static int mode() {
        return BARE.get() ? 2 : (LINES.get() ? 1 : 0);
    }

    static int drawing() {
        return drawing;
    }

    static void begin() {
        drawing = Wireframe.mode();
        if (drawing != 0) {
            GL11.glPolygonMode((int)1032, (int)6913);
        }
    }

    static void end() {
        if (drawing != 0) {
            GL11.glPolygonMode((int)1032, (int)6914);
        }
        drawing = 0;
    }

    static void apply(GlProgram glProgram) {
        if (glProgram.has("uWireframe")) {
            glProgram.setInt("uWireframe", drawing);
        }
    }

    private Wireframe() {
    }
}

