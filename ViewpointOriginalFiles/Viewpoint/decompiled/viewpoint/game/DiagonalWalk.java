/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.skinnedmodel.animation.AnimationClip
 *  zombie.core.skinnedmodel.animation.Keyframe
 */
package viewpoint.game;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.Keyframe;

public final class DiagonalWalk {
    private static final String RIGHT = "Bob_WalkNoAim1Hand_DiagR";
    private static final String LEFT = "Bob_WalkNoAim1Hand_DiagL";
    private static final String ROOT = "Translation_Data";
    private static AnimationClip fixed;

    public static void fix() {
        AnimationClip animationClip = DiagonalWalk.clip(RIGHT);
        AnimationClip animationClip2 = DiagonalWalk.clip(LEFT);
        if (animationClip == fixed) {
            return;
        }
        fixed = animationClip;
        if (Math.signum(DiagonalWalk.sideways(animationClip.getKeyframes())) != Math.signum(DiagonalWalk.sideways(animationClip2.getKeyframes()))) {
            System.out.println("[Viewpoint] Bob_WalkNoAim1Hand_DiagR already goes right: the game's clip needs no fix");
            return;
        }
        DiagonalWalk.turnOver(animationClip.getKeyframes());
        System.out.println("[Viewpoint] Bob_WalkNoAim1Hand_DiagR: its root motion turned to the right");
    }

    private static AnimationClip clip(String string) {
        AnimationClip animationClip = ModelManager.instance.getAnimationClip(string);
        if (animationClip == null) {
            throw new IllegalStateException("the game has no animation clip " + string);
        }
        return animationClip;
    }

    static float sideways(Keyframe[] keyframeArray) {
        float f = Float.NaN;
        float f2 = Float.NaN;
        for (Keyframe keyframe : keyframeArray) {
            if (!ROOT.equals(keyframe.boneName)) continue;
            f = Float.isNaN(f) ? keyframe.position.x : f;
            f2 = keyframe.position.x;
        }
        if (Float.isNaN(f)) {
            throw new IllegalStateException("an animation clip without its Translation_Data keys");
        }
        return f2 - f;
    }

    static void turnOver(Keyframe[] keyframeArray) {
        Set set = Collections.newSetFromMap(new IdentityHashMap());
        for (Keyframe keyframe : keyframeArray) {
            if (!ROOT.equals(keyframe.boneName) || !set.add(keyframe.position)) continue;
            keyframe.position.x = -keyframe.position.x;
        }
    }

    private DiagonalWalk() {
    }
}

