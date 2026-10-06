/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$Argument
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;

@Patch(className="zombie.iso.weather.ThunderStorm", methodName="enqueueThunderEvent")
public class Patch_Thunder {
    @Patch.OnEnter
    public static void enter(@Patch.Argument(value=0) int n, @Patch.Argument(value=1) int n2, @Patch.Argument(value=3) boolean bl) {
        Hooks.thunder(n, n2, bl);
    }
}

