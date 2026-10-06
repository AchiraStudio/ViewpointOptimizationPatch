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
import viewpoint.platform.Tuning;
import viewpoint.render.FrameContext;
import viewpoint.render.Meshes;
import viewpoint.render.ModelPass;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;
import viewpoint.render.WorldRenderer;

final class FlashShadow {
    private static final int SIZE = 2048;
    private static final float[] COLOR = new float[]{1.0f, 0.8235294f, 0.7058824f};
    private static final byte CONE_MESH = 32;
    private final GlProgram program;
    private final ModelPass models;
    private final int fbo;
    private final int texture;
    boolean on;
    private float x;
    private float y;
    private float z;
    private float dirX;
    private float dirY;
    private float dirZ;
    private float texel;
    private float reach;
    private float half;
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f viewProjection = new Matrix4f();
    private final Matrix4f matrix = new Matrix4f();

    FlashShadow(GlProgram glProgram, ModelPass modelPass) {
        this.program = glProgram;
        this.models = modelPass;
        this.texture = ShadowPass.depthTexture(2048);
        this.fbo = ShadowPass.depthFbo(this.texture);
    }

    void draw(FrameContext frameContext) {
        boolean bl = this.on = frameContext.scene.flashOn && !WorldRenderer.off(7);
        if (this.on) {
            this.aim(frameContext);
        }
        if (this.on && frameContext.pipeline.flashlightShadow) {
            GlDebug.push("flashlight shadow");
            GpuParts.begin(6);
            this.drawMap(frameContext.scene);
            GpuParts.end(6);
            GlDebug.pop();
        }
    }

    private void aim(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        boolean bl = WorldRenderer.handFromPlayer;
        Matrix4f matrix4f = bl ? WorldRenderer.handView : frameContext.view;
        float f = bl ? -(matrix4f.m00() * matrix4f.m30() + matrix4f.m01() * matrix4f.m31() + matrix4f.m02() * matrix4f.m32()) : frameContext.eyeX;
        float f2 = bl ? -(matrix4f.m10() * matrix4f.m30() + matrix4f.m11() * matrix4f.m31() + matrix4f.m12() * matrix4f.m32()) : frameContext.eyeY;
        float f3 = bl ? -(matrix4f.m20() * matrix4f.m30() + matrix4f.m21() * matrix4f.m31() + matrix4f.m22() * matrix4f.m32()) : frameContext.eyeZ;
        float f4 = -matrix4f.m02();
        float f5 = -matrix4f.m12();
        float f6 = -matrix4f.m22();
        this.x = f + matrix4f.m00() * Tuning.handRight - matrix4f.m01() * Tuning.handDown + f4 * Tuning.handForward;
        this.y = f2 + matrix4f.m10() * Tuning.handRight - matrix4f.m11() * Tuning.handDown + f5 * Tuning.handForward;
        this.z = f3 + matrix4f.m20() * Tuning.handRight - matrix4f.m21() * Tuning.handDown + f6 * Tuning.handForward;
        float f7 = f + f4 * Tuning.flashAim - this.x;
        float f8 = f2 + f5 * Tuning.flashAim - this.y;
        float f9 = f3 + f6 * Tuning.flashAim - this.z;
        float f10 = (float)Math.sqrt(f7 * f7 + f8 * f8 + f9 * f9);
        this.dirX = f7 / f10;
        this.dirY = f8 / f10;
        this.dirZ = f9 / f10;
        this.reach = sceneData.flashRange;
        this.half = Math.min((float)Math.acos(sceneData.flashCos) + 0.08f, 1.3f);
        this.texel = 2.0f * (float)Math.tan(this.half) / 2048.0f;
        boolean bl2 = Math.abs(this.dirY) > 0.99f;
        this.view.setLookAt(this.x, this.y, this.z, this.x + this.dirX, this.y + this.dirY, this.z + this.dirZ, 0.0f, bl2 ? 0.0f : 1.0f, bl2 ? 1.0f : 0.0f);
        this.viewProjection.setPerspective(2.0f * this.half, 1.0f, 0.05f, this.reach + 1.0f).mul((Matrix4fc)this.view);
        this.matrix.translation(0.5f, 0.5f, 0.5f).scale(0.5f).mul((Matrix4fc)this.viewProjection);
    }

    private void drawMap(SceneData sceneData) {
        this.markConeMeshes(sceneData);
        GL30.glBindFramebuffer((int)36160, (int)this.fbo);
        GL11.glViewport((int)0, (int)0, (int)2048, (int)2048);
        GL11.glClear((int)256);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)513);
        GL11.glEnable((int)32823);
        GL11.glPolygonOffset((float)1.5f, (float)3.0f);
        this.program.use();
        this.program.set("uAlphaRange", 0.3f, 2.0f);
        this.program.set("uFadeNoise", 0.0f);
        this.program.set("uViewProjection", this.viewProjection);
        this.program.set("uEye", this.x, this.y, this.z);
        Meshes.draw(sceneData, this.program, (byte)32);
        this.models.shadow(sceneData, this.viewProjection, 0.5f, this.reach + 1.0f);
        this.program.use();
        GL11.glDisable((int)32823);
    }

    private void markConeMeshes(SceneData sceneData) {
        float f = (float)Math.sin(this.half);
        float f2 = (float)Math.cos(this.half);
        for (int i = 0; i < sceneData.meshCount; ++i) {
            boolean bl;
            int n = i;
            sceneData.meshFlags[n] = (byte)(sceneData.meshFlags[n] & 0xFFFFFFDF);
            float f3 = sceneData.meshOffsets[i * 3] - 4.0f - this.x;
            float f4 = sceneData.meshOffsets[i * 3 + 1] + (float)sceneData.meshes[i].level * 2.4494896f + 1.5f - this.y;
            float f5 = sceneData.meshOffsets[i * 3 + 2] - 4.0f - this.z;
            float f6 = 8.0f;
            float f7 = (float)Math.sqrt(f3 * f3 + f4 * f4 + f5 * f5);
            float f8 = f3 * this.dirX + f4 * this.dirY + f5 * this.dirZ;
            float f9 = (float)Math.sqrt(Math.max(f7 * f7 - f8 * f8, 0.0f));
            boolean bl2 = bl = sceneData.meshes[i].pages.length > 0 && sceneData.meshes[i].pages[0] == null;
            if (!bl && (!(f7 <= this.reach + f6) || !(f8 >= -f6) || !(f2 * f9 - f * f8 <= f6))) continue;
            int n2 = i;
            sceneData.meshFlags[n2] = (byte)(sceneData.meshFlags[n2] | 0x20);
        }
    }

    void bind(boolean bl) {
        Gl.bind(21, bl ? this.texture : 0);
    }

    static void sampler(GlProgram glProgram) {
        glProgram.sampler("uFlashShadow", 21);
    }

    int texture() {
        return this.texture;
    }

    Matrix4f viewProjection() {
        return this.viewProjection;
    }

    void uniforms(GlProgram glProgram, FrameContext frameContext) {
        glProgram.set("uFlashOn", this.on ? 1.0f : 0.0f);
        if (!this.on) {
            return;
        }
        float f = frameContext.scene.flashStrength * Tuning.flashIntensity;
        glProgram.set("uFlashPos", this.x, this.y, this.z);
        glProgram.set("uFlashDir", this.dirX, this.dirY, this.dirZ);
        glProgram.set("uFlashColor", COLOR[0] * f, COLOR[1] * f, COLOR[2] * f);
        glProgram.set("uFlashCone", frameContext.scene.flashCos, Tuning.flashHot, this.reach);
        glProgram.set("uFlashTexel", this.texel);
        glProgram.set("uFlashMatrix", this.matrix);
    }
}

