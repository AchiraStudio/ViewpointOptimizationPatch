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
 *  org.lwjgl.opengl.GL33
 *  org.lwjgl.opengl.GL43
 *  org.lwjgl.system.MemoryUtil
 *  zombie.core.skinnedmodel.model.ModelOutlines
 *  zombie.core.textures.ColorInfo
 *  zombie.core.textures.Texture
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.HotReload;
import viewpoint.platform.Profile;
import viewpoint.render.CharacterTextures;
import viewpoint.render.FrameContext;
import viewpoint.render.FrameStream;
import viewpoint.render.ItemTextures;
import viewpoint.render.ModelBatches;
import viewpoint.render.ModelDraws;
import viewpoint.render.ModelMeshes;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;
import viewpoint.render.Targets;
import viewpoint.render.TextureFilter;
import viewpoint.render.VehiclePass;
import viewpoint.render.Wireframe;
import viewpoint.render.WorldRenderer;
import zombie.core.skinnedmodel.model.ModelOutlines;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;

final class ModelPass {
    private final GlProgram gbuffer = GlProgram.create("model", "model.vert", "model_gbuffer.frag");
    private final GlProgram shadow = GlProgram.create("model shadow", "model.vert", "model_shadow.frag");
    private final GlProgram outline = GlProgram.create("model outline", "model.vert", "model_outline.frag");
    private final ColorInfo outlineColour = new ColorInfo();
    private final FrustumIntersection frustum = new FrustumIntersection();
    private static final float[] STILL = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
    private final Targets targets;
    private ModelDraws draws;
    private ModelMeshes.Held[] held = new ModelMeshes.Held[256];
    private Texture[] drawn = new Texture[256];
    private final ItemTextures items = new ItemTextures();
    final ModelBatches batches = new ModelBatches();
    private final VehiclePass vehicles = new VehiclePass(this);
    private int paletteTexture;
    private int recordTexture;
    private Range palettes;
    private Range records;
    private FloatBuffer upload = MemoryUtil.memAllocFloat((int)49152);
    private long frameCounter;
    private int reloads;
    private int found;
    private final CharacterTextures characterTextures = new CharacterTextures();
    private final ModelBatches.State runs = new ModelBatches.State(){

        @Override
        public void set(int n) {
            ModelPass.this.cull(n);
        }

        @Override
        public void texture(int n, int n2) {
            ModelPass.this.bind(64 + n, n2);
        }
    };

    ModelPass(Targets targets) {
        this.targets = targets;
        for (GlProgram glProgram : new GlProgram[]{this.gbuffer, this.shadow, this.outline}) {
            glProgram.sampler("uPalettes", 31);
            glProgram.sampler("uDraws", 40);
            for (int i = 0; i < 16; ++i) {
                glProgram.sampler("uTextures[" + i + "]", 64 + i);
            }
        }
    }

    static void samplers(GlProgram glProgram) {
        glProgram.sampler("uTexture", 0);
        glProgram.sampler("uPalettes", 31);
        glProgram.sampler("uDraws", 40);
    }

    void prepare(SceneData sceneData) {
        ModelDraws modelDraws;
        this.draws = modelDraws = sceneData.models;
        ++this.frameCounter;
        if (this.reloads != HotReload.generation) {
            this.reloads = HotReload.generation;
            ModelMeshes.clear();
        }
        this.characterTextures.ready(modelDraws);
        if (this.held.length < modelDraws.count) {
            this.held = new ModelMeshes.Held[modelDraws.meshes.length];
            this.drawn = new Texture[modelDraws.meshes.length];
        }
        boolean bl = modelDraws.slotCount > 0;
        for (int i = 0; i < modelDraws.count; ++i) {
            Texture texture = modelDraws.items[i] == null ? null : this.items.make(modelDraws.items[i]);
            bl |= texture != null;
            this.drawn[i] = texture != null ? texture : ModelPass.texture(modelDraws, i);
            this.held[i] = ModelMeshes.available() ? ModelMeshes.get(modelDraws.meshes[i], this.frameCounter) : null;
        }
        this.characterTextures.leaveOut(modelDraws, this.held);
        if (bl) {
            Gl.resetState();
        }
        ModelMeshes.makeRoom(this.frameCounter);
        this.batches.order(modelDraws, this.held, this.drawn);
        if (modelDraws.count == 0) {
            return;
        }
        if (this.paletteTexture == 0) {
            this.paletteTexture = GL11.glGenTextures();
            this.recordTexture = GL11.glGenTextures();
        }
        this.palettes = this.fill(modelDraws.palettes, modelDraws.bones * 12);
        this.records = this.fill(modelDraws.values, modelDraws.count * 60);
    }

    private Range fill(float[] fArray, int n) {
        if (this.upload.capacity() < n) {
            MemoryUtil.memFree((FloatBuffer)this.upload);
            this.upload = MemoryUtil.memAllocFloat((int)(n * 2));
        }
        this.upload.clear();
        this.upload.put(fArray, 0, n).flip();
        long l = FrameStream.put(this.upload, FrameStream.textureAlignment());
        return new Range(FrameStream.buffer(), l, (long)n * 4L);
    }

    private static void attach(int n, Range range) {
        GL11.glBindTexture((int)35882, (int)n);
        if (range != null && range.bytes() > 0L) {
            GL43.glTexBufferRange((int)35882, (int)34836, (int)range.buffer(), (long)range.at(), (long)range.bytes());
        }
    }

    void gbuffer(FrameContext frameContext) {
        ModelDraws modelDraws = frameContext.scene.models;
        Profile.paletteBones = 0;
        Profile.modelRuns = 0;
        Profile.ownedModels = 0;
        Profile.paletteBytes = 0L;
        Profile.modelTriangles = 0L;
        this.targets.velocity(true);
        GL30.glClearBufferfv((int)6144, (int)3, (float[])STILL);
        if (modelDraws.count == 0) {
            this.targets.velocity(false);
            return;
        }
        this.frustum.set((Matrix4fc)frameContext.viewProjection);
        this.gbuffer.use();
        Wireframe.apply(this.gbuffer);
        TextureFilter.MODELS.apply(this.gbuffer);
        this.gbuffer.set("uViewProjection", frameContext.viewProjection);
        ModelPass.views(this.gbuffer, frameContext);
        this.gbuffer.set("uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        this.gbuffer.set("uFadeNoise", frameContext.fadeNoise());
        this.begin();
        GL11.glEnable((int)2960);
        GL11.glStencilFunc((int)519, (int)1, (int)255);
        GL11.glStencilOp((int)7680, (int)7680, (int)7681);
        int n2 = this.batches.draw(n -> modelDraws.material[n] < 0 && this.shown(modelDraws, n), this.runs);
        Profile.modelRuns = this.batches.runsDrawn;
        Profile.modelTriangles = this.batches.elementsDrawn / 3L;
        Profile.paletteBones = modelDraws.bones;
        Profile.paletteBytes = (long)modelDraws.bones * 12L * 4L;
        GL11.glDisable((int)2960);
        this.targets.velocity(false);
        this.end();
        Profile.ownedModels = n2 += this.vehicles.gbuffer(frameContext);
    }

    void shadow(SceneData sceneData, Matrix4f matrix4f, float f, float f2) {
        ModelDraws modelDraws = sceneData.models;
        this.shadow(sceneData, matrix4f, null, 0, n -> ModelPass.casts(modelDraws, n, f, f2));
    }

    void shadow(SceneData sceneData, Matrix4f matrix4f, int[] nArray, int n2) {
        this.shadow(sceneData, matrix4f, nArray, n2, n -> true);
    }

    private void shadow(SceneData sceneData, Matrix4f matrix4f, int[] nArray, int n2, ModelBatches.Filter filter) {
        ModelDraws modelDraws = sceneData.models;
        if (modelDraws.count == 0) {
            return;
        }
        this.shadow.use();
        this.shadow.set("uViewProjection", matrix4f);
        this.shadow.setInt("uShadowPalette", 1);
        this.shadow.set("uFadeNoise", 0.0f);
        this.begin();
        this.batches.draw(nArray, n2, n -> modelDraws.material[n] < 0 && filter.take(n), this.runs);
        this.vehicles.shadow(modelDraws, matrix4f, nArray, n2, filter, false);
        this.end();
    }

    void iris(GlProgram glProgram, SceneData sceneData, Matrix4f matrix4f, float f, boolean bl) {
        ModelDraws modelDraws = sceneData.models;
        if (modelDraws.count == 0) {
            return;
        }
        boolean bl2 = f > 0.0f;
        glProgram.setInt("uShadowPalette", bl2 ? 1 : 0);
        this.frustum.set((Matrix4fc)matrix4f);
        this.begin();
        this.batches.draw(n -> bl2 ? ModelPass.casts(modelDraws, n, 0.0f, f) : modelDraws.material[n] < 0 && ModelPass.block(modelDraws, n) == bl && this.shown(modelDraws, n), this.runs);
        this.end();
    }

    void irisDone() {
        this.items.release();
    }

    private static boolean block(ModelDraws modelDraws, int n) {
        return modelDraws.palette[n] < 0 && modelDraws.items[n] == null;
    }

    void irisVehicles(GlProgram glProgram, SceneData sceneData, boolean bl) {
        if (sceneData.models.vehicles.count() == 0) {
            return;
        }
        glProgram.setInt("uShadowPalette", 0);
        this.begin();
        this.vehicles.iris(glProgram, sceneData.models, bl);
        this.end();
    }

    void rain(SceneData sceneData, Matrix4f matrix4f, float f) {
        if (sceneData.models.vehicles.count() == 0) {
            return;
        }
        ModelDraws modelDraws = sceneData.models;
        this.begin();
        this.vehicles.shadow(modelDraws, matrix4f, null, 0, n -> ModelPass.casts(modelDraws, n, 0.0f, f), true);
        this.end();
    }

    void glass(FrameContext frameContext, ShadowPass shadowPass) {
        if (frameContext.scene.models.vehicles.count() == 0) {
            return;
        }
        this.begin();
        this.vehicles.glass(frameContext, shadowPass);
        this.end();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void outlines(FrameContext frameContext) {
        ModelDraws modelDraws = frameContext.scene.models;
        if (!modelDraws.outlined) {
            return;
        }
        int n = TextureFBO.lastID;
        TextureFBO.lastID = WorldRenderer.targetFbo();
        try {
            int n2 = 0;
            while (n2 < modelDraws.count) {
                int n3;
                if (modelDraws.values[n2 * 60 + 32 + 3] > 0.0f) {
                    for (n3 = n2 + 1; n3 < modelDraws.count && ModelPass.sameOutline(modelDraws, n2, n3); ++n3) {
                    }
                    this.silhouettes(frameContext, modelDraws, n2, n3);
                }
                n2 = n3;
            }
        }
        finally {
            TextureFBO.lastID = n;
            Gl.resetState();
        }
    }

    private void silhouettes(FrameContext frameContext, ModelDraws modelDraws, int n, int n2) {
        int n4 = n * 60 + 32;
        float[] fArray = modelDraws.values;
        this.outlineColour.set(fArray[n4], fArray[n4 + 1], fArray[n4 + 2], fArray[n4 + 3]);
        boolean bl = (modelDraws.flags[n] & 4) != 0;
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glDisable((int)2929);
        GL11.glDepthMask((boolean)false);
        boolean bl2 = ModelOutlines.instance.beginRenderOutline(this.outlineColour, bl, false);
        GL11.glDepthMask((boolean)true);
        GL11.glDisable((int)3089);
        ModelOutlines.instance.fboA.startDrawing(bl2, true);
        GL11.glViewport((int)frameContext.viewport[0], (int)frameContext.viewport[1], (int)frameContext.viewport[2], (int)frameContext.viewport[3]);
        GL11.glDisable((int)3042);
        this.outline.use();
        this.outline.set("uViewProjection", frameContext.plainViewProjection);
        this.outline.set("uColour", fArray[n4], fArray[n4 + 1], fArray[n4 + 2]);
        this.begin();
        this.batches.draw(n3 -> n3 >= n && n3 < n2 && (modelDraws.flags[n3] & 1) != 0, this.runs);
        this.end();
        ModelOutlines.instance.fboA.endDrawing();
    }

    void targets(FrameContext frameContext) {
        ModelDraws modelDraws = frameContext.scene.models;
        int n = 17;
        if (!modelDraws.targeted) {
            return;
        }
        this.outline.use();
        this.outline.set("uViewProjection", frameContext.plainViewProjection);
        this.outline.set("uColour", 1.0f, 1.0f, 1.0f);
        this.begin();
        this.batches.draw(n2 -> (modelDraws.flags[n2] & n) == n, this.runs);
        this.end();
    }

    void endFrame() {
        this.items.release();
    }

    private static boolean sameOutline(ModelDraws modelDraws, int n, int n2) {
        for (int i = 0; i < 4; ++i) {
            if (modelDraws.values[n * 60 + 32 + i] == modelDraws.values[n2 * 60 + 32 + i]) continue;
            return false;
        }
        return (modelDraws.flags[n] & 4) == (modelDraws.flags[n2] & 4);
    }

    boolean shown(ModelDraws modelDraws, int n) {
        int n2 = n * 60 + 24;
        float[] fArray = modelDraws.values;
        return (modelDraws.flags[n] & 1) != 0 && this.frustum.testSphere(fArray[n2], fArray[n2 + 1], fArray[n2 + 2], fArray[n2 + 3]);
    }

    static boolean casts(ModelDraws modelDraws, int n, float f, float f2) {
        int n2 = n * 60 + 24;
        float f3 = modelDraws.values[n2];
        float f4 = modelDraws.values[n2 + 2];
        float f5 = (float)Math.sqrt(f3 * f3 + f4 * f4);
        return (modelDraws.flags[n] & 2) != 0 && f5 >= f && f5 <= f2;
    }

    static void views(GlProgram glProgram, FrameContext frameContext) {
        glProgram.set("uPlainViewProjection", frameContext.plainViewProjection);
        glProgram.set("uPrevViewProjection", frameContext.previousViewProjection);
        glProgram.setInt("uMotionPass", 1);
    }

    void surface(int n) {
        this.cull(n);
        this.bind(0, n);
    }

    private void cull(int n) {
        if (this.draws.cull[n] == 0) {
            GL11.glDisable((int)2884);
        } else {
            GL11.glEnable((int)2884);
            GL11.glCullFace((int)this.draws.cull[n]);
        }
    }

    private void bind(int n, int n2) {
        GL13.glActiveTexture((int)(33984 + n));
        Texture.lastTextureID = -1;
        this.drawn[n2].bind();
        Texture.lastTextureID = -1;
        GL33.glBindSampler((int)n, (int)TextureFilter.MODELS.sampler(this.drawn[n2].getID()));
    }

    private static Texture texture(ModelDraws modelDraws, int n) {
        return modelDraws.instances[n] != null && modelDraws.instances[n].tex != null ? modelDraws.instances[n].tex : modelDraws.textures[n];
    }

    private void begin() {
        GL13.glActiveTexture((int)34015);
        ModelPass.attach(this.paletteTexture, this.palettes);
        GL13.glActiveTexture((int)34024);
        ModelPass.attach(this.recordTexture, this.records);
        GL13.glActiveTexture((int)33984);
        this.found = GL11.glGetInteger((int)32873);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)513);
        GL11.glDepthMask((boolean)true);
    }

    private void end() {
        GL30.glBindVertexArray((int)0);
        GL11.glDisable((int)2884);
        for (int n : new int[]{31, 40}) {
            GL13.glActiveTexture((int)(33984 + n));
            GL11.glBindTexture((int)35882, (int)0);
        }
        for (int i = 0; i < 16; ++i) {
            GL33.glBindSampler((int)(64 + i), (int)0);
        }
        GL13.glActiveTexture((int)33984);
        GL33.glBindSampler((int)0, (int)0);
        GL11.glBindTexture((int)3553, (int)this.found);
        Texture.lastTextureID = -1;
    }

    private record Range(int buffer, long at, long bytes) {
    }
}

