/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$This
 *  zombie.iso.objects.IsoWindow
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.iso.objects.IsoWindow;

@Patch(className="zombie.iso.objects.IsoWindow", methodName="ToggleWindow")
public class Patch_WindowToggle {
    @Patch.OnExit
    public static void exit(@Patch.This IsoWindow isoWindow) {
        Hooks.windowToggled(isoWindow);
    }
}

