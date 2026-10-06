/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  zombie.core.skinnedmodel.animation.BoneTransform
 *  zombie.core.skinnedmodel.animation.TwistableBoneTransform
 *  zombie.core.skinnedmodel.model.ModelInstanceTextureCreator
 *  zombie.core.skinnedmodel.visual.ItemVisuals
 *  zombie.core.textures.Texture
 *  zombie.iso.Vector2
 *  zombie.iso.objects.IsoDeadBody
 */
package viewpoint.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import viewpoint.core.Frame;
import viewpoint.render.CorpseTextures;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.BoneTransform;
import zombie.core.skinnedmodel.animation.TwistableBoneTransform;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceTextureCreator;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.Texture;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;

final class CorpsePose {
    private static final long MODEL_BYTES = 65536L;
    final Object look;
    private final AnimatedModel model = new AnimatedModel();
    private final ItemVisuals itemVisuals = new ItemVisuals();
    final float ambientR;
    final float ambientG;
    final float ambientB;
    private boolean built;
    ModelInstance[] parts;
    int[] paletteOf;
    Matrix4f[] attachments;
    float[][] palettes;
    int[] paletteBones;
    float angle;
    float scale;
    CorpseTextures textures;
    long lastUsed;
    private long bytes;

    CorpsePose(IsoDeadBody isoDeadBody, Object object) {
        this.look = object;
        DeadBodyAtlas.BodyParams bodyParams = new DeadBodyAtlas.BodyParams();
        bodyParams.init(isoDeadBody);
        this.ambientR = bodyParams.ambient.r;
        this.ambientG = bodyParams.ambient.g;
        this.ambientB = bodyParams.ambient.b;
        this.model.setAnimate(false);
        this.model.setAnimSetName(bodyParams.animSetName);
        this.model.setState(bodyParams.stateName);
        this.model.setPrimaryHandModelName(bodyParams.primaryHandItem);
        this.model.setSecondaryHandModelName(bodyParams.secondaryHandItem);
        this.model.setAttachedModelNames(bodyParams.attachedModelNames);
        this.model.setVariable("FallOnFront", bodyParams.fallOnFront);
        this.model.setVariable("KilledByFall", bodyParams.killedByFall);
        bodyParams.variables.forEach((string, string2) -> this.model.setVariable((String)string, (String)string2));
        this.itemVisuals.addAll((Collection)bodyParams.itemVisuals);
        this.model.setModelData(bodyParams.baseVisual, bodyParams.itemVisuals);
        this.model.setAngle(new Vector2().setLengthAndDirection(bodyParams.angle, 1.0f));
        this.deathPose(bodyParams.diedBoneTransforms);
        this.model.setTrackTime(bodyParams.trackTime);
        this.model.setGrappleable(bodyParams.grappleable);
        this.model.update();
    }

    private void deathPose(TwistableBoneTransform[] twistableBoneTransformArray) {
        AnimationPlayer animationPlayer = this.model.getAnimationPlayer();
        if (twistableBoneTransformArray == null || twistableBoneTransformArray.length == 0 || animationPlayer.isBoneCountMismatched((BoneTransform[])twistableBoneTransformArray)) {
            return;
        }
        animationPlayer.stopAll();
        AnimationTrack animationTrack = animationPlayer.play("DeathPose", true, true, -1.0f);
        if (animationTrack != null) {
            animationTrack.setBlendWeight(1.0f);
            animationTrack.initRagdollTransforms(twistableBoneTransformArray);
        }
    }

    boolean advance() {
        if (this.built) {
            return true;
        }
        if (!this.model.isReadyToRender()) {
            return false;
        }
        this.model.update();
        ArrayList<ModelInstance> arrayList = new ArrayList<ModelInstance>();
        ArrayList<float[]> arrayList2 = new ArrayList<float[]>();
        ArrayList<Integer> arrayList3 = new ArrayList<Integer>();
        ArrayList<Matrix4f> arrayList4 = new ArrayList<Matrix4f>();
        CorpsePose.list(this.model.getModelInstance(), null, arrayList, arrayList2, arrayList3, arrayList4);
        this.parts = arrayList.toArray(new ModelInstance[0]);
        this.paletteOf = arrayList3.stream().mapToInt(Integer::intValue).toArray();
        this.attachments = arrayList4.toArray(new Matrix4f[0]);
        this.palettes = (float[][])arrayList2.toArray((T[])new float[0][]);
        this.paletteBones = arrayList2.stream().mapToInt(fArray -> ((float[])fArray).length / 12).toArray();
        this.angle = this.model.getAnimationPlayer().getRenderedAngle();
        this.scale = this.model.getScale();
        ModelInstanceTextureCreator modelInstanceTextureCreator = ModelInstanceTextureCreator.alloc();
        modelInstanceTextureCreator.init(this.model.getVisual(), this.itemVisuals, this.model.getModelInstance());
        this.textures = new CorpseTextures(modelInstanceTextureCreator);
        this.releasePlayer();
        this.built = true;
        return true;
    }

    private void releasePlayer() {
        CorpsePose.forgetPlayer(this.model.getModelInstance());
        this.model.releaseAnimationPlayer();
    }

    private static void forgetPlayer(ModelInstance modelInstance) {
        modelInstance.animPlayer = null;
        for (ModelInstance modelInstance2 : modelInstance.sub) {
            CorpsePose.forgetPlayer(modelInstance2);
        }
    }

    private static void list(ModelInstance modelInstance, AnimatedModel.AnimatedModelInstanceRenderData animatedModelInstanceRenderData, ArrayList<ModelInstance> arrayList, ArrayList<float[]> arrayList2, ArrayList<Integer> arrayList3, ArrayList<Matrix4f> arrayList4) {
        AnimatedModel.AnimatedModelInstanceRenderData animatedModelInstanceRenderData2 = new AnimatedModel.AnimatedModelInstanceRenderData();
        animatedModelInstanceRenderData2.initModel(modelInstance, animatedModelInstanceRenderData);
        animatedModelInstanceRenderData2.transformToParent(animatedModelInstanceRenderData);
        boolean bl = modelInstance.model.isStatic;
        if (modelInstance.model.mesh != null && (bl || animatedModelInstanceRenderData2.matrixPalette != null && animatedModelInstanceRenderData2.matrixPalette.limit() >= 16)) {
            arrayList.add(modelInstance);
            arrayList3.add(bl ? -1 : CorpsePose.shared(arrayList2, animatedModelInstanceRenderData2.matrixPalette.limit() / 16, animatedModelInstanceRenderData2));
            arrayList4.add(new Matrix4f((Matrix4fc)animatedModelInstanceRenderData2.xfrm));
        }
        for (ModelInstance modelInstance2 : modelInstance.sub) {
            CorpsePose.list(modelInstance2, animatedModelInstanceRenderData2, arrayList, arrayList2, arrayList3, arrayList4);
        }
    }

    private static int shared(ArrayList<float[]> arrayList, int n, AnimatedModel.AnimatedModelInstanceRenderData animatedModelInstanceRenderData) {
        int n2;
        float[] fArray = new float[n * 12];
        for (n2 = 0; n2 < n; ++n2) {
            animatedModelInstanceRenderData.matrixPalette.get(n2 * 16, fArray, n2 * 12, 12);
        }
        for (n2 = 0; n2 < arrayList.size(); ++n2) {
            if (!Arrays.equals(arrayList.get(n2), fArray)) continue;
            return n2;
        }
        arrayList.add(fArray);
        return arrayList.size() - 1;
    }

    boolean drawable() {
        return this.built && this.textures.made();
    }

    boolean built() {
        return this.built;
    }

    long bytes() {
        if (this.bytes == 0L && this.drawable()) {
            IdentityHashMap<Texture, Boolean> identityHashMap = new IdentityHashMap<Texture, Boolean>();
            long l = 65536L;
            for (ModelInstance modelInstance : this.parts) {
                Texture texture = modelInstance.tex;
                if (texture == null || !texture.isReady() || identityHashMap.put(texture, Boolean.TRUE) != null) continue;
                l += (long)texture.getWidth() * (long)texture.getHeight() * 4L * 4L / 3L;
            }
            this.bytes = l;
        }
        return this.bytes == 0L ? 65536L : this.bytes;
    }

    void hold(Frame frame) {
        CorpsePose.hold(frame, this.model.getModelInstance());
    }

    private static void hold(Frame frame, ModelInstance modelInstance) {
        if (modelInstance == null) {
            return;
        }
        ++modelInstance.renderRefCount;
        frame.corpseInstances.add(modelInstance);
        for (ModelInstance modelInstance2 : modelInstance.sub) {
            CorpsePose.hold(frame, modelInstance2);
        }
    }

    void letGo() {
        this.reset(this.model.getModelInstance());
        this.model.releaseAnimationPlayer();
    }

    private void reset(ModelInstance modelInstance) {
        if (modelInstance == null) {
            return;
        }
        for (ModelInstance modelInstance2 : new ArrayList<ModelInstance>(modelInstance.sub)) {
            this.reset(modelInstance2);
        }
        ModelManager.instance.resetModelInstance(modelInstance, (Object)this.model);
    }
}

