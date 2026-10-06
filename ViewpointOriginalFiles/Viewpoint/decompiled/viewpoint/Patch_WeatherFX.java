/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;

@Patch(className="zombie.iso.IsoWorld", methodName="renderWeatherFX")
public class Patch_WeatherFX {
    @Patch.OnEnter(skipOn=true)
    public static boolean enter() {
        return Hooks.skipWeatherFx();
    }
}

