/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlDebug;
import viewpoint.platform.GlProgram;
import viewpoint.platform.GpuParts;
import viewpoint.render.Meshes;
import viewpoint.render.ModelPass;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;
import viewpoint.render.WeatherPass;

final class RainMap {
    static final float RADIUS = 48.0f;
    private static final int SIZE = 2048;
    private static final float HEIGHT = 150.0f;
    private final GlProgram program;
    private final ModelPass models;
    private final int fbo;
    private final int texture;
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f viewProjection = new Matrix4f();
    private final Matrix4f matrix = new Matrix4f();
    private final Matrix4f unproject = new Matrix4f();
    boolean on;

    RainMap(GlProgram glProgram, ModelPass modelPass) {
        this.program = glProgram;
        this.models = modelPass;
        this.texture = Gl.texture(33190, 2048, 2048, 6402, 5126, 9728);
        this.fbo = ShadowPass.depthFbo(this.texture);
    }

    void draw(SceneData sceneData) {
        this.on = WeatherPass.falling(sceneData);
        if (this.on) {
            GlDebug.push("rain map");
            GpuParts.begin(7);
            this.drawMap(sceneData);
            GpuParts.end(7);
            GlDebug.pop();
        }
    }

    private void drawMap(SceneData sceneData) {
        float f = WeatherPass.fallSpeed(sceneData);
        float f2 = sceneData.windX / f;
        float f3 = sceneData.windZ / f;
        float f4 = (float)Math.sqrt(f2 * f2 + 1.0f + f3 * f3);
        float f5 = -1.0f / f4;
        boolean bl = Math.abs(f3 /= f4) > 0.7f;
        this.view.setLookAt(-(f2 /= f4) * 150.0f, -f5 * 150.0f, -f3 * 150.0f, 0.0f, 0.0f, 0.0f, bl ? 1.0f : 0.0f, 0.0f, bl ? 0.0f : 1.0f);
        double d = 0.046875;
        double d2 = (double)this.view.m00() * sceneData.originX + (double)this.view.m10() * sceneData.originY + (double)this.view.m20() * sceneData.originZ;
        double d3 = (double)this.view.m01() * sceneData.originX + (double)this.view.m11() * sceneData.originY + (double)this.view.m21() * sceneData.originZ;
        float f6 = (float)(d2 - Math.floor(d2 / d) * d);
        float f7 = (float)(d3 - Math.floor(d3 / d) * d);
        this.viewProjection.setOrtho(-48.0f - f6, 48.0f - f6, -48.0f - f7, 48.0f - f7, 1.0f, 300.0f).mul((Matrix4fc)this.view);
        this.matrix.translation(0.5f, 0.5f, 0.5f).scale(0.5f).mul((Matrix4fc)this.viewProjection);
        this.matrix.invert(this.unproject);
        GL30.glBindFramebuffer((int)36160, (int)this.fbo);
        GL11.glViewport((int)0, (int)0, (int)2048, (int)2048);
        GL11.glClear((int)256);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)513);
        this.program.use();
        this.program.set("uAlphaRange", 0.3f, 2.0f);
        this.program.set("uFadeNoise", 0.0f);
        this.program.set("uViewProjection", this.viewProjection);
        float f8 = 100000.0f;
        this.program.set("uEye", -f2 * f8, -f5 * f8, -f3 * f8);
        Meshes.draw(sceneData, this.program, (byte)64);
        this.models.rain(sceneData, this.viewProjection, 52.0f);
        this.program.use();
    }

    void uniforms(GlProgram glProgram, boolean bl) {
        Gl.bind(24, bl ? this.texture : 0);
        if (bl) {
            glProgram.set("uRainMatrix", this.matrix);
            glProgram.set("uRainUnproject", this.unproject);
        }
    }
}

