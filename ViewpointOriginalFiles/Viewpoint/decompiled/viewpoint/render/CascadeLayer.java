/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL43
 */
package viewpoint.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import viewpoint.platform.Gl;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;

final class CascadeLayer {
    private int texture;
    private int fbo;
    private int side;
    private boolean valid;
    private int size;
    private int margin;
    private long key;
    private float sunX;
    private float sunY;
    private float sunZ;
    private float sunDistance;
    private float radius;
    private double originX;
    private double originY;
    private double originZ;
    private final Matrix4f view = new Matrix4f();
    private long cellX;
    private long cellY;
    private float fractionX;
    private float fractionY;
    int windowX;
    int windowY;

    CascadeLayer() {
    }

    static int margin(int n) {
        return n / 8;
    }

    static double marginSquares(int n, float f) {
        return (double)CascadeLayer.margin(n) * 2.0 * (double)f / (double)n;
    }

    boolean holds(SceneData sceneData, int n, float f, float f2, long l) {
        if (!this.valid || n != this.size || f != this.radius || f2 != this.sunDistance || l != this.key || sceneData.sunX != this.sunX || sceneData.sunY != this.sunY || sceneData.sunZ != this.sunZ) {
            return false;
        }
        double d = sceneData.originX - this.originX;
        double d2 = sceneData.originY - this.originY;
        double d3 = sceneData.originZ - this.originZ;
        double d4 = 2.0 * (double)f / (double)n;
        double d5 = (double)this.margin * d4;
        if (d * d + d2 * d2 + d3 * d3 > d5 * d5) {
            return false;
        }
        this.windowX = (int)(CascadeLayer.cell(this.view.m00(), this.view.m10(), this.view.m20(), sceneData, d4) - this.cellX) + this.margin;
        this.windowY = (int)(CascadeLayer.cell(this.view.m01(), this.view.m11(), this.view.m21(), sceneData, d4) - this.cellY) + this.margin;
        return this.windowX >= 0 && this.windowY >= 0 && this.windowX <= 2 * this.margin && this.windowY <= 2 * this.margin;
    }

    void aim(SceneData sceneData, int n, float f, Matrix4f matrix4f, float f2, long l, Matrix4f matrix4f2) {
        this.size = n;
        this.radius = f;
        this.sunDistance = f2;
        this.key = l;
        this.margin = CascadeLayer.margin(n);
        this.sunX = sceneData.sunX;
        this.sunY = sceneData.sunY;
        this.sunZ = sceneData.sunZ;
        this.originX = sceneData.originX;
        this.originY = sceneData.originY;
        this.originZ = sceneData.originZ;
        this.view.set((Matrix4fc)matrix4f);
        double d = 2.0 * (double)f / (double)n;
        this.cellX = CascadeLayer.cell(this.view.m00(), this.view.m10(), this.view.m20(), sceneData, d);
        this.cellY = CascadeLayer.cell(this.view.m01(), this.view.m11(), this.view.m21(), sceneData, d);
        this.fractionX = (float)(CascadeLayer.light(this.view.m00(), this.view.m10(), this.view.m20(), sceneData) - (double)this.cellX * d);
        this.fractionY = (float)(CascadeLayer.light(this.view.m01(), this.view.m11(), this.view.m21(), sceneData) - (double)this.cellY * d);
        float f3 = f + (float)((double)this.margin * d);
        matrix4f2.setOrtho(-f3 - this.fractionX, f3 - this.fractionX, -f3 - this.fractionY, f3 - this.fractionY, 1.0f, f2 * 2.0f).mul((Matrix4fc)this.view);
        this.windowX = this.margin;
        this.windowY = this.margin;
        this.valid = true;
    }

    void cascade(SceneData sceneData, Matrix4f matrix4f) {
        float f = 2.0f * this.radius / (float)this.size;
        float f2 = -this.radius - this.fractionX + (float)(this.windowX - this.margin) * f;
        float f3 = -this.radius - this.fractionY + (float)(this.windowY - this.margin) * f;
        matrix4f.setOrtho(f2, f2 + 2.0f * this.radius, f3, f3 + 2.0f * this.radius, 1.0f, this.sunDistance * 2.0f).mul((Matrix4fc)this.view).translate((float)(sceneData.originX - this.originX), (float)(sceneData.originY - this.originY), (float)(sceneData.originZ - this.originZ));
    }

    private static double light(float f, float f2, float f3, SceneData sceneData) {
        return (double)f * sceneData.originX + (double)f2 * sceneData.originY + (double)f3 * sceneData.originZ;
    }

    private static long cell(float f, float f2, float f3, SceneData sceneData, double d) {
        return (long)Math.floor(CascadeLayer.light(f, f2, f3, sceneData) / d);
    }

    void begin() {
        int n = this.size + 2 * this.margin;
        if (this.texture == 0 || this.side != n) {
            this.free();
            int n2 = GL11.glGetInteger((int)32873);
            this.texture = Gl.texture(33190, n, n, 6402, 5126, 9728);
            this.fbo = ShadowPass.depthFbo(this.texture);
            this.side = n;
            GL11.glBindTexture((int)3553, (int)n2);
        }
        GL30.glBindFramebuffer((int)36160, (int)this.fbo);
        GL11.glViewport((int)0, (int)0, (int)this.side, (int)this.side);
        GL11.glClear((int)256);
    }

    void copyTo(int n) {
        GL43.glCopyImageSubData((int)this.texture, (int)3553, (int)0, (int)this.windowX, (int)this.windowY, (int)0, (int)n, (int)3553, (int)0, (int)0, (int)0, (int)0, (int)this.size, (int)this.size, (int)1);
    }

    void release() {
        this.free();
        this.valid = false;
    }

    private void free() {
        if (this.texture != 0) {
            GL30.glDeleteFramebuffers((int)this.fbo);
            GL11.glDeleteTextures((int)this.texture);
            this.texture = 0;
            this.fbo = 0;
            this.side = 0;
        }
    }
}

