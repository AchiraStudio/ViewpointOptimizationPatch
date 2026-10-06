/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$Return
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;

@Patch(className="zombie.core.PerformanceSettings", methodName="isFramerateUncapped")
public class Patch_FramerateUncapped {
    @Patch.OnExit
    public static void exit(@Patch.Return(readOnly=false) boolean bl) {
        bl = Hooks.framerateUncapped(bl);
    }
}

