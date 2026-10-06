/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.util.Arrays;
import viewpoint.far.BiomeTrees;
import viewpoint.far.FarTiles;
import viewpoint.far.FarTrees;
import viewpoint.far.ShellMesher;

public final class FarCell {
    public static final int CHUNKS = 32;
    static final int STEPS = 8;
    static final int TREE_SHORTS = 4;
    static final int TURN_SHIFT = 8;
    private static final int GRASS = 5135133;
    final int cellX;
    final int cellY;
    public final short[] height = new short[65536];
    public final int[] top = new int[65536];
    public final int[] side = new int[65536];
    public float maxHeight;
    byte[] biomes;
    short[] trees;
    int[] treesBelow;

    private FarCell(int n, int n2) {
        this.cellX = n;
        this.cellY = n2;
    }

    static FarCell read(int n5, int n6, byte[] byArray, int n7, int n8, FarTiles.Tile[] tileArray, int[] nArray, byte[] byArray2, BiomeTrees.Table table, BiomeTrees.Legend legend) {
        FarCell farCell = new FarCell(n5, n6);
        float[] fArray = new float[65536];
        float[] fArray2 = new float[65536];
        Trees trees = new Trees();
        BiomeTrees.Ground ground = new BiomeTrees.Ground(0, 0, 256);
        int[] nArray2 = farCell.top;
        int[] nArray3 = farCell.side;
        boolean[] blArray = new boolean[65536];
        Arrays.fill(nArray2, -1);
        Arrays.fill(nArray3, -1);
        ShellMesher.read(byArray, n7, n8, 0, 0, 32, (n, n2, n3, n4) -> {
            if (ground.add(n, tileArray, n2, n3, n4) || n >= tileArray.length) {
                return;
            }
            if (tileArray[n].treeKind >= 0) {
                trees.add(tileArray[n], n2, n3, n4, 50);
            } else {
                FarCell.add(tileArray[n], n4, n3 * 256 + n2, fArray, fArray2, nArray2, nArray3, blArray);
            }
        });
        farCell.biomes = byArray2;
        ground.grow(table, byArray2, n5, n6, tileArray, legend, null, (tile, n3, n4) -> {
            if (!trees.on[n4 * 256 + n3]) {
                int n5 = n5 * 256 + n3;
                int n6 = n6 * 256 + n4;
                trees.add(tile, n3, n4, 0, table == null ? 50 : table.turn(n5, n6));
            }
        }, null);
        FarCell.finishSquares(farCell, nArray, fArray, nArray2, nArray3);
        farCell.treesBelow = new int[257];
        farCell.trees = trees.ranked(n5, n6, farCell.treesBelow);
        farCell.maxHeight = Math.max(farCell.maxHeight, trees.top);
        return farCell;
    }

    private static void finishSquares(FarCell farCell, int[] nArray, float[] fArray, int[] nArray2, int[] nArray3) {
        for (int i = 0; i < 65536; ++i) {
            int n = nArray != null ? nArray[i] & 0xFFFFFF : -1;
            int n2 = Math.min(Short.MAX_VALUE, Math.round(fArray[i] * 8.0f));
            farCell.height[i] = (short)n2;
            farCell.maxHeight = Math.max(farCell.maxHeight, (float)n2 / 8.0f);
            if (nArray2[i] < 0) {
                int n3 = nArray2[i] = n >= 0 ? n : 5135133;
            }
            if (nArray3[i] >= 0) continue;
            nArray3[i] = nArray2[i];
        }
    }

    private static void add(FarTiles.Tile tile, int n, int n2, float[] fArray, float[] fArray2, int[] nArray, int[] nArray2, boolean[] blArray) {
        boolean bl;
        if (tile.kind == 0) {
            return;
        }
        float f = (float)n + tile.height;
        if (f >= fArray[n2]) {
            fArray[n2] = f;
            if (tile.colour >= 0) {
                nArray[n2] = tile.colour;
            }
        }
        if (tile.kind == 1 || tile.colour < 0) {
            return;
        }
        boolean bl2 = bl = tile.kind == 2;
        if (bl && (!blArray[n2] || f >= fArray2[n2]) || !bl && !blArray[n2] && f >= fArray2[n2]) {
            fArray2[n2] = f;
            nArray2[n2] = tile.colour;
            int n3 = n2;
            blArray[n3] = blArray[n3] | bl;
        }
    }

    static final class Trees {
        final boolean[] on = new boolean[65536];
        short[] list = new short[1024];
        int count;
        float top;

        Trees() {
        }

        void add(FarTiles.Tile tile, int n, int n2, int n3, int n4) {
            if ((this.count + 1) * 4 > this.list.length) {
                this.list = Arrays.copyOf(this.list, this.list.length * 2);
            }
            int n5 = this.count++ * 4;
            this.list[n5] = (short)n;
            this.list[n5 + 1] = (short)n2;
            this.list[n5 + 2] = (short)(n3 | n4 << 8);
            this.list[n5 + 3] = (short)tile.treeKind;
            this.top = Math.max(this.top, (float)n3 + tile.height);
            this.on[n2 * 256 + n] = true;
        }

        short[] ranked(int n, int n2, int[] nArray) {
            int n3;
            int n4;
            int[] nArray2 = new int[this.count];
            for (n4 = 0; n4 < this.count; ++n4) {
                short s = this.list[n4 * 4];
                n3 = this.list[n4 * 4 + 1];
                nArray2[n4] = FarTrees.rank(n * 256 + s, n2 * 256 + n3);
                int n5 = nArray2[n4] + 1;
                nArray[n5] = nArray[n5] + 1;
            }
            for (n4 = 0; n4 < 256; ++n4) {
                int n6 = n4 + 1;
                nArray[n6] = nArray[n6] + nArray[n4];
            }
            int[] nArray3 = Arrays.copyOf(nArray, 256);
            short[] sArray = new short[this.count * 4];
            for (n3 = 0; n3 < this.count; ++n3) {
                int n7 = nArray2[n3];
                int n8 = nArray3[n7];
                nArray3[n7] = n8 + 1;
                System.arraycopy(this.list, n3 * 4, sArray, n8 * 4, 4);
            }
            return sArray;
        }
    }
}

