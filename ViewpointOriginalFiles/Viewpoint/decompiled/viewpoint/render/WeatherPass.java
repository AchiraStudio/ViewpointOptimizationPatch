/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL14
 *  org.lwjgl.opengl.GL30
 *  zombie.core.textures.Texture
 */
package viewpoint.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.Tuning;
import viewpoint.render.DistantRain;
import viewpoint.render.FlashShadow;
import viewpoint.render.FrameContext;
import viewpoint.render.Meshes;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;
import viewpoint.render.SkyPass;
import viewpoint.render.SurfacePass;
import viewpoint.render.Targets;
import viewpoint.render.WeatherMap;
import zombie.core.textures.Texture;

final class WeatherPass {
    private static final int RAIN_DROPS = 24000;
    private static final int RAIN_DROPS_FAR = 144000;
    private static final int SNOW_FLAKES = 8000;
    private static final int SNOW_FLAKES_FAR = 32000;
    private static final int SPLASHES = 3000;
    static final float PERIOD = 600.0f;
    private static final double ORIGIN_WRAP = 1536.0;
    private static final double WIND_WRAP = 12288.0;
    private final GlProgram puddles = GlProgram.create("puddle", "mesh.vert", "puddle.frag");
    private final GlProgram precip = GlProgram.create("precip", "precip.vert", "precip.frag");
    private final GlProgram over = GlProgram.create("rain over", "screen.vert", "rain_over.frag");
    private final Targets targets;
    private final ShadowPass shadows;
    private final DistantRain distantRain;
    private boolean layer;

    WeatherPass(Targets targets, ShadowPass shadowPass, WeatherMap weatherMap) {
        this.targets = targets;
        this.shadows = shadowPass;
        this.distantRain = new DistantRain(targets, weatherMap);
        this.precip.sampler("uRainMap", 24);
        FlashShadow.sampler(this.precip);
        this.over.sampler("uRain", 0);
        this.puddles.sampler("uTexture", 0);
        this.puddles.sampler("uLight", 5);
    }

    static boolean any(SceneData sceneData) {
        return sceneData.wetGround > 0.001f || sceneData.puddles > 0.001f || WeatherPass.falling(sceneData) || DistantRain.any(sceneData);
    }

    static boolean falling(SceneData sceneData) {
        return sceneData.rain > 0.001f || sceneData.snow > 0.001f;
    }

    static float fallSpeed(SceneData sceneData) {
        return sceneData.snow > sceneData.rain ? Tuning.snowFall : Tuning.rainFall;
    }

    static void air(GlProgram glProgram, SceneData sceneData) {
        if (glProgram.has("uWindOffset")) {
            glProgram.set("uWindOffset", Gl.wrap(sceneData.originX - sceneData.windCarriedX, 12288.0), Gl.wrap(sceneData.originZ - sceneData.windCarriedZ, 12288.0));
        }
        if (glProgram.has("uRainClock")) {
            glProgram.set("uRainClock", sceneData.time % 600.0f);
        }
        if (glProgram.has("uSheets")) {
            glProgram.set("uSheets", sceneData.sheets);
        }
        if (glProgram.has("uGusts")) {
            glProgram.set("uGusts", sceneData.gusts);
        }
        if (glProgram.has("uWeatherNoise")) {
            glProgram.setInt("uWeatherNoise", 84);
        }
    }

    static int drawn(float f, int n, float f2, int n2) {
        return Math.round(f * ((float)n / 100.0f) * (1.0f + f2) * (float)n2);
    }

    void draw(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        boolean bl = (sceneData.wetGround > 0.001f || sceneData.puddles > 0.001f) && frameContext.pipeline.puddles;
        boolean bl2 = WeatherPass.falling(sceneData);
        this.targets.uploadGrids(sceneData, frameContext.index);
        GL13.glActiveTexture((int)33984);
        GL11.glEnable((int)2929);
        GL11.glDepthMask((boolean)false);
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glDisable((int)2884);
        GL11.glDisable((int)3008);
        GL11.glDisable((int)3089);
        if (bl) {
            this.drawPuddles(frameContext, sceneData);
        }
        if (bl2 && this.shadows.rain.on) {
            this.drawFalling(frameContext, sceneData);
        }
        if (DistantRain.any(sceneData)) {
            this.distantRain.draw(frameContext);
        }
        GL11.glBindTexture((int)3553, (int)0);
        Texture.lastTextureID = -1;
        GL11.glDepthMask((boolean)true);
        GL11.glDepthFunc((int)513);
        GL11.glDisable((int)3042);
    }

    private void drawPuddles(FrameContext frameContext, SceneData sceneData) {
        float f = (float)(-sceneData.originX - Math.floor(-sceneData.originX / 1024.0) * 1024.0);
        float f2 = (float)(-sceneData.originZ - Math.floor(-sceneData.originZ / 1024.0) * 1024.0);
        GL11.glDepthFunc((int)515);
        this.puddles.use();
        this.puddles.set("uViewProjection", frameContext.viewProjection);
        this.puddles.set("uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        this.puddles.set("uSunDir", sceneData.skySunX, sceneData.skySunY, sceneData.skySunZ);
        this.puddles.set("uSunColor", sceneData.sunR, sceneData.sunG, sceneData.sunB);
        this.puddles.set("uHorizon", sceneData.fogR, sceneData.fogG, sceneData.fogB);
        this.puddles.set("uZenith", sceneData.zenithR, sceneData.zenithG, sceneData.zenithB);
        this.puddles.set("uSunStrength", sceneData.sunStrength);
        this.puddles.set("uPuddles", sceneData.puddles);
        this.puddles.set("uWet", sceneData.wetGround);
        SkyPass.weatherUniforms(this.puddles, sceneData);
        this.puddles.set("uFadeNoise", frameContext.fadeNoise());
        this.puddles.set("uFogRange", 1000000.0f, 2000000.0f);
        this.puddles.set("uCam", f, f2);
        GL13.glActiveTexture((int)33984);
        Meshes.draw(sceneData, this.puddles, (byte)1, Math.max(0, (int)Math.floor(sceneData.originY / 2.4494895935058594 + 0.01)), false, false);
        GL13.glActiveTexture((int)33984);
    }

    private void drawFalling(FrameContext frameContext, SceneData sceneData) {
        boolean bl = sceneData.snow > sceneData.rain;
        float f = bl ? sceneData.snow : sceneData.rain;
        GL30.glBindFramebuffer((int)36160, (int)this.targets.rainFbo);
        GL11.glViewport((int)0, (int)0, (int)frameContext.width, (int)frameContext.height);
        GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        GL11.glClear((int)16384);
        GL11.glBlendFunc((int)1, (int)771);
        GL11.glDepthFunc((int)513);
        this.precip.use();
        this.precip.set("uViewProjection", frameContext.plainViewProjection);
        this.precip.set("uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        this.precip.set("uOrigin", (float)(sceneData.originX - Math.floor(sceneData.originX / 1536.0) * 1536.0), (float)sceneData.originY, (float)(sceneData.originZ - Math.floor(sceneData.originZ / 1536.0) * 1536.0));
        SkyPass.weatherUniforms(this.precip, sceneData);
        this.precip.set("uFall", WeatherPass.fallSpeed(sceneData));
        this.precip.set("uExposure", Tuning.rainExposure);
        this.precip.set("uPixel", 2.0f / ((float)frameContext.height * sceneData.projection.m11()));
        this.precip.set("uSky", sceneData.fogR, sceneData.fogG, sceneData.fogB);
        this.precip.set("uLampGain", Tuning.rainLampGain);
        this.precip.set("uIntensity", f);
        SurfacePass.lamps(this.precip, sceneData);
        this.shadows.flash.uniforms(this.precip, frameContext);
        Gl.bind(21, this.shadows.flash.on ? this.shadows.flash.texture() : 0);
        this.shadows.rain.uniforms(this.precip, true);
        GL13.glActiveTexture((int)33984);
        this.precip.set("uMode", bl ? 1.0f : 0.0f);
        this.precip.set("uFar", 0.0f);
        this.field(frameContext, f, bl ? 8000 : 24000);
        this.precip.set("uFar", 1.0f);
        this.field(frameContext, f, bl ? 32000 : 144000);
        this.precip.set("uFar", 0.0f);
        if (!bl) {
            this.precip.set("uMode", 2.0f);
            this.field(frameContext, f, 3000);
        }
        this.shadows.rain.uniforms(this.precip, false);
        Gl.bind(21, 0);
        GL13.glActiveTexture((int)33984);
        GL11.glBlendFunc((int)770, (int)771);
        this.layer = true;
    }

    private void field(FrameContext frameContext, float f, int n) {
        int n2 = WeatherPass.drawn(f, frameContext.pipeline.rainDensity, frameContext.scene.sheets, n);
        this.precip.set("uMean", f * ((float)frameContext.pipeline.rainDensity / 100.0f) * (float)n);
        this.precip.set("uCount", n2);
        Gl.generated(n2 * 6);
    }

    void overlay() {
        if (!this.layer) {
            return;
        }
        this.layer = false;
        GL11.glDisable((int)2929);
        GL11.glEnable((int)3042);
        GL14.glBlendFuncSeparate((int)1, (int)771, (int)0, (int)1);
        this.over.use();
        GL13.glActiveTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)this.targets.rain);
        Gl.screenTriangle();
        GL11.glBindTexture((int)3553, (int)0);
        Texture.lastTextureID = -1;
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glDisable((int)3042);
    }
}

