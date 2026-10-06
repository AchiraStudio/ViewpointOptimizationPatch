/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import viewpoint.platform.PackOption;
import viewpoint.platform.PackPass;
import viewpoint.platform.PackPreset;
import viewpoint.platform.Pipeline;
import viewpoint.platform.ShaderPack;

public final class ShaderPacks {
    public static final String NORMAL = "normal";
    public static final String DEFAULT = "default";
    public static final String VIVID = "vivid";
    static final List<String> BUILT_IN = List.of("normal", "default", "vivid");
    static final Map<String, String> REMOVED = Map.of("dreamy", "vivid");
    public static final String FOLDER = "viewpoint-shaderpacks";
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]+");
    private static final Map<String, ShaderPack> packs = new LinkedHashMap<String, ShaderPack>();
    private static final Map<String, String> broken = new LinkedHashMap<String, String>();
    private static volatile Active active;
    private static volatile Active drawn;
    private static volatile Failure failure;

    public static Active active() {
        return active;
    }

    public static Active drawn() {
        return drawn;
    }

    public static Failure failure() {
        return failure;
    }

    static void discover(File file) {
        packs.clear();
        broken.clear();
        for (String fileArray2 : BUILT_IN) {
            packs.put(fileArray2, ShaderPacks.builtIn(fileArray2));
        }
        TreeMap treeMap = new TreeMap();
        File[] fileArray = file == null ? null : file.listFiles(File::isDirectory);
        for (File file2 : fileArray == null ? new File[]{} : fileArray) {
            String string = file2.getName();
            if (!new File(file2, "pack.properties").isFile()) continue;
            if (!ID.matcher(string).matches() || packs.containsKey(string)) {
                broken.put(string, "a folder name of letters, digits, _ and -, and none of the built-in packs' names");
                continue;
            }
            treeMap.put(string, file2);
        }
        for (String entry : treeMap.keySet()) {
            ShaderPacks.player(entry, treeMap, new HashSet<String>());
        }
        for (Map.Entry entry : broken.entrySet()) {
            System.out.println("[Viewpoint] shader pack " + (String)entry.getKey() + " left out: " + (String)entry.getValue());
        }
        System.out.println("[Viewpoint] shader packs: " + String.join((CharSequence)", ", packs.keySet()));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static ShaderPack player(String string2, Map<String, File> map, Set<String> set) {
        ShaderPack shaderPack = packs.get(string2);
        File file = map.get(string2);
        if (shaderPack != null || file == null || broken.containsKey(string2) || !set.add(string2)) {
            return shaderPack;
        }
        try {
            ShaderPack shaderPack2 = ShaderPack.parse(string2, ShaderPack.folder(file), string -> ShaderPacks.player(string, map, set), false);
            packs.put(string2, shaderPack2);
            ShaderPack shaderPack3 = shaderPack2;
            return shaderPack3;
        }
        catch (IOException | IllegalArgumentException exception) {
            broken.put(string2, exception.getMessage());
            ShaderPack shaderPack4 = null;
            return shaderPack4;
        }
        finally {
            set.remove(string2);
        }
    }

    static List<ShaderPack> all() {
        return new ArrayList<ShaderPack>(packs.values());
    }

    static ShaderPack find(String string) {
        return packs.get(string);
    }

    static Map<String, String> leftOut() {
        return new LinkedHashMap<String, String>(broken);
    }

    static void use(ShaderPack shaderPack, Function<PackOption, String> function) {
        Pipeline pipeline = shaderPack.pipeline(function);
        String string = shaderPack.defines(function, pipeline);
        Active active = ShaderPacks.active;
        if (active.pack.id.equals(shaderPack.id) && active.defines.equals(string)) {
            return;
        }
        ShaderPacks.active = new Active(shaderPack, pipeline, string, active.generation + 1, function);
    }

    public static void useBuiltIn(String string, String string2) {
        ShaderPacks.useBuiltIn(string, string2, Map.of());
    }

    public static void useBuiltIn(String string, String string2, Map<String, String> map) {
        ShaderPack shaderPack = ShaderPacks.builtIn(string);
        PackPreset packPreset = shaderPack.preset(string2);
        ShaderPacks.use(shaderPack, packOption -> map.getOrDefault(packOption.id, packPreset.valueFor((PackOption)packOption)));
    }

    public static void linked(Active active) {
        drawn = active;
    }

    public static void failed(Active active, String string) {
        failure = new Failure(active.pack.id, active.generation, string);
        System.out.println("[Viewpoint] shader pack " + active.pack.id + " did not compile, drawn with " + ShaderPacks.drawn.pack.id + " meanwhile: " + string);
    }

    public static Active library(int n) {
        return ShaderPacks.atDefaults(ShaderPacks.builtIn(DEFAULT), n);
    }

    private static Active atDefaults(ShaderPack shaderPack, int n) {
        Pipeline pipeline = shaderPack.pipeline(packOption -> packOption.fallback);
        return new Active(shaderPack, pipeline, shaderPack.defines(packOption -> packOption.fallback, pipeline), n);
    }

    private static ShaderPack builtIn(String string2) {
        try {
            return ShaderPack.parse(string2, ShaderPack.jar(string2), string -> BUILT_IN.contains(string) ? ShaderPacks.builtIn(string) : null, true);
        }
        catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("built-in shader pack " + string2 + ": " + exception.getMessage(), exception);
        }
    }

    private ShaderPacks() {
    }

    static {
        drawn = active = ShaderPacks.atDefaults(ShaderPacks.builtIn(DEFAULT), 0);
    }

    public static final class Active {
        public final ShaderPack pack;
        public final Pipeline pipeline;
        final String defines;
        public final int generation;
        public final List<PackPass> passes;

        Active(ShaderPack shaderPack, Pipeline pipeline, String string, int n) {
            this(shaderPack, pipeline, string, n, packOption -> packOption.fallback);
        }

        Active(ShaderPack shaderPack, Pipeline pipeline, String string, int n, Function<PackOption, String> function) {
            this.pack = shaderPack;
            this.pipeline = pipeline;
            this.defines = string;
            this.generation = n;
            this.passes = shaderPack.passes.stream().filter(packPass -> packPass.when == null || ((String)function.apply(shaderPack.option(packPass.when))).equals("true")).toList();
        }
    }

    public record Failure(String packId, int generation, String log) {
    }
}

