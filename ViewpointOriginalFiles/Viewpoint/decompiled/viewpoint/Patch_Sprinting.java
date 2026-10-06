/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$Return
 *  me.zed_0xff.zombie_buddy.Patch$This
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.characters.IsoGameCharacter;

@Patch(className="zombie.characters.IsoGameCharacter", methodName="isSprinting")
public class Patch_Sprinting {
    @Patch.OnExit
    public static void exit(@Patch.This IsoGameCharacter isoGameCharacter, @Patch.Return(readOnly=false) boolean bl) {
        bl = Hooks.sprinting(isoGameCharacter, bl);
    }
}

