/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL43
 */
package viewpoint.render;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.PackPass;
import viewpoint.platform.PackTarget;
import viewpoint.platform.ShaderPacks;
import viewpoint.platform.Tuning;
import viewpoint.render.FrameContext;
import viewpoint.render.SceneData;
import viewpoint.render.SkyPass;
import viewpoint.render.Targets;
import viewpoint.render.WeatherMap;

final class PackStages {
    private final Targets targets;
    private final WeatherMap weatherMap;
    private final Map<String, Made> made = new HashMap<String, Made>();
    private int madeGeneration = -1;
    private int madeWidth;
    private int madeHeight;
    private int spare;
    private int spareFbo;
    private final int[] late = new int[2];
    private final int[] lateFbo = new int[2];
    private final Matrix4f matrix = new Matrix4f();

    PackStages(Targets targets, WeatherMap weatherMap) {
        this.targets = targets;
        this.weatherMap = weatherMap;
    }

    boolean any(PackPass.Stage stage) {
        for (PackPass packPass : GlProgram.pack().passes) {
            if (packPass.stage != stage) continue;
            return true;
        }
        return false;
    }

    int run(PackPass.Stage stage, FrameContext frameContext, int n) {
        ShaderPacks.Active active = GlProgram.pack();
        this.prepare(active, frameContext);
        if (!this.any(stage)) {
            return n;
        }
        GL11.glDisable((int)2929);
        GL11.glDisable((int)3042);
        GL11.glDisable((int)2884);
        GL11.glDepthMask((boolean)false);
        this.weatherMap.bind(true);
        int n2 = n;
        int n3 = 0;
        for (PackPass packPass : active.passes) {
            if (packPass.stage != stage) continue;
            boolean bl = !packPass.target.equals("color");
            this.draw(packPass, frameContext, n2, bl ? null : stage, n3);
            if (bl) continue;
            if (stage == PackPass.Stage.FINAL) {
                n2 = this.late[n3];
                n3 ^= 1;
                continue;
            }
            GL43.glCopyImageSubData((int)this.spare, (int)3553, (int)0, (int)0, (int)0, (int)0, (int)this.targets.hdr, (int)3553, (int)0, (int)0, (int)0, (int)0, (int)frameContext.width, (int)frameContext.height, (int)1);
        }
        for (int i = 0; i < 12; ++i) {
            Gl.bind(52 + i, 0);
        }
        this.weatherMap.bind(false);
        GL13.glActiveTexture((int)33984);
        GL11.glDepthMask((boolean)true);
        return n2;
    }

    private void draw(PackPass packPass, FrameContext frameContext, int n, PackPass.Stage stage, int n2) {
        Object object;
        int n3 = frameContext.width;
        int n4 = frameContext.height;
        if (stage == null) {
            object = this.made.get(packPass.target);
            GL30.glBindFramebuffer((int)36160, (int)((Made)object).fbo[((Made)object).current]);
            n3 = ((Made)object).width;
            n4 = ((Made)object).height;
        } else {
            GL30.glBindFramebuffer((int)36160, (int)(stage == PackPass.Stage.FINAL ? this.lateFbo[n2] : this.spareFbo));
        }
        GL11.glViewport((int)0, (int)0, (int)n3, (int)n4);
        object = GlProgram.pass(packPass.id);
        ((GlProgram)object).use();
        for (int i = 0; i < packPass.inputs.size(); ++i) {
            String string = packPass.inputs.get(i);
            Gl.bind(52 + i, this.texture(string, n));
            ((GlProgram)object).setInt(PackPass.sampler(string), 52 + i);
        }
        this.uniforms((GlProgram)object, frameContext, n3, n4);
        GL13.glActiveTexture((int)33984);
        Gl.screenTriangle();
    }

    private int texture(String string, int n) {
        return switch (string) {
            case "color" -> n;
            case "depth" -> this.targets.depth;
            case "albedo" -> this.targets.albedo;
            case "normal" -> this.targets.normal;
            case "light" -> this.targets.engineLight;
            case "velocity" -> this.targets.velocity;
            case "farDepth" -> this.targets.farDepth;
            case "gi" -> this.targets.gi;
            case "volume" -> this.targets.volume;
            case "bloom" -> this.targets.bloom[0];
            default -> string.endsWith(".previous") ? this.made.get(string.substring(0, string.length() - ".previous".length())).previous() : this.made.get((Object)string).texture[this.made.get((Object)string).current];
        };
    }

    private void uniforms(GlProgram glProgram, FrameContext frameContext, int n, int n2) {
        SceneData sceneData = frameContext.scene;
        PackStages.vec(glProgram, "uResolution", n, n2);
        PackStages.vec(glProgram, "uTexel", 1.0f / (float)n, 1.0f / (float)n2);
        this.mat(glProgram, "uView", frameContext.view);
        this.mat(glProgram, "uProjection", frameContext.jitteredProjection);
        this.mat(glProgram, "uViewProjection", frameContext.viewProjection);
        this.mat(glProgram, "uInvViewProjection", frameContext.invViewProjection);
        this.mat(glProgram, "uPlainViewProjection", frameContext.plainViewProjection);
        this.mat(glProgram, "uInvPlainViewProjection", frameContext.invPlainViewProjection);
        this.mat(glProgram, "uPrevious", frameContext.reprojection);
        if (glProgram.has("uCamera")) {
            float f = 1.0f / sceneData.projection.m00();
            float f2 = 1.0f / sceneData.projection.m11();
            glProgram.set("uCamera", 0.05f, 400.0f, f, f2);
        }
        PackStages.vec(glProgram, "uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        PackStages.vec(glProgram, "uSunDir", sceneData.sunX, sceneData.sunY, sceneData.sunZ);
        PackStages.vec(glProgram, "uSunColor", sceneData.sunR, sceneData.sunG, sceneData.sunB);
        PackStages.vec(glProgram, "uSkySun", sceneData.skySunX, sceneData.skySunY, sceneData.skySunZ);
        PackStages.vec(glProgram, "uFogColor", sceneData.fogR, sceneData.fogG, sceneData.fogB);
        PackStages.vec(glProgram, "uFogRange", sceneData.fogStart, sceneData.fogEnd);
        PackStages.vec(glProgram, "uFarRange", frameContext.farNear, frameContext.farFar);
        PackStages.one(glProgram, "uSunStrength", sceneData.sunStrength);
        PackStages.one(glProgram, "uSkyGlow", sceneData.skyGlow);
        PackStages.one(glProgram, "uTime", sceneData.time);
        PackStages.one(glProgram, "uFrameSeconds", frameContext.frameSeconds);
        PackStages.one(glProgram, "uPreviousValid", frameContext.previousValid ? 1.0f : 0.0f);
        PackStages.one(glProgram, "uExposure", Tuning.exposure);
        SkyPass.cloudUniforms(glProgram, sceneData);
        SkyPass.weatherUniforms(glProgram, sceneData);
        if (glProgram.has("uFrame")) {
            glProgram.setInt("uFrame", (int)frameContext.index);
        }
        if (glProgram.has("uWeatherMap")) {
            glProgram.setInt("uWeatherMap", 14);
        }
        if (glProgram.has("uCloudNoise")) {
            glProgram.setInt("uCloudNoise", 18);
        }
    }

    private static void one(GlProgram glProgram, String string, float f) {
        if (glProgram.has(string)) {
            glProgram.set(string, f);
        }
    }

    private static void vec(GlProgram glProgram, String string, float f, float f2) {
        if (glProgram.has(string)) {
            glProgram.set(string, f, f2);
        }
    }

    private static void vec(GlProgram glProgram, String string, float f, float f2, float f3) {
        if (glProgram.has(string)) {
            glProgram.set(string, f, f2, f3);
        }
    }

    private void mat(GlProgram glProgram, String string, Matrix4f matrix4f) {
        if (glProgram.has(string)) {
            glProgram.set(string, this.matrix.set((Matrix4fc)matrix4f));
        }
    }

    private void prepare(ShaderPacks.Active active, FrameContext frameContext) {
        if (active.generation != this.madeGeneration || frameContext.width != this.madeWidth || frameContext.height != this.madeHeight) {
            this.release();
            this.madeGeneration = active.generation;
            this.madeWidth = frameContext.width;
            this.madeHeight = frameContext.height;
            this.make(active.passes, active.pack.targets, frameContext);
        }
        for (Made made : this.made.values()) {
            if (!made.history || made.frame == frameContext.index) continue;
            made.frame = frameContext.index;
            made.current ^= 1;
        }
    }

    private void make(List<PackPass> list, List<PackTarget> list2, FrameContext frameContext) {
        int n = GL11.glGetInteger((int)32873);
        HashSet<String> hashSet = new HashSet<String>();
        boolean bl = false;
        boolean bl2 = false;
        for (PackPass object : list) {
            hashSet.add(object.target);
            object.inputs.forEach(string -> hashSet.add(string.endsWith(".previous") ? string.substring(0, string.length() - ".previous".length()) : string));
            boolean bl3 = object.target.equals("color");
            bl |= bl3 && object.stage != PackPass.Stage.FINAL;
            bl2 |= bl3 && object.stage == PackPass.Stage.FINAL;
        }
        for (PackTarget packTarget : list2) {
            if (!hashSet.contains(packTarget.name)) continue;
            this.made.put(packTarget.name, new Made(packTarget, frameContext.width, frameContext.height));
        }
        if (bl) {
            this.spare = PackStages.hdrLike(frameContext);
            this.spareFbo = PackStages.framebuffer("pack spare", this.spare);
        }
        for (int i = 0; bl2 && i < 2; ++i) {
            this.late[i] = PackStages.hdrLike(frameContext);
            this.lateFbo[i] = PackStages.framebuffer("pack final " + i, this.late[i]);
        }
        GL11.glBindTexture((int)3553, (int)n);
    }

    private static int framebuffer(String string, int n) {
        int n2 = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer((int)36160, (int)n2);
        GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)3553, (int)n, (int)0);
        int n3 = GL30.glCheckFramebufferStatus((int)36160);
        if (n3 != 36053) {
            throw new IllegalStateException(string + " framebuffer incomplete: 0x" + Integer.toHexString(n3));
        }
        return n2;
    }

    private static int hdrLike(FrameContext frameContext) {
        return Gl.texture(34842, frameContext.width, frameContext.height, 6408, 5126, 9729);
    }

    void release() {
        this.made.values().forEach(Made::release);
        this.made.clear();
        this.madeGeneration = -1;
        if (this.spare != 0) {
            GL30.glDeleteFramebuffers((int)this.spareFbo);
            GL11.glDeleteTextures((int)this.spare);
            this.spare = 0;
        }
        for (int i = 0; i < 2; ++i) {
            if (this.late[i] == 0) continue;
            GL30.glDeleteFramebuffers((int)this.lateFbo[i]);
            GL11.glDeleteTextures((int)this.late[i]);
            this.late[i] = 0;
        }
    }

    private static final class Made {
        final int[] texture = new int[2];
        final int[] fbo = new int[2];
        final boolean history;
        final int width;
        final int height;
        int current;
        long frame = -1L;

        Made(PackTarget packTarget, int n, int n2) {
            this.history = packTarget.history;
            this.width = packTarget.width(n);
            this.height = packTarget.height(n2);
            for (int i = 0; i < (this.history ? 2 : 1); ++i) {
                this.texture[i] = Gl.texture(packTarget.format.internal, this.width, this.height, packTarget.format.format, packTarget.format.type, 9729);
                this.fbo[i] = PackStages.framebuffer("pack target " + packTarget.name, this.texture[i]);
                GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
                GL11.glClear((int)16384);
            }
        }

        int previous() {
            return this.history ? this.texture[this.current ^ 1] : this.texture[this.current];
        }

        void release() {
            for (int i = 0; i < 2; ++i) {
                if (this.texture[i] == 0) continue;
                GL30.glDeleteFramebuffers((int)this.fbo[i]);
                GL11.glDeleteTextures((int)this.texture[i]);
            }
        }
    }
}

