/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL43
 *  org.lwjgl.opengl.GL44
 *  org.lwjgl.system.MemoryUtil
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import org.lwjgl.system.MemoryUtil;
import viewpoint.render.PackData;
import viewpoint.render.PackModel;
import viewpoint.render.Ranges;

final class PackArena {
    static final int PAGE_VERTICES = 131072;
    static final int PAGE_INDICES = 0x100000;
    static final int VERTEX_BYTES = 32;
    static final int INDEX_BYTES = 4;
    static final long PAGE_BYTES = 0x800000L;
    static final int POSITION = 0;
    static final int UV = 1;
    static final int NORMAL = 3;
    static final int RUN = 9;
    static final int RUN_INTS = 4;
    private static final int RUNS = 9;
    private static final int ONE_RECORD = 0x40000000;
    private static final ArrayList<Page> pages = new ArrayList();

    static boolean put(PackModel packModel, PackData packData, long l) {
        int n = packData.vertices().length / 8;
        int n2 = packData.indices().length;
        if (n > 131072 || n2 > 0x100000) {
            return false;
        }
        for (int i = 0; i < pages.size(); ++i) {
            if (!PackArena.place(packModel, packData, i, n, n2)) continue;
            return true;
        }
        if (l < 0x800000L) {
            return false;
        }
        pages.add(PackArena.page());
        return PackArena.place(packModel, packData, pages.size() - 1, n, n2);
    }

    static void free(PackModel packModel) {
        Page page = pages.get(packModel.meshPage);
        page.vertexRoom.add(packModel.firstVertex, packModel.vertices);
        page.indexRoom.add(packModel.firstIndex, packModel.indices);
    }

    static long bytes() {
        return (long)pages.size() * 0x800000L;
    }

    static int pages() {
        return pages.size();
    }

    static void bind(int n, int n2, long l) {
        GL30.glBindVertexArray((int)PackArena.pages.get((int)n).vao);
        GL43.glBindVertexBuffer((int)9, (int)n2, (long)l, (int)16);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static boolean place(PackModel packModel, PackData packData, int n, int n2, int n3) {
        Page page = pages.get(n);
        int n4 = page.vertexRoom.take(n2);
        if (n4 < 0) {
            return false;
        }
        int n5 = page.indexRoom.take(n3);
        if (n5 < 0) {
            page.vertexRoom.add(n4, n2);
            return false;
        }
        FloatBuffer floatBuffer = MemoryUtil.memAllocFloat((int)packData.vertices().length).put(packData.vertices()).flip();
        IntBuffer intBuffer = MemoryUtil.memAllocInt((int)n3).put(packData.indices()).flip();
        try {
            GL15.glBindBuffer((int)34962, (int)page.vertices);
            GL15.glBufferSubData((int)34962, (long)((long)n4 * 32L), (FloatBuffer)floatBuffer);
            GL15.glBindBuffer((int)34962, (int)page.indices);
            GL15.glBufferSubData((int)34962, (long)((long)n5 * 4L), (IntBuffer)intBuffer);
            GL15.glBindBuffer((int)34962, (int)0);
        }
        finally {
            MemoryUtil.memFree((FloatBuffer)floatBuffer);
            MemoryUtil.memFree((IntBuffer)intBuffer);
        }
        packModel.meshPage = n;
        packModel.firstVertex = n4;
        packModel.vertices = n2;
        packModel.firstIndex = n5;
        packModel.indices = n3;
        return true;
    }

    private static Page page() {
        int n = GL15.glGenBuffers();
        int n2 = GL15.glGenBuffers();
        int n3 = GL30.glGenVertexArrays();
        GL15.glBindBuffer((int)34962, (int)n);
        GL44.glBufferStorage((int)34962, (long)0x400000L, (int)256);
        GL15.glBindBuffer((int)34962, (int)n2);
        GL44.glBufferStorage((int)34962, (long)0x400000L, (int)256);
        GL30.glBindVertexArray((int)n3);
        GL15.glBindBuffer((int)34962, (int)n);
        PackArena.attribute(0, 3, 0L);
        PackArena.attribute(3, 3, 12L);
        PackArena.attribute(1, 2, 24L);
        GL15.glBindBuffer((int)34963, (int)n2);
        GL20.glEnableVertexAttribArray((int)9);
        GL43.glVertexAttribIFormat((int)9, (int)4, (int)5124, (int)0);
        GL43.glVertexAttribBinding((int)9, (int)9);
        GL43.glVertexBindingDivisor((int)9, (int)0x40000000);
        GL30.glBindVertexArray((int)0);
        GL15.glBindBuffer((int)34962, (int)0);
        System.out.println("[Viewpoint] model packs: mesh page " + (pages.size() + 1) + " made");
        return new Page(n3, n, n2);
    }

    private static void attribute(int n, int n2, long l) {
        GL20.glEnableVertexAttribArray((int)n);
        GL20.glVertexAttribPointer((int)n, (int)n2, (int)5126, (boolean)false, (int)32, (long)l);
    }

    private PackArena() {
    }

    private static final class Page {
        final int vao;
        final int vertices;
        final int indices;
        final Ranges vertexRoom = new Ranges();
        final Ranges indexRoom = new Ranges();

        Page(int n, int n2, int n3) {
            this.vao = n;
            this.vertices = n2;
            this.indices = n3;
            this.vertexRoom.add(0, 131072);
            this.indexRoom.add(0, 0x100000);
        }
    }
}

