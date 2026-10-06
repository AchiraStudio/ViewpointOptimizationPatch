/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$Argument
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$Return
 *  zombie.iso.IsoObjectPicker$ClickObject
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.iso.IsoObjectPicker;

@Patch(className="zombie.iso.IsoObjectPicker", methodName="ContextPick")
public class Patch_ContextPick {
    @Patch.OnExit
    public static void exit(@Patch.Argument(value=0) int n, @Patch.Argument(value=1) int n2, @Patch.Return(readOnly=false) IsoObjectPicker.ClickObject clickObject) {
        clickObject = Hooks.contextPick(n, n2, clickObject);
    }
}

