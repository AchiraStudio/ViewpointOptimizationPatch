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
 *  org.lwjgl.opengl.GL43
 *  org.lwjgl.opengl.GL45
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import java.util.Arrays;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;
import viewpoint.platform.GlDebug;
import viewpoint.platform.GlProgram;
import viewpoint.platform.GpuParts;
import viewpoint.platform.Pipeline;
import viewpoint.platform.Profile;
import viewpoint.platform.Tuning;
import viewpoint.render.FrameContext;
import viewpoint.render.Meshes;
import viewpoint.render.ModelDraws;
import viewpoint.render.ModelPass;
import viewpoint.render.SceneData;
import viewpoint.render.WorldRenderer;

final class LampShadows {
    static final int FACES = 6;
    static final float[][] FORWARD = new float[][]{{1.0f, 0.0f, 0.0f}, {-1.0f, 0.0f, 0.0f}, {0.0f, 1.0f, 0.0f}, {0.0f, -1.0f, 0.0f}, {0.0f, 0.0f, 1.0f}, {0.0f, 0.0f, -1.0f}};
    static final float[][] UP = new float[][]{{0.0f, 1.0f, 0.0f}, {0.0f, 1.0f, 0.0f}, {0.0f, 0.0f, 1.0f}, {0.0f, 0.0f, 1.0f}, {0.0f, 1.0f, 0.0f}, {0.0f, 1.0f, 0.0f}};
    private static final float NEAR = 0.6f;
    private static final float OWN_FIXTURE = 0.5f;
    private static final int FACES_A_FRAME = 16;
    static final int POSE_FRAMES = 4;
    private static final byte LAMP_MESH = -128;
    private static final float FADE_IN = 0.25f;
    private static final float MESH_RADIUS = 8.0f;
    private static final long FREE = -1L;
    private final GlProgram program = GlProgram.create("lamp shadow", "shadow.vert", "shadow.frag");
    private final ModelPass models;
    private final FrustumIntersection frustum = new FrustumIntersection();
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f faceViewProjection = new Matrix4f();
    private final FloatBuffer slotData = BufferUtils.createFloatBuffer((int)32);
    private int texture;
    private int fbo;
    private int size;
    private int slots;
    private long[] keys = new long[0];
    private long[] worldSums = new long[0];
    private long[] modelSums = new long[0];
    private float[] shown = new float[0];
    private boolean[] drawn = new boolean[0];
    private int count;
    private final int[] slotOf = new int[8];
    private int[] near = new int[64];
    private int[] nearModels = new int[64];
    private int[] faceModels = new int[16];
    private int nearCount;
    private int nearModelCount;
    private int faceModelCount;
    private boolean facePosed;
    private long tick;
    private long facesDrawn;
    private long worldsDrawn;
    private long posesDrawn;
    private long frames;

    LampShadows(ModelPass modelPass) {
        this.models = modelPass;
        this.program.sampler("uTexture", 0);
        Profile.lampShadowsReport = this::report;
    }

    static void sampler(GlProgram glProgram) {
        glProgram.sampler("uLampShadow", 49);
    }

    void draw(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        Pipeline pipeline = frameContext.pipeline;
        ++this.tick;
        ++this.frames;
        if (pipeline.lampShadows == 0 && this.texture != 0) {
            this.release();
        }
        int n = this.count = WorldRenderer.off(8) ? 0 : Math.min(Math.min(sceneData.lampShadowCount, pipeline.lampShadows), this.slotOf.length);
        if (this.count == 0) {
            return;
        }
        GlDebug.push("lamp shadows");
        GpuParts.begin(17);
        this.resize(pipeline.lampShadows, pipeline.lampShadowResolution);
        this.assign(sceneData);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)513);
        GL11.glDepthMask((boolean)true);
        GL11.glEnable((int)32823);
        GL11.glPolygonOffset((float)1.5f, (float)3.0f);
        int n2 = 16;
        for (int i = 0; i < this.count; ++i) {
            n2 = this.lamp(sceneData, i, n2);
        }
        GL11.glDisable((int)32823);
        GpuParts.end(17);
        GlDebug.pop();
    }

    void bind(boolean bl) {
        GL13.glActiveTexture((int)34033);
        GL11.glBindTexture((int)35866, (int)(bl && this.count > 0 ? this.texture : 0));
        GL13.glActiveTexture((int)33984);
    }

    void uniforms(GlProgram glProgram, SceneData sceneData) {
        if (!glProgram.has("uLampShadowCount")) {
            return;
        }
        glProgram.setInt("uLampShadowCount", this.count);
        if (this.count == 0) {
            return;
        }
        this.slotData.clear();
        for (int i = 0; i < this.count; ++i) {
            int n = this.slotOf[i];
            int n2 = i * 10;
            this.slotData.put(n * 6).put(this.shown[n] * sceneData.lights[n2 + 9]).put(0.6f).put(LampShadows.far(sceneData.lights[n2 + 3]));
        }
        this.slotData.flip();
        glProgram.setVec4s("uLampShadowSlot", this.slotData);
        glProgram.set("uLampShadowFloor", Tuning.lampShadowFloor);
    }

    private void resize(int n, int n2) {
        if (n == this.slots && n2 == this.size) {
            return;
        }
        this.release();
        this.slots = n;
        this.size = n2;
        int n3 = this.slots * 6 * 2;
        this.texture = GL45.glCreateTextures((int)35866);
        GL45.glTextureParameteri((int)this.texture, (int)10241, (int)9729);
        GL45.glTextureParameteri((int)this.texture, (int)10240, (int)9729);
        GL45.glTextureParameteri((int)this.texture, (int)10242, (int)33071);
        GL45.glTextureParameteri((int)this.texture, (int)10243, (int)33071);
        GL45.glTextureParameteri((int)this.texture, (int)34892, (int)34894);
        GL45.glTextureParameteri((int)this.texture, (int)34893, (int)515);
        GL45.glTextureStorage3D((int)this.texture, (int)1, (int)33189, (int)this.size, (int)this.size, (int)n3);
        this.fbo = GL45.glCreateFramebuffers();
        GL45.glNamedFramebufferDrawBuffer((int)this.fbo, (int)0);
        GL45.glNamedFramebufferReadBuffer((int)this.fbo, (int)0);
        this.keys = new long[this.slots];
        Arrays.fill(this.keys, -1L);
        this.shown = new float[this.slots];
        this.worldSums = new long[this.slots * 6];
        this.modelSums = new long[this.slots * 6];
        this.drawn = new boolean[this.slots * 6];
        System.out.println("[Viewpoint] lamp shadows: " + this.slots + " cubes of " + this.size + " texels a side, " + ((long)n3 * (long)this.size * (long)this.size * 2L >> 20) + " MB");
    }

    private void release() {
        if (this.texture != 0) {
            GL11.glDeleteTextures((int)this.texture);
            GL30.glDeleteFramebuffers((int)this.fbo);
            this.fbo = 0;
            this.texture = 0;
            this.size = 0;
            this.slots = 0;
        }
    }

    private void assign(SceneData sceneData) {
        int n;
        for (n = 0; n < this.slots; ++n) {
            if (this.keys[n] == -1L || this.light(sceneData, this.keys[n]) >= 0) continue;
            this.keys[n] = -1L;
        }
        for (n = 0; n < this.count; ++n) {
            int n2;
            long l = sceneData.lightKeys[n];
            for (n2 = 0; n2 < this.slots && this.keys[n2] != l; ++n2) {
            }
            if (n2 == this.slots) {
                n2 = 0;
                while (this.keys[n2] != -1L) {
                    ++n2;
                }
                this.keys[n2] = l;
                this.shown[n2] = 0.0f;
                Arrays.fill(this.drawn, n2 * 6, n2 * 6 + 6, false);
            }
            this.slotOf[n] = n2;
        }
    }

    private int light(SceneData sceneData, long l) {
        for (int i = 0; i < this.count; ++i) {
            if (sceneData.lightKeys[i] != l) continue;
            return i;
        }
        return -1;
    }

    private int lamp(SceneData sceneData, int n, int n2) {
        int n3 = this.slotOf[n];
        int n4 = n * 10;
        float f = sceneData.lights[n4];
        float f2 = sceneData.lights[n4 + 1];
        float f3 = sceneData.lights[n4 + 2];
        float f4 = sceneData.lights[n4 + 3];
        this.gather(sceneData, f, f2, f3, f4);
        this.projection.setPerspective((float)Math.toRadians(90.0), 1.0f, 0.6f, LampShadows.far(f4));
        boolean bl = true;
        for (int i = 0; i < 6; ++i) {
            boolean bl2;
            int n5 = n3 * 6 + i;
            this.faceViewProjection.set((Matrix4fc)this.projection).lookAt(f, f2, f3, f + FORWARD[i][0], f2 + FORWARD[i][1], f3 + FORWARD[i][2], UP[i][0], UP[i][1], UP[i][2]);
            this.frustum.set((Matrix4fc)this.faceViewProjection);
            long l = this.mark(sceneData);
            long l2 = this.modelsHeld(sceneData);
            boolean bl3 = !this.drawn[n5] || this.worldSums[n5] != l;
            boolean bl4 = bl2 = this.modelSums[n5] != l2;
            if (!LampShadows.due(bl3, bl2, this.facePosed, this.tick, n5)) continue;
            if (n2 == 0) {
                bl &= this.drawn[n5];
                continue;
            }
            this.face(sceneData, n5, bl3, f, f2, f3);
            this.posesDrawn += bl3 || bl2 ? 0L : 1L;
            this.drawn[n5] = true;
            this.worldSums[n5] = l;
            this.modelSums[n5] = l2;
            --n2;
        }
        this.unmark(sceneData);
        this.shown[n3] = bl ? Math.min(1.0f, this.shown[n3] + 0.25f) : this.shown[n3];
        return n2;
    }

    static boolean due(boolean bl, boolean bl2, boolean bl3, long l, int n) {
        return bl || bl2 || bl3 && (l + (long)n) % 4L == 0L;
    }

    private void face(SceneData sceneData, int n, boolean bl, float f, float f2, float f3) {
        int n2 = this.slots * 6 + n;
        if (bl) {
            this.target(n2);
            GL11.glClear((int)256);
            this.program.use();
            this.program.set("uAlphaRange", 0.3f, 2.0f);
            this.program.set("uFadeNoise", 0.0f);
            this.program.set("uViewProjection", this.faceViewProjection);
            this.program.set("uEye", f, f2, f3);
            this.program.set("uOwnFixture", 0.5f);
            Meshes.draw(sceneData, this.program, (byte)-128);
            ++this.worldsDrawn;
        }
        GL43.glCopyImageSubData((int)this.texture, (int)35866, (int)0, (int)0, (int)0, (int)n2, (int)this.texture, (int)35866, (int)0, (int)0, (int)0, (int)n, (int)this.size, (int)this.size, (int)1);
        if (this.faceModelCount > 0) {
            this.target(n);
            this.models.shadow(sceneData, this.faceViewProjection, this.faceModels, this.faceModelCount);
        }
        ++this.facesDrawn;
    }

    private void target(int n) {
        GL45.glNamedFramebufferTextureLayer((int)this.fbo, (int)36096, (int)this.texture, (int)0, (int)n);
        GL30.glBindFramebuffer((int)36160, (int)this.fbo);
        GL11.glViewport((int)0, (int)0, (int)this.size, (int)this.size);
    }

    private void gather(SceneData sceneData, float f, float f2, float f3, float f4) {
        float f5;
        float f6;
        this.nearCount = 0;
        for (int i = 0; i < sceneData.meshCount; ++i) {
            float f7 = sceneData.meshOffsets[i * 3] - 4.0f - f;
            float f8 = sceneData.meshOffsets[i * 3 + 1] + (float)sceneData.meshes[i].level * 2.4494896f + 1.5f - f2;
            f6 = sceneData.meshOffsets[i * 3 + 2] - 4.0f - f3;
            f5 = f4 + 8.0f;
            if (!(f7 * f7 + f8 * f8 + f6 * f6 < f5 * f5)) continue;
            this.near = this.nearCount == this.near.length ? Arrays.copyOf(this.near, this.nearCount * 2) : this.near;
            this.near[this.nearCount++] = i;
        }
        ModelDraws modelDraws = sceneData.models;
        this.nearModelCount = 0;
        for (int i = 0; i < modelDraws.count; ++i) {
            int n = i * 60 + 24;
            f6 = modelDraws.values[n] - f;
            f5 = modelDraws.values[n + 1] - f2;
            float f9 = modelDraws.values[n + 2] - f3;
            float f10 = f4 + modelDraws.values[n + 3];
            if ((modelDraws.flags[i] & 2) == 0 || !(f6 * f6 + f5 * f5 + f9 * f9 < f10 * f10)) continue;
            this.nearModels = this.nearModelCount == this.nearModels.length ? Arrays.copyOf(this.nearModels, this.nearModelCount * 2) : this.nearModels;
            this.nearModels[this.nearModelCount++] = i;
        }
    }

    private long mark(SceneData sceneData) {
        long l = 0L;
        for (int i = 0; i < this.nearCount; ++i) {
            int n = this.near[i];
            float f = sceneData.meshOffsets[n * 3] - 4.0f;
            float f2 = sceneData.meshOffsets[n * 3 + 2] - 4.0f;
            float f3 = sceneData.meshOffsets[n * 3 + 1] + (float)sceneData.meshes[n].level * 2.4494896f + 1.5f;
            boolean bl = sceneData.meshes[n].pages.length > 0 && sceneData.meshes[n].pages[0] == null;
            int n2 = n;
            sceneData.meshFlags[n2] = (byte)(sceneData.meshFlags[n2] & 0x7F);
            if (!bl && !this.frustum.testSphere(f, f3, f2, 8.0f)) continue;
            int n3 = n;
            sceneData.meshFlags[n3] = (byte)(sceneData.meshFlags[n3] | 0xFFFFFF80);
            long l2 = (long)System.identityHashCode(sceneData.meshes[n]) * -7046029254386353131L + (long)Float.floatToIntBits(sceneData.meshFade[n * 2]) * 31L + (long)Float.floatToIntBits(sceneData.meshFade[n * 2 + 1]);
            l += l2 ^ l2 >>> 29;
        }
        return l;
    }

    private void unmark(SceneData sceneData) {
        for (int i = 0; i < this.nearCount; ++i) {
            int n = this.near[i];
            sceneData.meshFlags[n] = (byte)(sceneData.meshFlags[n] & 0x7F);
        }
    }

    private long modelsHeld(SceneData sceneData) {
        ModelDraws modelDraws = sceneData.models;
        long l = 0L;
        this.faceModelCount = 0;
        this.facePosed = false;
        for (int i = 0; i < this.nearModelCount; ++i) {
            int n = this.nearModels[i];
            if (!this.takes(modelDraws, n)) continue;
            this.faceModels = this.faceModelCount == this.faceModels.length ? Arrays.copyOf(this.faceModels, this.faceModelCount * 2) : this.faceModels;
            this.faceModels[this.faceModelCount++] = n;
            int n2 = n * 60;
            this.facePosed |= modelDraws.palette[n] >= 0 && (modelDraws.flags[n] & 0x20) == 0;
            long l2 = Math.round(((double)modelDraws.values[n2 + 12] + sceneData.originX) * 256.0);
            l2 = l2 * 31L + Math.round(((double)modelDraws.values[n2 + 13] + sceneData.originY) * 256.0);
            l2 = l2 * 31L + Math.round(((double)modelDraws.values[n2 + 14] + sceneData.originZ) * 256.0);
            for (int j = 0; j < 11; ++j) {
                l2 = l2 * 31L + (long)Float.floatToIntBits(modelDraws.values[n2 + j]);
            }
            l2 = l2 * 31L + (long)Float.floatToIntBits(modelDraws.values[n2 + 56]);
            l2 = l2 * 31L + (long)Float.floatToIntBits(modelDraws.values[n2 + 56 + 1]);
            l += (l2 *= -7046029254386353131L) ^ l2 >>> 29;
        }
        return l;
    }

    private boolean takes(ModelDraws modelDraws, int n) {
        int n2 = n * 60 + 24;
        return (modelDraws.flags[n] & 2) != 0 && this.frustum.testSphere(modelDraws.values[n2], modelDraws.values[n2 + 1], modelDraws.values[n2 + 2], modelDraws.values[n2 + 3]);
    }

    private static float far(float f) {
        return f + 1.0f;
    }

    private String report() {
        double d = this.frames == 0L ? 0.0 : 1.0 / (double)this.frames;
        String string = String.format("%d of %d (%d texels), faces drawn/frame %.2f (world %.2f, poses %.2f)", this.count, this.slots, this.size, (double)this.facesDrawn * d, (double)this.worldsDrawn * d, (double)this.posesDrawn * d);
        this.facesDrawn = 0L;
        this.worldsDrawn = 0L;
        this.posesDrawn = 0L;
        this.frames = 0L;
        return string;
    }
}

