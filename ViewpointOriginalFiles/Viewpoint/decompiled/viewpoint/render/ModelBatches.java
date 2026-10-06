/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL43
 *  zombie.core.textures.Texture
 */
package viewpoint.render;

import java.nio.IntBuffer;
import java.util.Arrays;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import viewpoint.platform.Profile;
import viewpoint.render.FrameStream;
import viewpoint.render.ModelDraws;
import viewpoint.render.ModelMeshes;
import zombie.core.textures.Texture;

final class ModelBatches {
    private static final int COMMAND = 5;
    private static final int RUN = 5;
    private ModelDraws draws;
    private ModelMeshes.Held[] held;
    private Texture[] textures;
    private long[] keys = new long[256];
    private int[] order = new int[256];
    private int[] runs = new int[320];
    private int[] slotDraws = new int[64];
    private int runCount;
    private int slotTotal;
    private int[] rank = new int[256];
    private int[] picked = new int[64];
    private int count;
    int runsDrawn;
    long elementsDrawn;
    private IntBuffer instances = BufferUtils.createIntBuffer((int)1024);
    private IntBuffer commands = BufferUtils.createIntBuffer((int)1280);

    ModelBatches() {
    }

    void order(ModelDraws modelDraws, ModelMeshes.Held[] heldArray, Texture[] textureArray) {
        int n;
        this.draws = modelDraws;
        this.held = heldArray;
        this.textures = textureArray;
        if (this.keys.length < modelDraws.count) {
            this.keys = new long[modelDraws.meshes.length];
            this.order = new int[modelDraws.meshes.length];
            this.rank = new int[modelDraws.meshes.length];
        }
        this.count = 0;
        for (n = 0; n < modelDraws.count; ++n) {
            Texture texture = textureArray[n];
            if (heldArray[n] == null || texture == null || !texture.isReady()) continue;
            this.keys[this.count++] = (long)Math.min(modelDraws.material[n] + 1, 255) << 55 | (long)(heldArray[n].layout.index & 7) << 52 | (long)(modelDraws.cull[n] == 0 ? 0 : (modelDraws.cull[n] == 1028 ? 1 : 2)) << 50 | (long)(System.identityHashCode(texture) & 0x3FFF) << 36 | (long)(heldArray[n].id & 0xFFFF) << 20 | (long)n;
        }
        Arrays.sort(this.keys, 0, this.count);
        Arrays.fill(this.rank, 0, modelDraws.count, -1);
        for (n = 0; n < this.count; ++n) {
            this.order[n] = (int)(this.keys[n] & 0xFFFFFL);
            this.rank[this.order[n]] = n;
        }
    }

    int draw(Filter filter, State state) {
        return this.inOrder(this.order, this.count, filter, state);
    }

    int draw(int[] nArray, int n, Filter filter, State state) {
        int n2;
        if (nArray == null) {
            return this.draw(filter, state);
        }
        this.picked = ModelBatches.grow(this.picked, n);
        int n3 = 0;
        for (n2 = 0; n2 < n; ++n2) {
            int n4 = this.rank[nArray[n2]];
            if (n4 < 0) continue;
            this.picked[n3++] = n4;
        }
        Arrays.sort(this.picked, 0, n3);
        for (n2 = 0; n2 < n3; ++n2) {
            this.picked[n2] = this.order[this.picked[n2]];
        }
        return this.inOrder(this.picked, n3, filter, state);
    }

    private int inOrder(int[] nArray, int n, Filter filter, State state) {
        this.instances.clear();
        this.commands.clear();
        this.slotTotal = 0;
        this.runCount = 0;
        int n2 = 0;
        int n3 = -1;
        int n4 = -1;
        long l = 0L;
        for (int i = 0; i < n; ++i) {
            int n5;
            int n6 = nArray[i];
            if (!filter.take(n6)) continue;
            boolean bl = n3 < 0 || !this.alike(n3, n6);
            int n7 = n5 = bl ? -1 : this.slot(n6);
            if (n5 < 0) {
                this.run(n6);
                n5 = this.slot(n6);
                bl = true;
            }
            this.room(1, 1);
            if (!bl && this.held[n3] == this.held[n6] && n5 == n4) {
                int n8 = this.commands.position() - 5 + 1;
                this.commands.put(n8, this.commands.get(n8) + 1);
            } else {
                ModelMeshes.Held held = this.held[n6];
                this.commands.put(held.elements).put(1).put(held.firstIndex).put(held.firstVertex).put(this.instances.position());
                int n9 = (this.runCount - 1) * 5 + 1;
                this.runs[n9] = this.runs[n9] + 1;
            }
            this.instances.put(n6 | n5 << 24);
            l += (long)this.held[n6].elements;
            ++n2;
            n3 = n6;
            n4 = n5;
        }
        this.runsDrawn = this.runCount;
        this.elementsDrawn = l;
        if (n2 == 0) {
            return 0;
        }
        this.issue(state);
        return n2;
    }

    private void run(int n) {
        this.runs = ModelBatches.grow(this.runs, (this.runCount + 1) * 5);
        int n2 = this.runCount++ * 5;
        this.runs[n2] = this.commands.position() / 5;
        this.runs[n2 + 1] = 0;
        this.runs[n2 + 2] = n;
        this.runs[n2 + 3] = this.slotTotal;
        this.runs[n2 + 4] = 0;
    }

    private int slot(int n) {
        int n2 = (this.runCount - 1) * 5;
        int n3 = this.runs[n2 + 3];
        int n4 = this.runs[n2 + 4];
        for (int i = 0; i < n4; ++i) {
            if (this.textures[this.slotDraws[n3 + i]] != this.textures[n]) continue;
            return i;
        }
        if (n4 == (this.draws.material[n] < 0 ? 16 : 1)) {
            return -1;
        }
        this.slotDraws = ModelBatches.grow(this.slotDraws, n3 + n4 + 1);
        this.slotDraws[n3 + n4] = n;
        int n5 = n2 + 4;
        this.runs[n5] = this.runs[n5] + 1;
        ++this.slotTotal;
        return n4;
    }

    private void issue(State state) {
        this.instances.flip();
        this.commands.flip();
        long l = FrameStream.put(this.instances, 4);
        int n = FrameStream.buffer();
        long l2 = FrameStream.put(this.commands, 4);
        GL15.glBindBuffer((int)36671, (int)FrameStream.buffer());
        for (int i = 0; i < this.runCount; ++i) {
            int n2 = i * 5;
            int n3 = this.runs[n2];
            int n4 = this.runs[n2 + 1];
            int n5 = this.runs[n2 + 2];
            GL30.glBindVertexArray((int)this.held[n5].layout.vao);
            GL43.glBindVertexBuffer((int)6, (int)n, (long)l, (int)4);
            state.set(n5);
            for (int j = 0; j < this.runs[n2 + 4]; ++j) {
                state.texture(j, this.slotDraws[this.runs[n2 + 3] + j]);
            }
            GL43.glMultiDrawElementsIndirect((int)4, (int)5125, (long)(l2 + (long)n3 * 5L * 4L), (int)n4, (int)20);
            ++Profile.draws;
        }
        GL30.glBindVertexArray((int)0);
        GL15.glBindBuffer((int)36671, (int)0);
    }

    private boolean alike(int n, int n2) {
        return this.draws.material[n] == this.draws.material[n2] && this.held[n].layout == this.held[n2].layout && this.draws.cull[n] == this.draws.cull[n2] && (this.draws.material[n] < 0 || this.textures[n] == this.textures[n2]);
    }

    private void room(int n, int n2) {
        IntBuffer intBuffer;
        if (this.instances.remaining() < n) {
            intBuffer = BufferUtils.createIntBuffer((int)(this.instances.capacity() * 2));
            this.instances.flip();
            this.instances = intBuffer.put(this.instances);
        }
        if (this.commands.remaining() < n2 * 5) {
            intBuffer = BufferUtils.createIntBuffer((int)(this.commands.capacity() * 2));
            this.commands.flip();
            this.commands = intBuffer.put(this.commands);
        }
    }

    private static int[] grow(int[] nArray, int n) {
        return nArray.length >= n ? nArray : Arrays.copyOf(nArray, Math.max(n, nArray.length * 2));
    }

    static interface Filter {
        public boolean take(int var1);
    }

    static interface State {
        public void set(int var1);

        default public void texture(int n, int n2) {
        }
    }
}

