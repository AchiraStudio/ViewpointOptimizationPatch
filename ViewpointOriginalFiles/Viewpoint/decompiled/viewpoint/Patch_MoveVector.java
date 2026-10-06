/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$Argument
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$This
 *  zombie.iso.Vector2
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.characters.IsoPlayer;
import zombie.iso.Vector2;

@Patch(className="zombie.characters.IsoPlayer", methodName="getInputMoveVector")
public class Patch_MoveVector {
    @Patch.OnExit
    public static void exit(@Patch.This IsoPlayer isoPlayer, @Patch.Argument(value=0) Vector2 vector2) {
        Hooks.inputMoveVector(isoPlayer, vector2);
    }
}

