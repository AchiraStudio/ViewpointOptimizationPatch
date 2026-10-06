/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.EnumMap;
import java.util.Map;

public final class Pipeline {
    public static final Pipeline LIBRARY = Pipeline.of(new EnumMap<Knob, String>(Knob.class));
    public final boolean shadows;
    public final boolean flashlightShadow;
    public final boolean farShadows;
    public final boolean farLamps;
    public final boolean clouds;
    public final boolean volumetrics;
    public final boolean indirect;
    public final boolean bloom;
    public final boolean taa;
    public final boolean puddles;
    public final int shadowResolution;
    public final int shadowCascades;
    public final int lampShadows;
    public final int lampShadowResolution;
    public final int cloudResolution;
    public final int cloudSteps;
    public final int rainDensity;
    final String defines;

    private Pipeline(int[] nArray) {
        this.shadows = nArray[Knob.SHADOWS.ordinal()] != 0;
        this.shadowResolution = nArray[Knob.SHADOW_RESOLUTION.ordinal()];
        this.shadowCascades = nArray[Knob.SHADOW_CASCADES.ordinal()];
        this.flashlightShadow = nArray[Knob.FLASHLIGHT_SHADOW.ordinal()] != 0;
        this.lampShadows = nArray[Knob.LAMP_SHADOWS.ordinal()];
        this.lampShadowResolution = nArray[Knob.LAMP_SHADOW_RESOLUTION.ordinal()];
        this.farShadows = nArray[Knob.FAR_SHADOWS.ordinal()] != 0;
        this.farLamps = nArray[Knob.FAR_LAMPS.ordinal()] != 0;
        this.clouds = nArray[Knob.CLOUDS.ordinal()] != 0;
        this.cloudResolution = nArray[Knob.CLOUD_RESOLUTION.ordinal()];
        this.cloudSteps = nArray[Knob.CLOUD_STEPS.ordinal()];
        this.rainDensity = nArray[Knob.RAIN_DENSITY.ordinal()];
        this.volumetrics = nArray[Knob.VOLUMETRICS.ordinal()] != 0;
        this.indirect = nArray[Knob.INDIRECT.ordinal()] != 0;
        this.bloom = nArray[Knob.BLOOM.ordinal()] != 0;
        this.taa = nArray[Knob.TAA.ordinal()] != 0;
        this.puddles = nArray[Knob.PUDDLES.ordinal()] != 0;
        StringBuilder stringBuilder = new StringBuilder();
        for (Knob knob : Knob.values()) {
            stringBuilder.append("#define PIPELINE_").append(knob.name()).append(' ').append(nArray[knob.ordinal()]).append('\n');
        }
        this.defines = stringBuilder.toString();
    }

    static Pipeline of(Map<Knob, String> map) {
        int[] nArray = new int[Knob.values().length];
        for (Knob knob : Knob.values()) {
            nArray[knob.ordinal()] = knob.parse(map.getOrDefault((Object)knob, knob.library));
        }
        return new Pipeline(nArray);
    }

    public static enum Knob {
        SHADOWS("shadows", "true"),
        SHADOW_RESOLUTION("shadowResolution", "2048", 256, 16384),
        SHADOW_CASCADES("shadowCascades", "3", 1, 3),
        FLASHLIGHT_SHADOW("flashlightShadow", "true"),
        LAMP_SHADOWS("lampShadows", "2", 0, 8),
        LAMP_SHADOW_RESOLUTION("lampShadowResolution", "512", 128, 2048),
        FAR_SHADOWS("farShadows", "true"),
        FAR_LAMPS("farLamps", "true"),
        CLOUDS("clouds", "true"),
        CLOUD_RESOLUTION("cloudResolution", "50", 25, 100),
        CLOUD_STEPS("cloudSteps", "40", 8, 128),
        VOLUMETRICS("volumetrics", "true"),
        INDIRECT("indirect", "true"),
        BLOOM("bloom", "true"),
        TAA("taa", "true"),
        PUDDLES("puddles", "true"),
        RAIN_DENSITY("rainDensity", "100", 0, 200);

        final String key;
        final String library;
        private final int min;
        private final int max;
        private final boolean toggle;

        private Knob(String string2, String string3) {
            this(string2, string3, 0, 1, true);
        }

        private Knob(String string2, String string3, int n2, int n3) {
            this(string2, string3, n2, n3, false);
        }

        private Knob(String string2, String string3, int n2, int n3, boolean bl) {
            this.key = string2;
            this.library = string3;
            this.min = n2;
            this.max = n3;
            this.toggle = bl;
        }

        static Knob named(String string) {
            for (Knob knob : Knob.values()) {
                if (!knob.key.equals(string)) continue;
                return knob;
            }
            return null;
        }

        int parse(String string) {
            String string2 = string.trim();
            if (this.toggle) {
                if (!string2.equals("true") && !string2.equals("false")) {
                    throw new IllegalArgumentException("pipeline." + this.key + " is true or false, not " + string);
                }
                return string2.equals("true") ? 1 : 0;
            }
            try {
                int n = Integer.parseInt(string2);
                if (n >= this.min && n <= this.max) {
                    return n;
                }
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
            throw new IllegalArgumentException("pipeline." + this.key + " is a whole number " + this.min + " to " + this.max + ", not " + string);
        }
    }
}

