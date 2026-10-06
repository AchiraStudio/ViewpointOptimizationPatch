/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoCell
 */
package viewpoint.light;

import java.util.ArrayList;
import java.util.Arrays;
import viewpoint.light.LayoutCapture;
import viewpoint.light.LightJobs;
import viewpoint.light.MapCheck;
import viewpoint.platform.Caches;
import viewpoint.platform.LongMap;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;

public final class LightLayouts
implements Caches.Cache {
    static final int SQUARES = 64;
    private static final long LEVEL_BYTES = 952L;
    private static final LongMap<Level> levels = new LongMap(2048);
    private static final LongMap<Level> map = new LongMap(8192);
    static int captures;
    static long captureNanos;

    public static void captured(IsoCell isoCell, IsoChunk isoChunk, int n) {
        Level level;
        long l = System.nanoTime();
        long l2 = LightLayouts.key(isoChunk.wx, isoChunk.wy, n);
        Level level2 = map.get(l2);
        Level level3 = levels.get(l2);
        Level level4 = LayoutCapture.edges(isoCell, LayoutCapture.level(isoChunk, n), level2);
        Level level5 = level = level3 != null ? level3 : level2;
        boolean bl = level4 == null ? level3 != null : level3 == null || !level4.sameAs(level3);
        MapCheck.layout(level2, level4);
        if (bl) {
            Level level6;
            LightLayouts.put(l2, level4);
            Level level7 = level6 = level4 != null ? level4 : level2;
            if (level6 == null ? level != null : level == null || !level6.sameAs(level)) {
                LightJobs.layoutChanged(isoChunk.wx, isoChunk.wy, n);
            }
            LightLayouts.around(isoCell, isoChunk.wx, isoChunk.wy, n);
            if (level3 != null) {
                LightLayouts.below(isoCell, isoChunk, n - 1);
            }
        }
        ++captures;
        captureNanos += System.nanoTime() - l;
    }

    private static void around(IsoCell isoCell, int n, int n2, int n3) {
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                Level level = levels.get(LightLayouts.key(n + j, n2 + i, n3));
                if (j == 0 && i == 0 || level == null) continue;
                long l = LightLayouts.key(n + j, n2 + i, n3);
                Level level2 = LayoutCapture.edges(isoCell, LayoutCapture.facing(isoCell, level, n, n2), map.get(l));
                if (level2 == level || level2.sameAs(level)) continue;
                LightLayouts.put(l, level2);
                LightJobs.layoutChanged(n + j, n2 + i, n3);
            }
        }
    }

    private static void below(IsoCell isoCell, IsoChunk isoChunk, int n) {
        long l = LightLayouts.key(isoChunk.wx, isoChunk.wy, n);
        Level level = levels.get(l);
        if (level == null) {
            return;
        }
        Level level2 = LayoutCapture.edges(isoCell, LayoutCapture.level(isoChunk, n), map.get(l));
        if (level2 == null || !level2.sameAs(level)) {
            LightLayouts.put(l, level2);
            if (level2 != null || map.get(l) == null || !map.get(l).sameAs(level)) {
                LightJobs.layoutChanged(isoChunk.wx, isoChunk.wy, n);
            }
        }
    }

    private static void put(long l, Level level) {
        if (level == null) {
            levels.remove(l);
        } else {
            levels.put(l, level);
        }
    }

    static Level get(int n, int n2, int n3) {
        long l = LightLayouts.key(n, n2, n3);
        Level level = levels.get(l);
        return level != null ? level : map.get(l);
    }

    public static void forget(int n, int n2) {
        ArrayList arrayList = new ArrayList();
        levels.removeIf(level -> {
            boolean bl;
            boolean bl2 = bl = level.chunkX == n && level.chunkY == n2;
            if (bl) {
                arrayList.add(level);
            }
            return bl;
        });
        for (Level level2 : arrayList) {
            Level level3 = map.get(LightLayouts.key(level2.chunkX, level2.chunkY, level2.level));
            if (level3 != null && level3.sameAs(level2)) continue;
            LightJobs.layoutChanged(level2.chunkX, level2.chunkY, level2.level);
        }
    }

    static void mapped(IsoCell isoCell, Level level) {
        Level level2;
        long l = LightLayouts.key(level.chunkX, level.chunkY, level.level);
        Level level3 = map.get(l);
        Level level4 = levels.get(l);
        map.put(l, level);
        if (!(level4 != null || level3 != null && level3.sameAs(level))) {
            LightJobs.layoutChanged(level.chunkX, level.chunkY, level.level);
        } else if (level4 != null && isoCell != null && (level2 = LayoutCapture.edges(isoCell, level4, level)) != level4) {
            levels.put(l, level2);
            LightJobs.layoutChanged(level.chunkX, level.chunkY, level.level);
        }
    }

    static void unmapped(int n, int n2, int n3) {
        long l = LightLayouts.key(n, n2, n3);
        if (map.remove(l) != null && levels.get(l) == null) {
            LightJobs.layoutChanged(n, n2, n3);
        }
    }

    public static void clear() {
        levels.clear();
        map.clear();
    }

    static long key(int n, int n2, int n3) {
        return (long)(n & 0xFFFFFF) << 40 | (long)(n2 & 0xFFFFFF) << 16 | (long)(n3 + 32 & 0xFFFF);
    }

    @Override
    public long bytes() {
        return (long)(levels.size() + map.size()) * 952L;
    }

    @Override
    public void list(Caches.Survey survey) {
    }

    @Override
    public void evictBefore(long l) {
    }

    static {
        Caches.register(new LightLayouts());
    }

    static final class Level {
        final int chunkX;
        final int chunkY;
        final int level;
        final byte[] flags;
        final int[] passes;
        final long[] rooms;

        Level(int n, int n2, int n3, byte[] byArray, int[] nArray, long[] lArray) {
            this.chunkX = n;
            this.chunkY = n2;
            this.level = n3;
            this.flags = byArray;
            this.passes = nArray;
            this.rooms = lArray;
        }

        boolean sameAs(Level level) {
            return Arrays.equals(this.flags, level.flags) && Arrays.equals(this.passes, level.passes) && Arrays.equals(this.rooms, level.rooms);
        }
    }
}

