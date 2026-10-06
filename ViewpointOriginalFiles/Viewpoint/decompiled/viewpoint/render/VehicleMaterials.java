/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.textures.Texture
 */
package viewpoint.render;

import java.util.Arrays;
import zombie.core.textures.Texture;

public final class VehicleMaterials {
    public static final int SINGLE_UV = 0;
    public static final int MULTI_UV = 1;
    public static final int NO_PAINT = 2;
    public static final int TEXTURES = 7;
    public static final int MATRICES = 12;
    static final int VALUES = 197;
    static final int RUST = 192;
    static final int PAINT = 193;
    static final int KIND = 196;
    Texture[] textures = new Texture[112];
    float[] values = new float[3152];
    int count;

    void reset() {
        Arrays.fill(this.textures, 0, this.count * 7, null);
        this.count = 0;
    }

    public int add(Texture[] textureArray, float[][] fArray, float f, float f2, float f3, float f4, int n) {
        if (this.count * 197 == this.values.length) {
            this.textures = Arrays.copyOf(this.textures, this.count * 2 * 7);
            this.values = Arrays.copyOf(this.values, this.count * 2 * 197);
        }
        int n2 = this.count++;
        System.arraycopy(textureArray, 0, this.textures, n2 * 7, 7);
        for (int i = 0; i < 12; ++i) {
            System.arraycopy(fArray[i], 0, this.values, n2 * 197 + i * 16, 16);
        }
        this.values[n2 * 197 + 192] = f;
        this.values[n2 * 197 + 193] = f2;
        this.values[n2 * 197 + 193 + 1] = f3;
        this.values[n2 * 197 + 193 + 2] = f4;
        this.values[n2 * 197 + 196] = n;
        return n2;
    }

    public int count() {
        return this.count;
    }
}

