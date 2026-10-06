/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$Argument
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$Return
 *  me.zed_0xff.zombie_buddy.Patch$This
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.characters.IsoZombie;

@Patch(className="zombie.characters.IsoZombie", methodName="getScreenProperX")
public class Patch_ZombieScreenX {
    @Patch.OnExit
    public static void exit(@Patch.This IsoZombie isoZombie, @Patch.Argument(value=0) int n, @Patch.Return(readOnly=false) int n2) {
        n2 = Hooks.zombieScreenX(isoZombie, n, n2);
    }
}

