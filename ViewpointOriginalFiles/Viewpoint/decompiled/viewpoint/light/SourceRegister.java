/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.util.ArrayList;
import java.util.List;
import viewpoint.light.LightModel;
import viewpoint.light.LightSources;
import viewpoint.light.SourceLight;
import viewpoint.platform.LongMap;

final class SourceRegister {
    static final int GRID_BORDER = 2;
    private final LongMap<Entry> lamps = new LongMap(512);
    private final LongMap<Entry> rooms = new LongMap(2048);
    private final LongMap<Entry> mapLamps = new LongMap(512);
    private final LongMap<Entry> mapRooms = new LongMap(2048);
    private final LongMap<ArrayList<Entry>> byChunk = new LongMap(1024);
    private long update;
    private LightSources.Marks marks;
    private int lampsOn;
    private int roomsOn;
    private int mapLampsOn;
    private int mapRoomsOn;
    private int changes;
    private int marked;
    private long lightBytes;

    SourceRegister() {
    }

    void begin(LightSources.Marks marks) {
        this.marks = marks;
        ++this.update;
    }

    void lamp(int n, int n2, int n3, int n4, int n5, float f, float f2, float f3, long l, boolean bl) {
        Entry entry = this.lamps.get(n);
        int n6 = Math.min(n5, 20);
        if (entry != null && entry.on == bl && SourceRegister.samePlace(entry.lamp, n2, n3, n4, l)) {
            Lamp lamp = entry.lamp;
            if (n6 <= lamp.radius() && f <= lamp.r() && f2 <= lamp.g() && f3 <= lamp.b()) {
                entry.seen = this.update;
                return;
            }
            n6 = Math.max(n6, lamp.radius());
            f = Math.max(f, lamp.r());
            f2 = Math.max(f2, lamp.g());
            f3 = Math.max(f3, lamp.b());
        }
        entry = this.replace(this.lamps, n, entry, bl);
        entry.lamp = new Lamp(n2, n3, n4, n6, f, f2, f3, l);
        int n7 = LightModel.levelsReached(n6);
        this.reach(entry, n2 - n6, n3 - n6, n2 + n6, n3 + n6, n4 - n7, n4 + n7);
        this.lampsOn += bl ? 1 : 0;
    }

    void room(int n, int n2, int n3, int n4, int n5, int n6, long l, long l2, boolean bl) {
        Entry entry = this.rooms.get(n);
        if (entry != null && entry.on == bl && SourceRegister.same(entry.room, n2, n3, n4, n5, n6, l, l2)) {
            entry.seen = this.update;
            return;
        }
        entry = this.replace(this.rooms, n, entry, bl);
        entry.room = new RoomLight(n2, n3, n4, n5, n6, l, l2);
        int n7 = 7;
        int n8 = LightModel.levelsReached(n7);
        this.reach(entry, n2 - n7, n3 - n7, n2 + n5 - 1 + n7, n3 + n6 - 1 + n7, n4 - n8, n4 + n8);
        this.roomsOn += bl ? 1 : 0;
    }

    void finish() {
        this.lamps.removeIf(this::gone);
        this.rooms.removeIf(this::gone);
        this.marks = null;
    }

    void mapLamp(long l, Lamp lamp, boolean bl, LightSources.Marks marks) {
        Entry entry = this.mapLamps.get(l);
        if (entry != null && entry.on == bl && entry.lamp.equals(lamp)) {
            return;
        }
        this.marks = marks;
        entry = this.replace(this.mapLamps, l, entry, bl);
        entry.map = true;
        entry.lamp = lamp;
        int n = LightModel.levelsReached(lamp.radius());
        this.reach(entry, lamp.x() - lamp.radius(), lamp.y() - lamp.radius(), lamp.x() + lamp.radius(), lamp.y() + lamp.radius(), lamp.z() - n, lamp.z() + n);
        this.mapLampsOn += bl ? 1 : 0;
        this.marks = null;
    }

    void mapRoom(long l, RoomLight roomLight, boolean bl, LightSources.Marks marks) {
        Entry entry = this.mapRooms.get(l);
        if (entry != null && entry.on == bl && entry.room.equals(roomLight)) {
            return;
        }
        this.marks = marks;
        entry = this.replace(this.mapRooms, l, entry, bl);
        entry.map = true;
        entry.room = roomLight;
        int n = 7;
        int n2 = LightModel.levelsReached(n);
        this.reach(entry, roomLight.x() - n, roomLight.y() - n, roomLight.x() + roomLight.width() - 1 + n, roomLight.y() + roomLight.height() - 1 + n, roomLight.z() - n2, roomLight.z() + n2);
        this.mapRoomsOn += bl ? 1 : 0;
        this.marks = null;
    }

    void unmap(long l, boolean bl, LightSources.Marks marks) {
        Entry entry = (bl ? this.mapLamps : this.mapRooms).remove(l);
        if (entry != null) {
            this.marks = marks;
            this.leave(entry);
            ++this.changes;
            this.marks = null;
        }
    }

    void engineRooms(LongMap<Boolean> longMap) {
        for (Entry entry : this.rooms.values()) {
            longMap.put(entry.room.room(), Boolean.TRUE);
        }
    }

    void engineLamps(List<Entry> list) {
        for (Entry entry : this.lamps.values()) {
            list.add(entry);
        }
    }

    Reaching reaching(int n, int n2, int n3) {
        ArrayList<Lamp> arrayList = new ArrayList<Lamp>();
        ArrayList<RoomLight> arrayList2 = new ArrayList<RoomLight>();
        ArrayList<Entry> arrayList3 = this.byChunk.get(SourceRegister.key(n, n2));
        if (arrayList3 != null) {
            for (Entry entry : arrayList3) {
                if (!entry.on || n3 < entry.minLevel || n3 > entry.maxLevel) continue;
                if (entry.lamp != null) {
                    arrayList.add(entry.lamp);
                    continue;
                }
                arrayList2.add(entry.room);
            }
        }
        return new Reaching(List.copyOf(arrayList), List.copyOf(arrayList2));
    }

    boolean lights(int n, int n2, int n3, List<SourceLight> list) {
        ArrayList<Entry> arrayList = this.byChunk.get(SourceRegister.key(n, n2));
        if (arrayList == null) {
            return true;
        }
        for (Entry entry : arrayList) {
            if (!entry.on || n3 < entry.minLevel || n3 > entry.maxLevel) continue;
            if (entry.light == null) {
                return false;
            }
            list.add(entry.light);
        }
        return true;
    }

    void on(List<Entry> list) {
        SourceRegister.on(this.lamps, list);
        SourceRegister.on(this.rooms, list);
        SourceRegister.on(this.mapLamps, list);
        SourceRegister.on(this.mapRooms, list);
    }

    private static void on(LongMap<Entry> longMap, List<Entry> list) {
        for (int i = 0; i < longMap.capacity(); ++i) {
            Entry entry = longMap.valueAt(i);
            if (entry == null || !entry.on) continue;
            list.add(entry);
        }
    }

    void due(List<Entry> list) {
        SourceRegister.due(this.lamps, list);
        SourceRegister.due(this.rooms, list);
        SourceRegister.due(this.mapLamps, list);
        SourceRegister.due(this.mapRooms, list);
    }

    private static void due(LongMap<Entry> longMap, List<Entry> list) {
        for (Entry entry : longMap.values()) {
            if (!entry.due || entry.pending) continue;
            list.add(entry);
        }
    }

    void worked(Entry entry, int n, int n2, SourceLight sourceLight, LightSources.Marks marks) {
        entry.pending = false;
        if (!entry.on || entry.sourceVersion != n2 || entry.version != n && entry.light != null) {
            return;
        }
        SourceLight sourceLight2 = entry.light;
        this.lightBytes += sourceLight.bytes() - (sourceLight2 == null ? 0L : sourceLight2.bytes());
        entry.light = sourceLight;
        boolean bl = entry.due = entry.version != n;
        if (sourceLight2 != null && !sourceLight2.same(sourceLight)) {
            SourceRegister.markReach(entry, marks);
        }
    }

    void relayout(int n, int n2, int n3) {
        ArrayList<Entry> arrayList = this.byChunk.get(SourceRegister.key(n, n2));
        if (arrayList == null) {
            return;
        }
        for (Entry entry : arrayList) {
            if (!entry.on || n3 < entry.minLevel || n3 > entry.maxLevel) continue;
            entry.due = true;
            ++entry.version;
        }
    }

    long lightBytes() {
        return this.lightBytes;
    }

    void clear() {
        this.lamps.clear();
        this.rooms.clear();
        this.mapLamps.clear();
        this.mapRooms.clear();
        this.byChunk.clear();
        this.marked = 0;
        this.changes = 0;
        this.mapRoomsOn = 0;
        this.mapLampsOn = 0;
        this.roomsOn = 0;
        this.lampsOn = 0;
        this.lightBytes = 0L;
    }

    int lamps() {
        return this.lamps.size();
    }

    int rooms() {
        return this.rooms.size();
    }

    int lampsOn() {
        return this.lampsOn;
    }

    int roomsOn() {
        return this.roomsOn;
    }

    int mapLamps() {
        return this.mapLamps.size();
    }

    int mapRooms() {
        return this.mapRooms.size();
    }

    int mapLampsOn() {
        return this.mapLampsOn;
    }

    int mapRoomsOn() {
        return this.mapRoomsOn;
    }

    int takeChanges() {
        int n = this.changes;
        this.changes = 0;
        return n;
    }

    int takeMarked() {
        int n = this.marked;
        this.marked = 0;
        return n;
    }

    private static boolean samePlace(Lamp lamp, int n, int n2, int n3, long l) {
        return lamp.x() == n && lamp.y() == n2 && lamp.z() == n3 && lamp.building() == l;
    }

    private static boolean same(RoomLight roomLight, int n, int n2, int n3, int n4, int n5, long l, long l2) {
        return roomLight.x() == n && roomLight.y() == n2 && roomLight.z() == n3 && roomLight.width() == n4 && roomLight.height() == n5 && roomLight.room() == l && roomLight.building() == l2;
    }

    private Entry replace(LongMap<Entry> longMap, long l, Entry entry, boolean bl) {
        if (entry == null) {
            entry = new Entry();
            longMap.put(l, entry);
        } else {
            this.leave(entry);
        }
        entry.on = bl;
        entry.due = bl;
        entry.seen = this.update;
        ++this.changes;
        return entry;
    }

    private void reach(Entry entry, int n, int n2, int n3, int n4, int n5, int n6) {
        entry.minX = Math.floorDiv(n - 2, 8);
        entry.minY = Math.floorDiv(n2 - 2, 8);
        entry.maxX = Math.floorDiv(n3 + 2, 8);
        entry.maxY = Math.floorDiv(n4 + 2, 8);
        entry.minLevel = n5;
        entry.maxLevel = n6;
        for (int i = entry.minY; i <= entry.maxY; ++i) {
            for (int j = entry.minX; j <= entry.maxX; ++j) {
                ArrayList<Entry> arrayList = this.byChunk.get(SourceRegister.key(j, i));
                if (arrayList == null) {
                    arrayList = new ArrayList(4);
                    this.byChunk.put(SourceRegister.key(j, i), arrayList);
                }
                arrayList.add(entry);
            }
        }
        this.mark(entry);
    }

    private void leave(Entry entry) {
        this.mark(entry);
        this.letGoLight(entry);
        if (entry.on && entry.lamp != null) {
            if (entry.map) {
                --this.mapLampsOn;
            } else {
                --this.lampsOn;
            }
        } else if (entry.on) {
            if (entry.map) {
                --this.mapRoomsOn;
            } else {
                --this.roomsOn;
            }
        }
        for (int i = entry.minY; i <= entry.maxY; ++i) {
            for (int j = entry.minX; j <= entry.maxX; ++j) {
                ArrayList<Entry> arrayList = this.byChunk.get(SourceRegister.key(j, i));
                if (arrayList == null) continue;
                arrayList.remove(entry);
                if (!arrayList.isEmpty()) continue;
                this.byChunk.remove(SourceRegister.key(j, i));
            }
        }
    }

    private boolean gone(Entry entry) {
        if (entry.seen == this.update) {
            return false;
        }
        this.leave(entry);
        ++this.changes;
        return true;
    }

    private void mark(Entry entry) {
        if (entry.on && this.marks != null) {
            this.marked += SourceRegister.markReach(entry, this.marks);
        }
    }

    private static int markReach(Entry entry, LightSources.Marks marks) {
        for (int i = entry.minLevel; i <= entry.maxLevel; ++i) {
            for (int j = entry.minY; j <= entry.maxY; ++j) {
                for (int k = entry.minX; k <= entry.maxX; ++k) {
                    marks.mark(k, j, i);
                }
            }
        }
        return (entry.maxLevel - entry.minLevel + 1) * (entry.maxY - entry.minY + 1) * (entry.maxX - entry.minX + 1);
    }

    private void letGoLight(Entry entry) {
        this.lightBytes -= entry.light == null ? 0L : entry.light.bytes();
        entry.light = null;
        ++entry.version;
        ++entry.sourceVersion;
    }

    private static long key(int n, int n2) {
        return (long)n << 32 | (long)n2 & 0xFFFFFFFFL;
    }

    static final class Entry {
        Lamp lamp;
        RoomLight room;
        boolean map;
        boolean on;
        long seen;
        int minX;
        int minY;
        int maxX;
        int maxY;
        int minLevel;
        int maxLevel;
        SourceLight light;
        boolean due;
        boolean pending;
        int version;
        int sourceVersion;

        Entry() {
        }
    }

    record Lamp(int x, int y, int z, int radius, float r, float g, float b, long building) {
    }

    record RoomLight(int x, int y, int z, int width, int height, long room, long building) {
    }

    record Reaching(List<Lamp> lamps, List<RoomLight> rooms) {
    }
}

