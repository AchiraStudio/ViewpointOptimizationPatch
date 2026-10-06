/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$Argument
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$This
 *  zombie.iso.objects.IsoWindow
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.iso.objects.IsoWindow;

@Patch(className="zombie.iso.objects.IsoWindow", methodName="smashWindow")
public class Patch_WindowSmash {
    @Patch.OnExit
    public static void exit(@Patch.This IsoWindow isoWindow, @Patch.Argument(value=0) boolean bl, @Patch.Argument(value=1) boolean bl2) {
        Hooks.windowSmashed(isoWindow);
    }
}

