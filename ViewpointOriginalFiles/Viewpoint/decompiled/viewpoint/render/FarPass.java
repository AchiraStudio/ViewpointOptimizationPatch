/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.FrustumIntersection
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.GpuParts;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.Profile;
import viewpoint.platform.Tuning;
import viewpoint.render.BandLight;
import viewpoint.render.CellLightPages;
import viewpoint.render.FarGpu;
import viewpoint.render.FarLamps;
import viewpoint.render.FarShadow;
import viewpoint.render.FloorFilter;
import viewpoint.render.FrameContext;
import viewpoint.render.LodView;
import viewpoint.render.MeshArena;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;
import viewpoint.render.ShellArena;
import viewpoint.render.SkyPass;
import viewpoint.render.Targets;
import viewpoint.render.TextureFilter;
import viewpoint.render.TreeBaker;
import viewpoint.render.TreeKinds;
import viewpoint.render.Wireframe;
import viewpoint.render.WorldRenderer;

public final class FarPass {
    private static final float NEAR = 16.0f;
    private static final float DAYLIGHT = 0.8f;
    static final LiveSettings.Number TREES_FADE = LiveSettings.number("lod.treesFadeBlocks", "Far trees fade out from (blocks)", "World/Levels of detail", 0.0f, 64.0f, 1.0f, 12.0f);
    static final LiveSettings.Number TREES_GONE = LiveSettings.number("lod.treesGoneBlocks", "Far trees gone at (blocks)", "World/Levels of detail", 0.0f, 64.0f, 1.0f, 36.0f);
    private final GlProgram program = GlProgram.create("far", "far.vert", "far.frag");
    private final GlProgram shellProgram = GlProgram.create("far shell", "far_shell.vert", "far_shell.frag");
    private final GlProgram bandProgram = GlProgram.create("far shell band", "far_shell.vert", "gbuffer_shell.frag");
    private final GlProgram treeProgram = GlProgram.create("far trees", "far_tree.vert", "far_tree.frag");
    private final GlProgram prime = GlProgram.create("far prime", "screen.vert", "far_prime.frag");
    private static long commanded;
    private final Targets targets;
    private final ShadowPass shadows;
    private final FarShadow shadow;
    final TreeBaker trees;
    private final FarLamps lamps = new FarLamps();
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f viewProjection = new Matrix4f();
    private final Matrix4f cullViewProjection = new Matrix4f();
    private final FrustumIntersection frustum = new FrustumIntersection();
    private final ByteBuffer maskPixels = BufferUtils.createByteBuffer((int)361);
    private final ByteBuffer shellPixels = BufferUtils.createByteBuffer((int)8427);
    int mask;
    int shellMask;
    private boolean active;

    FarPass(Targets targets, ShadowPass shadowPass, FarShadow farShadow, TreeBaker treeBaker) {
        this.targets = targets;
        this.shadows = shadowPass;
        this.shadow = farShadow;
        this.trees = treeBaker;
        this.program.sampler("uTop", 25);
        this.program.sampler("uSide", 26);
        this.program.sampler("uMask", 27);
        this.program.sampler("uMainDepth", 12);
        this.program.sampler("uShellMask", 28);
        ShadowPass.samplers(this.program);
        this.shellProgram.sampler("uTexture", 0);
        this.shellProgram.sampler("uMask", 27);
        this.shellProgram.sampler("uMainDepth", 12);
        this.prime.sampler("uMainDepth", 12);
        this.shellProgram.sampler("uShellMask", 28);
        ShadowPass.samplers(this.shellProgram);
        this.shellProgram.sampler("uFloorPages", 41);
        this.shellProgram.sampler("uTreeAtlas", 42);
        this.shellProgram.sampler("uCellLight", 46);
        this.shellProgram.sampler("uCellTable", 47);
        this.program.sampler("uCellLight", 46);
        this.program.sampler("uCellTable", 47);
        this.bandProgram.sampler("uTexture", 0);
        this.bandProgram.sampler("uFloorPages", 41);
        this.bandProgram.sampler("uMask", 27);
        this.bandProgram.sampler("uShellMask", 28);
        this.bandProgram.sampler("uTreeAtlas", 42);
        this.bandProgram.sampler("uLight", 5);
        this.bandProgram.sampler("uBandLight", 45);
        this.bandProgram.sampler("uCellLight", 46);
        this.bandProgram.sampler("uCellTable", 47);
        this.treeProgram.sampler("uTrees", 43);
        this.treeProgram.sampler("uKinds", 44);
        this.treeProgram.sampler("uMask", 27);
        this.treeProgram.sampler("uMainDepth", 12);
        this.treeProgram.sampler("uShellMask", 28);
        ShadowPass.samplers(this.treeProgram);
    }

    void prepare(FrameContext frameContext, boolean bl) {
        FarGpu.uploadShells();
        FarGpu.uploadCells();
        BandLight.update();
        CellLightPages.update();
        SceneData sceneData = frameContext.scene;
        Profile.farDrawn = sceneData.farCount;
        Profile.farInView = 0;
        Profile.farTrees = 0;
        Profile.shellsInView = 0;
        frameContext.farFar = 0.0f;
        frameContext.farNear = 0.0f;
        boolean bl2 = this.active = (sceneData.farCount > 0 || sceneData.shellCount > 0) && sceneData.fogEnd > sceneData.nearEnd();
        if (!this.active || !bl) {
            this.shadow.off();
        }
        if (!this.active) {
            return;
        }
        float f = sceneData.fogEnd * 1.5f + 256.0f;
        this.projection.set((Matrix4fc)sceneData.projection);
        this.projection.m22((f + 16.0f) / (16.0f - f));
        this.projection.m32(2.0f * f * 16.0f / (16.0f - f));
        this.projection.mul((Matrix4fc)frameContext.view, this.viewProjection).invert(frameContext.farInvPlainViewProjection);
        frameContext.farNear = 16.0f;
        frameContext.farFar = f;
        this.projection.set((Matrix4fc)frameContext.jitteredProjection);
        this.projection.m22((f + 16.0f) / (16.0f - f));
        this.projection.m32(2.0f * f * 16.0f / (16.0f - f));
        this.frustum.set((Matrix4fc)(WorldRenderer.cullFromPlayer ? this.projection.mul((Matrix4fc)WorldRenderer.cullView, this.cullViewProjection) : this.projection.mul((Matrix4fc)frameContext.view, this.cullViewProjection)));
        this.projection.mul((Matrix4fc)frameContext.view, this.viewProjection);
        this.uploadMask(sceneData);
        this.trees.bake();
        if (bl) {
            this.shadow.draw(frameContext, TREES_GONE.get() * 64.0f, this.mask, this.shellMask, this.shadows.quiet(frameContext));
        }
        GL13.glActiveTexture((int)33984);
    }

    void gbuffer(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        if (!this.active || sceneData.shellCount == 0) {
            return;
        }
        this.bandProgram.use();
        Wireframe.apply(this.bandProgram);
        FloorFilter.apply(this.bandProgram);
        TextureFilter.SPRITES.apply(this.bandProgram);
        this.bandProgram.set("uViewProjection", frameContext.viewProjection);
        this.bandProgram.set("uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        this.bandProgram.set("uFadeNoise", frameContext.fadeNoise());
        FarPass.daylight(this.bandProgram, sceneData);
        this.bandProgram.set("uSkyLevel", sceneData.skyR, sceneData.skyG, sceneData.skyB);
        this.bandProgram.set("uWet", FarPass.wetness(frameContext));
        FarPass.masks(this.bandProgram, sceneData);
        Gl.bind(27, this.mask);
        Gl.bind(28, this.shellMask);
        MeshArena.bindLight(5, this.bandProgram);
        BandLight.bind(45);
        CellLightPages.bind(46, 47);
        this.trees.bind(true);
        GL13.glActiveTexture((int)33984);
        GL11.glEnable((int)2960);
        GL11.glStencilFunc((int)514, (int)0, (int)255);
        GL11.glStencilOp((int)7680, (int)7680, (int)7680);
        ShellArena.begin();
        long l = commanded;
        for (int i = 0; i < sceneData.shellCount; ++i) {
            FarGpu.BlockDraw blockDraw = sceneData.shells[i];
            if (blockDraw.shell == null || !this.blockInView(blockDraw) || !(FarPass.distance(blockDraw, frameContext, false) < 256.0f)) continue;
            FarPass.command(blockDraw);
        }
        Profile.bandVertices = commanded - l;
        ShellArena.draw();
        GL11.glDisable((int)2960);
        GL11.glBindTexture((int)3553, (int)0);
        Gl.bind(27, 0);
        Gl.bind(28, 0);
        MeshArena.unbindLight(5);
        BandLight.unbind(45);
        CellLightPages.unbind(46, 47);
        this.trees.bind(false);
        GL13.glActiveTexture((int)33984);
    }

    void draw(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        if (!this.active) {
            return;
        }
        GL30.glBindFramebuffer((int)36160, (int)this.targets.farFbo);
        GL11.glViewport((int)0, (int)0, (int)frameContext.width, (int)frameContext.height);
        GL11.glDepthMask((boolean)true);
        GL11.glClear((int)1280);
        Gl.bind(27, this.mask);
        Gl.bind(28, this.shellMask);
        Gl.bind(12, this.targets.depth);
        this.prime();
        this.shadows.bind(true);
        this.trees.bind(true);
        CellLightPages.bind(46, 47);
        GpuParts.begin(9);
        this.drawShells(frameContext, sceneData);
        GpuParts.end(9);
        GpuParts.begin(10);
        this.drawBoxes(frameContext, sceneData);
        GpuParts.end(10);
        GpuParts.begin(11);
        this.drawTrees(frameContext, sceneData);
        GpuParts.end(11);
        GL11.glDisable((int)2960);
        this.lamps.draw(frameContext, this.viewProjection);
        CellLightPages.unbind(46, 47);
        GL30.glBindVertexArray((int)0);
        Gl.bind(25, 0);
        Gl.bind(26, 0);
        Gl.bind(27, 0);
        Gl.bind(28, 0);
        Gl.bind(12, 0);
        this.shadows.bind(false);
        this.trees.bind(false);
        GL13.glActiveTexture((int)33984);
    }

    private void prime() {
        boolean bl = GL11.glIsEnabled((int)2929);
        GL11.glDisable((int)2929);
        GL11.glColorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        GL11.glDepthMask((boolean)false);
        GL11.glEnable((int)2960);
        GL11.glStencilFunc((int)519, (int)1, (int)255);
        GL11.glStencilOp((int)7680, (int)7680, (int)7681);
        this.prime.use();
        Gl.screenTriangle();
        GL11.glStencilFunc((int)514, (int)0, (int)255);
        GL11.glStencilOp((int)7680, (int)7680, (int)7680);
        GL11.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GL11.glDepthMask((boolean)true);
        if (bl) {
            GL11.glEnable((int)2929);
        }
    }

    private void drawShells(FrameContext frameContext, SceneData sceneData) {
        this.shellProgram.use();
        Wireframe.apply(this.shellProgram);
        FloorFilter.apply(this.shellProgram);
        TextureFilter.SPRITES.apply(this.shellProgram);
        this.common(this.shellProgram, frameContext);
        this.shellProgram.set("uWet", FarPass.wetness(frameContext));
        GL13.glActiveTexture((int)33984);
        ShellArena.begin();
        for (int i = 0; i < sceneData.shellCount; ++i) {
            FarGpu.BlockDraw blockDraw = sceneData.shells[i];
            if (blockDraw.shell == null || !this.blockInView(blockDraw)) continue;
            ++Profile.shellsInView;
            if (!(FarPass.distance(blockDraw, frameContext, true) >= 256.0f)) continue;
            FarPass.command(blockDraw);
        }
        Profile.shellVerticesDrawn = commanded;
        commanded = 0L;
        ShellArena.draw();
        GL11.glBindTexture((int)3553, (int)0);
    }

    static void command(FarGpu.BlockDraw blockDraw) {
        int n = blockDraw.shell.arenaFirst();
        if (n < 0) {
            return;
        }
        int n2 = ShellArena.record(blockDraw.originX, blockDraw.originY, blockDraw.originZ, blockDraw.worldX, blockDraw.worldY, blockDraw.groundLayer, blockDraw.fadeLo, blockDraw.fadeHi);
        for (int i = 0; i < blockDraw.shell.pages.length; ++i) {
            commanded += (long)blockDraw.shell.count[i];
            if (blockDraw.shell.pages[i] != null) {
                ShellArena.command(blockDraw.shell, i, n + blockDraw.shell.first[i], blockDraw.shell.count[i], n2);
                continue;
            }
            if (blockDraw.groundArray == null) continue;
            ShellArena.ground(blockDraw.groundArray, blockDraw.groundTexture, n + blockDraw.shell.first[i], blockDraw.shell.count[i], n2);
        }
    }

    private void drawBoxes(FrameContext frameContext, SceneData sceneData) {
        Object object;
        int n;
        this.program.use();
        this.common(this.program, frameContext);
        this.program.set("uWet", FarPass.wetness(frameContext));
        this.program.set("uNearReach", sceneData.nearReach);
        this.program.set("uWindowGlow", Tuning.farWindowGlow);
        this.program.set("uInterior", 1.0f);
        for (n = 0; n < sceneData.shellCount; ++n) {
            object = sceneData.shells[n];
            if (((FarGpu.BlockDraw)object).inside == null || ((FarGpu.BlockDraw)object).inside.vertices == 0 || !this.blockInView((FarGpu.BlockDraw)object)) continue;
            this.program.set("uOrigin", ((FarGpu.BlockDraw)object).originX, ((FarGpu.BlockDraw)object).originY, ((FarGpu.BlockDraw)object).originZ);
            this.program.set("uCellWorld", ((FarGpu.BlockDraw)object).worldX, ((FarGpu.BlockDraw)object).worldY);
            this.program.set("uFade", ((FarGpu.BlockDraw)object).fadeLo, ((FarGpu.BlockDraw)object).fadeHi);
            GL30.glBindVertexArray((int)FarGpu.vertexArray(((FarGpu.BlockDraw)object).inside));
            GL11.glDrawArrays((int)4, (int)0, (int)((FarGpu.BlockDraw)object).inside.vertices);
            ++Profile.draws;
        }
        this.program.set("uInterior", 0.0f);
        for (n = 0; n < sceneData.farCount; ++n) {
            object = sceneData.far[n];
            float f = ((FarGpu.CellDraw)object).originY + ((FarGpu.CellDraw)object).maxHeight * 2.4494896f;
            if (!this.frustum.testAab(((FarGpu.CellDraw)object).originX - 256.0f, ((FarGpu.CellDraw)object).originY, ((FarGpu.CellDraw)object).originZ - 256.0f, ((FarGpu.CellDraw)object).originX, f, ((FarGpu.CellDraw)object).originZ)) continue;
            ++Profile.farInView;
            FarGpu.textures(((FarGpu.CellDraw)object).colours);
            Gl.bind(25, ((FarGpu.CellDraw)object).colours.topTexture);
            Gl.bind(26, ((FarGpu.CellDraw)object).colours.sideTexture);
            this.program.set("uOrigin", ((FarGpu.CellDraw)object).originX, ((FarGpu.CellDraw)object).originY, ((FarGpu.CellDraw)object).originZ);
            this.program.set("uCellWorld", ((FarGpu.CellDraw)object).worldX, ((FarGpu.CellDraw)object).worldY);
            this.program.set("uFade", ((FarGpu.CellDraw)object).fadeLo, ((FarGpu.CellDraw)object).fadeHi);
            GL30.glBindVertexArray((int)FarGpu.vertexArray(((FarGpu.CellDraw)object).mesh));
            GL11.glDrawArrays((int)4, (int)0, (int)((FarGpu.CellDraw)object).mesh.vertices);
            ++Profile.draws;
        }
    }

    private void drawTrees(FrameContext frameContext, SceneData sceneData) {
        if (!TreeKinds.bind()) {
            return;
        }
        float f = TREES_GONE.get() * 64.0f;
        this.treeProgram.use();
        this.common(this.treeProgram, frameContext);
        TreeKinds.uniforms(this.treeProgram);
        this.treeProgram.set("uNearReach", sceneData.nearReach);
        this.treeProgram.set("uPixel", 2.0f / (this.projection.m11() * (float)frameContext.height));
        this.treeProgram.set("uTreeFade", Math.min(TREES_FADE.get() * 64.0f, f), f);
        this.treeProgram.set("uTreeVariation", Tuning.farTreeVariation, Tuning.farTreeRelief);
        this.treeProgram.set("uBareCrown", Tuning.farBareCrown);
        for (int i = 0; i < sceneData.farCount; ++i) {
            int n;
            FarGpu.CellDraw cellDraw = sceneData.far[i];
            float f2 = cellDraw.originY + cellDraw.maxHeight * 2.4494896f;
            float f3 = Math.max(0.0f, Math.max(cellDraw.originX - 256.0f - frameContext.eyeX, frameContext.eyeX - cellDraw.originX));
            float f4 = Math.max(0.0f, Math.max(cellDraw.originZ - 256.0f - frameContext.eyeZ, frameContext.eyeZ - cellDraw.originZ));
            if (cellDraw.trees == null || cellDraw.trees.count == 0 || f3 * f3 + f4 * f4 >= f * f || !this.frustum.testAab(cellDraw.originX - 256.0f, cellDraw.originY, cellDraw.originZ - 256.0f, cellDraw.originX, f2, cellDraw.originZ) || (n = cellDraw.trees.kept(cellDraw.treeKeep)) == 0) continue;
            this.treeProgram.set("uGrow", cellDraw.treeGrow);
            this.treeProgram.set("uOrigin", cellDraw.originX, cellDraw.originY, cellDraw.originZ);
            this.treeProgram.set("uCellWorld", cellDraw.worldX, cellDraw.worldY);
            this.treeProgram.set("uFade", cellDraw.fadeLo, cellDraw.fadeHi);
            FarGpu.bindTrees(cellDraw.trees);
            Gl.generated(n * 6);
            Profile.farTrees += n;
        }
        TreeKinds.unbind();
        GL13.glActiveTexture((int)34027);
        GL11.glBindTexture((int)35882, (int)0);
        GL13.glActiveTexture((int)33984);
    }

    private void common(GlProgram glProgram, FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        glProgram.set("uViewProjection", this.viewProjection);
        glProgram.set("uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        glProgram.set("uFadeNoise", frameContext.fadeNoise());
        glProgram.set("uFogColor", sceneData.fogR, sceneData.fogG, sceneData.fogB);
        glProgram.set("uFogRange", sceneData.fogStart, sceneData.fogEnd);
        glProgram.set("uHaze", Tuning.farHaze, sceneData.nearEnd());
        SkyPass.fogUniforms(glProgram, sceneData);
        FarPass.daylight(glProgram, sceneData);
        FarPass.masks(glProgram, sceneData);
        this.shadows.sunUniforms(glProgram, frameContext);
        glProgram.setInt("uDebugView", WorldRenderer.debugView);
    }

    private static float wetness(FrameContext frameContext) {
        return frameContext.pipeline.puddles ? frameContext.scene.wetGround : 0.0f;
    }

    static void daylight(GlProgram glProgram, SceneData sceneData) {
        glProgram.set("uDaylight", sceneData.daylightKnown ? sceneData.daylightR : 0.8f, sceneData.daylightKnown ? sceneData.daylightG : 0.8f, sceneData.daylightKnown ? sceneData.daylightB : 0.8f);
    }

    private static void masks(GlProgram glProgram, SceneData sceneData) {
        glProgram.set("uMaskGrid", sceneData.farMaskX, sceneData.farMaskY, sceneData.farMaskSize);
        LodView.apply(glProgram);
        glProgram.set("uShellGrid", sceneData.shellMaskX, sceneData.shellMaskY, 53.0f);
    }

    private boolean blockInView(FarGpu.BlockDraw blockDraw) {
        float f = blockDraw.originX - (float)blockDraw.blockX;
        float f2 = f - 64.0f;
        float f3 = blockDraw.originZ - (float)blockDraw.blockY;
        float f4 = f3 - 64.0f;
        return this.frustum.testAab(f2, blockDraw.originY, f4, f, blockDraw.originY + blockDraw.top * 2.4494896f, f3);
    }

    private static float distance(FarGpu.BlockDraw blockDraw, FrameContext frameContext, boolean bl) {
        float f = blockDraw.originX - (float)blockDraw.blockX - frameContext.eyeX;
        float f2 = f - 64.0f;
        float f3 = blockDraw.originZ - (float)blockDraw.blockY - frameContext.eyeZ;
        float f4 = f3 - 64.0f;
        float f5 = blockDraw.originY - frameContext.eyeY;
        float f6 = f5 + blockDraw.top * 2.4494896f;
        float f7 = bl ? Math.max(-f2, f) : Math.max(0.0f, Math.max(f2, -f));
        float f8 = bl ? Math.max(Math.abs(f5), Math.abs(f6)) : Math.max(0.0f, Math.max(f5, -f6));
        float f9 = bl ? Math.max(-f4, f3) : Math.max(0.0f, Math.max(f4, -f3));
        return (float)Math.sqrt(f7 * f7 + f8 * f8 + f9 * f9);
    }

    private void uploadMask(SceneData sceneData) {
        int n = sceneData.farMaskSize;
        if (this.mask == 0) {
            GL13.glActiveTexture((int)34011);
            this.mask = Gl.texture(33321, 19, 19, 6403, 5121, 9728);
            GL13.glActiveTexture((int)34012);
            this.shellMask = Gl.texture(32849, 53, 53, 6407, 5121, 9728);
        }
        this.shellPixels.clear();
        this.shellPixels.put(sceneData.shellMask).flip();
        GL13.glActiveTexture((int)34012);
        GL11.glBindTexture((int)3553, (int)this.shellMask);
        GL11.glPixelStorei((int)3317, (int)1);
        GL11.glTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)53, (int)53, (int)6407, (int)5121, (ByteBuffer)this.shellPixels);
        GL11.glPixelStorei((int)3317, (int)4);
        if (n > 0) {
            this.maskPixels.clear();
            this.maskPixels.put(sceneData.farMask, 0, n * n).flip();
            GL13.glActiveTexture((int)34011);
            GL11.glBindTexture((int)3553, (int)this.mask);
            GL11.glPixelStorei((int)3317, (int)1);
            GL11.glTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)n, (int)n, (int)6403, (int)5121, (ByteBuffer)this.maskPixels);
            GL11.glPixelStorei((int)3317, (int)4);
        }
        GL11.glBindTexture((int)3553, (int)0);
        GL13.glActiveTexture((int)33984);
    }
}

