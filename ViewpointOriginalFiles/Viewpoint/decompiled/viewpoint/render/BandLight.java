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
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL45;
import viewpoint.platform.LongMap;
import viewpoint.platform.Pool;
import viewpoint.render.MeshArena;

public final class BandLight {
    private static final Change CLEAR = new Change(0, 0, 0, null);
    private static final ConcurrentLinkedQueue<Change> CHANGES = new ConcurrentLinkedQueue();
    private static final int WINDOW = 72;
    private static final int LEVELS = 32;
    private static final int RETRIES = 64;
    private static final LongMap<int[]> slots = new LongMap(4096);
    private static final LongMap<Change> waiting = new LongMap(256);
    private static final int[] table = new int[165888];
    private static final boolean[] dirty = new boolean[32];
    private static final IntBuffer slice = BufferUtils.createIntBuffer((int)5184);
    private static final ByteBuffer upload = BufferUtils.createByteBuffer((int)3200);
    private static int texture;
    private static volatile int shown;
    private static volatile int turnedAway;

    public static void show(int n, int n2, int n3, byte[] byArray) {
        CHANGES.add(new Change(n, n2, n3, byArray));
    }

    public static void hide(int n, int n2, int n3) {
        CHANGES.add(new Change(n, n2, n3, null));
    }

    public static void clear() {
        CHANGES.add(CLEAR);
    }

    public static int grids() {
        return shown;
    }

    public static int turnedAway() {
        return turnedAway;
    }

    public static long share() {
        return Pool.LIGHT_GRIDS.cap() / 3200L / 3L;
    }

    static void update() {
        Change change;
        while ((change = CHANGES.poll()) != null) {
            if (change == CLEAR) {
                BandLight.letGoAll();
                continue;
            }
            if (change.grid == null) {
                BandLight.letGo(change);
                continue;
            }
            BandLight.show(change);
        }
        BandLight.retry();
        if (texture == 0) {
            texture = GL45.glCreateTextures((int)32879);
            GL45.glTextureParameteri((int)texture, (int)10241, (int)9728);
            GL45.glTextureParameteri((int)texture, (int)10240, (int)9728);
            GL45.glTextureStorage3D((int)texture, (int)1, (int)33333, (int)72, (int)72, (int)32);
            Arrays.fill(dirty, true);
        }
        shown = slots.size();
        turnedAway = waiting.size();
        for (int i = 0; i < 32; ++i) {
            if (!dirty[i]) continue;
            BandLight.dirty[i] = false;
            slice.clear();
            slice.put(table, i * 72 * 72, 5184).flip();
            GL45.glTextureSubImage3D((int)texture, (int)0, (int)0, (int)0, (int)i, (int)72, (int)72, (int)1, (int)36244, (int)5124, (IntBuffer)slice);
        }
    }

    static void bind(int n) {
        GL13.glActiveTexture((int)(33984 + n));
        GL11.glBindTexture((int)32879, (int)texture);
        GL13.glActiveTexture((int)33984);
    }

    static void unbind(int n) {
        GL13.glActiveTexture((int)(33984 + n));
        GL11.glBindTexture((int)32879, (int)0);
        GL13.glActiveTexture((int)33984);
    }

    private static void show(Change change) {
        if (change.level < 0 || change.level >= 32) {
            return;
        }
        long l = BandLight.key(change.chunkX, change.chunkY, change.level);
        int[] nArray = slots.get(l);
        if (nArray == null) {
            int n;
            int n2 = n = (long)slots.size() < BandLight.share() ? MeshArena.takeSlot() : -1;
            if (n < 0) {
                waiting.put(l, change);
                return;
            }
            nArray = new int[]{n};
            slots.put(l, nArray);
            BandLight.setTexel(change, n + 1);
        }
        upload.clear();
        upload.put(change.grid).flip();
        MeshArena.uploadLight(nArray[0], upload);
    }

    private static void letGo(Change change) {
        if (change.level < 0 || change.level >= 32) {
            return;
        }
        long l = BandLight.key(change.chunkX, change.chunkY, change.level);
        int[] nArray = slots.remove(l);
        waiting.remove(l);
        if (nArray != null) {
            MeshArena.freeSlot(nArray[0]);
            BandLight.setTexel(change, 0);
        }
    }

    private static void letGoAll() {
        for (int[] nArray : slots.values()) {
            MeshArena.freeSlot(nArray[0]);
        }
        slots.clear();
        waiting.clear();
        Arrays.fill(table, 0);
        Arrays.fill(dirty, true);
    }

    private static void retry() {
        int n = 0;
        for (int i = 0; i < waiting.capacity() && n < 64 && (long)slots.size() < BandLight.share(); ++i) {
            Change change = waiting.valueAt(i);
            if (change == null) continue;
            waiting.removeAt(i);
            ++n;
            BandLight.show(change);
        }
    }

    private static void setTexel(Change change, int n) {
        int n2 = Math.floorMod(change.chunkX, 72);
        int n3 = Math.floorMod(change.chunkY, 72);
        BandLight.table[(change.level * 72 + n3) * 72 + n2] = n;
        BandLight.dirty[change.level] = true;
    }

    private static long key(int n, int n2, int n3) {
        return (long)(n & 0xFFFFFF) << 40 | (long)(n2 & 0xFFFFFF) << 16 | (long)(n3 + 32 & 0xFFFF);
    }

    private BandLight() {
    }

    private record Change(int chunkX, int chunkY, int level, byte[] grid) {
    }
}

