/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.render.FrameContext;
import viewpoint.render.SceneData;
import viewpoint.render.SkyPass;

final class WeatherMap {
    static final int SIZE = 512;
    private static final double TEXEL = 64.0;
    private static final double SPAN = 32768.0;
    private static final double NOISE_PERIOD = 131072.0;
    private static final double CLOUD_PERIOD = 1280.0;
    private static final FloatBuffer ALONG = BufferUtils.createFloatBuffer((int)97);
    private final GlProgram program = GlProgram.create("weather map", "screen.vert", "weather_map.frag");
    private final int texture;
    private final int fbo;
    private final int cloudNoise;
    private long drawnFrame = -1L;

    WeatherMap(int n) {
        this.cloudNoise = n;
        int n2 = GL11.glGetInteger((int)32873);
        int n3 = GL11.glGetInteger((int)36006);
        this.texture = Gl.texture(32856, 512, 512, 6408, 5121, 9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33648);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33648);
        GL11.glBindTexture((int)3553, (int)n2);
        this.fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer((int)36160, (int)this.fbo);
        GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)3553, (int)this.texture, (int)0);
        GL30.glBindFramebuffer((int)36160, (int)n3);
        this.program.sampler("uCloudNoise", 18);
    }

    void draw(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        GL30.glBindFramebuffer((int)36160, (int)this.fbo);
        GL11.glViewport((int)0, (int)0, (int)512, (int)512);
        GL11.glDisable((int)2929);
        GL11.glDisable((int)3042);
        this.program.use();
        WeatherMap.centre(this.program, sceneData);
        this.program.set("uCloudTime", sceneData.cloudTime);
        SkyPass.weatherUniforms(this.program, sceneData);
        this.bindNoise(true);
        Gl.screenTriangle();
        this.bindNoise(false);
        this.drawnFrame = frameContext.index;
    }

    boolean drawn(FrameContext frameContext) {
        return this.drawnFrame == frameContext.index;
    }

    int texture() {
        return this.texture;
    }

    void bind(boolean bl) {
        Gl.bind(14, bl ? this.texture : 0);
        this.bindNoise(bl);
    }

    private void bindNoise(boolean bl) {
        GL13.glActiveTexture((int)34002);
        GL11.glBindTexture((int)32879, (int)(bl ? this.cloudNoise : 0));
        GL13.glActiveTexture((int)33984);
    }

    static void samplers(GlProgram glProgram) {
        glProgram.sampler("uCloudNoise", 18);
        glProgram.sampler("uWeatherMap", 14);
    }

    static void centre(GlProgram glProgram, SceneData sceneData) {
        glProgram.set("uMapCentre", Gl.wrap(WeatherMap.centre(WeatherMap.carriedX(sceneData)), 131072.0), Gl.wrap(WeatherMap.centre(WeatherMap.carriedZ(sceneData)), 131072.0));
    }

    static void placement(GlProgram glProgram, SceneData sceneData) {
        double d = WeatherMap.carriedX(sceneData);
        double d2 = WeatherMap.carriedZ(sceneData);
        if (glProgram.has("uWeatherOffset")) {
            glProgram.set("uWeatherOffset", (float)(d - WeatherMap.centre(d)), (float)(d2 - WeatherMap.centre(d2)));
        }
        if (glProgram.has("uWeatherSpan")) {
            glProgram.set("uWeatherSpan", 32768.0f);
        }
    }

    static void along(GlProgram glProgram, SceneData sceneData) {
        if (glProgram.has("uCoverAlong")) {
            ALONG.clear().put(sceneData.coverAlong).flip();
            glProgram.setFloats("uCoverAlong", ALONG);
        }
        if (glProgram.has("uTypeAlong")) {
            ALONG.clear().put(sceneData.typeAlong).flip();
            glProgram.setFloats("uTypeAlong", ALONG);
        }
        if (glProgram.has("uAlongStep")) {
            glProgram.set("uAlongStep", 512.0f);
        }
        if (glProgram.has("uRainAlong")) {
            ALONG.clear().put(sceneData.rainAlong).flip();
            glProgram.setFloats("uRainAlong", ALONG);
        }
    }

    static float cloudOffsetX(SceneData sceneData) {
        return Gl.wrap(WeatherMap.carriedX(sceneData), 1280.0);
    }

    static float cloudOffsetZ(SceneData sceneData) {
        return Gl.wrap(WeatherMap.carriedZ(sceneData), 1280.0);
    }

    private static double carriedX(SceneData sceneData) {
        return sceneData.originX - sceneData.cloudCarriedX;
    }

    private static double carriedZ(SceneData sceneData) {
        return sceneData.originZ - sceneData.cloudCarriedZ;
    }

    private static double centre(double d) {
        return Math.floor(d / 64.0 + 0.5) * 64.0;
    }
}

