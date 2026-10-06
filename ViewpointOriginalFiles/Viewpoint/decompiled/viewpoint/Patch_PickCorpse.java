/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$Return
 *  zombie.iso.IsoObject
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.iso.IsoObject;

@Patch(className="zombie.iso.IsoObjectPicker", methodName="PickCorpse")
public class Patch_PickCorpse {
    @Patch.OnExit
    public static void exit(@Patch.Return(readOnly=false) IsoObject isoObject) {
        isoObject = Hooks.pickCorpse(isoObject);
    }
}

