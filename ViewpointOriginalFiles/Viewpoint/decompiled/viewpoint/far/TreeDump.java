/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.ZomboidFileSystem
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.worldgen.WorldGenChunk
 *  zombie.iso.worldgen.WorldGenParams
 *  zombie.iso.worldgen.biomes.Feature
 *  zombie.iso.worldgen.biomes.IBiome
 *  zombie.iso.worldgen.biomes.TileGroup
 *  zombie.iso.worldgen.maps.BiomeMap
 */
package viewpoint.far;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import viewpoint.far.Erosion;
import zombie.ZomboidFileSystem;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.worldgen.WorldGenChunk;
import zombie.iso.worldgen.WorldGenParams;
import zombie.iso.worldgen.biomes.Feature;
import zombie.iso.worldgen.biomes.IBiome;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.maps.BiomeMap;

final class TreeDump {
    private static final String FILE = "viewpoint-tree-dump.txt";
    private static final int VALUES = 256;

    static void write(IsoChunkMap isoChunkMap, int n, int n2, int n3) throws IOException {
        File file = new File(ZomboidFileSystem.instance.getCacheDir(), FILE);
        try (PrintWriter printWriter = new PrintWriter(file, StandardCharsets.UTF_8);){
            printWriter.println("seed " + WorldGenParams.INSTANCE.getSeed());
            Erosion erosion = Erosion.current();
            if (erosion != null) {
                erosion.dump(printWriter);
            }
            TreeDump.biomes(printWriter);
            for (int i = n2 - n3; i <= n2 + n3; ++i) {
                for (int j = n - n3; j <= n + n3; ++j) {
                    IsoChunk isoChunk = isoChunkMap.getChunkForGridSquare(j * 8, i * 8);
                    if (isoChunk == null || !isoChunk.loaded) continue;
                    TreeDump.squares(printWriter, isoChunk, j, i);
                }
            }
        }
        System.out.println("[Viewpoint] far tree check: ground truth written to " + String.valueOf(file));
    }

    private static void biomes(PrintWriter printWriter) {
        StringBuilder stringBuilder;
        Object object;
        BiomeMap biomeMap = IsoWorld.instance.getBiomeMap();
        WorldGenChunk worldGenChunk = IsoWorld.instance.getWgChunk();
        ArrayDeque<StringBuilder> arrayDeque = new ArrayDeque<StringBuilder>();
        HashSet<String> hashSet = new HashSet<String>();
        for (int i = 0; i < 256; ++i) {
            object = biomeMap.getEntry(i);
            if (object == null) continue;
            printWriter.println("value " + i + " " + object.biome() + " " + object.ore() + " " + object.zone());
            for (String object2 : new String[]{object.biome(), object.ore()}) {
                StringBuilder stringBuilder2 = stringBuilder = object2 == null || object2.startsWith("$") ? null : worldGenChunk.getMapBiome(0, 0, object2);
                if (stringBuilder == null || !hashSet.add(stringBuilder.name())) continue;
                arrayDeque.add(stringBuilder);
            }
        }
        while (!arrayDeque.isEmpty()) {
            IBiome iBiome = (IBiome)arrayDeque.poll();
            TreeDump.biome(printWriter, iBiome);
            object = iBiome.subBiomes();
            for (Map.Entry entry : object == null ? Map.of().entrySet() : object.entrySet()) {
                for (Map.Entry entry2 : ((Map)entry.getValue()).entrySet()) {
                    stringBuilder = new StringBuilder(" sub " + String.valueOf(entry.getKey()) + " " + String.valueOf(entry2.getKey()));
                    for (IBiome iBiome2 : (List)entry2.getValue()) {
                        stringBuilder.append(' ').append(iBiome2.name());
                        if (!hashSet.add(iBiome2.name())) continue;
                        arrayDeque.add((StringBuilder)iBiome2);
                    }
                    printWriter.println(stringBuilder);
                }
            }
        }
    }

    private static void biome(PrintWriter printWriter, IBiome iBiome) {
        printWriter.println("biome " + iBiome.name() + " parent " + iBiome.parent() + " zombies " + iBiome.zombies());
        Map map = iBiome.placements();
        for (Map.Entry object : map == null ? Map.of().entrySet() : map.entrySet()) {
            printWriter.println(" place " + String.valueOf(object.getKey()) + " " + String.join((CharSequence)" ", (Iterable)object.getValue()));
        }
        Map map2 = iBiome.getFeatures();
        for (Map.Entry entry : map2 == null ? Map.of().entrySet() : map2.entrySet()) {
            for (Feature feature : (List)entry.getValue()) {
                printWriter.println(" feature " + String.valueOf(entry.getKey()) + " " + feature.probability().getValue() + " " + feature.minSize() + " " + feature.maxSize());
                for (TileGroup tileGroup : feature.tileGroups()) {
                    printWriter.println("  group " + tileGroup.sx() + " " + tileGroup.sy() + " " + String.join((CharSequence)" ", tileGroup.tiles()));
                }
            }
        }
    }

    private static void squares(PrintWriter printWriter, IsoChunk isoChunk, int n, int n2) {
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                IsoGridSquare isoGridSquare = isoChunk.getGridSquare(j, i, 0);
                StringBuilder stringBuilder = new StringBuilder("square " + (n * 8 + j) + " " + (n2 * 8 + i));
                for (int k = 0; isoGridSquare != null && k < isoGridSquare.getObjects().size(); ++k) {
                    IsoObject isoObject = (IsoObject)isoGridSquare.getObjects().get(k);
                    stringBuilder.append(' ').append(isoObject.getSprite() == null ? "-" : isoObject.getSprite().getName());
                }
                printWriter.println(stringBuilder);
            }
        }
    }

    private TreeDump() {
    }
}

