/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Biomes {
    private static final List<String> BIOMES;
    private static final List<String> CATEGORIES;
    private static final Map<String, Double> CONSTANTS;
    public static final int PLAINS;
    public static final int CATEGORY_PLAINS;

    static Double constant(String string) {
        return CONSTANTS.get(string);
    }

    static Map<String, String> macros() {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        for (Map.Entry<String, Double> entry : CONSTANTS.entrySet()) {
            hashMap.put(entry.getKey(), Integer.toString(entry.getValue().intValue()));
        }
        return hashMap;
    }

    private Biomes() {
    }

    static {
        int n;
        BIOMES = List.of("THE_VOID", "PLAINS", "SUNFLOWER_PLAINS", "SNOWY_PLAINS", "ICE_SPIKES", "DESERT", "SWAMP", "MANGROVE_SWAMP", "FOREST", "FLOWER_FOREST", "BIRCH_FOREST", "DARK_FOREST", "OLD_GROWTH_BIRCH_FOREST", "OLD_GROWTH_PINE_TAIGA", "OLD_GROWTH_SPRUCE_TAIGA", "TAIGA", "SNOWY_TAIGA", "SAVANNA", "SAVANNA_PLATEAU", "WINDSWEPT_HILLS", "WINDSWEPT_GRAVELLY_HILLS", "WINDSWEPT_FOREST", "WINDSWEPT_SAVANNA", "JUNGLE", "SPARSE_JUNGLE", "BAMBOO_JUNGLE", "BADLANDS", "ERODED_BADLANDS", "WOODED_BADLANDS", "MEADOW", "CHERRY_GROVE", "GROVE", "SNOWY_SLOPES", "FROZEN_PEAKS", "JAGGED_PEAKS", "STONY_PEAKS", "RIVER", "FROZEN_RIVER", "BEACH", "SNOWY_BEACH", "STONY_SHORE", "WARM_OCEAN", "LUKEWARM_OCEAN", "DEEP_LUKEWARM_OCEAN", "OCEAN", "DEEP_OCEAN", "COLD_OCEAN", "DEEP_COLD_OCEAN", "FROZEN_OCEAN", "DEEP_FROZEN_OCEAN", "MUSHROOM_FIELDS", "DRIPSTONE_CAVES", "LUSH_CAVES", "DEEP_DARK", "NETHER_WASTES", "WARPED_FOREST", "CRIMSON_FOREST", "SOUL_SAND_VALLEY", "BASALT_DELTAS", "THE_END", "END_HIGHLANDS", "END_MIDLANDS", "SMALL_END_ISLANDS", "END_BARRENS", "PALE_GARDEN");
        CATEGORIES = List.of("NONE", "TAIGA", "EXTREME_HILLS", "JUNGLE", "MESA", "PLAINS", "SAVANNA", "ICY", "THE_END", "BEACH", "FOREST", "OCEAN", "DESERT", "RIVER", "SWAMP", "MUSHROOM", "NETHER", "MOUNTAIN", "UNDERGROUND");
        CONSTANTS = new HashMap<String, Double>();
        for (n = 0; n < BIOMES.size(); ++n) {
            CONSTANTS.put("BIOME_" + BIOMES.get(n), Double.valueOf(n));
        }
        for (n = 0; n < CATEGORIES.size(); ++n) {
            CONSTANTS.put("CAT_" + CATEGORIES.get(n), Double.valueOf(n));
        }
        CONSTANTS.put("PPT_NONE", 0.0);
        CONSTANTS.put("PPT_RAIN", 1.0);
        CONSTANTS.put("PPT_SNOW", 2.0);
        PLAINS = BIOMES.indexOf("PLAINS");
        CATEGORY_PLAINS = CATEGORIES.indexOf("PLAINS");
    }
}

