/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  zombie.core.skinnedmodel.model.ModelInstanceRenderData
 *  zombie.core.skinnedmodel.model.ModelSlotRenderData
 *  zombie.core.skinnedmodel.model.VehicleModelInstance
 *  zombie.core.skinnedmodel.shader.Shader
 *  zombie.core.textures.Texture
 *  zombie.iso.IsoGridSquare
 *  zombie.vehicles.BaseVehicle
 */
package viewpoint.models;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import viewpoint.core.Frame;
import viewpoint.light.OwnedLight;
import viewpoint.models.ModelCapture;
import viewpoint.models.ModelMotion;
import viewpoint.render.ModelDraws;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.skinnedmodel.model.VehicleModelInstance;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.textures.Texture;
import zombie.iso.IsoGridSquare;
import zombie.vehicles.BaseVehicle;

final class VehicleCapture {
    private static final Matrix4f place = new Matrix4f();
    private static final Matrix4f model = new Matrix4f();
    private static final Texture[] textures = new Texture[7];
    private static final float[][] switches = new float[12][];

    static boolean capture(Frame frame, ModelSlotRenderData modelSlotRenderData, BaseVehicle baseVehicle) {
        if (modelSlotRenderData.modelData.isEmpty()) {
            return false;
        }
        for (int i = 0; i < modelSlotRenderData.modelData.size(); ++i) {
            if (VehicleCapture.drawable((ModelInstanceRenderData)modelSlotRenderData.modelData.get(i))) continue;
            return false;
        }
        ModelDraws modelDraws = frame.scene.models;
        VehicleCapture.placed(frame, modelSlotRenderData);
        IsoGridSquare isoGridSquare = baseVehicle.getCurrentSquare();
        int n = isoGridSquare == null ? -1 : OwnedLight.modelLightAt(isoGridSquare.x, isoGridSquare.y, isoGridSquare.z);
        boolean bl = isoGridSquare != null && isoGridSquare.isOutside() && !isoGridSquare.haveRoof;
        VehicleModelInstance vehicleModelInstance = null;
        int n2 = -1;
        int n3 = -1;
        for (int i = 0; i < modelSlotRenderData.modelData.size(); ++i) {
            VehicleModelInstance vehicleModelInstance2;
            ModelInstanceRenderData modelInstanceRenderData = (ModelInstanceRenderData)modelSlotRenderData.modelData.get(i);
            ModelInstance modelInstance = modelInstanceRenderData.modelInstance;
            Shader shader = modelInstanceRenderData.model.effect;
            int n4 = modelInstance.modelScript != null && modelInstance.modelScript.invertX ? 1029 : 1028;
            model.set((Matrix4fc)place).mul((Matrix4fc)modelInstanceRenderData.xfrm);
            int n5 = modelInstanceRenderData.model.isStatic ? -1 : modelDraws.palette(modelInstanceRenderData.matrixPalette, modelInstanceRenderData.matrixPalette.limit() / 16);
            VehicleModelInstance vehicleModelInstance3 = vehicleModelInstance2 = shader.isVehicleShader() ? VehicleCapture.body(modelInstance) : null;
            Texture texture = vehicleModelInstance2 != null ? vehicleModelInstance2.tex : (modelInstance.tex != null ? modelInstance.tex : modelInstanceRenderData.model.tex);
            int n6 = modelDraws.add(modelInstanceRenderData.model.mesh, null, texture, model, n5, n5, n4, 3);
            ModelMotion.follow(modelDraws, n6, (Object)modelInstance, modelInstanceRenderData.model.isStatic ? 0 : modelInstanceRenderData.matrixPalette.limit() / 16);
            if (vehicleModelInstance2 != null) {
                if (vehicleModelInstance2 != vehicleModelInstance || VehicleCapture.kind(shader) != n3) {
                    vehicleModelInstance = vehicleModelInstance2;
                    n3 = VehicleCapture.kind(shader);
                    n2 = VehicleCapture.material(modelDraws, vehicleModelInstance2, n3);
                }
                modelDraws.vehicle(n6, n2);
            }
            modelDraws.material(n6, modelInstance.tintR, modelInstance.tintG, modelInstance.tintB, 0.0f);
            VehicleCapture.light(modelDraws, n6, modelSlotRenderData, n, bl);
            modelDraws.bounds(n6, model, modelInstanceRenderData.model.mesh.minXyz, modelInstanceRenderData.model.mesh.maxXyz, modelInstanceRenderData.model.isStatic ? 1.0f : 2.0f);
        }
        return true;
    }

    private static void placed(Frame frame, ModelSlotRenderData modelSlotRenderData) {
        place.translation(-(modelSlotRenderData.x - frame.camX), (modelSlotRenderData.z - frame.camZ) * 2.4494896f + BaseVehicle.centerOfMassMagic, -(modelSlotRenderData.y - frame.camY)).scale(-1.0f, 1.0f, 1.0f).rotateY((float)Math.PI).translate(0.0f, modelSlotRenderData.centerOfMassY, 0.0f);
    }

    private static void light(ModelDraws modelDraws, int n, ModelSlotRenderData modelSlotRenderData, int n2, boolean bl) {
        modelDraws.light(n, n2 < 0 ? Math.min(1.0f, modelSlotRenderData.ambientR) : (float)(n2 & 0xFF) / 255.0f, n2 < 0 ? Math.min(1.0f, modelSlotRenderData.ambientG) : (float)(n2 >> 8 & 0xFF) / 255.0f, n2 < 0 ? Math.min(1.0f, modelSlotRenderData.ambientB) : (float)(n2 >> 16 & 0xFF) / 255.0f, bl);
    }

    private static boolean drawable(ModelInstanceRenderData modelInstanceRenderData) {
        if (!ModelCapture.loaded(modelInstanceRenderData.model) || modelInstanceRenderData.model.effect == null) {
            return false;
        }
        if (modelInstanceRenderData.model.effect.isVehicleShader() && VehicleCapture.body(modelInstanceRenderData.modelInstance) == null) {
            return false;
        }
        return modelInstanceRenderData.model.isStatic || modelInstanceRenderData.matrixPalette != null && modelInstanceRenderData.matrixPalette.limit() >= 16;
    }

    private static VehicleModelInstance body(ModelInstance modelInstance) {
        VehicleModelInstance vehicleModelInstance;
        if (modelInstance instanceof VehicleModelInstance) {
            VehicleModelInstance vehicleModelInstance2 = (VehicleModelInstance)modelInstance;
            return vehicleModelInstance2;
        }
        ModelInstance modelInstance2 = modelInstance.parent;
        return modelInstance2 instanceof VehicleModelInstance ? (vehicleModelInstance = (VehicleModelInstance)modelInstance2) : null;
    }

    private static int kind(Shader shader) {
        String string = shader.getName();
        if (string.contains("norandom")) {
            return 2;
        }
        return string.contains("multiuv") ? 1 : 0;
    }

    private static int material(ModelDraws modelDraws, VehicleModelInstance vehicleModelInstance, int n) {
        VehicleCapture.textures[0] = vehicleModelInstance.textureRust;
        VehicleCapture.textures[1] = vehicleModelInstance.textureMask;
        VehicleCapture.textures[2] = vehicleModelInstance.textureLights;
        VehicleCapture.textures[3] = vehicleModelInstance.textureDamage1Overlay;
        VehicleCapture.textures[4] = vehicleModelInstance.textureDamage1Shell;
        VehicleCapture.textures[5] = vehicleModelInstance.textureDamage2Overlay;
        VehicleCapture.textures[6] = vehicleModelInstance.textureDamage2Shell;
        VehicleCapture.switches[0] = vehicleModelInstance.textureUninstall1;
        VehicleCapture.switches[1] = vehicleModelInstance.textureUninstall2;
        VehicleCapture.switches[2] = vehicleModelInstance.textureLightsEnables1;
        VehicleCapture.switches[3] = vehicleModelInstance.textureLightsEnables2;
        VehicleCapture.switches[4] = vehicleModelInstance.textureDamage1Enables1;
        VehicleCapture.switches[5] = vehicleModelInstance.textureDamage1Enables2;
        VehicleCapture.switches[6] = vehicleModelInstance.textureDamage2Enables1;
        VehicleCapture.switches[7] = vehicleModelInstance.textureDamage2Enables2;
        VehicleCapture.switches[8] = vehicleModelInstance.matrixBlood1Enables1;
        VehicleCapture.switches[9] = vehicleModelInstance.matrixBlood1Enables2;
        VehicleCapture.switches[10] = vehicleModelInstance.matrixBlood2Enables1;
        VehicleCapture.switches[11] = vehicleModelInstance.matrixBlood2Enables2;
        return modelDraws.vehicles.add(textures, switches, vehicleModelInstance.textureRustA, vehicleModelInstance.painColor.x, vehicleModelInstance.painColor.y, vehicleModelInstance.painColor.z, n);
    }

    private VehicleCapture() {
    }
}

