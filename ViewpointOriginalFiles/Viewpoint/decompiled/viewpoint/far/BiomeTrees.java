/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.LotHeader
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.SpriteDetails.IsoObjectType
 *  zombie.iso.sprite.IsoSprite
 *  zombie.iso.sprite.IsoSpriteManager
 *  zombie.iso.worldgen.WorldGenChunk
 *  zombie.iso.worldgen.WorldGenParams
 *  zombie.iso.worldgen.biomes.Feature
 *  zombie.iso.worldgen.biomes.FeatureType
 *  zombie.iso.worldgen.biomes.IBiome
 *  zombie.iso.worldgen.biomes.TileGroup
 *  zombie.iso.worldgen.maps.BiomeMap
 *  zombie.iso.worldgen.maps.BiomeMapEntry
 */
package viewpoint.far;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;
import viewpoint.far.Erosion;
import viewpoint.far.FarTiles;
import viewpoint.far.WorldGen;
import zombie.core.properties.PropertyContainer;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.worldgen.WorldGenChunk;
import zombie.iso.worldgen.WorldGenParams;
import zombie.iso.worldgen.biomes.Feature;
import zombie.iso.worldgen.biomes.FeatureType;
import zombie.iso.worldgen.biomes.IBiome;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.maps.BiomeMap;
import zombie.iso.worldgen.maps.BiomeMapEntry;

final class BiomeTrees {
    private static final int VALUES = 256;
    private static final String RANDOM = "$random";
    private static Table table;
    private static final IdentityHashMap<LotHeader, Legend> legends;
    private static final HashMap<String, Integer> kinds;

    static byte[] image(String string, int n, int n2) {
        File file = new File(string, "maps/biomemap_" + n + "_" + n2 + ".png");
        if (!file.isFile()) {
            return null;
        }
        try {
            IndexColorModel indexColorModel;
            Object object;
            BufferedImage bufferedImage = ImageIO.read(file);
            if (bufferedImage == null || bufferedImage.getWidth() != 256 || bufferedImage.getHeight() != 256) {
                return null;
            }
            WritableRaster writableRaster = bufferedImage.getRaster();
            Object object2 = bufferedImage.getColorModel();
            if (object2 instanceof IndexColorModel) {
                object = (IndexColorModel)object2;
                indexColorModel = object;
            } else {
                indexColorModel = null;
            }
            IndexColorModel indexColorModel2 = indexColorModel;
            object = new byte[65536];
            object2 = new int[256];
            for (int i = 0; i < 256; ++i) {
                writableRaster.getSamples(0, i, 256, 1, 0, (int[])object2);
                for (int j = 0; j < 256; ++j) {
                    object[i * 256 + j] = (byte)(indexColorModel2 == null ? object2[j] : (Object)indexColorModel2.getRed((int)object2[j]));
                }
            }
            return object;
        }
        catch (Exception exception) {
            return null;
        }
    }

    static Table table() {
        WorldGenChunk worldGenChunk;
        Erosion erosion = Erosion.current();
        if (table != null) {
            if (BiomeTrees.table.erosion != erosion) {
                table = new Table(BiomeTrees.table.biomes, BiomeTrees.table.seed, erosion);
            }
            return table;
        }
        IsoWorld isoWorld = IsoWorld.instance;
        BiomeMap biomeMap = isoWorld == null ? null : isoWorld.getBiomeMap();
        WorldGenChunk worldGenChunk2 = worldGenChunk = isoWorld == null ? null : isoWorld.getWgChunk();
        if (biomeMap == null || worldGenChunk == null) {
            return new Table(new WorldGen.Biome[256], 0L, null);
        }
        WorldGen.Biome[] biomeArray = new WorldGen.Biome[256];
        IdentityHashMap<IBiome, WorldGen.Biome> identityHashMap = new IdentityHashMap<IBiome, WorldGen.Biome>();
        for (int i = 0; i < 256; ++i) {
            biomeArray[i] = BiomeTrees.biome(biomeMap, worldGenChunk, i, identityHashMap);
        }
        table = new Table(biomeArray, WorldGenParams.INSTANCE.getSeed(), erosion);
        return table;
    }

    static Legend legend(LotHeader lotHeader) {
        Legend legend = legends.get(lotHeader);
        if (legend == null) {
            String[] stringArray = lotHeader.tilesUsed.toArray(new String[0]);
            int[] nArray = new int[stringArray.length];
            for (int i = 0; i < stringArray.length; ++i) {
                nArray[i] = BiomeTrees.kind(stringArray[i]);
            }
            legend = new Legend(stringArray, nArray);
            legends.put(lotHeader, legend);
        }
        return legend;
    }

    static void forget() {
        table = null;
        legends.clear();
        Erosion.forget();
    }

    static int kind(String string) {
        int n;
        IsoSprite isoSprite;
        Integer n2 = kinds.get(string);
        if (n2 != null) {
            return n2;
        }
        IsoSprite isoSprite2 = isoSprite = string == null ? null : (IsoSprite)IsoSpriteManager.instance.getNamedMap().get(string);
        if (isoSprite == null || isoSprite.getProperties().has(IsoFlagType.FloorOverlay)) {
            n = -1;
        } else {
            PropertyContainer propertyContainer = isoSprite.getProperties();
            boolean bl = string.startsWith("e_newgrass_") || string.startsWith("blends_grassoverlays_");
            boolean bl2 = bl || string.startsWith("d_plants_") || string.startsWith("d_generic_1_") || string.startsWith("d_floorleaves_") || string.startsWith("vegetation_groundcover_");
            n = (propertyContainer.has(IsoFlagType.solidfloor) ? 1 : 0) | (isoSprite.getTileType() == IsoObjectType.tree ? 2 : 0) | ("f_bushes_1".equals(isoSprite.tilesetName) || propertyContainer.get("Bush") != null ? 4 : 0) | (bl ? 8 : 0) | (bl2 ? 16 : 0) | (string.startsWith("boulders_") || string.startsWith("crafting_ore_") ? 32 : 0);
        }
        kinds.put(string, n);
        return n;
    }

    private static WorldGen.Biome biome(BiomeMap biomeMap, WorldGenChunk worldGenChunk, int n, IdentityHashMap<IBiome, WorldGen.Biome> identityHashMap) {
        try {
            String string;
            BiomeMapEntry biomeMapEntry = biomeMap.getEntry(n);
            String string2 = string = biomeMapEntry == null ? null : biomeMapEntry.biome();
            if (string == null || RANDOM.equals(string)) {
                return null;
            }
            IBiome iBiome = worldGenChunk.getMapBiome(0, 0, string);
            return iBiome == null ? null : BiomeTrees.copy(iBiome, identityHashMap);
        }
        catch (RuntimeException runtimeException) {
            return null;
        }
    }

    private static WorldGen.Biome copy(IBiome iBiome, IdentityHashMap<IBiome, WorldGen.Biome> identityHashMap) {
        WorldGen.Biome biome = identityHashMap.get(iBiome);
        if (biome != null) {
            return biome;
        }
        Map map = iBiome.getFeatures();
        Map map2 = iBiome.placements();
        WorldGen.Feature[][] featureArray = map == null ? null : new WorldGen.Feature[5][];
        WorldGen.Patterns[] patternsArray = new WorldGen.Patterns[5];
        for (FeatureType featureType : FeatureType.values()) {
            List list;
            int n = featureType.ordinal();
            List list2 = list = map == null ? null : (List)map.get(featureType);
            if (list != null) {
                featureArray[n] = new WorldGen.Feature[list.size()];
                for (int i = 0; i < list.size(); ++i) {
                    featureArray[n][i] = BiomeTrees.feature((Feature)list.get(i));
                }
            }
            if (map2 == null || map2.get(featureType) == null) continue;
            patternsArray[n] = new WorldGen.Patterns((List)map2.get(featureType));
        }
        List list = iBiome.protectedList();
        biome = new WorldGen.Biome(iBiome.name(), featureArray, patternsArray, list == null ? null : new WorldGen.Patterns(list));
        identityHashMap.put(iBiome, biome);
        BiomeTrees.subs(iBiome, biome, identityHashMap);
        return biome;
    }

    private static void subs(IBiome iBiome, WorldGen.Biome biome, IdentityHashMap<IBiome, WorldGen.Biome> identityHashMap) {
        Map map = iBiome.subBiomes();
        if (map == null || map.isEmpty()) {
            return;
        }
        biome.subs = new WorldGen.Biome[5][][];
        biome.subCount = map.size();
        for (Map.Entry entry : map.entrySet()) {
            WorldGen.Biome[][] biomeArrayArray = new WorldGen.Biome[5][];
            for (Map.Entry entry2 : ((Map)entry.getValue()).entrySet()) {
                List list = (List)entry2.getValue();
                biomeArrayArray[((FeatureType)entry2.getKey()).ordinal()] = new WorldGen.Biome[list.size()];
                for (int i = 0; i < list.size(); ++i) {
                    biomeArrayArray[((FeatureType)entry2.getKey()).ordinal()][i] = BiomeTrees.copy((IBiome)list.get(i), identityHashMap);
                }
            }
            biome.subs[((FeatureType)entry.getKey()).ordinal()] = biomeArrayArray;
        }
    }

    private static WorldGen.Feature feature(Feature feature) {
        WorldGen.Group[] groupArray = new WorldGen.Group[feature.tileGroups().size()];
        for (int i = 0; i < groupArray.length; ++i) {
            TileGroup tileGroup = (TileGroup)feature.tileGroups().get(i);
            WorldGen.Thing[] thingArray = new WorldGen.Thing[tileGroup.tiles().size()];
            for (int j = 0; j < thingArray.length; ++j) {
                String string = (String)tileGroup.tiles().get(j);
                boolean bl = string != null && !string.startsWith("$");
                thingArray[j] = new WorldGen.Thing(string, bl ? Math.max(BiomeTrees.kind(string), 0) : 0, bl ? FarTiles.of(string) : null);
            }
            groupArray[i] = new WorldGen.Group(tileGroup.sx(), tileGroup.sy(), thingArray);
        }
        return new WorldGen.Feature(feature.probability().getValue(), feature.minSize(), groupArray);
    }

    private BiomeTrees() {
    }

    static {
        legends = new IdentityHashMap();
        kinds = new HashMap();
    }

    static final class Table
    implements WorldGen.Source {
        final WorldGen.Biome[] biomes;
        final Erosion erosion;
        private final long seed;
        private final long mixX;
        private final long mixY;
        private byte[] values;
        private int cellX0;
        private int cellY0;

        Table(WorldGen.Biome[] biomeArray, long l, Erosion erosion) {
            this.biomes = biomeArray;
            this.seed = l;
            this.erosion = erosion;
            Random random = new Random(l);
            this.mixX = random.nextLong();
            this.mixY = random.nextLong();
        }

        Table forCell(byte[] byArray, int n, int n2) {
            Table table = new Table(this.biomes, this.seed, this.erosion);
            table.values = byArray;
            table.cellX0 = n * 256;
            table.cellY0 = n2 * 256;
            return table;
        }

        @Override
        public WorldGen.Biome biome(int n, int n2) {
            return this.values == null ? null : this.biomes[this.values[(n2 - this.cellY0) * 256 + n - this.cellX0] & 0xFF];
        }

        @Override
        public long seed(int n, int n2) {
            return (long)n * this.mixX ^ (long)n2 * this.mixY ^ this.seed;
        }

        int turn(int n, int n2) {
            return WorldGen.generator(this.seed(n, n2)).nextInt(100);
        }
    }

    record Legend(String[] names, int[] kinds) {
    }

    static final class Ground {
        private final int x0;
        private final int y0;
        private final int size;
        private final int[] head;
        private final int[] tail;
        private int[] next = new int[1024];
        private int[] index = new int[1024];
        private int count;

        Ground(int n, int n2, int n3) {
            this.x0 = n;
            this.y0 = n2;
            this.size = n3;
            this.head = new int[n3 * n3];
            this.tail = new int[n3 * n3];
            Arrays.fill(this.head, -1);
        }

        boolean add(int n, FarTiles.Tile[] tileArray, int n2, int n3, int n4) {
            int n5 = n2 - this.x0;
            int n6 = n3 - this.y0;
            if (n4 != 0 || n5 < 0 || n6 < 0 || n5 >= this.size || n6 >= this.size) {
                return false;
            }
            if (this.count == this.next.length) {
                this.next = Arrays.copyOf(this.next, this.count * 2);
                this.index = Arrays.copyOf(this.index, this.count * 2);
            }
            int n7 = n6 * this.size + n5;
            this.next[this.count] = -1;
            this.index[this.count] = n;
            if (this.head[n7] < 0) {
                this.head[n7] = this.count;
            } else {
                this.next[this.tail[n7]] = this.count;
            }
            ++this.count;
            return n < tileArray.length && (tileArray[n].flags & 8) != 0;
        }

        void grow(Table table, byte[] byArray, int n, int n2, FarTiles.Tile[] tileArray, Legend legend, byte[][] byArray2, Grown grown, Grown grown2) {
            Table table2 = table == null ? null : table.forCell(byArray, n, n2);
            for (int i = 0; i < this.size; i += 16) {
                for (int j = 0; j < this.size; j += 16) {
                    this.block(table2, j, i, n, n2, tileArray, legend, byArray2, grown, grown2);
                }
            }
        }

        private void block(Table table, int n, int n2, int n3, int n4, FarTiles.Tile[] tileArray, Legend legend, byte[][] byArray, Grown grown, Grown grown2) {
            WorldGen worldGen;
            int n5 = n3 * 256 + this.x0 + n;
            int n6 = n4 * 256 + this.y0 + n2;
            ArrayList[] arrayListArray = new ArrayList[256];
            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 16; ++j) {
                    arrayListArray[j + i * 16] = this.objects((n2 + i) * this.size + n + j, tileArray, legend);
                }
            }
            WorldGen worldGen2 = worldGen = table == null ? null : new WorldGen(table, arrayListArray, n5, n6);
            if (worldGen != null) {
                worldGen.run();
            }
            Erosion erosion = table == null ? null : table.erosion;
            byte[][] byArray2 = grown2 == null ? null : byArray;
            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 16; ++j) {
                    int n7;
                    WorldGen.Thing thing = worldGen != null ? worldGen.tree(j, i) : Ground.first(arrayListArray[j + i * 16]);
                    FarTiles.Tile tile = thing == null ? null : thing.tile();
                    int n8 = n7 = erosion == null ? -1 : erosion.square(table, arrayListArray[j + i * 16], n5 + j, n6 + i, byArray2);
                    if (n7 != -1) {
                        tile = erosion.tile(n7);
                    }
                    if (tile == null || tile.treeKind < 0) continue;
                    if (table != null && byArray != null) {
                        tile = tile.inSeason(byArray[0][table.turn(n5 + j, n6 + i)]);
                    }
                    grown.put(tile, this.x0 + n + j, this.y0 + n2 + i);
                }
            }
            if (grown2 != null) {
                Ground.plants(arrayListArray, this.x0 + n, this.y0 + n2, grown2);
            }
        }

        private static void plants(ArrayList<WorldGen.Thing>[] arrayListArray, int n, int n2, Grown grown) {
            for (int i = 0; i < arrayListArray.length; ++i) {
                for (WorldGen.Thing thing : arrayListArray[i]) {
                    FarTiles.Tile tile = thing.tile();
                    if (tile == null || tile.shell != 5) continue;
                    grown.put(tile, n + i % 16, n2 + i / 16);
                }
            }
        }

        private ArrayList<WorldGen.Thing> objects(int n, FarTiles.Tile[] tileArray, Legend legend) {
            ArrayList<WorldGen.Thing> arrayList = new ArrayList<WorldGen.Thing>(4);
            String string = null;
            int n2 = this.head[n];
            while (n2 >= 0) {
                int n3 = this.index[n2];
                if (legend != null && n3 < legend.kinds.length && legend.kinds[n3] >= 0) {
                    String string2 = legend.names[n3];
                    int n4 = legend.kinds[n3];
                    if (!string2.startsWith("vegetation_trees") || string != null && string.startsWith("blends_natural")) {
                        if (string == null && (n4 & 1) != 0) {
                            string = string2;
                        }
                        arrayList.add(new WorldGen.Thing(string2, n4, n3 < tileArray.length ? tileArray[n3] : null));
                    }
                }
                n2 = this.next[n2];
            }
            return arrayList;
        }

        private static WorldGen.Thing first(ArrayList<WorldGen.Thing> arrayList) {
            for (WorldGen.Thing thing : arrayList) {
                if ((thing.kind() & 2) == 0) continue;
                return thing;
            }
            return null;
        }
    }

    static interface Grown {
        public void put(FarTiles.Tile var1, int var2, int var3);
    }
}

