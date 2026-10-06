/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import viewpoint.far.FarTiles;

final class WorldGen {
    static final int GROUND = 0;
    static final int PLANT = 1;
    static final int BUSH = 2;
    static final int TREE = 3;
    static final int ORE = 4;
    static final int TYPES = 5;
    static final int FLOOR = 1;
    static final int IS_TREE = 2;
    static final int IS_BUSH = 4;
    static final int GRASS = 8;
    static final int GRASS_LIKE = 16;
    static final int IS_ORE = 32;
    static final int BLOCK = 16;
    private static final int CHUNK = 8;
    private static final String ANY = "$any";
    private static final String SUBBIOME = "$subbiome";
    private static final String NO_TREE = "$no_tree";
    private static final String NO_BUSH = "$no_bush";
    private static final String NO_GRASS = "$no_grass";
    private static final ThreadLocal<Random> GENERATOR = ThreadLocal.withInitial(Random::new);
    private final Source source;
    private final ArrayList<Thing>[] squares;
    private final String[][] pending = new String[5][256];
    private final int x0;
    private final int y0;
    Consumer<String> trace;
    private final Biome[] override = new Biome[5];
    private final Random squareRandom = new Random();
    private final Random subRandom = new Random();
    private static final int SUCCESS = 0;
    private static final int PENDING = 1;
    private static final int DELETE = 2;
    private static final int FAILURE = 3;
    private static final int FOUND = 0;
    private static final int LIST_EMPTY = 1;
    private static final int NO_FEATURE = 2;
    private static final int ELSEWHERE = 3;
    private final Thing[][] pendingThings = new Thing[5][256];

    static Random generator(long l) {
        Random random = GENERATOR.get();
        random.setSeed(l);
        return random;
    }

    WorldGen(Source source, ArrayList<Thing>[] arrayListArray, int n, int n2) {
        this.source = source;
        this.squares = arrayListArray;
        this.x0 = n;
        this.y0 = n2;
    }

    void run() {
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 2; ++j) {
                for (int k = 0; k < 8; ++k) {
                    for (int i2 = 0; i2 < 8; ++i2) {
                        this.square(i, j, k, i2);
                    }
                }
            }
        }
    }

    Thing tree(int n, int n2) {
        return WorldGen.first(this.squares[n + n2 * 16], 2);
    }

    private void square(int n, int n2, int n3, int n4) {
        Biome biome;
        int n5 = n * 8 + n3;
        int n6 = n2 * 8 + n4;
        int n7 = this.x0 + n5;
        int n8 = this.y0 + n6;
        ArrayList<Thing> arrayList = this.squares[n5 + n6 * 16];
        Thing thing = arrayList == null ? null : WorldGen.first(arrayList, 1);
        Biome biome2 = biome = thing == null ? null : this.source.biome(n7, n8);
        if (biome == null) {
            return;
        }
        int n9 = (this.x0 >> 3) + n;
        int n10 = (this.y0 >> 3) + n2;
        Random random = this.squareRandom;
        random.setSeed(this.source.seed(n9 * 8 + n7, n10 * 8 + n8));
        Arrays.fill(this.override, null);
        this.pending(biome, arrayList, n5, n6, n9, n10, n7, n8);
        Thing[] thingArray = new Thing[]{null, WorldGen.first(arrayList, 8), WorldGen.first(arrayList, 4), WorldGen.first(arrayList, 2), null};
        if (this.trace != null) {
            this.trace.accept("square " + n7 + "," + n8 + " " + biome.name + " " + String.valueOf(arrayList) + " override " + Arrays.toString(this.override));
        }
        for (int i = 1; i <= 3; ++i) {
            Thing thing2 = thingArray[i];
            if (thing2 == null || this.replace(biome, i, thing2, arrayList, thing, n5, n6, n3, n4, n9, n10, n7, n8, random)) continue;
            return;
        }
    }

    private boolean replace(Biome biome, int n, Thing thing, ArrayList<Thing> arrayList, Thing thing2, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, Random random) {
        block7: {
            Result result;
            block6: {
                Biome biome2;
                Biome biome3 = biome2 = this.override[n] != null ? this.override[n] : biome;
                if (biome2.protect != null && biome2.protect.matchesAny(thing.name())) {
                    return true;
                }
                Patterns patterns = biome2.placements[n];
                if (patterns == null || !patterns.places(thing2.name())) {
                    WorldGen.remove(arrayList, thing);
                    return true;
                }
                result = this.apply(biome2, n, arrayList, n2, n3, n4, n5, random);
                if (result.outcome == 1) {
                    this.pending(biome, arrayList, n2, n3, n6, n7, n8, n9);
                    biome2 = this.override[n] != null ? this.override[n] : biome;
                    result = this.apply(biome2, n, arrayList, n2, n3, n4, n5, random);
                }
                if (this.trace != null) {
                    this.trace.accept("  type " + n + " by " + biome2.name + ": " + String.valueOf(result));
                }
                if (result.outcome != 0) break block6;
                for (int i = 1; i < arrayList.size(); ++i) {
                    Thing thing3 = arrayList.get(i);
                    if (thing3.name().equals(result.placed) || (thing3.kind() & 0x16) == 0) continue;
                    arrayList.remove(i);
                }
                break block7;
            }
            if (result.outcome != 2) break block7;
            for (int i = 1; i < arrayList.size(); ++i) {
                if ((arrayList.get(i).kind() & 0x36) == 0) continue;
                arrayList.remove(i);
            }
        }
        return true;
    }

    private Result apply(Biome biome, int n, ArrayList<Thing> arrayList, int n2, int n3, int n4, int n5, Random random) {
        for (int i = 0; i < 16; ++i) {
            int n6 = 8 >> i;
            if (n6 == 0) {
                return new Result(3, null);
            }
            Lookup lookup = this.tile(biome, n, n2, n3, n4, n5, random, n6, 0);
            if (lookup.tile == null && (lookup.reason == 1 || lookup.reason == 2)) {
                return new Result(2, null);
            }
            if (lookup.tile == null) continue;
            String string = lookup.tile.name();
            if (string.equals(ANY) || string.equals(SUBBIOME) || string.equals(NO_BUSH) || string.equals(NO_GRASS) || string.equals(NO_TREE)) {
                return new Result(1, null);
            }
            arrayList.add(lookup.tile);
            random.nextFloat();
            return new Result(0, string);
        }
        return new Result(3, null);
    }

    private Lookup tile(Biome biome, int n, int n2, int n3, int n4, int n5, Random random, int n6, int n7) {
        int n8;
        int n9;
        Feature[] featureArray;
        int n10 = n2 + n3 * 16;
        String string = this.pending[n][n10];
        if (!(string == null || string.isEmpty() || string.equals(ANY) || string.equals(SUBBIOME))) {
            return new Lookup(0, this.pendingThing(n, n10));
        }
        Feature[] featureArray2 = featureArray = biome.features == null ? null : biome.features[n];
        if (featureArray == null) {
            return new Lookup(3, null);
        }
        if (featureArray.length == 0) {
            return new Lookup(1, null);
        }
        Feature feature = WorldGen.feature(featureArray, n6, random);
        if (feature == null) {
            return new Lookup(2, null);
        }
        ArrayList<Group> arrayList = new ArrayList<Group>();
        for (Group object : feature.groups()) {
            if (object.sx() > n6 || object.sy() > n6) continue;
            arrayList.add(object);
        }
        if (arrayList.isEmpty()) {
            return new Lookup(3, null);
        }
        Group group = (Group)arrayList.get(random.nextInt(arrayList.size()));
        if (n2 + group.sx() - 1 >= 16 || n3 + group.sy() - 1 >= 16 || biome.placements[n] != null && !this.fits(group, n4, n5, biome.placements[n])) {
            return new Lookup(3, null);
        }
        for (n9 = 0; n9 < group.sx(); ++n9) {
            for (n8 = 0; n8 < group.sy(); ++n8) {
                String string2 = this.pending[n][n2 + n9 + (n3 + n8) * 16];
                if (string2 == null || string2.startsWith("$")) continue;
                return new Lookup(3, null);
            }
        }
        for (n9 = 0; n9 < group.sx(); ++n9) {
            for (n8 = 0; n8 < group.sy(); ++n8) {
                this.pending[n][n2 + n9 + (n3 + n8) * 16] = group.tiles()[n9 + n8 * group.sx()].name();
                this.pendingThings[n][n2 + n9 + (n3 + n8) * 16] = group.tiles()[n9 + n8 * group.sx()];
            }
        }
        String string2 = this.pending[n][n10];
        if (ANY.equals(string2)) {
            this.pending[n][n10] = null;
            if (n7 < 1) {
                return this.tile(biome, n, n2, n3, n4, n5, random, n6, n7 + 1);
            }
        }
        return new Lookup(0, this.thing(n, n10, string2));
    }

    private Thing pendingThing(int n, int n2) {
        return this.thing(n, n2, this.pending[n][n2]);
    }

    private Thing thing(int n, int n2, String string) {
        Thing thing = this.pendingThings[n][n2];
        return thing != null && thing.name().equals(string) ? thing : new Thing(string, 0, null);
    }

    private static Feature feature(Feature[] featureArray, int n, Random random) {
        float f = 0.0f;
        float f2 = 0.0f;
        boolean bl = false;
        for (Feature feature : featureArray) {
            f += feature.p();
        }
        for (Feature feature : featureArray) {
            if (feature.minSize() > n) continue;
            f2 += feature.p();
            bl = true;
        }
        if (!bl) {
            return null;
        }
        float f3 = random.nextFloat();
        float f4 = 0.0f;
        for (Feature feature : featureArray) {
            if (feature.minSize() > n || !(f3 < (f4 += feature.p() / f2 * f))) continue;
            return feature;
        }
        return null;
    }

    private boolean fits(Group group, int n, int n2, Patterns patterns) {
        if (group.sx() == 1 && group.sy() == 1) {
            return true;
        }
        for (int i = 0; i < group.sx(); ++i) {
            for (int j = 0; j < group.sy(); ++j) {
                Thing thing;
                if (i == 0 && j == 0) continue;
                int n3 = n + i;
                int n4 = n2 + j;
                ArrayList<Thing> arrayList = n3 < 16 && n4 < 16 ? this.squares[n3 + n4 * 16] : null;
                Thing thing2 = thing = arrayList == null ? null : WorldGen.first(arrayList, 1);
                if (thing != null && !WorldGen.humanStructure(arrayList) && patterns.places(thing.name())) continue;
                return false;
            }
        }
        return true;
    }

    private static boolean humanStructure(ArrayList<Thing> arrayList) {
        int n = 0;
        boolean bl = false;
        for (Thing thing : arrayList) {
            bl |= (thing.kind() & 2) != 0;
            n += (thing.kind() & 0x34) == 0 ? 1 : 0;
        }
        return n > 1 && !bl;
    }

    private void pending(Biome biome, ArrayList<Thing> arrayList, int n, int n2, int n3, int n4, int n5, int n6) {
        int n7 = n + n2 * 16;
        block12: for (int i = 0; i < 5; ++i) {
            String string = this.pending[i][n7];
            if (string == null || string.equals(ANY)) {
                this.pending[i][n7] = null;
                continue;
            }
            Thing thing = WorldGen.first(arrayList, 2);
            Thing thing2 = WorldGen.first(arrayList, 4);
            Thing thing3 = WorldGen.first(arrayList, 8);
            switch (string) {
                case "$subbiome": {
                    this.subBiome(biome, i, n3, n4, n5, n6);
                    continue block12;
                }
                case "$no_tree": {
                    WorldGen.remove(arrayList, thing);
                    continue block12;
                }
                case "$no_bush": {
                    WorldGen.remove(arrayList, thing2);
                    continue block12;
                }
                case "$no_grass": {
                    WorldGen.remove(arrayList, thing3);
                    continue block12;
                }
                default: {
                    WorldGen.remove(arrayList, thing);
                    WorldGen.remove(arrayList, thing2);
                    WorldGen.remove(arrayList, thing3);
                    arrayList.add(this.pendingThing(i, n7));
                }
            }
        }
    }

    private void subBiome(Biome biome, int n, int n2, int n3, int n4, int n5) {
        if (n == 4 || biome.subs == null || biome.subCount == 0) {
            return;
        }
        for (int i = 0; i < 5; ++i) {
            if (biome.subs[i] == null) continue;
            Biome[] biomeArray = biome.subs[i][n];
            this.subRandom.setSeed(this.source.seed(n2 * 8 + n4, n3 * 8 + n5));
            int n6 = this.subRandom.nextInt(0, biome.subCount);
            this.override[i] = biomeArray == null || n6 >= biomeArray.length ? null : biomeArray[n6];
            return;
        }
    }

    private static void remove(ArrayList<Thing> arrayList, Thing thing) {
        for (int i = 0; i < arrayList.size(); ++i) {
            if (arrayList.get(i) != thing) continue;
            arrayList.remove(i);
            return;
        }
    }

    private static Thing first(ArrayList<Thing> arrayList, int n) {
        for (Thing thing : arrayList) {
            if ((thing.kind() & n) == 0) continue;
            return thing;
        }
        return null;
    }

    static final class Biome {
        final String name;
        final Feature[][] features;
        final Patterns[] placements;
        final Patterns protect;
        Biome[][][] subs;
        int subCount;

        Biome(String string, Feature[][] featureArray, Patterns[] patternsArray, Patterns patterns) {
            this.name = string;
            this.features = featureArray;
            this.placements = patternsArray;
            this.protect = patterns;
        }
    }

    record Thing(String name, int kind, FarTiles.Tile tile) {
    }

    static interface Source {
        public Biome biome(int var1, int var2);

        public long seed(int var1, int var2);
    }

    static final class Patterns {
        private final Pattern[] patterns;
        private final boolean[] allow;
        private final ConcurrentHashMap<String, Boolean> placeable = new ConcurrentHashMap();
        private final ConcurrentHashMap<String, Boolean> matched = new ConcurrentHashMap();

        Patterns(List<String> list) {
            this.patterns = new Pattern[list.size()];
            this.allow = new boolean[list.size()];
            for (int i = 0; i < this.patterns.length; ++i) {
                String string = list.get(i);
                this.allow[i] = !string.startsWith("!");
                String string2 = this.allow[i] ? string : string.substring(1);
                this.patterns[i] = Pattern.compile(string2.replace(".", "\\.").replace("*", ".*").replace("?", ".?"));
            }
        }

        boolean places(String string2) {
            return this.placeable.computeIfAbsent(string2, string -> {
                boolean bl = false;
                for (int i = 0; i < this.patterns.length; ++i) {
                    if (!this.patterns[i].matcher((CharSequence)string).matches()) continue;
                    bl = this.allow[i];
                }
                return bl;
            });
        }

        boolean matchesAny(String string2) {
            return this.matched.computeIfAbsent(string2, string -> {
                for (Pattern pattern : this.patterns) {
                    if (!pattern.matcher((CharSequence)string).matches()) continue;
                    return true;
                }
                return false;
            });
        }
    }

    private record Result(int outcome, String placed) {
    }

    private record Lookup(int reason, Thing tile) {
    }

    record Feature(float p, int minSize, Group[] groups) {
    }

    record Group(int sx, int sy, Thing[] tiles) {
    }
}

