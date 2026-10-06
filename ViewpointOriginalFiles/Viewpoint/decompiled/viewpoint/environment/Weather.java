/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.weather.ClimateManager
 */
package viewpoint.environment;

import viewpoint.core.Frame;
import viewpoint.environment.Lightning;
import viewpoint.environment.ShoreField;
import viewpoint.render.SceneData;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoPuddles;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.weather.ClimateManager;

public final class Weather {
    private static final int ROWS_PER_FRAME = 8;
    private static final float EYE_SKY_PORCH = 0.8f;
    private static final float EYE_SKY_INSIDE = 0.5f;
    public static final float WIND_MOST = 6.0f;
    private static final float SHEETS_CALM = 0.35f;
    private static final float SHEETS_WINDY = 0.8f;
    private static final float GUSTS_CALM = 0.3f;
    private static final float MAX_STEP_SECONDS = 0.25f;
    private static final byte[] exposure = new byte[4096];
    private static final byte[] rooms = new byte[exposure.length];
    private static final byte[] water = new byte[exposure.length];
    private static final byte[] oldExposure = new byte[exposure.length];
    private static final byte[] oldRooms = new byte[exposure.length];
    private static final byte[] oldWater = new byte[exposure.length];
    private static final byte[] shore = new byte[exposure.length];
    private static boolean waterChanged = true;
    private static int gridX;
    private static int gridY;
    private static int gridZ;
    private static int nextRow;
    private static double windCarriedX;
    private static double windCarriedZ;
    private static long carriedNanos;

    public static void snapshot(Frame frame, IsoCell isoCell) {
        int n;
        SceneData sceneData = frame.scene;
        ClimateManager climateManager = ClimateManager.getInstance();
        sceneData.rain = Weather.clamp01(climateManager.getRainIntensity());
        sceneData.snow = Weather.clamp01(climateManager.getSnowIntensity());
        float f = Weather.clamp01(climateManager.getWindIntensity());
        float f2 = 6.0f * f;
        sceneData.windX = -((float)Math.cos(climateManager.getWindAngleRadians())) * f2;
        sceneData.windZ = -((float)Math.sin(climateManager.getWindAngleRadians())) * f2;
        sceneData.sheets = Weather.sheets(f);
        sceneData.gusts = Weather.gusts(f);
        Weather.carry(sceneData);
        Lightning.snapshot(sceneData, frame.camX, frame.camY);
        IsoPuddles isoPuddles = IsoPuddles.getInstance();
        sceneData.puddles = Weather.clamp01(isoPuddles.getPuddlesSizeFinalValue());
        sceneData.wetGround = Weather.clamp01(isoPuddles.getWetGroundFinalValue());
        int n2 = 64;
        int n3 = n2 / 2;
        int n4 = (int)Math.floor(frame.camX) - n3;
        int n5 = (int)Math.floor(frame.camY) - n3;
        int n6 = (int)Math.floor(frame.camZ);
        sceneData.exposureAX = frame.camX - (float)n4;
        sceneData.exposureAY = frame.camY - (float)n5;
        Weather.move(isoCell, n4, n5, n6);
        for (n = 0; n < 8; ++n) {
            for (int i = 0; i < n2; ++i) {
                Weather.read(isoCell, i, nextRow);
            }
            nextRow = (nextRow + 1) % n2;
        }
        System.arraycopy(exposure, 0, sceneData.exposure, 0, exposure.length);
        System.arraycopy(rooms, 0, sceneData.rooms, 0, rooms.length);
        if (waterChanged) {
            waterChanged = false;
            ShoreField.compute(water, shore);
        }
        System.arraycopy(shore, 0, sceneData.water, 0, shore.length);
        n = exposure[n3 * n2 + n3];
        sceneData.eyeSky = n == -1 ? 1.0f : (n == 100 ? 0.8f : 0.5f);
    }

    public static float sheets(float f) {
        return 0.35f + 0.45000002f * f;
    }

    public static float gusts(float f) {
        return 0.3f + 0.7f * f;
    }

    private static void carry(SceneData sceneData) {
        long l = System.nanoTime();
        float f = carriedNanos == 0L ? 0.0f : (float)(l - carriedNanos) * 1.0E-9f;
        carriedNanos = l;
        if (f > 0.0f && f <= 0.25f) {
            windCarriedX += (double)(sceneData.windX * f);
            windCarriedZ += (double)(sceneData.windZ * f);
        }
        sceneData.windCarriedX = windCarriedX;
        sceneData.windCarriedZ = windCarriedZ;
    }

    private static void move(IsoCell isoCell, int n, int n2, int n3) {
        int n4 = 64;
        int n5 = n - gridX;
        int n6 = n2 - gridY;
        if (n3 == gridZ && n5 == 0 && n6 == 0) {
            return;
        }
        boolean bl = n3 == gridZ && Math.abs(n5) < n4 && Math.abs(n6) < n4;
        System.arraycopy(exposure, 0, oldExposure, 0, exposure.length);
        System.arraycopy(rooms, 0, oldRooms, 0, rooms.length);
        System.arraycopy(water, 0, oldWater, 0, water.length);
        waterChanged = true;
        gridX = n;
        gridY = n2;
        gridZ = n3;
        for (int i = 0; i < n4; ++i) {
            for (int j = 0; j < n4; ++j) {
                int n7 = j + n5;
                int n8 = i + n6;
                if (bl && n7 >= 0 && n8 >= 0 && n7 < n4 && n8 < n4) {
                    Weather.exposure[i * n4 + j] = oldExposure[n8 * n4 + n7];
                    Weather.rooms[i * n4 + j] = oldRooms[n8 * n4 + n7];
                    Weather.water[i * n4 + j] = oldWater[n8 * n4 + n7];
                    continue;
                }
                Weather.read(isoCell, j, i);
            }
        }
    }

    private static void read(IsoCell isoCell, int n, int n2) {
        boolean bl;
        int n3 = n2 * 64 + n;
        IsoGridSquare isoGridSquare = isoCell.getGridSquare(gridX + n, gridY + n2, gridZ);
        boolean bl2 = bl = isoGridSquare != null && isoGridSquare.isOutside();
        Weather.exposure[n3] = isoGridSquare == null || bl && !isoGridSquare.haveRoof ? -1 : (bl ? 100 : 0);
        Weather.rooms[n3] = isoGridSquare == null ? -1 : (int)Weather.room(isoGridSquare.getRoomID());
        IsoGridSquare isoGridSquare2 = gridZ == 0 ? isoGridSquare : isoCell.getGridSquare(gridX + n, gridY + n2, 0);
        byte by = isoGridSquare2 != null && isoGridSquare2.getProperties().has(IsoFlagType.water) ? (byte)-1 : 0;
        waterChanged |= water[n3] != by;
        Weather.water[n3] = by;
    }

    private static byte room(long l) {
        if (l < 0L) {
            return 0;
        }
        int n = (int)(l ^ l >>> 32) * -1640531535;
        return (byte)(1 + (n >>> 8) % 254);
    }

    private static float clamp01(float f) {
        return f < 0.0f ? 0.0f : Math.min(f, 1.0f);
    }

    private Weather() {
    }

    static {
        gridZ = Integer.MIN_VALUE;
    }
}

