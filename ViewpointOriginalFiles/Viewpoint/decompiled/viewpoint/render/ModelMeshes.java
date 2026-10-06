/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL31
 *  org.lwjgl.opengl.GL43
 *  zombie.core.skinnedmodel.model.ModelMesh
 *  zombie.core.skinnedmodel.model.VertexBufferObject
 *  zombie.core.skinnedmodel.model.VertexBufferObject$Vbo
 *  zombie.core.skinnedmodel.model.VertexBufferObject$VertexElement
 *  zombie.core.skinnedmodel.model.VertexBufferObject$VertexFormat
 */
package viewpoint.render;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL43;
import viewpoint.platform.Pool;
import viewpoint.render.Ranges;
import viewpoint.render.Retirement;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.VertexBufferObject;

final class ModelMeshes {
    static final int POSITION = 0;
    static final int NORMAL = 1;
    static final int WEIGHTS = 2;
    static final int BONES = 3;
    static final int UV = 4;
    static final int UV2 = 5;
    static final int DRAW = 6;
    private static final int INDEX_BYTES = 4;
    private static final int START_VERTICES = 65536;
    private static final int START_INDICES = 262144;
    private static final IdentityHashMap<ModelMesh, Held> held = new IdentityHashMap();
    private static final HashMap<Long, Layout> layouts = new HashMap();
    static final Set<ModelMesh> refused = ConcurrentHashMap.newKeySet();
    private static final Ranges ids = new Ranges();
    private static int nextId;
    private static long used;
    private static long capacity;
    private static final Field HANDLE;
    private static final Field FORMAT;

    static boolean available() {
        return HANDLE != null;
    }

    static Held get(ModelMesh modelMesh, long l) {
        VertexBufferObject.Vbo vbo;
        VertexBufferObject vertexBufferObject = modelMesh.vb;
        VertexBufferObject.Vbo vbo2 = vbo = vertexBufferObject == null ? null : ModelMeshes.handle(vertexBufferObject);
        if (vbo == null || vbo.vboId == 0 || vbo.eboId == 0 || vbo.numElements <= 0) {
            return null;
        }
        Held held = ModelMeshes.held.get(modelMesh);
        if (held != null && held.sourceVbo == vbo.vboId && held.sourceEbo == vbo.eboId && held.modification == modelMesh.modificationCount) {
            held.used = l;
            return held;
        }
        if (held != null) {
            ModelMeshes.letGo(ModelMeshes.held.remove(modelMesh));
        }
        if ((held = ModelMeshes.copy(modelMesh, vertexBufferObject, vbo)) != null) {
            held.used = l;
            ModelMeshes.held.put(modelMesh, held);
        }
        return held;
    }

    private static VertexBufferObject.Vbo handle(VertexBufferObject vertexBufferObject) {
        try {
            return (VertexBufferObject.Vbo)HANDLE.get(vertexBufferObject);
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static Held copy(ModelMesh modelMesh, VertexBufferObject vertexBufferObject, VertexBufferObject.Vbo vbo) {
        int n;
        int[] nArray;
        try {
            nArray = ModelMeshes.offsets((VertexBufferObject.VertexFormat)FORMAT.get(vertexBufferObject));
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
        if (nArray[0] < 0 || nArray[4] < 0 || vbo.vertexStride <= 0) {
            refused.add(modelMesh);
            return null;
        }
        Layout layout = ModelMeshes.layout(vbo.vertexStride, nArray);
        int n2 = (int)(ModelMeshes.size(vbo.vboId) / (long)vbo.vertexStride);
        int n3 = vbo.numElements;
        int n4 = ModelMeshes.take(layout, n2, true);
        int n5 = n = n4 < 0 ? -1 : ModelMeshes.take(layout, n3, false);
        if (n < 0) {
            if (n4 >= 0) {
                layout.vertexRoom.add(n4, n2);
            }
            return null;
        }
        ModelMeshes.copyInto(vbo.vboId, layout.vbo, (long)n4 * (long)layout.stride, (long)n2 * (long)layout.stride);
        ModelMeshes.copyInto(vbo.eboId, layout.ebo, (long)n * 4L, (long)n3 * 4L);
        Held held = new Held();
        held.layout = layout;
        held.firstVertex = n4;
        held.vertices = n2;
        held.firstIndex = n;
        held.elements = n3;
        held.sourceVbo = vbo.vboId;
        held.sourceEbo = vbo.eboId;
        held.modification = modelMesh.modificationCount;
        held.id = ModelMeshes.id();
        used += (long)n2 * (long)layout.stride + (long)n3 * 4L;
        return held;
    }

    /*
     * WARNING - void declaration
     */
    private static Layout layout(int n, int[] nArray) {
        void var4_6;
        long l = n;
        for (int n2 : nArray) {
            l = l * 257L + (long)n2 + 1L;
        }
        Layout layout = layouts.get(l);
        if (layout == null) {
            Layout layout2 = new Layout(n, nArray, layouts.size());
            layout2.vbo = GL15.glGenBuffers();
            layout2.ebo = GL15.glGenBuffers();
            layout2.vao = GL30.glGenVertexArrays();
            layouts.put(l, layout2);
        }
        return var4_6;
    }

    private static int take(Layout layout, int n, boolean bl) {
        Ranges ranges = bl ? layout.vertexRoom : layout.indexRoom;
        int n2 = ranges.take(n);
        if (n2 >= 0) {
            return n2;
        }
        int n3 = bl ? layout.vertexCapacity : layout.indexCapacity;
        int n4 = bl ? layout.stride : 4;
        long l = Math.max(Math.max((long)n3 * 2L, (long)n3 + (long)n), bl ? 65536L : 262144L);
        int n5 = (int)Math.min(l, (long)n3 + (Pool.MODEL_MESHES.cap() - capacity) / (long)n4);
        if (n5 < n3 + n) {
            return -1;
        }
        ModelMeshes.grow(layout, bl, n3, n5);
        capacity += (long)(n5 - n3) * (long)n4;
        ranges.add(n3, n5 - n3);
        return ranges.take(n);
    }

    private static void grow(Layout layout, boolean bl, int n, int n2) {
        int n3 = bl ? layout.stride : 4;
        int n4 = bl ? layout.vbo : layout.ebo;
        int n5 = GL15.glGenBuffers();
        GL15.glBindBuffer((int)36663, (int)n5);
        GL15.glBufferData((int)36663, (long)((long)n2 * (long)n3), (int)35044);
        if (n > 0) {
            GL15.glBindBuffer((int)36662, (int)n4);
            GL31.glCopyBufferSubData((int)36662, (int)36663, (long)0L, (long)0L, (long)((long)n * (long)n3));
            GL15.glBindBuffer((int)36662, (int)0);
        }
        GL15.glBindBuffer((int)36663, (int)0);
        GL15.glDeleteBuffers((int)n4);
        if (bl) {
            layout.vbo = n5;
            layout.vertexCapacity = n2;
        } else {
            layout.ebo = n5;
            layout.indexCapacity = n2;
        }
        ModelMeshes.point(layout);
    }

    private static void point(Layout layout) {
        GL30.glBindVertexArray((int)layout.vao);
        GL15.glBindBuffer((int)34962, (int)layout.vbo);
        int n = layout.stride;
        int[] nArray = layout.offsets;
        boolean bl = nArray[2] >= 0 && nArray[3] >= 0;
        ModelMeshes.attribute(0, 3, n, nArray[0]);
        ModelMeshes.attribute(1, 3, n, nArray[1]);
        ModelMeshes.attribute(2, 4, n, bl ? nArray[2] : -1);
        ModelMeshes.attribute(3, 4, n, bl ? nArray[3] : -1);
        ModelMeshes.attribute(4, 2, n, nArray[4]);
        ModelMeshes.attribute(5, 2, n, nArray[5]);
        GL43.glVertexAttribIFormat((int)6, (int)1, (int)5124, (int)0);
        GL43.glVertexAttribBinding((int)6, (int)6);
        GL43.glVertexBindingDivisor((int)6, (int)1);
        GL20.glEnableVertexAttribArray((int)6);
        GL15.glBindBuffer((int)34963, (int)layout.ebo);
        GL30.glBindVertexArray((int)0);
        GL15.glBindBuffer((int)34962, (int)0);
    }

    static int[] offsets(VertexBufferObject.VertexFormat vertexFormat) {
        int[] nArray = new int[]{-1, -1, -1, -1, -1, -1};
        for (int i = 0; i < vertexFormat.getNumElements(); ++i) {
            int n;
            VertexBufferObject.VertexElement vertexElement = vertexFormat.getElement(i);
            switch (vertexElement.type) {
                case VertexArray: {
                    int n2 = 0;
                    break;
                }
                case NormalArray: {
                    int n2 = 1;
                    break;
                }
                case BlendWeightArray: {
                    int n2 = 2;
                    break;
                }
                case BlendIndexArray: {
                    int n2 = 3;
                    break;
                }
                case TextureCoordArray: {
                    int n2 = 4;
                    break;
                }
                default: {
                    int n2 = n = -1;
                }
            }
            if (n == 4 && nArray[4] >= 0) {
                n = 5;
            }
            if (n < 0 || nArray[n] >= 0) continue;
            nArray[n] = vertexElement.byteOffset;
        }
        return nArray;
    }

    private static void attribute(int n, int n2, int n3, int n4) {
        if (n4 < 0) {
            GL20.glDisableVertexAttribArray((int)n);
            GL20.glVertexAttrib4f((int)n, (float)0.0f, (float)(n == 1 ? 1.0f : 0.0f), (float)0.0f, (float)0.0f);
            return;
        }
        GL20.glVertexAttribPointer((int)n, (int)n2, (int)5126, (boolean)false, (int)n3, (long)n4);
        GL20.glEnableVertexAttribArray((int)n);
    }

    private static void copyInto(int n, int n2, long l, long l2) {
        GL15.glBindBuffer((int)36662, (int)n);
        GL15.glBindBuffer((int)36663, (int)n2);
        GL31.glCopyBufferSubData((int)36662, (int)36663, (long)0L, (long)l, (long)l2);
        GL15.glBindBuffer((int)36662, (int)0);
        GL15.glBindBuffer((int)36663, (int)0);
    }

    private static long size(int n) {
        GL15.glBindBuffer((int)36662, (int)n);
        long l = GL15.glGetBufferParameteri((int)36662, (int)34660);
        GL15.glBindBuffer((int)36662, (int)0);
        return l;
    }

    private static int id() {
        int n;
        int n2 = ids.take(1);
        if (n2 >= 0) {
            n = n2;
        } else {
            int n3 = nextId;
            n = n3;
            nextId = n3 + 1;
        }
        return n;
    }

    private static void letGo(Held held) {
        used -= (long)held.vertices * (long)held.layout.stride + (long)held.elements * 4L;
        Retirement.retireDrawing(() -> {
            held.layout.vertexRoom.add(held.firstVertex, held.vertices);
            held.layout.indexRoom.add(held.firstIndex, held.elements);
            ids.add(held.id, 1);
        });
    }

    static void clear() {
        for (Held held : ModelMeshes.held.values()) {
            ModelMeshes.letGo(held);
        }
        held.clear();
        refused.clear();
        Pool.MODEL_MESHES.use(used);
    }

    static void makeRoom(long l) {
        Pool.MODEL_MESHES.use(used);
        if (!Pool.MODEL_MESHES.over(0.9)) {
            return;
        }
        ArrayList<ModelMesh> arrayList = new ArrayList<ModelMesh>();
        for (Map.Entry<ModelMesh, Held> entry : held.entrySet()) {
            if (entry.getValue().used >= l) continue;
            arrayList.add(entry.getKey());
        }
        arrayList.sort((modelMesh, modelMesh2) -> Long.compare(ModelMeshes.held.get((Object)modelMesh).used, ModelMeshes.held.get((Object)modelMesh2).used));
        for (int i = 0; i < arrayList.size() && Pool.MODEL_MESHES.over(0.8); ++i) {
            ModelMeshes.letGo(held.remove(arrayList.get(i)));
            Pool.MODEL_MESHES.use(used);
        }
    }

    private ModelMeshes() {
    }

    static {
        Field field = null;
        Field field2 = null;
        try {
            field = VertexBufferObject.class.getDeclaredField("handle");
            field2 = VertexBufferObject.class.getDeclaredField("vertexFormat");
            field.setAccessible(true);
            field2.setAccessible(true);
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            System.out.println("[Viewpoint] models: the game's vertex buffers cannot be read (models stay the game's): " + String.valueOf(exception));
            field2 = null;
            field = null;
        }
        HANDLE = field;
        FORMAT = field2;
    }

    static final class Held {
        Layout layout;
        int firstVertex;
        int vertices;
        int firstIndex;
        int elements;
        int sourceVbo;
        int sourceEbo;
        int modification;
        int id;
        long used;

        Held() {
        }
    }

    static final class Layout {
        final int stride;
        final int index;
        final int[] offsets;
        final Ranges vertexRoom = new Ranges();
        final Ranges indexRoom = new Ranges();
        int vao;
        int vbo;
        int ebo;
        int vertexCapacity;
        int indexCapacity;

        Layout(int n, int[] nArray, int n2) {
            this.stride = n;
            this.offsets = nArray;
            this.index = n2;
        }

        long bytes() {
            return (long)this.vertexCapacity * (long)this.stride + (long)this.indexCapacity * 4L;
        }
    }
}

