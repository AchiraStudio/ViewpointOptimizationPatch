/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import zombie.core.textures.TextureID;

public final class GroundTiles {
    private final TextureID[] atlases;
    private final float[] numbers;

    public GroundTiles(TextureID[] textureIDArray, float[] fArray) {
        this.atlases = textureIDArray;
        this.numbers = fArray;
    }

    public long bytes() {
        return 32L + (long)this.atlases.length * 8L + (long)this.numbers.length * 4L;
    }

    public boolean paints(int n) {
        return n >= 0 && n < this.atlases.length && this.atlases[n] != null;
    }

    TextureID atlas(int n) {
        return this.atlases[n];
    }

    float number(int n, int n2) {
        return this.numbers[n * 14 + n2];
    }
}

