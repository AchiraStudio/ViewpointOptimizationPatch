/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.environment;

import viewpoint.environment.SkyWeather;
import viewpoint.render.SceneData;

final class SkyProfile {
    private static final int SIDE = 48;
    private static final double STEP = 512.0;
    private static final int RING = 96;
    private static final double MAX_MOVE = 2048.0;
    private static final float MAX_FORECAST_HOURS = 1.0f;
    private static final float LEAST_SPEED = 0.1f;
    private final float[] ringCover = new float[96];
    private final float[] ringType = new float[96];
    private final float[] ringRain = new float[96];
    private final long[] ringKey = new long[96];
    private final float[] aheadCover = new float[48];
    private final float[] aheadType = new float[48];
    private final SkyWeather ahead = new SkyWeather();
    private final SkyWeather.Climate future = new SkyWeather.Climate();
    private boolean started;
    private double along;
    private double lastX;
    private double lastZ;
    private long lastKey;

    SkyProfile() {
    }

    void update(SceneData sceneData, double d, double d2, SkyWeather skyWeather, SkyWeather.Climate climate, float f) {
        float f2 = Math.max(0.1f, (float)Math.hypot(sceneData.cloudWindX, sceneData.cloudWindZ));
        float f3 = sceneData.cloudWindX / f2;
        float f4 = sceneData.cloudWindZ / f2;
        float f5 = sceneData.cloudCover;
        float f6 = skyWeather.type();
        float f7 = climate.precipitation;
        double d3 = d - this.lastX;
        double d4 = d2 - this.lastZ;
        if (!this.started || skyWeather.restarted() || d3 * d3 + d4 * d4 > 4194304.0) {
            this.started = true;
            this.along = 0.0;
            this.lastKey = 0L;
            for (int i = 0; i < 96; ++i) {
                this.keep(i, f5, f6, f7);
            }
        } else {
            this.along += d3 * (double)f3 + d4 * (double)f4;
        }
        this.lastX = d;
        this.lastZ = d2;
        this.record(f5, f6, f7);
        this.forecast(skyWeather, climate, 512.0 / (double)f2 * (double)f, f5);
        this.build(sceneData, f5, f6, f7);
    }

    private void record(float f, float f2, float f3) {
        long l = (long)Math.floor(this.along / 512.0);
        long l2 = Math.max(l, this.lastKey);
        for (long i = Math.max(Math.min(l, this.lastKey), l2 - 96L + 1L); i <= l2; ++i) {
            this.keep(i, f, f2, f3);
        }
        this.lastKey = l;
    }

    private void keep(long l, float f, float f2, float f3) {
        int n = (int)Math.floorMod(l, 96L);
        this.ringKey[n] = l;
        this.ringCover[n] = f;
        this.ringType[n] = f2;
        this.ringRain[n] = f3;
    }

    private void forecast(SkyWeather skyWeather, SkyWeather.Climate climate, double d, float f) {
        skyWeather.copyTo(this.ahead);
        this.future.set(climate);
        float f2 = skyWeather.trend();
        for (int i = 0; i < 48; ++i) {
            for (double d2 = d; d2 > 1.0E-6; d2 -= 1.0) {
                float f3 = (float)Math.min(d2, 1.0);
                this.future.hours += (double)f3;
                this.future.airMass = Math.max(-1.0f, Math.min(1.0f, this.future.airMass + f2 * f3));
                f = this.ahead.step(this.future, f3);
            }
            this.aheadCover[i] = f;
            this.aheadType[i] = this.ahead.type();
        }
    }

    private void build(SceneData sceneData, float f, float f2, float f3) {
        sceneData.coverAlong[48] = f;
        sceneData.typeAlong[48] = f2;
        sceneData.rainAlong[48] = f3;
        float f4 = f2;
        for (int i = 1; i <= 48; ++i) {
            sceneData.coverAlong[48 - i] = this.aheadCover[i - 1];
            sceneData.typeAlong[48 - i] = this.aheadType[i - 1];
            sceneData.rainAlong[48 - i] = f3;
            double d = (this.along + (double)i * 512.0) / 512.0 - 0.5;
            long l = (long)Math.floor(d);
            float f5 = (float)(d - (double)l);
            sceneData.coverAlong[48 + i] = SkyProfile.lerp(this.kept(this.ringCover, l, f), this.kept(this.ringCover, l + 1L, f), f5);
            sceneData.typeAlong[48 + i] = SkyProfile.lerp(this.kept(this.ringType, l, f2), this.kept(this.ringType, l + 1L, f2), f5);
            sceneData.rainAlong[48 + i] = SkyProfile.lerp(this.kept(this.ringRain, l, f3), this.kept(this.ringRain, l + 1L, f3), f5);
            f4 = Math.max(f4, Math.max(sceneData.typeAlong[48 - i], sceneData.typeAlong[48 + i]));
        }
        sceneData.cloudType = f4;
    }

    private float kept(float[] fArray, long l, float f) {
        int n = (int)Math.floorMod(l, 96L);
        return this.ringKey[n] == l ? fArray[n] : f;
    }

    private static float lerp(float f, float f2, float f3) {
        return f + (f2 - f) * f3;
    }
}

