/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 *  org.lwjgl.util.vector.Matrix4f
 *  zombie.core.skinnedmodel.model.ModelSlotRenderData
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoObject
 *  zombie.iso.fboRenderChunk.FBORenderObjectOutline
 *  zombie.vehicles.BaseVehicle
 */
package viewpoint.models;

import org.joml.Vector3f;
import org.lwjgl.util.vector.Matrix4f;
import viewpoint.core.Frame;
import viewpoint.input.Controls;
import viewpoint.input.FreeCam;
import viewpoint.input.ThirdPerson;
import viewpoint.models.CharacterRate;
import viewpoint.models.KeptSnapshots;
import viewpoint.models.ModelCapture;
import viewpoint.models.ModelCull;
import viewpoint.models.VehicleCapture;
import viewpoint.visibility.Rooms;
import zombie.characters.IsoGameCharacter;
import zombie.core.PerformanceSettings;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.iso.IsoCell;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.fboRenderChunk.FBORenderObjectOutline;
import zombie.ui.UIManager;
import zombie.vehicles.BaseVehicle;

public final class Characters {
    private static final float MODEL_RANGE = 124.0f;
    private static final float BEHIND = -0.17f;
    private static final float VEHICLE_ALWAYS = 12.0f;
    private static final float BLOB_RADIUS = 0.45f;
    private static final org.joml.Matrix4f eyeSeat = new org.joml.Matrix4f();
    private static final Vector3f head = new Vector3f();
    private static final String[] HEAD_BONES = new String[]{"Bip01_Head", "Bip01_HeadNub"};

    public static void snapshot(Frame frame, IsoCell isoCell, IsoGameCharacter isoGameCharacter) {
        ModelCull.range = Math.min(124.0f, frame.scene.fogEnd);
        CharacterRate.begin();
        float f = (float)Math.cos(frame.viewYaw);
        float f2 = (float)Math.sin(frame.viewYaw);
        float f3 = frame.camX + ThirdPerson.offsetX;
        float f4 = frame.camY + ThirdPerson.offsetY;
        for (IsoMovingObject isoMovingObject : isoCell.getObjectList()) {
            float f5;
            IsoGameCharacter isoGameCharacter2;
            if (!(isoMovingObject instanceof IsoGameCharacter) || !(isoGameCharacter2 = (IsoGameCharacter)isoMovingObject).hasActiveModel() || !Characters.inRange(isoGameCharacter2, frame.camX, frame.camY, ModelCull.range) || isoGameCharacter2 != isoGameCharacter && Characters.behind(isoGameCharacter2, f3, f4, f, f2)) continue;
            if (isoGameCharacter2 != isoGameCharacter && ModelCull.roomHidden(isoGameCharacter2)) {
                Rooms.leftOut();
                continue;
            }
            ModelManager.ModelSlot modelSlot = isoGameCharacter2.legsSprite.modelSlot;
            if (modelSlot.model == null || modelSlot.model.object == null) continue;
            isoGameCharacter2.checkUpdateModelTextures();
            float f6 = isoGameCharacter2.getX() - frame.camX;
            float f7 = isoGameCharacter2.getY() - frame.camY;
            ModelCapture.Pose pose = CharacterRate.pose(isoGameCharacter2, modelSlot, (float)Math.sqrt(f6 * f6 + f7 * f7), isoGameCharacter2 == isoGameCharacter);
            boolean bl = pose != ModelCapture.Pose.SNAPSHOT;
            ModelSlotRenderData modelSlotRenderData = bl ? CharacterRate.kept(isoGameCharacter2) : Characters.fresh(frame, modelSlot, isoGameCharacter2, isoGameCharacter);
            float f8 = bl ? isoGameCharacter2.getX() : modelSlotRenderData.x;
            float f9 = bl ? isoGameCharacter2.getY() : modelSlotRenderData.y;
            float f10 = bl ? isoGameCharacter2.getZ() + CharacterRate.lift(isoGameCharacter2) : modelSlotRenderData.z;
            float f11 = f5 = bl ? isoGameCharacter2.getAnimationPlayer().getRenderedAngle() : modelSlotRenderData.animPlayerAngle;
            if (!modelSlotRenderData.inVehicle) {
                frame.scene.blob(-(f8 - frame.camX), (f10 - frame.camZ) * 2.4494896f, -(f9 - frame.camY), 0.45f);
            }
            boolean bl2 = isoGameCharacter2 == isoGameCharacter && !FreeCam.active && !ThirdPerson.headShown;
            int n = frame.scene.models.count();
            ModelCapture.capture(frame, modelSlotRenderData, isoGameCharacter2, bl2, pose, f8, f9, f10, f5);
            if (isoGameCharacter2 == frame.lootAnimal) {
                frame.scene.models.target(n);
            }
            CharacterRate.drawn(pose);
            KeptSnapshots.use(frame, modelSlotRenderData);
        }
        CharacterRate.end();
        Characters.vehicles(frame, isoCell, f3, f4, f, f2);
    }

    private static ModelSlotRenderData fresh(Frame frame, ModelManager.ModelSlot modelSlot, IsoGameCharacter isoGameCharacter, IsoGameCharacter isoGameCharacter2) {
        modelSlot.model.updateLights();
        ModelSlotRenderData modelSlotRenderData = ModelSlotRenderData.alloc();
        modelSlotRenderData.initModel(modelSlot);
        modelSlotRenderData.init(modelSlot);
        modelSlotRenderData.alpha = 1.0f;
        ++modelSlot.renderRefCount;
        CharacterRate.took(isoGameCharacter, modelSlotRenderData);
        if (isoGameCharacter == isoGameCharacter2 && modelSlotRenderData.inVehicle) {
            Characters.seatedEye(frame, modelSlotRenderData, isoGameCharacter);
        }
        return modelSlotRenderData;
    }

    private static void vehicles(Frame frame, IsoCell isoCell, float f, float f2, float f3, float f4) {
        for (BaseVehicle baseVehicle : isoCell.getVehicles()) {
            ModelManager.ModelSlot modelSlot;
            if (baseVehicle.sprite == null || !baseVehicle.sprite.hasActiveModel() || !Characters.inRange((IsoMovingObject)baseVehicle, frame.camX, frame.camY, frame.scene.fogEnd) || !Characters.inRange((IsoMovingObject)baseVehicle, frame.camX, frame.camY, 12.0f) && Characters.behind((IsoMovingObject)baseVehicle, f, f2, f3, f4) || (modelSlot = baseVehicle.sprite.modelSlot) == null || modelSlot.model == null || modelSlot.model.object == null) continue;
            ModelSlotRenderData modelSlotRenderData = ModelSlotRenderData.alloc();
            modelSlotRenderData.initModel(modelSlot);
            modelSlotRenderData.init(modelSlot);
            ++modelSlot.renderRefCount;
            int n = frame.scene.models.count();
            VehicleCapture.capture(frame, modelSlotRenderData, baseVehicle);
            frame.snapshots.add(modelSlotRenderData);
            if (baseVehicle != frame.lootVehicle) continue;
            frame.scene.models.target(n);
        }
    }

    public static void outlinesShown(IsoCell isoCell, int n) {
        FBORenderObjectOutline fBORenderObjectOutline = FBORenderObjectOutline.getInstance();
        for (IsoMovingObject isoMovingObject : isoCell.getObjectList()) {
            if (!isoMovingObject.isOutlineHighlight(n)) continue;
            if (PerformanceSettings.fboRenderChunk) {
                long l = fBORenderObjectOutline.getDuringUIRenderTime(n, (IsoObject)isoMovingObject);
                long l2 = fBORenderObjectOutline.getDuringUIUpdateTime(n, (IsoObject)isoMovingObject);
                if (l != 0L && l == UIManager.uiRenderTimeMS || l2 != 0L && l2 == UIManager.uiUpdateTimeMS) continue;
            }
            isoMovingObject.setOutlineHighlight(n, false);
        }
    }

    private static boolean behind(IsoMovingObject isoMovingObject, float f, float f2, float f3, float f4) {
        float f5;
        float f6 = isoMovingObject.getX() - f;
        float f7 = (float)Math.sqrt(f6 * f6 + (f5 = isoMovingObject.getY() - f2) * f5);
        return f7 > 3.0f && (f6 * f3 + f5 * f4) / f7 < -0.17f;
    }

    private static boolean inRange(IsoMovingObject isoMovingObject, float f, float f2, float f3) {
        float f4;
        float f5 = isoMovingObject.getX() - f;
        return f5 * f5 + (f4 = isoMovingObject.getY() - f2) * f4 <= f3 * f3;
    }

    private static void seatedEye(Frame frame, ModelSlotRenderData modelSlotRenderData, IsoGameCharacter isoGameCharacter) {
        int n;
        AnimationPlayer animationPlayer = isoGameCharacter.getAnimationPlayer();
        int n2 = n = animationPlayer == null ? -1 : animationPlayer.getSkinningBoneIndex(HEAD_BONES[0], -1);
        if (n < 0) {
            return;
        }
        Matrix4f matrix4f = animationPlayer.getModelTransformAt(n);
        head.set(matrix4f.m03 * 1.5f, matrix4f.m13 * 1.5f, matrix4f.m23 * 1.5f);
        Characters.seatMatrix(eyeSeat, modelSlotRenderData, frame.camX, frame.camY, frame.camZ).transformPosition(head);
        Controls.eyeAtHead(isoGameCharacter, frame, Characters.head.x, Characters.head.y, Characters.head.z);
    }

    static org.joml.Matrix4f seatMatrix(org.joml.Matrix4f matrix4f, ModelSlotRenderData modelSlotRenderData, float f, float f2, float f3) {
        return matrix4f.translation(-(modelSlotRenderData.x - f), (modelSlotRenderData.z - f3) * 2.4494896f + BaseVehicle.centerOfMassMagic, -(modelSlotRenderData.y - f2)).scale(-1.0f, 1.0f, 1.0f).rotateY((float)Math.PI).translate(0.0f, modelSlotRenderData.centerOfMassY, 0.0f).rotateZ((float)Math.toRadians(modelSlotRenderData.vehicleAngleZ)).rotateY((float)Math.toRadians(modelSlotRenderData.vehicleAngleY)).rotateX((float)Math.toRadians(modelSlotRenderData.vehicleAngleX)).rotateY((float)Math.PI).translate(modelSlotRenderData.inVehicleX, modelSlotRenderData.inVehicleY, -modelSlotRenderData.inVehicleZ);
    }

    public static void release(Frame frame) {
        for (int i = 0; i < frame.snapshots.size(); ++i) {
            ModelSlotRenderData modelSlotRenderData = frame.snapshots.get(i);
            if (!KeptSnapshots.release(modelSlotRenderData)) continue;
            modelSlotRenderData.postRender();
        }
        frame.snapshots.clear();
    }

    private Characters() {
    }
}

