/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.nio.ByteBuffer;
import viewpoint.light.BandGrids;
import viewpoint.light.Daylight;
import viewpoint.light.LevelLight;
import viewpoint.light.LightJobs;
import viewpoint.light.LightLayouts;
import viewpoint.light.LightModel;
import viewpoint.light.MapCheck;
import viewpoint.light.MapLight;
import viewpoint.platform.IrisPacks;
import viewpoint.platform.Profile;
import viewpoint.render.BandLight;

public final class OwnedLight {
    private static int sky;
    private static long mainNanos;
    private static final int CHECK_EVERY = 60;
    private static volatile long publishedCaptures;
    private static volatile long publishedCaptureNanos;
    private static volatile long publishedMainNanos;
    private static volatile long publishedSources;
    private static volatile long publishedSourceNanos;
    private static volatile long publishedLevels;
    private static volatile long publishedLevelNanos;
    private static volatile String publishedCheck;
    private static volatile int publishedBandReach;
    private static int publishes;
    private static long[] reported;

    public static void update(int n, int n2, int n3, Grids grids) {
        long l = System.nanoTime();
        sky = Daylight.skyLevel();
        LightJobs.update(n, n2, n3);
        BandGrids.update(n, n2);
        for (LevelLight levelLight : LightJobs.worked) {
            grids.rewrite(levelLight.chunkX, levelLight.chunkY, levelLight.level);
        }
        mainNanos += System.nanoTime() - l;
        OwnedLight.publish();
    }

    public static boolean write(ByteBuffer byteBuffer, int n, int n2, int n3) {
        LevelLight levelLight = LightJobs.light(n, n2, n3);
        if (levelLight == null) {
            return false;
        }
        long l = System.nanoTime();
        byteBuffer.put(0, levelLight.grid);
        mainNanos += System.nanoTime() - l;
        return true;
    }

    public static int lightAt(int n, int n2, int n3) {
        LevelLight levelLight = LightJobs.light(Math.floorDiv(n, 8), Math.floorDiv(n2, 8), n3);
        if (levelLight == null) {
            return -1;
        }
        int n4 = OwnedLight.square(n, n2);
        return LightModel.combine(sky, levelLight.steps[n4], levelLight.light[n4]);
    }

    public static int modelLightAt(int n, int n2, int n3) {
        if (IrisPacks.active() == null) {
            return OwnedLight.lightAt(n, n2, n3);
        }
        LevelLight levelLight = LightJobs.light(Math.floorDiv(n, 8), Math.floorDiv(n2, 8), n3);
        return levelLight == null ? 0 : levelLight.light[OwnedLight.square(n, n2)];
    }

    private static int square(int n, int n2) {
        return Math.floorMod(n2, 8) * 8 + Math.floorMod(n, 8);
    }

    public static void forget(int n, int n2) {
        LightLayouts.forget(n, n2);
    }

    public static void clear() {
        LightLayouts.clear();
        LightJobs.clear();
        BandGrids.clear();
        MapLight.clear();
        MapCheck.clear();
    }

    private static void publish() {
        publishedCaptures += (long)LightLayouts.captures;
        publishedCaptureNanos += LightLayouts.captureNanos;
        LightLayouts.captures = 0;
        LightLayouts.captureNanos = 0L;
        publishedSources += (long)LightJobs.sourceJobs;
        publishedSourceNanos += LightJobs.sourceNanos;
        publishedLevels += (long)LightJobs.levelJobs;
        publishedLevelNanos += LightJobs.levelNanos;
        LightJobs.levelJobs = 0;
        LightJobs.sourceJobs = 0;
        LightJobs.levelNanos = 0L;
        LightJobs.sourceNanos = 0L;
        publishedMainNanos = mainNanos;
        publishedBandReach = BandGrids.reachChunks();
        if (publishes++ % 60 == 0) {
            publishedCheck = MapCheck.report();
        }
    }

    private static String report() {
        long[] lArray = new long[]{publishedCaptures, publishedCaptureNanos, publishedMainNanos, publishedSources, publishedSourceNanos, publishedLevels, publishedLevelNanos};
        long[] lArray2 = new long[lArray.length];
        for (int i = 0; i < lArray.length; ++i) {
            lArray2[i] = lArray[i] - reported[i];
        }
        reported = lArray;
        return "layouts " + lArray2[0] + " (" + OwnedLight.ms(lArray2[1]) + " ms), main " + OwnedLight.ms(lArray2[2]) + " ms, sources " + lArray2[3] + " (" + OwnedLight.ms(lArray2[4]) + " ms on workers), levels " + lArray2[5] + " (" + OwnedLight.ms(lArray2[6]) + " ms on workers), band grids " + BandLight.grids() + " of " + BandLight.share() + " within " + publishedBandReach + " chunks (" + BandLight.turnedAway() + " turned away), " + publishedCheck;
    }

    private static String ms(long l) {
        return String.format("%.1f", (double)l / 1000000.0);
    }

    private OwnedLight() {
    }

    static {
        Profile.ownedLightReport = OwnedLight::report;
        publishedCheck = "";
        reported = new long[7];
    }

    public static interface Grids {
        public void rewrite(int var1, int var2, int var3);
    }
}

