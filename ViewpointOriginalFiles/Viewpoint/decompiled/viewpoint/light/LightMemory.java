/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.SandboxOptions
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoLightSource
 *  zombie.iso.IsoRoomLight
 */
package viewpoint.light;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import viewpoint.light.LightSources;
import viewpoint.light.MapSources;
import viewpoint.light.SourceRegister;
import viewpoint.platform.Caches;
import viewpoint.platform.LongMap;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoLightSwitch;

final class LightMemory {
    static final int CELL_CHUNKS = 32;
    private static final Lamp[] NONE = new Lamp[0];
    private static final long CAPTURE_NANOS = 1000000000L;
    private static final long CHUNK_BYTES = 64L;
    private static final long LAMP_BYTES = 160L;
    private static final long ROOM_BYTES = 48L;
    private static final int CHUNK_SHIFT = 34;
    private static final int INDEX_BITS = 20;
    private static final LongMap<Chunk> chunks = new LongMap(1024);
    private static final LongMap<Cell> cells = new LongMap(64);
    private static final LongMap<Room> roomPower = new LongMap(256);
    private static IsoCell lastCell;
    private static long nextCapture;
    private static long[] order;
    private static int lampCount;
    private static int evictions;
    private static long captureNanos;

    static void capture(IsoCell isoCell) {
        if (isoCell != lastCell) {
            LightMemory.clear();
            lastCell = isoCell;
        }
        long l = System.nanoTime();
        IsoChunkMap isoChunkMap = isoCell.getChunkMap(0);
        if (l < nextCapture || isoChunkMap == null) {
            return;
        }
        nextCapture = l + 1000000000L;
        int n = Math.floorDiv(isoChunkMap.getWorldXMinTiles(), 8);
        int n2 = Math.floorDiv(isoChunkMap.getWorldYMinTiles(), 8);
        int n3 = Math.floorDiv(isoChunkMap.getWorldXMaxTiles() - 1, 8) - n + 1;
        int n4 = Math.floorDiv(isoChunkMap.getWorldYMaxTiles() - 1, 8) - n2 + 1;
        Stack stack = isoCell.getLamppostPositions();
        int n5 = LightMemory.sortByChunk(stack, n, n2, n3, n4);
        boolean bl = SandboxOptions.instance.doesPowerGridExist();
        boolean bl2 = GameTime.getInstance().getNight() >= 0.5f;
        IsoMetaGrid isoMetaGrid = IsoWorld.instance.getMetaGrid();
        int n6 = 0;
        for (int i = 0; i < n3 * n4; ++i) {
            int n7;
            for (n7 = n6; n7 < n5 && order[n7] >>> 34 == (long)i; ++n7) {
            }
            int n8 = n + i % n3;
            int n9 = n2 + i / n3;
            IsoChunk isoChunk = isoCell.getChunk(n8, n9);
            if (isoChunk != null && isoChunk.loaded) {
                LightMemory.seen(n8, n9, stack, n6, n7, bl, bl2, isoMetaGrid);
            }
            n6 = n7;
        }
        LightMemory.rooms(isoCell);
        captureNanos += System.nanoTime() - l;
    }

    static Lamp[] lamps(int n, int n2) {
        Chunk chunk = chunks.get(LightMemory.key(n, n2));
        return chunk == null ? null : chunk.lamps;
    }

    static boolean seen(int n, int n2) {
        return chunks.get(LightMemory.key(n, n2)) != null;
    }

    static boolean cellSeen(int n, int n2) {
        return cells.get(LightMemory.key(n, n2)) != null;
    }

    static Lamp[] outdoorLamps(int n, int n2) {
        Cell cell = cells.get(LightMemory.key(n, n2));
        if (cell == null) {
            return NONE;
        }
        if (cell.stale) {
            ArrayList<Lamp> arrayList = new ArrayList<Lamp>();
            for (int i = n2 * 32; i < (n2 + 1) * 32; ++i) {
                for (int j = n * 32; j < (n + 1) * 32; ++j) {
                    Chunk chunk = chunks.get(LightMemory.key(j, i));
                    for (int k = 0; chunk != null && k < chunk.lamps.length; ++k) {
                        if (!chunk.lamps[k].outdoor) continue;
                        arrayList.add(chunk.lamps[k]);
                    }
                }
            }
            cell.outdoor = arrayList.toArray(NONE);
            cell.stale = false;
        }
        return cell.outdoor;
    }

    static boolean ownPower(long l) {
        Room room = roomPower.get(l);
        return room != null && room.ownPower;
    }

    static int evictions() {
        return evictions;
    }

    static void remember(int n, int n2, Lamp[] lampArray, long l) {
        Chunk chunk = chunks.get(LightMemory.key(n, n2));
        if (chunk == null) {
            chunk = new Chunk();
            chunks.put(LightMemory.key(n, n2), chunk);
            ++LightMemory.cell((int)n, (int)n2).chunks;
        }
        if (!Arrays.equals(chunk.lamps, lampArray)) {
            lampCount += lampArray.length - chunk.lamps.length;
            chunk.lamps = lampArray;
            LightMemory.cell((int)n, (int)n2).stale = true;
        }
        chunk.used = l;
    }

    static void rememberRoom(long l, boolean bl, long l2) {
        Room room = roomPower.get(l);
        if (room == null) {
            room = new Room();
            roomPower.put(l, room);
        }
        room.ownPower = bl;
        room.used = l2;
    }

    static int chunks() {
        return chunks.size();
    }

    static int lampsHeld() {
        return lampCount;
    }

    static long takeCaptureNanos() {
        long l = captureNanos;
        captureNanos = 0L;
        return l;
    }

    static void clear() {
        chunks.clear();
        cells.clear();
        roomPower.clear();
        lampCount = 0;
        nextCapture = 0L;
    }

    static void evict(long l) {
        Object object;
        int n;
        boolean bl = false;
        for (n = 0; n < chunks.capacity(); ++n) {
            object = chunks.valueAt(n);
            if (object == null || ((Chunk)object).used >= l) continue;
            long l2 = chunks.keyAt(n);
            int n2 = (int)(l2 >> 32);
            int n3 = (int)l2;
            chunks.removeAt(n);
            lampCount -= ((Chunk)object).lamps.length;
            Cell cell = cells.get(LightMemory.key(Math.floorDiv(n2, 32), Math.floorDiv(n3, 32)));
            if (--cell.chunks == 0) {
                cells.remove(LightMemory.key(Math.floorDiv(n2, 32), Math.floorDiv(n3, 32)));
            } else {
                cell.stale = true;
            }
            bl = true;
        }
        for (n = 0; n < roomPower.capacity(); ++n) {
            object = roomPower.valueAt(n);
            if (object == null || ((Room)object).used >= l) continue;
            roomPower.removeAt(n);
            bl = true;
        }
        evictions += bl ? 1 : 0;
    }

    private static int sortByChunk(Stack<IsoLightSource> stack, int n, int n2, int n3, int n4) {
        int n5 = 0;
        for (int i = 0; i < stack.size(); ++i) {
            IsoLightSource isoLightSource = (IsoLightSource)stack.get(i);
            if (isoLightSource.id == 0 || isoLightSource.life != -1 || !isoLightSource.hydroPowered || isoLightSource.switches.isEmpty()) continue;
            int n6 = Math.floorDiv(isoLightSource.x, 8) - n;
            int n7 = Math.floorDiv(isoLightSource.y, 8) - n2;
            if (n6 < 0 || n7 < 0 || n6 >= n3 || n7 >= n4) continue;
            order = n5 == order.length ? Arrays.copyOf(order, n5 * 2) : order;
            long l = (long)(isoLightSource.x & 7 | (isoLightSource.y & 7) << 3) | (long)(isoLightSource.z + 32 & 0xFF) << 6;
            LightMemory.order[n5++] = (long)(n7 * n3 + n6) << 34 | l << 20 | (long)i;
        }
        Arrays.sort(order, 0, n5);
        return n5;
    }

    private static void seen(int n, int n2, Stack<IsoLightSource> stack, int n3, int n4, boolean bl, boolean bl2, IsoMetaGrid isoMetaGrid) {
        Chunk chunk = chunks.get(LightMemory.key(n, n2));
        Lamp[] lampArray = chunk == null ? NONE : chunk.lamps;
        boolean bl3 = lampArray.length == n4 - n3;
        for (int i = n3; bl3 && i < n4; ++i) {
            bl3 = LightMemory.same(lampArray[i - n3], LightMemory.source(stack, i), bl, bl2);
        }
        if (bl3 && chunk != null) {
            chunk.used = Caches.tick();
            return;
        }
        Lamp[] lampArray2 = n4 == n3 ? NONE : new Lamp[n4 - n3];
        for (int i = n3; i < n4; ++i) {
            lampArray2[i - n3] = LightMemory.lamp(LightMemory.source(stack, i), lampArray, bl, bl2, isoMetaGrid);
        }
        LightMemory.remember(n, n2, lampArray2, Caches.tick());
    }

    private static IsoLightSource source(Stack<IsoLightSource> stack, int n) {
        return (IsoLightSource)stack.get((int)(order[n] & 0xFFFFFL));
    }

    private static boolean same(Lamp lamp, IsoLightSource isoLightSource, boolean bl, boolean bl2) {
        SourceRegister.Lamp lamp2 = lamp.light;
        IsoLightSwitch isoLightSwitch = (IsoLightSwitch)((Object)isoLightSource.switches.get(0));
        return lamp2.x() == isoLightSource.x && lamp2.y() == isoLightSource.y && lamp2.z() == isoLightSource.z && lamp2.radius() == Math.min(isoLightSource.radius, 20) && lamp2.r() == LightSources.engine(isoLightSource.r) && lamp2.g() == LightSources.engine(isoLightSource.g) && lamp2.b() == LightSources.engine(isoLightSource.b) && lamp2.building() == LightSources.building(isoLightSource) && lamp.street == isoLightSwitch.streetLight && lamp.switchedOn == isoLightSwitch.isActivated() && lamp.ownPower == LightMemory.ownPower(isoLightSource, isoLightSwitch, bl, bl2, lamp.inBuilding, lamp.noPower);
    }

    private static Lamp lamp(IsoLightSource isoLightSource, Lamp[] lampArray, boolean bl, boolean bl2, IsoMetaGrid isoMetaGrid) {
        boolean bl3;
        boolean bl4;
        Lamp lamp = null;
        for (bl4 = false; lamp == null && bl4 < lampArray.length; bl4 += 1) {
            SourceRegister.Lamp lamp2 = lampArray[bl4].light;
            lamp = lamp2.x() == isoLightSource.x && lamp2.y() == isoLightSource.y && lamp2.z() == isoLightSource.z ? lampArray[bl4] : null;
        }
        bl4 = lamp != null ? lamp.inBuilding : MapSources.inBuilding(isoMetaGrid, isoLightSource.x, isoLightSource.y, isoLightSource.z);
        boolean bl5 = bl3 = lamp != null ? lamp.noPower : MapSources.noPower(isoMetaGrid, isoLightSource.x, isoLightSource.y);
        boolean bl6 = lamp != null ? lamp.outdoor : isoLightSource.z >= 0 && isoMetaGrid.getRoomAt(isoLightSource.x, isoLightSource.y, isoLightSource.z) == null;
        IsoLightSwitch isoLightSwitch = (IsoLightSwitch)((Object)isoLightSource.switches.get(0));
        SourceRegister.Lamp lamp3 = new SourceRegister.Lamp(isoLightSource.x, isoLightSource.y, isoLightSource.z, Math.min(isoLightSource.radius, 20), LightSources.engine(isoLightSource.r), LightSources.engine(isoLightSource.g), LightSources.engine(isoLightSource.b), LightSources.building(isoLightSource));
        int n = LightMemory.channel(isoLightSource.r) | LightMemory.channel(isoLightSource.g) << 8 | LightMemory.channel(isoLightSource.b) << 16 | Math.min(isoLightSource.radius, 63) << 24 | (isoLightSwitch.streetLight ? 0x40000000 : 0);
        return new Lamp(lamp3, n, isoLightSwitch.streetLight, isoLightSwitch.isActivated(), LightMemory.ownPower(isoLightSource, isoLightSwitch, bl, bl2, bl4, bl3), bl4, bl3, bl6);
    }

    private static boolean ownPower(IsoLightSource isoLightSource, IsoLightSwitch isoLightSwitch, boolean bl, boolean bl2, boolean bl3, boolean bl4) {
        return isoLightSource.active && !MapSources.gridPowered(bl, bl2, isoLightSwitch.streetLight, bl3, bl4);
    }

    private static int channel(float f) {
        return Math.max(0, Math.min(255, Math.round(f * 255.0f)));
    }

    private static void rooms(IsoCell isoCell) {
        if (IsoWorld.instance.isHydroPowerOn()) {
            return;
        }
        ArrayList arrayList = isoCell.roomLights;
        for (int i = 0; i < arrayList.size(); ++i) {
            IsoRoomLight isoRoomLight = (IsoRoomLight)arrayList.get(i);
            if (isoRoomLight.room == null || isoRoomLight.room.def == null || !isoRoomLight.room.def.lightsActive) continue;
            LightMemory.rememberRoom(isoRoomLight.room.def.getID(), isoRoomLight.active, Caches.tick());
        }
    }

    private static Cell cell(int n, int n2) {
        long l = LightMemory.key(Math.floorDiv(n, 32), Math.floorDiv(n2, 32));
        Cell cell = cells.get(l);
        if (cell == null) {
            cell = new Cell();
            cells.put(l, cell);
        }
        return cell;
    }

    private static long key(int n, int n2) {
        return (long)n << 32 | (long)n2 & 0xFFFFFFFFL;
    }

    private LightMemory() {
    }

    static {
        order = new long[512];
        Caches.register(new Caches.Cache(){

            @Override
            public long bytes() {
                return (long)chunks.size() * 64L + (long)lampCount * 160L + (long)roomPower.size() * 48L;
            }

            @Override
            public void list(Caches.Survey survey) {
                for (Chunk object : chunks.values()) {
                    survey.add(object.used, 64L + (long)object.lamps.length * 160L);
                }
                for (Room room : roomPower.values()) {
                    survey.add(room.used, 48L);
                }
            }

            @Override
            public void evictBefore(long l) {
                LightMemory.evict(l);
            }
        });
    }

    private static final class Chunk {
        Lamp[] lamps = NONE;
        long used;

        private Chunk() {
        }
    }

    record Lamp(SourceRegister.Lamp light, int tile, boolean street, boolean switchedOn, boolean ownPower, boolean inBuilding, boolean noPower, boolean outdoor) {
        boolean on(boolean bl, boolean bl2) {
            return this.switchedOn && (this.ownPower || MapSources.gridPowered(bl, bl2, this.street, this.inBuilding, this.noPower));
        }
    }

    private static final class Cell {
        int chunks;
        Lamp[] outdoor = NONE;
        boolean stale;

        private Cell() {
        }
    }

    private static final class Room {
        boolean ownPower;
        long used;

        private Room() {
        }
    }
}

