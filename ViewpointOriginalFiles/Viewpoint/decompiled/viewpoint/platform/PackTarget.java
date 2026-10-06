/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import viewpoint.platform.PackOption;

public final class PackTarget {
    private static final int MAX_TEXELS = 4096;
    public final String name;
    public final Format format;
    private final float scale;
    private final int width;
    private final int height;
    public final boolean history;

    private PackTarget(String string, Format format, float f, int n, int n2, boolean bl) {
        this.name = string;
        this.format = format;
        this.scale = f;
        this.width = n;
        this.height = n2;
        this.history = bl;
    }

    static PackTarget parse(String string, String string2) {
        String string3 = "target." + string;
        if (!PackOption.ID.matcher(string).matches()) {
            throw new IllegalArgumentException(string3 + ": a target's name is letters and digits");
        }
        String[] stringArray = string2.trim().split("\\s+");
        if (stringArray.length < 2 || stringArray.length > 3 || stringArray.length == 3 && !stringArray[2].equals("history")) {
            throw new IllegalArgumentException(string3 + " is [format] [size] [history], not " + string2);
        }
        Format format = null;
        for (Format format2 : Format.values()) {
            format = format2.key.equals(stringArray[0]) ? format2 : format;
        }
        if (format == null) {
            throw new IllegalArgumentException(string3 + ": " + stringArray[0] + " is not rgba8, rgba16f, rg16f, r16f or r32f");
        }
        boolean bl = stringArray.length == 3;
        int n = stringArray[1].indexOf(120);
        if (n < 0) {
            float f = PackTarget.number(string3, stringArray[1]);
            if (f <= 0.0f || f > 1.0f) {
                throw new IllegalArgumentException(string3 + ": a share of the frame is over 0 and at most 1");
            }
            return new PackTarget(string, format, f, 0, 0, bl);
        }
        int n2 = (int)PackTarget.number(string3, stringArray[1].substring(0, n));
        int n3 = (int)PackTarget.number(string3, stringArray[1].substring(n + 1));
        if (n2 < 1 || n3 < 1 || n2 > 4096 || n3 > 4096) {
            throw new IllegalArgumentException(string3 + ": fixed texels are 1 to 4096 a side");
        }
        return new PackTarget(string, format, 0.0f, n2, n3, bl);
    }

    private static float number(String string, String string2) {
        try {
            return Float.parseFloat(string2);
        }
        catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException(string + ": " + string2 + " is not a size (0.5, 1, 256x256)", numberFormatException);
        }
    }

    public int width(int n) {
        return this.scale > 0.0f ? Math.max(1, Math.round((float)n * this.scale)) : this.width;
    }

    public int height(int n) {
        return this.scale > 0.0f ? Math.max(1, Math.round((float)n * this.scale)) : this.height;
    }

    public String toString() {
        return this.name;
    }

    public static enum Format {
        RGBA8("rgba8", 32856, 6408, 5121),
        RGBA16F("rgba16f", 34842, 6408, 5126),
        RG16F("rg16f", 33327, 33319, 5126),
        R16F("r16f", 33325, 6403, 5126),
        R32F("r32f", 33326, 6403, 5126);

        final String key;
        public final int internal;
        public final int format;
        public final int type;

        private Format(String string2, int n2, int n3, int n4) {
            this.key = string2;
            this.internal = n2;
            this.format = n3;
            this.type = n4;
        }
    }
}

