/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.util.IdentityHashMap;
import viewpoint.far.FarWorld;
import viewpoint.platform.Caches;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

final class CellRooms {
    static short[][] of(FarWorld.Slot slot) {
        IsoMetaCell isoMetaCell;
        slot.used = Caches.tick();
        if (slot.rooms != null) {
            return slot.rooms;
        }
        short[][] sArrayArray = new short[Math.max(1, slot.maxLevel + 1)][];
        IdentityHashMap<RoomDef, Integer> identityHashMap = new IdentityHashMap<RoomDef, Integer>();
        IsoMetaGrid isoMetaGrid = IsoWorld.instance.getMetaGrid();
        int n = slot.cellX - isoMetaGrid.minX;
        int n2 = slot.cellY - isoMetaGrid.minY;
        IsoMetaCell isoMetaCell2 = isoMetaCell = n >= 0 && n2 >= 0 && n <= isoMetaGrid.maxX - isoMetaGrid.minX && n2 <= isoMetaGrid.maxY - isoMetaGrid.minY ? isoMetaGrid.getCell(n, n2) : null;
        if (isoMetaCell != null) {
            int n3 = slot.cellX * 256;
            int n4 = slot.cellY * 256;
            for (RoomDef roomDef : isoMetaCell.roomList) {
                RoomDef roomDef2;
                Integer n5;
                if (roomDef.level < 0 || roomDef.level >= sArrayArray.length || roomDef.isEmptyOutside()) continue;
                short[] sArray = sArrayArray[roomDef.level];
                if (sArray == null) {
                    sArray = sArrayArray[roomDef.level] = new short[65536];
                }
                if ((n5 = (Integer)identityHashMap.get(roomDef2 = roomDef.building != null ? roomDef.building : roomDef)) == null) {
                    n5 = identityHashMap.size() + 1;
                    identityHashMap.put(roomDef2, n5);
                }
                for (RoomDef.RoomRect roomRect : roomDef.rects) {
                    for (int i = Math.max(0, roomRect.y - n4); i < Math.min(256, roomRect.y + roomRect.h - n4); ++i) {
                        for (int j = Math.max(0, roomRect.x - n3); j < Math.min(256, roomRect.x + roomRect.w - n3); ++j) {
                            sArray[i * 256 + j] = (short)n5.intValue();
                        }
                    }
                }
            }
        }
        slot.buildings = identityHashMap.size();
        slot.rooms = sArrayArray;
        return sArrayArray;
    }

    private CellRooms() {
    }
}

