/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Exposer$LuaClass
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Exposer;
import viewpoint.interact.MouseTargets;

@Exposer.LuaClass(name="Viewpoint.Mouse")
public final class MouseLua {
    public static Double worldX() {
        return MouseTargets.on() ? MouseTargets.world(true) : null;
    }

    public static Double worldY() {
        return MouseTargets.on() ? MouseTargets.world(false) : null;
    }

    private MouseLua() {
    }
}

