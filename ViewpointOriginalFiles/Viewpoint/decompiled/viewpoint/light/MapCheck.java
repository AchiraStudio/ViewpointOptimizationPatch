/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoCell
 */
package viewpoint.light;

import java.util.ArrayList;
import viewpoint.light.LightLayout;
import viewpoint.light.LightLayouts;
import viewpoint.light.LightSources;
import viewpoint.light.MapSources;
import viewpoint.light.SourceRegister;
import viewpoint.platform.LongMap;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;

final class MapCheck {
    private static final int REPORTED = 40;
    private static final long CHECK_NANOS = 5000000000L;
    private static long levels;
    private static long squares;
    private static long sameExists;
    private static long sameFlags;
    private static long samePasses;
    private static long sameRooms;
    private static int reported;
    private static int gameLamps;
    private static int foundLamps;
    private static int sameLamps;
    private static int sameLampsOn;
    private static int gameRooms;
    private static int foundRooms;
    private static int sameRoomsOn;
    private static long lastSources;
    private static final LongMap<SourceRegister.Lamp> mapLamps;
    private static final LongMap<Boolean> mapLampsOn;
    private static final LongMap<Boolean> mapRoomsOn;
    private static final ArrayList<SourceRegister.Entry> entries;

    static void layout(LightLayouts.Level level, LightLayouts.Level level2) {
        if (level == null || level2 == null) {
            return;
        }
        ++levels;
        for (int i = 0; i < 64; ++i) {
            boolean bl;
            boolean bl2 = (level.flags[i] & 1) != 0;
            boolean bl3 = bl = (level2.flags[i] & 1) != 0;
            if (!bl2 && !bl) continue;
            ++squares;
            boolean bl4 = bl2 == bl;
            boolean bl5 = level.flags[i] == level2.flags[i];
            boolean bl6 = level.passes[i] == level2.passes[i];
            boolean bl7 = level.rooms[i] == level2.rooms[i];
            sameExists += bl4 ? 1L : 0L;
            sameFlags += bl5 ? 1L : 0L;
            samePasses += bl6 ? 1L : 0L;
            sameRooms += bl7 ? 1L : 0L;
            if (bl4 && bl5 && bl6 && bl7 || reported >= 40) continue;
            ++reported;
            System.out.println("[Viewpoint] map layout differs at " + (level.chunkX * 8 + i % 8) + "," + (level.chunkY * 8 + i / 8) + "," + level.level + ": flags map " + level.flags[i] + " game " + level2.flags[i] + ", passes map " + MapCheck.passes(level.passes[i]) + " game " + MapCheck.passes(level2.passes[i]) + ", room map " + level.rooms[i] + " game " + level2.rooms[i]);
        }
    }

    static void sources(IsoCell isoCell) {
        long l = System.nanoTime();
        if (l - lastSources < 5000000000L) {
            return;
        }
        lastSources = l;
        mapLamps.clear();
        mapLampsOn.clear();
        mapRoomsOn.clear();
        MapSources.lamps(mapLamps, mapLampsOn);
        MapSources.rooms(mapRoomsOn);
        entries.clear();
        SourceRegister sourceRegister = LightSources.register();
        sourceRegister.engineLamps(entries);
        sameLampsOn = 0;
        sameLamps = 0;
        foundLamps = 0;
        gameLamps = 0;
        for (SourceRegister.Entry entry : entries) {
            SourceRegister.Lamp lamp = entry.lamp;
            IsoChunk isoChunk = isoCell.getChunkForGridSquare(lamp.x(), lamp.y(), lamp.z());
            if (isoChunk == null || lamp.z() < 0) continue;
            ++gameLamps;
            long l2 = MapSources.lampKey(lamp.x(), lamp.y(), lamp.z());
            SourceRegister.Lamp lamp2 = mapLamps.get(l2);
            if (lamp2 == null) continue;
            ++foundLamps;
            sameLamps += lamp2.radius() == lamp.radius() && MapCheck.same(lamp2.r(), lamp.r()) && MapCheck.same(lamp2.g(), lamp.g()) && MapCheck.same(lamp2.b(), lamp.b()) ? 1 : 0;
            sameLampsOn += mapLampsOn.get(l2) == entry.on ? 1 : 0;
        }
        MapCheck.rooms(sourceRegister);
    }

    private static void rooms(SourceRegister sourceRegister) {
        LongMap<Boolean> longMap = new LongMap<Boolean>(256);
        sourceRegister.engineRooms(longMap);
        entries.clear();
        sourceRegister.on(entries);
        LongMap<Boolean> longMap2 = new LongMap<Boolean>(256);
        for (SourceRegister.Entry entry : entries) {
            if (entry.map || entry.room == null) continue;
            longMap2.put(entry.room.room(), Boolean.TRUE);
        }
        sameRoomsOn = 0;
        foundRooms = 0;
        gameRooms = 0;
        for (int i = 0; i < longMap.capacity(); ++i) {
            if (longMap.valueAt(i) == null) continue;
            long l = longMap.keyAt(i);
            Boolean bl = mapRoomsOn.get(l);
            ++gameRooms;
            if (bl == null) continue;
            ++foundRooms;
            sameRoomsOn += bl == (longMap2.get(l) != null) ? 1 : 0;
        }
    }

    static String report() {
        return "map layouts " + levels + " levels (exist " + MapCheck.share(sameExists) + ", flags " + MapCheck.share(sameFlags) + ", passes " + MapCheck.share(samePasses) + ", rooms " + MapCheck.share(sameRooms) + "), map lamps " + foundLamps + "/" + gameLamps + " (light " + sameLamps + ", on " + sameLampsOn + "), map rooms " + foundRooms + "/" + gameRooms + " (on " + sameRoomsOn + ")";
    }

    static void clear() {
        sameRooms = 0L;
        samePasses = 0L;
        sameFlags = 0L;
        sameExists = 0L;
        squares = 0L;
        levels = 0L;
        sameRoomsOn = 0;
        foundRooms = 0;
        gameRooms = 0;
        sameLampsOn = 0;
        sameLamps = 0;
        foundLamps = 0;
        gameLamps = 0;
    }

    private static String share(long l) {
        return squares == 0L ? "-" : String.format("%.1f%%", 100.0 * (double)l / (double)squares);
    }

    private static boolean same(float f, float f2) {
        return Math.abs(f - f2) < 0.01f;
    }

    private static String passes(int n) {
        StringBuilder stringBuilder = new StringBuilder(10);
        for (int i = 0; i < 10; ++i) {
            stringBuilder.append(LightLayout.passIn(n, i));
        }
        return stringBuilder.toString();
    }

    private MapCheck() {
    }

    static {
        mapLamps = new LongMap(512);
        mapLampsOn = new LongMap(512);
        mapRoomsOn = new LongMap(512);
        entries = new ArrayList();
    }
}

