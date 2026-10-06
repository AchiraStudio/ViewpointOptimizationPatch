/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 *  zombie.core.textures.Texture
 */
package viewpoint.render;

import java.util.Arrays;
import viewpoint.render.FloorLayers;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;

public final class FloorArt
implements FloorLayers {
    public static final int SIZE = 8;
    public static final int SQUARES = 64;
    public static final int U0 = 0;
    public static final int V0 = 1;
    public static final int U1 = 2;
    public static final int V1 = 3;
    public static final int WIDTH = 4;
    public static final int HEIGHT = 5;
    public static final int LEFT = 6;
    public static final int TOP = 7;
    public static final int SPAN_X = 8;
    public static final int SPAN_Y = 9;
    public static final int R = 10;
    public static final int G = 11;
    public static final int B = 12;
    public static final int ALPHA = 13;
    public static final int NUMBERS = 14;
    public static final int FLIP = 1;
    public static final int OPAQUE = 2;
    private static final float ISO_HALF_WIDTH = 32.0f;
    private static final float ISO_HALF_HEIGHT = 16.0f;
    private final int[] starts;
    private final TextureID[] atlases;
    private final float[] numbers;
    private final byte[] flags;

    public static void numbers(float[] fArray, Texture texture, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        float f9 = 32.0f * (float)Core.tileScale;
        float f10 = 16.0f * (float)Core.tileScale;
        fArray[0] = texture.getXStart();
        fArray[1] = texture.getYStart();
        fArray[2] = texture.getXEnd();
        fArray[3] = texture.getYEnd();
        fArray[4] = texture.getWidth();
        fArray[5] = texture.getHeight();
        fArray[6] = f / f9;
        fArray[7] = f2 / f10;
        fArray[8] = (float)texture.getWidth() * f3 / f9;
        fArray[9] = (float)texture.getHeight() * f4 / f10;
        fArray[10] = f5;
        fArray[11] = f6;
        fArray[12] = f7;
        fArray[13] = f8;
    }

    private FloorArt(int[] nArray, TextureID[] textureIDArray, float[] fArray, byte[] byArray) {
        this.starts = nArray;
        this.atlases = textureIDArray;
        this.numbers = fArray;
        this.flags = byArray;
    }

    @Override
    public int first(int n) {
        return this.starts[n];
    }

    @Override
    public int end(int n) {
        return this.starts[n + 1];
    }

    public boolean present(int n) {
        return this.starts[n + 1] > this.starts[n];
    }

    public int layers() {
        return this.starts[64];
    }

    @Override
    public TextureID atlas(int n) {
        return this.atlases[n];
    }

    @Override
    public float number(int n, int n2) {
        return this.numbers[n * 14 + n2];
    }

    @Override
    public boolean has(int n, int n2) {
        return (this.flags[n] & n2) != 0;
    }

    public boolean sameArt(FloorArt floorArt) {
        if (floorArt == this) {
            return true;
        }
        if (floorArt == null || !Arrays.equals(this.starts, floorArt.starts) || !Arrays.equals(this.flags, floorArt.flags)) {
            return false;
        }
        for (int i = 0; i < this.atlases.length; ++i) {
            if (this.atlases[i] == floorArt.atlases[i]) continue;
            return false;
        }
        return Arrays.equals(this.numbers, floorArt.numbers);
    }

    public long bytes() {
        return 64L + (long)this.starts.length * 4L + (long)this.atlases.length * 8L + (long)this.numbers.length * 4L + (long)this.flags.length;
    }

    public static final class Builder {
        private final int[] starts = new int[65];
        private TextureID[] atlases = new TextureID[256];
        private float[] numbers = new float[3584];
        private byte[] flags = new byte[256];
        private int square;
        private int layers;

        public void begin() {
            this.square = 0;
            this.layers = 0;
            this.starts[0] = 0;
        }

        public void square(int n) {
            while (this.square < n) {
                this.starts[++this.square] = this.layers;
            }
        }

        public void layer(TextureID textureID, float[] fArray, int n) {
            if (this.layers == this.atlases.length) {
                this.atlases = Arrays.copyOf(this.atlases, this.layers * 2);
                this.numbers = Arrays.copyOf(this.numbers, this.layers * 2 * 14);
                this.flags = Arrays.copyOf(this.flags, this.layers * 2);
            }
            this.atlases[this.layers] = textureID;
            System.arraycopy(fArray, 0, this.numbers, this.layers * 14, 14);
            this.flags[this.layers] = (byte)n;
            this.starts[this.square + 1] = ++this.layers;
        }

        public FloorArt build() {
            this.square(64);
            if (this.layers == 0) {
                return null;
            }
            return new FloorArt((int[])this.starts.clone(), Arrays.copyOf(this.atlases, this.layers), Arrays.copyOf(this.numbers, this.layers * 14), Arrays.copyOf(this.flags, this.layers));
        }
    }
}

