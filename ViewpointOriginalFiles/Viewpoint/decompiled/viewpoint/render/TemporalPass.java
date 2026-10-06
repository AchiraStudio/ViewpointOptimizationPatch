/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.Tuning;
import viewpoint.render.FrameContext;
import viewpoint.render.SceneData;
import viewpoint.render.Targets;
import viewpoint.render.WorldRenderer;

final class TemporalPass {
    private final GlProgram resolve = GlProgram.create("taa", "screen.vert", "taa.frag");
    private final GlProgram output = GlProgram.create("taa out", "screen.vert", "taa_out.frag");
    private final Targets targets;
    private int historyIndex;
    boolean historyValid;
    private long lastFrameNanos;
    private double previousOriginX;
    private double previousOriginY;
    private double previousOriginZ;
    private final Matrix4f previousViewProjection = new Matrix4f();

    TemporalPass(Targets targets) {
        this.targets = targets;
        this.resolve.sampler("uCurrent", 0);
        this.resolve.sampler("uHistory", 19);
        this.resolve.sampler("uDepth", 12);
        this.resolve.sampler("uFarDepth", 29);
        this.resolve.sampler("uVelocity", 39);
        this.output.sampler("uColor", 0);
    }

    void begin(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        long l = WorldRenderer.clock.getAsLong();
        double d = Math.abs(sceneData.originX - this.previousOriginX) + Math.abs(sceneData.originY - this.previousOriginY) + Math.abs(sceneData.originZ - this.previousOriginZ);
        boolean bl = frameContext.previousValid = l - this.lastFrameNanos <= 250000000L && d <= 4.0;
        if (!frameContext.previousValid) {
            this.historyValid = false;
        }
        frameContext.frameSeconds = this.lastFrameNanos == 0L ? 0.0f : (float)(l - this.lastFrameNanos) * 1.0E-9f;
        this.lastFrameNanos = l;
        frameContext.previousViewProjection.set((Matrix4fc)this.previousViewProjection);
        frameContext.reprojection.set((Matrix4fc)this.previousViewProjection).translate((float)(sceneData.originX - this.previousOriginX), (float)(sceneData.originY - this.previousOriginY), (float)(sceneData.originZ - this.previousOriginZ));
        frameContext.jitteredProjection.set((Matrix4fc)sceneData.projection);
        if (!WorldRenderer.off(1)) {
            frameContext.jitterPhase = frameContext.jitterPhase + 1 & 7;
            float f = TemporalPass.halton(frameContext.jitterPhase + 1, 2) - 0.5f;
            float f2 = TemporalPass.halton(frameContext.jitterPhase + 1, 3) - 0.5f;
            frameContext.jitteredProjection.m20(frameContext.jitteredProjection.m20() + f * 2.0f / (float)frameContext.width);
            frameContext.jitteredProjection.m21(frameContext.jitteredProjection.m21() + f2 * 2.0f / (float)frameContext.height);
        }
        sceneData.projection.mul((Matrix4fc)frameContext.view, frameContext.plainViewProjection);
        frameContext.jitteredProjection.mul((Matrix4fc)frameContext.view, frameContext.viewProjection);
        frameContext.viewProjection.invert(frameContext.invViewProjection);
        frameContext.plainViewProjection.invert(frameContext.invPlainViewProjection);
    }

    int resolve(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        int n = this.historyIndex ^ 1;
        GL30.glBindFramebuffer((int)36160, (int)this.targets.historyFbo[n]);
        GL11.glViewport((int)0, (int)0, (int)frameContext.width, (int)frameContext.height);
        this.resolve.use();
        frameContext.camera(this.resolve);
        this.resolve.set("uUnproject", frameContext.invPlainViewProjection);
        this.resolve.set("uPrevious", frameContext.reprojection);
        this.resolve.set("uHistoryValid", this.historyValid ? 1.0f : 0.0f);
        this.resolve.set("uHistoryWeight", Tuning.historyWeight);
        this.resolve.set("uTexel", 1.0f / (float)frameContext.width, 1.0f / (float)frameContext.height);
        this.resolve.set("uFarUnproject", frameContext.farInvPlainViewProjection);
        this.resolve.set("uFarRange", frameContext.farNear, frameContext.farFar);
        Gl.bind(19, this.targets.history[this.historyIndex]);
        Gl.bind(12, this.targets.depth);
        Gl.bind(29, this.targets.farDepth);
        Gl.bind(39, this.targets.velocity);
        GL13.glActiveTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)this.targets.taaInput);
        Gl.screenTriangle();
        Gl.bind(19, 0);
        Gl.bind(12, 0);
        Gl.bind(29, 0);
        Gl.bind(39, 0);
        GL13.glActiveTexture((int)33984);
        this.historyIndex = n;
        this.historyValid = true;
        this.remember(frameContext);
        return this.targets.history[n];
    }

    void remember(FrameContext frameContext) {
        this.previousViewProjection.set((Matrix4fc)frameContext.plainViewProjection);
        this.previousOriginX = frameContext.scene.originX;
        this.previousOriginY = frameContext.scene.originY;
        this.previousOriginZ = frameContext.scene.originZ;
    }

    void present(FrameContext frameContext, int n, float f) {
        GL30.glBindFramebuffer((int)36009, (int)frameContext.outputDrawFbo);
        GL30.glBindFramebuffer((int)36008, (int)frameContext.outputReadFbo);
        GL11.glViewport((int)frameContext.viewport[0], (int)frameContext.viewport[1], (int)frameContext.viewport[2], (int)frameContext.viewport[3]);
        this.output.use();
        this.output.set("uSharpen", f);
        this.output.set("uTexel", 1.0f / (float)frameContext.width, 1.0f / (float)frameContext.height);
        GL13.glActiveTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)n);
        Gl.screenTriangle();
        GL11.glBindTexture((int)3553, (int)0);
    }

    private static float halton(int n, int n2) {
        float f = 0.0f;
        float f2 = 1.0f;
        for (int i = n; i > 0; i /= n2) {
            f += (f2 /= (float)n2) * (float)(i % n2);
        }
        return f;
    }
}

