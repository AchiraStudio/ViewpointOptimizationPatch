/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL13
 */
package viewpoint.render;

import java.util.IdentityHashMap;
import org.lwjgl.opengl.GL13;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.render.FrameContext;
import viewpoint.render.SceneData;
import viewpoint.render.Targets;

final class FrameUniforms {
    private static final String[] NAMES = new String[]{"uRainStrength", "uSnowStrength", "uWet", "uPuddles", "uMist", "uNight", "uCloudCover", "uOvercast", "uTime", "uFrame", "uDayFraction", "uSunSine", "uMoonPhase", "uEyeSky", "uSceneOrigin", "uHorizon", "uZenith", "uSkySun", "uSkyMoon", "uSkySunColor", "uSkyGlow", "uSkyLevel", "uWind", "uExposureA", "uExposureSize", "uNoiseTex", "uWaterGrid", "uSceneFog", "uShadowDepth0", "uShadowDepth1", "uShadowDepth2"};
    private static final float EYE_EASE = 1.5f;
    private static final IdentityHashMap<GlProgram, Boolean> declares = new IdentityHashMap();
    private static int links = -1;
    private static float eyeSky = -1.0f;

    static void set(FrameContext frameContext, Targets targets) {
        SceneData sceneData = frameContext.scene;
        float f = 1.0f - (float)Math.exp(-frameContext.frameSeconds / 1.5f);
        float f2 = eyeSky = eyeSky < 0.0f ? sceneData.eyeSky : eyeSky + (sceneData.eyeSky - eyeSky) * f;
        if (links != GlProgram.links()) {
            declares.clear();
            links = GlProgram.links();
        }
        targets.uploadGrids(sceneData, frameContext.index);
        GlProgram.forEach(glProgram -> {
            if (declares.computeIfAbsent((GlProgram)glProgram, FrameUniforms::declaresAny).booleanValue()) {
                glProgram.use();
                FrameUniforms.values(glProgram, frameContext);
            }
        });
        FrameUniforms.bind(targets);
    }

    static void bind(Targets targets) {
        Gl.bind(84, targets.noise2d);
        Gl.bind(80, targets.water);
        GL13.glActiveTexture((int)33984);
    }

    private static boolean declaresAny(GlProgram glProgram) {
        for (String string : NAMES) {
            if (!glProgram.has(string)) continue;
            return true;
        }
        return false;
    }

    private static void values(GlProgram glProgram, FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        FrameUniforms.one(glProgram, "uRainStrength", sceneData.rain);
        FrameUniforms.one(glProgram, "uSnowStrength", sceneData.snow);
        FrameUniforms.one(glProgram, "uWet", sceneData.wetGround);
        FrameUniforms.one(glProgram, "uPuddles", sceneData.puddles);
        FrameUniforms.one(glProgram, "uMist", sceneData.mist);
        FrameUniforms.one(glProgram, "uNight", sceneData.night);
        FrameUniforms.one(glProgram, "uCloudCover", sceneData.cloudCover);
        FrameUniforms.one(glProgram, "uOvercast", sceneData.overcast);
        FrameUniforms.one(glProgram, "uTime", sceneData.time);
        FrameUniforms.one(glProgram, "uDayFraction", sceneData.dayFraction);
        FrameUniforms.one(glProgram, "uSunSine", sceneData.sunSine);
        FrameUniforms.one(glProgram, "uEyeSky", eyeSky);
        FrameUniforms.three(glProgram, "uSceneOrigin", (float)sceneData.originX, (float)sceneData.originY, (float)sceneData.originZ);
        FrameUniforms.three(glProgram, "uHorizon", sceneData.fogR, sceneData.fogG, sceneData.fogB);
        FrameUniforms.three(glProgram, "uZenith", sceneData.zenithR, sceneData.zenithG, sceneData.zenithB);
        FrameUniforms.three(glProgram, "uSkySun", sceneData.skySunX, sceneData.skySunY, sceneData.skySunZ);
        FrameUniforms.three(glProgram, "uSkyMoon", sceneData.skyMoonX, sceneData.skyMoonY, sceneData.skyMoonZ);
        FrameUniforms.three(glProgram, "uSkySunColor", sceneData.sunR, sceneData.sunG, sceneData.sunB);
        FrameUniforms.one(glProgram, "uSkyGlow", sceneData.skyGlow);
        FrameUniforms.three(glProgram, "uSkyLevel", sceneData.skyR, sceneData.skyG, sceneData.skyB);
        if (glProgram.has("uWind")) {
            glProgram.set("uWind", sceneData.windX, sceneData.windZ);
        }
        if (glProgram.has("uSceneFog")) {
            glProgram.set("uSceneFog", sceneData.fogStart, sceneData.fogEnd);
        }
        if (glProgram.has("uExposureA")) {
            glProgram.set("uExposureA", sceneData.exposureAX, sceneData.exposureAY);
        }
        FrameUniforms.one(glProgram, "uExposureSize", 64.0f);
        FrameUniforms.integer(glProgram, "uFrame", (int)frameContext.index);
        FrameUniforms.integer(glProgram, "uMoonPhase", sceneData.moonPhase);
        FrameUniforms.integer(glProgram, "uNoiseTex", 84);
        FrameUniforms.integer(glProgram, "uWaterGrid", 80);
        for (int i = 0; i < Gl.UNIT_SHADOW_DEPTH.length; ++i) {
            FrameUniforms.integer(glProgram, "uShadowDepth" + i, Gl.UNIT_SHADOW_DEPTH[i]);
        }
    }

    private static void one(GlProgram glProgram, String string, float f) {
        if (glProgram.has(string)) {
            glProgram.set(string, f);
        }
    }

    private static void three(GlProgram glProgram, String string, float f, float f2, float f3) {
        if (glProgram.has(string)) {
            glProgram.set(string, f, f2, f3);
        }
    }

    private static void integer(GlProgram glProgram, String string, int n) {
        if (glProgram.has(string)) {
            glProgram.setInt(string, n);
        }
    }

    private FrameUniforms() {
    }
}

