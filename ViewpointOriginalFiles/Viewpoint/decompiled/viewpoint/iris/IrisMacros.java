/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import viewpoint.iris.Biomes;

public final class IrisMacros {
    public static final String MC_VERSION = "12104";
    public static final String IRIS_VERSION = "10812";
    public static final String VIEWPOINT_VERSION = "1";
    public static final String[] RENDER_STAGES = new String[]{"NONE", "SKY", "SUNSET", "CUSTOM_SKY", "SUN", "MOON", "STARS", "VOID", "TERRAIN_SOLID", "TERRAIN_CUTOUT_MIPPED", "TERRAIN_CUTOUT", "ENTITIES", "BLOCK_ENTITIES", "DESTROY", "OUTLINE", "DEBUG", "HAND_SOLID", "TERRAIN_TRANSLUCENT", "TRIPWIRE", "PARTICLES", "CLOUDS", "RAIN_SNOW", "WORLD_BORDER", "HAND_TRANSLUCENT"};
    public static final String[] DH_BLOCKS = new String[]{"UNKNOWN", "LEAVES", "STONE", "WOOD", "METAL", "DIRT", "LAVA", "DEEPSLATE", "SNOW", "SAND", "TERRACOTTA", "NETHER_STONE", "WATER", "GRASS", "AIR", "ILLUMINATED"};

    private IrisMacros() {
    }

    public static Map<String, String> standard(int n, String string, String string2, Set<String> set, boolean bl) {
        int n2;
        String string3;
        String string4;
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        linkedHashMap.put("MC_VERSION", MC_VERSION);
        linkedHashMap.put("MC_GL_VERSION", Integer.toString(n));
        linkedHashMap.put("MC_GLSL_VERSION", Integer.toString(Math.min(n, 460)));
        linkedHashMap.put("IS_IRIS", "");
        linkedHashMap.put("IRIS_VERSION", IRIS_VERSION);
        linkedHashMap.put("VIEWPOINT", "");
        linkedHashMap.put("VIEWPOINT_VERSION", VIEWPOINT_VERSION);
        linkedHashMap.put("MC_RENDER_QUALITY", "1.0");
        linkedHashMap.put("MC_SHADOW_QUALITY", "1.0");
        linkedHashMap.put("MC_HAND_DEPTH", "0.125");
        linkedHashMap.put("MC_NORMAL_MAP", "");
        linkedHashMap.put("MC_SPECULAR_MAP", "");
        linkedHashMap.put("MC_OS_" + IrisMacros.os(), "");
        String string5 = string4 = string == null ? "" : string.toLowerCase(Locale.ROOT);
        linkedHashMap.put(string4.contains("nvidia") ? "MC_GL_VENDOR_NVIDIA" : (string4.contains("ati") || string4.contains("amd") ? "MC_GL_VENDOR_ATI" : (string4.contains("intel") ? "MC_GL_VENDOR_INTEL" : "MC_GL_VENDOR_OTHER")), "");
        if (string4.contains("amd")) {
            linkedHashMap.put("MC_GL_VENDOR_AMD", "");
        }
        String string6 = string3 = string2 == null ? "" : string2.toLowerCase(Locale.ROOT);
        linkedHashMap.put(string3.contains("geforce") ? "MC_GL_RENDERER_GEFORCE" : (string3.contains("radeon") ? "MC_GL_RENDERER_RADEON" : (string3.contains("intel") ? "MC_GL_RENDERER_INTEL" : "MC_GL_RENDERER_OTHER")), "");
        for (String string7 : set) {
            linkedHashMap.put("IRIS_FEATURE_" + string7, "");
        }
        for (n2 = 0; n2 < RENDER_STAGES.length; ++n2) {
            linkedHashMap.put("MC_RENDER_STAGE_" + RENDER_STAGES[n2], Integer.toString(n2));
        }
        for (n2 = 0; n2 < DH_BLOCKS.length; ++n2) {
            linkedHashMap.put("DH_BLOCK_" + DH_BLOCKS[n2], Integer.toString(n2));
        }
        if (bl) {
            linkedHashMap.put("DISTANT_HORIZONS", "");
        }
        linkedHashMap.putAll(Biomes.macros());
        return linkedHashMap;
    }

    public static int renderStage(String string) {
        for (int i = 0; i < RENDER_STAGES.length; ++i) {
            if (!RENDER_STAGES[i].equals(string)) continue;
            return i;
        }
        throw new IllegalArgumentException("no render stage " + string);
    }

    private static String os() {
        String string = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return string.contains("win") ? "WINDOWS" : (string.contains("mac") ? "MAC" : (string.contains("bsd") ? "BSD" : "LINUX"));
    }
}

