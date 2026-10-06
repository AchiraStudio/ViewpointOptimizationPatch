/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$This
 *  zombie.iso.sprite.IsoReticle
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.iso.sprite.IsoReticle;

@Patch(className="zombie.iso.sprite.IsoReticle", methodName="render")
public class Patch_IsoReticle {
    @Patch.OnEnter(skipOn=true)
    public static boolean enter(@Patch.This IsoReticle isoReticle) {
        return Hooks.skipIsoReticle(isoReticle);
    }

    @Patch.OnExit
    public static void exit() {
        Hooks.isoReticleDone();
    }
}

