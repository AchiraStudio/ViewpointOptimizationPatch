/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL31
 *  org.lwjgl.opengl.GL44
 */
package viewpoint.render;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL44;
import viewpoint.platform.Pool;
import viewpoint.render.PackArena;
import viewpoint.render.PackArt;
import viewpoint.render.PackData;
import viewpoint.render.PackModel;
import viewpoint.render.Retirement;

public final class PackModels {
    static final int TABLE_TEXELS = 2;
    private static final long UPLOAD_BYTES = 0x400000L;
    private static final long KEEP_NANOS = 10000000000L;
    private static final int ROOM_CHECK_FRAMES = 30;
    private static final HashMap<File, PackModel> byFile = new HashMap();
    private static final ArrayList<PackModel> made = new ArrayList();
    private static volatile PackModel[] byIndex = new PackModel[0];
    static final ConcurrentLinkedQueue<PackModel> READ = new ConcurrentLinkedQueue();
    static final ConcurrentLinkedQueue<PackModel> SETTLED = new ConcurrentLinkedQueue();
    private static final AtomicInteger onTheirWay = new AtomicInteger();
    private static volatile boolean full;
    private static volatile int roomAgain;
    private static final ArrayList<PackModel> resident;
    private static int tableBuffer;
    private static int tableTexture;
    private static int tableModels;
    private static int turnedAway;
    private static int fullFrames;
    private static final FloatBuffer entry;

    public static PackModel of(File file) {
        PackModel packModel = byFile.get(file);
        if (packModel == null) {
            packModel = new PackModel(made.size(), file);
            byFile.put(file, packModel);
            made.add(packModel);
        }
        return packModel;
    }

    public static void publish() {
        if (made.size() != byIndex.length) {
            byIndex = made.toArray(new PackModel[0]);
        }
    }

    public static PackModel get(int n) {
        PackModel[] packModelArray = byIndex;
        return n >= 0 && n < packModelArray.length ? packModelArray[n] : null;
    }

    public static void onItsWay() {
        onTheirWay.incrementAndGet();
    }

    public static void offItsWay() {
        onTheirWay.decrementAndGet();
    }

    public static int onTheirWay() {
        return onTheirWay.get();
    }

    public static PackModel settled() {
        return SETTLED.poll();
    }

    public static void dropped(PackModel packModel) {
        packModel.retry();
        SETTLED.add(packModel);
    }

    public static boolean full() {
        return full;
    }

    public static int roomAgain() {
        return roomAgain;
    }

    static void upload(long l) {
        PackModel packModel;
        long l2 = 0L;
        while (l2 < 0x400000L && (packModel = READ.poll()) != null) {
            PackData packData = packModel.take();
            l2 += packData.bytes();
            onTheirWay.decrementAndGet();
            if (PackModels.place(packModel, packData, l)) {
                packModel.state(3);
                resident.add(packModel);
            } else {
                packModel.refused(l);
                packModel.state(5);
                ++turnedAway;
                full = true;
            }
            SETTLED.add(packModel);
        }
        if (full && ++fullFrames % 30 == 0 && PackModels.room(l)) {
            full = false;
            ++roomAgain;
        }
        Pool.PACKS.use(PackArena.bytes() + PackArt.bytes());
    }

    private static boolean room(long l) {
        if (Pool.PACKS.cap() - PackArena.bytes() - PackArt.bytes() >= PackArt.PAGE_BYTES) {
            return true;
        }
        for (PackModel packModel : resident) {
            if (packModel.held() || l - packModel.wanted() <= 10000000000L) continue;
            return true;
        }
        return false;
    }

    static int table() {
        return tableTexture;
    }

    private static boolean place(PackModel packModel, PackData packData, long l) {
        do {
            long l2;
            if (!PackArena.put(packModel, packData, l2 = Pool.PACKS.cap() - PackArena.bytes() - PackArt.bytes())) continue;
            l2 = Pool.PACKS.cap() - PackArena.bytes() - PackArt.bytes();
            if (PackArt.put(packModel, packData, l2)) {
                PackModels.tableEntry(packModel);
                return true;
            }
            PackArena.free(packModel);
        } while (PackModels.letGoOne(l));
        return false;
    }

    private static boolean letGoOne(long l) {
        PackModel packModel = null;
        for (PackModel packModel2 : resident) {
            if (packModel2.held() || l - packModel2.wanted() <= 10000000000L || packModel != null && packModel2.wanted() >= packModel.wanted()) continue;
            packModel = packModel2;
        }
        if (packModel == null) {
            return false;
        }
        resident.remove(packModel);
        PackArena.free(packModel);
        PackArt.free(packModel);
        packModel.state(0);
        return true;
    }

    private static void tableEntry(PackModel packModel) {
        int n = Math.max(packModel.index + 1, byIndex.length);
        if (n > tableModels) {
            PackModels.growTable(Math.max(n, tableModels * 2));
        }
        float f = 2048.0f;
        entry.clear();
        entry.put((float)packModel.artX / f).put((float)packModel.artY / f);
        entry.put((float)packModel.artWidth / f).put((float)packModel.artHeight / f);
        entry.put((float)(packModel.artX + packModel.artW) / f).put((float)(packModel.artY + packModel.artH) / f);
        entry.put(packModel.artPage).put(0.0f).flip();
        GL15.glBindBuffer((int)35882, (int)tableBuffer);
        GL15.glBufferSubData((int)35882, (long)((long)packModel.index * 2L * 16L), (FloatBuffer)entry);
        GL15.glBindBuffer((int)35882, (int)0);
    }

    private static void growTable(int n) {
        int n2 = GL15.glGenBuffers();
        GL15.glBindBuffer((int)36663, (int)n2);
        GL44.glBufferStorage((int)36663, (long)((long)n * 2L * 16L), (int)256);
        if (tableBuffer != 0) {
            GL15.glBindBuffer((int)36662, (int)tableBuffer);
            GL31.glCopyBufferSubData((int)36662, (int)36663, (long)0L, (long)0L, (long)((long)tableModels * 2L * 16L));
            GL15.glBindBuffer((int)36662, (int)0);
            int n3 = tableBuffer;
            int n4 = tableTexture;
            Retirement.retireDrawing(() -> {
                GL11.glDeleteTextures((int)n4);
                GL15.glDeleteBuffers((int)n3);
            });
        }
        GL15.glBindBuffer((int)36663, (int)0);
        tableBuffer = n2;
        tableModels = n;
        tableTexture = GL11.glGenTextures();
        GL11.glBindTexture((int)35882, (int)tableTexture);
        GL31.glTexBuffer((int)35882, (int)34836, (int)tableBuffer);
        GL11.glBindTexture((int)35882, (int)0);
    }

    static String report() {
        String string = resident.size() + " models on the GPU (" + PackArena.pages() + " mesh pages, " + PackArt.pages() + " art pages), " + onTheirWay.get() + " on their way, " + turnedAway + " turned away";
        turnedAway = 0;
        return string;
    }

    private PackModels() {
    }

    static {
        resident = new ArrayList();
        entry = BufferUtils.createFloatBuffer((int)8);
    }
}

