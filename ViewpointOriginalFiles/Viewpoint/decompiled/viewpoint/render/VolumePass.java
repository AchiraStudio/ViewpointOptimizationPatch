/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.Tuning;
import viewpoint.render.BlurPass;
import viewpoint.render.FlashShadow;
import viewpoint.render.FrameContext;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;
import viewpoint.render.SurfacePass;
import viewpoint.render.Targets;
import viewpoint.render.WorldRenderer;

final class VolumePass {
    private final GlProgram volume = GlProgram.create("volume", "screen.vert", "volume.frag");
    private final Targets targets;
    private final ShadowPass shadows;
    private final BlurPass blur;

    VolumePass(Targets targets, ShadowPass shadowPass, BlurPass blurPass) {
        this.targets = targets;
        this.shadows = shadowPass;
        this.blur = blurPass;
        this.volume.sampler("uDepth", 12);
        this.volume.sampler("uNoise", 18);
        this.volume.sampler("uExposure", 8);
        this.volume.sampler("uFarDepth", 29);
        ShadowPass.samplers(this.volume);
        FlashShadow.sampler(this.volume);
    }

    void draw(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        boolean bl = WorldRenderer.off(4);
        GL30.glBindFramebuffer((int)36160, (int)this.targets.volumeFbo);
        GL11.glViewport((int)0, (int)0, (int)frameContext.halfWidth, (int)frameContext.halfHeight);
        GL11.glDisable((int)2929);
        GL11.glDisable((int)3042);
        GL11.glDepthMask((boolean)false);
        this.volume.use();
        frameContext.camera(this.volume);
        this.shadows.sunUniforms(this.volume, frameContext);
        this.volume.set("uFogColor", sceneData.fogR, sceneData.fogG, sceneData.fogB);
        this.volume.set("uFogRange", sceneData.fogStart, sceneData.nearEnd());
        this.volume.set("uFarOn", frameContext.farFar > 0.0f ? 1.0f : 0.0f);
        this.volume.set("uFloorY", frameContext.floorY());
        this.volume.set("uVolumeSun", bl ? 0.0f : Tuning.volumeSun);
        this.volume.set("uGlowDir", sceneData.skySunX, sceneData.skySunY, sceneData.skySunZ);
        this.volume.set("uDensity", Tuning.airDensity + Tuning.mistDensity * sceneData.mist);
        this.volume.set("uClearDensity", Tuning.airDensity);
        this.volume.set("uDust", Tuning.dust);
        this.volume.set("uAmbientHaze", Tuning.ambientHaze);
        this.volume.set("uLampHalo", bl || WorldRenderer.off(3) ? 0.0f : Tuning.lampHalo);
        this.volume.set("uTime", sceneData.time);
        this.volume.set("uFrameNoise", frameContext.jitterPhase);
        this.volume.set("uWind", sceneData.windX, sceneData.windZ);
        this.volume.set("uNoiseOrigin", Gl.wrap(sceneData.originX, 48.0), Gl.wrap(sceneData.originY, 48.0), Gl.wrap(sceneData.originZ, 48.0));
        this.volume.set("uExposureA", sceneData.exposureAX, sceneData.exposureAY);
        this.volume.set("uExposureSize", 64.0f);
        SurfacePass.lamps(this.volume, sceneData);
        this.shadows.flash.uniforms(this.volume, frameContext);
        this.volume.set("uFlashScatter", bl ? 0.0f : Tuning.flashScatter);
        this.targets.uploadGrids(sceneData, frameContext.index);
        Gl.bind(8, this.targets.exposure);
        GL13.glActiveTexture((int)34002);
        GL11.glBindTexture((int)32879, (int)this.targets.noise);
        Gl.bind(12, this.targets.depth);
        Gl.bind(29, this.targets.farDepth);
        this.shadows.bind(true);
        GL13.glActiveTexture((int)33984);
        Gl.screenTriangle();
        Gl.bind(12, 0);
        Gl.bind(29, 0);
        Gl.bind(8, 0);
        GL13.glActiveTexture((int)34002);
        GL11.glBindTexture((int)32879, (int)0);
        this.shadows.bind(false);
        this.blur.run(frameContext, this.targets.depth, this.targets.volume, this.targets.volumeFbo, this.targets.volumeSpare, this.targets.volumeSpareFbo);
        GL11.glBindTexture((int)3553, (int)0);
        Gl.bind(12, 0);
        GL13.glActiveTexture((int)33984);
    }
}

