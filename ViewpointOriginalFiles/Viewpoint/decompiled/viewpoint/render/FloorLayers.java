/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import zombie.core.textures.TextureID;

public interface FloorLayers {
    public int first(int var1);

    public int end(int var1);

    public TextureID atlas(int var1);

    public float number(int var1, int var2);

    public boolean has(int var1, int var2);
}

