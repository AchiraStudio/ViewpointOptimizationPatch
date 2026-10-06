/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.util.ArrayList;
import java.util.Arrays;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

final class BlockRooms {
    private static final int SIZE = 64;
    final long[][] ids;
    final ArrayList<RoomDef> lit = new ArrayList();

    private BlockRooms(int n) {
        this.ids = new long[n][];
    }

    static BlockRooms of(int n, int n2, int n3) {
        IsoMetaCell isoMetaCell;
        BlockRooms blockRooms = new BlockRooms(n3);
        int n4 = 4;
        IsoMetaGrid isoMetaGrid = IsoWorld.instance.getMetaGrid();
        int n5 = Math.floorDiv(n, n4) - isoMetaGrid.minX;
        int n6 = Math.floorDiv(n2, n4) - isoMetaGrid.minY;
        IsoMetaCell isoMetaCell2 = isoMetaCell = n5 >= 0 && n6 >= 0 && n5 <= isoMetaGrid.maxX - isoMetaGrid.minX && n6 <= isoMetaGrid.maxY - isoMetaGrid.minY ? isoMetaGrid.getCell(n5, n6) : null;
        if (isoMetaCell != null) {
            for (RoomDef roomDef : isoMetaCell.roomList) {
                blockRooms.add(roomDef, n * 64, n2 * 64);
            }
        }
        return blockRooms;
    }

    private void add(RoomDef roomDef, int n, int n2) {
        if (roomDef.level < 0 || roomDef.level >= this.ids.length || roomDef.isEmptyOutside() || roomDef.rects.isEmpty()) {
            return;
        }
        RoomDef.RoomRect roomRect = roomDef.rects.get(0);
        if (roomRect.x >= n && roomRect.y >= n2 && roomRect.x < n + 64 && roomRect.y < n2 + 64) {
            this.lit.add(roomDef);
        }
        for (RoomDef.RoomRect roomRect2 : roomDef.rects) {
            for (int i = Math.max(0, roomRect2.y - n2); i < Math.min(64, roomRect2.y + roomRect2.h - n2); ++i) {
                for (int j = Math.max(0, roomRect2.x - n); j < Math.min(64, roomRect2.x + roomRect2.w - n); ++j) {
                    this.level((int)roomDef.level)[i * 64 + j] = roomDef.id;
                }
            }
        }
    }

    private long[] level(int n) {
        if (this.ids[n] == null) {
            this.ids[n] = new long[4096];
            Arrays.fill(this.ids[n], -1L);
        }
        return this.ids[n];
    }
}

