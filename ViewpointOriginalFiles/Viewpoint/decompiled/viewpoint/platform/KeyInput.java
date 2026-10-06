/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;
import viewpoint.platform.KeyBind;
import viewpoint.platform.KeyChord;
import viewpoint.platform.Keys;
import viewpoint.platform.Settings;
import viewpoint.platform.SettingsWindow;
import zombie.input.GameKeyboard;

public final class KeyInput {
    private static final boolean[] raw = new boolean[256];
    private static final boolean[] hidden = new boolean[256];
    private static final boolean[] fresh = new boolean[256];
    private static final boolean[] passing = new boolean[256];
    private static int passingFor = -1;
    private static volatile KeyBind target;
    private static volatile boolean appending;

    public static boolean keyDown(int n, boolean bl) {
        if (n <= 0 || n >= raw.length) {
            return bl;
        }
        boolean bl2 = raw[n];
        KeyInput.raw[n] = bl;
        KeyBind keyBind = target;
        if (keyBind != null && bl && !bl2) {
            KeyInput.fresh[n] = true;
        } else if (keyBind != null && !bl && bl2 && fresh[n]) {
            KeyInput.take(keyBind, n);
        }
        int n2 = n;
        fresh[n2] = fresh[n2] & bl;
        KeyInput.hidden[n] = bl && (hidden[n] || keyBind != null);
        return bl && !hidden[n];
    }

    static boolean pressed(KeyBind keyBind) {
        if (keyBind.kind != KeyBind.Kind.PRESS || target != null || !GameKeyboard.doLuaKeyPressed) {
            return false;
        }
        KeyChord keyChord = KeyInput.fires(keyBind, Keys.all(), Settings.dev, GameKeyboard::isKeyPressed, GameKeyboard::isKeyDown);
        if (keyChord == null) {
            return false;
        }
        GameKeyboard.eatKeyPress(keyChord.key());
        return true;
    }

    public static KeyBind menu(int n) {
        if (target != null) {
            return null;
        }
        for (KeyBind keyBind : Keys.all()) {
            if (keyBind.kind != KeyBind.Kind.MENU || KeyInput.fires(keyBind, Keys.all(), Settings.dev, n2 -> n2 == n, KeyInput::isDown) == null) continue;
            return keyBind;
        }
        return null;
    }

    static KeyChord fires(KeyBind keyBind, List<KeyBind> list, boolean bl, IntPredicate intPredicate, IntPredicate intPredicate2) {
        if (keyBind.dev && !bl) {
            return null;
        }
        for (KeyChord keyChord : keyBind.chords()) {
            if (!intPredicate.test(keyChord.key()) || !keyChord.heldDown(intPredicate2) || KeyInput.outranked(keyChord, keyBind.kind, list, bl, intPredicate2)) continue;
            return keyChord;
        }
        return null;
    }

    private static boolean outranked(KeyChord keyChord, KeyBind.Kind kind, List<KeyBind> list, boolean bl, IntPredicate intPredicate) {
        for (KeyBind keyBind : list) {
            if (keyBind.kind != kind || keyBind.dev && !bl) continue;
            for (KeyChord keyChord2 : keyBind.chords()) {
                if (!keyChord2.outranks(keyChord) || !keyChord2.heldDown(intPredicate)) continue;
                return true;
            }
        }
        return false;
    }

    public static boolean reachesGameFlying(int n) {
        if (n <= 0 || n >= passing.length) {
            return false;
        }
        if (passingFor != KeyBind.changes()) {
            KeyInput.passes();
        }
        return passing[n];
    }

    private static void passes() {
        passingFor = KeyBind.changes();
        Arrays.fill(passing, false);
        KeyInput.passing[1] = true;
        for (KeyBind keyBind : Keys.all()) {
            if (keyBind.kind != KeyBind.Kind.PRESS) continue;
            for (KeyChord keyChord : keyBind.chords()) {
                KeyInput.passing[keyChord.key()] = true;
                for (int n : keyChord.held()) {
                    KeyInput.passing[n] = true;
                    KeyInput.passing[KeyChord.otherSide((int)n)] = true;
                }
            }
        }
        KeyInput.passing[0] = false;
    }

    public static String captured(int n) {
        return n > 0 && n < raw.length ? KeyInput.heldWith(n).text() : "";
    }

    private static void take(KeyBind keyBind, int n) {
        KeyChord keyChord;
        target = null;
        Arrays.fill(fresh, false);
        if (n == 1) {
            System.out.println("[Viewpoint] key " + keyBind.key + " left as it was");
            return;
        }
        ArrayList<KeyChord> arrayList = new ArrayList<KeyChord>(appending ? keyBind.chords() : List.of());
        if (!arrayList.contains(keyChord = KeyInput.heldWith(n))) {
            arrayList.add(keyChord);
        }
        Keys.choose(keyBind, arrayList);
        SettingsWindow.edited();
    }

    private static KeyChord heldWith(int n) {
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (int i = 1; i < raw.length; ++i) {
            if (i == n || !raw[i]) continue;
            arrayList.add(i);
        }
        return KeyChord.of(n, arrayList);
    }

    private static boolean isDown(int n) {
        return n > 0 && n < raw.length && raw[n];
    }

    static void capture(KeyBind keyBind, boolean bl) {
        appending = bl;
        target = keyBind;
    }

    static void cancel() {
        target = null;
    }

    static KeyBind capturing() {
        return target;
    }

    private KeyInput() {
    }
}

