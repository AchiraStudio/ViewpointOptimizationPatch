/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL31
 *  org.lwjgl.system.MemoryUtil
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.Profile;
import viewpoint.render.GroundPage;
import viewpoint.render.Retirement;
import viewpoint.render.ShellArena;
import zombie.core.textures.TextureID;

public final class FarGpu {
    public static final float WHOLE = 2.0f;
    private static final LiveSettings.Number UPLOAD_MB = LiveSettings.number("far.shellUploadMb", "Shell uploaded a frame (MB)", "World/Far world", 0.25f, 32.0f, 0.25f, 4.0f);
    private static final LiveSettings.Number CELL_UPLOAD_MB = LiveSettings.number("far.cellUploadMb", "Far cells uploaded a frame (MB)", "World/Far world", 0.25f, 32.0f, 0.25f, 2.0f);
    private static final long MEGABYTE = 0x100000L;
    private static final ConcurrentLinkedQueue<Shell> UPLOADS = new ConcurrentLinkedQueue();
    private static final ConcurrentLinkedQueue<Upload> CELL_UPLOADS = new ConcurrentLinkedQueue();

    public static void retire(Retirement.Freeable freeable) {
        Retirement.retire(freeable);
    }

    public static void upload(Upload upload) {
        if (upload != null) {
            CELL_UPLOADS.add(upload);
        }
    }

    public static void upload(Shell shell) {
        if (shell != null) {
            UPLOADS.add(shell);
        }
    }

    static void uploadShells() {
        Shell shell;
        long l = (long)(UPLOAD_MB.get() * 1048576.0f);
        for (long i = 0L; (shell = UPLOADS.peek()) != null && (i == 0L || i + shell.bytes() <= l) && shell.upload(); i += shell.uploaded() ? shell.bytes() : 0L) {
            UPLOADS.poll();
        }
        Profile.shellUploads = UPLOADS.size();
    }

    static void uploadCells() {
        Upload upload;
        long l = (long)(CELL_UPLOAD_MB.get() * 1048576.0f);
        for (long i = 0L; (upload = CELL_UPLOADS.peek()) != null && (i == 0L || i + upload.bytes() <= l); i += Math.max(1L, upload.bytes())) {
            CELL_UPLOADS.poll();
            upload.upload();
        }
    }

    static int vertexArray(Mesh mesh) {
        mesh.upload();
        return mesh.vao;
    }

    static void bindTrees(Trees trees) {
        trees.upload();
        GL13.glActiveTexture((int)34027);
        GL11.glBindTexture((int)35882, (int)trees.texture);
        GL13.glActiveTexture((int)33984);
    }

    static void textures(Colours colours) {
        colours.upload();
    }

    private static int colourTexture(int[] nArray) {
        IntBuffer intBuffer = MemoryUtil.memAllocInt((int)nArray.length);
        for (int n : nArray) {
            intBuffer.put(0xFF000000 | (n & 0xFF) << 16 | n & 0xFF00 | n >> 16 & 0xFF);
        }
        intBuffer.flip();
        int n = GL11.glGenTextures();
        GL11.glBindTexture((int)3553, (int)n);
        GL11.glTexImage2D((int)3553, (int)0, (int)32856, (int)256, (int)256, (int)0, (int)6408, (int)5121, (IntBuffer)intBuffer);
        GL30.glGenerateMipmap((int)3553);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9987);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        MemoryUtil.memFree((IntBuffer)intBuffer);
        return n;
    }

    private FarGpu() {
    }

    public static final class Shell
    implements Retirement.Freeable {
        final TextureID[] pages;
        final int[] first;
        final int[] count;
        public final int vertices;
        private ByteBuffer packed;
        private int arenaFirst = -1;
        private volatile boolean uploaded;
        ShellArena.IntList[] lists;

        public Shell(TextureID[] textureIDArray, int[] nArray, int[] nArray2, FloatBuffer floatBuffer) {
            this.pages = textureIDArray;
            this.first = nArray;
            this.count = nArray2;
            this.vertices = floatBuffer.remaining() / 14;
            this.packed = ShellArena.pack(floatBuffer);
            MemoryUtil.memFree((FloatBuffer)floatBuffer);
        }

        public long bytes() {
            return (long)this.vertices * 40L;
        }

        public boolean uploaded() {
            return this.uploaded;
        }

        int arenaFirst() {
            return this.arenaFirst;
        }

        private boolean upload() {
            if (this.packed == null) {
                return true;
            }
            this.arenaFirst = ShellArena.upload(this.packed, this.vertices);
            if (this.arenaFirst < 0) {
                return false;
            }
            MemoryUtil.memFree((ByteBuffer)this.packed);
            this.packed = null;
            this.uploaded = true;
            return true;
        }

        @Override
        public void free() {
            if (this.packed != null) {
                MemoryUtil.memFree((ByteBuffer)this.packed);
                this.packed = null;
            }
            if (this.arenaFirst >= 0) {
                ShellArena.free(this.arenaFirst, this.vertices);
                this.arenaFirst = -1;
                this.lists = null;
            }
        }
    }

    public static interface Upload {
        public long bytes();

        public boolean uploaded();

        public void upload();
    }

    public static final class Mesh
    implements Retirement.Freeable,
    Upload {
        public final int detail;
        FloatBuffer verts;
        int vertices;
        int vao;
        int vbo;
        private volatile boolean uploaded;

        public Mesh(int n, FloatBuffer floatBuffer) {
            this.detail = n;
            this.verts = floatBuffer;
            this.vertices = floatBuffer.remaining() / 4;
        }

        @Override
        public long bytes() {
            return (long)this.vertices * 4L * 4L;
        }

        @Override
        public boolean uploaded() {
            return this.uploaded;
        }

        @Override
        public void upload() {
            if (this.vao != 0 || this.verts == null) {
                return;
            }
            this.vao = GL30.glGenVertexArrays();
            this.vbo = GL15.glGenBuffers();
            GL30.glBindVertexArray((int)this.vao);
            GL15.glBindBuffer((int)34962, (int)this.vbo);
            GL15.glBufferData((int)34962, (FloatBuffer)this.verts, (int)35044);
            GL20.glEnableVertexAttribArray((int)0);
            GL20.glVertexAttribPointer((int)0, (int)4, (int)5126, (boolean)false, (int)16, (long)0L);
            GL30.glBindVertexArray((int)0);
            GL15.glBindBuffer((int)34962, (int)0);
            MemoryUtil.memFree((FloatBuffer)this.verts);
            this.verts = null;
            this.uploaded = true;
        }

        @Override
        public void free() {
            if (this.verts != null) {
                MemoryUtil.memFree((FloatBuffer)this.verts);
                this.verts = null;
            }
            if (this.vbo != 0) {
                GL15.glDeleteBuffers((int)this.vbo);
                GL30.glDeleteVertexArrays((int)this.vao);
                this.vao = 0;
                this.vbo = 0;
            }
        }
    }

    public static final class Trees
    implements Retirement.Freeable,
    Upload {
        public static final int RANKS = 256;
        public final int count;
        private final int[] below;
        short[] list;
        int buffer;
        int texture;
        private volatile boolean uploaded;

        public Trees(short[] sArray, int[] nArray) {
            this.list = sArray;
            this.below = nArray;
            this.count = sArray.length / 4;
        }

        public int kept(int n) {
            return this.below[Math.max(0, Math.min(256, n))];
        }

        @Override
        public long bytes() {
            return (long)this.count * 8L;
        }

        @Override
        public boolean uploaded() {
            return this.uploaded;
        }

        @Override
        public void upload() {
            if (this.buffer != 0 || this.list == null) {
                return;
            }
            ShortBuffer shortBuffer = MemoryUtil.memAllocShort((int)this.list.length).put(this.list).flip();
            this.buffer = GL15.glGenBuffers();
            GL15.glBindBuffer((int)35882, (int)this.buffer);
            GL15.glBufferData((int)35882, (ShortBuffer)shortBuffer, (int)35044);
            GL15.glBindBuffer((int)35882, (int)0);
            MemoryUtil.memFree((ShortBuffer)shortBuffer);
            this.texture = GL11.glGenTextures();
            GL11.glBindTexture((int)35882, (int)this.texture);
            GL31.glTexBuffer((int)35882, (int)36214, (int)this.buffer);
            GL11.glBindTexture((int)35882, (int)0);
            this.list = null;
            this.uploaded = true;
        }

        @Override
        public void free() {
            this.list = null;
            if (this.buffer != 0) {
                GL15.glDeleteBuffers((int)this.buffer);
                GL11.glDeleteTextures((int)this.texture);
                this.texture = 0;
                this.buffer = 0;
            }
        }
    }

    public static final class Colours
    implements Retirement.Freeable,
    Upload {
        public static final long BYTES = 699050L;
        int[] top;
        int[] side;
        int topTexture;
        int sideTexture;
        private volatile boolean uploaded;

        public Colours(int[] nArray, int[] nArray2) {
            this.top = nArray;
            this.side = nArray2;
        }

        @Override
        public long bytes() {
            return 699050L;
        }

        @Override
        public boolean uploaded() {
            return this.uploaded;
        }

        @Override
        public void upload() {
            if (this.topTexture != 0 || this.top == null) {
                return;
            }
            GL13.glActiveTexture((int)34009);
            this.topTexture = FarGpu.colourTexture(this.top);
            this.sideTexture = FarGpu.colourTexture(this.side);
            GL11.glBindTexture((int)3553, (int)0);
            GL13.glActiveTexture((int)33984);
            this.side = null;
            this.top = null;
            this.uploaded = true;
        }

        @Override
        public void free() {
            this.side = null;
            this.top = null;
            if (this.topTexture != 0) {
                GL11.glDeleteTextures((int)this.topTexture);
                GL11.glDeleteTextures((int)this.sideTexture);
                this.sideTexture = 0;
                this.topTexture = 0;
            }
        }
    }

    public static final class BlockDraw {
        public Shell shell;
        public Mesh inside;
        public float originX;
        public float originY;
        public float originZ;
        public int worldX;
        public int worldY;
        public int blockX;
        public int blockY;
        public float top;
        public GroundPage ground;
        public boolean lite;
        public float distance;
        public int maskIndex = -1;
        public float fadeLo;
        public float fadeHi = 2.0f;
        Object groundArray;
        int groundTexture;
        int groundLayer;
    }

    public static final class CellDraw {
        public Mesh mesh;
        public Colours colours;
        public Trees trees;
        public float originX;
        public float originY;
        public float originZ;
        public int worldX;
        public int worldY;
        public float maxHeight;
        public float fadeLo;
        public float fadeHi = 2.0f;
        public int treeKeep = 256;
        public float treeGrow = 1.0f;
    }
}

