/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import viewpoint.render.SceneData;

final class IrisSun {
    static final double TILT = Math.toRadians(26.0);
    static final String PATH_ROTATION = "-26.0";
    final double[] sun = new double[3];
    final double[] moon = new double[3];
    double sunAngle;
    double shadowAngle;
    int worldTime;
    boolean sunLights;

    IrisSun() {
    }

    void update(SceneData sceneData, boolean bl) {
        boolean bl2;
        boolean bl3 = bl2 = sceneData.nightFraction >= 0.0f;
        if (!bl) {
            IrisSun.set(this.sun, sceneData.sunTrueX, sceneData.sunTrueY, sceneData.sunTrueZ);
            IrisSun.set(this.moon, sceneData.skyMoonX, sceneData.skyMoonY, sceneData.skyMoonZ);
            this.sunAngle = bl2 ? 0.5 + 0.5 * (double)sceneData.nightFraction : 0.5 * Math.max(0.0, Math.min(1.0, (double)sceneData.dayFraction));
        } else {
            double d;
            if (bl2) {
                d = Math.PI + Math.PI * (double)sceneData.nightFraction;
            } else {
                double d2 = Math.max(-1.0, Math.min(1.0, (double)sceneData.sunTrueY / Math.cos(TILT)));
                double d3 = Math.acos(d2);
                d = (double)sceneData.dayFraction < 0.5 ? 1.5707963267948966 - d3 : 1.5707963267948966 + d3;
            }
            this.sun[0] = Math.cos(d);
            this.sun[1] = Math.sin(d) * Math.cos(TILT);
            this.sun[2] = Math.sin(d) * Math.sin(TILT);
            this.moon[0] = -this.sun[0];
            this.moon[1] = -this.sun[1];
            this.moon[2] = -this.sun[2];
            this.sunAngle = d / (Math.PI * 2);
        }
        this.sunAngle -= Math.floor(this.sunAngle);
        this.sunLights = this.sunAngle < 0.5;
        this.shadowAngle = this.sunLights ? this.sunAngle : this.sunAngle - 0.5;
        this.worldTime = IrisSun.worldTime(this.sunAngle);
    }

    private static void set(double[] dArray, float f, float f2, float f3) {
        double d = Math.sqrt(f * f + f2 * f2 + f3 * f3);
        d = d > 0.0 ? d : 1.0;
        dArray[0] = (double)(-f) / d;
        dArray[1] = (double)f2 / d;
        dArray[2] = (double)(-f3) / d;
    }

    static int worldTime(double d) {
        double d2 = d - 0.25;
        d2 -= Math.floor(d2);
        double d3 = 0.0;
        double d4 = 1.0;
        for (int i = 0; i < 40; ++i) {
            double d5 = (d3 + d4) * 0.5;
            if (IrisSun.eased(d5) < d2) {
                d3 = d5;
                continue;
            }
            d4 = d5;
        }
        double d6 = (d3 + d4) * 0.5 + 0.25;
        d6 -= Math.floor(d6);
        return (int)Math.round(d6 * 24000.0) % 24000;
    }

    static double eased(double d) {
        double d2 = 0.5 - Math.cos(d * Math.PI) / 2.0;
        return (d * 2.0 + d2) / 3.0;
    }
}

