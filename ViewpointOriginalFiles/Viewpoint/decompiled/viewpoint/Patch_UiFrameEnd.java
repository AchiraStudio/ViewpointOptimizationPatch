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

@Patch(className="org.lwjglx.opengl.Display", methodName="imguiEndFrame")
public class Patch_UiFrameEnd {
    @Patch.OnExit
    public static void exit() {
        Hooks.uiFrameEnding();
    }
}

