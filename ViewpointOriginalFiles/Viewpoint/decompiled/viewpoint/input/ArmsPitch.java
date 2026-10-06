/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.util.vector.Matrix4f
 *  zombie.core.skinnedmodel.model.SkinningBone
 *  zombie.core.skinnedmodel.model.SkinningData
 *  zombie.inventory.types.HandWeapon
 *  zombie.iso.Vector3
 */
package viewpoint.input;

import java.util.Arrays;
import org.lwjgl.util.vector.Matrix4f;
import viewpoint.input.Controls;
import viewpoint.input.CrosshairAim;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.inventory.types.HandWeapon;
import zombie.iso.Vector3;

public final class ArmsPitch {
    private static final float STEP = 0.08f;
    private static final float MOST = 1.5707964f;
    private static final String[] ROOTS = new String[]{"Bip01_L_Clavicle", "Bip01_R_Clavicle", "Bip01_Prop1", "Bip01_Prop2"};
    private static final Matrix4f turn = new Matrix4f();
    private static final Matrix4f parentInverse = new Matrix4f();
    private static final Vector3 across = new Vector3();
    private static final Vector3 along = new Vector3();
    private static final float[] facing = new float[2];
    private static float pitch;
    private static SkinningData bound;
    private static int leftClavicle;
    private static int rightClavicle;
    private static int[] turned;
    private static int[] tops;
    private static int[] topParents;

    public static void posed(AnimationPlayer animationPlayer) {
        GameTime gameTime;
        IsoPlayer isoPlayer = IsoPlayer.players[0];
        if (isoPlayer == null || animationPlayer.getIsoGameCharacter() != isoPlayer) {
            return;
        }
        if (!Controls.owns((IsoGameCharacter)((Object)isoPlayer))) {
            pitch = 0.0f;
            return;
        }
        float f = ArmsPitch.aimsFirearm(isoPlayer) ? ArmsPitch.clamp(CrosshairAim.pitch((IsoGameCharacter)((Object)isoPlayer)), 1.5707964f) : 0.0f;
        if ((pitch += ArmsPitch.clamp(f - pitch, 0.08f * (gameTime = GameTime.getInstance()).getMultiplierFromTimeDelta(gameTime.getTimeDelta()))) != 0.0f && ArmsPitch.bind(animationPlayer)) {
            ArmsPitch.turn(animationPlayer, isoPlayer);
        }
    }

    static float pitch() {
        return pitch;
    }

    static void pivot(float f, float f2, float f3, float f4, float f5, float f6, Matrix4f matrix4f) {
        float f7 = -f2;
        float f8 = f;
        float f9 = (float)Math.cos(f6);
        float f10 = (float)Math.sin(f6);
        float f11 = 1.0f - f9;
        matrix4f.m00 = f9 + f11 * f7 * f7;
        matrix4f.m01 = -f10 * f8;
        matrix4f.m02 = f11 * f7 * f8;
        matrix4f.m10 = f10 * f8;
        matrix4f.m11 = f9;
        matrix4f.m12 = -f10 * f7;
        matrix4f.m20 = f11 * f7 * f8;
        matrix4f.m21 = f10 * f7;
        matrix4f.m22 = f9 + f11 * f8 * f8;
        matrix4f.m30 = 0.0f;
        matrix4f.m31 = 0.0f;
        matrix4f.m32 = 0.0f;
        matrix4f.m33 = 1.0f;
        matrix4f.m03 = f3 - (matrix4f.m00 * f3 + matrix4f.m01 * f4 + matrix4f.m02 * f5);
        matrix4f.m13 = f4 - (matrix4f.m10 * f3 + matrix4f.m11 * f4 + matrix4f.m12 * f5);
        matrix4f.m23 = f5 - (matrix4f.m20 * f3 + matrix4f.m21 * f4 + matrix4f.m22 * f5);
    }

    static void forward(float f, float f2, float f3, float[] fArray) {
        across.set(1.0f, 0.0f, 0.0f);
        Model.vectorToWorldCoords(0.0f, 0.0f, 0.0f, f3, across);
        along.set(0.0f, 0.0f, 1.0f);
        Model.vectorToWorldCoords(0.0f, 0.0f, 0.0f, f3, along);
        float f4 = f * ArmsPitch.across.x + f2 * ArmsPitch.across.y;
        float f5 = f * ArmsPitch.along.x + f2 * ArmsPitch.along.y;
        float f6 = (float)Math.hypot(f4, f5);
        fArray[0] = f4 / f6;
        fArray[1] = f5 / f6;
    }

    private static boolean aimsFirearm(IsoPlayer isoPlayer) {
        HandWeapon handWeapon = isoPlayer.getUseHandWeapon();
        return isoPlayer.isAiming() && handWeapon != null && handWeapon.isAimedFirearm();
    }

    private static void turn(AnimationPlayer animationPlayer, IsoPlayer isoPlayer) {
        Matrix4f matrix4f = animationPlayer.getModelTransformAt(leftClavicle);
        Matrix4f matrix4f2 = animationPlayer.getModelTransformAt(rightClavicle);
        ArmsPitch.forward(isoPlayer.getForwardDirectionX(), isoPlayer.getForwardDirectionY(), animationPlayer.getRenderedAngle(), facing);
        ArmsPitch.pivot(facing[0], facing[1], (matrix4f.m03 + matrix4f2.m03) * 0.5f, (matrix4f.m13 + matrix4f2.m13) * 0.5f, (matrix4f.m23 + matrix4f2.m23) * 0.5f, pitch, turn);
        for (int n : turned) {
            Matrix4f matrix4f3 = animationPlayer.getModelTransformAt(n);
            Matrix4f.mul((Matrix4f)matrix4f3, (Matrix4f)turn, (Matrix4f)matrix4f3);
        }
        for (int i = 0; i < tops.length; ++i) {
            Matrix4f.invert((Matrix4f)animationPlayer.getModelTransformAt(topParents[i]), (Matrix4f)parentInverse);
            animationPlayer.boneTransforms[tops[i]].mul(animationPlayer.getModelTransformAt(tops[i]), parentInverse);
        }
    }

    private static boolean bind(AnimationPlayer animationPlayer) {
        SkinningData skinningData = animationPlayer.getSkinningData();
        if (skinningData == bound) {
            return turned.length > 0;
        }
        bound = skinningData;
        topParents = new int[0];
        tops = topParents;
        turned = topParents;
        int[] nArray = new int[ROOTS.length];
        for (int i = 0; i < ROOTS.length; ++i) {
            nArray[i] = animationPlayer.getSkinningBoneIndex(ROOTS[i], -1);
        }
        leftClavicle = nArray[0];
        rightClavicle = nArray[1];
        if (skinningData == null || leftClavicle < 0 || rightClavicle < 0) {
            return false;
        }
        int[] nArray2 = new int[skinningData.numBones()];
        int n = 0;
        for (int i = 0; i < nArray2.length; ++i) {
            if (!ArmsPitch.hangsFrom(skinningData.getBoneAt(i), nArray)) continue;
            nArray2[n++] = i;
        }
        turned = Arrays.copyOf(nArray2, n);
        int[] nArray3 = new int[nArray.length];
        int[] nArray4 = new int[nArray.length];
        int n2 = 0;
        for (int n3 : nArray) {
            SkinningBone skinningBone;
            SkinningBone skinningBone2 = skinningBone = n3 < 0 ? null : skinningData.getBoneAt((int)n3).parent;
            if (skinningBone == null || ArmsPitch.hangsFrom(skinningBone, nArray)) continue;
            nArray3[n2] = n3;
            nArray4[n2++] = skinningBone.index;
        }
        tops = Arrays.copyOf(nArray3, n2);
        topParents = Arrays.copyOf(nArray4, n2);
        return n > 0;
    }

    private static boolean hangsFrom(SkinningBone skinningBone, int[] nArray) {
        SkinningBone skinningBone2 = skinningBone;
        while (skinningBone2 != null) {
            for (int n : nArray) {
                if (skinningBone2.index != n) continue;
                return true;
            }
            skinningBone2 = skinningBone2.parent;
        }
        return false;
    }

    private static float clamp(float f, float f2) {
        return Math.max(-f2, Math.min(f2, f));
    }

    private ArmsPitch() {
    }

    static {
        leftClavicle = -1;
        rightClavicle = -1;
        turned = new int[0];
        tops = new int[0];
        topParents = new int[0];
    }
}

