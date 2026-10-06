/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class HotReload {
    public static volatile int generation;
    public static final String DIR_PROPERTY = "viewpoint.hotReloadDir";
    private static final String PREFIX = "viewpoint/";
    private static final String CLASS = ".class";
    private static final String ROOT = "viewpoint.";
    private static final long POLL_MILLIS = 250L;
    private static boolean started;

    public static void start() {
        String string = System.getProperty(DIR_PROPERTY);
        if (string == null || string.isBlank() || started) {
            return;
        }
        started = true;
        try {
            Path path = Path.of(string.trim(), new String[0]).toAbsolutePath().normalize();
            Path path2 = path.resolve("requests");
            Path path3 = path.resolve("results");
            HotReload.empty(Files.createDirectories(path2, new FileAttribute[0]));
            HotReload.empty(Files.createDirectories(path3, new FileAttribute[0]));
            Instrumentation instrumentation = HotReload.instrumentation();
            Map<String, byte[]> map = HotReload.classes(Path.of(HotReload.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
            Thread thread = new Thread(() -> HotReload.watch(instrumentation, map, path2, path3), "Viewpoint reload");
            thread.setDaemon(true);
            thread.start();
            System.out.println("[Viewpoint] hot reload ready: scripts/Reload-Mod.ps1 sends the changed classes to " + String.valueOf(path));
        }
        catch (Throwable throwable) {
            System.out.println("[Viewpoint] hot reload unavailable: " + String.valueOf(throwable));
        }
    }

    private static Instrumentation instrumentation() throws ReflectiveOperationException {
        Instrumentation instrumentation;
        Field field = Class.forName("me.zed_0xff.zombie_buddy.Loader").getDeclaredField("g_instrumentation");
        field.setAccessible(true);
        Object object = field.get(null);
        if (!(object instanceof Instrumentation) || !(instrumentation = (Instrumentation)object).isRedefineClassesSupported()) {
            throw new IllegalStateException("ZombieBuddy gives no instrumentation that can redefine classes");
        }
        return instrumentation;
    }

    private static void watch(Instrumentation instrumentation, Map<String, byte[]> map, Path path2, Path path3) {
        TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>(map);
        while (true) {
            try {
                while (true) {
                    List<Path> list;
                    try (Stream<Path> stream = Files.list(path2);){
                        list = stream.filter(path -> path.toString().endsWith(".jar")).sorted().toList();
                    }
                    for (Path path4 : list) {
                        String string = path4.getFileName().toString();
                        String string2 = string.substring(0, string.length() - 4);
                        Outcome outcome = HotReload.apply(instrumentation, map, treeMap, path4);
                        HotReload.write(path3.resolve(string2 + ".json"), outcome.json(string2));
                        Files.deleteIfExists(path4);
                        System.out.println("[Viewpoint] hot reload " + outcome.status + " (" + string2 + "): " + outcome.message + (String)(outcome.classes.isEmpty() ? "" : ": " + String.join((CharSequence)", ", outcome.classes)));
                    }
                    Thread.sleep(250L);
                }
            }
            catch (InterruptedException interruptedException) {
                return;
            }
            catch (Throwable throwable) {
                System.out.println("[Viewpoint] hot reload watcher: " + String.valueOf(throwable));
                HotReload.pause();
                continue;
            }
            break;
        }
    }

    private static Outcome apply(Instrumentation instrumentation, Map<String, byte[]> map, Map<String, byte[]> map2, Path path) {
        try {
            Map<String, byte[]> map3 = HotReload.classes(path);
            Map<String, Class<?>> map4 = HotReload.loaded(instrumentation);
            List<String> list = HotReload.problems(map, map2, map3, string -> map4.containsKey(string) && instrumentation.isModifiableClass((Class)map4.get(string)));
            if (!list.isEmpty()) {
                return new Outcome("RESTART_REQUIRED", String.join((CharSequence)"; ", list), List.of());
            }
            ArrayList<ClassDefinition> arrayList = new ArrayList<ClassDefinition>();
            ArrayList<String> arrayList2 = new ArrayList<String>();
            for (Map.Entry<String, byte[]> entry : map3.entrySet()) {
                if (Arrays.equals(map2.get(entry.getKey()), entry.getValue())) continue;
                String string2 = HotReload.binaryName(entry.getKey());
                arrayList.add(new ClassDefinition(map4.get(string2), entry.getValue()));
                arrayList2.add(string2);
            }
            if (arrayList.isEmpty()) {
                return new Outcome("NO_CHANGES", "the classes match those running", List.of());
            }
            instrumentation.redefineClasses((ClassDefinition[])arrayList.toArray(ClassDefinition[]::new));
            map2.clear();
            map2.putAll(map3);
            ++generation;
            return new Outcome("APPLIED", arrayList.size() + " classes redefined; the caches start over", arrayList2);
        }
        catch (LinkageError | UnsupportedOperationException throwable) {
            return new Outcome("RESTART_REQUIRED", "the JVM refused a change it cannot make while running: " + throwable.getMessage(), List.of());
        }
        catch (Throwable throwable) {
            return new Outcome("FAILED", String.valueOf(throwable), List.of());
        }
    }

    static List<String> problems(Map<String, byte[]> map, Map<String, byte[]> map2, Map<String, byte[]> map3, Predicate<String> predicate) {
        ArrayList<String> arrayList = new ArrayList<String>();
        for (String object : map.keySet()) {
            if (map3.containsKey(object)) continue;
            arrayList.add("a class removed: " + HotReload.binaryName(object));
        }
        for (Map.Entry entry : map3.entrySet()) {
            String string = HotReload.binaryName((String)entry.getKey());
            if (!map.containsKey(entry.getKey())) {
                arrayList.add("a class added: " + string);
                continue;
            }
            if (Arrays.equals(map2.get(entry.getKey()), (byte[])entry.getValue())) continue;
            if (HotReload.restartOnly(string)) {
                arrayList.add("a patch or bootstrap class changed: " + string);
                continue;
            }
            if (predicate.test(string)) continue;
            arrayList.add("changed but not loaded yet (it would load from the jar the game started with): " + string);
        }
        return arrayList;
    }

    static boolean restartOnly(String string) {
        return string.startsWith("viewpoint.Patch_") || string.equals("viewpoint.Main") || string.equals(HotReload.class.getName()) || string.startsWith(HotReload.class.getName() + "$");
    }

    private static Map<String, byte[]> classes(Path path) throws IOException {
        TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>();
        try (JarFile jarFile = new JarFile(path.toFile());){
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                if (!jarEntry.getName().startsWith(PREFIX) || !jarEntry.getName().endsWith(CLASS)) continue;
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                try {
                    treeMap.put(jarEntry.getName(), inputStream.readAllBytes());
                }
                finally {
                    if (inputStream == null) continue;
                    inputStream.close();
                }
            }
        }
        return treeMap;
    }

    private static Map<String, Class<?>> loaded(Instrumentation instrumentation) {
        HashMap hashMap = new HashMap();
        for (Class clazz : instrumentation.getAllLoadedClasses()) {
            if (!clazz.getName().startsWith(ROOT)) continue;
            hashMap.put(clazz.getName(), clazz);
        }
        return hashMap;
    }

    static String binaryName(String string) {
        return string.substring(0, string.length() - CLASS.length()).replace('/', '.');
    }

    private static void write(Path path, String string) throws IOException {
        Path path2 = path.resolveSibling(String.valueOf(path.getFileName()) + ".part");
        Files.writeString(path2, (CharSequence)(string + "\n"), StandardCharsets.UTF_8, new OpenOption[0]);
        Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void empty(Path path) throws IOException {
        try (Stream<Path> stream = Files.list(path);){
            for (Path path2 : stream.toList()) {
                Files.deleteIfExists(path2);
            }
        }
    }

    private static void pause() {
        try {
            Thread.sleep(1000L);
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private HotReload() {
    }

    record Outcome(String status, String message, List<String> classes) {
        String json(String string) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String string2 : this.classes) {
                stringBuilder.append(stringBuilder.isEmpty() ? "" : ",").append('\"').append(Outcome.escape(string2)).append('\"');
            }
            return "{\"requestId\":\"" + Outcome.escape(string) + "\",\"status\":\"" + this.status + "\",\"message\":\"" + Outcome.escape(this.message) + "\",\"classes\":[" + String.valueOf(stringBuilder) + "]}";
        }

        private static String escape(String string) {
            return string.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        }
    }
}

