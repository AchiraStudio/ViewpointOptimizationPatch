/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import java.util.Arrays;
import viewpoint.render.FloorLayers;
import viewpoint.render.GroundTiles;
import zombie.core.textures.TextureID;

public final class GroundArt
implements FloorLayers {
    private final GroundTiles tiles;
    private final int[] starts;
    private final int[] layers;

    private GroundArt(GroundTiles groundTiles, int[] nArray, int[] nArray2) {
        this.tiles = groundTiles;
        this.starts = nArray;
        this.layers = nArray2;
    }

    @Override
    public int first(int n) {
        return this.starts[n];
    }

    @Override
    public int end(int n) {
        return this.starts[n + 1];
    }

    @Override
    public TextureID atlas(int n) {
        return this.tiles.atlas(this.layers[n] >> 1);
    }

    @Override
    public float number(int n, int n2) {
        return this.tiles.number(this.layers[n] >> 1, n2);
    }

    @Override
    public boolean has(int n, int n2) {
        return n2 == 2 && (this.layers[n] & 1) != 0;
    }

    public long bytes() {
        return 32L + (long)this.starts.length * 4L + (long)this.layers.length * 4L;
    }

    public static final class Builder {
        private final int[] starts = new int[65];
        private int[] layers = new int[256];
        private int square;
        private int count;
        private GroundTiles tiles;

        public void begin(GroundTiles groundTiles) {
            this.tiles = groundTiles;
            this.square = 0;
            this.count = 0;
            this.starts[0] = 0;
        }

        public void layer(int n, int n2, boolean bl) {
            while (this.square < n) {
                this.starts[++this.square] = this.count;
            }
            if (this.count == this.layers.length) {
                this.layers = Arrays.copyOf(this.layers, this.count * 2);
            }
            this.layers[this.count++] = n2 << 1 | (bl ? 1 : 0);
            this.starts[this.square + 1] = this.count;
        }

        public GroundArt build() {
            while (this.square < 64) {
                this.starts[++this.square] = this.count;
            }
            return this.count == 0 ? null : new GroundArt(this.tiles, (int[])this.starts.clone(), Arrays.copyOf(this.layers, this.count));
        }
    }
}

