/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.textures.Texture
 *  zombie.erosion.ErosionMain
 *  zombie.erosion.season.ErosionSeason
 *  zombie.iso.IsoDirections
 *  zombie.iso.sprite.IsoSprite
 *  zombie.iso.sprite.IsoSpriteManager
 */
package viewpoint.far;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import viewpoint.far.FarColours;
import viewpoint.far.FarTiles;
import viewpoint.far.FarTrees;
import viewpoint.render.TreeKinds;
import viewpoint.world.TileMesh;
import viewpoint.world.TileMeshes;
import viewpoint.world.WorldMesher;
import zombie.core.textures.Texture;
import zombie.erosion.ErosionMain;
import zombie.erosion.season.ErosionSeason;
import zombie.iso.IsoDirections;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;

final class TreeSeasons {
    static final int TURNS = 100;
    static final int DISPLAYS = 5;
    static final int TREES = 0;
    static final int BUSHES = 1;
    static final int GRASS = 2;
    static final int TABLES = 3;
    private static final int SUMMER = 2;
    private static final String[] EVERGREENS = new String[]{"e_americanholly", "e_canadianhemlock", "e_virginiapine"};
    private static final int[][] FIRST_DISPLAY = new int[][]{{0, 1, 2, 0, 4, 0}, {0, 1, 2, 0, 4, 0}, {0, 1, 2, 0, 4, 5}};
    private static final int[][] SECOND_DISPLAY = new int[][]{{-1, -1, 3, -1, 0, -1}, {-1, -1, 2, -1, 0, -1}, {-1, -1, 3, -1, 5, -1}};
    private static final int[] BEFORE = new int[]{2, 5, 1, 2, 2, 4};
    private static int shown = -1;
    private static volatile byte[][] displays = new byte[3][100];
    private static final byte[][] next = new byte[3][100];
    private static final IdentityHashMap<FarTiles.Tile, Texture> seasonal = new IdentityHashMap();

    static boolean update() {
        int n;
        int n2;
        ErosionMain erosionMain = ErosionMain.getInstance();
        ErosionSeason erosionSeason = erosionMain == null ? null : erosionMain.getSeasons();
        for (n2 = 0; n2 < 3; ++n2) {
            for (n = 0; n < 100; ++n) {
                TreeSeasons.next[n2][n] = (byte)(erosionSeason == null ? 2 : TreeSeasons.display(n2, erosionSeason.getSeason(), erosionSeason.getSeasonDay(), erosionSeason.getSeasonDays(), (float)n / 100.0f));
            }
        }
        n2 = next[0][50];
        if (n2 == shown && Arrays.deepEquals((Object[])next, (Object[])displays)) {
            return false;
        }
        n = n2 != shown ? 1 : 0;
        shown = n2;
        byte[][] byArrayArray = new byte[3][];
        for (int i = 0; i < 3; ++i) {
            byArrayArray[i] = (byte[])next[i].clone();
        }
        displays = byArrayArray;
        TreeKinds.displays(byArrayArray[0]);
        for (Map.Entry<FarTiles.Tile, Texture> entry : seasonal.entrySet()) {
            if (n != 0) {
                entry.getKey().leaves = TreeSeasons.leaves(entry.getKey().tree, shown);
            }
            TreeSeasons.variants(entry.getKey(), entry.getValue());
        }
        if (n != 0) {
            FarTrees.seasonChanged();
        }
        return true;
    }

    static byte[][] displays() {
        return displays;
    }

    static int display(int n, float f, float f2, float f3) {
        return TreeSeasons.display(0, n, f, f2, f3);
    }

    static int display(int n, int n2, float f, float f2, float f3) {
        boolean bl;
        int[] nArray = FIRST_DISPLAY[n];
        int[] nArray2 = SECOND_DISPLAY[n];
        float f4 = f2 / 2.0f;
        boolean bl2 = bl = nArray2[n2] >= 0;
        if (bl && f >= f4 + f4 * f3) {
            return nArray2[n2];
        }
        float f5 = bl ? f4 : f2;
        if (f >= f5 * f3) {
            return nArray[n2];
        }
        int n3 = BEFORE[n2];
        return nArray2[n3] >= 0 ? nArray2[n3] : nArray[n3];
    }

    static boolean seasonal(String string) {
        return TreeSeasons.leafSheet(string) != null;
    }

    static void add(FarTiles.Tile tile, Texture texture) {
        tile.leaves = shown < 0 ? null : TreeSeasons.leaves(tile.tree, shown);
        seasonal.put(tile, texture);
        TreeSeasons.variants(tile, texture);
    }

    static void forget() {
        shown = -1;
    }

    private static void variants(FarTiles.Tile tile, Texture texture) {
        FarTiles.Tile[] tileArray = tile.seasons;
        FarTiles.Tile[] tileArray2 = tileArray == null ? new FarTiles.Tile[5] : (FarTiles.Tile[])tileArray.clone();
        boolean bl = false;
        for (byte by : displays[0]) {
            if (by == shown || tileArray2[by] != null || shown < 0) continue;
            FarTiles.Tile tile2 = new FarTiles.Tile(tile.kind, tile.height, tile.colour, tile.shell, tile.page, tile.map, tile.mesh, tile.north, tile.west, tile.flags, null);
            tile2.leaves = TreeSeasons.leaves(tile.tree, by);
            tile2.card = tile.card;
            if (tile.kind == 3) {
                FarTrees.add(tile2, texture);
            }
            tileArray2[by] = tile2;
            bl = true;
        }
        if (bl) {
            tile.seasons = tileArray2;
        }
    }

    private static String[] leafSheet(String string) {
        String[] stringArray;
        if (string == null || !string.startsWith("e_") || string.startsWith("e_newgrass_1_")) {
            return null;
        }
        for (String string2 : EVERGREENS) {
            if (!string.startsWith(string2)) continue;
            return null;
        }
        int n = string.lastIndexOf(95);
        if (n > 0) {
            String[] stringArray2 = new String[2];
            stringArray2[0] = string.substring(0, n);
            stringArray = stringArray2;
            stringArray2[1] = string.substring(n + 1);
        } else {
            stringArray = null;
        }
        return stringArray;
    }

    private static FarTiles.Tile leaves(String string, int n) {
        String string2 = TreeSeasons.leavesName(string, n);
        return string2 == null ? null : TreeSeasons.leafTile(string2, false);
    }

    static String leavesName(String string, int n) {
        int n2;
        String[] stringArray = TreeSeasons.leafSheet(string);
        if (stringArray == null || n <= 0) {
            return null;
        }
        try {
            n2 = Integer.parseInt(stringArray[1]);
        }
        catch (NumberFormatException numberFormatException) {
            return null;
        }
        String string2 = stringArray[0];
        int n3 = n + 1;
        int n4 = string2.contains("JUMBOX") ? n3 : (string2.contains("JUMBO") ? n3 * 2 + n2 % 2 : (n2 < 4 ? n3 * 4 + n2 : -1));
        return n4 < 0 ? null : string2 + "_" + n4;
    }

    static FarTiles.Tile leafTile(String string, boolean bl) {
        TileMesh tileMesh;
        Texture texture;
        IsoSprite isoSprite = (IsoSprite)IsoSpriteManager.instance.getNamedMap().get(string);
        Texture texture2 = texture = isoSprite == null ? null : isoSprite.getTextureForCurrentFrame(IsoDirections.N);
        if (texture == null || !texture.isReady() || texture.getTextureId() == null) {
            return null;
        }
        TileMesh tileMesh2 = tileMesh = bl ? null : TileMeshes.crossed(texture, 2);
        if (tileMesh != null && tileMesh.vertCount == 0) {
            return null;
        }
        FarTiles.Tile tile = new FarTiles.Tile(bl ? (byte)0 : 3, 0.0f, FarColours.of(string), bl ? (byte)5 : 4, texture.getTextureId(), WorldMesher.textureMapping(texture), tileMesh, 0, 0, bl ? (byte)32 : 0, null);
        tile.card = bl ? TileMeshes.plantCard(texture) : null;
        return tile;
    }

    private TreeSeasons() {
    }
}

