/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL42
 *  org.lwjgl.opengl.GL43
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import viewpoint.iris.CustomUniforms;
import viewpoint.iris.ProgramSet;
import viewpoint.iris.ShadersProperties;
import viewpoint.platform.GlDebug;
import viewpoint.render.FarPass;
import viewpoint.render.FrameContext;
import viewpoint.render.IrisBindings;
import viewpoint.render.IrisFar;
import viewpoint.render.IrisGeometry;
import viewpoint.render.IrisPipeline;
import viewpoint.render.IrisProgram;
import viewpoint.render.IrisPrograms;
import viewpoint.render.IrisTargets;
import viewpoint.render.ModelPass;
import viewpoint.render.MousePick;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;

final class IrisFrame {
    static Watcher watcher;
    private final IrisGeometry geometry = new IrisGeometry();
    private final Map<String, float[]> values = new HashMap<String, float[]>();
    private final float[] ortho = new float[16];
    private final float[] identity = new float[16];
    private final float[] textureIdentities = new float[48];
    private final int[] area = new int[4];
    private int quadVao;
    private int quadVbo;
    private IrisPipeline linkedFor;
    private ModelPass models;
    private FarPass far;
    private final IrisFar distant = new IrisFar();
    private final Matrix4f pickInverse = new Matrix4f();

    IrisFrame() {
    }

    void init() {
        this.geometry.init();
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer((int)8).put(new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f});
        floatBuffer.flip();
        this.quadVao = GL30.glGenVertexArrays();
        this.quadVbo = GL15.glGenBuffers();
        GL30.glBindVertexArray((int)this.quadVao);
        GL15.glBindBuffer((int)34962, (int)this.quadVbo);
        GL15.glBufferData((int)34962, (FloatBuffer)floatBuffer, (int)35044);
        GL20.glVertexAttribPointer((int)0, (int)2, (int)5126, (boolean)false, (int)8, (long)0L);
        GL20.glEnableVertexAttribArray((int)0);
        GL30.glBindVertexArray((int)0);
        GL15.glBindBuffer((int)34962, (int)0);
        new Matrix4f().setOrtho(0.0f, 1.0f, 0.0f, 1.0f, -1.0f, 1.0f).get(this.ortho);
        new Matrix4f().get(this.identity);
        for (int i = 0; i < 3; ++i) {
            System.arraycopy(this.identity, 0, this.textureIdentities, i * 16, 16);
        }
    }

    void draw(IrisPipeline irisPipeline, FrameContext frameContext, ShadowPass shadowPass, ModelPass modelPass, FarPass farPass) {
        this.models = modelPass;
        this.far = farPass;
        SceneData sceneData = frameContext.scene;
        if (!irisPipeline.targets.ensure(frameContext.width, frameContext.height)) {
            throw new IllegalStateException(irisPipeline.pack.id + ": its screen targets could not be made");
        }
        if (irisPipeline.shadowOn) {
            irisPipeline.shadows.ensure(irisPipeline.shadowResolution);
        }
        if (this.linkedFor != irisPipeline) {
            this.linkedFor = irisPipeline;
            irisPipeline.images.load(irisPipeline.configured.properties(), frameContext.width, frameContext.height);
            irisPipeline.report.addAll(irisPipeline.images.notes);
            irisPipeline.checkPool();
            this.geometry.blocks(irisPipeline.configured.blocks());
        }
        irisPipeline.uniforms.update(frameContext, irisPipeline.pack.clock, irisPipeline.shadowDistance, irisPipeline.eyeHalflife, irisPipeline.wetHalflife, irisPipeline.dryHalflife, irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.DH_TERRAIN) != null, sceneData.fogEnd);
        this.values(irisPipeline, frameContext);
        irisPipeline.images.frame();
        shadowPass.drawLights(frameContext);
        if (!irisPipeline.setupDone) {
            this.stage(irisPipeline, ProgramSet.Stage.SETUP, frameContext);
            irisPipeline.setupDone = true;
        }
        this.stage(irisPipeline, ProgramSet.Stage.BEGIN, frameContext);
        if (irisPipeline.shadowOn) {
            this.shadowPass(irisPipeline, frameContext, shadowPass);
        }
        this.stage(irisPipeline, ProgramSet.Stage.SHADOWCOMP, frameContext);
        this.stage(irisPipeline, ProgramSet.Stage.PREPARE, frameContext);
        this.opaque(irisPipeline, frameContext, shadowPass);
        this.stage(irisPipeline, ProgramSet.Stage.DEFERRED, frameContext);
        this.translucent(irisPipeline, frameContext, shadowPass);
        this.weather(irisPipeline, frameContext, shadowPass);
        this.stage(irisPipeline, ProgramSet.Stage.COMPOSITE, frameContext);
        this.stage(irisPipeline, ProgramSet.Stage.FINAL, frameContext);
        GL30.glBindFramebuffer((int)36009, (int)frameContext.outputDrawFbo);
        GL30.glBindFramebuffer((int)36008, (int)frameContext.outputReadFbo);
        GL11.glViewport((int)frameContext.viewport[0], (int)frameContext.viewport[1], (int)frameContext.viewport[2], (int)frameContext.viewport[3]);
    }

    private void values(IrisPipeline irisPipeline, FrameContext frameContext) {
        this.values.clear();
        this.values.putAll(irisPipeline.uniforms.values);
        CustomUniforms customUniforms = irisPipeline.custom;
        customUniforms.evaluate(string -> {
            float[] fArray = irisPipeline.uniforms.values.get(string);
            if (fArray == null) {
                return null;
            }
            double[] dArray = new double[fArray.length];
            for (int i = 0; i < fArray.length; ++i) {
                dArray[i] = fArray[i];
            }
            return dArray;
        }, frameContext.frameSeconds);
        for (CustomUniforms.Entry entry : customUniforms.uniforms()) {
            double[] dArray = customUniforms.value(entry.declaration().name());
            if (dArray == null) continue;
            float[] fArray = new float[dArray.length];
            for (int i = 0; i < dArray.length; ++i) {
                fArray[i] = (float)dArray[i];
            }
            this.values.put(entry.declaration().name(), fArray);
        }
    }

    private void shadowPass(IrisPipeline irisPipeline, FrameContext frameContext, ShadowPass shadowPass) {
        GlDebug.push("minecraft shadow");
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.SHADOW);
        irisPipeline.shadows.clear();
        GL30.glBindFramebuffer((int)36160, (int)irisPipeline.shadows.framebuffer(irisProgram.drawBuffers));
        GL11.glViewport((int)0, (int)0, (int)irisPipeline.shadows.size, (int)irisPipeline.shadows.size);
        this.geometry.shadow(irisPipeline, this.values, frameContext, shadowPass, this.models);
        this.distant.shadow(irisPipeline, this.values, frameContext, this.far, this.geometry);
        irisPipeline.shadows.copyOpaque();
        irisPipeline.shadows.mipmaps();
        GlDebug.pop();
    }

    private void opaque(IrisPipeline irisPipeline, FrameContext frameContext, ShadowPass shadowPass) {
        GlDebug.push("minecraft opaque");
        SceneData sceneData = frameContext.scene;
        irisPipeline.targets.clear(sceneData.fogR, sceneData.fogG, sceneData.fogB);
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.TERRAIN);
        List<Integer> list = irisProgram != null ? irisProgram.drawBuffers : List.of(Integer.valueOf(0));
        int n = irisProgram != null ? irisProgram.tintOutput : -1;
        GL11.glViewport((int)0, (int)0, (int)frameContext.width, (int)frameContext.height);
        GL11.glClearDepth((double)1.0);
        for (int i = 1; i <= 2; ++i) {
            GL30.glBindFramebuffer((int)36160, (int)irisPipeline.targets.framebuffer(List.of(), false, i, -1));
            GL11.glClear((int)256);
        }
        GL30.glBindFramebuffer((int)36160, (int)irisPipeline.targets.framebuffer(List.of(), false, 0, 0));
        GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        GL11.glClear((int)16384);
        this.skyInto(irisPipeline, frameContext);
        IrisProgram irisProgram2 = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.DH_TERRAIN);
        if (irisProgram2 != null) {
            GL30.glBindFramebuffer((int)36160, (int)irisPipeline.targets.framebuffer(irisProgram2.drawBuffers, false, 2, -1));
            this.distant.terrain(irisPipeline, this.values, frameContext, this.far, this.geometry);
        }
        GL30.glBindFramebuffer((int)36160, (int)irisPipeline.targets.framebuffer(list, false, 1, n));
        GL11.glViewport((int)0, (int)0, (int)frameContext.width, (int)frameContext.height);
        this.geometry.surfaces(irisPipeline, this.values, frameContext, shadowPass, false);
        IrisProgram irisProgram3 = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.ENTITIES);
        if (irisProgram3 != null) {
            GL30.glBindFramebuffer((int)36160, (int)irisPipeline.targets.framebuffer(irisProgram3.drawBuffers, false, 1, irisProgram3.tintOutput));
            this.geometry.models(irisPipeline, this.values, frameContext, shadowPass, this.models);
        }
        MousePick.read(frameContext, irisPipeline.targets.framebuffer(List.of(), false, 1, -1), this.pickInverse.set((Matrix4fc)irisPipeline.uniforms.projection).mul((Matrix4fc)frameContext.view).invert());
        irisPipeline.targets.copyDepth(irisPipeline.targets.depth, irisPipeline.targets.depthOpaque);
        irisPipeline.targets.copyDepth(irisPipeline.targets.farDepth, irisPipeline.targets.farDepthOpaque);
        irisPipeline.targets.copyDepth(irisPipeline.targets.depth, irisPipeline.targets.depthNoHand);
        IrisFrame.watch(irisPipeline, "opaque", list);
        GlDebug.pop();
    }

    private void skyInto(IrisPipeline irisPipeline, FrameContext frameContext) {
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.SKY);
        List<Integer> list = irisProgram != null ? irisProgram.drawBuffers : List.of(Integer.valueOf(0));
        GL30.glBindFramebuffer((int)36160, (int)irisPipeline.targets.framebuffer(list, false, 1, -1));
        GL11.glViewport((int)0, (int)0, (int)frameContext.width, (int)frameContext.height);
        this.geometry.sky(irisPipeline, this.values, frameContext);
        IrisFrame.watch(irisPipeline, "sky", list);
    }

    private void translucent(IrisPipeline irisPipeline, FrameContext frameContext, ShadowPass shadowPass) {
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.WATER);
        IrisProgram irisProgram2 = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.VEHICLE_GLASS);
        GlDebug.push("minecraft translucent");
        GL11.glViewport((int)0, (int)0, (int)frameContext.width, (int)frameContext.height);
        if (irisProgram != null) {
            GL30.glBindFramebuffer((int)36160, (int)irisPipeline.targets.framebuffer(irisProgram.drawBuffers, false, 1, -1));
            this.geometry.surfaces(irisPipeline, this.values, frameContext, shadowPass, true);
            IrisFrame.watch(irisPipeline, "translucent", irisProgram.drawBuffers);
        }
        if (irisProgram2 != null) {
            GL30.glBindFramebuffer((int)36160, (int)irisPipeline.targets.framebuffer(irisProgram2.drawBuffers, false, 1, -1));
            this.geometry.vehicleGlass(irisPipeline, this.values, frameContext, shadowPass, this.models);
        }
        GlDebug.pop();
    }

    private void weather(IrisPipeline irisPipeline, FrameContext frameContext, ShadowPass shadowPass) {
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.WEATHER);
        IrisProgram irisProgram2 = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.LIGHTNING);
        GlDebug.push("minecraft weather");
        GL11.glViewport((int)0, (int)0, (int)frameContext.width, (int)frameContext.height);
        if (irisProgram != null) {
            GL30.glBindFramebuffer((int)36160, (int)irisPipeline.targets.framebuffer(irisProgram.drawBuffers, false, 1, -1));
            this.geometry.weather(irisPipeline, this.values, frameContext, shadowPass);
        }
        if (irisProgram2 != null) {
            GL30.glBindFramebuffer((int)36160, (int)irisPipeline.targets.framebuffer(irisProgram2.drawBuffers, false, 1, -1));
            this.geometry.lightning(irisPipeline, this.values, frameContext);
            IrisFrame.watch(irisPipeline, "lightning", irisProgram2.drawBuffers);
        }
        GlDebug.pop();
    }

    private void stage(IrisPipeline irisPipeline, ProgramSet.Stage stage, FrameContext frameContext) {
        List<IrisPrograms.Pass> list = irisPipeline.programs.passes.get((Object)stage);
        if (list == null || list.isEmpty()) {
            return;
        }
        GlDebug.push("minecraft " + stage.prefix);
        GL11.glDisable((int)2929);
        GL11.glDepthMask((boolean)false);
        String string = switch (stage) {
            case ProgramSet.Stage.SETUP, ProgramSet.Stage.BEGIN -> "begin";
            case ProgramSet.Stage.SHADOWCOMP -> "shadowcomp";
            case ProgramSet.Stage.PREPARE -> "prepare";
            case ProgramSet.Stage.DEFERRED -> "deferred";
            default -> "composite";
        };
        HashSet<Integer> hashSet = new HashSet<Integer>();
        for (IrisPrograms.Pass pass : list) {
            this.computes(irisPipeline, pass, string, frameContext, hashSet);
            if (pass.program() == null) continue;
            this.screen(irisPipeline, pass, string, frameContext, stage == ProgramSet.Stage.FINAL, hashSet);
        }
        GL11.glDepthMask((boolean)true);
        GL11.glEnable((int)2929);
        GlDebug.pop();
    }

    private void computes(IrisPipeline irisPipeline, IrisPrograms.Pass pass, String string, FrameContext frameContext, Set<Integer> set) {
        for (int i = 0; i < pass.computes().size(); ++i) {
            IrisProgram irisProgram = pass.computes().get(i);
            irisProgram.use();
            irisProgram.set(this.values);
            irisPipeline.bindings.bind(irisProgram, string, set);
            int[] nArray = pass.groups().get(i);
            float[] fArray = pass.scales().get(i);
            int n = nArray != null ? nArray[0] : (int)Math.ceil((float)frameContext.width * fArray[0]);
            int n2 = nArray != null ? nArray[1] : (int)Math.ceil((float)frameContext.height * fArray[1]);
            int n3 = nArray != null ? nArray[2] : 1;
            GL43.glDispatchCompute((int)Math.max(n, 1), (int)Math.max(n2, 1), (int)Math.max(n3, 1));
            GL42.glMemoryBarrier((int)-1);
            irisPipeline.bindings.unbind(irisProgram);
        }
    }

    private void screen(IrisPipeline irisPipeline, IrisPrograms.Pass pass, String string, FrameContext frameContext, boolean bl, Set<Integer> set) {
        IrisProgram irisProgram = pass.program();
        List<Integer> list = irisProgram.drawBuffers;
        if (bl) {
            GL30.glBindFramebuffer((int)36009, (int)frameContext.outputDrawFbo);
            System.arraycopy(frameContext.viewport, 0, this.area, 0, 4);
        } else {
            GL30.glBindFramebuffer((int)36160, (int)irisPipeline.targets.framebuffer(list, true, 0, -1));
            IrisFrame.viewport(irisPipeline, pass.name(), irisPipeline.targets.buffers[list.get(0)], this.area);
        }
        GL11.glViewport((int)this.area[0], (int)this.area[1], (int)this.area[2], (int)this.area[3]);
        irisProgram.use();
        irisProgram.set(this.values);
        irisProgram.set("vp_ModelViewMatrix", this.identity);
        irisProgram.set("vp_ProjectionMatrix", this.ortho);
        irisProgram.set("vp_ModelViewProjectionMatrix", this.ortho);
        irisProgram.set("vp_TextureMatrix", this.textureIdentities);
        irisProgram.set("vp_viewArea", this.area[0], this.area[1], this.area[2], this.area[3]);
        irisProgram.set("uIrisLampColours", 1.0f);
        irisPipeline.bindings.bind(irisProgram, string, set);
        IrisGeometry.blend(irisPipeline, irisProgram, false);
        GL30.glBindVertexArray((int)this.quadVao);
        GL11.glDrawArrays((int)5, (int)0, (int)4);
        GL30.glBindVertexArray((int)0);
        GL11.glDisable((int)3042);
        irisPipeline.bindings.unbind(irisProgram);
        if (!bl) {
            set.addAll(IrisFrame.flips(irisPipeline, pass.name(), list));
            IrisFrame.watch(irisPipeline, pass.name(), list);
        }
    }

    private static void viewport(IrisPipeline irisPipeline, String string, IrisTargets.Buffer buffer, int[] nArray) {
        float f;
        List<String> list = ShadersProperties.words(irisPipeline.configured.properties().flag("scale." + string, "1.0"));
        float f2 = 0.0f;
        float f3 = 0.0f;
        try {
            f = Float.parseFloat(list.get(0));
            if (list.size() >= 3) {
                f2 = Float.parseFloat(list.get(1));
                f3 = Float.parseFloat(list.get(2));
            }
        }
        catch (NumberFormatException numberFormatException) {
            throw new IllegalStateException(irisPipeline.pack.id + ": scale." + string + " is not a scale: " + String.valueOf(list), numberFormatException);
        }
        nArray[0] = Math.round((float)buffer.width * f2);
        nArray[1] = Math.round((float)buffer.height * f3);
        nArray[2] = Math.round((float)buffer.width * f);
        nArray[3] = Math.round((float)buffer.height * f);
    }

    private static Set<Integer> flips(IrisPipeline irisPipeline, String string, List<Integer> list) {
        LinkedHashSet<Integer> linkedHashSet = new LinkedHashSet<Integer>(list);
        for (Map.Entry<String, Boolean> entry : irisPipeline.configured.properties().flip.entrySet()) {
            int n;
            String string2 = entry.getKey();
            if (!string2.startsWith(string + ".") || (n = IrisBindings.colortex(string2.substring(string.length() + 1))) < 0) continue;
            if (entry.getValue().booleanValue()) {
                linkedHashSet.add(n);
                continue;
            }
            linkedHashSet.remove(n);
        }
        Iterator<Map.Entry<String, Boolean>> iterator = linkedHashSet.iterator();
        while (iterator.hasNext()) {
            int n = (Integer)((Object)iterator.next());
            if (list.contains(n)) {
                irisPipeline.targets.flip(n);
                continue;
            }
            irisPipeline.targets.buffers[n].read = 1 - irisPipeline.targets.buffers[n].read;
        }
        return linkedHashSet;
    }

    private static void watch(IrisPipeline irisPipeline, String string, List<Integer> list) {
        if (watcher != null) {
            watcher.written(irisPipeline, string, list);
        }
    }

    void release() {
        this.geometry.release();
        GL30.glDeleteVertexArrays((int)this.quadVao);
        GL15.glDeleteBuffers((int)this.quadVbo);
    }

    static interface Watcher {
        public void written(IrisPipeline var1, String var2, List<Integer> var3);
    }
}

