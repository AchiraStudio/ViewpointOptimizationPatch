/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.GlProgram;
import viewpoint.platform.Profile;
import viewpoint.render.CascadeLayer;
import viewpoint.render.FarShadow;
import viewpoint.render.Meshes;
import viewpoint.render.ModelPass;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;

final class CascadeLayers {
    static final int COUNT = 2;
    private final CascadeLayer[] layers = new CascadeLayer[]{new CascadeLayer(), new CascadeLayer()};
    private final GlProgram program;
    private final ModelPass models;
    private final FarShadow farMaps;
    private long farKey;
    private final long[] drawnLayers = new long[2];
    private long frames;

    CascadeLayers(GlProgram glProgram, ModelPass modelPass, FarShadow farShadow) {
        this.program = glProgram;
        this.models = modelPass;
        this.farMaps = farShadow;
        Profile.cascadeLayersReport = this::report;
    }

    void frame(SceneData sceneData, int n) {
        ++this.frames;
        this.farKey = this.farMaps.castersKey(sceneData);
        for (int i = n; i < 2; ++i) {
            this.layers[i].release();
        }
    }

    void draw(SceneData sceneData, int n, byte by, float f, Cascade cascade, Matrix4f matrix4f) {
        CascadeLayer cascadeLayer = this.layers[n];
        long l = ShadowPass.casters(sceneData, by) * 31L + this.farKey;
        if (!cascadeLayer.holds(sceneData, cascade.size, f, cascade.sunDistance, l)) {
            cascadeLayer.aim(sceneData, cascade.size, f, cascade.lightView, cascade.sunDistance, l, matrix4f);
            cascadeLayer.begin();
            this.program.set("uViewProjection", matrix4f);
            this.program.set("uFadeNoise", -CascadeLayer.margin(cascade.size));
            Meshes.draw(sceneData, this.program, by);
            this.program.set("uFadeNoise", 0.0f);
            this.farMaps.castInto(sceneData, matrix4f, n, (float)CascadeLayer.marginSquares(cascade.size, f));
            this.program.use();
            int n2 = n;
            this.drawnLayers[n2] = this.drawnLayers[n2] + 1L;
        }
        cascadeLayer.cascade(sceneData, matrix4f);
        cascadeLayer.copyTo(cascade.texture);
        GL30.glBindFramebuffer((int)36160, (int)cascade.fbo);
        GL11.glViewport((int)0, (int)0, (int)cascade.size, (int)cascade.size);
        float f2 = n == 0 ? 0.0f : 13.200001f;
        float f3 = (n == 0 ? 22.0f : 64.0f) + 6.0f;
        this.models.shadow(sceneData, matrix4f, f2, f3);
        this.program.use();
    }

    void release() {
        for (CascadeLayer cascadeLayer : this.layers) {
            cascadeLayer.release();
        }
    }

    private String report() {
        double d = Math.max(this.frames, 1L);
        String string = String.format("drawn/frame near %.3f, mid %.3f", (double)this.drawnLayers[0] / d, (double)this.drawnLayers[1] / d);
        this.frames = 0L;
        this.drawnLayers[0] = 0L;
        this.drawnLayers[1] = 0L;
        return string;
    }

    static final class Cascade {
        int texture;
        int fbo;
        int size;
        float sunDistance;
        Matrix4f lightView;

        Cascade() {
        }
    }
}

