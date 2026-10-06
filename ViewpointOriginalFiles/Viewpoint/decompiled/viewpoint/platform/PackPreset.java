/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import viewpoint.platform.PackOption;

public final class PackPreset {
    public final String id;
    public final String label;
    private final Map<String, String> values;

    private PackPreset(String string, String string2, Map<String, String> map) {
        this.id = string;
        this.label = string2;
        this.values = Map.copyOf(map);
    }

    public String valueFor(PackOption packOption) {
        return this.values.getOrDefault(packOption.id, packOption.fallback);
    }

    static Ladder parse(Properties properties, List<PackOption> list, List<String> list2, List<PackPreset> list3, String string2) {
        Object object2;
        Map<String, Map<String, String>> map = new LinkedHashMap<String, Map<String, String>>();
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (PackPreset packPreset : list3) {
            object2 = new LinkedHashMap<String, String>(packPreset.values);
            object2.keySet().removeAll(list2);
            map.put(packPreset.id, (Map<String, String>)object2);
            linkedHashMap.put(packPreset.id, packPreset.label);
        }
        boolean bl = list2.stream().anyMatch(string -> PackPreset.hasOldLines(properties, string));
        boolean bl2 = properties.stringPropertyNames().stream().anyMatch(string -> string.equals("presets") || string.startsWith("preset."));
        if (bl && bl2) {
            throw new IllegalArgumentException("option.[id].preset.[name] and presets / preset.[id] are two forms of the same: use one");
        }
        if (properties.getProperty("presets") != null) {
            map = PackPreset.listed(properties.getProperty("presets"), map);
        }
        if (bl) {
            PackPreset.oldForm(properties, list, list2, map);
        }
        for (String object3 : properties.stringPropertyNames()) {
            if (!object3.startsWith("preset.") || object3.equals("preset.default")) continue;
            PackPreset.line(object3, properties.getProperty(object3).trim(), map, linkedHashMap);
        }
        if (map.isEmpty()) {
            return new Ladder(List.of(), null);
        }
        object2 = properties.getProperty("preset.default", string2 != null && map.containsKey(string2) ? string2 : null);
        Object object = object2 != null ? ((String)object2).trim() : (object2 = map.containsKey("default") ? "default" : map.keySet().iterator().next());
        if (!map.containsKey(object2)) {
            throw new IllegalArgumentException("preset.default: no preset " + (String)object2);
        }
        ArrayList<PackPreset> arrayList = new ArrayList<PackPreset>();
        for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
            PackPreset.check(entry.getKey(), entry.getValue(), list, entry.getKey().equals(object2));
            String string3 = linkedHashMap.getOrDefault(entry.getKey(), PackPreset.title(entry.getKey()));
            arrayList.add(new PackPreset(entry.getKey(), string3, entry.getValue()));
        }
        return new Ladder(List.copyOf(arrayList), (String)object2);
    }

    private static boolean hasOldLines(Properties properties, String string) {
        String string3 = "option." + string + ".preset.";
        return properties.stringPropertyNames().stream().anyMatch(string2 -> string2.startsWith(string3));
    }

    private static Map<String, Map<String, String>> listed(String string, Map<String, Map<String, String>> map) {
        LinkedHashMap<String, Map<String, String>> linkedHashMap = new LinkedHashMap<String, Map<String, String>>();
        for (String string2 : string.trim().split("\\s+")) {
            if (string2.isEmpty()) continue;
            if (!PackOption.ID.matcher(string2).matches()) {
                throw new IllegalArgumentException("presets: " + string2 + " is not letters and digits");
            }
            linkedHashMap.put(string2, map.getOrDefault(string2, new LinkedHashMap()));
        }
        return linkedHashMap;
    }

    private static void oldForm(Properties properties, List<PackOption> list, List<String> list2, Map<String, Map<String, String>> map) {
        if (map.isEmpty()) {
            for (String string : PackOption.PRESETS) {
                map.put(string, new LinkedHashMap());
            }
        }
        for (String string : list2) {
            for (String string2 : PackOption.PRESETS) {
                String string3 = properties.getProperty("option." + string + ".preset." + string2);
                if (string3 == null || !map.containsKey(string2)) continue;
                map.get(string2).put(string, string3.trim());
            }
        }
    }

    private static void line(String string, String string2, Map<String, Map<String, String>> map, Map<String, String> map2) {
        String string3;
        String string4 = string.substring("preset.".length());
        boolean bl = string4.endsWith(".label");
        String string5 = string3 = bl ? string4.substring(0, string4.length() - ".label".length()) : string4;
        if (!map.containsKey(string3)) {
            throw new IllegalArgumentException(string + ": no such preset (" + String.join((CharSequence)", ", map.keySet()) + ")");
        }
        if (bl) {
            map2.put(string3, string2);
            return;
        }
        for (String string6 : string2.split("\\s+")) {
            if (string6.isEmpty()) continue;
            int n = string6.indexOf(61);
            if (n <= 0 || n == string6.length() - 1) {
                throw new IllegalArgumentException(string + ": " + string6 + " is not option=value");
            }
            map.get(string3).put(string6.substring(0, n), string6.substring(n + 1));
        }
    }

    private static void check(String string, Map<String, String> map, List<PackOption> list, boolean bl) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            PackOption packOption2 = list.stream().filter(packOption -> packOption.id.equals(entry.getKey())).findFirst().orElseThrow(() -> new IllegalArgumentException("preset." + string + ": no option " + (String)entry.getKey()));
            packOption2.check("preset." + string + " " + packOption2.id, entry.getValue());
            if (!bl || packOption2.same(entry.getValue(), packOption2.fallback)) continue;
            throw new IllegalArgumentException("preset." + string + " is the one a fresh install starts at, so " + packOption2.id + " must be its default there (" + packOption2.fallback + ")");
        }
    }

    private static String title(String string) {
        String string2 = PackOption.constant(string).replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(string2.charAt(0)) + string2.substring(1);
    }

    public String toString() {
        return this.id;
    }

    record Ladder(List<PackPreset> presets, String first) {
    }
}

