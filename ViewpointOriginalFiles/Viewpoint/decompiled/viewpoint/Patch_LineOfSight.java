/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 *  me.zed_0xff.zombie_buddy.Patch$This
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.characters.IsoPlayer;

@Patch(className="zombie.characters.IsoPlayer", methodName="updateLOS")
public class Patch_LineOfSight {
    @Patch.OnEnter
    public static void enter(@Patch.This IsoPlayer isoPlayer) {
        Hooks.lineOfSightStarting(isoPlayer);
    }
}

