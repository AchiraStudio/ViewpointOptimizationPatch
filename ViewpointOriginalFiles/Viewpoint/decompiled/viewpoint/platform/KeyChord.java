/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjglx.input.Keyboard
 */
package viewpoint.platform;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import org.lwjglx.input.Keyboard;

public record KeyChord(int key, List<Integer> held) {
    static final int ESCAPE = 1;
    static final int LEFT_SHIFT = 42;
    static final int RIGHT_SHIFT = 54;
    static final int LEFT_CTRL = 29;
    static final int RIGHT_CTRL = 157;
    static final int LEFT_ALT = 56;
    static final int RIGHT_ALT = 184;
    private static final String NONE = "NONE";
    private static final String[] NAMES = new String[256];
    private static final Map<String, Integer> CODES = new HashMap<String, Integer>();
    private static final Comparator<Integer> ORDER;

    public KeyChord {
        list = List.copyOf(list);
    }

    static KeyChord of(int n, List<Integer> list) {
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (int n2 : list) {
            int n3 = KeyChord.canonical(n2);
            if (n3 == KeyChord.canonical(n) || arrayList.contains(n3)) continue;
            arrayList.add(n3);
        }
        arrayList.sort(ORDER);
        return new KeyChord(n, arrayList);
    }

    public boolean heldDown(IntPredicate intPredicate) {
        for (int n : this.held) {
            if (KeyChord.down(n, intPredicate)) continue;
            return false;
        }
        return true;
    }

    public List<Integer> keys() {
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        arrayList.add(this.key);
        for (int n : this.held) {
            arrayList.add(n);
            if (KeyChord.otherSide(n) == 0) continue;
            arrayList.add(KeyChord.otherSide(n));
        }
        return arrayList;
    }

    boolean outranks(KeyChord keyChord) {
        return this.key == keyChord.key && this.held.size() > keyChord.held.size() && this.held.containsAll(keyChord.held);
    }

    String text() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int n : this.held) {
            stringBuilder.append(KeyChord.heldName(n)).append('+');
        }
        return stringBuilder.append(KeyChord.name(this.key)).toString();
    }

    String display(IntFunction<String> intFunction) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int n : this.held) {
            stringBuilder.append(KeyChord.modifier(n) ? KeyChord.heldName(n) : intFunction.apply(n)).append(" + ");
        }
        return stringBuilder.append(intFunction.apply(this.key)).toString();
    }

    static String text(List<KeyChord> list) {
        StringBuilder stringBuilder = new StringBuilder();
        for (KeyChord keyChord : list) {
            stringBuilder.append(stringBuilder.length() == 0 ? "" : ", ").append(keyChord.text());
        }
        return stringBuilder.toString();
    }

    static String display(List<KeyChord> list, IntFunction<String> intFunction) {
        StringBuilder stringBuilder = new StringBuilder();
        for (KeyChord keyChord : list) {
            stringBuilder.append(stringBuilder.length() == 0 ? "" : " / ").append(keyChord.display(intFunction));
        }
        return stringBuilder.toString();
    }

    static List<KeyChord> parse(String string) {
        ArrayList<KeyChord> arrayList = new ArrayList<KeyChord>();
        for (String string2 : string.split(",")) {
            String string3 = string2.trim().toUpperCase(Locale.ROOT);
            if (string3.isEmpty() || string3.equals(NONE)) continue;
            ArrayList<Integer> arrayList2 = new ArrayList<Integer>();
            for (String string4 : string3.split("\\+", -1)) {
                arrayList2.add(KeyChord.code(string4.trim()));
            }
            KeyChord object = KeyChord.of((Integer)arrayList2.get(arrayList2.size() - 1), arrayList2.subList(0, arrayList2.size() - 1));
            if (arrayList.contains(object)) continue;
            arrayList.add(object);
        }
        return List.copyOf(arrayList);
    }

    static String gameName(int n) {
        String string = n > 0 && n < NAMES.length ? Keyboard.getKeyName((int)n) : null;
        return string != null && !string.isEmpty() ? string : KeyChord.name(n);
    }

    static String latinName(int n) {
        String string = KeyChord.gameName(n);
        for (int i = 0; i < string.length(); ++i) {
            if (string.charAt(i) <= '\u00ff') continue;
            return KeyChord.name(n);
        }
        return string;
    }

    static String name(int n) {
        String string = n > 0 && n < NAMES.length ? NAMES[n] : null;
        return string != null ? string : Integer.toString(n);
    }

    static boolean modifier(int n) {
        return KeyChord.rank(n) < 3;
    }

    static int canonical(int n) {
        return n == 54 ? 42 : (n == 157 ? 29 : (n == 184 ? 56 : n));
    }

    static boolean down(int n, IntPredicate intPredicate) {
        return intPredicate.test(n) || n == 42 && intPredicate.test(54) || n == 29 && intPredicate.test(157) || n == 56 && intPredicate.test(184);
    }

    static int otherSide(int n) {
        return n == 42 ? 54 : (n == 29 ? 157 : (n == 56 ? 184 : 0));
    }

    private static String heldName(int n) {
        return switch (n) {
            case 42 -> "SHIFT";
            case 29 -> "CTRL";
            case 56 -> "ALT";
            default -> KeyChord.name(n);
        };
    }

    private static int rank(int n) {
        return switch (n) {
            case 42 -> 0;
            case 29 -> 1;
            case 56 -> 2;
            default -> 3;
        };
    }

    private static int code(String string) {
        Integer n = CODES.get(string);
        if (n == null && !string.isEmpty() && string.chars().allMatch(Character::isDigit)) {
            n = Integer.parseInt(string);
        }
        if (n == null || n <= 0 || n >= NAMES.length) {
            throw new IllegalArgumentException("no key is called " + string);
        }
        return n;
    }

    private static int constant(Field field) {
        try {
            return field.getInt(null);
        }
        catch (IllegalAccessException illegalAccessException) {
            return 0;
        }
    }

    static {
        for (Field field : Keyboard.class.getFields()) {
            int n;
            int n2 = field.getModifiers();
            String string = field.getName();
            if (!Modifier.isStatic(n2) || field.getType() != Integer.TYPE || !string.startsWith("KEY_") || string.endsWith("WIN") || (n = KeyChord.constant(field)) <= 0 || n >= NAMES.length) continue;
            KeyChord.NAMES[n] = string.substring(4);
            CODES.put(string.substring(4), n);
        }
        CODES.putAll(Map.of("SHIFT", 42, "CTRL", 29, "ALT", 56, "LALT", 56, "RALT", 184, "PAGEUP", 201, "PAGEDOWN", 209, "ENTER", 28, "BACKSPACE", 14));
        ORDER = Comparator.comparingInt(KeyChord::rank).thenComparingInt(Integer::intValue);
    }
}

