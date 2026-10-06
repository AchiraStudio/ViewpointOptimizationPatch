/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL30
 *  zombie.core.textures.Texture
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.render.FlashShadow;
import viewpoint.render.FrameContext;
import viewpoint.render.LampShadows;
import viewpoint.render.Meshes;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;
import viewpoint.render.Targets;
import viewpoint.render.WorldRenderer;
import zombie.core.textures.Texture;

final class SurfacePass {
    static final float OPAQUE_ALPHA = 0.85f;
    static final float TRANSLUCENT_ALPHA = 0.04f;
    private static final double GLASS_GRID = 64.0;
    private static final FloatBuffer lightData = BufferUtils.createFloatBuffer((int)192);
    private static final FloatBuffer blobData = BufferUtils.createFloatBuffer((int)128);
    private final GlProgram gbuffer = GlProgram.create("gbuffer", "mesh.vert", "gbuffer.frag");
    private final GlProgram light = GlProgram.create("light", "screen.vert", "light.frag");
    private final GlProgram forward = GlProgram.create("forward", "mesh.vert", "forward.frag");
    private final Targets targets;
    private final ShadowPass shadows;

    SurfacePass(Targets targets, ShadowPass shadowPass) {
        this.targets = targets;
        this.shadows = shadowPass;
        this.gbuffer.sampler("uTexture", 0);
        this.gbuffer.sampler("uLight", 5);
        this.forward.sampler("uTexture", 0);
        this.forward.sampler("uLight", 5);
        ShadowPass.samplers(this.forward);
        FlashShadow.sampler(this.forward);
        LampShadows.sampler(this.forward);
        this.light.sampler("uAlbedo", 9);
        this.light.sampler("uNormal", 10);
        this.light.sampler("uEngineLight", 11);
        this.light.sampler("uDepth", 12);
        ShadowPass.samplers(this.light);
        FlashShadow.sampler(this.light);
        LampShadows.sampler(this.light);
    }

    void gbuffer(FrameContext frameContext) {
        GL30.glBindFramebuffer((int)36160, (int)this.targets.gbufferFbo);
        GL11.glViewport((int)0, (int)0, (int)frameContext.width, (int)frameContext.height);
        GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        GL11.glClear((int)17664);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)513);
        this.gbuffer.use();
        this.gbuffer.set("uViewProjection", frameContext.viewProjection);
        this.gbuffer.set("uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        this.gbuffer.set("uAlphaRange", 0.85f, 2.0f);
        this.gbuffer.set("uFadeNoise", frameContext.fadeNoise());
        this.gbuffer.set("uSkyLevel", frameContext.scene.skyR, frameContext.scene.skyG, frameContext.scene.skyB);
        SurfacePass.glass(this.gbuffer, frameContext.scene);
        GL11.glEnable((int)2960);
        GL11.glStencilFunc((int)519, (int)1, (int)255);
        GL11.glStencilOp((int)7680, (int)7680, (int)7681);
        Meshes.drawPlanned(frameContext.scene, this.gbuffer, (byte)1, true);
        GL11.glDisable((int)2960);
    }

    void light(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        this.light.use();
        frameContext.camera(this.light);
        this.shadows.sunUniforms(this.light, frameContext);
        this.shadows.flash.uniforms(this.light, frameContext);
        this.light.set("uFogColor", sceneData.fogR, sceneData.fogG, sceneData.fogB);
        this.light.setInt("uDebugView", WorldRenderer.debugView);
        SurfacePass.lamps(this.light, sceneData);
        this.shadows.lamps.uniforms(this.light, sceneData);
        SurfacePass.torches(this.light, sceneData);
        SurfacePass.blobs(this.light, sceneData);
        Gl.bind(9, this.targets.albedo);
        Gl.bind(10, this.targets.normal);
        Gl.bind(11, this.targets.engineLight);
        Gl.bind(12, this.targets.depth);
        this.shadows.bind(true);
        GL13.glActiveTexture((int)33984);
        GL11.glDisable((int)2929);
        GL11.glDepthMask((boolean)false);
        Gl.screenTriangle();
        Gl.bind(9, 0);
        Gl.bind(10, 0);
        Gl.bind(11, 0);
        Gl.bind(12, 0);
        this.shadows.bind(false);
        GL13.glActiveTexture((int)33984);
        GL11.glDepthMask((boolean)true);
        GL11.glEnable((int)2929);
    }

    void translucent(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)515);
        GL11.glDepthMask((boolean)false);
        GL11.glDisable((int)2884);
        GL11.glDisable((int)3008);
        GL11.glDisable((int)3089);
        this.forward.use();
        this.forward.set("uViewProjection", frameContext.viewProjection);
        this.forward.set("uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        this.shadows.sunUniforms(this.forward, frameContext);
        SurfacePass.lamps(this.forward, sceneData);
        this.shadows.lamps.uniforms(this.forward, sceneData);
        SurfacePass.torches(this.forward, sceneData);
        this.shadows.flash.uniforms(this.forward, frameContext);
        this.forward.set("uFogColor", sceneData.fogR, sceneData.fogG, sceneData.fogB);
        this.forward.set("uFogRange", 1000000.0f, 2000000.0f);
        this.forward.setInt("uDebugView", WorldRenderer.debugView);
        this.forward.set("uAlphaRange", 0.04f, 0.85f);
        this.forward.set("uFadeNoise", frameContext.fadeNoise());
        this.forward.set("uSkyLevel", sceneData.skyR, sceneData.skyG, sceneData.skyB);
        SurfacePass.glass(this.forward, sceneData);
        this.shadows.bind(true);
        GL13.glActiveTexture((int)33984);
        Texture.lastTextureID = -1;
        Meshes.drawPlanned(sceneData, this.forward, (byte)4, true);
        this.shadows.bind(false);
        GL13.glActiveTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)0);
        Texture.lastTextureID = -1;
        GL11.glDepthMask((boolean)true);
        GL11.glDepthFunc((int)513);
    }

    static void lamps(GlProgram glProgram, SceneData sceneData) {
        int n;
        int n2;
        int n3 = WorldRenderer.off(3) ? 0 : Math.min(sceneData.lightCount, 48);
        glProgram.setInt("uLightCount", n3);
        if (n3 == 0) {
            return;
        }
        lightData.clear();
        for (n2 = 0; n2 < n3; ++n2) {
            n = n2 * 10;
            lightData.put(sceneData.lights[n]).put(sceneData.lights[n + 1]).put(sceneData.lights[n + 2]).put(sceneData.lights[n + 3]);
        }
        lightData.flip();
        glProgram.setVec4s("uLightPos", lightData);
        lightData.clear();
        for (n2 = 0; n2 < n3; ++n2) {
            n = n2 * 10;
            float f = sceneData.lights[n + 7];
            lightData.put(sceneData.lights[n + 4] * f).put(sceneData.lights[n + 5] * f).put(sceneData.lights[n + 6] * f);
        }
        lightData.flip();
        glProgram.setVec3s("uLightColor", lightData);
        if (glProgram.has("uLightOutdoors")) {
            lightData.clear();
            for (n2 = 0; n2 < n3; ++n2) {
                lightData.put(sceneData.lights[n2 * 10 + 8]);
            }
            lightData.flip();
            glProgram.setFloats("uLightOutdoors", lightData);
        }
    }

    static void glass(GlProgram glProgram, SceneData sceneData) {
        glProgram.setInt("uGlassFar", sceneData.glassFar ? 1 : 0);
        glProgram.set("uGlassRange", sceneData.glassStart, sceneData.glassOpaque, sceneData.glassStoreys, 0.04f);
        glProgram.set("uGlassEye", sceneData.viewerX, sceneData.viewerY, sceneData.viewerZ);
        glProgram.set("uGlassOrigin", Gl.wrap(sceneData.originX, 64.0), Gl.wrap(sceneData.originY, 156.767333984375), Gl.wrap(sceneData.originZ, 64.0));
    }

    private static void blobs(GlProgram glProgram, SceneData sceneData) {
        if (!glProgram.has("uBlobCount")) {
            return;
        }
        glProgram.setInt("uBlobCount", sceneData.blobCount);
        blobData.clear();
        blobData.put(sceneData.blobs, 0, sceneData.blobCount * 4).flip();
        if (sceneData.blobCount > 0) {
            glProgram.setVec4s("uBlobs", blobData);
        }
    }

    static void torches(GlProgram glProgram, SceneData sceneData) {
        int n;
        int n2;
        int n3 = Math.min(sceneData.torchCount, 8);
        glProgram.setInt("uTorchCount", n3);
        if (n3 == 0) {
            return;
        }
        for (n2 = 0; n2 < 2; ++n2) {
            lightData.clear();
            for (n = 0; n < n3; ++n) {
                int n4 = n * 11 + n2 * 4;
                lightData.put(sceneData.torches[n4]).put(sceneData.torches[n4 + 1]).put(sceneData.torches[n4 + 2]).put(sceneData.torches[n4 + 3]);
            }
            lightData.flip();
            glProgram.setVec4s(n2 == 0 ? "uTorchPos" : "uTorchDir", lightData);
        }
        lightData.clear();
        for (n2 = 0; n2 < n3; ++n2) {
            n = n2 * 11 + 8;
            lightData.put(sceneData.torches[n]).put(sceneData.torches[n + 1]).put(sceneData.torches[n + 2]);
        }
        lightData.flip();
        glProgram.setVec3s("uTorchColor", lightData);
    }
}

