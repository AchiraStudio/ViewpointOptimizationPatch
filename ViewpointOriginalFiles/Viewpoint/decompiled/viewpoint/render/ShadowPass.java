/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import java.util.Arrays;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlDebug;
import viewpoint.platform.GlProgram;
import viewpoint.platform.GpuParts;
import viewpoint.platform.Pipeline;
import viewpoint.render.CascadeLayers;
import viewpoint.render.FarShadow;
import viewpoint.render.FarShadowMap;
import viewpoint.render.FlashShadow;
import viewpoint.render.FrameContext;
import viewpoint.render.LampShadows;
import viewpoint.render.Meshes;
import viewpoint.render.ModelPass;
import viewpoint.render.RainMap;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowDepthViews;
import viewpoint.render.SkyPass;
import viewpoint.render.WeatherMap;
import viewpoint.render.WorldRenderer;

final class ShadowPass {
    static final int CASCADES = 3;
    static final int SUN_MAPS = 5;
    static final float NEAR_RADIUS = 22.0f;
    static final float MID_RADIUS = 64.0f;
    static final float PLANT_LIGHT = 100000.0f;
    private static final float FAR_DRIFT = 8.0f;
    private final GlProgram program = GlProgram.create("shadow", "shadow.vert", "shadow.frag");
    private final int[] fbos = new int[3];
    private final int[] textures = new int[3];
    private final ShadowDepthViews depthViews = new ShadowDepthViews();
    private int size;
    private int drawnCount;
    private long frameCount;
    private float drawnReach;
    private float farSunX;
    private float farSunY = 2.0f;
    private float farSunZ;
    private final boolean[] valid = new boolean[3];
    private final double[] origin = new double[9];
    private float sunDistance = 200.0f;
    private final Matrix4f lightView = new Matrix4f();
    private final Matrix4f[] viewProjection = new Matrix4f[]{new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final Matrix4f[] drawn = new Matrix4f[]{new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final Matrix4f[] matrix = new Matrix4f[]{new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final float[] reach = new float[3];
    private long farCasters;
    private final FloatBuffer matrices = BufferUtils.createFloatBuffer((int)80);
    private final FloatBuffer mapParams = BufferUtils.createFloatBuffer((int)20);
    private final Matrix4f farMatrix = new Matrix4f();
    final FlashShadow flash;
    final RainMap rain;
    private final ModelPass models;
    final LampShadows lamps;
    private final FarShadow farMaps;
    private final WeatherMap weatherMap;
    private final CascadeLayers layers;
    private final CascadeLayers.Cascade target = new CascadeLayers.Cascade();

    ShadowPass(ModelPass modelPass, FarShadow farShadow, WeatherMap weatherMap) {
        this.models = modelPass;
        this.farMaps = farShadow;
        this.weatherMap = weatherMap;
        this.lamps = new LampShadows(modelPass);
        this.layers = new CascadeLayers(this.program, modelPass, farShadow);
        this.program.sampler("uTexture", 0);
        int n = GL11.glGetInteger((int)32873);
        int n2 = GL11.glGetInteger((int)36006);
        this.makeCascades(GlProgram.pack().pipeline.shadowResolution);
        this.flash = new FlashShadow(this.program, modelPass);
        this.rain = new RainMap(this.program, modelPass);
        GL30.glBindFramebuffer((int)36160, (int)n2);
        GL11.glBindTexture((int)3553, (int)n);
    }

    boolean complete() {
        int n = GL11.glGetInteger((int)36006);
        GL30.glBindFramebuffer((int)36160, (int)this.fbos[0]);
        int n2 = GL30.glCheckFramebufferStatus((int)36160);
        GL30.glBindFramebuffer((int)36160, (int)n);
        if (n2 != 36053) {
            System.out.println("[Viewpoint] shadow framebuffer incomplete: 0x" + Integer.toHexString(n2));
            return false;
        }
        return true;
    }

    static float cascadeRadius(int n, SceneData sceneData) {
        return n == 0 ? 22.0f : (n == 1 ? 64.0f : sceneData.shadowReach);
    }

    static float farShadowRadius(float f) {
        return Math.max(96.0f, (float)Math.ceil((f + 12.0f) / 16.0f) * 16.0f);
    }

    void draw(FrameContext frameContext) {
        Pipeline pipeline = frameContext.pipeline;
        if (frameContext.sun && pipeline.shadows) {
            GlDebug.push("sun shadows");
            this.resize(pipeline.shadowResolution);
            this.drawCascades(frameContext.scene, pipeline.shadowCascades);
            GlDebug.pop();
        } else if (!pipeline.shadows) {
            this.layers.release();
        }
        this.drawLights(frameContext);
    }

    void drawLights(FrameContext frameContext) {
        this.flash.draw(frameContext);
        this.lamps.draw(frameContext);
        this.rain.draw(frameContext.scene);
    }

    private void resize(int n) {
        if (n == this.size) {
            return;
        }
        int n2 = GL11.glGetInteger((int)32873);
        int n3 = GL11.glGetInteger((int)36006);
        GL30.glDeleteFramebuffers((int[])this.fbos);
        GL11.glDeleteTextures((int[])this.textures);
        this.makeCascades(n);
        GL30.glBindFramebuffer((int)36160, (int)n3);
        GL11.glBindTexture((int)3553, (int)n2);
    }

    private void makeCascades(int n) {
        this.size = n;
        Arrays.fill(this.valid, false);
        for (int i = 0; i < 3; ++i) {
            this.textures[i] = ShadowPass.depthTexture(this.size);
            this.fbos[i] = ShadowPass.depthFbo(this.textures[i]);
        }
    }

    private void drawCascades(SceneData sceneData, int n) {
        this.sunDistance = sceneData.shadowReach + 90.0f;
        boolean bl = Math.abs(sceneData.sunY) > 0.99f;
        this.lightView.setLookAt(sceneData.sunX * this.sunDistance, sceneData.sunY * this.sunDistance, sceneData.sunZ * this.sunDistance, 0.0f, 0.0f, 0.0f, 0.0f, bl ? 0.0f : 1.0f, bl ? 1.0f : 0.0f);
        if (sceneData.shadowReach != this.drawnReach || n != this.drawnCount) {
            Arrays.fill(this.valid, false);
            this.drawnCount = n;
            this.drawnReach = sceneData.shadowReach;
        }
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)513);
        GL11.glEnable((int)32823);
        GL11.glPolygonOffset((float)2.0f, (float)4.0f);
        this.program.use();
        this.program.set("uAlphaRange", 0.3f, 2.0f);
        this.program.set("uFadeNoise", 0.0f);
        this.program.set("uEye", sceneData.sunX * 100000.0f, sceneData.sunY * 100000.0f, sceneData.sunZ * 100000.0f);
        boolean bl2 = this.frameCount % 2L == 0L;
        boolean bl3 = this.frameCount % 4L == 1L && n > 2 && this.farStale(sceneData);
        ++this.frameCount;
        this.layers.frame(sceneData, n);
        byte[] byArray = new byte[]{2, 16, 8};
        for (int i = 0; i < n; ++i) {
            if (i == 0 || !this.valid[i] || i == 1 && bl2 || i == 2 && bl3) {
                GpuParts.begin(3 + i);
                this.cascade(sceneData, i, byArray[i]);
                GpuParts.end(3 + i);
                if (i == 2) {
                    this.farCasters = ShadowPass.farCascadeCasters(sceneData);
                    this.farSunX = sceneData.sunX;
                    this.farSunY = sceneData.sunY;
                    this.farSunZ = sceneData.sunZ;
                }
                this.valid[i] = true;
                this.origin[i * 3] = sceneData.originX;
                this.origin[i * 3 + 1] = sceneData.originY;
                this.origin[i * 3 + 2] = sceneData.originZ;
            }
            this.matrix[i].set((Matrix4fc)this.drawn[i]).translate((float)(sceneData.originX - this.origin[i * 3]), (float)(sceneData.originY - this.origin[i * 3 + 1]), (float)(sceneData.originZ - this.origin[i * 3 + 2]));
        }
        GL11.glDisable((int)32823);
    }

    private boolean farStale(SceneData sceneData) {
        double d = sceneData.originX - this.origin[6];
        double d2 = sceneData.originY - this.origin[7];
        double d3 = sceneData.originZ - this.origin[8];
        return sceneData.sunX != this.farSunX || sceneData.sunY != this.farSunY || sceneData.sunZ != this.farSunZ || ShadowPass.farCascadeCasters(sceneData) != this.farCasters || d * d + d2 * d2 + d3 * d3 > 64.0;
    }

    boolean quiet(FrameContext frameContext) {
        int n;
        Pipeline pipeline = frameContext.pipeline;
        if (!frameContext.sun || !pipeline.shadows) {
            return true;
        }
        int n2 = pipeline.shadowCascades;
        boolean bl = pipeline.shadowResolution != this.size || frameContext.scene.shadowReach != this.drawnReach || n2 != this.drawnCount;
        for (n = 1; n < n2; ++n) {
            bl |= !this.valid[n];
        }
        n = n2 > 1 && this.frameCount % 2L == 0L ? 1 : 0;
        boolean bl2 = n2 > 2 && this.frameCount % 4L == 1L;
        return !bl && n == 0 && !bl2;
    }

    private static long farCascadeCasters(SceneData sceneData) {
        return ShadowPass.casters(sceneData, (byte)8) * 31L + FarShadow.farSum(sceneData);
    }

    static long casters(SceneData sceneData, byte by) {
        long l = 0L;
        for (int i = 0; i < sceneData.meshCount; ++i) {
            if ((sceneData.meshFlags[i] & by) == 0) continue;
            long l2 = (long)System.identityHashCode(sceneData.meshes[i]) * -7046029254386353131L + (long)Float.floatToIntBits(sceneData.meshFade[i * 2]) * 31L + (long)Float.floatToIntBits(sceneData.meshFade[i * 2 + 1]);
            l += l2 ^ l2 >>> 29;
        }
        return l;
    }

    private void cascade(SceneData sceneData, int n, byte by) {
        float f;
        this.reach[n] = f = ShadowPass.cascadeRadius(n, sceneData);
        if (n < 2) {
            this.target.texture = this.textures[n];
            this.target.fbo = this.fbos[n];
            this.target.size = this.size;
            this.target.sunDistance = this.sunDistance;
            this.target.lightView = this.lightView;
            this.layers.draw(sceneData, n, by, f, this.target, this.viewProjection[n]);
            this.drawn[n].translation(0.5f, 0.5f, 0.5f).scale(0.5f).mul((Matrix4fc)this.viewProjection[n]);
            return;
        }
        double d = 2.0 * (double)f / (double)this.size;
        double d2 = (double)this.lightView.m00() * sceneData.originX + (double)this.lightView.m10() * sceneData.originY + (double)this.lightView.m20() * sceneData.originZ;
        double d3 = (double)this.lightView.m01() * sceneData.originX + (double)this.lightView.m11() * sceneData.originY + (double)this.lightView.m21() * sceneData.originZ;
        float f2 = (float)(d2 - Math.floor(d2 / d) * d);
        float f3 = (float)(d3 - Math.floor(d3 / d) * d);
        Matrix4f matrix4f = this.viewProjection[n];
        matrix4f.setOrtho(-f - f2, f - f2, -f - f3, f - f3, 1.0f, this.sunDistance * 2.0f).mul((Matrix4fc)this.lightView);
        this.drawn[n].translation(0.5f, 0.5f, 0.5f).scale(0.5f).mul((Matrix4fc)matrix4f);
        GL30.glBindFramebuffer((int)36160, (int)this.fbos[n]);
        GL11.glViewport((int)0, (int)0, (int)this.size, (int)this.size);
        GL11.glClear((int)256);
        this.program.set("uViewProjection", matrix4f);
        Meshes.draw(sceneData, this.program, by);
        this.farMaps.castInto(sceneData, matrix4f, n, 0.0f);
        this.program.use();
    }

    void bind(boolean bl) {
        this.weatherMap.bind(bl);
        for (int i = 0; i < 3; ++i) {
            Gl.bind(Gl.UNIT_CASCADE[i], bl ? this.textures[i] : 0);
        }
        this.depthViews.bind(this.textures, bl);
        this.farMaps.bind(bl);
        this.flash.bind(bl);
        this.lamps.bind(bl);
    }

    static void samplers(GlProgram glProgram) {
        for (int i = 0; i < 5; ++i) {
            glProgram.sampler("uShadowMap" + i, Gl.UNIT_CASCADE[i]);
        }
        WeatherMap.samplers(glProgram);
    }

    void sunUniforms(GlProgram glProgram, FrameContext frameContext) {
        int n;
        SceneData sceneData = frameContext.scene;
        this.matrices.clear();
        this.mapParams.clear();
        int n2 = frameContext.pipeline.shadowCascades;
        float f = this.farMaps.cascadeReach(sceneData);
        for (n = 0; n < 3; ++n) {
            this.matrix[n].get(n * 16, this.matrices);
            float f2 = n < n2 ? 2.0f * this.reach[n] / (float)this.size : 0.0f;
            this.mapParams.put(f2).put(this.sunDistance * 2.0f - 1.0f).put(1.0f / (float)this.size).put(f);
        }
        for (n = 0; n < 2; ++n) {
            FarShadowMap farShadowMap = this.farMaps.map(n);
            farShadowMap.textureMatrix(this.farMatrix).get((3 + n) * 16, this.matrices);
            this.mapParams.put(farShadowMap.texelSquares()).put(farShadowMap.depth).put(farShadowMap.texelStep()).put(0.0f);
        }
        this.mapParams.flip();
        glProgram.setMatrices("uShadowMatrix", this.matrices);
        glProgram.setVec4s("uShadowMaps", this.mapParams);
        glProgram.set("uSunDir", sceneData.sunX, sceneData.sunY, sceneData.sunZ);
        glProgram.set("uSunColor", sceneData.sunR, sceneData.sunG, sceneData.sunB);
        glProgram.set("uSunStrength", frameContext.sun ? sceneData.sunStrength : 0.0f);
        glProgram.set("uShadowsOff", WorldRenderer.off(5) ? 1.0f : 0.0f);
        glProgram.set("uShadowPhase", WorldRenderer.off(1) ? 0.0f : (float)frameContext.jitterPhase);
        SkyPass.cloudUniforms(glProgram, sceneData);
        SkyPass.weatherUniforms(glProgram, sceneData);
    }

    static int depthTexture(int n) {
        int n2 = Gl.texture(33190, n, n, 6402, 5126, 9729);
        GL11.glTexParameteri((int)3553, (int)34892, (int)34894);
        GL11.glTexParameteri((int)3553, (int)34893, (int)515);
        return n2;
    }

    static int depthFbo(int n) {
        int n2 = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer((int)36160, (int)n2);
        GL30.glFramebufferTexture2D((int)36160, (int)36096, (int)3553, (int)n, (int)0);
        GL11.glDrawBuffer((int)0);
        GL11.glReadBuffer((int)0);
        return n2;
    }
}

