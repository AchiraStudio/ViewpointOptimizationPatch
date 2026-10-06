/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import viewpoint.iris.SourceFiles;

public final class PackSource
implements Closeable,
SourceFiles {
    private static final String SHADERS = "shaders";
    private final String name;
    private final ZipFile zip;
    private final File folder;
    private final Map<String, String> entries = new TreeMap<String, String>();
    private final Map<String, String> lowerCase = new HashMap<String, String>();
    private final Map<String, String> texts = new HashMap<String, String>();

    private PackSource(String string, ZipFile zipFile, File file) {
        this.name = string;
        this.zip = zipFile;
        this.folder = file;
    }

    public static PackSource open(File file) throws IOException {
        String string = file.getName();
        if (file.isFile()) {
            String string2 = string.toLowerCase(Locale.ROOT).endsWith(".zip") ? string.substring(0, string.length() - 4) : string;
            PackSource packSource = new PackSource(string2, new ZipFile(file, StandardCharsets.UTF_8), null);
            packSource.indexZip();
            return packSource;
        }
        PackSource packSource = new PackSource(string, null, file);
        packSource.indexFolder();
        return packSource;
    }

    public String name() {
        return this.name;
    }

    public Set<String> paths() {
        return Collections.unmodifiableSet(this.entries.keySet());
    }

    public boolean exists(String string) {
        return this.entry(string) != null;
    }

    @Override
    public String read(String string) throws IOException {
        String string2 = this.entry(string);
        if (string2 == null) {
            return null;
        }
        String string3 = this.texts.get(string2);
        if (string3 == null) {
            string3 = new String(this.bytes(string2), StandardCharsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n');
            if (!string3.isEmpty() && string3.charAt(0) == '\ufeff') {
                string3 = string3.substring(1);
            }
            this.texts.put(string2, string3);
        }
        return string3;
    }

    public byte[] readBytes(String string) throws IOException {
        String string2 = this.entry(string);
        return string2 == null ? null : this.bytes(string2);
    }

    @Override
    public void close() throws IOException {
        if (this.zip != null) {
            this.zip.close();
        }
    }

    private String entry(String string) {
        String string2 = PackSource.normalize(string);
        if (string2 == null) {
            return null;
        }
        return this.entries.containsKey(string2) ? string2 : this.lowerCase.get(string2.toLowerCase(Locale.ROOT));
    }

    private byte[] bytes(String string) throws IOException {
        String string2 = this.entries.get(string);
        if (this.zip != null) {
            ZipEntry zipEntry = this.zip.getEntry(string2);
            try (InputStream inputStream = this.zip.getInputStream(zipEntry);){
                byte[] byArray = inputStream.readAllBytes();
                return byArray;
            }
        }
        return Files.readAllBytes(new File(string2).toPath());
    }

    public static String normalize(String string) {
        ArrayDeque<String> arrayDeque = new ArrayDeque<String>();
        for (String string2 : string.replace('\\', '/').split("/")) {
            if (string2.isEmpty() || string2.equals(".")) continue;
            if (string2.equals("..")) {
                if (arrayDeque.isEmpty()) {
                    return null;
                }
                arrayDeque.removeLast();
                continue;
            }
            arrayDeque.addLast(string2);
        }
        return String.join((CharSequence)"/", arrayDeque);
    }

    private void indexZip() throws IOException {
        String string;
        String string2 = null;
        for (ZipEntry zipEntry : Collections.list(this.zip.entries())) {
            String string3;
            string = zipEntry.getName().replace('\\', '/');
            if (zipEntry.isDirectory() || !PackSource.isMarker(string) || !(string3 = string.substring(0, string.lastIndexOf(47) + 1)).endsWith("shaders/") || string2 != null && PackSource.depth(string3) >= PackSource.depth(string2)) continue;
            string2 = string3;
        }
        if (string2 == null) {
            throw new IOException("no shaders folder with shaders.properties or a program");
        }
        for (ZipEntry zipEntry : Collections.list(this.zip.entries())) {
            string = zipEntry.getName().replace('\\', '/');
            if (zipEntry.isDirectory() || !string.startsWith(string2)) continue;
            this.add(string.substring(string2.length()), zipEntry.getName());
        }
    }

    private void indexFolder() throws IOException {
        File file = PackSource.shallowest(this.folder, 0);
        if (file == null) {
            throw new IOException("no shaders folder with shaders.properties or a program");
        }
        this.indexFiles(file, "");
    }

    private static File shallowest(File file, int n) {
        File[] fileArray = file.listFiles();
        if (fileArray == null || n > 4) {
            return null;
        }
        if (file.getName().equals(SHADERS)) {
            for (File file2 : fileArray) {
                if (!file2.isFile() || !PackSource.isMarker("shaders/" + file2.getName())) continue;
                return file;
            }
        }
        Object object = null;
        for (File file3 : fileArray) {
            File file4;
            if (!file3.isDirectory() || (file4 = PackSource.shallowest(file3, n + 1)) == null || object != null && file4.getPath().length() >= ((File)object).getPath().length()) continue;
            object = file4;
        }
        return object;
    }

    private void indexFiles(File file, String string) {
        File[] fileArray = file.listFiles();
        if (fileArray == null) {
            return;
        }
        for (File file2 : fileArray) {
            String string2 = string + file2.getName();
            if (file2.isDirectory()) {
                this.indexFiles(file2, string2 + "/");
                continue;
            }
            this.add(string2, file2.getPath());
        }
    }

    private void add(String string, String string2) {
        this.entries.put(string, string2);
        this.lowerCase.putIfAbsent(string.toLowerCase(Locale.ROOT), string);
    }

    private static boolean isMarker(String string) {
        String string2 = string.substring(string.lastIndexOf(47) + 1);
        return string2.equals("shaders.properties") || string2.matches("(final|composite\\d*|gbuffers_\\w+|deferred\\d*)\\.(fsh|vsh)");
    }

    private static int depth(String string) {
        return string.length() - string.replace("/", "").length();
    }
}

