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

@Patch(className="zombie.input.KeyboardState", methodName="isKeyDown")
public class Patch_KeyDown {
    @Patch.OnExit
    public static void exit(@Patch.Argument(value=0) int n, @Patch.Return(readOnly=false) boolean bl) {
        bl = Hooks.keyDown(n, bl);
    }
}

