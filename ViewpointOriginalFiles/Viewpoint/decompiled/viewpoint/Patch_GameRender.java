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

@Patch(className="zombie.core.SpriteRenderer", methodName="postRender")
public class Patch_GameRender {
    @Patch.OnEnter
    public static void enter() {
        Hooks.gameRenderStarted();
    }

    @Patch.OnExit
    public static void exit() {
        Hooks.gameRenderEnded();
    }
}

