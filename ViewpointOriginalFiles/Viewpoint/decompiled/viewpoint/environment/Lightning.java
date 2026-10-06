/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 */
package viewpoint.environment;

import viewpoint.environment.SkyWeather;
import viewpoint.render.SceneData;
import zombie.core.Core;

public final class Lightning {
    private static final int KEPT = 4;
    private static final float LASTS = 1.2f;
    private static final int MOST_STROKES = 4;
    private static final float STROKE_GAP = 0.15f;
    private static final float STROKE_FADE = 0.045f;
    private static final float GLOW = 0.25f;
    private static final float GLOW_FADE = 0.3f;
    private static final float HEIGHT = 900.0f;
    private static final int[] strikeX = new int[4];
    private static final int[] strikeY = new int[4];
    private static final long[] struckNanos = new long[4];
    private static int next;

    public static void struck(int n, int n2, boolean bl) {
        if (bl && !Core.getInstance().getOptionDisableLightningDuringStorms()) {
            Lightning.flash(n, n2);
        }
    }

    static void flash(int n, int n2) {
        Lightning.strikeX[Lightning.next] = n;
        Lightning.strikeY[Lightning.next] = n2;
        Lightning.struckNanos[Lightning.next] = System.nanoTime();
        next = (next + 1) % 4;
    }

    public static void snapshot(SceneData sceneData, float f, float f2) {
        long l = System.nanoTime();
        float f3 = 0.0f;
        for (int i = 0; i < 4; ++i) {
            float f4;
            float f5;
            float f6 = f5 = struckNanos[i] == 0L ? 1.2f : (float)(l - struckNanos[i]) * 1.0E-9f;
            if (f5 >= 1.2f || !((f4 = Lightning.brightness(f5, Lightning.seed(i))) > f3)) continue;
            f3 = f4;
            sceneData.lightningX = -((float)strikeX[i] - f);
            sceneData.lightningZ = -((float)strikeY[i] - f2);
        }
        sceneData.lightningY = 900.0f;
        sceneData.lightning = f3;
    }

    static float brightness(float f, long l) {
        if (f < 0.0f || f >= 1.2f) {
            return 0.0f;
        }
        int n = 2 + (int)(Lightning.unit(l, 0) * 3.0f);
        float f2 = 0.0f;
        float f3 = 1.0f;
        float f4 = 0.0f;
        for (int i = 0; i < n && f2 <= f; ++i) {
            f4 = Math.max(f4, f3 * (float)Math.exp(-(f - f2) / 0.045f));
            f2 += 0.15f * (0.3f + 0.7f * Lightning.unit(l, i + 1));
            f3 *= 0.55f + 0.3f * Lightning.unit(l, i + 11);
        }
        return Math.max(f4, 0.25f * (float)Math.exp(-f / 0.3f)) * (1.0f - f / 1.2f);
    }

    private static long seed(int n) {
        return (long)strikeX[n] * 73856093L ^ (long)strikeY[n] * 19349663L ^ struckNanos[n];
    }

    private static float unit(long l, int n) {
        return SkyWeather.random(l, n + 40);
    }

    private Lightning() {
    }
}

