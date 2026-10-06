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

@Patch(className="zombie.iso.LightingJNI", methodName="update")
public class Patch_LightingUpdate {
    @Patch.OnEnter
    public static void enter() {
        Hooks.lightingUpdateStarted();
    }

    @Patch.OnExit
    public static void exit() {
        Hooks.lightingUpdateEnded();
    }
}

