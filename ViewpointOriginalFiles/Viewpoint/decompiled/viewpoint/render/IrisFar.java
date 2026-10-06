/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.FrustumIntersection
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import java.util.Map;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.Tuning;
import viewpoint.render.FarGpu;
import viewpoint.render.FarPass;
import viewpoint.render.FloorFilter;
import viewpoint.render.FrameContext;
import viewpoint.render.IrisGeometry;
import viewpoint.render.IrisPipeline;
import viewpoint.render.IrisProgram;
import viewpoint.render.IrisPrograms;
import viewpoint.render.SceneData;
import viewpoint.render.ShellArena;
import viewpoint.render.TextureFilter;
import viewpoint.render.TreeKinds;
import viewpoint.render.WorldRenderer;

final class IrisFar {
    private static final float NEAR_DITHER = 16.0f;
    private final FrustumIntersection frustum = new FrustumIntersection();
    private final Matrix4f viewProjection = new Matrix4f();

    IrisFar() {
    }

    void terrain(IrisPipeline irisPipeline, Map<String, float[]> map, FrameContext frameContext, FarPass farPass, IrisGeometry irisGeometry) {
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.DH_TERRAIN);
        SceneData sceneData = frameContext.scene;
        if (irisProgram == null || sceneData.farCount == 0) {
            return;
        }
        this.viewProjection.set((Matrix4fc)irisPipeline.uniforms.farProjection);
        this.frustum.set((Matrix4fc)this.viewProjection.mul((Matrix4fc)(WorldRenderer.cullFromPlayer ? WorldRenderer.cullView : frameContext.view)));
        irisGeometry.begin(irisPipeline, irisProgram, map, irisPipeline.uniforms.modelView, irisPipeline.uniforms.farProjection, "NONE", frameContext, false);
        IrisFar.common(irisPipeline, irisProgram, frameContext, farPass);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)513);
        GL11.glDepthMask((boolean)true);
        GL11.glEnable((int)2884);
        GL11.glCullFace((int)1029);
        this.boxes(irisProgram.gl, frameContext, 0.0f);
        GL11.glDisable((int)2884);
        irisPipeline.bindings.unbind(irisProgram);
        this.shells(irisPipeline, map, frameContext, farPass, irisGeometry);
        IrisProgram irisProgram2 = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.DH_TREES);
        if (irisProgram2 != null && TreeKinds.bind()) {
            irisGeometry.begin(irisPipeline, irisProgram2, map, irisPipeline.uniforms.modelView, irisPipeline.uniforms.farProjection, "NONE", frameContext, false);
            IrisFar.common(irisPipeline, irisProgram2, frameContext, farPass);
            TreeKinds.uniforms(irisProgram2.gl);
            this.trees(irisProgram2.gl, frameContext);
            TreeKinds.unbind();
            irisPipeline.bindings.unbind(irisProgram2);
        }
        GL30.glBindVertexArray((int)0);
        Gl.bind(27, 0);
        Gl.bind(28, 0);
        Gl.bind(25, 0);
        Gl.bind(26, 0);
        GL13.glActiveTexture((int)33984);
    }

    void shadow(IrisPipeline irisPipeline, Map<String, float[]> map, FrameContext frameContext, FarPass farPass, IrisGeometry irisGeometry) {
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.DH_SHADOW);
        if (irisProgram == null || frameContext.scene.farCount == 0) {
            return;
        }
        irisGeometry.begin(irisPipeline, irisProgram, map, irisPipeline.uniforms.shadowView, irisPipeline.uniforms.shadowProjection, "NONE", frameContext, true);
        IrisFar.common(irisPipeline, irisProgram, frameContext, farPass);
        Gl.patches(true);
        this.boxes(irisProgram.gl, frameContext, irisPipeline.shadowDistance);
        Gl.patches(false);
        GL30.glBindVertexArray((int)0);
        irisPipeline.bindings.unbind(irisProgram);
        Gl.bind(27, 0);
    }

    private void shells(IrisPipeline irisPipeline, Map<String, float[]> map, FrameContext frameContext, FarPass farPass, IrisGeometry irisGeometry) {
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.DH_SHELL);
        SceneData sceneData = frameContext.scene;
        if (irisProgram == null || sceneData.shellCount == 0) {
            return;
        }
        int n = GL11.glGetInteger((int)32873);
        irisGeometry.begin(irisPipeline, irisProgram, map, irisPipeline.uniforms.modelView, irisPipeline.uniforms.farProjection, "NONE", frameContext, false);
        IrisFar.common(irisPipeline, irisProgram, frameContext, farPass);
        irisProgram.gl.set("uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        irisProgram.gl.setInt("uIrisShadow", 0);
        FloorFilter.apply(irisProgram.gl);
        TextureFilter.SPRITES.apply(irisProgram.gl);
        farPass.trees.bind(true);
        GL13.glActiveTexture((int)33984);
        ShellArena.begin();
        for (int i = 0; i < sceneData.shellCount; ++i) {
            FarGpu.BlockDraw blockDraw = sceneData.shells[i];
            if (blockDraw.shell == null || !this.blockInView(blockDraw)) continue;
            FarPass.command(blockDraw);
        }
        ShellArena.draw();
        farPass.trees.bind(false);
        GL13.glActiveTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)n);
        irisPipeline.bindings.unbind(irisProgram);
    }

    private boolean blockInView(FarGpu.BlockDraw blockDraw) {
        float f = blockDraw.originX - (float)blockDraw.blockX;
        float f2 = f - 64.0f;
        float f3 = blockDraw.originZ - (float)blockDraw.blockY;
        float f4 = f3 - 64.0f;
        return this.frustum.testAab(f2, blockDraw.originY, f4, f, blockDraw.originY + blockDraw.top * 2.4494896f, f3);
    }

    private static void common(IrisPipeline irisPipeline, IrisProgram irisProgram, FrameContext frameContext, FarPass farPass) {
        SceneData sceneData = frameContext.scene;
        GlProgram glProgram = irisProgram.gl;
        if (WorldRenderer.freeCamera) {
            irisProgram.set("far", irisPipeline.uniforms.farNear);
        }
        glProgram.set("uIrisEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        glProgram.set("uIrisNearKept", irisPipeline.uniforms.far - 16.0f);
        glProgram.set("uMaskGrid", sceneData.farMaskX, sceneData.farMaskY, sceneData.farMaskSize);
        glProgram.set("uShellGrid", sceneData.shellMaskX, sceneData.shellMaskY, 53.0f);
        glProgram.set("uFadeNoise", frameContext.fadeNoise());
        Gl.bind(27, farPass.mask);
        Gl.bind(28, farPass.shellMask);
    }

    private void boxes(GlProgram glProgram, FrameContext frameContext, float f) {
        SceneData sceneData = frameContext.scene;
        for (int i = 0; i < sceneData.farCount; ++i) {
            boolean bl;
            FarGpu.CellDraw cellDraw = sceneData.far[i];
            float f2 = cellDraw.originY + cellDraw.maxHeight * 2.4494896f;
            float f3 = cellDraw.originX - 256.0f;
            float f4 = cellDraw.originZ - 256.0f;
            boolean bl2 = f > 0.0f ? IrisFar.nearest(cellDraw, frameContext.eyeX, frameContext.eyeZ) < f : (bl = this.frustum.testAab(f3, cellDraw.originY, f4, cellDraw.originX, f2, cellDraw.originZ));
            if (!bl) continue;
            FarGpu.textures(cellDraw.colours);
            Gl.bind(25, cellDraw.colours.topTexture);
            Gl.bind(26, cellDraw.colours.sideTexture);
            glProgram.set("uOrigin", cellDraw.originX, cellDraw.originY, cellDraw.originZ);
            glProgram.set("uCellWorld", cellDraw.worldX, cellDraw.worldY);
            glProgram.set("uFade", cellDraw.fadeLo, cellDraw.fadeHi);
            GL30.glBindVertexArray((int)FarGpu.vertexArray(cellDraw.mesh));
            GL11.glDrawArrays((int)Gl.triangles(), (int)0, (int)cellDraw.mesh.vertices);
        }
    }

    private void trees(GlProgram glProgram, FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        float f = FarPass.TREES_GONE.get() * 64.0f;
        glProgram.set("uTreeFade", f, f);
        glProgram.set("uTreeVariation", Tuning.farTreeVariation, Tuning.farTreeRelief);
        glProgram.set("uBareCrown", Tuning.farBareCrown);
        glProgram.set("uPixel", 2.0f / (frameContext.scene.projection.m11() * (float)frameContext.height));
        for (int i = 0; i < sceneData.farCount; ++i) {
            int n;
            FarGpu.CellDraw cellDraw = sceneData.far[i];
            float f2 = cellDraw.originY + cellDraw.maxHeight * 2.4494896f;
            float f3 = cellDraw.originX - 256.0f;
            float f4 = cellDraw.originZ - 256.0f;
            if (cellDraw.trees == null || cellDraw.trees.count == 0 || IrisFar.nearest(cellDraw, frameContext.eyeX, frameContext.eyeZ) >= f || !this.frustum.testAab(f3, cellDraw.originY, f4, cellDraw.originX, f2, cellDraw.originZ) || (n = cellDraw.trees.kept(cellDraw.treeKeep)) == 0) continue;
            glProgram.set("uGrow", cellDraw.treeGrow);
            glProgram.set("uOrigin", cellDraw.originX, cellDraw.originY, cellDraw.originZ);
            glProgram.set("uCellWorld", cellDraw.worldX, cellDraw.worldY);
            glProgram.set("uFade", cellDraw.fadeLo, cellDraw.fadeHi);
            FarGpu.bindTrees(cellDraw.trees);
            Gl.generated(n * 6);
        }
        GL13.glActiveTexture((int)34027);
        GL11.glBindTexture((int)35882, (int)0);
        GL13.glActiveTexture((int)33984);
    }

    private static float nearest(FarGpu.CellDraw cellDraw, float f, float f2) {
        float f3 = Math.max(0.0f, Math.max(cellDraw.originX - 256.0f - f, f - cellDraw.originX));
        float f4 = Math.max(0.0f, Math.max(cellDraw.originZ - 256.0f - f2, f2 - cellDraw.originZ));
        return (float)Math.sqrt(f3 * f3 + f4 * f4);
    }
}

