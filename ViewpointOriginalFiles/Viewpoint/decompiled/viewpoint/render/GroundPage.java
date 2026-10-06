/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import viewpoint.render.FloorBakeSource;
import viewpoint.render.FloorLayers;
import viewpoint.render.GroundArt;

public final class GroundPage
implements FloorBakeSource {
    public static final int CHUNKS = 8;
    public static final int TILES = 66;
    public static final int SPAN = 10;
    private final GroundArt[] arts;

    public GroundPage(GroundArt[] groundArtArray) {
        this.arts = (GroundArt[])groundArtArray.clone();
    }

    @Override
    public int tiles() {
        return 66;
    }

    @Override
    public FloorLayers artAt(int n, int n2) {
        int n3 = Math.floorDiv(n - 1, 8) + 1;
        int n4 = Math.floorDiv(n2 - 1, 8) + 1;
        return this.arts[n4 * 10 + n3];
    }

    public long bytes() {
        long l = 16L + (long)this.arts.length * 8L;
        for (GroundArt groundArt : this.arts) {
            l += groundArt == null ? 0L : groundArt.bytes();
        }
        return l;
    }
}

