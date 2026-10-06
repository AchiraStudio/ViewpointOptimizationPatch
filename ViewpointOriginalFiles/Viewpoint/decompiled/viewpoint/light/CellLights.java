/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.SandboxOptions
 */
package viewpoint.light;

import java.util.ArrayList;
import java.util.Arrays;
import viewpoint.light.LightMemory;
import viewpoint.light.MapSources;
import viewpoint.light.SourceRegister;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

public final class CellLights {
    public static Cell cell(int n, int n2, int[] nArray) {
        int n3;
        IsoMetaGrid isoMetaGrid = IsoWorld.instance.getMetaGrid();
        int n4 = 0;
        int[] nArray2 = new int[nArray.length];
        boolean[] blArray = new boolean[nArray.length / 4];
        boolean[] blArray2 = new boolean[blArray.length];
        boolean[] blArray3 = new boolean[blArray.length];
        int n5 = 0;
        while (n5 + 4 <= nArray.length) {
            int n6 = nArray[n5];
            n3 = nArray[n5 + 1];
            int n7 = nArray[n5 + 2];
            if (n7 >= 0 && isoMetaGrid.getRoomAt(n6, n3, n7) == null) {
                System.arraycopy(nArray, n5, nArray2, n4 * 4, 4);
                blArray[n4] = (nArray[n5 + 3] & 0x40000000) != 0;
                blArray2[n4] = MapSources.inBuilding(isoMetaGrid, n6, n3, n7);
                blArray3[n4] = MapSources.noPower(isoMetaGrid, n6, n3);
                ++n4;
            }
            n5 += 4;
        }
        ArrayList<RoomDef> arrayList = CellLights.rooms(isoMetaGrid, n, n2);
        boolean[] blArray4 = new boolean[arrayList.size()];
        for (n3 = 0; n3 < blArray4.length; ++n3) {
            blArray4[n3] = MapSources.noPower(isoMetaGrid, arrayList.get(n3));
        }
        return new Cell(n, n2, Arrays.copyOf(nArray2, n4 * 4), Arrays.copyOf(blArray, n4), Arrays.copyOf(blArray2, n4), Arrays.copyOf(blArray3, n4), arrayList.toArray(new RoomDef[0]), blArray4);
    }

    public static Inputs decide(Cell cell, Cell[] cellArray) {
        boolean bl = SandboxOptions.instance.doesPowerGridExist();
        boolean bl2 = GameTime.getInstance().getNight() >= 0.5f;
        boolean bl3 = IsoWorld.instance.isHydroPowerOn();
        int n = cell.cellX * 256 - 20;
        int n2 = n + 256 + 40;
        int n3 = cell.cellY * 256 - 20;
        int n4 = n3 + 256 + 40;
        IntList intList = new IntList();
        for (Cell cell2 : cellArray) {
            if (cell2 == null) continue;
            CellLights.lamps(cell2, bl, bl2, n, n3, n2, n4, intList);
        }
        IntList intList2 = new IntList();
        for (int i = 0; i < cell.rooms.length; ++i) {
            RoomDef roomDef = cell.rooms[i];
            if ((!bl3 || cell.roomNoPower[i]) && !LightMemory.ownPower(roomDef.id) || !roomDef.lightsActive) continue;
            for (RoomDef.RoomRect roomRect : roomDef.rects) {
                intList2.add(new int[]{roomRect.x, roomRect.y, roomDef.level, roomRect.w, roomRect.h}, 0, 5);
            }
        }
        return new Inputs(intList.array(), intList2.array());
    }

    private static void lamps(Cell cell, boolean bl, boolean bl2, int n, int n2, int n3, int n4, IntList intList) {
        boolean bl3 = LightMemory.cellSeen(cell.cellX, cell.cellY);
        for (int i = 0; i < cell.street.length; ++i) {
            boolean bl4;
            int n5 = i * 4;
            int n6 = cell.lamps[n5];
            int n7 = cell.lamps[n5 + 1];
            boolean bl5 = bl3 && LightMemory.seen(Math.floorDiv(n6, 8), Math.floorDiv(n7, 8));
            boolean bl6 = bl4 = !bl5 && MapSources.gridPowered(bl, bl2, cell.street[i], cell.inBuilding[i], cell.noPower[i]);
            if (!bl4 || n6 < n || n6 >= n3 || n7 < n2 || n7 >= n4) continue;
            intList.add(cell.lamps, n5, 4);
        }
        for (LightMemory.Lamp lamp : LightMemory.outdoorLamps(cell.cellX, cell.cellY)) {
            SourceRegister.Lamp lamp2 = lamp.light();
            if (!lamp.on(bl, bl2) || lamp2.x() < n || lamp2.x() >= n3 || lamp2.y() < n2 || lamp2.y() >= n4) continue;
            intList.add(new int[]{lamp2.x(), lamp2.y(), lamp2.z(), lamp.tile()}, 0, 4);
        }
    }

    private static ArrayList<RoomDef> rooms(IsoMetaGrid isoMetaGrid, int n, int n2) {
        IsoMetaCell isoMetaCell;
        ArrayList<RoomDef> arrayList = new ArrayList<RoomDef>();
        int n3 = n - isoMetaGrid.minX;
        int n4 = n2 - isoMetaGrid.minY;
        IsoMetaCell isoMetaCell2 = isoMetaCell = n3 >= 0 && n4 >= 0 && n3 <= isoMetaGrid.maxX - isoMetaGrid.minX && n4 <= isoMetaGrid.maxY - isoMetaGrid.minY ? isoMetaGrid.getCell(n3, n4) : null;
        if (isoMetaCell == null) {
            return arrayList;
        }
        for (RoomDef roomDef : isoMetaCell.roomList) {
            if (roomDef.level < 0 || roomDef.level >= 8 || roomDef.isEmptyOutside() || !MapSources.hasLights(roomDef)) continue;
            arrayList.add(roomDef);
        }
        return arrayList;
    }

    private CellLights() {
    }

    public static final class Cell {
        final int cellX;
        final int cellY;
        final int[] lamps;
        final boolean[] street;
        final boolean[] inBuilding;
        final boolean[] noPower;
        final RoomDef[] rooms;
        final boolean[] roomNoPower;

        private Cell(int n, int n2, int[] nArray, boolean[] blArray, boolean[] blArray2, boolean[] blArray3, RoomDef[] roomDefArray, boolean[] blArray4) {
            this.cellX = n;
            this.cellY = n2;
            this.lamps = nArray;
            this.street = blArray;
            this.inBuilding = blArray2;
            this.noPower = blArray3;
            this.rooms = roomDefArray;
            this.roomNoPower = blArray4;
        }
    }

    private static final class IntList {
        private int[] a = new int[64];
        private int n;

        private IntList() {
        }

        void add(int[] nArray, int n, int n2) {
            if (this.n + n2 > this.a.length) {
                this.a = Arrays.copyOf(this.a, Math.max(this.a.length * 2, this.n + n2));
            }
            System.arraycopy(nArray, n, this.a, this.n, n2);
            this.n += n2;
        }

        int[] array() {
            return Arrays.copyOf(this.a, this.n);
        }
    }

    public record Inputs(int[] lamps, int[] rooms) {
        public boolean same(Inputs inputs) {
            return inputs != null && Arrays.equals(this.lamps, inputs.lamps) && Arrays.equals(this.rooms, inputs.rooms);
        }
    }
}

