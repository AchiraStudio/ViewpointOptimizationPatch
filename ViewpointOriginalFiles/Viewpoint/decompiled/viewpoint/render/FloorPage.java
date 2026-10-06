/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import viewpoint.render.FloorArt;
import viewpoint.render.FloorBakeSource;

public final class FloorPage
implements FloorBakeSource {
    public static final int GUTTER = 1;
    public static final int TILES = 10;
    public final int wx;
    public final int wy;
    public final int level;
    private final FloorArt own;
    private final FloorArt[] neighbours;
    public static final int[] NEIGHBOURS = new int[]{-1, -1, 0, -1, 1, -1, -1, 0, 1, 0, -1, 1, 0, 1, 1, 1};

    public FloorPage(int n, int n2, int n3, FloorArt floorArt, FloorArt[] floorArtArray) {
        this.wx = n;
        this.wy = n2;
        this.level = n3;
        this.own = floorArt;
        this.neighbours = (FloorArt[])floorArtArray.clone();
    }

    public FloorArt own() {
        return this.own;
    }

    public long key() {
        return FloorPage.key(this.wx, this.wy, this.level);
    }

    public static long key(int n, int n2, int n3) {
        return (long)(n & 0xFFFFFF) << 40 | (long)(n2 & 0xFFFFFF) << 16 | (long)(n3 + 32 & 0xFFFF);
    }

    @Override
    public int tiles() {
        return 10;
    }

    @Override
    public FloorArt artAt(int n, int n2) {
        int n3;
        int n4;
        int n5 = n < 1 ? -1 : (n4 = n >= 9 ? 1 : 0);
        int n6 = n2 < 1 ? -1 : (n3 = n2 >= 9 ? 1 : 0);
        if (n4 == 0 && n3 == 0) {
            return this.own;
        }
        for (int i = 0; i < NEIGHBOURS.length / 2; ++i) {
            if (NEIGHBOURS[i * 2] != n4 || NEIGHBOURS[i * 2 + 1] != n3) continue;
            return this.neighbours[i];
        }
        return null;
    }

    public static int squareAt(int n, int n2) {
        int n3 = Math.floorMod(n - 1, 8);
        int n4 = Math.floorMod(n2 - 1, 8);
        return n4 * 8 + n3;
    }
}

