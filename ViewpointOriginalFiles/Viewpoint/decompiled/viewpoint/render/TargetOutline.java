/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL14
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL33
 */
package viewpoint.render;

import java.nio.IntBuffer;
import java.util.Arrays;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.Tuning;
import viewpoint.render.ChunkMeshData;
import viewpoint.render.CorpseCards;
import viewpoint.render.FrameContext;
import viewpoint.render.MeshArena;
import viewpoint.render.Meshes;
import viewpoint.render.ModelPass;
import viewpoint.render.PackDraws;
import viewpoint.render.SceneData;

final class TargetOutline {
    private static final String SECTION = "Game/Loot menu";
    private static final LiveSettings.Number RED = TargetOutline.colour("loot.outlineRed", "Outline red", 255.0f);
    private static final LiveSettings.Number GREEN = TargetOutline.colour("loot.outlineGreen", "Outline green", 214.0f);
    private static final LiveSettings.Number BLUE = TargetOutline.colour("loot.outlineBlue", "Outline blue", 120.0f);
    private static final LiveSettings.Number OPACITY = LiveSettings.number("loot.outlineOpacity", "Outline opacity", "Game/Loot menu", 0.0f, 1.0f, 0.05f, 0.8f);
    private static final LiveSettings.Number WIDTH = LiveSettings.number("loot.outlineWidth", "Outline width (pixels at 1080p)", "Game/Loot menu", 1.0f, 24.0f, 1.0f, 6.0f);
    private static final int MAX_STEPS = 32;
    private static final float WIDTH_AT = 1080.0f;
    private final GlProgram mask = GlProgram.create("target mask", "shadow.vert", "target_mask.frag");
    private final GlProgram blur = GlProgram.create("target blur", "screen.vert", "target_blur.frag");
    private final GlProgram outline = GlProgram.create("target outline", "screen.vert", "target_outline.frag");
    private int width;
    private int height;
    private int maskTexture;
    private int maskFbo;
    private final int[] blurTextures = new int[2];
    private final int[] blurFbos = new int[2];
    private int[] segments = new int[24];
    private boolean masked;

    TargetOutline() {
        this.mask.sampler("uTexture", 0);
        this.mask.sampler("uPlantSlots", 48);
        this.blur.sampler("uMask", 0);
        this.outline.sampler("uGlow", 0);
        this.outline.sampler("uMask", 51);
    }

    private static LiveSettings.Number colour(String string, String string2, float f) {
        return LiveSettings.number(string, string2, SECTION, 0.0f, 255.0f, 1.0f, f);
    }

    void mask(FrameContext frameContext, ModelPass modelPass) {
        SceneData sceneData = frameContext.scene;
        this.masked = false;
        if (OPACITY.get() <= 0.0f || sceneData.targetMesh == null && !sceneData.models.targeted) {
            return;
        }
        this.ensure(frameContext.width, frameContext.height);
        GL30.glBindFramebuffer((int)36160, (int)this.maskFbo);
        GL11.glViewport((int)0, (int)0, (int)this.width, (int)this.height);
        Gl.resetState();
        GL11.glDisable((int)2929);
        GL11.glDepthMask((boolean)false);
        GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        GL11.glClear((int)16384);
        this.world(frameContext);
        modelPass.targets(frameContext);
        Gl.resetState();
        this.masked = true;
    }

    void composite(FrameContext frameContext) {
        if (!this.masked) {
            return;
        }
        this.masked = false;
        float f = Math.min(32.0f, WIDTH.get() * (float)this.height / 1080.0f * 0.5f);
        int n = Math.max(1, this.width / 2);
        int n2 = Math.max(1, this.height / 2);
        Gl.resetState();
        GL11.glDisable((int)2929);
        GL11.glDepthMask((boolean)false);
        this.blur.use();
        this.blur.set("uRadius", f);
        GL11.glViewport((int)0, (int)0, (int)n, (int)n2);
        this.blurPass(this.blurFbos[0], this.maskTexture, 2.0f / (float)this.width, 0.0f);
        this.blurPass(this.blurFbos[1], this.blurTextures[0], 0.0f, 1.0f / (float)n2);
        GL30.glBindFramebuffer((int)36009, (int)frameContext.outputDrawFbo);
        GL30.glBindFramebuffer((int)36008, (int)frameContext.outputReadFbo);
        GL11.glViewport((int)frameContext.viewport[0], (int)frameContext.viewport[1], (int)frameContext.viewport[2], (int)frameContext.viewport[3]);
        GL11.glEnable((int)3042);
        GL14.glBlendFuncSeparate((int)770, (int)771, (int)0, (int)1);
        this.outline.use();
        this.outline.set("uColour", RED.get() / 255.0f, GREEN.get() / 255.0f, BLUE.get() / 255.0f);
        this.outline.set("uOpacity", OPACITY.get());
        this.outline.set("uEdge", Tuning.targetOutlineEdge);
        Gl.bind(51, this.maskTexture);
        Gl.bind(0, this.blurTextures[1]);
        Gl.screenTriangle();
        Gl.bind(51, 0);
        Gl.bind(0, 0);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glDisable((int)3042);
    }

    private void blurPass(int n, int n2, float f, float f2) {
        GL30.glBindFramebuffer((int)36160, (int)n);
        this.blur.set("uStep", f, f2);
        Gl.bind(0, n2);
        Gl.screenTriangle();
    }

    private void world(FrameContext frameContext) {
        int n;
        SceneData sceneData = frameContext.scene;
        ChunkMeshData chunkMeshData = sceneData.targetMesh;
        int n2 = TargetOutline.record(sceneData, chunkMeshData);
        if (n2 < 0 || chunkMeshData.arenaFirst < 0) {
            return;
        }
        if (sceneData.targetSquare >= 0) {
            this.mask.use();
            this.mask.set("uViewProjection", frameContext.plainViewProjection);
            if (chunkMeshData.hasModels()) {
                PackDraws.drawTarget(sceneData, this.mask, n2, 1L << sceneData.targetSquare);
            }
            CorpseCards.drawTarget(sceneData, this.mask, n2, 1L << sceneData.targetSquare);
        }
        if (chunkMeshData.runs == null) {
            return;
        }
        int n3 = 0;
        for (n = 0; n < chunkMeshData.runs.length; n += 3) {
            n3 += chunkMeshData.runs[n] == sceneData.targetOwner ? 1 : 0;
        }
        if (n3 == 0) {
            return;
        }
        n = this.commands(chunkMeshData, sceneData.targetOwner, n2, MeshArena.commands(n3));
        MeshArena.beginDraws();
        this.mask.use();
        this.mask.set("uViewProjection", frameContext.plainViewProjection);
        for (int i = 0; i < n; ++i) {
            Meshes.bindPage(chunkMeshData.pages[this.segments[i * 3]]);
            MeshArena.multiDraw(this.segments[i * 3 + 1], this.segments[i * 3 + 2]);
        }
        MeshArena.endDraws();
        GL33.glBindSampler((int)0, (int)0);
    }

    private int commands(ChunkMeshData chunkMeshData, int n, int n2, IntBuffer intBuffer) {
        int n3 = 0;
        for (int i = 0; i < chunkMeshData.pages.length; ++i) {
            int n4;
            int n5 = intBuffer.position() / 4;
            for (n4 = chunkMeshData.pageRuns[i]; n4 < chunkMeshData.pageRuns[i + 1]; ++n4) {
                if (chunkMeshData.runs[n4 * 3] != n) continue;
                intBuffer.put(chunkMeshData.runs[n4 * 3 + 2]).put(1).put(chunkMeshData.arenaFirst + chunkMeshData.runs[n4 * 3 + 1]);
                intBuffer.put(n2);
            }
            n4 = intBuffer.position() / 4 - n5;
            if (n4 <= 0 || chunkMeshData.pages[i] == null) continue;
            if (this.segments.length < (n3 + 1) * 3) {
                this.segments = Arrays.copyOf(this.segments, this.segments.length * 2);
            }
            this.segments[n3 * 3] = i;
            this.segments[n3 * 3 + 1] = n5;
            this.segments[n3 * 3 + 2] = n4;
            ++n3;
        }
        return n3;
    }

    private static int record(SceneData sceneData, ChunkMeshData chunkMeshData) {
        for (int i = 0; i < sceneData.meshCount; ++i) {
            if (sceneData.meshes[i] != chunkMeshData) continue;
            return i;
        }
        return -1;
    }

    private void ensure(int n, int n2) {
        if (this.maskFbo != 0 && n == this.width && n2 == this.height) {
            return;
        }
        this.release();
        this.width = n;
        this.height = n2;
        this.maskTexture = Gl.texture(33321, n, n2, 6403, 5121, 9729);
        this.maskFbo = TargetOutline.fbo(this.maskTexture);
        for (int i = 0; i < 2; ++i) {
            this.blurTextures[i] = Gl.texture(33321, Math.max(1, n / 2), Math.max(1, n2 / 2), 6403, 5121, 9729);
            this.blurFbos[i] = TargetOutline.fbo(this.blurTextures[i]);
        }
        GL11.glBindTexture((int)3553, (int)0);
    }

    private static int fbo(int n) {
        int n2 = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer((int)36160, (int)n2);
        GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)3553, (int)n, (int)0);
        return n2;
    }

    private void release() {
        if (this.maskFbo == 0) {
            return;
        }
        GL30.glDeleteFramebuffers((int)this.maskFbo);
        GL11.glDeleteTextures((int)this.maskTexture);
        for (int i = 0; i < 2; ++i) {
            GL30.glDeleteFramebuffers((int)this.blurFbos[i]);
            GL11.glDeleteTextures((int)this.blurTextures[i]);
        }
        this.maskFbo = 0;
    }
}

