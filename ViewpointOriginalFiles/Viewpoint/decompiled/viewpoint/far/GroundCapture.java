/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.util.Arrays;
import viewpoint.far.FarTiles;
import viewpoint.render.GroundArt;
import viewpoint.render.GroundPage;
import viewpoint.render.GroundTiles;

final class GroundCapture {
    private static final int SIDE = 80;
    private final GroundTiles tiles;
    private final int x0;
    private final int y0;
    private final int[] head = new int[6400];
    private final int[] tail = new int[6400];
    private final boolean[] based = new boolean[6400];
    private int[] next = new int[4096];
    private int[] value = new int[4096];
    private int count;

    GroundCapture(GroundTiles groundTiles, int n, int n2) {
        this.tiles = groundTiles;
        this.x0 = n * 64 - 8;
        this.y0 = n2 * 64 - 8;
        Arrays.fill(this.head, -1);
    }

    boolean add(int n, FarTiles.Tile tile, int n2, int n3) {
        int n4 = n2 - this.x0;
        int n5 = n3 - this.y0;
        if (!this.tiles.paints(n) || n4 < 0 || n5 < 0 || n4 >= 80 || n5 >= 80) {
            return false;
        }
        int n6 = n5 * 80 + n4;
        boolean bl = !this.based[n6] && (tile.flags & 2) != 0 && (tile.flags & 4) == 0;
        int n7 = n6;
        this.based[n7] = this.based[n7] | bl;
        if (this.count == this.value.length) {
            this.next = Arrays.copyOf(this.next, this.count * 2);
            this.value = Arrays.copyOf(this.value, this.count * 2);
        }
        this.value[this.count] = n << 1 | (bl ? 1 : 0);
        this.next[this.count] = -1;
        if (this.head[n6] < 0) {
            this.head[n6] = this.count;
        } else {
            this.next[this.tail[n6]] = this.count;
        }
        ++this.count;
        return true;
    }

    GroundPage build() {
        GroundArt[] groundArtArray = new GroundArt[100];
        GroundArt.Builder builder = new GroundArt.Builder();
        boolean bl = false;
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                int n;
                builder.begin(this.tiles);
                for (int k = 0; k < 8; ++k) {
                    for (n = 0; n < 8; ++n) {
                        int n2 = (i * 8 + k) * 80 + j * 8 + n;
                        int n3 = this.head[n2];
                        while (n3 >= 0) {
                            builder.layer(k * 8 + n, this.value[n3] >> 1, (this.value[n3] & 1) != 0);
                            n3 = this.next[n3];
                        }
                    }
                }
                GroundArt groundArt = builder.build();
                groundArtArray[i * 10 + j] = groundArt;
                GroundArt groundArt2 = groundArt;
                n = j > 0 && i > 0 && j < 9 && i < 9 ? 1 : 0;
                bl |= n != 0 && groundArt2 != null;
            }
        }
        return bl ? new GroundPage(groundArtArray) : null;
    }
}

