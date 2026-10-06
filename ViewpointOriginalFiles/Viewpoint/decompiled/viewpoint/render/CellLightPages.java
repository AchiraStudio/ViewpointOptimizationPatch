/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL45
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL45;
import viewpoint.platform.LongMap;

public final class CellLightPages {
    private static final Change CLEAR = new Change(0, 0, null, null);
    private static final ConcurrentLinkedQueue<Change> CHANGES = new ConcurrentLinkedQueue();
    private static final int WINDOW = 16;
    private static final int LAYERS = 96;
    private static final int TEXELS = 128;
    private static final LongMap<int[]> layers = new LongMap(128);
    private static final LongMap<int[]> cards = new LongMap(128);
    private static final ArrayDeque<Integer> freeLayers = new ArrayDeque();
    private static final int[] table = new int[256];
    private static final IntBuffer tableUpload = BufferUtils.createIntBuffer((int)256);
    private static final ByteBuffer upload = BufferUtils.createByteBuffer((int)65536);
    private static int pages;
    private static int tableTexture;
    private static int nextLayer;
    private static int cardsChanged;
    private static boolean tableDirty;

    public static void show(int n, int n2, byte[] byArray, int[] nArray) {
        CHANGES.add(new Change(n, n2, byArray, nArray));
    }

    public static void hide(int n, int n2) {
        CHANGES.add(new Change(n, n2, null, null));
    }

    public static void clear() {
        CHANGES.add(CLEAR);
    }

    static void update() {
        Change change;
        if (pages == 0) {
            pages = GL45.glCreateTextures((int)35866);
            GL45.glTextureParameteri((int)pages, (int)10241, (int)9729);
            GL45.glTextureParameteri((int)pages, (int)10240, (int)9729);
            GL45.glTextureParameteri((int)pages, (int)10242, (int)33071);
            GL45.glTextureParameteri((int)pages, (int)10243, (int)33071);
            GL45.glTextureStorage3D((int)pages, (int)1, (int)32856, (int)128, (int)128, (int)96);
            tableTexture = GL45.glCreateTextures((int)3553);
            GL45.glTextureParameteri((int)tableTexture, (int)10241, (int)9728);
            GL45.glTextureParameteri((int)tableTexture, (int)10240, (int)9728);
            GL45.glTextureStorage2D((int)tableTexture, (int)1, (int)33333, (int)16, (int)16);
        }
        while ((change = CHANGES.poll()) != null) {
            if (change == CLEAR) {
                CellLightPages.letGoAll();
                continue;
            }
            if (change.page == null) {
                CellLightPages.letGo(change.cellX, change.cellY);
                continue;
            }
            CellLightPages.show(change);
        }
        if (tableDirty) {
            tableDirty = false;
            tableUpload.clear();
            tableUpload.put(table).flip();
            GL45.glTextureSubImage2D((int)tableTexture, (int)0, (int)0, (int)0, (int)16, (int)16, (int)36244, (int)5124, (IntBuffer)tableUpload);
        }
    }

    static void bind(int n, int n2) {
        GL13.glActiveTexture((int)(33984 + n));
        GL11.glBindTexture((int)35866, (int)pages);
        GL13.glActiveTexture((int)(33984 + n2));
        GL11.glBindTexture((int)3553, (int)tableTexture);
        GL13.glActiveTexture((int)33984);
    }

    static void unbind(int n, int n2) {
        GL13.glActiveTexture((int)(33984 + n));
        GL11.glBindTexture((int)35866, (int)0);
        GL13.glActiveTexture((int)(33984 + n2));
        GL11.glBindTexture((int)3553, (int)0);
        GL13.glActiveTexture((int)33984);
    }

    static int cardsChanged() {
        return cardsChanged;
    }

    static void cards(IntBuffer intBuffer) {
        for (int[] nArray : cards.values()) {
            intBuffer.put(nArray, 0, Math.min(nArray.length, intBuffer.remaining()));
        }
    }

    private static void show(Change change) {
        long l = (long)change.cellX << 32 | (long)change.cellY & 0xFFFFFFFFL;
        cards.put(l, change.cards);
        ++cardsChanged;
        int[] nArray = layers.get(l);
        if (nArray == null) {
            int n;
            if (!freeLayers.isEmpty()) {
                v0 = freeLayers.pop();
            } else if (nextLayer < 96) {
                int n2 = nextLayer;
                v0 = n2;
                nextLayer = n2 + 1;
            } else {
                v0 = n = -1;
            }
            if (n < 0) {
                return;
            }
            nArray = new int[]{n};
            layers.put(l, nArray);
            CellLightPages.setTexel(change.cellX, change.cellY, n + 1);
        }
        upload.clear();
        upload.put(change.page).flip();
        GL11.glPixelStorei((int)3317, (int)4);
        GL45.glTextureSubImage3D((int)pages, (int)0, (int)0, (int)0, (int)nArray[0], (int)128, (int)128, (int)1, (int)6408, (int)5121, (ByteBuffer)upload);
    }

    private static void letGo(int n, int n2) {
        long l = (long)n << 32 | (long)n2 & 0xFFFFFFFFL;
        int[] nArray = layers.remove(l);
        cardsChanged += cards.remove(l) != null ? 1 : 0;
        if (nArray != null) {
            freeLayers.push(nArray[0]);
            CellLightPages.setTexel(n, n2, 0);
        }
    }

    private static void letGoAll() {
        for (int[] nArray : layers.values()) {
            freeLayers.push(nArray[0]);
        }
        layers.clear();
        cards.clear();
        ++cardsChanged;
        Arrays.fill(table, 0);
        tableDirty = true;
    }

    private static void setTexel(int n, int n2, int n3) {
        CellLightPages.table[Math.floorMod((int)n2, (int)16) * 16 + Math.floorMod((int)n, (int)16)] = n3;
        tableDirty = true;
    }

    private CellLightPages() {
    }

    static {
        tableDirty = true;
    }

    private record Change(int cellX, int cellY, byte[] page, int[] cards) {
    }
}

