/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.system.MemoryUtil
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;
import viewpoint.render.MeshArena;
import viewpoint.render.PackModels;
import viewpoint.render.Retirement;
import zombie.core.textures.TextureID;

public final class ChunkMeshData
implements Retirement.Freeable {
    public final TextureID[] pages;
    public final int[] first;
    public final int[] count;
    final int vertCount;
    public final int level;
    public final int[] runs;
    public final int[] pageRuns;
    public int floorPage = -1;
    int plantBase;
    int[] plantFirst;
    int[] plantCount;
    int modelBase;
    int[] modelRuns;
    private boolean modelsHeld;
    private FloatBuffer verts;
    private final ByteBuffer lightA = BufferUtils.createByteBuffer((int)3200);
    private final ByteBuffer lightB = BufferUtils.createByteBuffer((int)3200);
    private volatile ByteBuffer published;
    private boolean lightFilled;
    private static final ByteBuffer DARK = BufferUtils.createByteBuffer((int)3200);
    public int arenaFirst = -1;
    public int lightSlot = -1;
    private ByteBuffer uploaded;

    public ChunkMeshData(TextureID[] textureIDArray, int[] nArray, int[] nArray2, FloatBuffer floatBuffer, int n, int[] nArray3, int[] nArray4) {
        this.pages = textureIDArray;
        this.first = nArray;
        this.count = nArray2;
        this.verts = floatBuffer;
        this.vertCount = floatBuffer.remaining() / 14;
        this.level = n;
        this.runs = nArray3;
        this.pageRuns = nArray4;
    }

    public ChunkMeshData(TextureID[] textureIDArray, int[] nArray, int[] nArray2, float[] fArray, int n) {
        this(textureIDArray, nArray, nArray2, MemoryUtil.memAllocFloat((int)fArray.length).put(fArray).flip(), n, null, null);
    }

    public int vertices() {
        return this.vertCount;
    }

    public void plants(int n, int[] nArray, int[] nArray2) {
        for (int n2 : nArray2) {
            if (n2 <= 0) continue;
            this.plantBase = n;
            this.plantFirst = nArray;
            this.plantCount = nArray2;
            return;
        }
    }

    public void models(int n, int[] nArray) {
        if (nArray == null) {
            return;
        }
        this.modelBase = n;
        this.modelRuns = nArray;
        this.modelsHeld = true;
        for (int i = 0; i < nArray.length; i += 3) {
            PackModels.get(nArray[i]).hold();
        }
    }

    public boolean hasModels() {
        return this.modelRuns != null;
    }

    public ByteBuffer spareLight() {
        return this.published == this.lightA ? this.lightB : this.lightA;
    }

    public void publishLight(ByteBuffer byteBuffer) {
        this.published = byteBuffer;
        this.lightFilled = true;
    }

    boolean hasLight() {
        return this.lightFilled;
    }

    public void retire() {
        Retirement.retire(this);
    }

    public void prepare() {
        ByteBuffer byteBuffer = this.published;
        if (this.arenaFirst < 0) {
            this.upload(byteBuffer);
        } else if (byteBuffer != null && byteBuffer != this.uploaded) {
            MeshArena.uploadLight(this.lightSlot, byteBuffer);
            this.uploaded = byteBuffer;
        }
    }

    private void upload(ByteBuffer byteBuffer) {
        int n = MeshArena.takeSlot();
        if (n < 0) {
            return;
        }
        int n2 = MeshArena.upload(this.verts, this.vertCount);
        if (n2 < 0) {
            MeshArena.freeSlot(n);
            return;
        }
        this.arenaFirst = n2;
        this.lightSlot = n;
        MemoryUtil.memFree((FloatBuffer)this.verts);
        this.verts = null;
        MeshArena.uploadLight(this.lightSlot, byteBuffer != null ? byteBuffer : DARK);
        this.uploaded = byteBuffer;
    }

    @Override
    public void free() {
        this.discard();
        if (this.arenaFirst >= 0) {
            MeshArena.free(this.arenaFirst, this.vertCount);
            MeshArena.freeSlot(this.lightSlot);
            this.lightSlot = -1;
            this.arenaFirst = -1;
        }
    }

    public void discard() {
        if (this.verts != null) {
            MemoryUtil.memFree((FloatBuffer)this.verts);
            this.verts = null;
        }
        if (this.modelsHeld) {
            this.modelsHeld = false;
            for (int i = 0; i < this.modelRuns.length; i += 3) {
                PackModels.get(this.modelRuns[i]).release();
            }
        }
    }

    static {
        for (int i = 1600; i < 3200; i += 4) {
            DARK.put(i, (byte)-1);
        }
    }
}

