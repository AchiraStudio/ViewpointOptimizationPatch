/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import viewpoint.light.BandGrids;
import viewpoint.light.LevelLight;
import viewpoint.light.LightLayouts;
import viewpoint.light.LightSources;
import viewpoint.light.LightWork;
import viewpoint.light.SourceLight;
import viewpoint.light.SourceRegister;
import viewpoint.platform.Caches;
import viewpoint.platform.LongMap;

final class LightJobs
implements Caches.Cache {
    private static final int SKY_CHUNKS = 3;
    private static final int MAX_IN_FLIGHT = 128;
    private static final int DISTANCES = 64;
    private static final int MAX_LOOKED = 1024;
    private static final LongMap<Held> levels = new LongMap(2048);
    private static final ArrayList<SourceRegister.Entry> dueSources = new ArrayList();
    private static final ArrayList<Held> dueLevels = new ArrayList();
    private static final ArrayList<SourceLight> reaching = new ArrayList();
    private static int inFlight;
    private static int generation;
    private static long levelBytes;
    private static final int[] counts;
    private static Held[] order;
    private static int[] orderDistance;
    static final ArrayList<LevelLight> worked;
    static int sourceJobs;
    static int levelJobs;
    static long sourceNanos;
    static long levelNanos;
    private static final ThreadPoolExecutor WORKERS;
    private static final ThreadLocal<LightWork> WORK;
    private static final ConcurrentLinkedQueue<SourceDone> SOURCES_DONE;
    private static final ConcurrentLinkedQueue<LevelDone> LEVELS_DONE;
    private static volatile boolean jobFailed;

    LightJobs() {
    }

    static void mark(int n, int n2, int n3) {
        Held held = levels.get(LightLayouts.key(n, n2, n3));
        if (held != null && !held.due) {
            held.due = true;
            dueLevels.add(held);
        }
    }

    static void layoutChanged(int n, int n2, int n3) {
        long l = LightLayouts.key(n, n2, n3);
        if (LightLayouts.get(n, n2, n3) == null) {
            LightJobs.drop(levels.remove(l));
        } else if (levels.get(l) == null) {
            Held held = new Held(n, n2, n3);
            levels.put(l, held);
            dueLevels.add(held);
        }
        for (int i = -3; i <= 3; ++i) {
            for (int j = -3; j <= 3; ++j) {
                LightJobs.mark(n + j, n2 + i, n3);
            }
        }
        LightSources.register().relayout(n, n2, n3);
    }

    static LevelLight light(int n, int n2, int n3) {
        Held held = levels.get(LightLayouts.key(n, n2, n3));
        return held == null ? null : held.light;
    }

    static void update(int n, int n2, int n3) {
        int n4;
        worked.clear();
        LightJobs.takeIn();
        SourceRegister sourceRegister = LightSources.register();
        dueSources.clear();
        sourceRegister.due(dueSources);
        dueSources.sort((entry, entry2) -> Integer.compare(LightJobs.distance(entry, n, n2, n3), LightJobs.distance(entry2, n, n2, n3)));
        for (n4 = 0; n4 < dueSources.size() && inFlight < 128; ++n4) {
            LightJobs.handOut(dueSources.get(n4));
        }
        if (inFlight >= 128) {
            return;
        }
        LightJobs.nearestFirst(n, n2, n3);
        n4 = 0;
        int n5 = 0;
        for (int i = 0; i < dueLevels.size(); ++i) {
            Held held = dueLevels.get(i);
            if (levels.get(LightLayouts.key(held.chunkX, held.chunkY, held.level)) != held) continue;
            reaching.clear();
            if (held.pending || inFlight >= 128 || n5++ >= 1024 || !sourceRegister.lights(held.chunkX, held.chunkY, held.level, reaching)) {
                dueLevels.set(n4++, held);
                continue;
            }
            LightJobs.handOut(held, List.copyOf(reaching));
        }
        dueLevels.subList(n4, dueLevels.size()).clear();
    }

    private static void handOut(SourceRegister.Entry entry) {
        SourceRegister.Lamp lamp = entry.lamp;
        SourceRegister.RoomLight roomLight = entry.room;
        SourceLight.Box box = lamp != null ? SourceLight.box(lamp) : SourceLight.box(roomLight);
        List<LightLayouts.Level> list = LightJobs.layouts(box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1());
        int n = entry.version;
        int n2 = entry.sourceVersion;
        int n3 = generation;
        entry.pending = true;
        ++inFlight;
        WORKERS.execute(() -> {
            long l = System.nanoTime();
            SourceLight sourceLight = SourceLight.NONE;
            try {
                LightWork lightWork = WORK.get();
                sourceLight = lamp != null ? SourceLight.lamp(lightWork, lamp, box, list) : SourceLight.room(lightWork, roomLight, box, list);
            }
            catch (Throwable throwable) {
                LightJobs.failed(throwable);
            }
            SOURCES_DONE.add(new SourceDone(entry, n, n2, n3, sourceLight, System.nanoTime() - l));
        });
    }

    private static void handOut(Held held, List<SourceLight> list) {
        int n = held.chunkX;
        int n2 = held.chunkY;
        int n3 = held.level;
        int n4 = generation;
        List<LightLayouts.Level> list2 = LightJobs.layouts(LevelLight.firstSquare(n), LevelLight.firstSquare(n2), n3, LevelLight.lastSquare(n), LevelLight.lastSquare(n2), n3);
        held.due = false;
        held.pending = true;
        ++inFlight;
        WORKERS.execute(() -> {
            long l = System.nanoTime();
            LevelLight levelLight = null;
            try {
                levelLight = LevelLight.work(WORK.get(), n, n2, n3, list2, list);
            }
            catch (Throwable throwable) {
                LightJobs.failed(throwable);
            }
            LEVELS_DONE.add(new LevelDone(held, n4, levelLight, System.nanoTime() - l));
        });
    }

    private static List<LightLayouts.Level> layouts(int n, int n2, int n3, int n4, int n5, int n6) {
        ArrayList<LightLayouts.Level> arrayList = new ArrayList<LightLayouts.Level>();
        for (int i = n3; i <= n6; ++i) {
            for (int j = Math.floorDiv(n2, 8); j <= Math.floorDiv(n5, 8); ++j) {
                for (int k = Math.floorDiv(n, 8); k <= Math.floorDiv(n4, 8); ++k) {
                    LightLayouts.Level level = LightLayouts.get(k, j, i);
                    if (level == null) continue;
                    arrayList.add(level);
                }
            }
        }
        return arrayList;
    }

    private static void takeIn() {
        Record record;
        SourceRegister sourceRegister = LightSources.register();
        while ((record = SOURCES_DONE.poll()) != null) {
            --inFlight;
            ++sourceJobs;
            sourceNanos += ((SourceDone)record).nanos();
            if (((SourceDone)record).generation() == generation) {
                sourceRegister.worked(((SourceDone)record).entry(), ((SourceDone)record).version(), ((SourceDone)record).sourceVersion(), ((SourceDone)record).light(), LightJobs::mark);
                continue;
            }
            ((SourceDone)record).entry().pending = false;
        }
        while ((record = LEVELS_DONE.poll()) != null) {
            --inFlight;
            ++levelJobs;
            levelNanos += ((LevelDone)record).nanos();
            Held held = ((LevelDone)record).held();
            held.pending = false;
            boolean bl = ((LevelDone)record).generation() == generation && levels.get(LightLayouts.key(held.chunkX, held.chunkY, held.level)) == held;
            if (!bl || ((LevelDone)record).light() == null) continue;
            levelBytes += ((LevelDone)record).light().bytes() - (held.light == null ? 0L : held.light.bytes());
            held.light = ((LevelDone)record).light();
            worked.add(((LevelDone)record).light());
        }
    }

    private static void nearestFirst(int n, int n2, int n3) {
        int n4;
        int n5 = dueLevels.size();
        if (order.length < n5) {
            order = new Held[n5 * 2];
            orderDistance = new int[n5 * 2];
        }
        Arrays.fill(counts, 0);
        for (n4 = 0; n4 < n5; ++n4) {
            int n6;
            Held held = dueLevels.get(n4);
            LightJobs.orderDistance[n4] = n6 = Math.min(64, LightJobs.distance(held.chunkX, held.chunkY, held.level, n, n2, n3));
            int n7 = n6 + 1;
            counts[n7] = counts[n7] + 1;
        }
        for (n4 = 0; n4 <= 64; ++n4) {
            int n8 = n4 + 1;
            counts[n8] = counts[n8] + counts[n4];
        }
        for (n4 = 0; n4 < n5; ++n4) {
            int n9 = orderDistance[n4];
            int n10 = counts[n9];
            counts[n9] = n10 + 1;
            LightJobs.order[n10] = dueLevels.get(n4);
        }
        for (n4 = 0; n4 < n5; ++n4) {
            dueLevels.set(n4, order[n4]);
            LightJobs.order[n4] = null;
        }
    }

    private static int distance(SourceRegister.Entry entry, int n, int n2, int n3) {
        int n4 = entry.lamp != null ? entry.lamp.x() : entry.room.x();
        int n5 = entry.lamp != null ? entry.lamp.y() : entry.room.y();
        int n6 = entry.lamp != null ? entry.lamp.z() : entry.room.z();
        return LightJobs.distance(Math.floorDiv(n4, 8), Math.floorDiv(n5, 8), n6, n, n2, n3);
    }

    private static int distance(int n, int n2, int n3, int n4, int n5, int n6) {
        return Math.max(Math.max(Math.abs(n - n4), Math.abs(n2 - n5)), Math.abs(n3 - n6));
    }

    private static void drop(Held held) {
        if (held != null && held.light != null) {
            levelBytes -= held.light.bytes();
            BandGrids.dropped(held.chunkX, held.chunkY, held.level);
        }
    }

    static void lights(Consumer<LevelLight> consumer) {
        for (Held held : levels.values()) {
            if (held.light == null) continue;
            consumer.accept(held.light);
        }
    }

    static void clear() {
        ++generation;
        levels.clear();
        dueLevels.clear();
        levelBytes = 0L;
        worked.clear();
    }

    @Override
    public long bytes() {
        return levelBytes + LightSources.register().lightBytes();
    }

    @Override
    public void list(Caches.Survey survey) {
    }

    @Override
    public void evictBefore(long l) {
    }

    private static void failed(Throwable throwable) {
        if (!jobFailed) {
            jobFailed = true;
            System.out.println("[Viewpoint] light: a job failed (a level keeps its light, a source lights nothing):");
            throwable.printStackTrace(System.out);
        }
    }

    static {
        counts = new int[66];
        order = new Held[256];
        orderDistance = new int[256];
        worked = new ArrayList();
        Caches.register(new LightJobs());
        WORKERS = new ThreadPoolExecutor(2, 2, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), runnable -> {
            Thread thread = new Thread(runnable, "Viewpoint light");
            thread.setDaemon(true);
            thread.setPriority(4);
            return thread;
        });
        WORK = ThreadLocal.withInitial(LightWork::new);
        SOURCES_DONE = new ConcurrentLinkedQueue();
        LEVELS_DONE = new ConcurrentLinkedQueue();
    }

    private static final class Held {
        final int chunkX;
        final int chunkY;
        final int level;
        LevelLight light;
        boolean due = true;
        boolean pending;

        Held(int n, int n2, int n3) {
            this.chunkX = n;
            this.chunkY = n2;
            this.level = n3;
        }
    }

    private record SourceDone(SourceRegister.Entry entry, int version, int sourceVersion, int generation, SourceLight light, long nanos) {
    }

    private record LevelDone(Held held, int generation, LevelLight light, long nanos) {
    }
}

