/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL20
 *  zombie.core.DefaultShader
 *  zombie.core.ShaderHelper
 *  zombie.core.SpriteRenderer
 *  zombie.core.opengl.GLStateRenderThread
 *  zombie.core.textures.Texture
 */
package viewpoint;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import viewpoint.Screenshot;
import viewpoint.core.Frame;
import viewpoint.core.View;
import viewpoint.input.Camera;
import viewpoint.input.FreeCam;
import viewpoint.input.Look;
import viewpoint.input.ThirdPerson;
import viewpoint.models.Characters;
import viewpoint.models.Corpses;
import viewpoint.models.Models;
import viewpoint.platform.Profile;
import viewpoint.platform.Threads;
import viewpoint.render.Latency;
import viewpoint.render.Retirement;
import viewpoint.render.WorldRenderer;
import zombie.core.DefaultShader;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;

final class SceneDrawer
extends TextureDraw.GenericDrawer {
    static volatile boolean failed;
    private static final Matrix4f view;
    private final Frame frame;
    private final float[] eye = new float[3];
    private final float[] dir = new float[3];

    SceneDrawer(Frame frame) {
        this.frame = frame;
    }

    @Override
    public void render() {
        Threads.render();
        if (failed) {
            return;
        }
        Look.viewDrawn();
        boolean bl = DefaultShader.isActive;
        int n = GL11.glGetInteger((int)35725);
        int n2 = Texture.lastTextureID;
        GL11.glPushClientAttrib((int)-1);
        GL11.glPushAttrib((int)1048575);
        try {
            this.drawFrame();
        }
        catch (Throwable throwable) {
            failed = true;
            View.enabled = false;
            System.out.println("[Viewpoint] disabled after error while drawing:");
            throwable.printStackTrace(System.out);
            WorldRenderer.restoreOutput();
        }
        GL11.glPopAttrib();
        GL11.glPopClientAttrib();
        GL20.glUseProgram((int)n);
        ShaderHelper.forgetCurrentlyBound();
        DefaultShader.isActive = bl;
        Texture.lastTextureID = n2;
        GL13.glActiveTexture((int)33984);
        SpriteRenderer.ringBuffer.restoreVbos = true;
        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        GLStateRenderThread.restore();
    }

    private void drawFrame() {
        boolean bl;
        Retirement.collect();
        Look.readBeforeDrawing();
        float f = Look.yaw;
        float f2 = Look.pitch;
        Camera.direction(f, f2, this.dir);
        Camera.eye(this.frame, f, this.eye);
        this.lookAlong();
        WorldRenderer.handView.set((Matrix4fc)view);
        boolean bl2 = ThirdPerson.view(this.frame, f, f2, this.eye);
        if (bl2) {
            this.lookAlong();
        }
        if (bl = FreeCam.view(this.frame, this.frame.eyeX, this.frame.eyeY, this.frame.eyeZ, this.eye, this.dir)) {
            WorldRenderer.cullView.set((Matrix4fc)view);
            this.lookAlong();
        }
        float f3 = this.eye[0];
        float f4 = this.eye[1];
        float f5 = this.eye[2];
        WorldRenderer.cullFromPlayer = bl && !this.frame.onCamera;
        WorldRenderer.freeCamera = bl;
        WorldRenderer.handFromPlayer = bl2 || bl;
        Latency.viewBuilt(System.nanoTime(), Look.captured ? Look.readNanos : 0L, this.frame.number, this.frame.takenNanos);
        if (!WorldRenderer.begin(this.frame.scene, view, f3, f4, f5)) {
            failed = true;
            View.enabled = false;
        } else {
            boolean bl3;
            Models.release(this.frame);
            WorldRenderer.drawOutlines();
            Profile.mark(6);
            WorldRenderer.drawTranslucent(this.frame.scene);
            WorldRenderer.drawIndirect(this.frame.scene);
            WorldRenderer.drawWeather(this.frame.scene, f3, f4, f5);
            WorldRenderer.finish(this.frame.scene);
            Screenshot.captureIfRequested();
            boolean bl4 = bl3 = bl2 && this.frame.cameraSquares.seated;
            if (Look.captured && !FreeCam.active && !bl3) {
                this.drawCrosshair();
            }
        }
        Retirement.drawn(this.frame.number);
    }

    private void lookAlong() {
        view.setLookAt(this.eye[0], this.eye[1], this.eye[2], this.eye[0] + this.dir[0], this.eye[1] + this.dir[1], this.eye[2] + this.dir[2], 0.0f, 1.0f, 0.0f);
    }

    @Override
    public void postRender() {
        Characters.release(this.frame);
        Corpses.release(this.frame);
        Models.release(this.frame);
    }

    private void drawCrosshair() {
        int[] nArray = new int[4];
        GL11.glGetIntegerv((int)2978, (int[])nArray);
        int n = nArray[0] + nArray[2] / 2;
        int n2 = nArray[1] + nArray[3] / 2;
        GL11.glEnable((int)3089);
        GL11.glScissor((int)(n - 2), (int)(n2 - 2), (int)4, (int)4);
        GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        GL11.glClear((int)16384);
        GL11.glScissor((int)(n - 1), (int)(n2 - 1), (int)2, (int)2);
        GL11.glClearColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glClear((int)16384);
    }

    static {
        view = new Matrix4f();
    }
}

