/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.textures.Texture
 */
package viewpoint.far;

import java.util.ArrayList;
import viewpoint.far.FarColours;
import viewpoint.far.FarTiles;
import viewpoint.far.TreeSeasons;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.Tuning;
import viewpoint.render.TreeAtlas;
import viewpoint.render.TreeKinds;
import viewpoint.world.TileMeshes;
import viewpoint.world.WorldMesher;
import zombie.core.textures.Texture;

final class FarTrees {
    static final int CARDS = 2;
    static final LiveSettings.Number LITE_SHARE = LiveSettings.number("lod.treeShare", "Trees kept past the full shell (share)", "World/Levels of detail", 0.0625f, 1.0f, 0.0625f, 0.5f);
    private static final int FOLIAGE = 3032358;
    private static final ArrayList<Kind> kinds = new ArrayList();

    static void add(FarTiles.Tile tile, Texture texture) {
        float f = WorldMesher.pixelScale(texture);
        float[] fArray = new float[]{(float)texture.getWidthOrig() / f, (float)texture.getHeightOrig() / f, TileMeshes.plantHalfWidth(texture), TileMeshes.plantHeight(texture)};
        tile.treeKind = kinds.size();
        Kind kind = new Kind(tile, fArray);
        kinds.add(kind);
        FarTrees.refresh(kind);
    }

    static void seasonChanged() {
        TreeAtlas.reset();
        for (Kind kind : kinds) {
            FarTrees.refresh(kind);
        }
    }

    private static void refresh(Kind kind) {
        FarTiles.Tile tile = kind.tile;
        FarTiles.Tile tile2 = tile.leaves;
        tile.billboard = tile.page == null ? null : TreeAtlas.slot(tile.page, tile.map, tile2 == null ? null : tile2.page, tile2 == null ? null : tile2.map, kind.size);
        int n = tile.colour >= 0 ? tile.colour : 3032358;
        int n2 = tile2 != null && tile2.colour >= 0 ? tile2.colour : n;
        int[] nArray = new int[5];
        boolean[] blArray = new boolean[5];
        for (int i = 0; i < nArray.length; ++i) {
            int n3;
            String string = tile.tree == null ? null : TreeSeasons.leavesName(tile.tree, i);
            int n4 = n3 = string == null ? -1 : FarColours.of(string);
            nArray[i] = tile.tree == null ? n2 : (n3 >= 0 ? n3 : n);
            blArray[i] = tile.tree != null && string == null;
        }
        TreeKinds.set(tile.treeKind, nArray, blArray, kind.size[2], kind.size[3]);
    }

    static int rank(int n, int n2) {
        return FarTrees.hash(n, n2) & 0xFF;
    }

    static int liteKeep() {
        return Math.max(1, Math.round(LITE_SHARE.get() * 256.0f));
    }

    static int boxKeep(int n) {
        return Math.max(1, FarTrees.liteKeep() / n);
    }

    static float grow(int n) {
        return (float)Math.pow(256.0 / (double)n, Tuning.farTreeGrowth);
    }

    static int hash(int n, int n2) {
        int n3 = n * 668265261 ^ n2 * 374761393;
        n3 ^= n3 >>> 15;
        return ((n3 *= -2048144789) ^ n3 >>> 13) & Integer.MAX_VALUE;
    }

    private FarTrees() {
    }

    private record Kind(FarTiles.Tile tile, float[] size) {
    }

    record Thinning(int cellX, int cellY, int keep, float grow) {
        static Thinning of(int n, int n2, int n3) {
            int n4 = n == 0 ? 256 : FarTrees.liteKeep();
            return new Thinning(n2, n3, n4, FarTrees.grow(n4));
        }

        boolean keeps(int n, int n2) {
            return this.keep >= 256 || FarTrees.rank(this.cellX * 256 + n, this.cellY * 256 + n2) < this.keep;
        }
    }
}

