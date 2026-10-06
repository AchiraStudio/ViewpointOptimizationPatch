/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import viewpoint.far.BiomeTrees;
import viewpoint.far.FarMesher;
import viewpoint.far.FarTiles;
import viewpoint.far.FarTrees;
import viewpoint.far.GroundCapture;
import viewpoint.platform.VertexFormat;
import viewpoint.render.GroundPage;
import viewpoint.render.GroundTiles;
import viewpoint.render.TreeAtlas;
import viewpoint.world.Cook;
import viewpoint.world.Recipe;

final class ShellMesher {
    static final int FULL = 0;
    static final int LITE = 1;
    private static final float INSET = 0.2f;
    private static final int MAX_LAYER = 3;
    private static final float[] GROUND_RECT = new float[]{0.015151516f, 0.015151516f, 0.9848485f, 0.9848485f};

    ShellMesher() {
    }

    static int[] facades(byte[] byArray, int n5, int n6, FarTiles.Tile[] tileArray, short[][] sArray, int n7) {
        HashMap hashMap = new HashMap();
        int[] nArray = new int[Math.max(1, n7) * 6];
        int[] nArray2 = new int[nArray.length];
        Arrays.fill(nArray, -1);
        ShellMesher.read(byArray, n5, n6, 0, 0, 32, (n, n2, n3, n4) -> {
            FarTiles.Tile tile;
            FarTiles.Tile tile2 = tile = n < tileArray.length ? tileArray[n] : FarTiles.EMPTY;
            if (tile.shell != 1 || ShellMesher.building(sArray, n2, n3, n4) != 0) {
                return;
            }
            for (int i = 0; i < 2; ++i) {
                int n5;
                long l;
                int n6;
                int n7;
                byte by = i == 0 ? tile.north : tile.west;
                int n8 = n7 = i == 0 ? ShellMesher.building(sArray, n2, n3 - 1, n4) : ShellMesher.building(sArray, n2 - 1, n3, n4);
                if (by < 0 || n7 == 0 || (n6 = hashMap.merge(l = (long)(n5 = (n7 - 1) * 6 + i * 3 + by) << 32 | (long)n, 1, Integer::sum).intValue()) <= nArray2[n5]) continue;
                nArray[n5] = n6;
                nArray2[n5] = n;
            }
        });
        return nArray;
    }

    static Result mesh(int n8, int n9, int n10, int n11, byte[] byArray, int n12, int n13, FarTiles.Tile[] tileArray, short[][] sArray, int[] nArray, byte[] byArray2, BiomeTrees.Table table, BiomeTrees.Legend legend, byte[][] byArray3, int n14, GroundTiles groundTiles, byte[] byArray4, FarTrees.Thinning thinning) {
        GroundPage groundPage;
        Recipe recipe = new Recipe(0);
        byte[] byArray5 = new byte[8192];
        GroundCapture groundCapture = groundTiles == null ? null : new GroundCapture(groundTiles, n10, n11);
        int n15 = 8;
        int n16 = groundCapture == null ? 0 : 1;
        BiomeTrees.Ground ground = new BiomeTrees.Ground(n10 * 64, n11 * 64, 64);
        ShellMesher.read(byArray, n12, n13, n10 * n15 - n16, n11 * n15 - n16, n15 + 2 * n16, (n4, n5, n6, n7) -> {
            FarTiles.Tile tile = n4 < tileArray.length ? tileArray[n4] : FarTiles.EMPTY;
            int n8 = n5 - n10 * 64;
            int n9 = n6 - n11 * 64;
            if (ground.add(n4, tileArray, n5, n6, n7)) {
                return;
            }
            if (groundCapture != null && n7 == 0 && tile.shell == 2 && ShellMesher.building(sArray, n5, n6, 0) == 0 && groundCapture.add(n4, tile, n5, n6)) {
                return;
            }
            if (n8 < 0 || n9 < 0 || n8 >= 64 || n9 >= 64) {
                return;
            }
            if (tile.shell != 0) {
                ShellMesher.add(recipe, tile, tileArray, n5, n6, n7, sArray, nArray, n14, byArray5, n10, n11, byArray4, thinning);
            }
        });
        ground.grow(table, byArray2, n8, n9, tileArray, legend, byArray3, (tile, n2, n3) -> {
            if (ShellMesher.building(sArray, n2, n3, 0) == 0) {
                ShellMesher.tree(recipe, tile, n2, n3, 0, n14, thinning);
            }
        }, n14 != 0 ? null : (tile, n, n2) -> ShellMesher.plant(recipe, tile, n, n2, 0, sArray, byArray4));
        GroundPage groundPage2 = groundPage = groundCapture == null ? null : groundCapture.build();
        if (groundPage != null) {
            int n17 = n10 * 64;
            int n18 = n11 * 64;
            Recipe.bakedFloor(recipe, n17, n18, n17 + 64, n18 + 64, 0.0f, GROUND_RECT);
        }
        return new Result(Cook.vertices(recipe), ShellMesher.insides(n10, n11, sArray, byArray4), groundPage);
    }

    private static FloatBuffer insides(int n, int n2, short[][] sArray, byte[] byArray) {
        short[] sArray2 = new short[4096];
        boolean bl = false;
        for (int i = 0; i < 64; ++i) {
            block1: for (int j = 0; j < 64; ++j) {
                int n3;
                for (int k = sArray.length - 1; !(k < 0 || (n3 = ShellMesher.building(sArray, n * 64 + j, n2 * 64 + i, k)) != 0 && ShellMesher.open(byArray, n3)); --k) {
                    if (n3 == 0) continue;
                    sArray2[i * 64 + j] = (short)((k + 1) * 8 - 1);
                    bl = true;
                    continue block1;
                }
            }
        }
        return FarMesher.boxes(bl ? sArray2 : new short[1], bl ? 64 : 1, 1, n * 64, n2 * 64, 0.2f, false);
    }

    private static boolean add(Recipe recipe, FarTiles.Tile tile, FarTiles.Tile[] tileArray, int n, int n2, int n3, short[][] sArray, int[] nArray, int n4, byte[] byArray, int n5, int n6, byte[] byArray2, FarTrees.Thinning thinning) {
        float f = (float)n + 0.5f;
        float f2 = (float)n2 + 0.5f;
        float f3 = (float)n3 * 2.4494896f;
        int n7 = ShellMesher.building(sArray, n, n2, n3);
        switch (tile.shell) {
            case 1: {
                boolean bl = tile.north >= 0 && ShellMesher.face(recipe, tile, tileArray, n, n2, n3, sArray, 0, nArray, byArray2);
                boolean bl2 = tile.west >= 0 && ShellMesher.face(recipe, tile, tileArray, n, n2, n3, sArray, 1, nArray, byArray2);
                return bl || bl2;
            }
            case 2: {
                if (n7 != 0 && !ShellMesher.open(byArray2, n7) || n3 == 0 && n4 != 0) {
                    return false;
                }
                int n8 = 0;
                if ((tile.flags & 2) != 0) {
                    int n9;
                    int n10 = n9 = ((n2 - n6 * 64) * 64 + n - n5 * 64) * 2 + (n3 == 0 ? 0 : 1);
                    byte by = byArray[n10];
                    byArray[n10] = (byte)(by + 1);
                    n8 = 4 + 16 * Math.min(by, 3);
                }
                Recipe.place(recipe, tile.page, tile.mesh, tile.map, f, f2, f3, n8);
                return true;
            }
            case 3: {
                return ShellMesher.thing(recipe, tile, n, n2, n3, sArray, n4, byArray2);
            }
            case 5: {
                return n3 != 0 && n4 == 0 && ShellMesher.plant(recipe, tile, n, n2, n3, sArray, byArray2);
            }
            case 4: {
                ShellMesher.tree(recipe, tile, n, n2, n3, n4, thinning);
                return true;
            }
        }
        return false;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private static boolean thing(Recipe recipe, FarTiles.Tile tile, int n, int n2, int n3, short[][] sArray, int n4, byte[] byArray) {
        boolean bl;
        boolean bl2 = (tile.flags & 1) != 0;
        boolean bl3 = bl = tile.north == 1 || tile.west == 1;
        if (n4 != 0 && !bl2 && !bl) {
            return false;
        }
        int n5 = ShellMesher.building(sArray, n, n2, n3);
        if (!(n5 == 0 || n5 < (byArray == null ? 0 : byArray.length) && byArray[n5] == 2 || tile.north == 1 && ShellMesher.building(sArray, n, n2 - 1, n3) == 0)) {
            if (tile.west != 1) return false;
            if (ShellMesher.building(sArray, n - 1, n2, n3) != 0) return false;
        }
        boolean bl4 = true;
        boolean bl5 = bl4;
        if (!bl5) return bl5;
        Recipe.place(recipe, tile.page, tile.mesh, tile.map, (float)n + 0.5f, (float)n2 + 0.5f, (float)n3 * 2.4494896f, 0);
        return bl5;
    }

    private static boolean face(Recipe recipe, FarTiles.Tile tile, FarTiles.Tile[] tileArray, int n, int n2, int n3, short[][] sArray, int n4, int[] nArray, byte[] byArray) {
        float f;
        boolean bl;
        float f2 = (float)n + 0.5f;
        float f3 = (float)n2 + 0.5f;
        float f4 = (float)n3 * 2.4494896f;
        int n5 = ShellMesher.building(sArray, n, n2, n3);
        int n6 = n4 == 0 ? ShellMesher.building(sArray, n, n2 - 1, n3) : ShellMesher.building(sArray, n - 1, n2, n3);
        boolean bl2 = n5 == 0 || ShellMesher.open(byArray, n5);
        boolean bl3 = bl = n6 == 0 || ShellMesher.open(byArray, n6);
        if (!bl2 && !bl) {
            return false;
        }
        float f5 = n4 == 0 ? 0.0f : 1.0f;
        float f6 = f = n4 == 0 ? 1.0f : 0.0f;
        if (bl2) {
            Recipe.placeFace(recipe, tile.page, tile.mesh, tile.map, f2, f3, f4, f5, f, 0, 0.0f);
        }
        if (bl) {
            FarTiles.Tile tile2;
            if (n5 == 0 || n6 != 0) {
                Recipe.placeFace(recipe, tile.page, tile.mesh, tile.map, f2, f3, f4, f5, f, 1, 0.0f);
                return true;
            }
            byte by = n4 == 0 ? tile.north : tile.west;
            int n7 = nArray == null || by < 0 ? -1 : nArray[(n5 - 1) * 6 + n4 * 3 + by];
            FarTiles.Tile tile3 = tile2 = n7 >= 0 && n7 < tileArray.length ? tileArray[n7] : null;
            if (tile2 != null && tile2.shell == 1) {
                Recipe.placeFace(recipe, tile2.page, tile2.mesh, tile2.map, f2, f3, f4, f5, f, 1, 0.0f);
            } else {
                Recipe.placeFace(recipe, tile.page, tile.mesh, tile.map, f2, f3, f4, f5, f, 1, (float)tile.colour + 1.0f);
            }
        }
        return true;
    }

    private static boolean plant(Recipe recipe, FarTiles.Tile tile, int n, int n2, int n3, short[][] sArray, byte[] byArray) {
        int n4 = ShellMesher.building(sArray, n, n2, n3);
        if (n4 != 0 && (n4 >= (byArray == null ? 0 : byArray.length) || byArray[n4] != 2)) {
            return false;
        }
        float[] fArray = tile.card;
        int n5 = (tile.flags & 0x20) != 0 ? 2 : 0;
        Recipe.card(recipe, tile.page, (float)n + 0.5f, (float)n2 + 0.5f, (float)n3 * 2.4494896f, fArray[0], fArray[1], fArray[2], fArray[3], tile.map, 0x200 | n5);
        return true;
    }

    private static void tree(Recipe recipe, FarTiles.Tile tile, int n, int n2, int n3, int n4, FarTrees.Thinning thinning) {
        if (!thinning.keeps(n, n2)) {
            return;
        }
        float f = (float)n + 0.5f;
        float f2 = (float)n2 + 0.5f;
        float f3 = (float)n3 * 2.4494896f;
        int n5 = VertexFormat.rooted(n, n2);
        TreeAtlas.Slot slot = tile.billboard;
        if (n4 == 1 && slot != null) {
            float f4 = thinning.grow();
            Recipe.billboard(recipe, tile.page, f, f2, f3, slot.halfWidth * f4, slot.top * f4, slot.rect, n5);
            return;
        }
        Recipe.place(recipe, tile.page, tile.mesh, tile.map, f, f2, f3, n5);
        FarTiles.Tile tile2 = tile.leaves;
        if (tile2 != null) {
            Recipe.place(recipe, tile2.page, tile2.mesh, tile2.map, f, f2, f3, n5 | 2);
        }
    }

    private static boolean open(byte[] byArray, int n) {
        return byArray != null && n < byArray.length && byArray[n] != 0;
    }

    private static int building(short[][] sArray, int n, int n2, int n3) {
        if (n < 0 || n2 < 0 || n >= 256 || n2 >= 256 || n3 < 0 || n3 >= sArray.length || sArray[n3] == null) {
            return 0;
        }
        return sArray[n3][n2 * 256 + n];
    }

    static void read(byte[] byArray, int n, int n2, int n3, int n4, int n5, Visit visit) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(byArray).order(ByteOrder.LITTLE_ENDIAN);
        int n6 = byArray.length >= 8 && byArray[0] == 76 && byArray[1] == 79 && byArray[2] == 84 && byArray[3] == 80 ? byteBuffer.getInt(4) : 0;
        int n7 = (n6 >= 1 ? 8 : 0) + 4;
        int n8 = Math.max(n, -32);
        int n9 = Math.min(n2, 31);
        for (int i = Math.max(0, n3); i < Math.min(32, n3 + n5); ++i) {
            for (int j = Math.max(0, n4); j < Math.min(32, n4 + n5); ++j) {
                byteBuffer.position(byteBuffer.getInt(n7 + (i * 32 + j) * 8));
                int n10 = 0;
                for (int k = n8; k <= n9; ++k) {
                    for (int i2 = 0; i2 < 8; ++i2) {
                        for (int i3 = 0; i3 < 8; ++i3) {
                            if (n10 > 0) {
                                --n10;
                                continue;
                            }
                            int n11 = byteBuffer.getInt();
                            if (n11 == -1 && (n10 = byteBuffer.getInt()) > 0) {
                                --n10;
                                continue;
                            }
                            if (n11 <= 1) continue;
                            byteBuffer.getInt();
                            for (int i4 = 1; i4 < n11; ++i4) {
                                int n12 = byteBuffer.getInt();
                                if (k < 0 || n12 < 0) continue;
                                visit.tile(n12, i * 8 + i2, j * 8 + i3, k);
                            }
                        }
                    }
                }
            }
        }
    }

    static interface Visit {
        public void tile(int var1, int var2, int var3, int var4);
    }

    record Result(Cook.Cooked shell, FloatBuffer inside, GroundPage ground) {
    }
}

