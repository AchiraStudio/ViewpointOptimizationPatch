/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import viewpoint.iris.PackSource;

public final class IrisOptions {
    private static final Pattern VALUE = Pattern.compile("^\\s*#\\s*define\\s+(\\w+)\\s+([^\\s/]+)\\s*//[^\\[]*\\[([^\\]]*)\\].*$");
    private static final Pattern TOGGLE = Pattern.compile("^\\s*(//+)?\\s*#\\s*define\\s+(\\w+)\\s*(//.*)?$");
    private static final Pattern CONST = Pattern.compile("^\\s*const\\s+(?:int|float|bool)\\s+(\\w+)\\s*=\\s*([^;]+?)\\s*;\\s*//[^\\[]*\\[([^\\]]*)\\].*$");
    private static final Pattern INCLUDED = Pattern.compile("(?m)^\\s*#\\s*include\\s*[\"<]([^\">]+)[\">]");
    private static final Pattern TESTED = Pattern.compile("#\\s*(?:ifdef|ifndef)\\s+(\\w+)|defined\\s*\\(?\\s*(\\w+)");
    private final Map<String, Option> options = new LinkedHashMap<String, Option>();

    private IrisOptions() {
    }

    public static IrisOptions scan(PackSource packSource) throws IOException {
        IrisOptions irisOptions = new IrisOptions();
        HashSet<String> hashSet = new HashSet<String>();
        ArrayList<String[]> arrayList = new ArrayList<String[]>();
        for (String stringArray : IrisOptions.sources(packSource)) {
            String string = packSource.read(stringArray);
            Matcher matcher = TESTED.matcher(string);
            while (matcher.find()) {
                hashSet.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
            }
            for (String string2 : string.split("\n")) {
                irisOptions.line(string2, stringArray, arrayList);
            }
        }
        for (String[] stringArray : arrayList) {
            if (!hashSet.contains(stringArray[0]) || irisOptions.options.containsKey(stringArray[0])) continue;
            irisOptions.options.put(stringArray[0], new Option(stringArray[0], Kind.TOGGLE, stringArray[1], List.of(), stringArray[2]));
        }
        return irisOptions;
    }

    private void line(String string, String string2, List<String[]> list) {
        if (!string.contains("define") && !string.contains("const")) {
            return;
        }
        Matcher matcher = VALUE.matcher(string);
        if (matcher.matches()) {
            this.add(matcher.group(1), Kind.VALUE, matcher.group(2), matcher.group(3), string2);
            return;
        }
        Matcher matcher2 = CONST.matcher(string);
        if (matcher2.matches()) {
            this.add(matcher2.group(1), Kind.CONST, matcher2.group(2).strip(), matcher2.group(3), string2);
            return;
        }
        Matcher matcher3 = TOGGLE.matcher(string);
        if (matcher3.matches()) {
            list.add(new String[]{matcher3.group(2), matcher3.group(1) == null ? "true" : "false", string2});
        }
    }

    private void add(String string, Kind kind, String string2, String string3, String string4) {
        if (this.options.containsKey(string)) {
            return;
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        for (String string5 : string3.strip().split("\\s+")) {
            if (string5.isEmpty()) continue;
            arrayList.add(string5);
        }
        if (!arrayList.contains(string2)) {
            arrayList.add(string2);
        }
        this.options.put(string, new Option(string, kind, string2, List.copyOf(arrayList), string4));
    }

    public List<Option> all() {
        return List.copyOf(this.options.values());
    }

    public Option get(String string) {
        return this.options.get(string);
    }

    public Set<String> toggles() {
        HashSet<String> hashSet = new HashSet<String>();
        for (Option option : this.options.values()) {
            if (option.kind != Kind.TOGGLE) continue;
            hashSet.add(option.name);
        }
        return Collections.unmodifiableSet(hashSet);
    }

    public Map<String, String> changed(Map<String, String> map) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Option option = this.options.get(entry.getKey());
            if (option == null || option.fallback.equals(entry.getValue())) continue;
            linkedHashMap.put(entry.getKey(), entry.getValue());
        }
        return linkedHashMap;
    }

    public Map<String, String> macros(Map<String, String> map) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (Option option : this.options.values()) {
            String string = map.getOrDefault(option.name, option.fallback);
            if (option.kind == Kind.TOGGLE) {
                if (!string.equals("true")) continue;
                linkedHashMap.put(option.name, "1");
                continue;
            }
            linkedHashMap.put(option.name, string);
        }
        return linkedHashMap;
    }

    public Map<String, String> values(Map<String, String> map) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (Option option : this.options.values()) {
            linkedHashMap.put(option.name, map.getOrDefault(option.name, option.fallback));
        }
        return linkedHashMap;
    }

    static Set<String> sources(PackSource packSource) throws IOException {
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();
        ArrayDeque<String> arrayDeque = new ArrayDeque<String>();
        for (String object : packSource.paths()) {
            if (!IrisOptions.isSource(object)) continue;
            arrayDeque.add(object);
        }
        while (!arrayDeque.isEmpty()) {
            String string;
            String string2 = (String)arrayDeque.poll();
            if (!linkedHashSet.add(string2)) continue;
            Matcher matcher = INCLUDED.matcher(packSource.read(string2));
            String string3 = string = string2.contains("/") ? string2.substring(0, string2.lastIndexOf(47) + 1) : "";
            while (matcher.find()) {
                String string4 = matcher.group(1);
                String string5 = PackSource.normalize((String)(string4.startsWith("/") ? string4 : string + string4));
                if (string5 == null || linkedHashSet.contains(string5) || !packSource.exists(string5)) continue;
                arrayDeque.add(string5);
            }
        }
        return linkedHashSet;
    }

    private static boolean isSource(String string) {
        return string.endsWith(".glsl") || string.endsWith(".fsh") || string.endsWith(".vsh") || string.endsWith(".gsh") || string.endsWith(".csh") || string.endsWith(".tcs") || string.endsWith(".tes");
    }

    public record Option(String name, Kind kind, String fallback, List<String> values, String file) {
    }

    public static enum Kind {
        TOGGLE,
        VALUE,
        CONST;

    }
}

