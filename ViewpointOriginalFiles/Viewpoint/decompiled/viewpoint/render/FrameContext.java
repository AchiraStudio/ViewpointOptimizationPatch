/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix3f
 *  org.joml.Matrix4f
 */
package viewpoint.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import viewpoint.platform.GlProgram;
import viewpoint.platform.Pipeline;
import viewpoint.render.SceneData;
import viewpoint.render.WorldRenderer;

public final class FrameContext {
    public SceneData scene;
    public Pipeline pipeline = Pipeline.LIBRARY;
    public Matrix4f view;
    public float eyeX;
    public float eyeY;
    public float eyeZ;
    final Matrix3f viewRotation = new Matrix3f();
    public final Matrix4f jitteredProjection = new Matrix4f();
    public final Matrix4f viewProjection = new Matrix4f();
    public final Matrix4f invViewProjection = new Matrix4f();
    final Matrix4f plainViewProjection = new Matrix4f();
    final Matrix4f invPlainViewProjection = new Matrix4f();
    final Matrix4f previousViewProjection = new Matrix4f();
    final Matrix4f reprojection = new Matrix4f();
    float frameSeconds;
    boolean previousValid;
    public float farNear;
    public float farFar;
    public final Matrix4f farInvPlainViewProjection = new Matrix4f();
    long index;
    int jitterPhase;
    public boolean sun;
    public int width;
    public int height;
    public int halfWidth;
    public int halfHeight;
    int outputDrawFbo;
    int outputReadFbo;
    boolean outputScissor;
    final int[] viewport = new int[4];

    float fadeNoise() {
        return WorldRenderer.off(1) ? 0.0f : (float)this.jitterPhase * 5.588238f;
    }

    void camera(GlProgram glProgram) {
        glProgram.set("uInvViewProjection", this.invViewProjection);
        glProgram.set("uCamera", 0.05f, 400.0f, 1.0f / this.scene.projection.m00(), 1.0f / this.scene.projection.m11());
        glProgram.set("uEye", this.eyeX, this.eyeY, this.eyeZ);
    }

    float floorY() {
        return (float)((Math.floor(this.scene.originY / 2.4494895935058594) - this.scene.originY / 2.4494895935058594) * 2.4494895935058594);
    }
}

