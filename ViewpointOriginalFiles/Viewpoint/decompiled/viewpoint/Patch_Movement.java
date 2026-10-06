/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$This
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.characters.IsoPlayer;

@Patch(className="zombie.characters.IsoPlayer", methodName="updateMovementFromInput")
public class Patch_Movement {
    @Patch.OnEnter
    public static void enter(@Patch.This IsoPlayer isoPlayer) {
        Hooks.movementStarting(isoPlayer);
    }

    @Patch.OnExit
    public static void exit(@Patch.This IsoPlayer isoPlayer) {
        Hooks.movementDone(isoPlayer);
    }
}

