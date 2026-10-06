/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL33
 *  org.lwjgl.opengl.GL43
 *  org.lwjgl.opengl.GL44
 *  org.lwjgl.system.MemoryUtil
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import org.lwjgl.system.MemoryUtil;
import viewpoint.platform.Pool;
import viewpoint.platform.Profile;
import viewpoint.render.FarGpu;
import viewpoint.render.FrameStream;
import viewpoint.render.Meshes;
import viewpoint.render.Ranges;
import zombie.core.textures.TextureID;

final class ShellArena {
    static final int VERTEX_BYTES = 40;
    private static final int RECORD_FLOATS = 8;
    private static final int LOCATION_ORIGIN = 6;
    private static final int LOCATION_CELL = 7;
    private static final int SPAN_INTS = 4;
    private static final int RECORDS = 6;
    private static final long PAGE_BYTES = 0x4000000L;
    private static Page[] pages = new Page[0];
    private static int pageVertices;
    private static int recordBuffer;
    private static int commandBuffer;
    private static long recordsAt;
    private static long commandsAt;
    private static FloatBuffer records;
    private static int recordCount;
    private static IntBuffer commands;
    private static final IntList spans;

    static ByteBuffer pack(FloatBuffer floatBuffer) {
        int n = floatBuffer.remaining() / 14;
        ByteBuffer byteBuffer = MemoryUtil.memAlloc((int)(n * 40)).order(ByteOrder.LITTLE_ENDIAN);
        int n2 = floatBuffer.position();
        for (int i = 0; i < n; ++i) {
            int n3;
            int n4 = n2 + i * 14;
            byteBuffer.putFloat(floatBuffer.get(n4)).putFloat(floatBuffer.get(n4 + 1)).putFloat(floatBuffer.get(n4 + 2));
            byteBuffer.putFloat(floatBuffer.get(n4 + 3)).putFloat(floatBuffer.get(n4 + 4));
            for (n3 = 5; n3 < 9; ++n3) {
                byteBuffer.putShort((short)Math.round(Math.max(0.0f, Math.min(1.0f, floatBuffer.get(n4 + n3))) * 65535.0f));
            }
            for (n3 = 9; n3 < 12; ++n3) {
                byteBuffer.put((byte)Math.round(Math.max(-1.0f, Math.min(1.0f, floatBuffer.get(n4 + n3))) * 127.0f));
            }
            byteBuffer.put((byte)0);
            byteBuffer.putFloat(floatBuffer.get(n4 + 12));
            byteBuffer.putInt(Float.floatToRawIntBits(floatBuffer.get(n4 + 13)));
        }
        return byteBuffer.flip();
    }

    static int upload(ByteBuffer byteBuffer, int n) {
        int n2;
        int n3;
        if (pageVertices == 0) {
            pageVertices = (int)Math.min(0x4000000L, Pool.SHELL.cap()) / 40;
        }
        if ((n3 = (int)Math.max(1L, Pool.SHELL.cap() / ShellArena.pageBytes())) > pages.length) {
            pages = Arrays.copyOf(pages, n3);
        }
        Page page = null;
        int n4 = -1;
        for (n2 = 0; n2 < pages.length && n4 < 0; ++n2) {
            page = pages[n2];
            n4 = page == null ? -1 : page.free.take(n);
        }
        for (n2 = 0; n2 < pages.length && n4 < 0 && n <= pageVertices; ++n2) {
            if (pages[n2] != null || ShellArena.madeBytes() + ShellArena.pageBytes() > Pool.SHELL.cap()) continue;
            page = ShellArena.newPage(n2);
            n4 = page.free.take(n);
        }
        if (n4 < 0) {
            return -1;
        }
        GL15.glBindBuffer((int)34962, (int)page.vertexBuffer);
        GL15.glBufferSubData((int)34962, (long)((long)n4 * 40L), (ByteBuffer)byteBuffer);
        GL15.glBindBuffer((int)34962, (int)0);
        page.used += n;
        return page.slot * pageVertices + n4;
    }

    static void free(int n, int n2) {
        Page page = pages[n / pageVertices];
        page.free.add(n % pageVertices, n2);
        page.used -= n2;
        if (page.used == 0 && ShellArena.emptyPages() > 1) {
            ShellArena.letGo(page);
        }
    }

    private static Page newPage(int n) {
        int n2 = GL11.glGetInteger((int)34964);
        int n3 = GL15.glGenBuffers();
        GL15.glBindBuffer((int)34962, (int)n3);
        GL44.glBufferStorage((int)34962, (long)ShellArena.pageBytes(), (int)256);
        int n4 = GL30.glGenVertexArrays();
        ShellArena.pointAttributes(n4, n3);
        GL15.glBindBuffer((int)34962, (int)n2);
        ShellArena.pages[n] = new Page(n, n3, n4);
        System.out.println("[Viewpoint] shell arena: page " + n + " made (" + (ShellArena.pageBytes() >> 20) + " MB), " + (ShellArena.madeBytes() >> 20) + " MB in all");
        return pages[n];
    }

    private static void letGo(Page page) {
        GL15.glDeleteBuffers((int)page.vertexBuffer);
        GL30.glDeleteVertexArrays((int)page.vao);
        ShellArena.pages[page.slot] = null;
        System.out.println("[Viewpoint] shell arena: page " + page.slot + " let go, " + (ShellArena.madeBytes() >> 20) + " MB in all");
    }

    private static long pageBytes() {
        return (long)pageVertices * 40L;
    }

    private static long madeBytes() {
        long l = 0L;
        for (Page page : pages) {
            l += page == null ? 0L : ShellArena.pageBytes();
        }
        return l;
    }

    private static int emptyPages() {
        int n = 0;
        for (Page page : pages) {
            n += page != null && page.used == 0 ? 1 : 0;
        }
        return n;
    }

    private static void pointAttributes(int n, int n2) {
        GL30.glBindVertexArray((int)n);
        GL15.glBindBuffer((int)34962, (int)n2);
        GL20.glEnableVertexAttribArray((int)0);
        GL20.glVertexAttribPointer((int)0, (int)3, (int)5126, (boolean)false, (int)40, (long)0L);
        GL20.glEnableVertexAttribArray((int)1);
        GL20.glVertexAttribPointer((int)1, (int)2, (int)5126, (boolean)false, (int)40, (long)12L);
        GL20.glEnableVertexAttribArray((int)2);
        GL20.glVertexAttribPointer((int)2, (int)4, (int)5123, (boolean)true, (int)40, (long)20L);
        GL20.glEnableVertexAttribArray((int)3);
        GL20.glVertexAttribPointer((int)3, (int)3, (int)5120, (boolean)true, (int)40, (long)28L);
        GL20.glEnableVertexAttribArray((int)4);
        GL20.glVertexAttribPointer((int)4, (int)1, (int)5126, (boolean)false, (int)40, (long)32L);
        GL20.glEnableVertexAttribArray((int)5);
        GL30.glVertexAttribIPointer((int)5, (int)1, (int)5124, (int)40, (long)36L);
        GL20.glEnableVertexAttribArray((int)6);
        GL43.glVertexAttribFormat((int)6, (int)4, (int)5126, (boolean)false, (int)0);
        GL43.glVertexAttribBinding((int)6, (int)6);
        GL20.glEnableVertexAttribArray((int)7);
        GL43.glVertexAttribFormat((int)7, (int)4, (int)5126, (boolean)false, (int)16);
        GL43.glVertexAttribBinding((int)7, (int)6);
        GL43.glVertexBindingDivisor((int)6, (int)1);
        GL30.glBindVertexArray((int)0);
        GL15.glBindBuffer((int)34962, (int)0);
    }

    static void begin() {
        records.clear();
        recordCount = 0;
        for (Page page : pages) {
            if (page == null) continue;
            for (IntList intList : page.byAtlas.values()) {
                intList.n = 0;
            }
            page.atlases.clear();
            page.arrays.clear();
        }
    }

    static int record(float f, float f2, float f3, float f4, float f5, int n, float f6, float f7) {
        if (records.remaining() < 8) {
            FloatBuffer floatBuffer = BufferUtils.createFloatBuffer((int)(records.capacity() * 2));
            records.flip();
            floatBuffer.put(records);
            records = floatBuffer;
        }
        records.put(f).put(f2).put(f3).put(n).put(f4).put(f5).put(f6).put(f7);
        return recordCount++;
    }

    static void command(FarGpu.Shell shell, int n, int n2, int n3, int n4) {
        Page page = pages[n2 / pageVertices];
        if (shell.lists == null) {
            shell.lists = new IntList[shell.pages.length];
        }
        if (shell.lists[n] == null) {
            shell.lists[n] = ShellArena.list(page, shell.pages[n]);
        }
        ShellArena.add(shell.lists[n], page, shell.pages[n], 0, n2, n3, n4);
    }

    static void ground(Object object, int n, int n2, int n3, int n4) {
        Page page = pages[n2 / pageVertices];
        ShellArena.add(ShellArena.list(page, object), page, object, n, n2, n3, n4);
    }

    private static IntList list(Page page, Object object) {
        IntList intList = page.byAtlas.get(object);
        if (intList == null) {
            intList = new IntList();
            page.byAtlas.put(object, intList);
        }
        return intList;
    }

    private static void add(IntList intList, Page page, Object object, int n, int n2, int n3, int n4) {
        if (intList.n == 0) {
            page.atlases.add(object);
            if (n != 0) {
                page.arrays.put(object, n);
            }
        }
        intList.add(n3);
        intList.add(1);
        intList.add(n2 % pageVertices);
        intList.add(n4);
    }

    static void draw() {
        int n;
        if (recordCount == 0) {
            return;
        }
        records.flip();
        recordsAt = FrameStream.put(records, 16);
        recordBuffer = FrameStream.buffer();
        int n2 = 0;
        for (Page page : pages) {
            for (n = 0; page != null && n < page.atlases.size(); ++n) {
                n2 += page.byAtlas.get((Object)page.atlases.get((int)n)).n;
            }
        }
        if (commands.capacity() < n2) {
            commands = BufferUtils.createIntBuffer((int)(n2 * 2));
        }
        commands.clear();
        ShellArena.spans.n = 0;
        for (Page page : pages) {
            for (n = 0; page != null && n < page.atlases.size(); ++n) {
                IntList intList = page.byAtlas.get(page.atlases.get(n));
                spans.add(page.slot);
                spans.add(n);
                spans.add(commands.position() / 4);
                spans.add(intList.n / 4);
                commands.put(intList.a, 0, intList.n);
            }
        }
        commands.flip();
        commandsAt = FrameStream.put(commands, 4);
        commandBuffer = FrameStream.buffer();
        ShellArena.issue();
    }

    static void drawAgain() {
        if (ShellArena.spans.n > 0) {
            ShellArena.issue();
        }
    }

    private static void issue() {
        GL15.glBindBuffer((int)36671, (int)commandBuffer);
        for (int i = 0; i < ShellArena.spans.n; i += 4) {
            Page page = pages[ShellArena.spans.a[i]];
            GL30.glBindVertexArray((int)page.vao);
            GL43.glBindVertexBuffer((int)6, (int)recordBuffer, (long)recordsAt, (int)32);
            Object object = page.atlases.get(ShellArena.spans.a[i + 1]);
            if (object instanceof TextureID) {
                TextureID textureID = (TextureID)object;
                Meshes.bindPage(textureID);
            } else {
                Meshes.bindFloorArray(page.arrays.get(object));
            }
            GL43.glMultiDrawArraysIndirect((int)4, (long)(commandsAt + (long)ShellArena.spans.a[i + 2] * 16L), (int)ShellArena.spans.a[i + 3], (int)16);
            ++Profile.draws;
        }
        Meshes.bindFloorArray(0);
        GL30.glBindVertexArray((int)0);
        GL15.glBindBuffer((int)36671, (int)0);
        GL33.glBindSampler((int)0, (int)0);
    }

    private ShellArena() {
    }

    static {
        records = BufferUtils.createFloatBuffer((int)8192);
        commands = BufferUtils.createIntBuffer((int)16384);
        spans = new IntList();
    }

    private static final class Page {
        final int slot;
        final int vertexBuffer;
        final int vao;
        final Ranges free = new Ranges();
        int used;
        final IdentityHashMap<Object, IntList> byAtlas = new IdentityHashMap();
        final ArrayList<Object> atlases = new ArrayList();
        final IdentityHashMap<Object, Integer> arrays = new IdentityHashMap();

        Page(int n, int n2, int n3) {
            this.slot = n;
            this.vertexBuffer = n2;
            this.vao = n3;
            this.free.add(0, pageVertices);
        }
    }

    static final class IntList {
        int[] a = new int[64];
        int n;

        IntList() {
        }

        void add(int n) {
            if (this.n == this.a.length) {
                this.a = Arrays.copyOf(this.a, this.n * 2);
            }
            this.a[this.n++] = n;
        }
    }
}

