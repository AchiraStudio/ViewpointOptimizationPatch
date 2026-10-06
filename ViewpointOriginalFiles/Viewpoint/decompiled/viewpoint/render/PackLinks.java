/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import viewpoint.platform.GlProgram;
import viewpoint.platform.ShaderPacks;

final class PackLinks {
    private static int packTried;

    private PackLinks() {
    }

    static void start() {
        ShaderPacks.Active active = GlProgram.pack();
        packTried = active.generation;
        if (!GlProgram.allLinked() && !active.pack.builtIn) {
            String string = GlProgram.lastError();
            GlProgram.relinkAll(ShaderPacks.library(active.generation));
            ShaderPacks.linked(GlProgram.pack());
            ShaderPacks.failed(active, string);
            return;
        }
        ShaderPacks.linked(active);
    }

    static void follow() {
        ShaderPacks.Active active = ShaderPacks.active();
        if (active.generation == packTried) {
            return;
        }
        packTried = active.generation;
        String string = GlProgram.relinkAll(active);
        if (string != null) {
            ShaderPacks.failed(active, string);
            return;
        }
        ShaderPacks.linked(active);
        System.out.println("[Viewpoint] shader pack " + active.pack.id + " linked");
    }
}

