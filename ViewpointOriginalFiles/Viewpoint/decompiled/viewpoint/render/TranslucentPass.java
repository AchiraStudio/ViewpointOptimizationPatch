/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL40
 */
package viewpoint.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.GpuParts;
import viewpoint.render.FrameContext;
import viewpoint.render.ModelPass;
import viewpoint.render.ShadowPass;
import viewpoint.render.SurfacePass;
import viewpoint.render.Targets;
import viewpoint.render.WorldRenderer;

final class TranslucentPass {
    private static final float[] NOTHING = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
    private static final float[] EVERYTHING = new float[]{1.0f, 0.0f, 0.0f, 0.0f};
    private final GlProgram over = GlProgram.create("translucent over", "screen.vert", "translucent_over.frag");
    private final Targets targets;

    TranslucentPass(Targets targets) {
        this.targets = targets;
        this.over.sampler("uTranslucentSum", 86);
        this.over.sampler("uTranslucentReveal", 87);
    }

    void draw(FrameContext frameContext, SurfacePass surfacePass, ModelPass modelPass, ShadowPass shadowPass) {
        GpuParts.begin(14);
        GL30.glBindFramebuffer((int)36160, (int)this.targets.translucentFbo);
        GL30.glClearBufferfv((int)6144, (int)0, (float[])NOTHING);
        GL30.glClearBufferfv((int)6144, (int)1, (float[])EVERYTHING);
        GL11.glEnable((int)3042);
        GL40.glBlendFunci((int)0, (int)1, (int)1);
        GL40.glBlendFunci((int)1, (int)0, (int)769);
        surfacePass.translucent(frameContext);
        modelPass.glass(frameContext, shadowPass);
        WorldRenderer.bindTarget();
        GL11.glDisable((int)2929);
        GL11.glBlendFunc((int)771, (int)770);
        this.over.use();
        Gl.bind(86, this.targets.translucentSum);
        Gl.bind(87, this.targets.translucentReveal);
        GL13.glActiveTexture((int)33984);
        Gl.screenTriangle();
        Gl.bind(86, 0);
        Gl.bind(87, 0);
        GL13.glActiveTexture((int)33984);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glDisable((int)3042);
        GL11.glEnable((int)2929);
        GpuParts.end(14);
    }
}

