/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL33
 */
package viewpoint.render;

import org.lwjgl.opengl.GL33;
import viewpoint.platform.GlProgram;
import viewpoint.render.TextureFilter;

final class FloorFilter {
    static final int BAKE_NEAREST = 0;
    static final int BAKE_LINEAR = 1;
    static final int BAKE_LANCZOS = 2;

    static int bakeFilter() {
        int n = TextureFilter.FLOORS.get();
        return n == 0 || n == 1 ? 0 : (n == 5 ? 2 : 1);
    }

    static void apply(GlProgram glProgram) {
        TextureFilter.FLOORS.apply(glProgram);
    }

    static void bind(int n) {
        GL33.glBindSampler((int)41, (int)(n == 0 ? 0 : TextureFilter.FLOORS.sampler(true)));
    }

    private FloorFilter() {
    }
}

