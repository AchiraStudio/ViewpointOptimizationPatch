/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.util.Arrays;
import viewpoint.light.LevelLight;
import viewpoint.light.LightJobs;
import viewpoint.light.LightLayouts;
import viewpoint.light.MapLight;
import viewpoint.platform.LongMap;
import viewpoint.render.BandLight;

final class BandGrids {
    private static final int BAND_CHUNKS = 35;
    private static final LongMap<LevelLight> shown = new LongMap(4096);
    private static final int[] rings = new int[1226];
    private static int centreX = Integer.MIN_VALUE;
    private static int centreY;
    private static int reach;

    static void update(int n, int n2) {
        if (!MapLight.band()) {
            if (centreX != Integer.MIN_VALUE) {
                BandGrids.clear();
            }
            return;
        }
        if (n != centreX || n2 != centreY) {
            centreX = n;
            centreY = n2;
            BandGrids.fit();
        }
        for (LevelLight levelLight : LightJobs.worked) {
            if (!BandGrids.inside(levelLight.chunkX, levelLight.chunkY, levelLight.level)) continue;
            BandGrids.show(levelLight);
        }
        if ((long)shown.size() > BandLight.share()) {
            BandGrids.fit();
        }
    }

    static void dropped(int n, int n2, int n3) {
        if (shown.remove(LightLayouts.key(n, n2, n3)) != null) {
            BandLight.hide(n, n2, n3);
        }
    }

    static int reachChunks() {
        return (int)Math.sqrt(Math.max(reach, 0));
    }

    static void clear() {
        shown.clear();
        centreX = Integer.MIN_VALUE;
        BandLight.clear();
    }

    private static void fit() {
        Arrays.fill(rings, 0);
        LightJobs.lights(levelLight -> {
            int n = BandGrids.distance(levelLight.chunkX, levelLight.chunkY);
            if (n < rings.length && BandGrids.onLevels(levelLight.level)) {
                int n2 = n;
                rings[n2] = rings[n2] + 1;
            }
        });
        long l = BandLight.share();
        long l2 = 0L;
        reach = -1;
        while (reach + 1 < rings.length && l2 + (long)rings[reach + 1] <= l) {
            l2 += (long)rings[++reach];
        }
        shown.removeIf(levelLight -> {
            boolean bl;
            boolean bl2 = bl = !BandGrids.inside(levelLight.chunkX, levelLight.chunkY, levelLight.level);
            if (bl) {
                BandLight.hide(levelLight.chunkX, levelLight.chunkY, levelLight.level);
            }
            return bl;
        });
        LightJobs.lights(levelLight -> {
            if (BandGrids.inside(levelLight.chunkX, levelLight.chunkY, levelLight.level) && shown.get(LightLayouts.key(levelLight.chunkX, levelLight.chunkY, levelLight.level)) == null) {
                BandGrids.show(levelLight);
            }
        });
    }

    private static void show(LevelLight levelLight) {
        shown.put(LightLayouts.key(levelLight.chunkX, levelLight.chunkY, levelLight.level), levelLight);
        BandLight.show(levelLight.chunkX, levelLight.chunkY, levelLight.level, levelLight.grid);
    }

    private static boolean inside(int n, int n2, int n3) {
        return BandGrids.distance(n, n2) <= reach && BandGrids.onLevels(n3);
    }

    private static int distance(int n, int n2) {
        int n3 = n - centreX;
        int n4 = n2 - centreY;
        return n3 * n3 + n4 * n4;
    }

    private static boolean onLevels(int n) {
        return n >= 0 && n < 32;
    }

    private BandGrids() {
    }
}

