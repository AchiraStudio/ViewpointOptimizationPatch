/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Exposer$LuaClass
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Exposer;
import viewpoint.platform.KeyInput;
import viewpoint.platform.Keys;

@Exposer.LuaClass(name="Viewpoint.Keys")
public final class KeysLua {
    public static int count() {
        return Keys.optionCount();
    }

    public static String id(int n) {
        return Keys.option(n);
    }

    public static String label(String string) {
        return Keys.label(string);
    }

    public static String tooltip(String string) {
        return Keys.tooltip(string);
    }

    public static String group(String string) {
        return Keys.group(string);
    }

    public static boolean loot(String string) {
        return Keys.loot(string);
    }

    public static String get(String string) {
        return Keys.text(string);
    }

    public static String fallback(String string) {
        return Keys.fallbackText(string);
    }

    public static String display(String string) {
        return Keys.display(string);
    }

    public static int trigger(String string) {
        return Keys.trigger(string);
    }

    public static boolean holds(String string, String string2) {
        return Keys.holds(string, string2);
    }

    public static String captured(int n) {
        return KeyInput.captured(n);
    }

    public static String without(String string, String string2, String string3, String string4) {
        return Keys.without(string, string2, string3, string4);
    }

    public static void set(String string, String string2) {
        Keys.set(string, string2);
    }

    private KeysLua() {
    }
}

