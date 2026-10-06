/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.erosion.season.ErosionIceQueen
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.fboRenderChunk.FBORenderLevels
 *  zombie.iso.objects.IsoWindow
 *  zombie.iso.objects.IsoWorldInventoryObject
 */
package viewpoint.world;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import viewpoint.core.Frame;
import viewpoint.light.LightSources;
import viewpoint.light.OwnedLight;
import viewpoint.models.Corpses;
import viewpoint.packs.ModelPacks;
import viewpoint.platform.Pipeline;
import viewpoint.platform.Profile;
import viewpoint.platform.ShaderPacks;
import viewpoint.render.ChunkMeshData;
import viewpoint.render.WorldRenderer;
import viewpoint.visibility.Rooms;
import viewpoint.world.BodyCards;
import viewpoint.world.ChunkBudget;
import viewpoint.world.ChunkBuilds;
import viewpoint.world.ChunkWalk;
import viewpoint.world.DrawList;
import viewpoint.world.Facades;
import viewpoint.world.FloorGather;
import viewpoint.world.FloorPages;
import viewpoint.world.ModelWaits;
import viewpoint.world.MutationInbox;
import viewpoint.world.TileMeshes;
import zombie.erosion.season.ErosionIceQueen;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;

public final class ChunkCache {
    public static final int RING = 5;
    private static final int EVICT_PERIOD_FRAMES = 240;
    static final int LEVEL_OFFSET = 32;
    static final int LEVEL_COUNT = 64;
    private static int lastSnowTarget = Integer.MIN_VALUE;
    private static boolean lastWinter;
    private static boolean lastBaked;
    private static int lastGrass;
    private static int lastPacks;
    static final int REASON_NEW = 0;
    static final int REASON_GAME = 1;
    static final int REASON_RETRY = 2;
    static final int REASON_RING = 3;
    static final int REASON_PACKS = 4;
    static long frameCounter;
    static final MutationInbox INBOX;
    private static final ArrayList<MutationInbox.Waiting> signals;
    private static final HashMap<IsoChunk, Entry> entries;
    private static final long GEOMETRY_DIRTY_FLAGS = 6111L;

    private static void watchSnow(IsoCell isoCell) {
        int n = isoCell.getSnowTarget();
        boolean bl = ErosionIceQueen.instance != null && ErosionIceQueen.instance.isSnow();
        boolean bl2 = FloorGather.BAKED.get();
        int n2 = TileMeshes.GRASS_CARDS.get();
        if (n != lastSnowTarget || bl != lastWinter || bl2 != lastBaked || n2 != lastGrass) {
            lastSnowTarget = n;
            lastWinter = bl;
            lastBaked = bl2;
            lastGrass = n2;
            ChunkCache.markAll(1);
        }
    }

    private static void watchPacks() {
        ModelPacks.frame();
        int n = ModelPacks.generation();
        if (n != lastPacks) {
            lastPacks = n;
            ChunkCache.markAll(4);
        }
        ModelWaits.frame();
    }

    public static void update(Frame frame, IsoCell isoCell, int n4) {
        ++frameCounter;
        ChunkBuilds.unchanged = 0;
        ChunkBuilds.builds = 0;
        ChunkBuilds.buildNanos = 0L;
        long l = System.nanoTime();
        IsoChunkMap isoChunkMap = isoCell.getChunkMap(n4);
        boolean bl = ChunkCache.takeSignals(isoChunkMap);
        LightSources.update(isoCell, n4);
        ChunkCache.watchSnow(isoCell);
        ChunkCache.watchPacks();
        int n5 = (int)Math.floor(frame.camZ);
        ChunkBuilds.installCooked(l);
        frame.scene.shadowReach = WorldRenderer.farShadowRadius(frame.scene.nearEnd());
        Pipeline pipeline = ShaderPacks.drawn().pipeline;
        boolean bl2 = frame.scene.sunStrength > 0.02f && pipeline.shadows;
        DrawList.begin(frame.scene, bl2);
        ChunkBudget.begin();
        Corpses.begin(frame);
        int n6 = ChunkWalk.walk(frame, isoCell, n4, n5, l, bl2);
        ChunkBuilds.buildQueued(System.nanoTime() + ChunkBuilds.buildBudget(n6), n4, isoCell, l);
        int n7 = Math.floorDiv((int)Math.floor(frame.camX), 8);
        int n8 = Math.floorDiv((int)Math.floor(frame.camY), 8);
        OwnedLight.update(n7, n8, n5, (n, n2, n3) -> ChunkCache.rewriteLight(isoChunkMap, n, n2, n3));
        Rooms.select(frame);
        Corpses.pick(frame);
        DrawList.commit(frame.scene, bl2, pipeline.shadowCascades);
        Rooms.endFrame();
        ChunkCache.makeRoom();
        Profile.chunks(ChunkBuilds.builds, ChunkBuilds.buildNanos, n6);
        Profile.chunkSignals(INBOX.takeSignalCount(), bl, ChunkBuilds.unchanged);
        if (frameCounter % 240L == 0L) {
            ChunkCache.evictUnseen();
            Rooms.sweep();
        }
    }

    private static boolean takeSignals(IsoChunkMap isoChunkMap) {
        if (!INBOX.drain(signals)) {
            ChunkCache.markAll(1);
            return true;
        }
        for (MutationInbox.Waiting waiting : signals) {
            IsoChunk isoChunk = isoChunkMap.getChunkForGridSquare(waiting.chunkX * 8, waiting.chunkY * 8);
            Entry entry = isoChunk == null ? null : entries.get(isoChunk);
            if (entry == null || entry.wx != waiting.chunkX || entry.wy != waiting.chunkY) continue;
            if (waiting.level == Integer.MIN_VALUE) {
                for (Level level : entry.levels) {
                    if (level == null) continue;
                    ChunkCache.apply(level, waiting);
                }
                continue;
            }
            if (waiting.level + 32 < 0 || waiting.level + 32 >= 64 || entry.levels[waiting.level + 32] == null) continue;
            ChunkCache.apply(entry.levels[waiting.level + 32], waiting);
        }
        signals.clear();
        return false;
    }

    private static void apply(Level level, MutationInbox.Waiting waiting) {
        if ((waiting.kinds & 1) != 0 && waiting.geometryStamp > level.gatherStamp) {
            ChunkCache.mark(level, 1);
        }
    }

    private static void rewriteLight(IsoChunkMap isoChunkMap, int n, int n2, int n3) {
        Level level = ChunkCache.level(isoChunkMap, n, n2, n3);
        if (level != null && level.data != null) {
            ChunkCache.ownLight(level, n, n2, n3);
        }
    }

    static void ownLight(Level level, int n, int n2, int n3) {
        ByteBuffer byteBuffer = level.data.spareLight();
        if (OwnedLight.write(byteBuffer, n, n2, n3)) {
            level.data.publishLight(byteBuffer);
        }
    }

    private static Level level(IsoChunkMap isoChunkMap, int n, int n2, int n3) {
        Entry entry;
        IsoChunk isoChunk = isoChunkMap.getChunkForGridSquare(n * 8, n2 * 8);
        Entry entry2 = entry = isoChunk == null ? null : entries.get(isoChunk);
        if (entry == null || entry.wx != n || entry.wy != n2 || n3 + 32 < 0 || n3 + 32 >= 64) {
            return null;
        }
        return entry.levels[n3 + 32];
    }

    private static void markAll(int n) {
        for (Entry entry : entries.values()) {
            for (Level level : entry.levels) {
                if (level == null) continue;
                ChunkCache.mark(level, n);
            }
        }
    }

    static float fadeOf(Level level, long l) {
        if (level.fadeNanos <= 0L) {
            return 1.0f;
        }
        float f = (float)(l - level.fadeStart) / (float)level.fadeNanos;
        if (f < 1.0f) {
            return Math.max(f, 0.0f);
        }
        level.fadeNanos = 0L;
        ChunkBudget.letGo(level.fading);
        level.fading = null;
        level.fadingModelled = null;
        level.fadingModelledItems = null;
        level.fadingBodies = null;
        return 1.0f;
    }

    public static void clear() {
        Facades.clear();
        INBOX.clear();
        Rooms.clear();
        OwnedLight.clear();
        FloorPages.clear();
        ModelWaits.clear();
        Iterator<Entry> iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            ChunkCache.retire(iterator.next());
            iterator.remove();
        }
        ChunkWalk.clear();
    }

    public static ChunkMeshData meshOn(IsoGridSquare isoGridSquare) {
        IsoChunk isoChunk = isoGridSquare.getChunk();
        Entry entry = isoChunk == null ? null : ChunkCache.existing(isoChunk);
        int n = isoGridSquare.z + 32;
        Level level = entry == null || n < 0 || n >= 64 ? null : entry.levels[n];
        return level == null ? null : level.data;
    }

    public static boolean packsOn(IsoGridSquare isoGridSquare) {
        IsoChunk isoChunk = isoGridSquare.getChunk();
        Entry entry = isoChunk == null ? null : ChunkCache.existing(isoChunk);
        int n = isoGridSquare.z + 32;
        Level level = entry == null || n < 0 || n >= 64 ? null : entry.levels[n];
        return level != null && level.gatheredPacksNear;
    }

    static Entry existing(IsoChunk isoChunk) {
        Entry entry = entries.get(isoChunk);
        return entry != null && entry.wx == isoChunk.wx && entry.wy == isoChunk.wy ? entry : null;
    }

    static Entry entryFor(IsoChunk isoChunk) {
        Entry entry = entries.get(isoChunk);
        if (entry != null && (entry.wx != isoChunk.wx || entry.wy != isoChunk.wy)) {
            ChunkCache.retire(entry);
            entry = null;
        }
        if (entry == null) {
            entry = new Entry();
            entry.wx = isoChunk.wx;
            entry.wy = isoChunk.wy;
            entries.put(isoChunk, entry);
        }
        return entry;
    }

    private static void evictUnseen() {
        Facades.clear();
        ChunkCache.drop(240L);
    }

    private static void makeRoom() {
        if (ChunkBudget.full()) {
            ChunkCache.drop(1L);
        }
        ChunkBudget.makeRoom();
    }

    private static void drop(long l) {
        Iterator<Entry> iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (frameCounter - entry.lastSeenFrame < l) continue;
            ChunkCache.retire(entry);
            iterator.remove();
        }
    }

    private static void retire(Entry entry) {
        Rooms.forget(entry.wx, entry.wy);
        OwnedLight.forget(entry.wx, entry.wy);
        for (int i = 0; i < 64; ++i) {
            Level level = entry.levels[i];
            if (level == null) continue;
            FloorPages.remove(entry.wx, entry.wy, i - 32);
            level.evicted = true;
            ChunkBudget.letGo(level.data);
            ChunkBudget.letGo(level.fading);
            level.fading = null;
            level.data = null;
        }
    }

    static void mark(Level level, int n) {
        level.dirty = true;
        level.reason = n;
        level.retryFrame = 0L;
        level.retryDelay = 0;
    }

    public static void invalidated(FBORenderLevels fBORenderLevels, int n, long l) {
        if ((l & 0x17DFL) == 0L || fBORenderLevels.getPlayerIndex() != 0) {
            return;
        }
        IsoChunk isoChunk = fBORenderLevels.getChunk();
        INBOX.record(isoChunk.wx, isoChunk.wy, n, 1);
    }

    public static void windowChanged(IsoWindow isoWindow) {
        IsoChunk isoChunk;
        IsoGridSquare isoGridSquare = isoWindow.getSquare();
        IsoChunk isoChunk2 = isoChunk = isoGridSquare == null ? null : isoGridSquare.getChunk();
        if (isoChunk != null) {
            INBOX.record(isoChunk.wx, isoChunk.wy, isoGridSquare.getZ(), 1);
        }
    }

    private ChunkCache() {
    }

    static {
        lastBaked = true;
        INBOX = new MutationInbox();
        signals = new ArrayList();
        entries = new HashMap();
    }

    static final class Entry {
        int wx;
        int wy;
        long lastSeenFrame;
        long leaveStart;
        final Level[] levels = new Level[64];

        Entry() {
        }
    }

    static final class Level {
        ChunkMeshData data;
        long version;
        boolean gatheredModelsNear;
        boolean evicted;
        boolean dirty = true;
        boolean modelsNear;
        boolean packsNear;
        boolean gatheredPacksNear;
        long fingerprint;
        long gatherStamp;
        ArrayList<IsoObject> modelled;
        ArrayList<IsoWorldInventoryObject> modelledItems;
        BodyCards bodies;
        ChunkMeshData fading;
        ArrayList<IsoObject> fadingModelled;
        ArrayList<IsoWorldInventoryObject> fadingModelledItems;
        BodyCards fadingBodies;
        long fadeStart;
        long fadeNanos;
        long retryFrame;
        int retryDelay;
        int reason = 0;

        Level() {
        }
    }
}

