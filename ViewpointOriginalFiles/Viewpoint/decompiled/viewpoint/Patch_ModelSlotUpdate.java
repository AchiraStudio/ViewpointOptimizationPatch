/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$This
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.core.skinnedmodel.ModelManager;

@Patch(className="zombie.core.skinnedmodel.ModelManager$ModelSlot", methodName="Update")
public class Patch_ModelSlotUpdate {
    @Patch.OnEnter
    public static void enter(@Patch.This ModelManager.ModelSlot modelSlot) {
        Hooks.modelSlotUpdating(modelSlot);
    }

    @Patch.OnExit
    public static void exit() {
        Hooks.modelSlotUpdated();
    }
}

