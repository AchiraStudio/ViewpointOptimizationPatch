/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL33
 *  zombie.core.textures.Texture
 */
package viewpoint.render;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.Tuning;
import viewpoint.render.BlurPass;
import viewpoint.render.FlashShadow;
import viewpoint.render.FrameContext;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;
import viewpoint.render.Targets;
import zombie.core.textures.Texture;

final class IndirectPass {
    private final GlProgram gi = GlProgram.create("gi", "screen.vert", "gi.frag");
    private final Targets targets;
    private final ShadowPass shadows;
    private final BlurPass blur;
    private final int rawDepthSampler;
    private final Matrix4f flashUnproject = new Matrix4f();

    IndirectPass(Targets targets, ShadowPass shadowPass, BlurPass blurPass) {
        this.targets = targets;
        this.shadows = shadowPass;
        this.blur = blurPass;
        this.gi.sampler("uColor", 0);
        this.gi.sampler("uDepth", 12);
        this.gi.sampler("uNormal", 10);
        this.gi.sampler("uAlbedo", 9);
        this.gi.sampler("uRooms", 23);
        this.gi.sampler("uFlashDepth", 22);
        ShadowPass.samplers(this.gi);
        FlashShadow.sampler(this.gi);
        this.rawDepthSampler = GL33.glGenSamplers();
        GL33.glSamplerParameteri((int)this.rawDepthSampler, (int)10241, (int)9728);
        GL33.glSamplerParameteri((int)this.rawDepthSampler, (int)10240, (int)9728);
        GL33.glSamplerParameteri((int)this.rawDepthSampler, (int)10242, (int)33071);
        GL33.glSamplerParameteri((int)this.rawDepthSampler, (int)10243, (int)33071);
        GL33.glSamplerParameteri((int)this.rawDepthSampler, (int)34892, (int)0);
    }

    void draw(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        GL30.glBindFramebuffer((int)36160, (int)this.targets.giFbo);
        GL11.glViewport((int)0, (int)0, (int)frameContext.halfWidth, (int)frameContext.halfHeight);
        GL11.glDisable((int)2929);
        GL11.glDisable((int)3042);
        GL11.glDepthMask((boolean)false);
        this.gi.use();
        frameContext.camera(this.gi);
        this.gi.set("uViewRot", frameContext.viewRotation);
        this.gi.set("uGiRadius", Tuning.giRadius);
        this.gi.set("uGiStrength", Tuning.giStrength);
        this.gi.set("uOriginFrac", Gl.wrap(sceneData.originX, 64.0), Gl.wrap(sceneData.originY, 64.0), Gl.wrap(sceneData.originZ, 64.0));
        this.shadows.sunUniforms(this.gi, frameContext);
        this.shadows.flash.uniforms(this.gi, frameContext);
        this.shadows.flash.viewProjection().invert(this.flashUnproject);
        this.gi.set("uFlashUnproject", this.flashUnproject);
        this.gi.set("uViewProjection", frameContext.viewProjection);
        this.gi.set("uSunBounce", Tuning.sunBounce);
        this.gi.set("uFloorY", frameContext.floorY());
        this.gi.set("uFrameNoise", frameContext.jitterPhase);
        this.gi.set("uExposureA", sceneData.exposureAX, sceneData.exposureAY);
        this.gi.set("uExposureSize", 64.0f);
        this.targets.uploadGrids(sceneData, frameContext.index);
        Gl.bind(23, this.targets.rooms);
        Gl.bind(22, this.shadows.flash.texture());
        GL33.glBindSampler((int)22, (int)this.rawDepthSampler);
        this.shadows.bind(true);
        Gl.bind(9, this.targets.albedo);
        Gl.bind(12, this.targets.depth);
        Gl.bind(10, this.targets.normal);
        GL13.glActiveTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)this.targets.hdr);
        Gl.screenTriangle();
        GL33.glBindSampler((int)22, (int)0);
        Gl.bind(22, 0);
        Gl.bind(23, 0);
        this.shadows.bind(false);
        Gl.bind(9, 0);
        Gl.bind(10, 0);
        this.blur.run(frameContext, this.targets.depth, this.targets.gi, this.targets.giFbo, this.targets.giSpare, this.targets.giSpareFbo);
        Gl.bind(12, 0);
        GL13.glActiveTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)0);
        Texture.lastTextureID = -1;
        GL11.glDepthMask((boolean)true);
        GL11.glEnable((int)2929);
    }
}

