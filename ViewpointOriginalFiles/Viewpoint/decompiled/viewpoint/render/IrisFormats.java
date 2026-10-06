/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class IrisFormats {
    private static final Map<String, Format> FORMATS;
    private static final Map<String, Integer> PIXEL_FORMATS;
    private static final Map<String, Integer> PIXEL_TYPES;

    private static void put(String string, int n, int n2, int n3, int n4) {
        FORMATS.put(string, new Format(n, n2, n3, n4, false));
    }

    private static void integer(String string, int n, int n2, int n3, int n4) {
        FORMATS.put(string, new Format(n, n2, n3, n4, true));
    }

    static Format format(String string) {
        return string == null ? null : FORMATS.get(string.strip().toUpperCase(Locale.ROOT));
    }

    static int pixelFormat(String string) {
        return PIXEL_FORMATS.getOrDefault(string.strip().toUpperCase(Locale.ROOT), -1);
    }

    static int pixelType(String string) {
        return PIXEL_TYPES.getOrDefault(string.strip().toUpperCase(Locale.ROOT), -1);
    }

    private IrisFormats() {
    }

    static {
        String[][] stringArrayArray;
        FORMATS = new HashMap<String, Format>();
        PIXEL_FORMATS = new HashMap<String, Integer>();
        PIXEL_TYPES = new HashMap<String, Integer>();
        int n = 5126;
        int n2 = 5121;
        IrisFormats.put("R8", 33321, 6403, n2, 1);
        IrisFormats.put("RG8", 33323, 33319, n2, 2);
        IrisFormats.put("RGB8", 32849, 6407, n2, 3);
        IrisFormats.put("RGBA8", 32856, 6408, n2, 4);
        IrisFormats.put("R8_SNORM", 36756, 6403, n, 1);
        IrisFormats.put("RG8_SNORM", 36757, 33319, n, 2);
        IrisFormats.put("RGB8_SNORM", 36758, 6407, n, 3);
        IrisFormats.put("RGBA8_SNORM", 36759, 6408, n, 4);
        IrisFormats.put("R16", 33322, 6403, n, 2);
        IrisFormats.put("RG16", 33324, 33319, n, 4);
        IrisFormats.put("RGB16", 32852, 6407, n, 6);
        IrisFormats.put("RGBA16", 32859, 6408, n, 8);
        IrisFormats.put("R16_SNORM", 36760, 6403, n, 2);
        IrisFormats.put("RG16_SNORM", 36761, 33319, n, 4);
        IrisFormats.put("RGB16_SNORM", 36762, 6407, n, 6);
        IrisFormats.put("RGBA16_SNORM", 36763, 6408, n, 8);
        IrisFormats.put("R16F", 33325, 6403, n, 2);
        IrisFormats.put("RG16F", 33327, 33319, n, 4);
        IrisFormats.put("RGB16F", 34843, 6407, n, 6);
        IrisFormats.put("RGBA16F", 34842, 6408, n, 8);
        IrisFormats.put("R32F", 33326, 6403, n, 4);
        IrisFormats.put("RG32F", 33328, 33319, n, 8);
        IrisFormats.put("RGB32F", 34837, 6407, n, 12);
        IrisFormats.put("RGBA32F", 34836, 6408, n, 16);
        IrisFormats.put("R11F_G11F_B10F", 35898, 6407, n, 4);
        IrisFormats.put("RGB9_E5", 35901, 6407, n, 4);
        IrisFormats.put("RGB10_A2", 32857, 6408, n, 4);
        IrisFormats.put("RGB5_A1", 32855, 6408, n, 2);
        IrisFormats.put("R3_G3_B2", 10768, 6407, n, 1);
        IrisFormats.integer("R8I", 33329, 36244, 5120, 1);
        IrisFormats.integer("RG8I", 33335, 33320, 5120, 2);
        IrisFormats.integer("RGBA8I", 36238, 36249, 5120, 4);
        IrisFormats.integer("R8UI", 33330, 36244, n2, 1);
        IrisFormats.integer("RG8UI", 33336, 33320, n2, 2);
        IrisFormats.integer("RGBA8UI", 36220, 36249, n2, 4);
        IrisFormats.integer("R16I", 33331, 36244, 5122, 2);
        IrisFormats.integer("RG16I", 33337, 33320, 5122, 4);
        IrisFormats.integer("RGBA16I", 36232, 36249, 5122, 8);
        IrisFormats.integer("R16UI", 33332, 36244, 5123, 2);
        IrisFormats.integer("RG16UI", 33338, 33320, 5123, 4);
        IrisFormats.integer("RGBA16UI", 36214, 36249, 5123, 8);
        IrisFormats.integer("R32I", 33333, 36244, 5124, 4);
        IrisFormats.integer("RG32I", 33339, 33320, 5124, 8);
        IrisFormats.integer("RGBA32I", 36226, 36249, 5124, 16);
        IrisFormats.integer("R32UI", 33334, 36244, 5125, 4);
        IrisFormats.integer("RG32UI", 33340, 33320, 5125, 8);
        IrisFormats.integer("RGBA32UI", 36208, 36249, 5125, 16);
        IrisFormats.integer("RGB10_A2UI", 36975, 36249, 5125, 4);
        String[][] stringArrayArray2 = stringArrayArray = new String[][]{{"RED", "6403"}, {"RG", "33319"}, {"RGB", "6407"}, {"BGR", "32992"}, {"RGBA", "6408"}, {"BGRA", "32993"}, {"RED_INTEGER", "36244"}, {"RG_INTEGER", "33320"}, {"RGB_INTEGER", "36248"}, {"BGR_INTEGER", "36250"}, {"RGBA_INTEGER", "36249"}, {"BGRA_INTEGER", "36251"}};
        int n3 = stringArrayArray2.length;
        for (int i = 0; i < n3; ++i) {
            String[] stringArray = stringArrayArray2[i];
            PIXEL_FORMATS.put(stringArray[0], Integer.parseInt(stringArray[1]));
        }
        for (String[] stringArray : stringArrayArray2 = new String[][]{{"BYTE", "5120"}, {"SHORT", "5122"}, {"INT", "5124"}, {"HALF_FLOAT", "5131"}, {"FLOAT", "5126"}, {"UNSIGNED_BYTE", "5121"}, {"UNSIGNED_SHORT", "5123"}, {"UNSIGNED_INT", "5125"}}) {
            PIXEL_TYPES.put(stringArray[0], Integer.parseInt(stringArray[1]));
        }
    }

    record Format(int internal, int pixels, int type, int bytes, boolean integer) {
    }
}

