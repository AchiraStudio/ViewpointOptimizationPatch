/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.environment;

final class SkyWeather {
    static final float LAYER = 0.0f;
    static final float HEAPED = 0.5f;
    static final float TOWERING = 1.0f;
    private static final float DECK_TYPE = 0.25f;
    private static final float DAY_MOST = 0.75f;
    private static final float DAY_SKEW = 1.7f;
    private static final float DAY_HOURS = 22.0f;
    private static final float DAY_TURN = 0.35f;
    private static final float GAME_CLOUDS = 0.35f;
    private static final float SPREAD_FROM = 0.45f;
    private static final float RAIN_DECK_RAMP = 0.1f;
    private static final float PERIOD_BROKEN = 0.55f;
    private static final float NOISE = 0.18f;
    private static final float NOISE_HOURS = 5.0f;
    private static final float TREND_HOURS = 0.5f;
    private static final float MAX_LEAD_HOURS = 14.0f;
    private static final float DUE_HOURS = 1.5f;
    private static final float RAIN_CATCH_HOURS = 0.1f;
    private static final float TYPE_HOURS = 0.5f;
    private static final float MAX_STEP_HOURS = 2.0f;
    private static final Front FAIR = new Front(0.0f, 0.0f, 1.0f, 1.5f, 1.5f, 0.5f, 0.0f);
    private static final float PREVAILING = 270.0f;
    private static final float WANDER = 30.0f;
    private static final float WANDER_HOURS = 60.0f;
    private static final float FRONT_TURN = 40.0f;
    private static final float TURN_HOURS = 3.0f;
    private static final float ALOFT_SPEED = 5.0f;
    private static final float FRONT_FRESHENS = 0.5f;
    private boolean started;
    private boolean restarted;
    private boolean building;
    private float cover;
    private float type = 0.5f;
    private float front;
    private float trend;
    private float sampleAirMass;
    private float buildingSign;
    private float turn;
    private float freshen;
    private float aloftFrom = 270.0f;
    private float aloftSpeed = 5.0f;
    private double sampleHours;
    private double turnHours;
    private Front character = FAIR;

    SkyWeather() {
    }

    float step(Climate climate, float f) {
        boolean bl = this.restarted = !this.started || f < 0.0f || f > 2.0f;
        if (this.restarted) {
            this.started = true;
            this.building = false;
            this.front = 0.0f;
            this.trend = 0.0f;
            this.sampleAirMass = climate.airMass;
            this.sampleHours = climate.hours;
            this.cover = this.target(climate);
            this.type = this.typeTarget(climate);
            this.turn = this.turnTarget(climate);
            this.freshen = this.freshenTarget(climate);
            this.aloft(climate);
            return this.cover;
        }
        this.followAirMass(climate);
        float f2 = this.target(climate);
        float f3 = f2 <= this.cover ? this.character.clearHours() : (this.cover < SkyWeather.rainDeck(climate.precipitation) ? 0.1f : this.character.buildHours());
        this.cover += (f2 - this.cover) * (1.0f - (float)Math.exp(-f / f3));
        this.type += (this.typeTarget(climate) - this.type) * (1.0f - (float)Math.exp(-f / 0.5f));
        float f4 = 1.0f - (float)Math.exp(-f / 3.0f);
        this.turn += (this.turnTarget(climate) - this.turn) * f4;
        this.freshen += (this.freshenTarget(climate) - this.freshen) * f4;
        this.aloft(climate);
        return this.cover;
    }

    float type() {
        return this.type;
    }

    float aloftFrom() {
        return this.aloftFrom;
    }

    float aloftSpeed() {
        return this.aloftSpeed;
    }

    float front() {
        return this.front;
    }

    boolean restarted() {
        return this.restarted;
    }

    void restart() {
        this.started = false;
    }

    float trend() {
        return this.trend;
    }

    void copyTo(SkyWeather skyWeather) {
        skyWeather.started = this.started;
        skyWeather.restarted = this.restarted;
        skyWeather.building = this.building;
        skyWeather.cover = this.cover;
        skyWeather.type = this.type;
        skyWeather.front = this.front;
        skyWeather.trend = this.trend;
        skyWeather.sampleAirMass = this.sampleAirMass;
        skyWeather.buildingSign = this.buildingSign;
        skyWeather.turn = this.turn;
        skyWeather.freshen = this.freshen;
        skyWeather.aloftFrom = this.aloftFrom;
        skyWeather.aloftSpeed = this.aloftSpeed;
        skyWeather.sampleHours = this.sampleHours;
        skyWeather.turnHours = this.turnHours;
        skyWeather.character = this.character;
    }

    private void followAirMass(Climate climate) {
        float f;
        if (climate.hours - this.sampleHours >= 0.5) {
            float f2 = (float)((double)(climate.airMass - this.sampleAirMass) / (climate.hours - this.sampleHours));
            this.trend = this.trend == 0.0f ? f2 : 0.5f * (this.trend + f2);
            this.sampleAirMass = climate.airMass;
            this.sampleHours = climate.hours;
        }
        boolean bl = climate.airMass * this.trend < 0.0f;
        float f3 = f = bl ? Math.abs(climate.airMass / this.trend) : Float.POSITIVE_INFINITY;
        if (!this.building && bl && f < 14.0f) {
            this.building = true;
            this.buildingSign = Math.signum(climate.airMass);
            this.turnHours = Double.NaN;
            this.character = Front.of((long)Math.floor(climate.hours), this.trend > 0.0f);
        }
        if (!this.building) {
            this.front = 0.0f;
            return;
        }
        if (bl) {
            this.front = SkyWeather.clamp01(1.0f - f / this.character.leadHours());
        } else if (Double.isNaN(this.turnHours) && Math.signum(climate.airMass) != this.buildingSign) {
            this.turnHours = climate.hours;
            this.front = 1.0f;
        } else if (Double.isNaN(this.turnHours) || climate.hours - this.turnHours > 1.5) {
            this.building = false;
            this.front = 0.0f;
        }
    }

    private float target(Climate climate) {
        float f = SkyWeather.fairCover(climate.hours, climate.cloud);
        float f2 = this.front > 0.0f ? SkyWeather.lerp(f, Math.max(f, this.character.cover()), (float)Math.pow(this.front, this.character.curve())) : f;
        float f3 = climate.periodRunning ? SkyWeather.lerp(0.55f, 1.0f, climate.period) : 0.0f;
        float f4 = Math.max(f2, f3);
        float f5 = SkyWeather.clamp01(f4 + 0.72f * f4 * (1.0f - f4) * SkyWeather.noise(climate.hours));
        return Math.max(f5, SkyWeather.rainDeck(climate.precipitation));
    }

    private float typeTarget(Climate climate) {
        if (climate.storm) {
            return 1.0f;
        }
        float f = climate.periodRunning ? 1.0f : (this.front > 0.0f ? (float)Math.pow(this.front, this.character.curve()) : 0.0f);
        float f2 = SkyWeather.lerp(SkyWeather.fairType(SkyWeather.fairCover(climate.hours, climate.cloud)), this.character.type(), f);
        return SkyWeather.lerp(f2, Math.min(f2, 0.25f), Math.min(1.0f, climate.precipitation / 0.1f));
    }

    private void aloft(Climate climate) {
        this.aloftFrom = 270.0f + 30.0f * SkyWeather.wave(climate.hours / 60.0, 13) + 40.0f * this.turn;
        this.aloftSpeed = 5.0f * (1.0f + 0.25f * SkyWeather.wave(climate.hours / 60.0, 14)) * (1.0f + 0.5f * this.freshen);
    }

    private float turnTarget(Climate climate) {
        if (climate.periodRunning) {
            return this.character.veer();
        }
        return this.front > 0.0f ? -((float)Math.pow(this.front, this.character.curve())) : 0.0f;
    }

    private float freshenTarget(Climate climate) {
        float f = this.front > 0.0f ? (float)Math.pow(this.front, this.character.curve()) : 0.0f;
        return Math.max(f, climate.periodRunning ? climate.period : 0.0f);
    }

    static float fairCover(double d, float f) {
        float f2 = SkyWeather.daySky(d);
        return f2 + (1.0f - f2) * 0.35f * f;
    }

    static float daySky(double d) {
        double d2 = d / 22.0;
        long l = (long)Math.floor(d2);
        float f = SkyWeather.clamp01(((float)(d2 - (double)l) - 0.65f) / 0.35f);
        return SkyWeather.lerp(SkyWeather.dayDraw(l), SkyWeather.dayDraw(l + 1L), f * f * (3.0f - 2.0f * f));
    }

    private static float dayDraw(long l) {
        return 0.75f * (float)Math.pow(SkyWeather.random(l, 15), 1.7f);
    }

    private static float fairType(float f) {
        float f2 = SkyWeather.clamp01((f - 0.45f) / 0.3f);
        return SkyWeather.lerp(0.5f, 0.25f, f2 * f2 * (3.0f - 2.0f * f2));
    }

    static float rainDeck(float f) {
        return Math.min(1.0f, f / 0.1f) * (0.9f + 0.1f * f);
    }

    static float noise(double d) {
        return 0.7f * SkyWeather.wave(d / 5.0, 11) + 0.3f * SkyWeather.wave(d / 1.35, 12);
    }

    private static float wave(double d, int n) {
        long l = (long)Math.floor(d);
        float f = (float)(d - (double)l);
        float f2 = f * f * (3.0f - 2.0f * f);
        return SkyWeather.lerp(SkyWeather.random(l, n), SkyWeather.random(l + 1L, n), f2) * 2.0f - 1.0f;
    }

    static float random(long l, int n) {
        long l2 = l * -7046029254386353131L + (long)n * -4417276706812531889L;
        l2 = (l2 ^ l2 >>> 31) * -4658895280553007687L;
        l2 = (l2 ^ l2 >>> 29) * -7723592293110705685L;
        return (float)(l2 >>> 40) / 1.6777216E7f;
    }

    private static float lerp(float f, float f2, float f3) {
        return f + (f2 - f) * f3;
    }

    private static float clamp01(float f) {
        return f < 0.0f ? 0.0f : Math.min(f, 1.0f);
    }

    record Front(float leadHours, float cover, float curve, float buildHours, float clearHours, float type, float veer) {
        static Front of(long l, boolean bl) {
            return new Front(SkyWeather.lerp(3.0f, 9.0f, SkyWeather.random(l, 1)) * (bl ? 1.5f : 1.0f), SkyWeather.lerp(0.6f, 0.92f, SkyWeather.random(l, 2)), SkyWeather.lerp(0.6f, 1.8f, SkyWeather.random(l, 3)), SkyWeather.lerp(0.8f, 2.5f, SkyWeather.random(l, 4)), SkyWeather.lerp(0.8f, 2.5f, SkyWeather.random(l, 5)) * (bl ? 1.4f : 0.7f), bl ? SkyWeather.lerp(0.05f, 0.25f, SkyWeather.random(l, 6)) : SkyWeather.lerp(0.6f, 0.85f, SkyWeather.random(l, 6)), bl ? 0.0f : SkyWeather.lerp(0.6f, 1.0f, SkyWeather.random(l, 7)));
        }
    }

    static final class Climate {
        float cloud;
        float precipitation;
        float airMass;
        boolean periodRunning;
        float period;
        boolean storm;
        double hours;

        Climate() {
        }

        void set(Climate climate) {
            this.cloud = climate.cloud;
            this.precipitation = climate.precipitation;
            this.airMass = climate.airMass;
            this.periodRunning = climate.periodRunning;
            this.period = climate.period;
            this.storm = climate.storm;
            this.hours = climate.hours;
        }
    }
}

