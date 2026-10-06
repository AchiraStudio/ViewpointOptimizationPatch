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
import zombie.characters.IsoPlayer;

@Patch(className="zombie.characters.IsoPlayer", methodName="setAngleFromAim")
public class Patch_AimFromReticle {
    @Patch.OnExit
    public static void exit(@Patch.This IsoPlayer isoPlayer) {
        Hooks.aimedFromReticle(isoPlayer);
    }
}

