/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.util.vector.Matrix4f
 *  zombie.core.skinnedmodel.model.ModelInstanceRenderData
 *  zombie.core.skinnedmodel.model.ModelMesh
 *  zombie.core.skinnedmodel.model.ModelSlotRenderData
 *  zombie.core.skinnedmodel.model.SkinningData
 *  zombie.core.textures.ColorInfo
 *  zombie.core.textures.Texture
 *  zombie.iso.IsoGridSquare
 *  zombie.scripting.objects.ModelScript
 */
package viewpoint.models;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import org.joml.Matrix4fc;
import org.lwjgl.util.vector.Matrix4f;
import viewpoint.core.Frame;
import viewpoint.light.OwnedLight;
import viewpoint.models.Characters;
import viewpoint.models.ModelMotion;
import viewpoint.render.ModelDraws;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.iso.IsoGridSquare;
import zombie.scripting.objects.ModelScript;

final class ModelCapture {
    private static final String[] HEAD_BONES = new String[]{"Bip01_Head", "Bip01_HeadNub"};
    private static final float MIDDLE = 0.9f;
    private static final float RADIUS = 1.3f;
    private static final org.joml.Matrix4f place = new org.joml.Matrix4f();
    private static final org.joml.Matrix4f model = new org.joml.Matrix4f();
    private static final org.joml.Matrix4f attachment = new org.joml.Matrix4f();
    private static final Field OUTLINED = ModelCapture.field("characterOutline");
    private static final Field COLOUR = ModelCapture.field("outlineColor");
    private static final Field BEHIND = ModelCapture.field("outlineBehindPlayer");

    static boolean capture(Frame frame, ModelSlotRenderData modelSlotRenderData, IsoGameCharacter isoGameCharacter, boolean bl, Pose pose, float f, float f2, float f3, float f4) {
        if (modelSlotRenderData.modelData.isEmpty()) {
            return false;
        }
        for (int i = 0; i < modelSlotRenderData.modelData.size(); ++i) {
            if (ModelCapture.drawable((ModelInstanceRenderData)modelSlotRenderData.modelData.get(i))) continue;
            return false;
        }
        ModelDraws modelDraws = frame.scene.models;
        ModelCapture.placed(frame, modelSlotRenderData, f, f2, f3, f4);
        float f5 = place.m30();
        float f6 = place.m31();
        float f7 = place.m32();
        modelDraws.slot(modelSlotRenderData, (float)Math.sqrt(f5 * f5 + f7 * f7));
        IsoGridSquare isoGridSquare = isoGameCharacter.getCurrentSquare();
        int n = isoGridSquare == null ? -1 : OwnedLight.modelLightAt(isoGridSquare.x, isoGridSquare.y, isoGridSquare.z);
        boolean bl2 = isoGridSquare != null && isoGridSquare.isOutside() && !isoGridSquare.haveRoof;
        int n2 = modelDraws.count();
        for (int i = 0; i < modelSlotRenderData.modelData.size(); ++i) {
            ModelInstanceRenderData modelInstanceRenderData = (ModelInstanceRenderData)modelSlotRenderData.modelData.get(i);
            int n3 = ModelCapture.add(modelDraws, modelInstanceRenderData, modelInstanceRenderData.model.tex, isoGameCharacter, bl, pose);
            ModelMotion.follow(modelDraws, n3, (Object)modelInstanceRenderData.modelInstance, modelInstanceRenderData.model.isStatic ? 0 : modelInstanceRenderData.matrixPalette.limit() / 16, pose == Pose.LAST);
            modelDraws.material(n3, modelInstanceRenderData.modelInstance.tintR, modelInstanceRenderData.modelInstance.tintG, modelInstanceRenderData.modelInstance.tintB, modelInstanceRenderData.hue);
            ModelCapture.light(modelDraws, n3, modelSlotRenderData, n, bl2);
            modelDraws.bounds(n3, f5, f6 + 0.9f, f7, 1.3f * Math.max(1.0f, modelSlotRenderData.finalScale));
        }
        ModelCapture.outline(modelDraws, modelSlotRenderData, n2);
        return true;
    }

    private static void placed(Frame frame, ModelSlotRenderData modelSlotRenderData, float f, float f2, float f3, float f4) {
        if (modelSlotRenderData.inVehicle) {
            Characters.seatMatrix(place, modelSlotRenderData, frame.camX, frame.camY, frame.camZ).scale(1.5f);
        } else {
            place.translation(-(f - frame.camX), (f3 - frame.camZ) * 2.4494896f, -(f2 - frame.camY)).scale(-1.5f, 1.5f, 1.5f).rotateY(f4 + (float)Math.PI);
        }
        place.scale(modelSlotRenderData.finalScale);
    }

    private static void light(ModelDraws modelDraws, int n, ModelSlotRenderData modelSlotRenderData, int n2, boolean bl) {
        modelDraws.light(n, n2 < 0 ? Math.min(1.0f, modelSlotRenderData.ambientR) : (float)(n2 & 0xFF) / 255.0f, n2 < 0 ? Math.min(1.0f, modelSlotRenderData.ambientG) : (float)(n2 >> 8 & 0xFF) / 255.0f, n2 < 0 ? Math.min(1.0f, modelSlotRenderData.ambientB) : (float)(n2 >> 16 & 0xFF) / 255.0f, bl);
    }

    private static void outline(ModelDraws modelDraws, ModelSlotRenderData modelSlotRenderData, int n) {
        if (OUTLINED == null || COLOUR == null || BEHIND == null) {
            return;
        }
        try {
            if (!OUTLINED.getBoolean(modelSlotRenderData)) {
                return;
            }
            ColorInfo colorInfo = (ColorInfo)COLOUR.get(modelSlotRenderData);
            boolean bl = BEHIND.getBoolean(modelSlotRenderData);
            for (int i = n; i < modelDraws.count(); ++i) {
                modelDraws.outline(i, colorInfo.r, colorInfo.g, colorInfo.b, colorInfo.a, bl);
            }
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            // empty catch block
        }
    }

    private static Field field(String string) {
        try {
            Field field = ModelSlotRenderData.class.getDeclaredField(string);
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            System.out.println("[Viewpoint] models: owned characters are not outlined (" + string + "): " + String.valueOf(exception));
            return null;
        }
    }

    static boolean loaded(Model model) {
        ModelMesh modelMesh = model == null ? null : model.mesh;
        return modelMesh != null && modelMesh.vb != null && modelMesh.isReady() && !ModelDraws.refused(modelMesh);
    }

    private static boolean drawable(ModelInstanceRenderData modelInstanceRenderData) {
        if (!ModelCapture.loaded(modelInstanceRenderData.model)) {
            return false;
        }
        return modelInstanceRenderData.model.isStatic || modelInstanceRenderData.matrixPalette != null && modelInstanceRenderData.matrixPalette.limit() >= 16;
    }

    private static int add(ModelDraws modelDraws, ModelInstanceRenderData modelInstanceRenderData, Texture texture, IsoGameCharacter isoGameCharacter, boolean bl, Pose pose) {
        Object object;
        int n;
        int n2;
        ModelScript modelScript = modelInstanceRenderData.modelInstance == null ? null : modelInstanceRenderData.modelInstance.modelScript;
        int n3 = n2 = modelScript == null || modelScript.cullFace == -1 ? 1028 : modelScript.cullFace;
        if (modelInstanceRenderData.model.isStatic) {
            model.set((Matrix4fc)place).mul((Matrix4fc)attachment.set((Matrix4fc)modelInstanceRenderData.xfrm).transpose());
            String string = modelInstanceRenderData.modelInstance == null ? null : modelInstanceRenderData.modelInstance.parentBoneName;
            boolean bl2 = bl && string != null && string.contains("Head");
            return modelDraws.add(modelInstanceRenderData.model.mesh, modelInstanceRenderData.modelInstance, texture, model, -1, -1, n2, bl2 ? 2 : 3);
        }
        FloatBuffer floatBuffer = modelInstanceRenderData.matrixPalette;
        int n4 = floatBuffer.limit() / 16;
        int n5 = n = ModelCapture.palette(modelDraws, modelInstanceRenderData, pose, n4);
        if (bl && (object = modelInstanceRenderData.model.tag) instanceof SkinningData) {
            SkinningData skinningData = (SkinningData)object;
            if (skinningData.boneIndices != null) {
                n5 = modelDraws.palette(floatBuffer, n4);
                ModelCapture.collapse(modelDraws, n5, n4, skinningData, isoGameCharacter.getAnimationPlayer());
            }
        }
        return modelDraws.add(modelInstanceRenderData.model.mesh, modelInstanceRenderData.modelInstance, texture, model.set((Matrix4fc)place), n5, n, n2, 3);
    }

    private static int palette(ModelDraws modelDraws, ModelInstanceRenderData modelInstanceRenderData, Pose pose, int n) {
        if (pose == Pose.PLAYER) {
            ModelInstance modelInstance = modelInstanceRenderData.modelInstance;
            return modelDraws.palette(modelInstance.animPlayer.getSkinTransforms((SkinningData)modelInstance.model.tag));
        }
        int n2 = pose == Pose.LAST ? ModelMotion.lastPose(modelDraws, (Object)modelInstanceRenderData.modelInstance, n) : -1;
        return n2 >= 0 ? n2 : modelDraws.palette(modelInstanceRenderData.matrixPalette, n);
    }

    static boolean posable(ModelSlotRenderData modelSlotRenderData, ModelManager.ModelSlot modelSlot) {
        if (modelSlotRenderData.inVehicle || OUTLINED == null) {
            return false;
        }
        try {
            if (OUTLINED.getBoolean(modelSlotRenderData)) {
                return false;
            }
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            return false;
        }
        int n = 0;
        if (ModelCapture.ready(modelSlot.model)) {
            n = ModelCapture.same(modelSlotRenderData, n, modelSlot.model) ? 1 : -1;
        }
        n = n < 0 ? -1 : ModelCapture.sameInstances(modelSlotRenderData, n, modelSlot.sub);
        return n == modelSlotRenderData.modelData.size();
    }

    private static int sameInstances(ModelSlotRenderData modelSlotRenderData, int n, ArrayList<ModelInstance> arrayList) {
        for (int i = 0; i < arrayList.size() && n >= 0; ++i) {
            ModelInstance modelInstance = arrayList.get(i);
            if (!ModelCapture.ready(modelInstance)) continue;
            n = ModelCapture.same(modelSlotRenderData, n, modelInstance) ? ModelCapture.sameInstances(modelSlotRenderData, n + 1, modelInstance.sub) : -1;
        }
        return n;
    }

    private static boolean ready(ModelInstance modelInstance) {
        return modelInstance.model.isReady() && (modelInstance.animPlayer == null || modelInstance.animPlayer.isReady());
    }

    private static boolean same(ModelSlotRenderData modelSlotRenderData, int n, ModelInstance modelInstance) {
        if (n >= modelSlotRenderData.modelData.size()) {
            return false;
        }
        ModelInstanceRenderData modelInstanceRenderData = (ModelInstanceRenderData)modelSlotRenderData.modelData.get(n);
        return modelInstanceRenderData.modelInstance == modelInstance && modelInstanceRenderData.model == modelInstance.model && !modelInstance.model.isStatic && modelInstance.animPlayer != null && modelInstanceRenderData.hue == modelInstance.hue && !modelInstance.hasTextureCreator() && modelInstanceRenderData.matrixPalette != null && modelInstance.animPlayer.getSkinTransforms((SkinningData)modelInstance.model.tag).length * 16 == modelInstanceRenderData.matrixPalette.limit();
    }

    private static void collapse(ModelDraws modelDraws, int n, int n2, SkinningData skinningData, AnimationPlayer animationPlayer) {
        int n3;
        int n4 = n3 = animationPlayer == null ? -1 : animationPlayer.getSkinningBoneIndex(HEAD_BONES[0], -1);
        if (n3 < 0) {
            return;
        }
        Matrix4f matrix4f = animationPlayer.getModelTransformAt(n3);
        for (String string : HEAD_BONES) {
            Integer n5 = (Integer)skinningData.boneIndices.get(string);
            if (n5 == null || n5 >= n2) continue;
            modelDraws.collapse(n + n5, matrix4f.m03, matrix4f.m13, matrix4f.m23);
        }
    }

    private ModelCapture() {
    }

    static enum Pose {
        SNAPSHOT,
        LAST,
        PLAYER;

    }
}

