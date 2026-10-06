/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix3f
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL14
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL40
 */
package viewpoint.render;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import viewpoint.iris.IdTable;
import viewpoint.iris.IrisMacros;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.render.FrameContext;
import viewpoint.render.IrisLightning;
import viewpoint.render.IrisPipeline;
import viewpoint.render.IrisProgram;
import viewpoint.render.IrisPrograms;
import viewpoint.render.IrisSky;
import viewpoint.render.IrisWeather;
import viewpoint.render.Meshes;
import viewpoint.render.ModelPass;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;
import viewpoint.render.SurfacePass;

final class IrisGeometry {
    private static final String[][] MATERIALS = new String[][]{new String[0], {"water", "flowing_water"}, {"glass_pane", "glass", "white_stained_glass_pane"}, {"short_grass", "grass", "tall_grass", "fern"}, {"oak_leaves", "leaves", "birch_leaves"}, new String[0]};
    private static final float FLASH_LEVEL = 1.0f;
    private final Matrix4f matrix = new Matrix4f();
    private final Matrix3f normal = new Matrix3f();
    private final float[] blockIds = new float[16];
    private final IrisSky sky = new IrisSky();
    private final IrisWeather weather = new IrisWeather();
    private final IrisLightning lightning = new IrisLightning();

    IrisGeometry() {
    }

    void init() {
        this.sky.init();
        this.weather.init();
        this.lightning.init();
    }

    void blocks(IdTable idTable) {
        Arrays.fill(this.blockIds, -1.0f);
        for (int i = 0; i < MATERIALS.length; ++i) {
            this.blockIds[i] = MATERIALS[i].length == 0 ? -1.0f : (float)idTable.id(MATERIALS[i]);
        }
    }

    void sky(IrisPipeline irisPipeline, Map<String, float[]> map, FrameContext frameContext) {
        IrisProgram irisProgram;
        this.sky.update(irisPipeline.uniforms.sun.sun, irisPipeline.uniforms.sun.moon);
        int n = GL11.glGetInteger((int)32873);
        GL11.glDisable((int)2929);
        GL11.glDepthMask((boolean)false);
        GL11.glEnable((int)34383);
        this.sky.bind();
        IrisProgram irisProgram2 = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.SKY);
        if (irisProgram2 != null) {
            this.begin(irisPipeline, irisProgram2, map, irisPipeline.uniforms.modelView, irisPipeline.uniforms.projection, "SKY", frameContext, false);
            GL13.glActiveTexture((int)33984);
            GL11.glBindTexture((int)3553, (int)irisPipeline.textures.white.id());
            this.sky.drawDome();
            GL11.glEnable((int)3042);
            GL14.glBlendFuncSeparate((int)770, (int)1, (int)1, (int)1);
            irisProgram2.set("renderStage", IrisMacros.renderStage("STARS"));
            this.sky.drawStars();
            GL11.glDisable((int)3042);
            irisPipeline.bindings.unbind(irisProgram2);
        }
        if ((irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.SKY_TEXTURED)) != null) {
            this.begin(irisPipeline, irisProgram, map, irisPipeline.uniforms.modelView, irisPipeline.uniforms.projection, "SUN", frameContext, false);
            GL11.glEnable((int)3042);
            GL14.glBlendFuncSeparate((int)770, (int)1, (int)1, (int)0);
            GL13.glActiveTexture((int)33984);
            this.sky.drawSun();
            irisProgram.set("renderStage", IrisMacros.renderStage("MOON"));
            this.sky.drawMoon();
            GL11.glDisable((int)3042);
            irisPipeline.bindings.unbind(irisProgram);
        }
        this.sky.unbind();
        GL11.glDisable((int)34383);
        GL11.glBindTexture((int)3553, (int)n);
        GL11.glDepthMask((boolean)true);
        GL11.glEnable((int)2929);
    }

    void surfaces(IrisPipeline irisPipeline, Map<String, float[]> map, FrameContext frameContext, ShadowPass shadowPass, boolean bl) {
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)(bl ? IrisPrograms.Draw.WATER : IrisPrograms.Draw.TERRAIN));
        if (irisProgram == null) {
            return;
        }
        SceneData sceneData = frameContext.scene;
        this.begin(irisPipeline, irisProgram, map, irisPipeline.uniforms.modelView, irisPipeline.uniforms.projection, bl ? "TERRAIN_TRANSLUCENT" : "TERRAIN_SOLID", frameContext, false);
        IrisGeometry.blend(irisPipeline, irisProgram, bl);
        this.viewpoint(irisProgram, frameContext, shadowPass, frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ, false, bl);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)515);
        GL11.glDepthMask((boolean)true);
        shadowPass.bind(true);
        Gl.patches(true);
        Meshes.drawPlanned(sceneData, irisProgram.gl, bl ? (byte)4 : 1, true);
        Gl.patches(false);
        shadowPass.bind(false);
        GL11.glDisable((int)3042);
        irisPipeline.bindings.unbind(irisProgram);
    }

    void weather(IrisPipeline irisPipeline, Map<String, float[]> map, FrameContext frameContext, ShadowPass shadowPass) {
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.WEATHER);
        float[] fArray = map.get("frameTimeCounter");
        if (irisProgram == null || !this.weather.update(frameContext.scene, frameContext.eyeX, frameContext.eyeZ, fArray == null ? 0.0 : (double)fArray[0])) {
            return;
        }
        this.begin(irisPipeline, irisProgram, map, irisPipeline.uniforms.modelView, irisPipeline.uniforms.projection, "RAIN_SNOW", frameContext, false);
        IrisGeometry.blend(irisPipeline, irisProgram, true);
        irisProgram.gl.set("uIrisEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        shadowPass.rain.uniforms(irisProgram.gl, true);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)515);
        GL11.glDepthMask((boolean)false);
        GL11.glDisable((int)2884);
        this.weather.draw();
        GL11.glDepthMask((boolean)true);
        shadowPass.rain.uniforms(irisProgram.gl, false);
        GL11.glDisable((int)3042);
        irisPipeline.bindings.unbind(irisProgram);
    }

    void lightning(IrisPipeline irisPipeline, Map<String, float[]> map, FrameContext frameContext) {
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.LIGHTNING);
        if (irisProgram == null || !this.lightning.update(frameContext.scene, frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ)) {
            return;
        }
        this.begin(irisPipeline, irisProgram, map, irisPipeline.uniforms.modelView, irisPipeline.uniforms.projection, "NONE", frameContext, false);
        irisProgram.set("uIrisLevels", 15.0f, 15.0f);
        if (irisPipeline.configured.properties().blend.containsKey(irisProgram.name)) {
            IrisGeometry.blend(irisPipeline, irisProgram, true);
        } else {
            GL11.glEnable((int)3042);
            GL11.glBlendFunc((int)770, (int)1);
        }
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)515);
        GL11.glDepthMask((boolean)false);
        GL11.glDisable((int)2884);
        GL13.glActiveTexture((int)33984);
        int n = GL11.glGetInteger((int)32873);
        GL11.glBindTexture((int)3553, (int)irisPipeline.textures.white.id());
        this.lightning.draw();
        GL11.glBindTexture((int)3553, (int)n);
        GL11.glDepthMask((boolean)true);
        GL11.glDisable((int)3042);
        irisPipeline.bindings.unbind(irisProgram);
    }

    void models(IrisPipeline irisPipeline, Map<String, float[]> map, FrameContext frameContext, ShadowPass shadowPass, ModelPass modelPass) {
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.ENTITIES);
        IrisProgram irisProgram2 = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.VEHICLES);
        if (irisProgram == null || irisProgram2 == null) {
            return;
        }
        this.entities(irisPipeline, map, frameContext, shadowPass, irisProgram, false, () -> modelPass.iris(irisProgram.gl, frameContext.scene, frameContext.viewProjection, 0.0f, false));
        IrisProgram irisProgram3 = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.BLOCKS);
        if (irisProgram3 != null) {
            this.entities(irisPipeline, map, frameContext, shadowPass, irisProgram3, false, () -> modelPass.iris(irisProgram.gl, frameContext.scene, frameContext.viewProjection, 0.0f, true));
        }
        this.entities(irisPipeline, map, frameContext, shadowPass, irisProgram2, false, () -> modelPass.irisVehicles(irisProgram.gl, frameContext.scene, false));
        modelPass.irisDone();
    }

    void vehicleGlass(IrisPipeline irisPipeline, Map<String, float[]> map, FrameContext frameContext, ShadowPass shadowPass, ModelPass modelPass) {
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.VEHICLE_GLASS);
        if (irisProgram != null) {
            this.entities(irisPipeline, map, frameContext, shadowPass, irisProgram, true, () -> modelPass.irisVehicles(irisProgram.gl, frameContext.scene, true));
        }
    }

    private void entities(IrisPipeline irisPipeline, Map<String, float[]> map, FrameContext frameContext, ShadowPass shadowPass, IrisProgram irisProgram, boolean bl, Runnable runnable) {
        this.begin(irisPipeline, irisProgram, map, irisPipeline.uniforms.modelView, irisPipeline.uniforms.projection, "ENTITIES", frameContext, false);
        IrisGeometry.blend(irisPipeline, irisProgram, bl);
        this.viewpoint(irisProgram, frameContext, shadowPass, frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ, false, bl);
        irisProgram.gl.setInt("uIrisEntity", -1);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)515);
        GL11.glDepthMask((!bl ? 1 : 0) != 0);
        shadowPass.bind(true);
        runnable.run();
        shadowPass.bind(false);
        GL11.glDepthMask((boolean)true);
        GL11.glDisable((int)3042);
        irisPipeline.bindings.unbind(irisProgram);
    }

    void shadow(IrisPipeline irisPipeline, Map<String, float[]> map, FrameContext frameContext, ShadowPass shadowPass, ModelPass modelPass) {
        IrisProgram irisProgram = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.SHADOW);
        double[] dArray = irisPipeline.uniforms.sun.sunLights ? irisPipeline.uniforms.sun.sun : irisPipeline.uniforms.sun.moon;
        float f = frameContext.eyeX - (float)dArray[0] * 1000.0f;
        float f2 = frameContext.eyeY + (float)dArray[1] * 1000.0f;
        float f3 = frameContext.eyeZ - (float)dArray[2] * 1000.0f;
        this.begin(irisPipeline, irisProgram, map, irisPipeline.uniforms.shadowView, irisPipeline.uniforms.shadowProjection, "NONE", frameContext, true);
        this.viewpoint(irisProgram, frameContext, shadowPass, f, f2, f3, true, false);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)515);
        GL11.glDepthMask((boolean)true);
        GL11.glDisable((int)2884);
        Gl.patches(true);
        Meshes.draw(frameContext.scene, irisProgram.gl, (byte)27);
        Gl.patches(false);
        irisPipeline.bindings.unbind(irisProgram);
        IrisProgram irisProgram2 = irisPipeline.programs.draws.get((Object)IrisPrograms.Draw.SHADOW_MODELS);
        if (irisProgram2 != null) {
            this.begin(irisPipeline, irisProgram2, map, irisPipeline.uniforms.shadowView, irisPipeline.uniforms.shadowProjection, "ENTITIES", frameContext, true);
            this.viewpoint(irisProgram2, frameContext, shadowPass, f, f2, f3, true, false);
            irisProgram2.gl.setInt("uIrisEntity", -1);
            modelPass.iris(irisProgram2.gl, frameContext.scene, frameContext.viewProjection, irisPipeline.shadowDistance, false);
            irisPipeline.bindings.unbind(irisProgram2);
        }
    }

    void begin(IrisPipeline irisPipeline, IrisProgram irisProgram, Map<String, float[]> map, Matrix4f matrix4f, Matrix4f matrix4f2, String string, FrameContext frameContext, boolean bl) {
        irisProgram.use();
        irisProgram.set(map);
        this.matrices(irisProgram, matrix4f, matrix4f2);
        irisProgram.set("renderStage", IrisMacros.renderStage(string));
        irisProgram.set("uIrisLevels", 0.0f, 15.0f);
        irisPipeline.bindings.bind(irisProgram, bl ? "shadow" : "gbuffers", Set.of());
    }

    private void matrices(IrisProgram irisProgram, Matrix4f matrix4f, Matrix4f matrix4f2) {
        float[] fArray = new float[16];
        float[] fArray2 = new float[16];
        float[] fArray3 = new float[16];
        float[] fArray4 = new float[16];
        matrix4f.get(fArray);
        matrix4f2.get(fArray2);
        this.matrix.set((Matrix4fc)matrix4f2).mul((Matrix4fc)matrix4f).get(fArray3);
        float[] fArray5 = new float[48];
        new Matrix4f().get(fArray5, 0);
        new Matrix4f().scale(0.00390625f).translate(8.0f, 8.0f, 0.0f).get(fArray5, 16);
        new Matrix4f().get(fArray5, 32);
        matrix4f.normal(this.normal);
        float[] fArray6 = new float[9];
        this.normal.get(fArray6);
        for (String string : new String[]{"vp_", ""}) {
            irisProgram.set(string + (string.isEmpty() ? "modelViewMatrix" : "ModelViewMatrix"), fArray);
            irisProgram.set(string + (string.isEmpty() ? "projectionMatrix" : "ProjectionMatrix"), fArray2);
            irisProgram.set(string.isEmpty() ? "normalMatrix" : "vp_NormalMatrix", fArray6);
            irisProgram.set(string.isEmpty() ? "textureMatrix" : "vp_TextureMatrix", fArray5);
        }
        irisProgram.set("vp_ModelViewProjectionMatrix", fArray3);
        new Matrix4f((Matrix4fc)matrix4f).invert().get(fArray4);
        irisProgram.set("vp_ModelViewMatrixInverse", fArray4);
        irisProgram.set("modelViewMatrixInverse", fArray4);
        new Matrix4f((Matrix4fc)matrix4f2).invert().get(fArray4);
        irisProgram.set("vp_ProjectionMatrixInverse", fArray4);
        irisProgram.set("projectionMatrixInverse", fArray4);
    }

    private void viewpoint(IrisProgram irisProgram, FrameContext frameContext, ShadowPass shadowPass, float f, float f2, float f3, boolean bl, boolean bl2) {
        SceneData sceneData = frameContext.scene;
        GlProgram glProgram = irisProgram.gl;
        glProgram.set("uEye", f, f2, f3);
        glProgram.set("uIrisEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        glProgram.setInt("uIrisShadow", bl ? 1 : 0);
        glProgram.setInt("uIrisTranslucent", bl2 ? 1 : 0);
        irisProgram.set("uBlockIds", this.blockIds);
        glProgram.set("uIrisLampColours", 1.0f);
        glProgram.set("uIrisFlashLevel", 1.0f);
        glProgram.set("uAlphaRange", bl2 ? 0.04f : 0.85f, bl2 ? 0.85f : 2.0f);
        glProgram.set("uFadeNoise", 0.0f);
        glProgram.set("uSkyLevel", sceneData.skyR, sceneData.skyG, sceneData.skyB);
        SurfacePass.glass(glProgram, sceneData);
        if (!bl) {
            SurfacePass.lamps(glProgram, sceneData);
            shadowPass.lamps.uniforms(glProgram, sceneData);
            SurfacePass.torches(glProgram, sceneData);
            shadowPass.flash.uniforms(glProgram, frameContext);
        }
    }

    static void blend(IrisPipeline irisPipeline, IrisProgram irisProgram, boolean bl) {
        Object object;
        Map<String, String> map = irisPipeline.configured.properties().blend;
        String string = map.get(irisProgram.name);
        if (string == null ? !bl : string.strip().equalsIgnoreCase("off")) {
            GL11.glDisable((int)3042);
        } else {
            int[] nArray;
            GL11.glEnable((int)3042);
            if (string == null) {
                int[] nArray2 = new int[4];
                nArray2[0] = 770;
                nArray2[1] = 771;
                nArray2[2] = 1;
                nArray = nArray2;
                nArray2[3] = 771;
            } else {
                nArray = IrisGeometry.factors(string);
            }
            object = nArray;
            GL14.glBlendFuncSeparate((int)object[0], (int)object[1], (int)object[2], (int)object[3]);
        }
        object = irisProgram.drawBuffers;
        for (int i = 0; i < object.size(); ++i) {
            String string2 = map.get(irisProgram.name + ".colortex" + String.valueOf(object.get(i)));
            if (string2 == null) continue;
            if (string2.strip().equalsIgnoreCase("off")) {
                GL30.glDisablei((int)3042, (int)i);
                continue;
            }
            GL30.glEnablei((int)3042, (int)i);
            int[] nArray = IrisGeometry.factors(string2);
            GL40.glBlendFuncSeparatei((int)i, (int)nArray[0], (int)nArray[1], (int)nArray[2], (int)nArray[3]);
        }
        if (irisProgram.tintOutput >= 0) {
            GL30.glDisablei((int)3042, (int)irisProgram.tintOutput);
        }
    }

    private static int[] factors(String string) {
        String[] stringArray = string.strip().split("\\s+");
        int[] nArray = new int[4];
        for (int i = 0; i < 4; ++i) {
            nArray[i] = IrisGeometry.factor(stringArray[Math.min(i, stringArray.length - 1)]);
        }
        return nArray;
    }

    private static int factor(String string) {
        return switch (string.toUpperCase(Locale.ROOT)) {
            case "ZERO" -> 0;
            case "ONE" -> 1;
            case "SRC_COLOR" -> 768;
            case "ONE_MINUS_SRC_COLOR" -> 769;
            case "DST_COLOR" -> 774;
            case "ONE_MINUS_DST_COLOR" -> 775;
            case "SRC_ALPHA" -> 770;
            case "ONE_MINUS_SRC_ALPHA" -> 771;
            case "DST_ALPHA" -> 772;
            case "ONE_MINUS_DST_ALPHA" -> 773;
            case "SRC_ALPHA_SATURATE" -> 776;
            default -> 1;
        };
    }

    void release() {
        this.sky.release();
        this.weather.release();
        this.lightning.release();
    }

    static int lightUnit() {
        return 5;
    }
}

