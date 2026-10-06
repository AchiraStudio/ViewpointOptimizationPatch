/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.SandboxOptions
 *  zombie.iso.IsoCell
 */
package viewpoint.light;

import java.util.ArrayList;
import java.util.Locale;
import viewpoint.light.LightJobs;
import viewpoint.light.LightMemory;
import viewpoint.light.LightSources;
import viewpoint.light.MapCheck;
import viewpoint.light.SourceRegister;
import viewpoint.platform.LongMap;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.zones.Zone;

final class MapSources {
    private static final long SAME_SQUARE = 0x100000000000000L;
    private static final long REMEMBERED = Long.MIN_VALUE;
    private static final int BLOCK_CHUNKS = 8;
    private static final int LIGHT_SWITCH = 7;
    private static final int LISTED_MARGIN = 25;
    private static final LongMap<Block> blocks = new LongMap(128);
    private static final LongMap<Boolean> listedRooms = new LongMap(256);
    private static final ArrayList<Zone> zones = new ArrayList();
    private static boolean grid;
    private static boolean lampGrid;
    private static boolean night;
    private static int readings;
    private static int evictions;
    private static LongMap<Boolean> spare;

    static void add(int n, int n2, int[] nArray, ArrayList<RoomDef> arrayList) {
        RoomDef roomDef2;
        MapSources.drop(n, n2);
        IsoMetaGrid isoMetaGrid = IsoWorld.instance.getMetaGrid();
        Lamp[] lampArray = new Lamp[nArray.length / 4];
        for (int i = 0; i < lampArray.length; ++i) {
            lampArray[i] = MapSources.lamp(isoMetaGrid, nArray, i * 4, lampArray, i);
        }
        ArrayList<Room> arrayList2 = new ArrayList<Room>();
        for (RoomDef roomDef2 : arrayList) {
            MapSources.rooms(isoMetaGrid, roomDef2, arrayList2);
        }
        Block block = new Block(n, n2, lampArray, arrayList2.toArray(new Room[0]));
        blocks.put(MapSources.key(n, n2), block);
        roomDef2 = IsoWorld.instance.getCell();
        if (roomDef2 != null) {
            MapSources.decide(block, (IsoCell)roomDef2, MapSources.window((IsoCell)roomDef2));
        }
    }

    static void drop(int n, int n2) {
        Block block = blocks.remove(MapSources.key(n, n2));
        if (block == null) {
            return;
        }
        SourceRegister sourceRegister = LightSources.register();
        for (Lamp record : block.lamps) {
            sourceRegister.unmap(record.key, true, LightJobs::mark);
        }
        for (Record record : block.rooms) {
            sourceRegister.unmap(((Room)record).key, false, LightJobs::mark);
        }
        for (int i = 0; i < block.remembered.capacity(); ++i) {
            if (block.remembered.valueAt(i) == null) continue;
            sourceRegister.unmap(block.remembered.keyAt(i), true, LightJobs::mark);
        }
    }

    static void update(IsoCell isoCell) {
        boolean bl;
        boolean bl2 = IsoWorld.instance.isHydroPowerOn();
        boolean bl3 = SandboxOptions.instance.doesPowerGridExist();
        boolean bl4 = GameTime.getInstance().getNight() >= 0.5f;
        int n = LightSources.readings();
        int n2 = LightMemory.evictions();
        boolean bl5 = bl = bl2 != grid || bl3 != lampGrid || bl4 != night || n2 != evictions;
        if (!bl && n == readings) {
            return;
        }
        grid = bl2;
        lampGrid = bl3;
        night = bl4;
        readings = n;
        evictions = n2;
        listedRooms.clear();
        LightSources.register().engineRooms(listedRooms);
        int[] nArray = MapSources.window(isoCell);
        for (int i = 0; i < blocks.capacity(); ++i) {
            Block block = blocks.valueAt(i);
            if (block == null || !bl && !MapSources.nearWindow(blocks.keyAt(i), nArray)) continue;
            MapSources.decide(block, isoCell, nArray);
        }
        LightSources.publish();
        MapCheck.sources(isoCell);
    }

    static void clear() {
        blocks.clear();
        readings = -1;
    }

    private static boolean powered(Lamp lamp) {
        return MapSources.gridPowered(lampGrid, night, lamp.street, lamp.inBuilding, lamp.noPower);
    }

    static boolean gridPowered(boolean bl, boolean bl2, boolean bl3, boolean bl4, boolean bl5) {
        return bl && !bl5 && (bl3 ? bl2 : bl4);
    }

    static void lamps(LongMap<SourceRegister.Lamp> longMap, LongMap<Boolean> longMap2) {
        for (Block block : blocks.values()) {
            for (Lamp lamp : block.lamps) {
                longMap.put(lamp.key, lamp.light);
                longMap2.put(lamp.key, MapSources.powered(lamp));
            }
        }
    }

    static void rooms(LongMap<Boolean> longMap) {
        for (Block block : blocks.values()) {
            for (Room room : block.rooms) {
                longMap.put(room.def.id, grid && !room.noPower && room.def.lightsActive);
            }
        }
    }

    static long lampKey(int n, int n2, int n3) {
        return (long)(n & 0xFFFFFF) << 32 | (long)(n2 & 0xFFFFFF) << 8 | (long)(n3 + 32 & 0xFF);
    }

    private static void decide(Block block, IsoCell isoCell, int[] nArray) {
        boolean bl;
        SourceRegister sourceRegister = LightSources.register();
        for (Lamp record : block.lamps) {
            SourceRegister.Lamp lamp = record.light;
            bl = MapSources.within(lamp.x(), lamp.y(), nArray, 0) && MapSources.loaded(isoCell, lamp.x(), lamp.y(), lamp.z());
            boolean bl2 = LightMemory.seen(Math.floorDiv(lamp.x(), 8), Math.floorDiv(lamp.y(), 8));
            sourceRegister.mapLamp(record.key, lamp, !bl && !bl2 && MapSources.powered(record), LightJobs::mark);
        }
        MapSources.remembered(block, isoCell, nArray, sourceRegister);
        for (Record record : block.rooms) {
            boolean bl3 = grid && !((Room)record).noPower || LightMemory.ownPower(((Room)record).def.id);
            bl = listedRooms.get(((Room)record).def.id) == null && bl3 && ((Room)record).def.lightsActive;
            sourceRegister.mapRoom(((Room)record).key, ((Room)record).light, bl, LightJobs::mark);
        }
    }

    private static void remembered(Block block, IsoCell isoCell, int[] nArray, SourceRegister sourceRegister) {
        int n;
        LongMap<Boolean> longMap = spare;
        longMap.clear();
        for (n = block.by * 8; n < (block.by + 1) * 8; ++n) {
            for (int i = block.bx * 8; i < (block.bx + 1) * 8; ++i) {
                LightMemory.Lamp[] lampArray = LightMemory.lamps(i, n);
                if (lampArray == null || lampArray.length == 0 || MapSources.within(i * 8, n * 8, nArray, 0) && MapSources.loaded(isoCell, i * 8, n * 8, 0)) continue;
                long l = 0L;
                for (int j = 0; j < lampArray.length; ++j) {
                    SourceRegister.Lamp lamp = lampArray[j].light();
                    l = j > 0 && MapSources.sameSquare(lampArray[j - 1].light(), lamp) ? l + 0x100000000000000L : Long.MIN_VALUE | MapSources.lampKey(lamp.x(), lamp.y(), lamp.z());
                    sourceRegister.mapLamp(l, lamp, lampArray[j].on(lampGrid, night), LightJobs::mark);
                    longMap.put(l, Boolean.TRUE);
                }
            }
        }
        for (n = 0; n < block.remembered.capacity(); ++n) {
            long l = block.remembered.keyAt(n);
            if (block.remembered.valueAt(n) == null || longMap.get(l) != null) continue;
            sourceRegister.unmap(l, true, LightJobs::mark);
        }
        spare = block.remembered;
        block.remembered = longMap;
    }

    private static boolean sameSquare(SourceRegister.Lamp lamp, SourceRegister.Lamp lamp2) {
        return lamp.x() == lamp2.x() && lamp.y() == lamp2.y() && lamp.z() == lamp2.z();
    }

    private static Lamp lamp(IsoMetaGrid isoMetaGrid, int[] nArray, int n, Lamp[] lampArray, int n2) {
        int n3;
        int n4 = nArray[n];
        int n5 = nArray[n + 1];
        int n6 = nArray[n + 2];
        int n7 = nArray[n + 3];
        long l = MapSources.lampKey(n4, n5, n6);
        for (n3 = 0; n3 < n2; ++n3) {
            l = lampArray[n3].key == l ? l + 0x100000000000000L : l;
        }
        n3 = Math.min(n7 >>> 24 & 0x3F, 20);
        SourceRegister.Lamp lamp = new SourceRegister.Lamp(n4, n5, n6, n3, MapSources.doubled(n7), MapSources.doubled(n7 >> 8), MapSources.doubled(n7 >> 16), -1L);
        return new Lamp(l, lamp, (n7 & 0x40000000) != 0, MapSources.inBuilding(isoMetaGrid, n4, n5, n6), MapSources.noPower(isoMetaGrid, n4, n5));
    }

    private static float doubled(int n) {
        return Math.min(1.0f, (float)(n & 0xFF) / 255.0f * 2.0f);
    }

    static boolean inBuilding(IsoMetaGrid isoMetaGrid, int n, int n2, int n3) {
        for (int i = 0; i >= -1 && n3 + i >= 0; --i) {
            for (int j = -1; j <= 1; ++j) {
                for (int k = -1; k <= 1; ++k) {
                    if (isoMetaGrid.getRoomAt(n + k, n2 + j, n3 + i) == null && isoMetaGrid.getEmptyOutsideAt(n + k, n2 + j, n3 + i) == null) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private static void rooms(IsoMetaGrid isoMetaGrid, RoomDef roomDef, ArrayList<Room> arrayList) {
        if (!MapSources.hasLights(roomDef)) {
            return;
        }
        boolean bl = MapSources.noPower(isoMetaGrid, roomDef);
        long l = roomDef.building == null ? -1L : roomDef.building.getID();
        for (int i = 0; i < roomDef.rects.size() && i < 256; ++i) {
            RoomDef.RoomRect roomRect = roomDef.rects.get(i);
            SourceRegister.RoomLight roomLight = new SourceRegister.RoomLight(roomRect.x, roomRect.y, roomDef.level, roomRect.w, roomRect.h, roomDef.id, l);
            arrayList.add(new Room(roomDef.id & 0xFFFFFFFF00000000L | (roomDef.id & 0xFFFFFFL) << 8 | (long)i, roomLight, roomDef, bl));
        }
    }

    static boolean hasLights(RoomDef roomDef) {
        for (int i = 0; i < roomDef.objects.size(); ++i) {
            if (roomDef.objects.get(i).getType() != 7) continue;
            return !roomDef.rects.isEmpty();
        }
        return false;
    }

    static boolean noPower(IsoMetaGrid isoMetaGrid, RoomDef roomDef) {
        RoomDef.RoomRect roomRect = roomDef.rects.get(0);
        return roomDef.name != null && roomDef.name.toLowerCase(Locale.ROOT).contains("derelict") || MapSources.noPower(isoMetaGrid, roomRect.x, roomRect.y);
    }

    static boolean noPower(IsoMetaGrid isoMetaGrid, int n, int n2) {
        zones.clear();
        isoMetaGrid.getZonesAt(n, n2, 0, zones);
        for (Zone zone : zones) {
            if (!"NoPower".equals(zone.type) && !"NoPowerOrWater".equals(zone.type)) continue;
            return true;
        }
        return false;
    }

    private static boolean loaded(IsoCell isoCell, int n, int n2, int n3) {
        IsoChunk isoChunk = isoCell.getChunkForGridSquare(n, n2, n3);
        return isoChunk != null && isoChunk.loaded;
    }

    private static int[] window(IsoCell isoCell) {
        int[] nArray;
        IsoChunkMap isoChunkMap = isoCell.getChunkMap(0);
        if (isoChunkMap == null) {
            nArray = null;
        } else {
            int[] nArray2 = new int[4];
            nArray2[0] = isoChunkMap.getWorldXMinTiles();
            nArray2[1] = isoChunkMap.getWorldYMinTiles();
            nArray2[2] = isoChunkMap.getWorldXMaxTiles();
            nArray = nArray2;
            nArray2[3] = isoChunkMap.getWorldYMaxTiles();
        }
        return nArray;
    }

    private static boolean within(int n, int n2, int[] nArray, int n3) {
        return nArray != null && n >= nArray[0] - n3 && n2 >= nArray[1] - n3 && n < nArray[2] + n3 && n2 < nArray[3] + n3;
    }

    private static boolean nearWindow(long l, int[] nArray) {
        int n = (int)(l >> 32);
        int n2 = (int)l;
        int n3 = n * 64;
        int n4 = n2 * 64;
        return nArray != null && n3 + 64 + 25 > nArray[0] && n4 + 64 + 25 > nArray[1] && n3 - 25 < nArray[2] && n4 - 25 < nArray[3];
    }

    private static long key(int n, int n2) {
        return (long)n << 32 | (long)n2 & 0xFFFFFFFFL;
    }

    private MapSources() {
    }

    static {
        readings = -1;
        spare = new LongMap(8);
    }

    private record Lamp(long key, SourceRegister.Lamp light, boolean street, boolean inBuilding, boolean noPower) {
    }

    private static final class Block {
        final int bx;
        final int by;
        final Lamp[] lamps;
        final Room[] rooms;
        LongMap<Boolean> remembered = new LongMap(8);

        Block(int n, int n2, Lamp[] lampArray, Room[] roomArray) {
            this.bx = n;
            this.by = n2;
            this.lamps = lampArray;
            this.rooms = roomArray;
        }
    }

    private record Room(long key, SourceRegister.RoomLight light, RoomDef def, boolean noPower) {
    }
}

