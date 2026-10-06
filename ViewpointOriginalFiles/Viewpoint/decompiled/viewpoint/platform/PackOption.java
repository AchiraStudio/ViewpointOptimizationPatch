/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import viewpoint.platform.LiveSettings;

public final class PackOption {
    public static final List<String> PRESETS = List.of("potato", "low", "default", "high", "ultra");
    static final Pattern ID = Pattern.compile("[A-Za-z][A-Za-z0-9]*");
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern VALUE = Pattern.compile("[A-Za-z0-9_.-]+");
    public final String id;
    public final String label;
    public final String tooltip;
    public final Kind kind;
    final List<String> values;
    final float min;
    final float max;
    final float step;
    final String fallback;

    private PackOption(String string, String string2, String string3, Kind kind, List<String> list, float[] fArray, String string4) {
        this.id = string;
        this.label = string2;
        this.tooltip = string3;
        this.kind = kind;
        this.values = list;
        this.min = fArray[0];
        this.max = fArray[1];
        this.step = fArray[2];
        this.fallback = string4;
    }

    static PackOption parse(String string2, Properties properties) {
        List<String> list;
        if (!ID.matcher(string2).matches()) {
            throw new IllegalArgumentException("option id " + string2 + " is not letters and digits");
        }
        String string3 = "option." + string2 + ".";
        String string4 = properties.getProperty(string3 + "values");
        String string5 = properties.getProperty(string3 + "range");
        String string6 = properties.getProperty(string3 + "default");
        if (string6 == null) {
            throw new IllegalArgumentException(string3 + "default is missing");
        }
        Kind kind = string4 != null ? Kind.CHOICE : (string5 != null ? Kind.NUMBER : Kind.TOGGLE);
        List<String> list2 = list = string4 == null ? List.of() : List.of(string4.trim().split("\\s+"));
        if (list.stream().anyMatch(string -> !VALUE.matcher((CharSequence)string).matches())) {
            throw new IllegalArgumentException(string3 + "values are words of letters, digits, _, . and -: " + string4);
        }
        float[] fArray = string5 == null ? new float[3] : PackOption.range(string3, string5);
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (String object : PRESETS) {
            String string7 = properties.getProperty(string3 + "preset." + object);
            if (string7 == null) continue;
            linkedHashMap.put(object, string7.trim());
        }
        for (String string8 : properties.stringPropertyNames()) {
            if (!string8.startsWith(string3 + "preset.") || linkedHashMap.containsKey(string8.substring(string3.length() + 7))) continue;
            throw new IllegalArgumentException(string8 + ": no such preset (" + String.join((CharSequence)", ", PRESETS) + ")");
        }
        PackOption packOption = new PackOption(string2, properties.getProperty(string3 + "label", string2).trim(), properties.getProperty(string3 + "tooltip", "").trim(), kind, list, fArray, string6.trim());
        packOption.check(string3 + "default", packOption.fallback);
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            packOption.check(string3 + "preset." + (String)entry.getKey(), (String)entry.getValue());
        }
        return packOption;
    }

    private static float[] range(String string, String string2) {
        String[] stringArray = string2.trim().split("\\s+");
        try {
            float[] fArray = new float[]{Float.parseFloat(stringArray[0]), Float.parseFloat(stringArray[1]), Float.parseFloat(stringArray[2])};
            if (stringArray.length == 3 && fArray[0] < fArray[1] && fArray[2] > 0.0f) {
                return fArray;
            }
        }
        catch (ArrayIndexOutOfBoundsException | NumberFormatException runtimeException) {
            // empty catch block
        }
        throw new IllegalArgumentException(string + "range is min max step, not " + string2);
    }

    void check(String string, String string2) {
        boolean bl;
        switch (this.kind.ordinal()) {
            default: {
                throw new IncompatibleClassChangeError();
            }
            case 0: {
                boolean bl2;
                if (string2.equals("true") || string2.equals("false")) {
                    bl2 = true;
                    break;
                }
                bl2 = false;
                break;
            }
            case 1: {
                boolean bl2;
                if (NUMBER.matcher(string2).matches() && Float.parseFloat(string2) >= this.min && Float.parseFloat(string2) <= this.max) {
                    bl2 = true;
                    break;
                }
                bl2 = false;
                break;
            }
            case 2: {
                boolean bl2 = bl = this.values.contains(string2);
            }
        }
        if (!bl) {
            throw new IllegalArgumentException(string + ": " + string2 + " is not one of " + this.describe());
        }
    }

    private String describe() {
        return switch (this.kind.ordinal()) {
            default -> throw new IncompatibleClassChangeError();
            case 0 -> "true, false";
            case 1 -> this.min + " to " + this.max;
            case 2 -> String.join((CharSequence)", ", this.values);
        };
    }

    List<String> extremes() {
        return switch (this.kind.ordinal()) {
            default -> throw new IncompatibleClassChangeError();
            case 0 -> List.of("true", "false");
            case 1 -> List.of(PackOption.text(this.min), PackOption.text(this.max));
            case 2 -> this.values;
        };
    }

    boolean same(String string, String string2) {
        return this.kind == Kind.NUMBER ? Float.parseFloat(string) == Float.parseFloat(string2) : string.equals(string2);
    }

    void define(StringBuilder stringBuilder, String string2) {
        String string3 = "OPTION_" + PackOption.constant(this.id);
        switch (this.kind.ordinal()) {
            case 0: {
                PackOption.line(stringBuilder, string3, string2.equals("true") ? "1" : "0");
                break;
            }
            case 1: {
                PackOption.line(stringBuilder, string3, this.step >= 1.0f ? PackOption.text(Math.round(Float.parseFloat(string2))) : string2);
                break;
            }
            case 2: {
                if (this.values.stream().allMatch(string -> NUMBER.matcher((CharSequence)string).matches())) {
                    PackOption.line(stringBuilder, string3, string2);
                    break;
                }
                PackOption.line(stringBuilder, string3, Integer.toString(this.values.indexOf(string2)));
                for (int i = 0; i < this.values.size(); ++i) {
                    PackOption.line(stringBuilder, string3 + "_" + PackOption.constant(this.values.get(i)), Integer.toString(i));
                }
                break;
            }
        }
    }

    LiveSettings.Setting declare(String string, String string2) {
        return switch (this.kind.ordinal()) {
            default -> throw new IncompatibleClassChangeError();
            case 0 -> LiveSettings.toggle(string, this.label, string2, this.fallback.equals("true"));
            case 1 -> LiveSettings.number(string, this.label, string2, this.min, this.max, this.step, Float.parseFloat(this.fallback));
            case 2 -> LiveSettings.choice(string, this.label, string2, this.values.toArray(new String[0]), this.values.indexOf(this.fallback));
        };
    }

    static String constant(String string) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            boolean bl = i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(string.charAt(i - 1));
            stringBuilder.append(bl ? "_" : "").append(Character.isLetterOrDigit(c) ? (char)Character.toUpperCase(c) : (char)'_');
        }
        return stringBuilder.toString();
    }

    private static void line(StringBuilder stringBuilder, String string, String string2) {
        stringBuilder.append("#define ").append(string).append(' ').append(string2).append('\n');
    }

    private static String text(float f) {
        return (double)f == Math.rint(f) ? Integer.toString((int)f) : Float.toString(f);
    }

    public static enum Kind {
        TOGGLE,
        NUMBER,
        CHOICE;

    }
}

