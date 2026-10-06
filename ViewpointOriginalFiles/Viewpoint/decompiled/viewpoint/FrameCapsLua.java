/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Exposer$LuaClass
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Exposer;
import viewpoint.game.FrameCaps;

@Exposer.LuaClass(name="Viewpoint.FrameCaps")
public final class FrameCapsLua {
    public static int getMenuFramerateIndex() {
        return FrameCaps.getMenuFramerateIndex();
    }

    public static void setMenuFramerateIndex(int n) {
        FrameCaps.setMenuFramerateIndex(n);
    }

    public static int getMenuFramerateChoices() {
        return FrameCaps.getMenuFramerateChoices();
    }

    public static void setGameFramerate(int n) {
        FrameCaps.setGameFramerate(n);
    }

    public static int getGameFramerate() {
        return FrameCaps.getGameFramerate();
    }

    public static boolean isFramerateUncapped() {
        return FrameCaps.isFramerateUncapped();
    }

    private FrameCapsLua() {
    }
}

