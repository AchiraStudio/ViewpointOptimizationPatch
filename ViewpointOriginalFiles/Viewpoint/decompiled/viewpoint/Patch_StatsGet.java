/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$Argument
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$Return
 *  me.zed_0xff.zombie_buddy.Patch$This
 *  zombie.characters.CharacterStat
 *  zombie.characters.Stats
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.characters.CharacterStat;
import zombie.characters.Stats;

@Patch(className="zombie.characters.Stats", methodName="get")
public class Patch_StatsGet {
    @Patch.OnEnter(skipOn=true)
    public static boolean enter() {
        return Hooks.skipStatsGet();
    }

    @Patch.OnExit
    public static void exit(@Patch.This Stats stats, @Patch.Argument(value=0) CharacterStat characterStat, @Patch.Return(readOnly=false) float f) {
        f = Hooks.statsGet(stats, characterStat, f);
    }
}

