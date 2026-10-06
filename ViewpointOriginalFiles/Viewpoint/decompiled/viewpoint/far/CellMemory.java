/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import viewpoint.far.FarWorld;
import viewpoint.platform.Caches;

final class CellMemory
implements Caches.Cache {
    CellMemory() {
    }

    @Override
    public long bytes() {
        long l = 0L;
        for (FarWorld.Slot slot : FarWorld.slots.values()) {
            l += CellMemory.bytes(slot);
        }
        return l;
    }

    @Override
    public void list(Caches.Survey survey) {
        for (FarWorld.Slot slot : FarWorld.slots.values()) {
            long l = CellMemory.bytes(slot);
            if (l <= 0L) continue;
            survey.add(slot.used, l);
        }
    }

    @Override
    public void evictBefore(long l) {
        for (FarWorld.Slot slot : FarWorld.slots.values()) {
            if (slot.used >= l) continue;
            slot.height = null;
            slot.biomes = null;
            slot.rooms = null;
            slot.facades = null;
            slot.groundTiles = null;
            slot.buildingBounds = null;
        }
    }

    private static long bytes(FarWorld.Slot slot) {
        long l = (slot.height == null ? 0L : (long)slot.height.length * 2L) + (slot.biomes == null ? 0L : (long)slot.biomes.length) + (slot.facades == null ? 0L : (long)slot.facades.length * 4L) + (slot.groundTiles == null ? 0L : slot.groundTiles.bytes()) + (slot.buildingBounds == null ? 0L : (long)slot.buildingBounds.length * 4L);
        if (slot.rooms != null) {
            for (short[] sArray : slot.rooms) {
                l += sArray == null ? 0L : (long)sArray.length * 2L;
            }
        }
        return l;
    }
}

