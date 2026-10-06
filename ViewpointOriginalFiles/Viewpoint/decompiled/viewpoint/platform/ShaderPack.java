/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import viewpoint.platform.PackOption;
import viewpoint.platform.PackPass;
import viewpoint.platform.PackPreset;
import viewpoint.platform.PackTarget;
import viewpoint.platform.Pipeline;
import viewpoint.platform.Settings;
import viewpoint.platform.Shaders;

public final class ShaderPack {
    public static final String PROPERTIES = "pack.properties";
    private static final String SHADERS = "shaders/";
    private static final Pattern OPTION = Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9]*)\\}");
    public final String id;
    public final String name;
    public final String description;
    public final String author;
    public final ShaderPack base;
    public final boolean builtIn;
    public final List<PackOption> options;
    public final List<PackPreset> presets;
    public final String firstPreset;
    public final List<PackTarget> targets;
    public final List<PackPass> passes;
    private final Map<Pipeline.Knob, String> pipeline;
    private final PackFiles files;

    private ShaderPack(String string, Properties properties, ShaderPack shaderPack, boolean bl, List<PackOption> list, PackPreset.Ladder ladder, PackPass.Graph graph, Map<Pipeline.Knob, String> map, PackFiles packFiles) {
        this.id = string;
        this.name = properties.getProperty("name", string).trim();
        this.description = properties.getProperty("description", "").trim();
        this.author = properties.getProperty("author", "").trim();
        this.base = shaderPack;
        this.builtIn = bl;
        this.options = List.copyOf(list);
        this.presets = ladder.presets();
        this.firstPreset = ladder.first();
        this.targets = graph.targets();
        this.passes = graph.passes();
        this.pipeline = map;
        this.files = packFiles;
    }

    static ShaderPack parse(String string, PackFiles packFiles, Function<String, ShaderPack> function, boolean bl) throws IOException {
        ShaderPack shaderPack;
        String string2 = packFiles.read(PROPERTIES);
        if (string2 == null) {
            throw new IllegalArgumentException("no pack.properties");
        }
        Properties properties = new Properties();
        properties.load(new StringReader(string2));
        String string3 = properties.getProperty("base", "").trim();
        ShaderPack shaderPack2 = shaderPack = string3.isEmpty() ? null : function.apply(string3);
        if (!string3.isEmpty() && shaderPack == null) {
            throw new IllegalArgumentException("base " + string3 + ": no such pack");
        }
        ArrayList<PackOption> arrayList = new ArrayList<PackOption>(shaderPack == null ? List.of() : shaderPack.options);
        ArrayList<String> arrayList2 = new ArrayList<String>();
        for (Object object : properties.getProperty("options", "").trim().split("\\s+")) {
            if (object.isEmpty()) continue;
            PackOption packOption = PackOption.parse(object, properties);
            arrayList2.add((String)object);
            int n = ShaderPack.indexOf(arrayList, object);
            if (n >= 0) {
                arrayList.set(n, packOption);
                continue;
            }
            arrayList.add(packOption);
        }
        EnumMap<Pipeline.Knob, Object> enumMap = shaderPack == null ? new EnumMap(Pipeline.Knob.class) : new EnumMap<Pipeline.Knob, String>(shaderPack.pipeline);
        for (String string4 : properties.stringPropertyNames()) {
            Object object;
            if (!string4.startsWith("pipeline.")) continue;
            object = Pipeline.Knob.named(string4.substring("pipeline.".length()));
            if (object == null) {
                throw new IllegalArgumentException(string4 + ": no such pipeline knob");
            }
            enumMap.put((Pipeline.Knob)((Object)object), (Object)properties.getProperty(string4).trim());
        }
        ShaderPack.checkPipeline((Map<Pipeline.Knob, String>)enumMap, arrayList);
        PackPreset.Ladder ladder = shaderPack == null ? PackPreset.parse(properties, arrayList, arrayList2, List.of(), null) : PackPreset.parse(properties, arrayList, arrayList2, shaderPack.presets, shaderPack.firstPreset);
        PackPass.Graph graph = PackPass.parse(properties, shaderPack == null ? new PackPass.Graph(List.of(), List.of()) : new PackPass.Graph(shaderPack.targets, shaderPack.passes), arrayList);
        ShaderPack.unusedDrivers(string, packFiles);
        return new ShaderPack(string, properties, shaderPack, bl, arrayList, ladder, graph, (Map<Pipeline.Knob, String>)enumMap, packFiles);
    }

    private static void unusedDrivers(String string, PackFiles packFiles) {
        for (String string2 : Shaders.DRIVERS) {
            if (packFiles.modified(SHADERS + string2) < 0L) continue;
            System.out.println("[Viewpoint] shader pack " + string + ": its " + string2 + " is not used: the weather is the renderer's, its look the pack's (the rain's in lib/precip_look.glsl)");
        }
    }

    private static void checkPipeline(Map<Pipeline.Knob, String> map, List<PackOption> list) {
        for (Map.Entry<Pipeline.Knob, String> entry : map.entrySet()) {
            Matcher matcher = OPTION.matcher(entry.getValue());
            if (!matcher.matches()) {
                entry.getKey().parse(entry.getValue());
                continue;
            }
            int n = ShaderPack.indexOf(list, matcher.group(1));
            if (n < 0) {
                throw new IllegalArgumentException("pipeline." + entry.getKey().key + ": no option " + matcher.group(1));
            }
            for (String string : list.get(n).extremes()) {
                entry.getKey().parse(string);
            }
        }
    }

    private static int indexOf(List<PackOption> list, String string) {
        for (int i = 0; i < list.size(); ++i) {
            if (!list.get((int)i).id.equals(string)) continue;
            return i;
        }
        return -1;
    }

    public PackOption option(String string) {
        int n = ShaderPack.indexOf(this.options, string);
        return n < 0 ? null : this.options.get(n);
    }

    public PackPreset preset(String string) {
        for (PackPreset packPreset : this.presets) {
            if (!packPreset.id.equals(string)) continue;
            return packPreset;
        }
        throw new IllegalArgumentException("shader pack " + this.id + " has no preset " + string);
    }

    Pipeline pipeline(Function<PackOption, String> function) {
        EnumMap<Pipeline.Knob, String> enumMap = new EnumMap<Pipeline.Knob, String>(Pipeline.Knob.class);
        Iterator<Map.Entry<Pipeline.Knob, String>> iterator = this.pipeline.entrySet().iterator();
        while (iterator.hasNext()) {
            Matcher matcher;
            Map.Entry<Pipeline.Knob, String> entry;
            enumMap.put(entry.getKey(), (matcher = OPTION.matcher((entry = iterator.next()).getValue())).matches() ? function.apply(this.option(matcher.group(1))) : entry.getValue());
        }
        return Pipeline.of(enumMap);
    }

    String defines(Function<PackOption, String> function, Pipeline pipeline) {
        StringBuilder stringBuilder = new StringBuilder(pipeline.defines);
        for (PackOption packOption : this.options) {
            packOption.define(stringBuilder, function.apply(packOption));
        }
        return stringBuilder.toString();
    }

    String shader(String string) {
        try {
            return this.files.read(SHADERS + string);
        }
        catch (IOException iOException) {
            throw new IllegalStateException("shader pack " + this.id + ": cannot read " + string + ": " + String.valueOf(iOException), iOException);
        }
    }

    long modified(String string) {
        return this.files.modified(SHADERS + string);
    }

    static PackFiles folder(final File file) {
        return new PackFiles(){

            @Override
            public String read(String string) throws IOException {
                File file2 = new File(file, string);
                return file2.isFile() ? ShaderPack.plain(Files.readString(file2.toPath(), StandardCharsets.UTF_8)) : null;
            }

            @Override
            public long modified(String string) {
                File file2 = new File(file, string);
                return file2.isFile() ? file2.lastModified() : -1L;
            }
        };
    }

    static PackFiles jar(final String string) {
        final String string2 = "/viewpoint/shaderpacks/" + string + "/";
        final ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap();
        return new PackFiles(){

            @Override
            public String read(String string3) throws IOException {
                File file = ShaderPack.devFile(string, string3);
                if (file != null) {
                    return ShaderPack.plain(Files.readString(file.toPath(), StandardCharsets.UTF_8));
                }
                try (InputStream inputStream = ShaderPack.class.getResourceAsStream(string2 + string3);){
                    String string22 = inputStream == null ? null : ShaderPack.plain(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
                    return string22;
                }
            }

            @Override
            public long modified(String string3) {
                File file = ShaderPack.devFile(string, string3);
                if (file != null) {
                    return file.lastModified();
                }
                return concurrentHashMap.computeIfAbsent(string3, string2 -> ShaderPack.class.getResource(string2 + string2) != null) != false ? 0L : -1L;
            }
        };
    }

    private static File devFile(String string, String string2) {
        String string3;
        String string4 = string3 = Settings.dev ? Settings.shaderDir : null;
        if (string3 == null || string3.isEmpty()) {
            return null;
        }
        File file = new File(new File(new File(string3).getParentFile(), "shaderpacks"), string + "/" + string2);
        return file.isFile() ? file : null;
    }

    private static String plain(String string) {
        return string.replace("\r\n", "\n");
    }

    public String toString() {
        return this.id;
    }

    static interface PackFiles {
        public String read(String var1) throws IOException;

        public long modified(String var1);
    }
}

