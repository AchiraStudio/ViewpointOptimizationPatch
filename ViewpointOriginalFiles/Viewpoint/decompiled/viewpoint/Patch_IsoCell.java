/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 *  me.zed_0xff.zombie_buddy.Patch$This
 *  zombie.iso.IsoCell
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.iso.IsoCell;

@Patch(className="zombie.iso.IsoCell", methodName="render")
public class Patch_IsoCell {
    @Patch.OnEnter(skipOn=true)
    public static boolean enter(@Patch.This IsoCell isoCell) {
        return Hooks.skipIsoCellRender(isoCell);
    }
}

