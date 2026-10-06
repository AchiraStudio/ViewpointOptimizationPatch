/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$Return
 *  zombie.vehicles.BaseVehicle
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.vehicles.BaseVehicle;

@Patch(className="zombie.iso.IsoObjectPicker", methodName="PickVehicle")
public class Patch_PickVehicle {
    @Patch.OnExit
    public static void exit(@Patch.Return(readOnly=false) BaseVehicle baseVehicle) {
        baseVehicle = Hooks.pickVehicle(baseVehicle);
    }
}

