/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.properties.IsoPropertyType
 *  zombie.erosion.ErosionMain
 *  zombie.erosion.ErosionRegions
 *  zombie.erosion.categories.ErosionCategory
 *  zombie.erosion.season.ErosionSeason
 *  zombie.erosion.utils.Noise2D
 *  zombie.iso.sprite.IsoSprite
 *  zombie.iso.sprite.IsoSpriteManager
 */
package viewpoint.far;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import viewpoint.far.ErosionPlants;
import viewpoint.far.FarTiles;
import viewpoint.far.WorldGen;
import zombie.core.properties.IsoPropertyType;
import zombie.erosion.ErosionMain;
import zombie.erosion.ErosionRegions;
import zombie.erosion.categories.ErosionCategory;
import zombie.erosion.season.ErosionSeason;
import zombie.erosion.utils.Noise2D;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;

final class Erosion {
    static final String NATURAL = "blends_natural_01";
    static final int STAGES = 8;
    static final int NONE = -1;
    private static final int FREE = -2;
    private static final int CYCLE = 60;
    private static final int SPAWN = 130;
    private static final int PLANT_NOISE = 50;
    private static final int PLANT_STEP = 17;
    private static final float SIZE_STEP = 51.0f;
    private static final int GROWN = 3;
    private static final int JUMBO = 5;
    private static final int FIRST_JUMBO = 4;
    private static final int XL = 6;
    private static final int XXL = 7;
    private static final int XL_SHEET = 2;
    private static final int CHANCE_DRAW = 101;
    private static final int CHUNK = 8;
    private static final float SOIL_SCALE = 5.0f;
    private static final float MAIN_SCALE = 10.0f;
    private static final float TENTHS = 10.0f;
    private static final float HUNDREDTHS = 100.0f;
    private static final int LAST_TENTH = 9;
    private static final String PLACEHOLDER = "vegetation_trees";
    private static final String JUMBO_PLACEHOLDER = "jumbo_tree_01";
    private final Noise main;
    private final Noise moisture;
    private final Noise minerals;
    private final int[][] soilTable;
    private final int[][] soilRef;
    private final int[] spawnChance;
    private final String[][] sheets;
    final int ticks;
    private final FarTiles.Tile[][] tiles;
    private final ErosionPlants plants;
    private static Erosion current;
    private static boolean unreadable;

    Erosion(Noise noise, Noise noise2, Noise noise3, int[][] nArray, int[][] nArray2, int[] nArray3, String[] stringArray, int n) {
        this.main = noise;
        this.moisture = noise2;
        this.minerals = noise3;
        this.soilTable = nArray;
        this.soilRef = nArray2;
        this.spawnChance = nArray3;
        this.ticks = n;
        this.sheets = new String[stringArray.length][];
        for (int i = 0; i < stringArray.length; ++i) {
            String string = stringArray[i];
            this.sheets[i] = new String[]{string, string.replace("_1", "JUMBO_1"), string.replace("_1", "JUMBOXL_1"), string.replace("_1", "JUMBOXXL_1")};
        }
        this.tiles = null;
        this.plants = null;
    }

    private Erosion(Erosion erosion, int n, FarTiles.Tile[][] tileArray, ErosionPlants erosionPlants) {
        this.main = erosion.main;
        this.moisture = erosion.moisture;
        this.minerals = erosion.minerals;
        this.soilTable = erosion.soilTable;
        this.soilRef = erosion.soilRef;
        this.spawnChance = erosion.spawnChance;
        this.sheets = erosion.sheets;
        this.ticks = n;
        this.tiles = tileArray;
        this.plants = erosionPlants;
    }

    Erosion withPlants(ErosionPlants erosionPlants) {
        return new Erosion(this, this.ticks, this.tiles, erosionPlants);
    }

    int tree(WorldGen.Source source, List<WorldGen.Thing> list, int n, int n2) {
        return this.square(source, list, n, n2, null);
    }

    int square(WorldGen.Source source, List<WorldGen.Thing> list, int n, int n2, byte[][] byArray) {
        int n3;
        int n4;
        if (list.isEmpty() || (list.get(0).kind() & 1) == 0 || !list.get(0).name().startsWith(NATURAL)) {
            return -1;
        }
        Random random = WorldGen.generator(source.seed(n, n2));
        int n5 = random.nextInt(100);
        for (n4 = list.size() - 1; n4 >= 1; --n4) {
            String string = list.get(n4).name();
            if (string.startsWith(JUMBO_PLACEHOLDER)) {
                return this.placeholder(random, n, n2, 5);
            }
            if (string.startsWith(PLACEHOLDER)) {
                return this.placeholder(random, n, n2, 3);
            }
            n3 = this.own(string);
            if (n3 == -1) continue;
            return n3;
        }
        n4 = this.soil(n, n2);
        int n6 = this.noise(n, n2);
        n3 = this.planted(random, list.size(), n4, n6);
        if (n3 == -2 && this.plants != null && byArray != null) {
            this.plants.erode(random, list, n4, n6, n5, this.ticks, byArray, n, n2);
        }
        return n3 == -2 ? -1 : n3;
    }

    FarTiles.Tile tile(int n) {
        return this.tiles == null ? null : this.tiles[n / 8][n % 8];
    }

    String name(int n) {
        String[] stringArray = this.sheets[n / 8];
        int n2 = n % 8;
        if (n2 >= 6) {
            return stringArray[2 + n2 - 6] + "_0";
        }
        return n2 < 4 ? stringArray[0] + "_" + n2 : stringArray[1] + "_" + (n2 - 4);
    }

    private int placeholder(Random random, int n, int n2, int n3) {
        int n4 = this.soil(n, n2);
        if (n4 < 0 || n4 >= this.soilRef.length) {
            n4 = Erosion.next(random, this.soilRef.length);
        }
        int[] nArray = this.soilRef[n4];
        int n5 = nArray[Erosion.next(random, nArray.length)] - 1;
        return this.code(n5, n3 + (int)Math.floor((float)this.noise(n, n2) / 51.0f) - 1);
    }

    private int planted(Random random, int n, int n2, int n3) {
        int n4;
        if (n > 1 || n2 < 0 || n2 >= this.soilRef.length) {
            return -2;
        }
        int n5 = n4 = n3 < this.spawnChance.length ? this.spawnChance[n3] : 0;
        if (n4 <= 0 || Erosion.next(random, 101) >= n4) {
            return -2;
        }
        int[] nArray = this.soilRef[n2];
        int n6 = nArray[Erosion.next(random, nArray.length)] - 1;
        int n7 = 1 + (n3 - 50) / 17;
        int n8 = 130 - n3;
        if (this.ticks < n8) {
            return -1;
        }
        int n9 = (int)Math.floor((float)(this.ticks - n8) / (60.0f / ((float)n7 + 1.0f)));
        return this.code(n6, Math.min(Math.max(n9, 0), n7));
    }

    private int code(int n, int n2) {
        return n < 0 || n >= this.sheets.length || n2 < 0 || n2 >= 8 ? -1 : n * 8 + n2;
    }

    private int own(String string) {
        for (int i = 0; i < this.sheets.length; ++i) {
            String[] stringArray = this.sheets[i];
            if (string.startsWith(stringArray[0])) {
                return this.code(i, 3);
            }
            if (string.startsWith(stringArray[1])) {
                return this.code(i, Erosion.frame(string) % 2 == 0 ? 4 : 5);
            }
            if (!string.startsWith(stringArray[2]) && !string.startsWith(stringArray[3])) continue;
            return this.code(i, string.startsWith(stringArray[2]) ? 6 : 7);
        }
        return -1;
    }

    private static int frame(String string) {
        try {
            return Integer.parseInt(string.substring(string.lastIndexOf(95) + 1));
        }
        catch (NumberFormatException numberFormatException) {
            return 0;
        }
    }

    private int soil(int n, int n2) {
        float f = (float)Math.floorDiv(n, 8) / 5.0f;
        float f2 = (float)Math.floorDiv(n2, 8) / 5.0f;
        float f3 = this.moisture.at(f, f2);
        float f4 = this.minerals.at(f, f2);
        int n3 = f3 < 1.0f ? (int)Math.floor(f3 * 10.0f) : 9;
        int n4 = f4 < 1.0f ? (int)Math.floor(f4 * 10.0f) : 9;
        return this.soilTable[n3][n4] - 1;
    }

    private int noise(int n, int n2) {
        return (int)Math.floor(this.main.at((float)n / 10.0f, (float)n2 / 10.0f) * 100.0f);
    }

    void dump(PrintWriter printWriter) {
        ErosionSeason erosionSeason;
        printWriter.println("erosion ticks " + this.ticks);
        printWriter.println("erosion perm " + Erosion.join(this.main.perm));
        String[] stringArray = new String[]{"main", "moisture", "minerals"};
        Noise[] noiseArray = new Noise[]{this.main, this.moisture, this.minerals};
        for (int i = 0; i < noiseArray.length; ++i) {
            for (int j = 0; j < noiseArray[i].freq.length; ++j) {
                printWriter.println("erosion layer " + stringArray[i] + " " + noiseArray[i].freq[j] + " " + noiseArray[i].amp[j] + " " + Erosion.join(noiseArray[i].p[j]));
            }
        }
        for (int[] nArray : this.soilTable) {
            printWriter.println("erosion soiltable " + Erosion.join(nArray));
        }
        for (int[] nArray : this.soilRef) {
            printWriter.println("erosion soilref " + Erosion.join(nArray));
        }
        printWriter.println("erosion chance " + Erosion.join(this.spawnChance));
        Object object = new StringBuilder("erosion trees");
        ErosionMain erosionMain = this.sheets;
        int n = ((String[][])erosionMain).length;
        for (int i = 0; i < n; ++i) {
            String[] stringArray2 = erosionMain[i];
            ((StringBuilder)object).append(' ').append(stringArray2[0]);
        }
        printWriter.println(object);
        if (this.plants != null) {
            this.plants.dump(printWriter);
        }
        ErosionSeason erosionSeason2 = erosionSeason = (erosionMain = ErosionMain.getInstance()) == null ? null : erosionMain.getSeasons();
        if (erosionSeason != null) {
            printWriter.println("erosion season " + erosionSeason.getSeason() + " " + erosionSeason.getSeasonDay() + " " + erosionSeason.getSeasonDays());
        }
    }

    static String join(int[] nArray) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int n : nArray) {
            stringBuilder.append(stringBuilder.isEmpty() ? "" : " ").append(n);
        }
        return stringBuilder.toString();
    }

    private static int next(Random random, int n) {
        return n > 0 ? random.nextInt(n) : 0;
    }

    static Erosion current() {
        ErosionMain erosionMain = ErosionMain.getInstance();
        if (current != null || unreadable || erosionMain == null) {
            return current;
        }
        try {
            Erosion erosion = Erosion.read(erosionMain);
            if (erosion == null) {
                return null;
            }
            FarTiles.Tile[][] tileArray = new FarTiles.Tile[erosion.sheets.length][8];
            for (int i = 0; i < tileArray.length; ++i) {
                for (int j = 0; j < 8; ++j) {
                    tileArray[i][j] = FarTiles.of(erosion.name(i * 8 + j));
                }
            }
            current = new Erosion(erosion, erosion.ticks, tileArray, erosion.plants.withTiles());
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            unreadable = true;
            System.out.println("[Viewpoint] far world: the erosion's trees left out, its numbers unreadable: " + String.valueOf(exception));
        }
        return current;
    }

    static boolean update() {
        Erosion erosion = Erosion.current();
        ErosionMain erosionMain = ErosionMain.getInstance();
        if (erosion == null || erosionMain == null || erosionMain.getEtick() == erosion.ticks) {
            return false;
        }
        current = new Erosion(erosion, erosionMain.getEtick(), erosion.tiles, erosion.plants);
        return Math.min(erosion.ticks, Erosion.current.ticks) <= 140;
    }

    static void forget() {
        current = null;
        unreadable = false;
    }

    static Erosion read(ErosionMain erosionMain) throws ReflectiveOperationException {
        Noise2D noise2D = (Noise2D)Erosion.field(ErosionMain.class, "noiseMain").get(erosionMain);
        if (noise2D == null) {
            return null;
        }
        ErosionCategory erosionCategory = ErosionRegions.getCategory((int)0, (int)0);
        Object[] objectArray = (Object[])Erosion.field(erosionCategory.getClass(), "trees").get(erosionCategory);
        String[] stringArray = new String[objectArray.length];
        for (int i = 0; i < objectArray.length; ++i) {
            stringArray[i] = (String)Erosion.field(objectArray[i].getClass(), "tilesetName").get(objectArray[i]);
        }
        ErosionCategory erosionCategory2 = ErosionRegions.getCategory((int)0, (int)1);
        ErosionCategory erosionCategory3 = ErosionRegions.getCategory((int)0, (int)2);
        return new Erosion(Erosion.noise(noise2D), Erosion.noise((Noise2D)Erosion.field(ErosionMain.class, "noiseMoisture").get(erosionMain)), Erosion.noise((Noise2D)Erosion.field(ErosionMain.class, "noiseMinerals").get(erosionMain)), (int[][])Erosion.field(ErosionMain.class, "soilTable").get(null), (int[][])Erosion.field(erosionCategory.getClass(), "soilRef").get(erosionCategory), (int[])Erosion.field(erosionCategory.getClass(), "spawnChance").get(erosionCategory), stringArray, erosionMain.getEtick()).withPlants(new ErosionPlants((int[][])Erosion.field(erosionCategory2.getClass(), "soilRef").get(erosionCategory2), (int[])Erosion.field(erosionCategory2.getClass(), "spawnChance").get(erosionCategory2), (int[][])Erosion.field(erosionCategory3.getClass(), "soilRef").get(erosionCategory3), (int[])Erosion.field(erosionCategory3.getClass(), "spawnChance").get(erosionCategory3), Erosion.twigs()));
    }

    private static Set<String> twigs() {
        HashSet<String> hashSet = new HashSet<String>();
        for (String string : ErosionPlants.sprites()) {
            IsoSprite isoSprite = (IsoSprite)IsoSpriteManager.instance.getNamedMap().get(string);
            String string2 = isoSprite == null ? null : isoSprite.getProperties().get(IsoPropertyType.CUSTOM_NAME);
            if (string2 == null || !string2.contains("Twig") && !string2.contains("Branch")) continue;
            hashSet.add(string);
        }
        return hashSet;
    }

    static Noise noise(Noise2D noise2D) throws ReflectiveOperationException {
        int[] nArray = (int[])Erosion.field(Noise2D.class, "perm").get(null);
        ArrayList arrayList = (ArrayList)Erosion.field(Noise2D.class, "layers").get(noise2D);
        float[] fArray = new float[arrayList.size()];
        float[] fArray2 = new float[arrayList.size()];
        int[][] nArrayArray = new int[arrayList.size()][];
        for (int i = 0; i < nArrayArray.length; ++i) {
            Object e = arrayList.get(i);
            fArray[i] = Erosion.field(e.getClass(), "freq").getFloat(e);
            fArray2[i] = Erosion.field(e.getClass(), "amp").getFloat(e);
            nArrayArray[i] = (int[])((int[])Erosion.field(e.getClass(), "p").get(e)).clone();
        }
        return new Noise(fArray, fArray2, nArrayArray, (int[])nArray.clone());
    }

    private static Field field(Class<?> clazz, String string) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(string);
        field.setAccessible(true);
        return field;
    }

    record Noise(float[] freq, float[] amp, int[][] p, int[] perm) {
        private static final float WRAP = 255.0f;

        float at(float f, float f2) {
            float f3 = 0.0f;
            float f4 = 0.0f;
            for (int i = 0; i < this.freq.length; ++i) {
                f4 += this.amp[i];
                f3 += this.noise(f * this.freq[i], f2 * this.freq[i], this.p[i]) * this.amp[i];
            }
            return f3 / f4 / 255.0f;
        }

        private float noise(float f, float f2, int[] nArray) {
            int n = (int)Math.floor((double)f - Math.floor(f / 255.0f) * 255.0);
            int n2 = (int)Math.floor((double)f2 - Math.floor(f2 / 255.0f) * 255.0);
            float f3 = Noise.fade(f - (float)Math.floor(f));
            float f4 = Noise.fade(f2 - (float)Math.floor(f2));
            int n3 = nArray[n] + n2;
            int n4 = nArray[n + 1] + n2;
            return Noise.lerp(f4, Noise.lerp(f3, this.perm[nArray[n3]], this.perm[nArray[n4]]), Noise.lerp(f3, this.perm[nArray[n3 + 1]], this.perm[nArray[n4 + 1]]));
        }

        private static float fade(float f) {
            return f * f * f * (f * (f * 6.0f - 15.0f) + 10.0f);
        }

        private static float lerp(float f, float f2, float f3) {
            return f2 + f * (f3 - f2);
        }
    }
}

