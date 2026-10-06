/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$Return
 *  me.zed_0xff.zombie_buddy.Patch$This
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.iso.objects.IsoLightSwitch;

@Patch(className="zombie.iso.objects.IsoLightSwitch", methodName="hasElectricityAround")
public class Patch_SwitchPower {
    @Patch.OnEnter(skipOn=true)
    public static boolean enter(@Patch.This IsoLightSwitch isoLightSwitch) {
        return Hooks.skipSwitchPower(isoLightSwitch);
    }

    @Patch.OnExit
    public static void exit(@Patch.This IsoLightSwitch isoLightSwitch, @Patch.Return(readOnly=false) boolean bl) {
        bl = Hooks.switchPower(isoLightSwitch, bl);
    }
}

