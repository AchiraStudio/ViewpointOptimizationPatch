/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector4f
 */
package viewpoint.render;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import viewpoint.iris.Biomes;
import viewpoint.iris.IrisMacros;
import viewpoint.render.FrameContext;
import viewpoint.render.IrisSun;
import viewpoint.render.SceneData;
import viewpoint.render.WorldRenderer;

final class IrisUniforms {
    static final double GROUND = 64.0;
    static final float NEAR = 0.05f;
    private static final float DEPTH_FAR = 4.0f;
    private static final float FREE_FAR_NEAR = 2.0f;
    final Map<String, float[]> values = new HashMap<String, float[]>();
    final IrisSun sun = new IrisSun();
    final Matrix4f modelView = new Matrix4f();
    final Matrix4f projection = new Matrix4f();
    final Matrix4f shadowView = new Matrix4f();
    final Matrix4f shadowProjection = new Matrix4f();
    final Matrix4f farProjection = new Matrix4f();
    float far = 256.0f;
    float farFar = 2048.0f;
    float farNear = 128.0f;
    double worldX;
    double worldY;
    double worldZ;
    private final Matrix4f previousModelView = new Matrix4f();
    private final Matrix4f previousProjection = new Matrix4f();
    private final Matrix4f previousFarProjection = new Matrix4f();
    private double previousX;
    private double previousY;
    private double previousZ;
    private boolean hasPrevious;
    private float eyeSkySmooth = -1.0f;
    private float wetness;
    private boolean distant;
    private long frames;
    private double seconds;

    IrisUniforms() {
    }

    void update(FrameContext frameContext, boolean bl, float f, float f2, float f3, float f4, boolean bl2, float f5) {
        SceneData sceneData = frameContext.scene;
        this.distant = bl2;
        ++this.frames;
        this.seconds += (double)frameContext.frameSeconds;
        this.far = Math.max(64.0f, Math.min(1024.0f, sceneData.nearEnd() > 0.0f ? sceneData.nearEnd() : 128.0f));
        this.farFar = Math.max(this.far * 2.0f, f5);
        this.matrices(frameContext);
        this.sun.update(sceneData, bl);
        this.shadow(f);
        this.values.clear();
        this.camera(frameContext);
        this.celestial();
        this.weather(sceneData, frameContext.frameSeconds, f2, f3, f4);
        this.put("lightningBoltPosition", -(sceneData.lightningX - frameContext.eyeX), sceneData.lightningY - frameContext.eyeY, -(sceneData.lightningZ - frameContext.eyeZ), sceneData.lightning > 0.0f ? 1.0f : 0.0f);
        this.world(sceneData, frameContext);
        this.neutral();
        this.remember();
    }

    private void matrices(FrameContext frameContext) {
        Matrix4f matrix4f = frameContext.scene.projection;
        float f = 1.0f / matrix4f.m11();
        float f2 = matrix4f.m11() / matrix4f.m00();
        float f3 = 2.0f * (float)Math.atan(f);
        float f4 = WorldRenderer.freeCamera ? (float)Math.sqrt(frameContext.eyeX * frameContext.eyeX + frameContext.eyeY * frameContext.eyeY + frameContext.eyeZ * frameContext.eyeZ) : 0.0f;
        this.projection.setPerspective(f3, f2, 0.05f, this.far * 4.0f + f4);
        this.farNear = WorldRenderer.freeCamera ? 2.0f : this.far * 0.5f;
        this.farProjection.setPerspective(f3, f2, this.farNear, this.farFar);
        this.modelView.set((Matrix4fc)frameContext.view).translate(frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ).scale(-1.0f, 1.0f, -1.0f);
        SceneData sceneData = frameContext.scene;
        this.worldX = -(sceneData.originX + (double)frameContext.eyeX);
        this.worldY = 64.0 + sceneData.originY + (double)frameContext.eyeY;
        this.worldZ = -(sceneData.originZ + (double)frameContext.eyeZ);
    }

    private void shadow(float f) {
        double[] dArray = this.sun.sunLights ? this.sun.sun : this.sun.moon;
        float f2 = (float)dArray[0];
        float f3 = (float)Math.max(dArray[1], 0.05);
        float f4 = (float)dArray[2];
        float f5 = (float)Math.sqrt(f2 * f2 + f3 * f3 + f4 * f4);
        float f6 = Math.abs(f3 /= f5) > 0.99f ? 1.0f : 0.0f;
        float f7 = Math.abs(f3) > 0.99f ? 0.0f : 1.0f;
        this.shadowView.setLookAt((f2 /= f5) * 100.0f, f3 * 100.0f, (f4 /= f5) * 100.0f, 0.0f, 0.0f, 0.0f, f6, f7, 0.0f);
        float f8 = (float)(this.worldX - Math.floor(this.worldX / 2.0) * 2.0);
        float f9 = (float)(this.worldY - Math.floor(this.worldY / 2.0) * 2.0);
        float f10 = (float)(this.worldZ - Math.floor(this.worldZ / 2.0) * 2.0);
        this.shadowView.translate(f8 - 1.0f, f9 - 1.0f, f10 - 1.0f);
        this.shadowProjection.setOrtho(-f, f, -f, f, 0.05f, 256.0f);
    }

    private void camera(FrameContext frameContext) {
        this.matrix("gbufferModelView", this.modelView);
        this.matrix("gbufferModelViewInverse", new Matrix4f((Matrix4fc)this.modelView).invert());
        this.matrix("gbufferProjection", this.projection);
        this.matrix("gbufferProjectionInverse", new Matrix4f((Matrix4fc)this.projection).invert());
        this.matrix("gbufferPreviousModelView", this.hasPrevious ? this.previousModelView : this.modelView);
        this.matrix("gbufferPreviousProjection", this.hasPrevious ? this.previousProjection : this.projection);
        this.matrix("shadowModelView", this.shadowView);
        this.matrix("shadowModelViewInverse", new Matrix4f((Matrix4fc)this.shadowView).invert());
        this.matrix("shadowProjection", this.shadowProjection);
        this.matrix("shadowProjectionInverse", new Matrix4f((Matrix4fc)this.shadowProjection).invert());
        this.matrix("dhProjection", this.farProjection);
        this.matrix("dhProjectionInverse", new Matrix4f((Matrix4fc)this.farProjection).invert());
        this.matrix("dhPreviousProjection", this.hasPrevious ? this.previousFarProjection : this.farProjection);
        this.position("cameraPosition", this.worldX, this.worldY, this.worldZ);
        this.position("previousCameraPosition", this.hasPrevious ? this.previousX : this.worldX, this.hasPrevious ? this.previousY : this.worldY, this.hasPrevious ? this.previousZ : this.worldZ);
        this.put("eyeAltitude", (float)this.worldY);
        this.put("eyePosition", (float)this.worldX, (float)this.worldY, (float)this.worldZ);
        this.put("relativeEyePosition", 0.0f, 0.0f, 0.0f);
        Vector4f vector4f = new Vector4f(0.0f, 0.0f, -1.0f, 0.0f).mul((Matrix4fc)new Matrix4f((Matrix4fc)this.modelView).invert());
        this.put("playerLookVector", vector4f.x, vector4f.y, vector4f.z);
        this.put("playerBodyVector", vector4f.x, 0.0f, vector4f.z);
        this.put("near", 0.05f);
        this.put("far", this.far);
        this.put("dhNearPlane", this.farNear);
        this.put("dhFarPlane", this.farFar);
        this.put("dhRenderDistance", this.farFar);
        this.put("viewWidth", frameContext.width);
        this.put("viewHeight", frameContext.height);
        this.put("aspectRatio", (float)frameContext.width / (float)Math.max(1, frameContext.height));
        this.put("frameCounter", this.frames % 720720L);
        this.put("frameTime", frameContext.frameSeconds);
        this.put("frameTimeCounter", (float)(this.seconds % 3600.0));
        this.put("firstPersonCamera", 1.0f);
    }

    private void celestial() {
        this.direction("sunPosition", this.sun.sun);
        this.direction("moonPosition", this.sun.moon);
        this.direction("shadowLightPosition", this.sun.sunLights ? this.sun.sun : this.sun.moon);
        this.direction("upPosition", new double[]{0.0, 1.0, 0.0});
        this.put("sunAngle", (float)this.sun.sunAngle);
        this.put("shadowAngle", (float)this.sun.shadowAngle);
        this.put("worldTime", this.sun.worldTime);
    }

    private void weather(SceneData sceneData, float f, float f2, float f3, float f4) {
        float f5 = Math.max(sceneData.rain, sceneData.snow);
        this.put("rainStrength", f5);
        float f6 = (f5 > this.wetness ? f3 : f4) / 20.0f;
        this.wetness += (f5 - this.wetness) * (1.0f - (float)Math.pow(0.5, f / Math.max(f6, 0.01f)));
        this.put("wetness", this.wetness);
        this.put("thunderStrength", sceneData.thunder);
        this.put("biome", Biomes.PLAINS);
        this.put("biome_category", Biomes.CATEGORY_PLAINS);
        this.put("biome_precipitation", f5 <= 0.0f ? 0.0f : (sceneData.snow > sceneData.rain ? 2.0f : 1.0f));
        this.put("temperature", sceneData.snow > sceneData.rain ? 0.0f : 0.2f + 0.8f * sceneData.warmth);
        this.put("rainfall", sceneData.humidity);
        float f7 = sceneData.eyeSky;
        float f8 = 1.0f - (float)Math.pow(0.5, f / Math.max(f2 / 20.0f, 0.01f));
        this.eyeSkySmooth = this.eyeSkySmooth < 0.0f ? f7 : this.eyeSkySmooth + (f7 - this.eyeSkySmooth) * f8;
        this.put("eyeBrightness", 0.0f, 240.0f * f7);
        this.put("eyeBrightnessSmooth", 0.0f, 240.0f * this.eyeSkySmooth);
        this.put("fogColor", IrisUniforms.srgb(sceneData.fogR), IrisUniforms.srgb(sceneData.fogG), IrisUniforms.srgb(sceneData.fogB));
        this.put("skyColor", IrisUniforms.srgb(sceneData.zenithR), IrisUniforms.srgb(sceneData.zenithG), IrisUniforms.srgb(sceneData.zenithB));
        float f9 = this.distant ? Float.MAX_VALUE : this.far;
        this.put("fogStart", Math.min(sceneData.fogStart, f9));
        this.put("fogEnd", Math.min(sceneData.fogEnd, f9));
        this.put("fogDensity", sceneData.mist);
        this.put("fogMode", 9729.0f);
        this.put("fogShape", 1.0f);
    }

    private void world(SceneData sceneData, FrameContext frameContext) {
        this.put("worldDay", sceneData.worldDay);
        this.put("moonPhase", sceneData.moonPhase);
        this.put("cloudHeight", 192.0f);
        this.put("cloudTime", sceneData.cloudTime * 20.0f);
        this.put("heightLimit", 384.0f);
        this.put("logicalHeightLimit", 384.0f);
        this.put("bedrockLevel", -64.0f);
        this.put("hasSkylight", 1.0f);
        this.put("ambientLight", 0.0f);
        this.put("screenBrightness", 0.5f);
        this.put("renderStage", IrisMacros.renderStage("NONE"));
        this.put("atlasSize", 4096.0f, 4096.0f);
        this.put("alphaTestRef", 0.1f);
        this.put("isRightHanded", 1.0f);
        this.put("currentColorSpace", 0.0f);
        this.put("textureFilteringMode", 0.0f);
        this.put("centerDepthSmooth", 0.5f);
        this.put("chunkOffset", 0.0f, 0.0f, 0.0f);
        LocalDateTime localDateTime = LocalDateTime.now();
        this.put("currentDate", localDateTime.getYear(), localDateTime.getMonthValue(), localDateTime.getDayOfMonth());
        this.put("currentTime", localDateTime.getHour(), localDateTime.getMinute(), localDateTime.getSecond());
        this.put("currentYearTime", (float)localDateTime.getDayOfYear() * 86400.0f, 0.0f);
    }

    private void neutral() {
        for (String string : new String[]{"blindness", "nightVision", "darknessFactor", "darknessLightFactor", "playerMood", "constantMood", "isEyeInWater", "is_burning", "is_hurt", "is_invisible", "is_sneaking", "is_sprinting", "hideGUI", "isSpectator", "isRiding", "inSwimmingAnimation", "feetInWater", "vehicleInWater", "isElytraFlying", "heldItemId", "heldItemId2", "heldBlockLightValue", "heldBlockLightValue2", "entityId", "blockEntityId", "currentRenderedItemId", "currentSelectedBlockId", "vehicleId", "hasCeiling"}) {
            this.put(string, 0.0f);
        }
        this.put("is_on_ground", 1.0f);
        this.put("entityColor", 0.0f, 0.0f, 0.0f, 0.0f);
        for (String string : new String[]{"Health", "Hunger", "Air", "Armor"}) {
            this.put("currentPlayer" + string, 1.0f);
            this.put("maxPlayer" + string, string.equals("Air") ? 300.0f : 20.0f);
        }
    }

    private void remember() {
        this.previousModelView.set((Matrix4fc)this.modelView);
        this.previousProjection.set((Matrix4fc)this.projection);
        this.previousFarProjection.set((Matrix4fc)this.farProjection);
        this.previousX = this.worldX;
        this.previousY = this.worldY;
        this.previousZ = this.worldZ;
        this.hasPrevious = true;
    }

    void reset() {
        this.hasPrevious = false;
    }

    private void direction(String string, double[] dArray) {
        Vector4f vector4f = new Vector4f((float)dArray[0] * 100.0f, (float)dArray[1] * 100.0f, (float)dArray[2] * 100.0f, 0.0f).mul((Matrix4fc)this.modelView);
        this.put(string, vector4f.x, vector4f.y, vector4f.z);
    }

    private void position(String string, double d, double d2, double d3) {
        this.put(string, (float)d, (float)d2, (float)d3);
        double d4 = d - Math.floor(d);
        double d5 = d2 - Math.floor(d2);
        double d6 = d3 - Math.floor(d3);
        this.put(string + "Fract", (float)d4, (float)d5, (float)d6);
        this.put(string + "Int", (float)Math.floor(d), (float)Math.floor(d2), (float)Math.floor(d3));
    }

    private void matrix(String string, Matrix4f matrix4f) {
        float[] fArray = new float[16];
        matrix4f.get(fArray);
        this.values.put(string, fArray);
    }

    void put(String string, float ... fArray) {
        this.values.put(string, fArray);
    }

    private static float srgb(float f) {
        float f2 = Math.max(0.0f, Math.min(1.0f, f));
        return f2 <= 0.0031308f ? f2 * 12.92f : 1.055f * (float)Math.pow(f2, 0.4166666666666667) - 0.055f;
    }
}

