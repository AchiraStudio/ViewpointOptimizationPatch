/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL14
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL31
 *  org.lwjgl.opengl.GL33
 *  org.lwjgl.opengl.GL44
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL44;
import viewpoint.platform.GlProgram;
import viewpoint.platform.GpuParts;
import viewpoint.platform.Profile;
import viewpoint.platform.Tuning;
import viewpoint.render.CellLightPages;
import viewpoint.render.FarPass;
import viewpoint.render.FrameContext;
import viewpoint.render.SceneData;
import viewpoint.render.Wireframe;
import viewpoint.render.WorldRenderer;

final class FarLamps {
    private static final int MAX_CARDS = 16384;
    private static final int CARD_BYTES = 16;
    private static final float NEAR_FADE = 16.0f;
    private final GlProgram program = GlProgram.create("far lamps", "far_lamp.vert", "far_lamp.frag");
    private final IntBuffer cards = BufferUtils.createIntBuffer((int)65536);
    private final ByteBuffer upload = BufferUtils.createByteBuffer((int)262144);
    private final Vector3f right = new Vector3f();
    private final Vector3f up = new Vector3f();
    private int buffer;
    private int vao;
    private int count;
    private int written = -1;

    FarLamps() {
        this.program.sampler("uDepth", 12);
    }

    void draw(FrameContext frameContext, Matrix4f matrix4f) {
        if (WorldRenderer.off(9) || Wireframe.mode() != 0) {
            return;
        }
        this.write();
        if (this.count == 0) {
            return;
        }
        SceneData sceneData = frameContext.scene;
        GpuParts.begin(18);
        this.program.use();
        frameContext.camera(this.program);
        this.program.set("uViewProjection", matrix4f);
        this.program.set("uCameraSquare", (float)(-sceneData.originX), (float)(-sceneData.originZ), (float)(sceneData.originY / 2.4494895935058594));
        frameContext.viewRotation.getColumn(0, this.right);
        frameContext.viewRotation.getColumn(1, this.up);
        this.program.set("uRight", this.right.x, this.right.y, this.right.z);
        this.program.set("uUp", this.up.x, this.up.y, this.up.z);
        this.program.set("uPixel", 2.0f / (sceneData.projection.m11() * (float)frameContext.height));
        this.program.set("uCard", Tuning.farLampSize, Tuning.farLampPixels);
        this.program.set("uGlow", Tuning.farLampGlow);
        this.program.set("uNearFade", sceneData.nearReach, 16.0f);
        this.program.set("uFogRange", sceneData.fogStart, sceneData.fogEnd);
        this.program.set("uHaze", Tuning.farHaze, sceneData.nearEnd());
        FarPass.daylight(this.program, sceneData);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)515);
        GL11.glDepthMask((boolean)false);
        GL11.glEnable((int)3042);
        GL14.glBlendFuncSeparate((int)1, (int)1, (int)0, (int)1);
        GL30.glBindVertexArray((int)this.vao);
        GL31.glDrawArraysInstanced((int)5, (int)0, (int)4, (int)this.count);
        GL30.glBindVertexArray((int)0);
        GL11.glDisable((int)3042);
        GL11.glDepthMask((boolean)true);
        GL11.glDepthFunc((int)513);
        ++Profile.draws;
        GpuParts.end(18);
    }

    private void write() {
        if (this.written == CellLightPages.cardsChanged()) {
            return;
        }
        this.written = CellLightPages.cardsChanged();
        if (this.buffer == 0) {
            this.make();
        }
        this.cards.clear();
        CellLightPages.cards(this.cards);
        this.cards.flip();
        Profile.farLampCards = this.count = this.cards.remaining() / 4;
        this.upload.clear();
        for (int i = 0; i < this.count; ++i) {
            int n = i * 4;
            this.upload.putFloat((float)this.cards.get(n) + 0.5f).putFloat((float)this.cards.get(n + 1) + 0.5f).putFloat(this.cards.get(n + 2));
            this.upload.putInt(this.cards.get(n + 3));
        }
        this.upload.flip();
        GL15.glBindBuffer((int)34962, (int)this.buffer);
        GL15.glBufferSubData((int)34962, (long)0L, (ByteBuffer)this.upload);
        GL15.glBindBuffer((int)34962, (int)0);
    }

    private void make() {
        this.buffer = GL15.glGenBuffers();
        GL15.glBindBuffer((int)34962, (int)this.buffer);
        GL44.glBufferStorage((int)34962, (long)262144L, (int)256);
        this.vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray((int)this.vao);
        GL20.glEnableVertexAttribArray((int)0);
        GL20.glVertexAttribPointer((int)0, (int)3, (int)5126, (boolean)false, (int)16, (long)0L);
        GL33.glVertexAttribDivisor((int)0, (int)1);
        GL20.glEnableVertexAttribArray((int)1);
        GL20.glVertexAttribPointer((int)1, (int)4, (int)5121, (boolean)true, (int)16, (long)12L);
        GL33.glVertexAttribDivisor((int)1, (int)1);
        GL30.glBindVertexArray((int)0);
        GL15.glBindBuffer((int)34962, (int)0);
    }
}

