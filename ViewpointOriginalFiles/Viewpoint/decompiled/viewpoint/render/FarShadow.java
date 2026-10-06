/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import java.util.Arrays;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.GpuParts;
import viewpoint.platform.Profile;
import viewpoint.platform.Tuning;
import viewpoint.render.FarFootprint;
import viewpoint.render.FarGpu;
import viewpoint.render.FarShadowMap;
import viewpoint.render.FrameContext;
import viewpoint.render.Meshes;
import viewpoint.render.SceneData;
import viewpoint.render.ShellArena;
import viewpoint.render.TreeBaker;
import viewpoint.render.TreeKinds;

final class FarShadow {
    private static final int FRAMES = 30;
    private static final int WIDE_FRAMES = 60;
    private static final int NEAR_FRAMES = 600;
    private static final float SERVED_PAST = 1.25f;
    private static final float WIDE_STEP = 256.0f;
    private static final float WIDE_RECENTRE = 256.0f;
    private static final float WIDE_DEPTH = 3.0f;
    private static final byte NEAR_CASTERS = 27;
    private static final float TREE_LEVELS = 4.0f;
    private final GlProgram near = GlProgram.create("far shadow near", "shadow.vert", "shadow.frag");
    private final GlProgram shell = GlProgram.create("far shadow shell", "far_shell.vert", "far_shadow_shell.frag");
    private final GlProgram box = GlProgram.create("far shadow box", "far.vert", "far_shadow_box.frag");
    private final GlProgram cell = GlProgram.create("far shadow cell", "far.vert", "far_shadow_cell.frag");
    private final GlProgram tree = GlProgram.create("far shadow tree", "far_tree.vert", "far_shadow_tree.frag");
    private final FarShadowMap sharp = new FarShadowMap(4096, 384.0f, 1200.0f, 64.0f, Gl.UNIT_CASCADE[3], 33190);
    private final FarShadowMap wide = new FarShadowMap(4096, 0.0f, 0.0f, 256.0f, Gl.UNIT_CASCADE[4], 33189);
    private final TreeBaker trees;
    private final Vector3f boxMin = new Vector3f();
    private final Vector3f boxMax = new Vector3f();
    private final FarFootprint footprint = new FarFootprint();
    private boolean active;
    private float treesGone;
    private int mask;
    private int shellMask;
    private FarShadowMap counting;
    private int cascade = -1;
    private final int[] cascadePieces = new int[3];
    private final long[] cascadeVertices = new long[3];
    private float sunDegrees;
    private float served;
    private float runX;
    private float runZ;

    FarShadow(TreeBaker treeBaker) {
        this.trees = treeBaker;
        Profile.farMapsReport = () -> "sharp " + this.sharp.report() + "; wide " + this.wide.report() + "; " + this.cascadesReport();
        this.near.sampler("uTexture", 0);
        this.shell.sampler("uTexture", 0);
        this.shell.sampler("uTreeAtlas", 42);
        for (GlProgram glProgram : new GlProgram[]{this.shell, this.box, this.cell, this.tree}) {
            glProgram.sampler("uMask", 27);
            glProgram.sampler("uShellMask", 28);
        }
        this.tree.sampler("uTrees", 43);
        this.tree.sampler("uKinds", 44);
    }

    FarShadowMap map(int n) {
        return n == 0 ? this.sharp : this.wide;
    }

    void off() {
        this.active = false;
        this.sharp.valid = false;
        this.wide.valid = false;
    }

    void draw(FrameContext frameContext, float f, int n, int n2, boolean bl) {
        boolean bl2;
        this.active = frameContext.pipeline.farShadows;
        this.treesGone = f;
        this.mask = n;
        this.shellMask = n2;
        SceneData sceneData = frameContext.scene;
        boolean bl3 = frameContext.sun && frameContext.pipeline.farShadows;
        double d = -sceneData.originX;
        double d2 = -sceneData.originZ;
        boolean bl4 = this.drawSharp(sceneData, bl3, bl, d, d2);
        float f2 = (float)Math.ceil((sceneData.fogEnd + 256.0f) / 256.0f) * 256.0f;
        this.wide.span(f2, f2 * 3.0f);
        ++this.wide.age;
        if (!bl3 || f2 <= this.sharp.reach) {
            this.wide.valid = false;
            return;
        }
        boolean bl5 = bl2 = !this.wide.valid || bl && FarShadow.stale(sceneData, this.wide, 60, d, d2);
        if (bl2 && !bl4) {
            this.redraw(sceneData, this.wide, 19, d, d2);
        }
        this.wide.carry(sceneData);
    }

    private boolean drawSharp(SceneData sceneData, boolean bl, boolean bl2, double d, double d2) {
        boolean bl3;
        ++this.sharp.age;
        if (!bl) {
            this.sharp.valid = false;
            return false;
        }
        boolean bl4 = bl3 = !this.sharp.valid || bl2 && FarShadow.stale(sceneData, this.sharp, 30, d, d2);
        if (bl3) {
            this.redraw(sceneData, this.sharp, 8, d, d2);
        }
        this.sharp.carry(sceneData);
        return bl3;
    }

    private static boolean stale(SceneData sceneData, FarShadowMap farShadowMap, int n, double d, double d2) {
        return farShadowMap.moved(sceneData, d, d2) || farShadowMap.age >= n && FarShadow.farSum(sceneData) != farShadowMap.drawnFar || farShadowMap.age >= 600 && FarShadow.nearSum(sceneData) != farShadowMap.drawnNear;
    }

    private void redraw(SceneData sceneData, FarShadowMap farShadowMap, int n, double d, double d2) {
        farShadowMap.drawnFar = FarShadow.farSum(sceneData);
        farShadowMap.drawnNear = FarShadow.nearSum(sceneData);
        GpuParts.begin(n);
        farShadowMap.begin(sceneData, d, d2);
        this.near.use();
        this.near.set("uAlphaRange", 0.3f, 2.0f);
        this.near.set("uFadeNoise", 0.0f);
        float f = 100000.0f;
        this.near.set("uEye", sceneData.sunX * f, sceneData.sunY * f, sceneData.sunZ * f);
        this.near.set("uViewProjection", farShadowMap.matrix);
        farShadowMap.count(0);
        Meshes.draw(sceneData, this.near, (byte)27);
        farShadowMap.counted(0, sceneData.meshCount, 0L);
        this.counting = farShadowMap;
        this.castInto(sceneData, farShadowMap.matrix, -1, 0.0f);
        this.counting = null;
        farShadowMap.end();
        GpuParts.end(n);
    }

    private static long nearSum(SceneData sceneData) {
        long l = 0L;
        for (int i = 0; i < sceneData.meshCount; ++i) {
            if ((sceneData.meshFlags[i] & 0x1B) == 0) continue;
            long l2 = (long)System.identityHashCode(sceneData.meshes[i]) * -7046029254386353131L;
            l += l2 ^ l2 >>> 29;
        }
        return l;
    }

    float cascadeReach(SceneData sceneData) {
        return this.sharp.valid ? (sceneData.nearReach + 8.0f) * 1.25f : 0.0f;
    }

    static long farSum(SceneData sceneData) {
        long l;
        Object object;
        int n;
        long l2 = (long)sceneData.shellCount * 31L + (long)sceneData.farCount;
        for (n = 0; n < sceneData.shellCount; ++n) {
            object = sceneData.shells[n];
            l = (long)System.identityHashCode(((FarGpu.BlockDraw)object).shell) * -7046029254386353131L + (long)System.identityHashCode(((FarGpu.BlockDraw)object).inside);
            l2 += l ^ l >>> 29;
        }
        for (n = 0; n < sceneData.farCount; ++n) {
            object = sceneData.far[n];
            l = (long)System.identityHashCode(((FarGpu.CellDraw)object).mesh) * -7046029254386353131L + (long)System.identityHashCode(((FarGpu.CellDraw)object).trees) * 31L + (long)((FarGpu.CellDraw)object).treeKeep;
            l2 += l ^ l >>> 29;
        }
        return l2;
    }

    long castersKey(SceneData sceneData) {
        if (!this.active) {
            return 0L;
        }
        long l = FarShadow.farSum(sceneData);
        for (int i = 0; i < sceneData.farCount; ++i) {
            l = l * 31L + (long)Float.floatToIntBits(sceneData.far[i].treeGrow);
        }
        l = l * 31L + (long)Arrays.hashCode(sceneData.farMask);
        l = l * 31L + ((long)sceneData.farMaskX * 31L + (long)sceneData.farMaskY) * 31L + (long)sceneData.farMaskSize;
        l = l * 31L + (long)Arrays.hashCode(sceneData.shellMask);
        l = l * 31L + (long)sceneData.shellMaskX * 31L + (long)sceneData.shellMaskY;
        l = l * 31L + (long)Float.floatToIntBits(sceneData.nearReach);
        l = l * 31L + (long)Float.floatToIntBits(this.treesGone);
        return l * 31L + (long)Float.floatToIntBits(this.cascadeReach(sceneData));
    }

    void castInto(SceneData sceneData, Matrix4f matrix4f, int n, float f) {
        this.cascade = n;
        if (n >= 0) {
            this.cascadePieces[n] = 0;
            this.cascadeVertices[n] = 0L;
            this.sunDegrees = (float)Math.toDegrees(Math.asin(Math.max(-1.0f, Math.min(1.0f, sceneData.sunY))));
        }
        if (!this.active) {
            return;
        }
        float f2 = this.cascadeReach(sceneData);
        this.served = n >= 0 && sceneData.sunY > 0.0f && f2 > 0.0f ? f2 + f : 0.0f;
        this.runX = this.served > 0.0f ? -sceneData.sunX / sceneData.sunY : 0.0f;
        this.runZ = this.served > 0.0f ? -sceneData.sunZ / sceneData.sunY : 0.0f;
        GL11.glEnable((int)34383);
        Gl.bind(27, this.mask);
        Gl.bind(28, this.shellMask);
        this.trees.bind(true);
        this.drawShell(sceneData, matrix4f);
        this.drawInsides(sceneData, matrix4f);
        this.drawCells(sceneData, matrix4f);
        this.drawTrees(sceneData, matrix4f);
        this.trees.bind(false);
        Gl.bind(27, 0);
        Gl.bind(28, 0);
        GL13.glActiveTexture((int)33984);
        GL30.glBindVertexArray((int)0);
        GL11.glDisable((int)34383);
    }

    private void drawShell(SceneData sceneData, Matrix4f matrix4f) {
        boolean bl = false;
        int n = 0;
        long l = 0L;
        for (int i = 0; i < sceneData.shellCount; ++i) {
            int n2;
            FarGpu.BlockDraw blockDraw = sceneData.shells[i];
            int n3 = n2 = blockDraw.shell == null || !this.meets(sceneData, matrix4f, blockDraw) ? -1 : blockDraw.shell.arenaFirst();
            if (n2 < 0) continue;
            if (!bl) {
                bl = true;
                this.shell.use();
                FarShadow.casterUniforms(this.shell, sceneData, matrix4f);
                this.shell.setInt("uCasting", 1);
                GL13.glActiveTexture((int)33984);
                ShellArena.begin();
            }
            int n4 = ShellArena.record(blockDraw.originX, blockDraw.originY, blockDraw.originZ, blockDraw.worldX, blockDraw.worldY, 0, 0.0f, 2.0f);
            ++n;
            for (int j = 0; j < blockDraw.shell.pages.length; ++j) {
                if (blockDraw.shell.pages[j] == null) continue;
                ShellArena.command(blockDraw.shell, j, n2 + blockDraw.shell.first[j], blockDraw.shell.count[j], n4);
                l += (long)blockDraw.shell.count[j];
            }
        }
        if (bl) {
            this.count(1);
            ShellArena.draw();
            this.counted(1, n, l);
            GL11.glBindTexture((int)3553, (int)0);
        }
    }

    private void count(int n) {
        if (this.counting != null) {
            this.counting.count(n);
        }
    }

    private void counted(int n, int n2, long l) {
        if (this.counting != null) {
            this.counting.counted(n, n2, l);
        } else if (this.cascade >= 0) {
            int n3 = this.cascade;
            this.cascadePieces[n3] = this.cascadePieces[n3] + n2;
            int n4 = this.cascade;
            this.cascadeVertices[n4] = this.cascadeVertices[n4] + l;
        }
    }

    private String cascadesReport() {
        return String.format("cascades near %d (%.2fM vertices), mid %d (%.2fM), far %d (%.2fM), sun %.0f degrees up", this.cascadePieces[0], (double)this.cascadeVertices[0] / 1000000.0, this.cascadePieces[1], (double)this.cascadeVertices[1] / 1000000.0, this.cascadePieces[2], (double)this.cascadeVertices[2] / 1000000.0, Float.valueOf(this.sunDegrees));
    }

    private void drawInsides(SceneData sceneData, Matrix4f matrix4f) {
        this.box.use();
        FarShadow.casterUniforms(this.box, sceneData, matrix4f);
        this.count(2);
        int n = 0;
        long l = 0L;
        for (int i = 0; i < sceneData.shellCount; ++i) {
            FarGpu.BlockDraw blockDraw = sceneData.shells[i];
            if (blockDraw.inside == null || blockDraw.inside.vertices == 0 || !this.meets(sceneData, matrix4f, blockDraw)) continue;
            this.box.set("uOrigin", blockDraw.originX, blockDraw.originY, blockDraw.originZ);
            this.box.set("uCellWorld", blockDraw.worldX, blockDraw.worldY);
            GL30.glBindVertexArray((int)FarGpu.vertexArray(blockDraw.inside));
            GL11.glDrawArrays((int)4, (int)0, (int)blockDraw.inside.vertices);
            ++Profile.draws;
            ++n;
            l += (long)blockDraw.inside.vertices;
        }
        this.counted(2, n, l);
    }

    private void drawCells(SceneData sceneData, Matrix4f matrix4f) {
        this.cell.use();
        FarShadow.casterUniforms(this.cell, sceneData, matrix4f);
        this.count(3);
        int n = 0;
        long l = 0L;
        for (int i = 0; i < sceneData.farCount; ++i) {
            FarGpu.CellDraw cellDraw = sceneData.far[i];
            if (cellDraw.mesh == null || cellDraw.mesh.vertices == 0 || !this.meets(sceneData, matrix4f, cellDraw)) continue;
            this.cell.set("uOrigin", cellDraw.originX, cellDraw.originY, cellDraw.originZ);
            this.cell.set("uCellWorld", cellDraw.worldX, cellDraw.worldY);
            GL30.glBindVertexArray((int)FarGpu.vertexArray(cellDraw.mesh));
            GL11.glDrawArrays((int)4, (int)0, (int)cellDraw.mesh.vertices);
            ++Profile.draws;
            ++n;
            l += (long)cellDraw.mesh.vertices;
        }
        this.counted(3, n, l);
    }

    private void drawTrees(SceneData sceneData, Matrix4f matrix4f) {
        if (!TreeKinds.bind()) {
            return;
        }
        this.tree.use();
        FarShadow.casterUniforms(this.tree, sceneData, matrix4f);
        this.tree.set("uPixel", 0.0f);
        TreeKinds.uniforms(this.tree);
        this.tree.set("uBareCrown", Tuning.farBareCrown);
        this.count(4);
        int n = 0;
        long l = 0L;
        for (int i = 0; i < sceneData.farCount; ++i) {
            int n2;
            FarGpu.CellDraw cellDraw = sceneData.far[i];
            float f = Math.max(0.0f, Math.max(cellDraw.originX - 256.0f, -cellDraw.originX));
            float f2 = Math.max(0.0f, Math.max(cellDraw.originZ - 256.0f, -cellDraw.originZ));
            if (cellDraw.trees == null || cellDraw.trees.count == 0 || f * f + f2 * f2 >= this.treesGone * this.treesGone || !this.meets(sceneData, matrix4f, cellDraw) || (n2 = cellDraw.trees.kept(cellDraw.treeKeep)) == 0) continue;
            this.tree.set("uGrow", cellDraw.treeGrow);
            this.tree.set("uOrigin", cellDraw.originX, cellDraw.originY, cellDraw.originZ);
            this.tree.set("uCellWorld", cellDraw.worldX, cellDraw.worldY);
            FarGpu.bindTrees(cellDraw.trees);
            Gl.generated(n2 * 6);
            ++n;
            l += (long)n2 * 6L;
        }
        this.counted(4, n, l);
        TreeKinds.unbind();
        GL13.glActiveTexture((int)34027);
        GL11.glBindTexture((int)35882, (int)0);
        GL13.glActiveTexture((int)33984);
    }

    private static void casterUniforms(GlProgram glProgram, SceneData sceneData, Matrix4f matrix4f) {
        float f = 100000.0f;
        glProgram.set("uViewProjection", matrix4f);
        glProgram.set("uMaskGrid", sceneData.farMaskX, sceneData.farMaskY, sceneData.farMaskSize);
        glProgram.set("uShellGrid", sceneData.shellMaskX, sceneData.shellMaskY, 53.0f);
        glProgram.set("uNearReach", sceneData.nearReach);
        if (glProgram.has("uEye")) {
            glProgram.set("uEye", sceneData.sunX * f, sceneData.sunY * f, sceneData.sunZ * f);
        }
    }

    private boolean meets(SceneData sceneData, Matrix4f matrix4f, FarGpu.BlockDraw blockDraw) {
        float f = blockDraw.originX - (float)blockDraw.blockX;
        float f2 = blockDraw.originZ - (float)blockDraw.blockY;
        float f3 = blockDraw.top * 2.4494896f;
        return this.casts(matrix4f, f - 64.0f, f2 - 64.0f, f, f2, blockDraw.originY, f3) && this.footprint.block(sceneData, blockDraw) && this.casts(matrix4f, this.footprint, blockDraw.originY, f3);
    }

    private boolean meets(SceneData sceneData, Matrix4f matrix4f, FarGpu.CellDraw cellDraw) {
        float f = (cellDraw.maxHeight + 4.0f) * 2.4494896f;
        return this.casts(matrix4f, cellDraw.originX - 256.0f, cellDraw.originZ - 256.0f, cellDraw.originX, cellDraw.originZ, cellDraw.originY, f) && this.footprint.cell(sceneData, cellDraw) && this.casts(matrix4f, this.footprint, cellDraw.originY, f);
    }

    private boolean casts(Matrix4f matrix4f, FarFootprint farFootprint, float f, float f2) {
        return this.casts(matrix4f, farFootprint.x0, farFootprint.z0, farFootprint.x1, farFootprint.z1, f, f2);
    }

    private boolean casts(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6) {
        return FarFootprint.reaches(this.served, this.runX, this.runZ, f, f2, f3, f4, f6) && this.meets(matrix4f, f, f5, f2, f3, f5 + f6, f4);
    }

    private boolean meets(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6) {
        matrix4f.transformAab(f, f2, f3, f4, f5, f6, this.boxMin, this.boxMax);
        return this.boxMax.x >= -1.0f && this.boxMin.x <= 1.0f && this.boxMax.y >= -1.0f && this.boxMin.y <= 1.0f && this.boxMin.z <= 1.0f;
    }

    void bind(boolean bl) {
        this.sharp.bind(bl);
        this.wide.bind(bl);
    }
}

