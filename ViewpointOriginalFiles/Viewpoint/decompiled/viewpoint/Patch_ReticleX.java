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

@Patch(className="zombie.input.AimingReticle", methodName="getXA")
public class Patch_ReticleX {
    @Patch.OnExit
    public static void exit(@Patch.Argument(value=0) int n, @Patch.Return(readOnly=false) int n2) {
        n2 = Hooks.reticleX(n, n2);
    }
}

