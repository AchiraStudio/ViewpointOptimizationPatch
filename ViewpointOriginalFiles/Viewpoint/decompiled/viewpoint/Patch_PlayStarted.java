/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;

@Patch(className="zombie.gameStates.IngameState", methodName="enter")
public class Patch_PlayStarted {
    @Patch.OnEnter
    public static void enter() {
        Hooks.playStarting();
    }

    @Patch.OnExit
    public static void exit() {
        Hooks.playStarted();
    }
}

