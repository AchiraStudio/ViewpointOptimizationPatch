/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.opengl.RenderSettings
 *  zombie.core.opengl.RenderSettings$PlayerRenderSettings
 *  zombie.erosion.season.ErosionSeason
 *  zombie.iso.weather.ClimateManager
 *  zombie.iso.weather.ClimateMoon
 *  zombie.iso.weather.WeatherPeriod
 */
package viewpoint.environment;

import viewpoint.core.View;
import viewpoint.environment.SkyClock;
import viewpoint.environment.SkyProfile;
import viewpoint.environment.SkyWeather;
import viewpoint.far.FarWorld;
import viewpoint.platform.Profile;
import viewpoint.render.SceneData;
import zombie.GameTime;
import zombie.core.opengl.RenderSettings;
import zombie.erosion.season.ErosionSeason;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.ClimateMoon;
import zombie.iso.weather.WeatherPeriod;

public final class Atmosphere {
    private static final float FOGGY_END = 40.0f;
    private static final float CLEAR_UP_TO = 0.22f;
    private static final long START_NANOS = System.nanoTime();
    private static final SkyClock clock = new SkyClock();
    private static final SkyWeather weather = new SkyWeather();
    private static final SkyWeather.Climate now = new SkyWeather.Climate();
    private static final SkyProfile profile = new SkyProfile();
    private static long lastNanos = START_NANOS;
    private static double carriedX;
    private static double carriedZ;
    private static final float MAX_ELEVATION;
    private static final float MOON_ARC = 0.75f;
    private static final float ARC_STEADY = 0.65f;
    private static final float NIGHT_DEPTH;
    private static final float HALF_LIGHT = 0.16666667f;
    private static final float MIN_SHADOW_ELEVATION;
    private static final int MOON_PHASES = 8;
    private static float dirX;
    private static float dirY;
    private static float dirZ;

    public static void gameLoaded() {
        weather.restart();
    }

    public static void apply(SceneData sceneData) {
        float f;
        ClimateManager climateManager = ClimateManager.getInstance();
        float f2 = Atmosphere.clamp01(climateManager.getFogIntensity());
        float f3 = Math.max(sceneData.rain, sceneData.snow);
        float f4 = Math.max(f2, Math.max(0.5f * sceneData.rain, 0.35f * sceneData.snow));
        Atmosphere.sky(sceneData, climateManager, f3);
        sceneData.overcast = f = Atmosphere.overcast(sceneData.cloudCover, f2, f3);
        ErosionSeason erosionSeason = climateManager.getSeason();
        float f5 = erosionSeason != null ? erosionSeason.getDawn() : 6.0f;
        float f6 = erosionSeason != null ? erosionSeason.getDusk() : 20.0f;
        float f7 = GameTime.getInstance().getTimeOfDay();
        float f8 = Atmosphere.sunShift(f5, f6);
        float f9 = f5 - f8;
        float f10 = f6 + f8;
        float f11 = (f7 - f9) / Math.max(1.0f, f10 - f9);
        float f12 = Atmosphere.elevation(f7, f9, f10);
        float f13 = Math.max(1.0f, 24.0f - (f10 - f9));
        float f14 = f7 >= f10 ? (f7 - f10) / f13 : (f7 < f9 ? (f7 + 24.0f - f10) / f13 : -1.0f);
        boolean bl = f14 >= 0.0f && f12 < (float)Math.toRadians(-6.0);
        float f15 = (float)Math.sin(Math.PI * (double)f14) * MAX_ELEVATION * 0.75f;
        if (bl) {
            Atmosphere.sun(sceneData, f14, Math.max(f15, MIN_SHADOW_ELEVATION));
            Atmosphere.direction(f14, f15, false);
        } else {
            Atmosphere.sun(sceneData, f11, Math.max(f12, MIN_SHADOW_ELEVATION));
            Atmosphere.direction(f11, f12, false);
        }
        Atmosphere.seen(sceneData, f11, f14, f12);
        Atmosphere.direction(f11, f12, false);
        sceneData.sunTrueX = dirX;
        sceneData.sunTrueY = dirY;
        sceneData.sunTrueZ = dirZ;
        sceneData.nightFraction = f14;
        float f16 = (float)Math.toDegrees(f12);
        Atmosphere.sunLight(sceneData, bl, f, f16);
        float f17 = Math.max(Atmosphere.clamp01(climateManager.getDayLightStrength()), 0.6f * Atmosphere.smooth(-5.0f, 14.0f, f16));
        float f18 = Atmosphere.clamp01(1.0f - Math.abs(f16 - 2.0f) / 12.0f);
        float f19 = Atmosphere.lerp(0.004f, 0.1f, f17);
        float f20 = Atmosphere.lerp(0.007f, 0.3f, f17);
        float f21 = Atmosphere.lerp(0.022f, 0.74f, f17);
        float f22 = Atmosphere.lerp(0.018f, 0.6f, f17);
        float f23 = Atmosphere.lerp(0.028f, 0.76f, f17);
        float f24 = Atmosphere.lerp(0.055f, 0.95f, f17);
        f22 = Atmosphere.lerp(f22, 0.92f, f18 * 0.8f);
        f23 = Atmosphere.lerp(f23, 0.55f, f18 * 0.8f);
        f24 = Atmosphere.lerp(f24, 0.34f, f18 * 0.8f);
        float f25 = Atmosphere.lerp(0.03f, 0.6f, f17) * (1.0f - 0.35f * sceneData.rain - 0.1f * sceneData.snow);
        sceneData.zenithR = Atmosphere.lerp(f19, f25 * 0.85f, f);
        sceneData.zenithG = Atmosphere.lerp(f20, f25 * 0.88f, f);
        sceneData.zenithB = Atmosphere.lerp(f21, f25 * 0.95f, f);
        sceneData.fogR = Atmosphere.lerp(f22, f25, f);
        sceneData.fogG = Atmosphere.lerp(f23, f25 * 1.02f, f);
        sceneData.fogB = Atmosphere.lerp(f24, f25 * 1.06f, f);
        float f26 = f7 - f5;
        float f27 = f6 - f7;
        float f28 = Atmosphere.smooth(-0.75f, 0.25f, f26) * Atmosphere.smooth(3.0f, 1.0f, f26);
        float f29 = Atmosphere.smooth(1.5f, 0.0f, f27) * Atmosphere.smooth(-2.0f, -0.5f, f27);
        float f30 = 1.0f - 0.6f * Atmosphere.clamp01(climateManager.getWindIntensity());
        float f31 = sceneData.rain > 0.0f ? 0.1f + 0.55f * sceneData.rain : 0.0f;
        sceneData.mist = Atmosphere.clamp01(Math.max(Math.max(f2, f31), Math.max(0.4f * sceneData.snow, (0.4f * f28 + 0.15f * f29) * f30)));
        sceneData.night = 1.0f - f17;
        Atmosphere.tint(sceneData, climateManager);
        Atmosphere.fogReach(sceneData, f4);
    }

    private static void seen(SceneData sceneData, float f, float f2, float f3) {
        sceneData.skySunX = dirX;
        sceneData.skySunY = dirY;
        sceneData.skySunZ = dirZ;
        sceneData.dayFraction = f;
        sceneData.sunSine = (float)Math.sin(f3);
        sceneData.moonPhase = (ClimateMoon.getInstance().getCurrentMoonPhase() + 4) % 8;
        Atmosphere.skyMoon(sceneData, f, f2);
    }

    static void skyMoon(SceneData sceneData, float f, float f2) {
        boolean bl = f2 >= 0.0f;
        float f3 = bl ? f2 : Atmosphere.clamp01(f);
        float f4 = (float)Math.sin(Math.PI * (double)f3) * MAX_ELEVATION * 0.75f;
        Atmosphere.direction(bl ? f3 : 1.0f - f3, bl ? f4 : -f4, false);
        sceneData.skyMoonX = dirX;
        sceneData.skyMoonY = dirY;
        sceneData.skyMoonZ = dirZ;
    }

    public static float overcast(float f, float f2, float f3) {
        float f4 = Atmosphere.clamp01((f - 0.22f) / 0.78f);
        return Math.max(Math.max(f2, f3), f4 * 0.8f);
    }

    private static void sunLight(SceneData sceneData, boolean bl, float f, float f2) {
        if (bl) {
            float f3 = Atmosphere.clamp01(ClimateMoon.getInstance().getMoonFloat());
            sceneData.sunStrength = 0.14f * f3 * (1.0f - 0.9f * f) * Atmosphere.smooth(-6.0f, -12.0f, f2);
            sceneData.sunR = 0.62f;
            sceneData.sunG = 0.74f;
            sceneData.sunB = 1.0f;
            sceneData.skyGlow = sceneData.sunStrength;
        } else {
            sceneData.sunStrength = Atmosphere.smooth(0.0f, 12.0f, f2) * (1.0f - 0.85f * f);
            float f4 = Atmosphere.smooth(4.0f, 35.0f, f2);
            sceneData.sunR = 1.0f;
            sceneData.sunG = Atmosphere.lerp(0.62f, 0.96f, f4);
            sceneData.sunB = Atmosphere.lerp(0.35f, 0.88f, f4);
            sceneData.skyGlow = Atmosphere.smooth(-6.0f, 4.0f, f2) * (1.0f - 0.85f * f);
        }
    }

    private static void sky(SceneData sceneData, ClimateManager climateManager, float f) {
        long l = System.nanoTime();
        sceneData.time = (float)(l - START_NANOS) * 1.0E-9f;
        float f2 = Atmosphere.skyStep(l);
        sceneData.cloudTime = (float)clock.seconds();
        GameTime gameTime = GameTime.getInstance();
        WeatherPeriod weatherPeriod = climateManager.getWeatherPeriod();
        Atmosphere.now.cloud = Atmosphere.clamp01(climateManager.getCloudIntensity());
        Atmosphere.now.precipitation = f;
        Atmosphere.now.airMass = climateManager.getAirMass();
        Atmosphere.now.periodRunning = weatherPeriod != null && weatherPeriod.isRunning();
        Atmosphere.now.period = Atmosphere.now.periodRunning ? Atmosphere.clamp01(weatherPeriod.getCurrentStrength()) : 0.0f;
        Atmosphere.now.storm = Atmosphere.now.periodRunning && (weatherPeriod.isThunderStorm() || weatherPeriod.isTropicalStorm());
        Atmosphere.now.hours = gameTime.getWorldAgeHours();
        float f3 = 24.0f / (60.0f * gameTime.getMinutesPerDay());
        sceneData.cloudCover = weather.step(now, f2 * f3);
        Atmosphere.driftClouds(sceneData, f2);
        profile.update(sceneData, sceneData.originX - sceneData.cloudCarriedX, sceneData.originZ - sceneData.cloudCarriedZ, weather, now, f3);
    }

    private static float skyStep(long l) {
        float f = (float)(l - lastNanos) * 1.0E-9f;
        lastNanos = l;
        GameTime gameTime = GameTime.getInstance();
        return clock.advance(f, gameTime.getTimeOfDay(), GameTime.isGamePaused(), gameTime.getMinutesPerDay());
    }

    private static void driftClouds(SceneData sceneData, float f) {
        double d = Math.toRadians(weather.aloftFrom());
        float f2 = weather.aloftSpeed();
        sceneData.cloudWindX = (float)Math.sin(d) * f2;
        sceneData.cloudWindZ = (float)(-Math.cos(d)) * f2;
        sceneData.cloudCarriedX = carriedX += (double)(f * sceneData.cloudWindX);
        sceneData.cloudCarriedZ = carriedZ += (double)(f * sceneData.cloudWindZ);
    }

    private static void tint(SceneData sceneData, ClimateManager climateManager) {
        RenderSettings.PlayerRenderSettings playerRenderSettings = RenderSettings.getInstance().getPlayerSettings(0);
        float f = playerRenderSettings.getRmod();
        float f2 = playerRenderSettings.getGmod();
        float f3 = playerRenderSettings.getBmod();
        float f4 = Math.max(0.001f, Math.max(f, Math.max(f2, f3)));
        float f5 = Atmosphere.lerp(Math.min(1.5f, f4), 1.0f, sceneData.night);
        float f6 = f / f4 * f5;
        float f7 = f2 / f4 * f5;
        float f8 = f3 / f4 * f5;
        sceneData.zenithR *= f6;
        sceneData.zenithG *= f7;
        sceneData.zenithB *= f8;
        sceneData.fogR *= f6;
        sceneData.fogG *= f7;
        sceneData.fogB *= f8;
        sceneData.desaturation = Atmosphere.clamp01(playerRenderSettings.getDesaturation());
        Profile.note = String.format("day %.2f night %.2f mod %.2f/%.2f/%.2f desat %.2f sun %.2f fog %.0f-%.0f rain %.2f cover %.2f type %.2f front %.2f aloft %.0f upwind %.2f downwind %.2f", Float.valueOf(Atmosphere.clamp01(climateManager.getDayLightStrength())), Float.valueOf(sceneData.night), Float.valueOf(f), Float.valueOf(f2), Float.valueOf(f3), Float.valueOf(playerRenderSettings.getDesaturation()), Float.valueOf(sceneData.sunStrength), Float.valueOf(sceneData.fogStart), Float.valueOf(sceneData.fogEnd), Float.valueOf(sceneData.rain), Float.valueOf(sceneData.cloudCover), Float.valueOf(weather.type()), Float.valueOf(weather.front()), Float.valueOf(weather.aloftFrom()), Float.valueOf(sceneData.coverAlong[0]), Float.valueOf(sceneData.coverAlong[sceneData.coverAlong.length - 1]));
    }

    private static void fogReach(SceneData sceneData, float f) {
        float f2 = View.radiusChunks() * 8;
        float f3 = Math.max(f2, FarWorld.reach());
        sceneData.nearReach = f2;
        sceneData.fogEnd = Atmosphere.lerp(f3, 40.0f, f);
        sceneData.fogStart = sceneData.fogEnd * Atmosphere.lerp(0.88f, 0.3f, f);
    }

    static float sunShift(float f, float f2) {
        return f2 > f ? 0.16666667f * (24.0f - (f2 - f)) : 0.0f;
    }

    static float elevation(float f, float f2, float f3) {
        if (f >= f2 && f <= f3) {
            float f4 = (float)Math.sin(Math.PI * (double)(f - f2) / (double)Math.max(1.0f, f3 - f2));
            return MAX_ELEVATION * (0.65f * f4 + 0.35000002f * f4 * f4);
        }
        float f5 = Math.max(1.0f, 24.0f - (f3 - f2));
        float f6 = f > f3 ? f - f3 : f + 24.0f - f3;
        float f7 = f < f2 ? f2 - f : f2 + 24.0f - f;
        return -NIGHT_DEPTH * Atmosphere.clamp01(Math.min(f6, f7) / (0.25f * f5));
    }

    private static void sun(SceneData sceneData, float f, float f2) {
        Atmosphere.direction(f, f2, true);
        sceneData.sunX = dirX;
        sceneData.sunY = dirY;
        sceneData.sunZ = dirZ;
    }

    private static void direction(float f, float f2, boolean bl) {
        double d = Math.toRadians(0.25);
        double d2 = Math.PI * (double)Math.min(1.0f, Math.max(0.0f, f));
        if (bl) {
            d2 = (double)Math.round(d2 / d) * d;
            f2 = (float)((double)Math.round((double)f2 / d) * d);
        }
        double d3 = Math.cos(d2);
        double d4 = 0.25 + 0.55 * Math.sin(d2);
        double d5 = Math.sqrt(d3 * d3 + d4 * d4);
        double d6 = Math.cos(f2);
        dirX = (float)(-d3 / d5 * d6);
        dirY = (float)Math.sin(f2);
        dirZ = (float)(-d4 / d5 * d6);
    }

    private static float smooth(float f, float f2, float f3) {
        float f4 = Atmosphere.clamp01((f3 - f) / (f2 - f));
        return f4 * f4 * (3.0f - 2.0f * f4);
    }

    private static float lerp(float f, float f2, float f3) {
        return f + (f2 - f) * f3;
    }

    private static float clamp01(float f) {
        return f < 0.0f ? 0.0f : Math.min(f, 1.0f);
    }

    private Atmosphere() {
    }

    static {
        MAX_ELEVATION = (float)Math.toRadians(64.0);
        NIGHT_DEPTH = (float)Math.toRadians(18.0);
        MIN_SHADOW_ELEVATION = (float)Math.toRadians(14.0);
    }
}

