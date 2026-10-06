/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoCell
 */
package viewpoint.world;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import viewpoint.light.LightLayouts;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.Profile;
import viewpoint.render.ChunkMeshData;
import viewpoint.visibility.Rooms;
import viewpoint.world.ChunkBudget;
import viewpoint.world.ChunkCache;
import viewpoint.world.Cook;
import viewpoint.world.FloorPages;
import viewpoint.world.ModelWaits;
import viewpoint.world.Recipe;
import viewpoint.world.WorldMesher;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;

final class ChunkBuilds {
    static final LiveSettings.Number BUILD_MIN = LiveSettings.number("chunks.buildBudgetMs", "Build budget (ms)", "World/Chunks", 0.5f, 20.0f, 0.5f, 3.0f);
    static final LiveSettings.Number BUILD_PER_WAITING = LiveSettings.number("chunks.buildPerWaitingMs", "More per level waiting (ms)", "World/Chunks", 0.0f, 1.0f, 0.05f, 0.15f);
    static final LiveSettings.Number BUILD_MAX = LiveSettings.number("chunks.buildMaxMs", "Build budget at most (ms)", "World/Chunks", 1.0f, 30.0f, 0.5f, 9.0f);
    private static final int RETRY_FIRST = 10;
    private static final int RETRY_PICTURE = 2;
    private static final int RETRY_MAX = 240;
    static final long APPEAR_NANOS = 600000000L;
    static final long CROSS_NANOS = 400000000L;
    static final long NEAR_CROSS_NANOS = 150000000L;
    private static int cooking;
    private static final LiveSettings.Number MAX_COOKING;
    static int builds;
    static int unchanged;
    static long buildNanos;
    private static final ArrayList<Queued> queue;
    private static int queued;
    private static final LiveSettings.Number COOK_THREADS;
    private static final ThreadPoolExecutor COOKS;
    private static final ConcurrentLinkedQueue<Cooked> COOKED;
    private static volatile boolean cookFailed;

    static long buildBudget(int n) {
        return (long)((double)Math.min(BUILD_MAX.get(), BUILD_MIN.get() + (float)n * BUILD_PER_WAITING.get()) * 1000000.0);
    }

    static void queue(ChunkCache.Level level, IsoChunk isoChunk, int n, boolean bl, float f) {
        if (queued == queue.size()) {
            queue.add(new Queued());
        }
        Queued queued = queue.get(ChunkBuilds.queued++);
        queued.l = level;
        queued.chunk = isoChunk;
        queued.level = n;
        queued.modelsNear = bl;
        queued.priority = f;
    }

    static void buildQueued(long l, int n, IsoCell isoCell, long l2) {
        queue.subList(0, ChunkBuilds.queued).sort((queued, queued2) -> Float.compare(queued.priority, queued2.priority));
        ChunkBuilds.resizeCooks();
        int n2 = MAX_COOKING.getInt();
        for (int i = 0; i < ChunkBuilds.queued; ++i) {
            Queued queued3 = queue.get(i);
            if ((i == 0 || System.nanoTime() < l) && cooking < n2) {
                ChunkBuilds.rebuild(queued3.l, queued3.chunk, queued3.level, n, queued3.modelsNear, isoCell, l2, 400000000L, false);
            }
            queued3.l = null;
            queued3.chunk = null;
        }
        ChunkBuilds.queued = 0;
    }

    static void rebuild(ChunkCache.Level level, IsoChunk isoChunk, int n, int n2, boolean bl, IsoCell isoCell, long l, long l2, boolean bl2) {
        long l3 = System.nanoTime();
        ++Profile.rebuilds;
        Profile.rebuildReason(level.data == null && level.fading == null ? 0 : level.reason);
        level.dirty = false;
        level.gatherStamp = ChunkCache.INBOX.stamp();
        Recipe recipe = WorldMesher.gather(isoChunk, n, n2, bl, level.packsNear);
        level.gatheredPacksNear = level.packsNear;
        Rooms.captured(isoChunk, n);
        LightLayouts.captured(isoCell, isoChunk, n);
        long l4 = ++level.version;
        level.gatheredModelsNear = bl;
        ModelWaits.gathered(level, recipe.waiting);
        if (recipe.pending) {
            level.dirty = true;
            level.reason = 2;
            int n3 = recipe.picturePending ? 2 : 10;
            level.retryDelay = level.retryDelay == 0 ? n3 : Math.min(240, level.retryDelay * 2);
            level.retryFrame = ChunkCache.frameCounter + (long)level.retryDelay;
        } else {
            level.retryDelay = 0;
        }
        if (bl2 && recipe.fingerprint() == level.fingerprint) {
            ChunkBuilds.keep(level, isoChunk, n, recipe, bl);
        } else if (bl2) {
            ChunkBuilds.install(level, isoChunk, n, recipe, Cook.mesh(recipe), bl, l, l2);
        } else {
            ChunkBuilds.cookLater(level, isoChunk, n, l4, recipe, bl, l2);
        }
        ++builds;
        buildNanos += System.nanoTime() - l3;
    }

    static void installCooked(long l) {
        Cooked cooked;
        while ((cooked = COOKED.poll()) != null) {
            boolean bl;
            --cooking;
            boolean bl2 = bl = !cooked.failed() && !cooked.l().evicted && cooked.version() == cooked.l().version;
            if (bl && cooked.recipe().fingerprint() == cooked.l().fingerprint) {
                ChunkBuilds.keep(cooked.l(), cooked.chunk(), cooked.level(), cooked.recipe(), cooked.modelsNear());
                continue;
            }
            if (bl) {
                ChunkBuilds.install(cooked.l(), cooked.chunk(), cooked.level(), cooked.recipe(), cooked.data(), cooked.modelsNear(), l, cooked.crossNanos());
                continue;
            }
            if (cooked.data() == null) continue;
            cooked.data().discard();
        }
        Profile.cooking = cooking;
    }

    private static void install(ChunkCache.Level level, IsoChunk isoChunk, int n, Recipe recipe, ChunkMeshData chunkMeshData, boolean bl, long l, long l2) {
        ChunkBudget.letGo(level.fading);
        if (level.fading == null && level.fadeNanos > 0L && l - level.fadeStart < level.fadeNanos) {
            ChunkBudget.letGo(level.data);
            level.fading = null;
            level.fadingModelled = null;
            level.fadingModelledItems = null;
            level.fadingBodies = null;
        } else {
            level.fading = level.data;
            level.fadingModelled = level.modelsNear ? level.modelled : null;
            level.fadingModelledItems = level.modelsNear ? level.modelledItems : null;
            level.fadingBodies = level.bodies;
            level.fadeStart = l;
            level.fadeNanos = level.fading == null ? 600000000L : l2;
        }
        level.modelsNear = bl;
        level.modelled = recipe.modelled;
        level.modelledItems = recipe.modelledItems;
        level.bodies = recipe.bodies;
        level.data = chunkMeshData;
        ChunkBudget.hold(chunkMeshData);
        level.fingerprint = recipe.fingerprint();
        FloorPages.put(isoChunk.wx, isoChunk.wy, n, recipe.floor);
        if (chunkMeshData != null) {
            ChunkCache.ownLight(level, isoChunk.wx, isoChunk.wy, n);
        }
    }

    private static void keep(ChunkCache.Level level, IsoChunk isoChunk, int n, Recipe recipe, boolean bl) {
        level.modelsNear = bl;
        level.modelled = recipe.modelled;
        level.modelledItems = recipe.modelledItems;
        level.bodies = recipe.bodies;
        FloorPages.put(isoChunk.wx, isoChunk.wy, n, recipe.floor);
        ++unchanged;
    }

    private static void cookLater(ChunkCache.Level level, IsoChunk isoChunk, int n, long l, Recipe recipe, boolean bl, long l2) {
        long l3 = level.fingerprint;
        ++cooking;
        COOKS.execute(() -> {
            boolean bl2;
            ChunkMeshData chunkMeshData;
            block2: {
                chunkMeshData = null;
                bl2 = false;
                try {
                    chunkMeshData = recipe.fingerprint() == l3 ? null : Cook.mesh(recipe);
                }
                catch (Throwable throwable) {
                    bl2 = true;
                    if (cookFailed) break block2;
                    cookFailed = true;
                    System.out.println("[Viewpoint] mesher: a level failed to cook (it keeps its old mesh):");
                    throwable.printStackTrace(System.out);
                }
            }
            COOKED.add(new Cooked(level, isoChunk, n, l, recipe, chunkMeshData, bl, l2, bl2));
        });
    }

    private static void resizeCooks() {
        int n = COOK_THREADS.getInt();
        if (n > COOKS.getMaximumPoolSize()) {
            COOKS.setMaximumPoolSize(n);
            COOKS.setCorePoolSize(n);
        } else if (n < COOKS.getCorePoolSize()) {
            COOKS.setCorePoolSize(n);
            COOKS.setMaximumPoolSize(n);
        }
    }

    private ChunkBuilds() {
    }

    static {
        MAX_COOKING = LiveSettings.number("chunks.cookingMax", "Levels cooking at once, at most", "World/Chunks", 16.0f, 1024.0f, 16.0f, 256.0f);
        queue = new ArrayList();
        COOK_THREADS = LiveSettings.number("chunks.cookThreads", "Mesher worker threads", "World/Chunks", 1.0f, 8.0f, 1.0f, Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2 - 1)));
        COOKS = new ThreadPoolExecutor(COOK_THREADS.getInt(), COOK_THREADS.getInt(), 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), runnable -> {
            Thread thread = new Thread(runnable, "Viewpoint mesher");
            thread.setDaemon(true);
            thread.setPriority(4);
            return thread;
        });
        COOKED = new ConcurrentLinkedQueue();
    }

    private static final class Queued {
        ChunkCache.Level l;
        IsoChunk chunk;
        int level;
        boolean modelsNear;
        float priority;

        private Queued() {
        }
    }

    private record Cooked(ChunkCache.Level l, IsoChunk chunk, int level, long version, Recipe recipe, ChunkMeshData data, boolean modelsNear, long crossNanos, boolean failed) {
    }
}

