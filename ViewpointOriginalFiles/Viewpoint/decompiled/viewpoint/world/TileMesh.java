/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.world;

import viewpoint.world.Recipe;

public final class TileMesh {
    public static final int STRIDE = 8;
    static final TileMesh EMPTY = new TileMesh(new float[0]);
    public final float[] data;
    public final int vertCount;
    final long hash;

    TileMesh(float[] fArray) {
        this.data = fArray;
        this.vertCount = fArray.length / 8;
        this.hash = Recipe.mix(-7046029254386353131L, fArray);
    }
}

