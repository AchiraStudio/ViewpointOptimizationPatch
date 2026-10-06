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
import viewpoint.platform.GpuParts;
import viewpoint.render.FrameContext;
import viewpoint.render.SceneData;
import viewpoint.render.SkyPass;
import viewpoint.render.Targets;
import viewpoint.render.WeatherMap;

final class DistantRain {
    private static final int DIRECTIONS = 512;
    private static final int ROWS = 64;
    private static final float LEAST = 0.001f;
    private final GlProgram stretches = GlProgram.create("distant rain stretches", "screen.vert", "distant_rain_table.frag");
    private final GlProgram sum = GlProgram.create("distant rain table", "screen.vert", "distant_rain_sum.frag");
    private final GlProgram veil = GlProgram.create("distant rain", "screen.vert", "distant_rain.frag");
    private final Targets targets;
    private final WeatherMap weatherMap;
    private final int stretchTexture;
    private final int stretchFbo;
    private final int texture;
    private final int fbo;

    DistantRain(Targets targets, WeatherMap weatherMap) {
        this.targets = targets;
        this.weatherMap = weatherMap;
        int n = GL11.glGetInteger((int)32873);
        int n2 = GL11.glGetInteger((int)36006);
        this.stretchTexture = Gl.texture(33325, 512, 64, 6403, 5126, 9728);
        this.texture = Gl.texture(33325, 512, 64, 6403, 5126, 9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)10497);
        GL11.glBindTexture((int)3553, (int)n);
        this.stretchFbo = DistantRain.framebuffer(this.stretchTexture);
        this.fbo = DistantRain.framebuffer(this.texture);
        GL30.glBindFramebuffer((int)36160, (int)n2);
        this.stretches.sampler("uWeatherMap", 14);
        this.sum.sampler("uStretches", 85);
        this.veil.sampler("uDepth", 12);
        this.veil.sampler("uFarDepth", 29);
        this.veil.sampler("uDistantRain", 85);
    }

    private static int framebuffer(int n) {
        int n2 = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer((int)36160, (int)n2);
        GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)3553, (int)n, (int)0);
        return n2;
    }

    static boolean any(SceneData sceneData) {
        for (float f : sceneData.rainAlong) {
            if (!(f > 0.001f)) continue;
            return true;
        }
        return false;
    }

    void draw(FrameContext frameContext) {
        if (!this.weatherMap.drawn(frameContext)) {
            return;
        }
        SceneData sceneData = frameContext.scene;
        GpuParts.begin(24);
        GL30.glBindFramebuffer((int)36160, (int)this.stretchFbo);
        GL11.glViewport((int)0, (int)0, (int)512, (int)64);
        GL11.glDisable((int)2929);
        GL11.glDisable((int)3042);
        this.stretches.use();
        this.stretches.set("uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        WeatherMap.centre(this.stretches, sceneData);
        SkyPass.weatherUniforms(this.stretches, sceneData);
        Gl.bind(14, this.weatherMap.texture());
        Gl.screenTriangle();
        GL30.glBindFramebuffer((int)36160, (int)this.fbo);
        this.sum.use();
        Gl.bind(85, this.stretchTexture);
        Gl.screenTriangle();
        GL30.glBindFramebuffer((int)36160, (int)this.targets.lightFbo);
        GL11.glViewport((int)0, (int)0, (int)frameContext.width, (int)frameContext.height);
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)1, (int)771);
        this.veil.use();
        frameContext.camera(this.veil);
        this.veil.set("uForward", -frameContext.view.m02(), -frameContext.view.m12(), -frameContext.view.m22());
        this.veil.set("uFarRange", frameContext.farNear, frameContext.farFar);
        this.veil.set("uHorizon", sceneData.fogR, sceneData.fogG, sceneData.fogB);
        this.veil.set("uSunDir", sceneData.skySunX, sceneData.skySunY, sceneData.skySunZ);
        this.veil.set("uSunStrength", sceneData.skyGlow);
        SkyPass.fogUniforms(this.veil, sceneData);
        Gl.bind(12, this.targets.depth);
        Gl.bind(29, this.targets.farDepth);
        Gl.bind(85, this.texture);
        Gl.screenTriangle();
        Gl.bind(85, 0);
        Gl.bind(29, 0);
        Gl.bind(12, 0);
        Gl.bind(14, 0);
        GL13.glActiveTexture((int)33984);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glDisable((int)3042);
        GL11.glEnable((int)2929);
        GpuParts.end(24);
    }
}

