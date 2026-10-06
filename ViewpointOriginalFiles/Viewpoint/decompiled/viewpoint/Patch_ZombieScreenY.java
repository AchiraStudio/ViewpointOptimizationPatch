/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$Argument
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$Return
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;

@Patch(className="zombie.characters.IsoZombie", methodName="getScreenProperY")
public class Patch_ZombieScreenY {
    @Patch.OnExit
    public static void exit(@Patch.Argument(value=0) int n, @Patch.Return(readOnly=false) int n2) {
        n2 = Hooks.zombieScreenY(n, n2);
    }
}

