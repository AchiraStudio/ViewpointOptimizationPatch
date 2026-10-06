/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;

@Patch(className="zombie.gameStates.GameLoadingState", methodName="enter")
public class Patch_GameLoaded {
    @Patch.OnExit
    public static void exit() {
        Hooks.gameLoaded();
    }
}

