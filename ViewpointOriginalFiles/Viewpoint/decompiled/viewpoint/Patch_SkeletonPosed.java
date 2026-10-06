/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$This
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.core.skinnedmodel.animation.AnimationPlayer;

@Patch(className="zombie.core.skinnedmodel.animation.AnimationPlayer", methodName="updateModelTransforms")
public class Patch_SkeletonPosed {
    @Patch.OnExit
    public static void exit(@Patch.This AnimationPlayer animationPlayer) {
        Hooks.skeletonPosed(animationPlayer);
    }
}

