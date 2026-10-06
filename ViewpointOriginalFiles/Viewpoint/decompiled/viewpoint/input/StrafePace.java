/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator
 *  zombie.core.skinnedmodel.advancedanimation.AnimLayer
 *  zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource
 *  zombie.core.skinnedmodel.advancedanimation.LiveAnimNode
 */
package viewpoint.input;

import java.util.List;
import viewpoint.input.Stride;
import viewpoint.platform.Build;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;

final class StrafePace {
    private static final String WALK_STATE = "movement";
    private static final String STRAFE_STATE = "strafe";
    private static final float ALONE = 0.99f;
    private final Stride stride = new Stride();
    private float walkSeen;
    private float strafeSeen;

    StrafePace() {
    }

    void update(IsoPlayer isoPlayer, float f, float f2, boolean bl, boolean bl2, float f3) {
        LiveAnimNode liveAnimNode;
        AnimLayer animLayer;
        AdvancedAnimator advancedAnimator = isoPlayer.getAdvancedAnimator();
        AnimLayer animLayer2 = animLayer = advancedAnimator == null ? null : advancedAnimator.getRootLayer();
        if (animLayer == null || f2 <= 0.0f) {
            return;
        }
        int n = isoPlayer.isSneaking() ? 1 : 0;
        float f4 = f / f2;
        LiveAnimNode liveAnimNode2 = liveAnimNode = bl ? StrafePace.strongest(animLayer, WALK_STATE) : null;
        if (liveAnimNode != null && StrafePace.alone(liveAnimNode)) {
            this.stride.walked(n, f4, f2);
            this.walkSeen = f4;
        }
        LiveAnimNode liveAnimNode3 = StrafePace.strongest(animLayer, STRAFE_STATE);
        if (!bl2 || liveAnimNode3 == null) {
            if (this.stride.factor() != 1.0f) {
                this.report();
                this.stride.stop();
                StrafePace.pace(isoPlayer, animLayer, 1.0f);
            }
            return;
        }
        float f5 = this.stride.factor();
        if (StrafePace.alone(liveAnimNode3)) {
            float f6 = liveAnimNode3.getSourceNode().getSpeedScale((IAnimationVariableSource)isoPlayer);
            float f7 = f6 > 0.0f ? liveAnimNode3.getMainAnimationTrackAt(0).getSpeedDelta() / f6 : 1.0f;
            f5 = this.stride.strafed(n, f4, f7, f2, f3);
            this.strafeSeen = f4;
        }
        StrafePace.pace(isoPlayer, animLayer, f5);
    }

    private void report() {
        if (!Build.RELEASE) {
            System.out.printf("[Viewpoint] strafe pace: played at %.2f, strafing %.2f squares/s, walking %.2f%n", Float.valueOf(this.stride.factor()), Float.valueOf(this.strafeSeen), Float.valueOf(this.walkSeen));
        }
    }

    private static void pace(IsoPlayer isoPlayer, AnimLayer animLayer, float f) {
        List list = animLayer.getLiveAnimNodes();
        for (int i = 0; i < list.size(); ++i) {
            LiveAnimNode liveAnimNode = (LiveAnimNode)list.get(i);
            if (!StrafePace.in(liveAnimNode, STRAFE_STATE)) continue;
            float f2 = liveAnimNode.getSourceNode().getSpeedScale((IAnimationVariableSource)isoPlayer) * f;
            for (int j = 0; j < liveAnimNode.getMainAnimationTracksCount(); ++j) {
                liveAnimNode.getMainAnimationTrackAt(j).setSpeedDelta(f2);
            }
        }
    }

    private static boolean alone(LiveAnimNode liveAnimNode) {
        return liveAnimNode.getWeight() >= 0.99f && liveAnimNode.getMainAnimationTracksCount() > 0;
    }

    private static LiveAnimNode strongest(AnimLayer animLayer, String string) {
        List list = animLayer.getLiveAnimNodes();
        LiveAnimNode liveAnimNode = null;
        for (int i = 0; i < list.size(); ++i) {
            LiveAnimNode liveAnimNode2 = (LiveAnimNode)list.get(i);
            if (!liveAnimNode2.isActive() || !StrafePace.in(liveAnimNode2, string) || liveAnimNode != null && !(liveAnimNode2.getWeight() > liveAnimNode.getWeight())) continue;
            liveAnimNode = liveAnimNode2;
        }
        return liveAnimNode;
    }

    private static boolean in(LiveAnimNode liveAnimNode, String string) {
        return liveAnimNode.getSourceNode() != null && liveAnimNode.getSourceNode().parentState != null && string.equals(liveAnimNode.getSourceNode().parentState.name);
    }
}

