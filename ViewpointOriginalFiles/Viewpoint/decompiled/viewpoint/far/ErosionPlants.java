/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import viewpoint.far.Erosion;
import viewpoint.far.FarTiles;
import viewpoint.far.FarTrees;
import viewpoint.far.TreeSeasons;
import viewpoint.far.WorldGen;

final class ErosionPlants {
    static final String BUSHES = "f_bushes_1_";
    static final String PLANTS = "d_plants_1_";
    static final String GRASS = "e_newgrass_1_";
    static final String GENERIC = "d_generic_1_";
    private static final String FOLIAGE = "vegetation_foliage";
    private static final String GROUNDCOVER = "vegetation_groundcover_01_";
    private static final String OVERLAYS = "blends_grassoverlays_01_";
    private static final String NATURAL = "blends_natural_01_";
    private static final int CHANCE_DRAW = 101;
    private static final int SPAWN = 100;
    private static final int CYCLE = 60;
    private static final float BUSH_STAGE = 60.0f;
    private static final int BUSH_KINDS = 16;
    private static final int STEMS = 8;
    private static final int GROWN_BUSH = 1;
    private static final int SPRING = 32;
    private static final int AUTUMN = 48;
    private static final int SUMMER = 64;
    private static final int SUMMER_STAGE = 32;
    private static final int PLANT_KINDS = 24;
    private static final int GROUNDCOVER_PLANT = 21;
    private static final int GROUNDCOVER_FIRST = 16;
    private static final int GROUNDCOVER_DRAWN = 18;
    private static final int GROUNDCOVER_LAST = 23;
    private static final int SPRING_PLANTS = 0;
    private static final int AUTUMN_PLANTS = 8;
    private static final int PLANTS_A_SEASON = 8;
    private static final int GRASS_KINDS = 6;
    private static final int GRASS_STAGES = 3;
    private static final int GRASS_ROW = 8;
    private static final int GRASS_SEASON = 24;
    private static final int GRASS_DISPLAYS = 5;
    private static final int LITTER = 16;
    private static final int FERNS = 48;
    private static final int FERN_STAGE = 32;
    private static final int FERN_KINDS = 8;
    private static final int[][] OVERLAY_GRASS = new int[][]{{0, 21, 24}, {22, 45, 48}, {48, 69, 72}};
    private static final int BARE = 0;
    private static final int SPRING_DISPLAY = 1;
    private static final int SUMMER_DISPLAY = 2;
    private static final int LATE_SUMMER = 3;
    private static final int AUTUMN_DISPLAY = 4;
    private final int[][] bushSoil;
    private final int[][] plantSoil;
    private final int[] bushChance;
    private final int[] plantChance;
    private final Set<String> twigs;
    private final Map<String, FarTiles.Tile> tiles;

    ErosionPlants(int[][] nArray, int[] nArray2, int[][] nArray3, int[] nArray4, Set<String> set) {
        this(nArray, nArray2, nArray3, nArray4, set, Map.of());
    }

    private ErosionPlants(int[][] nArray, int[] nArray2, int[][] nArray3, int[] nArray4, Set<String> set, Map<String, FarTiles.Tile> map) {
        this.bushSoil = nArray;
        this.bushChance = nArray2;
        this.plantSoil = nArray3;
        this.plantChance = nArray4;
        this.twigs = set;
        this.tiles = map;
    }

    ErosionPlants withTiles() {
        HashMap<String, FarTiles.Tile> hashMap = new HashMap<String, FarTiles.Tile>();
        for (String string : ErosionPlants.sprites()) {
            FarTiles.Tile tile = string.startsWith(BUSHES) && ErosionPlants.frame(string, BUSHES) >= 32 ? TreeSeasons.leafTile(string, true) : FarTiles.of(string);
            if (tile == null) continue;
            hashMap.put(string, tile);
        }
        return new ErosionPlants(this.bushSoil, this.bushChance, this.plantSoil, this.plantChance, this.twigs, hashMap);
    }

    static List<String> sprites() {
        int n;
        ArrayList<String> arrayList = new ArrayList<String>();
        for (n = 0; n < 128; ++n) {
            arrayList.add(BUSHES + n);
        }
        for (n = 0; n < 64; ++n) {
            arrayList.add(PLANTS + n);
        }
        for (n = 0; n < 120; ++n) {
            arrayList.add(GRASS + n);
        }
        for (n = 0; n < 112; ++n) {
            arrayList.add(GENERIC + n);
        }
        return arrayList;
    }

    void erode(Random random, List<WorldGen.Thing> list, int n, int n2, int n3, int n4, byte[][] byArray, int n5, int n6) {
        byte by = byArray[1][n3];
        if (this.replaceBushes(random, list, n, n2, by) || this.spawnBush(random, list, n, n2, n4, by) || this.replacePlants(random, list, by, n5, n6) || this.spawnPlant(random, list, n, n2, n4, by, n5, n6)) {
            return;
        }
        this.replaceGeneric(list, n4, byArray[2][n3]);
    }

    private boolean replaceBushes(Random random, List<WorldGen.Thing> list, int n, int n2, int n3) {
        int n4 = -1;
        int n5 = 0;
        for (int i = list.size() - 1; i >= 1; --i) {
            String string = list.get(i).name();
            if (string.startsWith(FOLIAGE)) {
                int n6 = n >= 0 && n < this.bushSoil.length ? n : ErosionPlants.next(random, this.bushSoil.length);
                int[] nArray = this.bushSoil[n6];
                n4 = nArray[ErosionPlants.next(random, nArray.length)] - 1;
                n5 = (int)Math.floor((float)n2 / 60.0f);
                list.remove(i);
                continue;
            }
            if (!string.startsWith(BUSHES)) continue;
            n4 = ErosionPlants.frame(string, BUSHES) % 16;
            n5 = 1;
            list.remove(i);
        }
        if (n4 < 0) {
            return false;
        }
        this.bush(list, n4, n5, n3);
        return true;
    }

    private boolean spawnBush(Random random, List<WorldGen.Thing> list, int n, int n2, int n3, int n4) {
        if (list.size() > 1 || n < 0 || n >= this.bushSoil.length) {
            return false;
        }
        int[] nArray = this.bushSoil[n];
        if (ErosionPlants.next(random, 101) >= this.bushChance[n2]) {
            return false;
        }
        int n5 = nArray[ErosionPlants.next(random, nArray.length)] - 1;
        int n6 = (int)Math.floor((float)n2 / 60.0f);
        int n7 = 100 - n2;
        if (n3 >= n7) {
            int n8 = (int)Math.floor((float)(n3 - n7) / (60.0f / ((float)n6 + 1.0f)));
            this.bush(list, n5, Math.min(Math.max(n8, 0), n6), n4);
        }
        return true;
    }

    private void bush(List<WorldGen.Thing> list, int n, int n2, int n3) {
        int n4;
        if (n < 0 || n >= 16) {
            return;
        }
        int n5 = n % 8;
        this.put(list, BUSHES + (n5 + 8 * n2));
        switch (n3) {
            case 1: {
                int n6 = n5 + 32 + 8 * n2;
                break;
            }
            case 2: 
            case 3: {
                int n6 = 64 + n + 32 * n2;
                break;
            }
            case 4: {
                int n6 = n5 + 48 + 8 * n2;
                break;
            }
            default: {
                int n6 = n4 = -1;
            }
        }
        if (n4 >= 0) {
            this.put(list, BUSHES + n4);
        }
    }

    private boolean replacePlants(Random random, List<WorldGen.Thing> list, int n, int n2, int n3) {
        for (int i = list.size() - 1; i >= 1; --i) {
            int n4;
            String string = list.get(i).name();
            if (string.startsWith(PLANTS)) {
                var9_8 = ErosionPlants.frame(string, PLANTS);
                n4 = var9_8 < 32 ? var9_8 % 8 : (var9_8 < 48 ? var9_8 % 8 + 8 : var9_8 % 8 + 16);
                list.remove(i);
            } else {
                int n5 = var9_8 = string.startsWith(GROUNDCOVER) ? ErosionPlants.frame(string, GROUNDCOVER) : -1;
                if (var9_8 < 16 || var9_8 > 23) continue;
                n4 = var9_8 < 18 ? 21 : ErosionPlants.next(random, 24);
                list.remove(i);
                while (--i > 0) {
                    if (!list.get(i).name().startsWith(GROUNDCOVER)) continue;
                    list.remove(i);
                }
            }
            this.plant(list, n4, n, n2, n3);
            return true;
        }
        return false;
    }

    private boolean spawnPlant(Random random, List<WorldGen.Thing> list, int n, int n2, int n3, int n4, int n5, int n6) {
        if (list.size() > 1 || n < 0 || n >= this.plantSoil.length) {
            return false;
        }
        int[] nArray = this.plantSoil[n];
        if (ErosionPlants.next(random, 101) >= this.plantChance[n2]) {
            return false;
        }
        int n7 = nArray[ErosionPlants.next(random, nArray.length)] - 1;
        if (n3 >= 100 - n2) {
            this.plant(list, n7, n4, n5, n6);
        }
        return true;
    }

    private void plant(List<WorldGen.Thing> list, int n, int n2, int n3, int n4) {
        int n5;
        if (n < 0 || n >= 24) {
            return;
        }
        int n6 = FarTrees.hash(n3, n4) % 8;
        switch (n2) {
            case 1: {
                int n7 = 0 + n6;
                break;
            }
            case 4: {
                int n7 = 8 + n6;
                break;
            }
            case 2: {
                int n7 = (n < 8 ? 16 : (n < 16 ? 24 : 32)) + n;
                break;
            }
            default: {
                int n7 = n5 = -1;
            }
        }
        if (n5 >= 0) {
            this.put(list, PLANTS + n5);
        }
    }

    private void replaceGeneric(List<WorldGen.Thing> list, int n, int n2) {
        for (int i = list.size() - 1; i >= 1; --i) {
            int n3;
            int n4;
            String string = ErosionPlants.asGrass(list.get(i).name());
            if (this.twigs.contains(string)) continue;
            if (string.startsWith(GRASS)) {
                n4 = ErosionPlants.frame(string, GRASS);
                n3 = n4 % 8;
                if (n3 >= 6 || n4 >= 120) continue;
                int n5 = 2 - n4 % 24 / 8;
                int n6 = 2;
                int n7 = n5 == n6 ? n6 : Math.min(n / 20, n6);
                int n8 = Math.max(n2, ErosionPlants.dried(list.get(0).name()));
                list.remove(i);
                if (n8 > 0) {
                    this.put(list, i, GRASS + ((n8 - 1) * 24 + (n6 - n7) * 8 + n3));
                }
                return;
            }
            if (!string.startsWith(GENERIC)) continue;
            n4 = ErosionPlants.frame(string, GENERIC);
            int n9 = n3 = n4 >= 48 && n4 < 112 && (n4 - 48) % 32 < 8 ? 1 : 0;
            if (n3 == 0 && n4 >= 32) continue;
            int n10 = n3 != 0 ? (n4 - 48) / 32 : n4 / 16;
            int n11 = n3 != 0 ? (n4 - 48) % 32 : n4 % 16;
            int n12 = n10 == 1 ? 1 : Math.min(n / 30, 1);
            list.set(i, this.thing(GENERIC + (n3 != 0 ? 48 + 32 * n12 + n11 : 16 * n12 + n11)));
            return;
        }
    }

    private static String asGrass(String string) {
        if (!string.startsWith(OVERLAYS)) {
            return string;
        }
        int n = ErosionPlants.frame(string, OVERLAYS);
        for (int[] nArray : OVERLAY_GRASS) {
            if (n < nArray[0] || n > nArray[1]) continue;
            return GRASS + (nArray[2] + n - nArray[0]);
        }
        return string;
    }

    private static int dried(String string) {
        boolean bl;
        if (!string.startsWith(NATURAL)) {
            return 0;
        }
        int n = ErosionPlants.frame(string, NATURAL);
        int n2 = n / 8;
        int n3 = n % 8;
        boolean bl2 = bl = n3 == 0 || n3 >= 5;
        return bl && n2 == 4 ? 3 : (bl && n2 == 6 ? 4 : 0);
    }

    private void put(List<WorldGen.Thing> list, String string) {
        list.add(this.thing(string));
    }

    private void put(List<WorldGen.Thing> list, int n, String string) {
        list.add(Math.min(n, list.size()), this.thing(string));
    }

    private WorldGen.Thing thing(String string) {
        return new WorldGen.Thing(string, 0, this.tiles.get(string));
    }

    private static int frame(String string, String string2) {
        try {
            return Integer.parseInt(string.substring(string2.length()));
        }
        catch (IndexOutOfBoundsException | NumberFormatException runtimeException) {
            return -1;
        }
    }

    void dump(PrintWriter printWriter) {
        for (int[] nArray : this.bushSoil) {
            printWriter.println("erosion bushsoil " + Erosion.join(nArray));
        }
        printWriter.println("erosion bushchance " + Erosion.join(this.bushChance));
        for (int[] nArray : this.plantSoil) {
            printWriter.println("erosion plantsoil " + Erosion.join(nArray));
        }
        printWriter.println("erosion plantchance " + Erosion.join(this.plantChance));
        printWriter.println("erosion twigs " + String.join((CharSequence)" ", this.twigs));
    }

    private static int next(Random random, int n) {
        return n > 0 ? random.nextInt(n) : 0;
    }
}

