/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.render.FrameContext;
import viewpoint.render.SceneData;
import viewpoint.render.Targets;
import viewpoint.render.WeatherMap;
import viewpoint.render.WeatherPass;
import viewpoint.render.WorldRenderer;

final class SkyPass {
    private static final double OLD_CLOUD_PERIOD = 1100.0;
    private static final double OLD_DRIFT_SQUARES = 440.0;
    private final GlProgram clouds = GlProgram.create("clouds", "screen.vert", "cloud.frag");
    private final GlProgram sky = GlProgram.create("sky", "sky.vert", "sky.frag");
    private final Targets targets;
    private final WeatherMap weatherMap;

    SkyPass(Targets targets, WeatherMap weatherMap) {
        this.targets = targets;
        this.weatherMap = weatherMap;
        WeatherMap.samplers(this.clouds);
        this.sky.sampler("uClouds", 17);
    }

    void clouds(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        Matrix4f matrix4f = frameContext.view;
        GL30.glBindFramebuffer((int)36160, (int)this.targets.cloudFbo);
        GL11.glViewport((int)0, (int)0, (int)this.targets.cloudWidth, (int)this.targets.cloudHeight);
        GL11.glDisable((int)2929);
        GL11.glDisable((int)3042);
        GL11.glDepthMask((boolean)false);
        this.clouds.use();
        float f = 1.0f / sceneData.projection.m00();
        float f2 = 1.0f / sceneData.projection.m11();
        this.clouds.set("uRight", matrix4f.m00() * f, matrix4f.m10() * f, matrix4f.m20() * f);
        this.clouds.set("uUp", matrix4f.m01() * f2, matrix4f.m11() * f2, matrix4f.m21() * f2);
        this.clouds.set("uForward", -matrix4f.m02(), -matrix4f.m12(), -matrix4f.m22());
        this.clouds.set("uHorizon", sceneData.fogR, sceneData.fogG, sceneData.fogB);
        this.clouds.set("uZenith", sceneData.zenithR, sceneData.zenithG, sceneData.zenithB);
        this.clouds.set("uSunDir", sceneData.skySunX, sceneData.skySunY, sceneData.skySunZ);
        this.clouds.set("uSunColor", sceneData.sunR, sceneData.sunG, sceneData.sunB);
        this.clouds.set("uSunStrength", sceneData.skyGlow);
        this.clouds.set("uPixelAngle", 2.0f * f2 / (float)this.targets.cloudHeight);
        this.clouds.set("uNight", sceneData.night);
        if (this.clouds.has("uCloudPhase")) {
            this.clouds.set("uCloudPhase", WorldRenderer.off(1) ? 0.0f : (float)frameContext.jitterPhase);
        }
        SkyPass.eye(this.clouds, frameContext);
        SkyPass.cloudUniforms(this.clouds, sceneData);
        SkyPass.weatherUniforms(this.clouds, sceneData);
        SkyPass.oldCloudNames(this.clouds, sceneData);
        this.weatherMap.bind(true);
        Gl.screenTriangle();
        this.weatherMap.bind(false);
        GL11.glDepthMask((boolean)true);
    }

    void sky(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        Matrix4f matrix4f = frameContext.view;
        GL11.glDisable((int)2929);
        GL11.glDepthMask((boolean)false);
        this.sky.use();
        float f = 1.0f / sceneData.projection.m00();
        float f2 = 1.0f / sceneData.projection.m11();
        this.sky.set("uRight", matrix4f.m00() * f, matrix4f.m10() * f, matrix4f.m20() * f);
        this.sky.set("uUp", matrix4f.m01() * f2, matrix4f.m11() * f2, matrix4f.m21() * f2);
        this.sky.set("uForward", -matrix4f.m02(), -matrix4f.m12(), -matrix4f.m22());
        this.sky.set("uHorizon", sceneData.fogR, sceneData.fogG, sceneData.fogB);
        this.sky.set("uZenith", sceneData.zenithR, sceneData.zenithG, sceneData.zenithB);
        this.sky.set("uSunDir", sceneData.skySunX, sceneData.skySunY, sceneData.skySunZ);
        this.sky.set("uSunColor", sceneData.sunR, sceneData.sunG, sceneData.sunB);
        this.sky.set("uSunStrength", sceneData.skyGlow);
        this.sky.set("uCloudCover", sceneData.cloudCover);
        this.sky.set("uNight", sceneData.night);
        this.sky.set("uTime", sceneData.time);
        this.sky.set("uCloudDrift", SkyPass.oldDriftX(sceneData), SkyPass.oldDriftZ(sceneData));
        this.sky.set("uCloudsOn", WorldRenderer.off(6) ? 0.0f : 1.0f);
        SkyPass.weatherUniforms(this.sky, sceneData);
        SkyPass.eye(this.sky, frameContext);
        Gl.bind(17, this.targets.clouds);
        GL13.glActiveTexture((int)33984);
        Gl.screenTriangle();
        Gl.bind(17, 0);
        GL13.glActiveTexture((int)33984);
        GL11.glDepthMask((boolean)true);
    }

    private static void eye(GlProgram glProgram, FrameContext frameContext) {
        if (glProgram.has("uEye")) {
            glProgram.set("uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        }
    }

    static void cloudUniforms(GlProgram glProgram, SceneData sceneData) {
        if (glProgram.has("uCloudCover")) {
            glProgram.set("uCloudCover", sceneData.cloudCover);
        }
        if (glProgram.has("uCloudTime")) {
            glProgram.set("uCloudTime", sceneData.cloudTime);
        }
        if (glProgram.has("uCloudOffset")) {
            glProgram.set("uCloudOffset", WeatherMap.cloudOffsetX(sceneData), WeatherMap.cloudOffsetZ(sceneData));
        }
        if (glProgram.has("uCloudOrigin")) {
            glProgram.set("uCloudOrigin", Gl.wrap(sceneData.originX, 1100.0), Gl.wrap(sceneData.originZ, 1100.0));
        }
        if (glProgram.has("uCloudDrift")) {
            glProgram.set("uCloudDrift", SkyPass.oldDriftX(sceneData), SkyPass.oldDriftZ(sceneData));
        }
    }

    private static float oldDriftX(SceneData sceneData) {
        return (float)(-sceneData.cloudCarriedX / 440.0);
    }

    private static float oldDriftZ(SceneData sceneData) {
        return (float)(-sceneData.cloudCarriedZ / 440.0);
    }

    static void weatherUniforms(GlProgram glProgram, SceneData sceneData) {
        if (glProgram.has("uRain")) {
            glProgram.set("uRain", sceneData.rain);
        }
        if (glProgram.has("uSnow")) {
            glProgram.set("uSnow", sceneData.snow);
        }
        if (glProgram.has("uWind")) {
            glProgram.set("uWind", sceneData.windX, sceneData.windZ);
        }
        if (glProgram.has("uCloudType")) {
            glProgram.set("uCloudType", sceneData.cloudType);
        }
        if (glProgram.has("uCloudWind")) {
            glProgram.set("uCloudWind", sceneData.cloudWindX, sceneData.cloudWindZ);
        }
        if (glProgram.has("uCloudCarried")) {
            glProgram.set("uCloudCarried", (float)sceneData.cloudCarriedX, (float)sceneData.cloudCarriedZ);
        }
        if (glProgram.has("uLightning")) {
            glProgram.set("uLightning", sceneData.lightningX, sceneData.lightningY, sceneData.lightningZ, sceneData.lightning);
        }
        WeatherMap.placement(glProgram, sceneData);
        WeatherMap.along(glProgram, sceneData);
        WeatherPass.air(glProgram, sceneData);
    }

    private static void oldCloudNames(GlProgram glProgram, SceneData sceneData) {
        if (glProgram.has("uNoise")) {
            glProgram.setInt("uNoise", 18);
        }
        if (glProgram.has("uTime")) {
            glProgram.set("uTime", sceneData.cloudTime);
        }
    }

    static void fogUniforms(GlProgram glProgram, SceneData sceneData) {
        SkyPass.weatherUniforms(glProgram, sceneData);
        if (glProgram.has("uZenith")) {
            glProgram.set("uZenith", sceneData.zenithR, sceneData.zenithG, sceneData.zenithB);
        }
        if (glProgram.has("uSkySun")) {
            glProgram.set("uSkySun", sceneData.skySunX, sceneData.skySunY, sceneData.skySunZ);
        }
        if (glProgram.has("uSkySunColor")) {
            glProgram.set("uSkySunColor", sceneData.sunR, sceneData.sunG, sceneData.sunB);
        }
        if (glProgram.has("uSkyGlow")) {
            glProgram.set("uSkyGlow", sceneData.skyGlow);
        }
    }
}

