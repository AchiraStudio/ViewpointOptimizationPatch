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
import viewpoint.platform.PackTarget;

public final class PackPass {
    public static final String COLOR = "color";
    public static final String PREVIOUS = ".previous";
    public static final int MAX_TARGETS = 8;
    public static final int MAX_INPUTS = 12;
    private static final Map<String, Stage> RENDERER = Map.of("color", Stage.TRANSLUCENT, "depth", Stage.TRANSLUCENT, "albedo", Stage.TRANSLUCENT, "normal", Stage.TRANSLUCENT, "light", Stage.TRANSLUCENT, "velocity", Stage.TRANSLUCENT, "farDepth", Stage.TRANSLUCENT, "gi", Stage.COMPOSITE, "volume", Stage.COMPOSITE, "bloom", Stage.COMPOSITE);
    public final String id;
    public final String vertex;
    public final String fragment;
    public final Stage stage;
    public final List<String> inputs;
    public final String target;
    public final String when;

    private PackPass(String string, Stage stage, String string2, String string3, List<String> list, String string4, String string5) {
        this.id = string;
        this.stage = stage;
        this.vertex = string2;
        this.fragment = string3;
        this.inputs = List.copyOf(list);
        this.target = string4;
        this.when = string5;
    }

    public static String sampler(String string) {
        String string2 = string.endsWith(PREVIOUS) ? string.substring(0, string.length() - PREVIOUS.length()) + "Previous" : string;
        return "u" + Character.toUpperCase(string2.charAt(0)) + string2.substring(1);
    }

    static Graph parse(Properties properties, Graph graph, List<PackOption> list) {
        LinkedHashMap<String, PackTarget> linkedHashMap = new LinkedHashMap<String, PackTarget>();
        graph.targets.forEach(packTarget -> linkedHashMap.put(packTarget.name, (PackTarget)packTarget));
        for (String object : PackPass.words(properties.getProperty("targets", ""))) {
            String string = properties.getProperty("target." + object);
            if (string == null) {
                throw new IllegalArgumentException("target." + object + " is missing");
            }
            linkedHashMap.put(object, PackTarget.parse(object, string));
        }
        if (linkedHashMap.size() > 8) {
            throw new IllegalArgumentException("targets: at most 8, not " + linkedHashMap.size());
        }
        LinkedHashMap linkedHashMap2 = new LinkedHashMap();
        graph.passes.forEach(packPass -> linkedHashMap2.put(packPass.id, packPass));
        for (String string : PackPass.words(properties.getProperty("passes", ""))) {
            linkedHashMap2.put(string, PackPass.parse(string, properties, linkedHashMap, list));
        }
        return new Graph(List.copyOf(linkedHashMap.values()), List.copyOf(linkedHashMap2.values()));
    }

    private static PackPass parse(String string, Properties properties, Map<String, PackTarget> map, List<PackOption> list) {
        String string2;
        String string32;
        String string4 = "pass." + string + ".";
        if (!PackOption.ID.matcher(string).matches()) {
            throw new IllegalArgumentException("passes: " + string + " is not letters and digits");
        }
        Stage stage = PackPass.stage(string4, properties.getProperty(string4 + "stage"));
        String string5 = properties.getProperty(string4 + "fragment");
        if (string5 == null || string5.isBlank()) {
            throw new IllegalArgumentException(string4 + "fragment is missing");
        }
        ArrayList<String> arrayList = new ArrayList<String>(PackPass.words(properties.getProperty(string4 + "inputs", "")));
        if (arrayList.size() > 12) {
            throw new IllegalArgumentException(string4 + "inputs: at most 12");
        }
        for (String string32 : arrayList) {
            PackPass.checkInput(string4, string32, stage, map);
        }
        String string6 = properties.getProperty(string4 + "target", COLOR).trim();
        if (!string6.equals(COLOR) && !map.containsKey(string6)) {
            throw new IllegalArgumentException(string4 + "target: " + (String)string6 + " is neither color nor a target");
        }
        if (!string6.equals(COLOR) && arrayList.contains(string6)) {
            throw new IllegalArgumentException(string4 + "inputs: it cannot read " + (String)string6 + " as it writes it; " + (String)string6 + ".previous is last frame's");
        }
        string32 = properties.getProperty(string4 + "when");
        if (string32 != null) {
            string2 = string32 = string32.trim();
            if (list.stream().noneMatch(packOption -> packOption.id.equals(string2) && packOption.kind == PackOption.Kind.TOGGLE)) {
                throw new IllegalArgumentException(string4 + "when: " + string32 + " is no toggle option of the pack");
            }
        }
        string2 = properties.getProperty(string4 + "vertex", "screen.vert").trim();
        return new PackPass(string, stage, string2, string5.trim(), arrayList, string6, string32);
    }

    private static Stage stage(String string, String string2) {
        if (string2 == null) {
            throw new IllegalArgumentException(string + "stage is missing");
        }
        for (Stage stage : Stage.values()) {
            if (!stage.name().toLowerCase(Locale.ROOT).equals(string2.trim())) continue;
            return stage;
        }
        throw new IllegalArgumentException(string + "stage: " + string2 + " is not translucent, composite or final");
    }

    private static void checkInput(String string, String string2, Stage stage, Map<String, PackTarget> map) {
        Stage stage2 = RENDERER.get(string2);
        if (stage2 != null) {
            if (stage2.compareTo(stage) > 0) {
                throw new IllegalArgumentException(string + "inputs: " + string2 + " is not there yet at " + String.valueOf((Object)stage));
            }
            return;
        }
        boolean bl = string2.endsWith(PREVIOUS);
        String string3 = bl ? string2.substring(0, string2.length() - PREVIOUS.length()) : string2;
        PackTarget packTarget = map.get(string3);
        if (packTarget == null) {
            throw new IllegalArgumentException(string + "inputs: " + string2 + " is neither the renderer's nor a target");
        }
        if (bl && !packTarget.history) {
            throw new IllegalArgumentException(string + "inputs: " + string3 + " keeps no history (target." + string3 + ")");
        }
    }

    private static List<String> words(String string) {
        ArrayList<String> arrayList = new ArrayList<String>();
        for (String string2 : string.trim().split("\\s+")) {
            if (string2.isEmpty()) continue;
            arrayList.add(string2);
        }
        return arrayList;
    }

    public String toString() {
        return this.id;
    }

    public static enum Stage {
        TRANSLUCENT,
        COMPOSITE,
        FINAL;

    }

    record Graph(List<PackTarget> targets, List<PackPass> passes) {
    }
}

