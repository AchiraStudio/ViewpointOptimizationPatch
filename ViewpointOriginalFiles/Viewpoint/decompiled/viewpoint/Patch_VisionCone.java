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
import zombie.characters.IsoGameCharacter;

@Patch(className="zombie.iso.LightingJNI", methodName="calculateVisionCone")
public class Patch_VisionCone {
    @Patch.OnExit
    public static void exit(@Patch.Argument(value=0) IsoGameCharacter isoGameCharacter, @Patch.Return(readOnly=false) float f) {
        f = Hooks.visionCone(isoGameCharacter, f);
    }
}

