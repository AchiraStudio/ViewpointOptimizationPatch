/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import viewpoint.iris.IdTable;
import viewpoint.iris.IfExpression;
import viewpoint.iris.IrisOptions;
import viewpoint.iris.Macros;
import viewpoint.iris.PackSource;
import viewpoint.iris.Preprocessor;
import viewpoint.iris.ProgramSet;
import viewpoint.iris.ShadersProperties;

public final class IrisPack {
    private static final Pattern CONST_ROTATION = Pattern.compile("\\s*const\\s+float\\s+sunPathRotation\\b");
    private static final Pattern FORMATTING = Pattern.compile("\u00a7.?");
    public static final String PREFIX = "minecraft-";
    public final String id;
    public final File file;
    public final PackSource source;
    public final IrisOptions options;
    public final Map<String, String> lang;
    public final ShadersProperties defaults;
    public final String folder;
    public final boolean clock;

    private IrisPack(String string, File file, PackSource packSource, IrisOptions irisOptions, Map<String, String> map, ShadersProperties shadersProperties, String string2, boolean bl) {
        this.id = string;
        this.file = file;
        this.source = packSource;
        this.options = irisOptions;
        this.lang = map;
        this.defaults = shadersProperties;
        this.folder = string2;
        this.clock = bl;
    }

    public static IrisPack load(File file, Map<String, String> map) throws IOException {
        PackSource packSource = PackSource.open(file);
        IrisOptions irisOptions = IrisOptions.scan(packSource);
        Map<String, String> map2 = IrisPack.lang(packSource);
        ShadersProperties shadersProperties = IrisPack.properties(packSource, map, irisOptions, Map.of());
        String string = ProgramSet.overworld(packSource, packSource.read("dimension.properties"));
        return new IrisPack(IrisPack.id(packSource.name()), file, packSource, irisOptions, map2, shadersProperties, string, IrisPack.clock(packSource));
    }

    public Configured configure(Map<String, String> map, Map<String, String> map2) throws IOException {
        Map<String, String> map3 = this.options.values(map);
        Map<String, String> map4 = IrisPack.propertyMacros(map2, this.options.macros(map3));
        ShadersProperties shadersProperties = IrisPack.properties(this.source, map2, this.options, map3);
        ArrayList<String> arrayList = new ArrayList<String>();
        IdTable idTable = IdTable.parse(this.preprocessed("block.properties", map4), "block");
        IdTable idTable2 = IdTable.parse(this.preprocessed("entity.properties", map4), "entity");
        IdTable idTable3 = IdTable.parse(this.preprocessed("item.properties", map4), "item");
        Set<String> set = this.options.toggles();
        ProgramSet programSet = ProgramSet.find(this.source, this.folder, string -> this.enabled(shadersProperties, (String)string, map4, set, (List<String>)arrayList));
        return new Configured(this, map3, map4, shadersProperties, idTable, idTable2, idTable3, programSet, arrayList);
    }

    public Preprocessor.Result preprocess(Configured configured, String string, Map<String, String> map) throws IOException {
        return this.preprocess(configured, string, map, Map.of());
    }

    public Preprocessor.Result preprocess(Configured configured, String string, Map<String, String> map, Map<String, String> map2) throws IOException {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>(this.options.changed(configured.values()));
        linkedHashMap.putAll(map2);
        Preprocessor preprocessor = new Preprocessor(this.source, map, linkedHashMap, this.options.toggles());
        return preprocessor.run(string);
    }

    private static boolean clock(PackSource packSource) throws IOException {
        for (String string : IrisOptions.sources(packSource)) {
            for (String string2 : packSource.read(string).split("\n")) {
                if (!string2.contains("sunPathRotation") || CONST_ROTATION.matcher(string2).lookingAt()) continue;
                return true;
            }
        }
        return false;
    }

    public Map<String, String> profile(String string) {
        return this.profile(string, new HashSet<String>());
    }

    private Map<String, String> profile(String string, Set<String> set) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        List<String> list = this.defaults.profiles.get(string);
        if (list == null || !set.add(string)) {
            return linkedHashMap;
        }
        for (String string2 : list) {
            if (string2.startsWith("profile.")) {
                linkedHashMap.putAll(this.profile(string2.substring(8), set));
                continue;
            }
            if (string2.startsWith("!")) {
                this.put(linkedHashMap, string2.substring(1), "false");
                continue;
            }
            if (string2.contains("=") || string2.contains(":")) {
                int n = string2.indexOf(61) >= 0 ? string2.indexOf(61) : string2.indexOf(58);
                this.put(linkedHashMap, string2.substring(0, n), string2.substring(n + 1));
                continue;
            }
            this.put(linkedHashMap, string2, "true");
        }
        return linkedHashMap;
    }

    private void put(Map<String, String> map, String string, String string2) {
        if (this.options.get(string) != null) {
            map.put(string, string2);
        }
    }

    public String label(String string, String string2) {
        String string3 = this.text(string).strip();
        return string3.isEmpty() ? string2 : string3;
    }

    public String text(String string) {
        return FORMATTING.matcher(this.lang.getOrDefault(string, "")).replaceAll("");
    }

    private String preprocessed(String string, Map<String, String> map) throws IOException {
        String string2 = this.source.read(string);
        if (string2 == null) {
            return null;
        }
        return new Preprocessor(this.source, map, Map.of(), Set.of()).runProperties(string, string2);
    }

    private static ShadersProperties properties(PackSource packSource, Map<String, String> map, IrisOptions irisOptions, Map<String, String> map2) throws IOException {
        String string = packSource.read("shaders.properties");
        if (string == null) {
            return ShadersProperties.parse("");
        }
        Map<String, String> map3 = IrisPack.propertyMacros(map, irisOptions.macros(irisOptions.values(map2)));
        return ShadersProperties.parse(new Preprocessor(packSource, map3, Map.of(), Set.of()).runProperties("shaders.properties", string));
    }

    private static Map<String, String> propertyMacros(Map<String, String> map, Map<String, String> map2) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        map.forEach((string, string2) -> linkedHashMap.put((String)string, string2.isEmpty() ? "1" : string2));
        linkedHashMap.putAll(map2);
        return linkedHashMap;
    }

    private boolean enabled(ShadersProperties shadersProperties, String string, Map<String, String> map, Set<String> set, List<String> list) {
        String string2 = shadersProperties.programEnabled.get(this.folder + string);
        if (string2 == null) {
            string2 = shadersProperties.programEnabled.get(string);
        }
        if (string2 == null) {
            return true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String string3 : string2.split("(?<=[^\\w])|(?=[^\\w])")) {
            stringBuilder.append((String)(set.contains(string3) ? "defined(" + string3 + ")" : string3));
        }
        try {
            Macros illegalArgumentException = new Macros();
            map.forEach(illegalArgumentException::define);
            return IfExpression.test(stringBuilder.toString(), illegalArgumentException);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            list.add("program." + string + ".enabled: " + illegalArgumentException.getMessage());
            return true;
        }
    }

    private static Map<String, String> lang(PackSource packSource) throws IOException {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (String string : packSource.paths()) {
            if (!string.equalsIgnoreCase("lang/en_us.lang")) continue;
            for (String string2 : packSource.read(string).split("\n")) {
                String string3 = string2.strip();
                int n = string3.indexOf(61);
                if (n <= 0 || string3.startsWith("#")) continue;
                linkedHashMap.put(string3.substring(0, n).strip(), string3.substring(n + 1).strip());
            }
        }
        return linkedHashMap;
    }

    static String id(String string) {
        return PREFIX + string.replaceAll("[^A-Za-z0-9_-]+", "_");
    }

    public record Configured(IrisPack pack, Map<String, String> values, Map<String, String> macros, ShadersProperties properties, IdTable blocks, IdTable entities, IdTable items, ProgramSet programs, List<String> notes) {
    }
}

