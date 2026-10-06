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

@Patch(className="zombie.core.PerformanceSettings", methodName="getLockFPS")
public class Patch_LockFps {
    @Patch.OnExit
    public static void exit(@Patch.Return(readOnly=false) int n) {
        n = Hooks.lockFps(n);
    }
}

