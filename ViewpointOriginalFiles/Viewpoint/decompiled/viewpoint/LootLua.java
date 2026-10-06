/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Exposer$LuaClass
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Exposer;
import viewpoint.interact.LootMenu;

@Exposer.LuaClass(name="Viewpoint.Loot")
public final class LootLua {
    public static void setEnabled(boolean bl) {
        LootMenu.setEnabled(bl);
    }

    private LootLua() {
    }
}

