/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.erosion.ErosionData$Square
 *  zombie.erosion.categories.ErosionCategory$Data
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.objects.IsoTree
 */
package viewpoint.far;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import viewpoint.core.View;
import viewpoint.far.BiomeTrees;
import viewpoint.far.FarTiles;
import viewpoint.far.FarWorld;
import viewpoint.far.Lotpacks;
import viewpoint.far.ShellMesher;
import viewpoint.far.TreeDump;
import viewpoint.far.TreeSeasons;
import viewpoint.platform.LiveSettings;
import zombie.characters.IsoPlayer;
import zombie.erosion.ErosionData;
import zombie.erosion.categories.ErosionCategory;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoTree;

final class FarTreeCheck {
    private static final LiveSettings.Toggle RUN = LiveSettings.toggle("debug.checkFarTrees", "Compare the far trees with the chunks' (console, once)", "Debug/Views", false);
    private static final int EXAMPLES = 12;
    private static final int REACH = 4;

    static void update() {
        if (!RUN.get()) {
            return;
        }
        RUN.set(false);
        try {
            FarTreeCheck.run();
        }
        catch (IOException | RuntimeException exception) {
            System.out.println("[Viewpoint] far tree check failed: " + String.valueOf(exception));
        }
    }

    private static void run() throws IOException {
        Object object;
        IsoPlayer isoPlayer = IsoPlayer.getInstance();
        IsoChunkMap isoChunkMap = IsoWorld.instance.getCell().getChunkMap(0);
        int n = View.radiusChunks();
        int n2 = Math.floorDiv((int)isoPlayer.getX(), 8);
        int n3 = Math.floorDiv((int)isoPlayer.getY(), 8);
        HashMap<Long, GameTree> hashMap = new HashMap<Long, GameTree>();
        HashSet<Long> hashSet = new HashSet<Long>();
        for (int i = n3 - n; i <= n3 + n; ++i) {
            for (int j = n2 - n; j <= n2 + n; ++j) {
                object = isoChunkMap.getChunkForGridSquare(j * 8, i * 8);
                if (object == null || !object.loaded) continue;
                FarTreeCheck.gameTrees((IsoChunk)object, j, i, hashMap, hashSet);
            }
        }
        HashMap<Long, FarTiles.Tile> hashMap2 = new HashMap<Long, FarTiles.Tile>();
        HashSet<Long> hashSet2 = new HashSet<Long>();
        object = FarTreeCheck.farTrees(n2, n3, n, hashMap2, hashSet2);
        FarTreeCheck.report(hashMap, hashSet, hashMap2, hashSet2, object);
        TreeDump.write(isoChunkMap, n2, n3, n);
    }

    private static void gameTrees(IsoChunk isoChunk, int n, int n2, HashMap<Long, GameTree> hashMap, HashSet<Long> hashSet) {
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                int n3;
                IsoTree isoTree;
                int n4 = n * 8 + j;
                int n5 = n2 * 8 + i;
                hashSet.add(FarWorld.key(n4, n5));
                IsoGridSquare isoGridSquare = isoChunk.getGridSquare(j, i, 0);
                IsoTree isoTree2 = isoTree = isoGridSquare == null ? null : isoGridSquare.getTree();
                if (isoTree == null || isoTree.getSprite() == null || isoTree.getSprite().getName() == null) continue;
                ErosionData.Square square = isoGridSquare.getErosionData();
                int n6 = -1;
                boolean bl = false;
                for (n3 = 0; square != null && n3 < square.regions.size(); ++n3) {
                    ErosionCategory.Data data = (ErosionCategory.Data)square.regions.get(n3);
                    if (!data.getClass().getName().endsWith("NatureTrees$CategoryData")) continue;
                    n6 = data.dispSeason;
                    bl = data.hasSpawned;
                }
                n3 = square == null || !square.init ? -1 : Math.round(square.magicNum * 100.0f);
                hashMap.put(FarWorld.key(n4, n5), new GameTree(isoTree.getSprite().getName(), n3, n6, bl));
            }
        }
    }

    private static int[] farTrees(int n, int n2, int n7, HashMap<Long, FarTiles.Tile> hashMap, HashSet<Long> hashSet) throws IOException {
        int n8 = 32;
        int n9 = 0;
        int n10 = 0;
        BiomeTrees.Table table = BiomeTrees.table();
        for (int i = Math.floorDiv(n2 - n7, n8); i <= Math.floorDiv(n2 + n7, n8); ++i) {
            for (int j = Math.floorDiv(n - n7, n8); j <= Math.floorDiv(n + n7, n8); ++j) {
                FarWorld.Slot slot = FarWorld.readCell(j, i);
                if (slot == null) {
                    ++n10;
                    continue;
                }
                ++n9;
                int n11 = j * 256;
                int n12 = i * 256;
                FarTiles.Tile[] tileArray = slot.tiles;
                BiomeTrees.Ground ground = new BiomeTrees.Ground(0, 0, 256);
                byte[] byArray = Lotpacks.read(slot.lotpack, Lotpacks.get(slot.lotpack));
                ShellMesher.read(byArray, slot.minLevel, slot.maxLevel, 0, 0, 32, (n3, n4, n5, n6) -> {
                    if (ground.add(n3, tileArray, n4, n5, n6)) {
                        hashSet.add(FarWorld.key(n11 + n4, n12 + n5));
                    }
                });
                int n13 = j;
                int n14 = i;
                ground.grow(table, slot.biomes, n13, n14, tileArray, slot.legend, null, (tile, n3, n4) -> hashMap.put(FarWorld.key(n11 + n3, n12 + n4), tile), null);
            }
        }
        return new int[]{n9, n10};
    }

    private static void report(HashMap<Long, GameTree> hashMap, HashSet<Long> hashSet, HashMap<Long, FarTiles.Tile> hashMap2, HashSet<Long> hashSet2, int[] nArray) {
        IdentityHashMap<FarTiles.Tile, String> identityHashMap = new IdentityHashMap<FarTiles.Tile, String>();
        for (Map.Entry<String, FarTiles.Tile> object2 : FarTiles.byName().entrySet()) {
            identityHashMap.putIfAbsent(object2.getValue(), object2.getKey());
        }
        BiomeTrees.Table table = BiomeTrees.table();
        byte[] byArray = TreeSeasons.displays()[0];
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        ArrayList<String> arrayList = new ArrayList<String>();
        ArrayList<String> arrayList2 = new ArrayList<String>();
        ArrayList<String> arrayList3 = new ArrayList<String>();
        for (Map.Entry<Long, GameTree> entry : hashMap.entrySet()) {
            FarTiles.Tile tile;
            int n6 = (int)(entry.getKey() >> 32);
            int n7 = (int)entry.getKey().longValue();
            GameTree gameTree = entry.getValue();
            int n8 = table.turn(n6, n7);
            if (gameTree.turn >= 0) {
                ++n2;
                n3 += gameTree.turn == n8 ? 1 : 0;
            }
            if (gameTree.season >= 0) {
                ++n4;
                n5 += gameTree.season == byArray[n8] ? 1 : 0;
            }
            if ((tile = hashMap2.get(entry.getKey())) == null) {
                arrayList2.add(n6 + "," + n7 + " " + gameTree.name + (gameTree.spawned ? " erosion-spawned" : "") + (FarTreeCheck.spotNear(hashSet2, n6, n7) ? " (a spot within reach)" : " (no spot)"));
                continue;
            }
            if (gameTree.name.equals(identityHashMap.get(tile))) {
                ++n;
                continue;
            }
            arrayList.add(n6 + "," + n7 + " game " + gameTree.name + ", far " + (String)identityHashMap.get(tile));
        }
        for (Map.Entry<Long, Object> entry : hashMap2.entrySet()) {
            if (!hashSet.contains(entry.getKey()) || hashMap.containsKey(entry.getKey())) continue;
            arrayList3.add((int)(entry.getKey() >> 32) + "," + (int)entry.getKey().longValue() + " " + (String)identityHashMap.get(entry.getValue()));
        }
        System.out.println("[Viewpoint] far tree check: " + hashMap.size() + " trees on the loaded ground, cells read " + nArray[0] + " (not read " + nArray[1] + "); same " + n + ", other kind " + arrayList.size() + ", game only " + arrayList2.size() + ", far only " + arrayList3.size() + "; erosion turn right " + n3 + " of " + n2 + ", season right " + n5 + " of " + n4);
        FarTreeCheck.examples("other kind", arrayList);
        FarTreeCheck.examples("game only", arrayList2);
        FarTreeCheck.examples("far only", arrayList3);
    }

    private static boolean spotNear(HashSet<Long> hashSet, int n, int n2) {
        for (int i = 0; i <= 4; ++i) {
            for (int j = 0; j <= 4; ++j) {
                if (!hashSet.contains(FarWorld.key(n - j, n2 - i))) continue;
                return true;
            }
        }
        return false;
    }

    private static void examples(String string, ArrayList<String> arrayList) {
        for (int i = 0; i < Math.min(12, arrayList.size()); ++i) {
            System.out.println("[Viewpoint] far tree check, " + string + ": " + arrayList.get(i));
        }
    }

    private FarTreeCheck() {
    }

    private record GameTree(String name, int turn, int season, boolean spawned) {
    }
}

