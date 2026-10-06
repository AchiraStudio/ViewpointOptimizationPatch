/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL31
 *  org.lwjgl.opengl.GL43
 *  org.lwjgl.opengl.GL45
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.TreeMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.Pool;
import viewpoint.platform.Profile;
import viewpoint.platform.VertexFormat;
import viewpoint.render.FrameStream;

public final class MeshArena {
    static final int RECORD_FLOATS = 12;
    private static final int LOCATION_OFFSET = 6;
    private static final int LOCATION_FADE = 7;
    private static final int LOCATION_BELOW = 8;
    private static final int RECORDS = 6;
    private static final int VERTEX_BYTES = 56;
    private static final int START_VERTICES = 0x100000;
    private static final int START_ROWS = 16;
    private static int vao;
    private static int vertexBuffer;
    private static int lightArray;
    private static int plantVao;
    private static int slotTexture;
    private static int recordBuffer;
    private static int commandBuffer;
    private static long recordsAt;
    private static long commandsAt;
    private static long recordBytes;
    private static int recordsTexture;
    private static int capacity;
    private static int rows;
    private static int nextSlot;
    private static int used;
    private static final TreeMap<Integer, Integer> free;
    private static final ArrayDeque<Integer> freeSlots;
    private static FloatBuffer records;
    private static IntBuffer commands;

    public static void init() {
        Profile.arenaUsage = MeshArena::usage;
        MeshArena.limitToSlots();
        int n = GL11.glGetInteger((int)34964);
        int n2 = GL11.glGetInteger((int)32873);
        vertexBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer((int)34962, (int)vertexBuffer);
        capacity = (int)Math.min(0x100000L, Pool.NEAR_MESHES.cap() / 56L);
        GL15.glBufferData((int)34962, (long)((long)capacity * 56L), (int)35048);
        free.put(0, capacity);
        vao = GL30.glGenVertexArrays();
        MeshArena.pointAttributes();
        plantVao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray((int)plantVao);
        MeshArena.recordAttributes();
        GL30.glBindVertexArray((int)0);
        slotTexture = GL11.glGenTextures();
        MeshArena.pointSlots();
        rows = (int)Math.min(16L, Pool.LIGHT_GRIDS.cap() / 652800L);
        lightArray = MeshArena.lightTexture(rows);
        GL15.glBindBuffer((int)34962, (int)n);
        GL11.glBindTexture((int)3553, (int)n2);
    }

    private static void limitToSlots() {
        long l = GL11.glGetInteger((int)35883);
        long l2 = l / 7L;
        if (l2 < 0x100000L) {
            throw new IllegalStateException("[Viewpoint] this GPU's buffer textures reach " + l + " texels: the mesh arena's plants need 7340032");
        }
        Pool.NEAR_MESHES.limit(l2 * 56L);
        System.out.println("[Viewpoint] mesh arena: buffer textures reach " + l + " texels, " + (l2 * 56L >> 20) + " MiB of arena");
    }

    static int upload(FloatBuffer floatBuffer, int n) {
        int n2 = MeshArena.allocate(n);
        if (n2 < 0) {
            return -1;
        }
        GL15.glBindBuffer((int)34962, (int)vertexBuffer);
        GL15.glBufferSubData((int)34962, (long)((long)n2 * 56L), (FloatBuffer)floatBuffer);
        GL15.glBindBuffer((int)34962, (int)0);
        used += n;
        return n2;
    }

    static void free(int n, int n2) {
        used -= n2;
        MeshArena.release(n, n2);
    }

    private static void release(int n, int n2) {
        Integer n3;
        int n4 = n;
        int n5 = n2;
        Map.Entry<Integer, Integer> entry = free.floorEntry(n);
        if (entry != null && entry.getKey() + entry.getValue() == n) {
            n4 = entry.getKey();
            n5 += entry.getValue().intValue();
            free.remove(entry.getKey());
        }
        if ((n3 = free.get(n + n2)) != null) {
            n5 += n3.intValue();
            free.remove(n + n2);
        }
        free.put(n4, n5);
    }

    private static int allocate(int n) {
        for (Map.Entry<Integer, Integer> entry : free.entrySet()) {
            if (entry.getValue() < n) continue;
            int n2 = entry.getKey();
            int n3 = entry.getValue() - n;
            free.remove(n2);
            if (n3 > 0) {
                free.put(n2 + n, n3);
            }
            return n2;
        }
        int n4 = (int)Math.min(Integer.MAX_VALUE, Pool.NEAR_MESHES.cap() / 56L);
        if (capacity >= n4) {
            return -1;
        }
        MeshArena.grow(Math.min(n4, Math.max(capacity * 2, capacity + n)));
        return MeshArena.allocate(n);
    }

    private static void grow(int n) {
        int n2 = capacity;
        capacity = n;
        int n3 = GL15.glGenBuffers();
        GL15.glBindBuffer((int)36663, (int)n3);
        GL15.glBufferData((int)36663, (long)((long)capacity * 56L), (int)35048);
        GL15.glBindBuffer((int)36662, (int)vertexBuffer);
        GL31.glCopyBufferSubData((int)36662, (int)36663, (long)0L, (long)0L, (long)((long)n2 * 56L));
        GL15.glBindBuffer((int)36662, (int)0);
        GL15.glBindBuffer((int)36663, (int)0);
        GL15.glDeleteBuffers((int)vertexBuffer);
        vertexBuffer = n3;
        MeshArena.release(n2, capacity - n2);
        MeshArena.pointAttributes();
        MeshArena.pointSlots();
        System.out.println("[Viewpoint] mesh arena grown to " + capacity / 1024 + "k vertices (" + ((long)capacity * 56L >> 20) + " MB)");
    }

    private static void pointAttributes() {
        GL30.glBindVertexArray((int)vao);
        GL15.glBindBuffer((int)34962, (int)vertexBuffer);
        long l = 0L;
        for (int i = 0; i < VertexFormat.ATTRIBUTE_SIZES.length; ++i) {
            GL20.glEnableVertexAttribArray((int)i);
            if (i == 5) {
                GL30.glVertexAttribIPointer((int)i, (int)VertexFormat.ATTRIBUTE_SIZES[i], (int)5124, (int)56, (long)l);
            } else {
                GL20.glVertexAttribPointer((int)i, (int)VertexFormat.ATTRIBUTE_SIZES[i], (int)5126, (boolean)false, (int)56, (long)l);
            }
            l += (long)VertexFormat.ATTRIBUTE_SIZES[i] * 4L;
        }
        MeshArena.recordAttributes();
        GL30.glBindVertexArray((int)0);
        GL15.glBindBuffer((int)34962, (int)0);
    }

    private static void recordAttributes() {
        GL20.glEnableVertexAttribArray((int)6);
        GL43.glVertexAttribFormat((int)6, (int)4, (int)5126, (boolean)false, (int)0);
        GL43.glVertexAttribBinding((int)6, (int)6);
        GL20.glEnableVertexAttribArray((int)7);
        GL43.glVertexAttribFormat((int)7, (int)4, (int)5126, (boolean)false, (int)16);
        GL43.glVertexAttribBinding((int)7, (int)6);
        GL20.glEnableVertexAttribArray((int)8);
        GL43.glVertexAttribFormat((int)8, (int)4, (int)5126, (boolean)false, (int)32);
        GL43.glVertexAttribBinding((int)8, (int)6);
        GL43.glVertexBindingDivisor((int)6, (int)1);
    }

    private static void pointSlots() {
        GL11.glBindTexture((int)35882, (int)slotTexture);
        GL31.glTexBuffer((int)35882, (int)33328, (int)vertexBuffer);
        GL11.glBindTexture((int)35882, (int)0);
    }

    static int takeSlot() {
        if (!freeSlots.isEmpty()) {
            return freeSlots.pop();
        }
        if (nextSlot == rows * 204) {
            int n = (int)Math.min((long)(GL11.glGetInteger((int)3379) / 40), Pool.LIGHT_GRIDS.cap() / 652800L);
            int n2 = Math.min(rows * 2, n);
            if (n2 <= rows) {
                return -1;
            }
            int n3 = MeshArena.lightTexture(n2);
            GL43.glCopyImageSubData((int)lightArray, (int)3553, (int)0, (int)0, (int)0, (int)0, (int)n3, (int)3553, (int)0, (int)0, (int)0, (int)0, (int)4080, (int)(rows * 40), (int)1);
            GL11.glDeleteTextures((int)lightArray);
            lightArray = n3;
            rows = n2;
            System.out.println("[Viewpoint] light atlas grown to " + rows * 204 + " grids");
        }
        return nextSlot++;
    }

    static void freeSlot(int n) {
        freeSlots.push(n);
    }

    static void uploadLight(int n, ByteBuffer byteBuffer) {
        GL11.glPixelStorei((int)3317, (int)4);
        GL45.glTextureSubImage2D((int)lightArray, (int)0, (int)(n % 204 * 20), (int)(n / 204 * 40), (int)20, (int)40, (int)6408, (int)5121, (ByteBuffer)byteBuffer);
    }

    public static void bindLight(int n, GlProgram glProgram) {
        GL13.glActiveTexture((int)(33984 + n));
        GL11.glBindTexture((int)3553, (int)lightArray);
        GL13.glActiveTexture((int)33984);
        glProgram.set("uLightAtlasSize", 4080.0f, rows * 40);
    }

    public static void unbindLight(int n) {
        GL13.glActiveTexture((int)(33984 + n));
        GL11.glBindTexture((int)3553, (int)0);
        GL13.glActiveTexture((int)33984);
    }

    private static int lightTexture(int n) {
        int n2 = GL45.glCreateTextures((int)3553);
        GL45.glTextureParameteri((int)n2, (int)10241, (int)9729);
        GL45.glTextureParameteri((int)n2, (int)10240, (int)9729);
        GL45.glTextureParameteri((int)n2, (int)10242, (int)33071);
        GL45.glTextureParameteri((int)n2, (int)10243, (int)33071);
        GL45.glTextureStorage2D((int)n2, (int)1, (int)32856, (int)4080, (int)(n * 40));
        return n2;
    }

    public static FloatBuffer records(int n) {
        if (records.capacity() < n * 12) {
            records = BufferUtils.createFloatBuffer((int)(n * 12 * 2));
        }
        records.clear();
        return records;
    }

    public static void uploadRecords() {
        records.flip();
        recordBytes = (long)records.remaining() * 4L;
        recordsAt = FrameStream.put(records, Math.max(16, FrameStream.textureAlignment()));
        recordBuffer = FrameStream.buffer();
    }

    static void bindForModels(int n, int n2) {
        if (recordsTexture == 0) {
            recordsTexture = GL11.glGenTextures();
        }
        GL13.glActiveTexture((int)(33984 + n));
        GL11.glBindTexture((int)35882, (int)recordsTexture);
        if (recordBytes > 0L) {
            GL43.glTexBufferRange((int)35882, (int)34836, (int)recordBuffer, (long)recordsAt, (long)recordBytes);
        }
        GL13.glActiveTexture((int)(33984 + n2));
        GL11.glBindTexture((int)35882, (int)slotTexture);
        GL13.glActiveTexture((int)33984);
    }

    static void unbindForModels(int n, int n2) {
        GL13.glActiveTexture((int)(33984 + n));
        GL11.glBindTexture((int)35882, (int)0);
        GL13.glActiveTexture((int)(33984 + n2));
        GL11.glBindTexture((int)35882, (int)0);
        GL13.glActiveTexture((int)33984);
    }

    public static IntBuffer commands(int n) {
        if (commands.capacity() < n * 4) {
            commands = BufferUtils.createIntBuffer((int)(n * 8));
        }
        commands.clear();
        return commands;
    }

    public static void beginDraws() {
        commands.flip();
        commandsAt = FrameStream.put(commands, 4);
        commandBuffer = FrameStream.buffer();
        GL15.glBindBuffer((int)36671, (int)commandBuffer);
        GL30.glBindVertexArray((int)vao);
        GL43.glBindVertexBuffer((int)6, (int)recordBuffer, (long)recordsAt, (int)48);
    }

    static void beginPlants(int n) {
        GL30.glBindVertexArray((int)plantVao);
        GL43.glBindVertexBuffer((int)6, (int)recordBuffer, (long)recordsAt, (int)48);
        GL13.glActiveTexture((int)(33984 + n));
        GL11.glBindTexture((int)35882, (int)slotTexture);
        GL13.glActiveTexture((int)33984);
    }

    static void endPlants(int n) {
        GL13.glActiveTexture((int)(33984 + n));
        GL11.glBindTexture((int)35882, (int)0);
        GL13.glActiveTexture((int)33984);
    }

    public static void multiDraw(int n, int n2) {
        GL43.glMultiDrawArraysIndirect((int)Gl.triangles(), (long)(commandsAt + (long)n * 16L), (int)n2, (int)16);
        ++Profile.draws;
        Profile.meshDraws += n2;
    }

    public static void endDraws() {
        GL30.glBindVertexArray((int)0);
        GL15.glBindBuffer((int)36671, (int)0);
    }

    private static String usage() {
        return (used >> 10) + "k/" + (capacity >> 10) + "k vertices, " + (nextSlot - freeSlots.size()) + " light grids";
    }

    private MeshArena() {
    }

    static {
        free = new TreeMap();
        freeSlots = new ArrayDeque();
        records = BufferUtils.createFloatBuffer((int)12288);
        commands = BufferUtils.createIntBuffer((int)16384);
    }
}

