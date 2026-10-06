/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 *  me.zed_0xff.zombie_buddy.Patch$This
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.iso.IsoWorld;

@Patch(className="zombie.iso.IsoWorld", methodName="sceneCullAnimals")
public class Patch_CullAnimals {
    @Patch.OnEnter(skipOn=true)
    public static boolean enter(@Patch.This IsoWorld isoWorld) {
        return Hooks.skipAnimalCull(isoWorld);
    }
}

