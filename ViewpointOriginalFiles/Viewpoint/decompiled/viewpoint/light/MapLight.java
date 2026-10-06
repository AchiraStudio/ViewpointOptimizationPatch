/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoCell
 */
package viewpoint.light;

import java.util.ArrayList;
import java.util.List;
import viewpoint.light.LightLayouts;
import viewpoint.light.MapLayout;
import viewpoint.light.MapSources;
import viewpoint.platform.LongMap;
import zombie.iso.IsoCell;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

public final class MapLight {
    public static final int MARGIN = 37;
    private static final LongMap<List<LightLayouts.Level>> blocks = new LongMap(128);
    private static int generation;
    private static boolean band;

    public static int generation() {
        return generation;
    }

    public static void install(MapLayout.Block block, ArrayList<RoomDef> arrayList) {
        long l = MapLight.key(block.bx, block.by);
        List<LightLayouts.Level> list = blocks.get(l);
        if (list != null) {
            for (LightLayouts.Level object : list) {
                if (MapLight.has(block.levels, object)) continue;
                LightLayouts.unmapped(object.chunkX, object.chunkY, object.level);
            }
        }
        IsoCell isoCell = IsoWorld.instance.getCell();
        for (LightLayouts.Level level : block.levels) {
            LightLayouts.mapped(isoCell, level);
        }
        blocks.put(l, block.levels);
        MapSources.add(block.bx, block.by, block.lamps, arrayList);
    }

    public static void drop(int n, int n2) {
        List<LightLayouts.Level> list = blocks.remove(MapLight.key(n, n2));
        if (list == null) {
            return;
        }
        for (LightLayouts.Level level : list) {
            LightLayouts.unmapped(level.chunkX, level.chunkY, level.level);
        }
        MapSources.drop(n, n2);
    }

    public static void update(IsoCell isoCell) {
        band = true;
        MapSources.update(isoCell);
    }

    public static void off() {
        band = false;
    }

    static boolean band() {
        return band;
    }

    static void clear() {
        blocks.clear();
        MapSources.clear();
        ++generation;
    }

    private static boolean has(List<LightLayouts.Level> list, LightLayouts.Level level) {
        for (LightLayouts.Level level2 : list) {
            if (level2.chunkX != level.chunkX || level2.chunkY != level.chunkY || level2.level != level.level) continue;
            return true;
        }
        return false;
    }

    private static long key(int n, int n2) {
        return (long)n << 32 | (long)n2 & 0xFFFFFFFFL;
    }

    private MapLight() {
    }
}

