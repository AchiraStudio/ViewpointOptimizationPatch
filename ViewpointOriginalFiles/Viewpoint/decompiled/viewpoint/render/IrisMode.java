/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import viewpoint.platform.GlDebug;
import viewpoint.platform.IrisPacks;
import viewpoint.render.FarPass;
import viewpoint.render.FrameContext;
import viewpoint.render.IrisFrame;
import viewpoint.render.IrisPipeline;
import viewpoint.render.ModelPass;
import viewpoint.render.ShadowPass;

final class IrisMode {
    private static final IrisFrame frame = new IrisFrame();
    private static IrisPipeline pipeline;
    private static IrisPacks.Active drawing;
    private static int tried;
    private static boolean ready;

    static boolean draw(FrameContext frameContext, ShadowPass shadowPass, ModelPass modelPass, FarPass farPass) {
        IrisPacks.Active active = IrisPacks.active();
        if (active == null) {
            IrisMode.release();
            return false;
        }
        if (!ready) {
            frame.init();
            ready = true;
        }
        if (active.generation() != tried) {
            tried = active.generation();
            IrisMode.link(active);
        }
        if (pipeline == null) {
            return false;
        }
        farPass.prepare(frameContext, false);
        try {
            GlDebug.push(IrisMode.drawing.pack().id);
            frame.draw(pipeline, frameContext, shadowPass, modelPass, farPass);
            GlDebug.pop();
            return true;
        }
        catch (IllegalStateException illegalStateException) {
            GlDebug.pop();
            IrisPacks.failed(drawing, illegalStateException.getMessage());
            IrisMode.release();
            return false;
        }
    }

    private static void link(IrisPacks.Active active) {
        try {
            IrisPipeline irisPipeline = IrisPipeline.create(active.pack(), active.values(), IrisPacks.standard());
            if (pipeline != null) {
                pipeline.release();
            }
            pipeline = irisPipeline;
            drawing = active;
            IrisPacks.linked(active, irisPipeline.report);
            System.out.println("[Viewpoint] Minecraft shader pack " + active.pack().id + " linked");
            for (String string : irisPipeline.report) {
                System.out.println("[Viewpoint] Minecraft shader pack " + active.pack().id + ": " + string);
            }
        }
        catch (IllegalStateException illegalStateException) {
            IrisPacks.failed(active, illegalStateException.getMessage());
        }
    }

    static IrisPipeline pipeline() {
        return pipeline;
    }

    private static void release() {
        if (pipeline != null) {
            pipeline.release();
            pipeline = null;
            drawing = null;
        }
    }

    private IrisMode() {
    }
}

