/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.core.textures.TextureID;

public final class TreeAtlas {
    static final int SIZE = 2048;
    static final int CELL_WIDTH = 64;
    static final int CELL_HEIGHT = 128;
    static final int COLUMNS = 32;
    static final int CELLS = 512;
    static final int MIPS = 7;
    public static final long BYTES = 0x1555555L;
    private static final HashMap<Key, Slot> slots = new HashMap();
    private static boolean made;
    private static boolean full;
    static final ConcurrentLinkedQueue<Slot> PENDING;

    public static Slot slot(TextureID textureID, float[] fArray, TextureID textureID2, float[] fArray2, float[] fArray3) {
        Key key = new Key(textureID, fArray[0], fArray[1], textureID2, fArray2 == null ? 0.0f : fArray2[0], fArray2 == null ? 0.0f : fArray2[1]);
        Slot slot = slots.get(key);
        if (slot != null || full) {
            return slot;
        }
        if (slots.size() == 512) {
            full = true;
            System.out.println("[Viewpoint] tree atlas full at 512 trees: the rest keep their cards");
            return null;
        }
        slot = new Slot(slots.size(), textureID, fArray, textureID2, fArray2, fArray3);
        slots.put(key, slot);
        made = true;
        PENDING.add(slot);
        return slot;
    }

    public static void reset() {
        slots.clear();
        full = false;
    }

    public static long bytes() {
        return made ? 0x1555555L : 0L;
    }

    private TreeAtlas() {
    }

    static {
        PENDING = new ConcurrentLinkedQueue();
    }

    private record Key(TextureID trunk, float trunkU, float trunkV, TextureID leaves, float leavesU, float leavesV) {
    }

    public static final class Slot {
        public final float[] rect;
        public final float halfWidth;
        public final float top;
        final int index;
        final TextureID trunkPage;
        final TextureID leavesPage;
        final float[] trunkMap;
        final float[] leavesMap;
        final float width;
        final float height;

        Slot(int n, TextureID textureID, float[] fArray, TextureID textureID2, float[] fArray2, float[] fArray3) {
            this.index = n;
            this.trunkPage = textureID;
            this.trunkMap = fArray;
            this.leavesPage = textureID2;
            this.leavesMap = fArray2;
            this.width = fArray3[0];
            this.height = fArray3[1];
            this.halfWidth = fArray3[2];
            this.top = fArray3[3];
            float f = n % 32 * 64;
            float f2 = n / 32 * 128;
            this.rect = new float[]{(f + 0.5f) / 2048.0f, (f2 + 0.5f) / 2048.0f, (f + 64.0f - 0.5f) / 2048.0f, (f2 + 128.0f - 0.5f) / 2048.0f};
        }

        int x() {
            return this.index % 32 * 64;
        }

        int y() {
            return this.index / 32 * 128;
        }
    }
}

