/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import viewpoint.platform.Caches;

final class Lotpacks
implements Caches.Cache {
    private static final int KEEP = 4;
    private static final LinkedHashMap<String, Entry> kept = new LinkedHashMap(8, 0.75f, true);
    private static long held;
    private static final ConcurrentLinkedQueue<Read> READ;

    Lotpacks() {
    }

    static byte[] get(String string) {
        Entry entry = kept.get(string);
        if (entry == null) {
            return null;
        }
        entry.used = Caches.tick();
        return entry.bytes;
    }

    static byte[] read(String string, byte[] byArray) throws IOException {
        if (byArray != null) {
            return byArray;
        }
        byte[] byArray2 = Files.readAllBytes(new File(string).toPath());
        READ.add(new Read(string, byArray2));
        return byArray2;
    }

    static void takeIn() {
        Object object;
        while ((object = READ.poll()) != null) {
            if (kept.containsKey(((Read)object).path)) continue;
            kept.put(((Read)object).path, new Entry(((Read)object).bytes));
            held += (long)((Read)object).bytes.length;
        }
        object = kept.values().iterator();
        while (kept.size() > 4 && object.hasNext()) {
            held -= (long)((Entry)object.next()).bytes.length;
            object.remove();
        }
    }

    static void clear() {
        kept.clear();
        READ.clear();
        held = 0L;
    }

    @Override
    public long bytes() {
        return held;
    }

    @Override
    public void list(Caches.Survey survey) {
        for (Entry entry : kept.values()) {
            survey.add(entry.used, entry.bytes.length);
        }
    }

    @Override
    public void evictBefore(long l) {
        Iterator<Entry> iterator = kept.values().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (entry.used >= l) continue;
            held -= (long)entry.bytes.length;
            iterator.remove();
        }
    }

    static {
        Caches.register(new Lotpacks());
        READ = new ConcurrentLinkedQueue();
    }

    private static final class Entry {
        final byte[] bytes;
        long used;

        Entry(byte[] byArray) {
            this.bytes = byArray;
            this.used = Caches.tick();
        }
    }

    private record Read(String path, byte[] bytes) {
    }
}

