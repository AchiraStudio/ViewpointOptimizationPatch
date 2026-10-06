/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.lwjgl.util.vector.Matrix4f
 *  zombie.core.skinnedmodel.model.ItemModelRenderer
 *  zombie.core.skinnedmodel.model.ModelMesh
 *  zombie.core.skinnedmodel.model.ModelSlotRenderData
 *  zombie.core.textures.Texture
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import java.util.Arrays;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import viewpoint.render.CharacterTextures;
import viewpoint.render.CorpseTextures;
import viewpoint.render.ModelMeshes;
import viewpoint.render.VehicleMaterials;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.textures.Texture;

public final class ModelDraws {
    public static final int COLOUR = 1;
    public static final int SHADOW = 2;
    public static final int OUTLINE_BEHIND = 4;
    public static final int MOTION = 8;
    public static final int TARGET = 16;
    public static final int STILL = 32;
    public static final int BONE_FLOATS = 12;
    static final int VALUES = 60;
    static final int MATERIAL = 16;
    static final int LIGHT = 20;
    static final int SPHERE = 24;
    static final int UV = 28;
    static final int OUTLINE = 32;
    static final int PREVIOUS = 36;
    static final int INDICES = 52;
    static final int FADE = 56;
    private static final float WHOLE_LO = 0.0f;
    private static final float WHOLE_HI = 2.0f;
    ModelMesh[] meshes = new ModelMesh[256];
    ModelInstance[] instances = new ModelInstance[256];
    Texture[] textures = new Texture[256];
    ItemModelRenderer[] items = new ItemModelRenderer[256];
    CharacterTextures.Composited[] slots = new CharacterTextures.Composited[64];
    int[] slotFirst = new int[64];
    int[] slotDraws = new int[64];
    float[] slotDistance = new float[64];
    int slotCount;
    float[] values = new float[15360];
    int[] palette = new int[256];
    int[] shadowPalette = new int[256];
    int[] cull = new int[256];
    int[] flags = new int[256];
    int[] material = new int[256];
    Object[] keys = new Object[256];
    int[] previous = new int[256];
    public final VehicleMaterials vehicles = new VehicleMaterials();
    float[] palettes = new float[49152];
    int count;
    int bones;
    private int[] copiedTo = new int[4096];
    private int[] copiedStamp = new int[4096];
    private int stamp = 1;
    boolean outlined;
    boolean targeted;
    private final Vector3f scratch = new Vector3f();

    void reset() {
        Arrays.fill(this.meshes, 0, this.count, null);
        Arrays.fill((Object[])this.instances, 0, this.count, null);
        Arrays.fill(this.textures, 0, this.count, null);
        Arrays.fill(this.items, 0, this.count, null);
        Arrays.fill(this.keys, 0, this.count, null);
        Arrays.fill(this.slots, 0, this.slotCount, null);
        this.count = 0;
        this.slotCount = 0;
        this.bones = 0;
        ++this.stamp;
        this.outlined = false;
        this.targeted = false;
        this.vehicles.reset();
    }

    public void slot(ModelSlotRenderData modelSlotRenderData, float f) {
        this.look(new CharacterTextures.Snapshot(modelSlotRenderData), modelSlotRenderData.modelData.size(), f);
    }

    public void look(CorpseTextures corpseTextures, float f) {
        this.look(corpseTextures, 0, f);
    }

    private void look(CharacterTextures.Composited composited, int n, float f) {
        if (this.slotCount == this.slots.length) {
            this.slots = Arrays.copyOf(this.slots, this.slotCount * 2);
            this.slotFirst = Arrays.copyOf(this.slotFirst, this.slotCount * 2);
            this.slotDraws = Arrays.copyOf(this.slotDraws, this.slotCount * 2);
            this.slotDistance = Arrays.copyOf(this.slotDistance, this.slotCount * 2);
        }
        this.slotFirst[this.slotCount] = this.count;
        this.slotDraws[this.slotCount] = n;
        this.slotDistance[this.slotCount] = f;
        this.slots[this.slotCount++] = composited;
    }

    public int palette(float[] fArray, int n) {
        return this.palette(fArray, 0, n);
    }

    public int palette(FloatBuffer floatBuffer, int n) {
        if ((this.bones + n) * 12 > this.palettes.length) {
            this.palettes = Arrays.copyOf(this.palettes, Math.max(this.palettes.length * 2, (this.bones + n) * 12));
        }
        int n2 = this.bones;
        for (int i = 0; i < n; ++i) {
            floatBuffer.get(i * 16, this.palettes, (n2 + i) * 12, 12);
        }
        this.bones += n;
        return n2;
    }

    public int palette(org.lwjgl.util.vector.Matrix4f[] matrix4fArray) {
        int n = matrix4fArray.length;
        if ((this.bones + n) * 12 > this.palettes.length) {
            this.palettes = Arrays.copyOf(this.palettes, Math.max(this.palettes.length * 2, (this.bones + n) * 12));
        }
        int n2 = this.bones;
        float[] fArray = this.palettes;
        int n3 = 0;
        int n4 = n2 * 12;
        while (n3 < n) {
            org.lwjgl.util.vector.Matrix4f matrix4f = matrix4fArray[n3];
            fArray[n4] = matrix4f.m00;
            fArray[n4 + 1] = matrix4f.m01;
            fArray[n4 + 2] = matrix4f.m02;
            fArray[n4 + 3] = matrix4f.m03;
            fArray[n4 + 4] = matrix4f.m10;
            fArray[n4 + 5] = matrix4f.m11;
            fArray[n4 + 6] = matrix4f.m12;
            fArray[n4 + 7] = matrix4f.m13;
            fArray[n4 + 8] = matrix4f.m20;
            fArray[n4 + 9] = matrix4f.m21;
            fArray[n4 + 10] = matrix4f.m22;
            fArray[n4 + 11] = matrix4f.m23;
            ++n3;
            n4 += 12;
        }
        this.bones += n;
        return n2;
    }

    public int lastPose(ModelDraws modelDraws, int n, int n2) {
        int n3 = modelDraws.palette[n];
        if (n3 < 0 || n3 + n2 > modelDraws.bones) {
            return -1;
        }
        if (n3 < this.copiedTo.length && this.copiedStamp[n3] == this.stamp) {
            return this.copiedTo[n3];
        }
        int n4 = this.palette(modelDraws.palettes, n3, n2);
        if (n3 >= this.copiedTo.length) {
            this.copiedTo = Arrays.copyOf(this.copiedTo, Math.max(n3 + 1, this.copiedTo.length * 2));
            this.copiedStamp = Arrays.copyOf(this.copiedStamp, this.copiedTo.length);
        }
        this.copiedTo[n3] = n4;
        this.copiedStamp[n3] = this.stamp;
        return n4;
    }

    private int palette(float[] fArray, int n, int n2) {
        if ((this.bones + n2) * 12 > this.palettes.length) {
            this.palettes = Arrays.copyOf(this.palettes, Math.max(this.palettes.length * 2, (this.bones + n2) * 12));
        }
        int n3 = this.bones;
        System.arraycopy(fArray, n * 12, this.palettes, n3 * 12, n2 * 12);
        this.bones += n2;
        return n3;
    }

    private void indices(int n) {
        int n2 = n * 60 + 52;
        this.values[n2] = this.palette[n];
        this.values[n2 + 1] = this.shadowPalette[n];
        this.values[n2 + 2] = this.previous[n];
        this.values[n2 + 3] = this.flags[n];
    }

    public void key(int n, Object object) {
        this.keys[n] = object;
    }

    public Object key(int n) {
        return this.keys[n];
    }

    public int flags(int n) {
        return this.flags[n];
    }

    public void motion(int n, ModelDraws modelDraws, int n2, int n3, boolean bl) {
        if (n3 > 0) {
            int n4;
            int n5 = n4 = bl ? this.palette[n] : this.lastPose(modelDraws, n2, n3);
            if (n4 < 0) {
                return;
            }
            this.previous[n] = n4;
        }
        System.arraycopy(modelDraws.values, n2 * 60, this.values, n * 60 + 36, 16);
        int n6 = n;
        this.flags[n6] = this.flags[n6] | 8;
        this.indices(n);
    }

    public void collapse(int n, float f, float f2, float f3) {
        int n2 = n * 12;
        Arrays.fill(this.palettes, n2, n2 + 12, 0.0f);
        this.palettes[n2 + 3] = f;
        this.palettes[n2 + 7] = f2;
        this.palettes[n2 + 11] = f3;
    }

    public int add(ModelMesh modelMesh, ModelInstance modelInstance, Texture texture, Matrix4f matrix4f, int n, int n2, int n3, int n4) {
        if (this.count == this.meshes.length) {
            this.meshes = Arrays.copyOf(this.meshes, this.count * 2);
            this.instances = Arrays.copyOf(this.instances, this.count * 2);
            this.textures = Arrays.copyOf(this.textures, this.count * 2);
            this.items = Arrays.copyOf(this.items, this.count * 2);
            this.values = Arrays.copyOf(this.values, this.count * 2 * 60);
            this.palette = Arrays.copyOf(this.palette, this.count * 2);
            this.shadowPalette = Arrays.copyOf(this.shadowPalette, this.count * 2);
            this.cull = Arrays.copyOf(this.cull, this.count * 2);
            this.flags = Arrays.copyOf(this.flags, this.count * 2);
            this.material = Arrays.copyOf(this.material, this.count * 2);
            this.keys = Arrays.copyOf(this.keys, this.count * 2);
            this.previous = Arrays.copyOf(this.previous, this.count * 2);
        }
        int n5 = this.count++;
        this.meshes[n5] = modelMesh;
        this.instances[n5] = modelInstance;
        this.textures[n5] = texture;
        matrix4f.get(this.values, n5 * 60);
        this.uv(n5, 0.0f, 0.0f, 1.0f, 1.0f);
        this.fade(n5, 0.0f, 2.0f);
        this.values[n5 * 60 + 32 + 3] = 0.0f;
        this.palette[n5] = n;
        this.shadowPalette[n5] = n2;
        this.cull[n5] = n3;
        this.flags[n5] = n4;
        this.material[n5] = -1;
        this.previous[n5] = -1;
        this.indices(n5);
        return n5;
    }

    public void material(int n, float f, float f2, float f3, float f4) {
        int n2 = n * 60 + 16;
        this.values[n2] = f;
        this.values[n2 + 1] = f2;
        this.values[n2 + 2] = f3;
        this.values[n2 + 3] = f4;
    }

    public void light(int n, float f, float f2, float f3, boolean bl) {
        int n2 = n * 60 + 20;
        this.values[n2] = f;
        this.values[n2 + 1] = f2;
        this.values[n2 + 2] = f3;
        this.values[n2 + 3] = bl ? 1.0f : 0.0f;
    }

    public void uv(int n, float f, float f2, float f3, float f4) {
        int n2 = n * 60 + 28;
        this.values[n2] = f;
        this.values[n2 + 1] = f2;
        this.values[n2 + 2] = f3;
        this.values[n2 + 3] = f4;
    }

    public void fade(int n, float f, float f2) {
        int n2 = n * 60 + 56;
        this.values[n2] = f;
        this.values[n2 + 1] = f2;
        this.values[n2 + 2] = 0.0f;
        this.values[n2 + 3] = 0.0f;
    }

    public void item(int n, ItemModelRenderer itemModelRenderer) {
        this.items[n] = itemModelRenderer;
    }

    public void outline(int n, float f, float f2, float f3, float f4, boolean bl) {
        int n2 = n * 60 + 32;
        this.values[n2] = f;
        this.values[n2 + 1] = f2;
        this.values[n2 + 2] = f3;
        this.values[n2 + 3] = Math.max(f4, 0.001f);
        this.flags[n] = bl ? this.flags[n] | 4 : this.flags[n] & 0xFFFFFFFB;
        this.indices(n);
        this.outlined = true;
    }

    public void target(int n) {
        for (int i = n; i < this.count; ++i) {
            int n2 = i;
            this.flags[n2] = this.flags[n2] | 0x10;
            this.indices(i);
            this.targeted = true;
        }
    }

    public void vehicle(int n, int n2) {
        this.material[n] = n2;
    }

    public void bounds(int n, Matrix4f matrix4f, Vector3f vector3f, Vector3f vector3f2, float f) {
        boolean bl = vector3f.x <= vector3f2.x;
        float f2 = bl ? vector3f.distance((Vector3fc)vector3f2) * 0.5f : 1.0f;
        Vector3f vector3f3 = bl ? this.scratch.set((Vector3fc)vector3f).add((Vector3fc)vector3f2).mul(0.5f) : this.scratch.zero();
        matrix4f.transformPosition(vector3f3);
        float f3 = vector3f3.x;
        float f4 = vector3f3.y;
        float f5 = vector3f3.z;
        matrix4f.getScale(this.scratch);
        this.bounds(n, f3, f4, f5, f2 * Math.max(this.scratch.x, Math.max(this.scratch.y, this.scratch.z)) * f);
    }

    public void bounds(int n, float f, float f2, float f3, float f4) {
        int n2 = n * 60 + 24;
        this.values[n2] = f;
        this.values[n2 + 1] = f2;
        this.values[n2 + 2] = f3;
        this.values[n2 + 3] = f4;
    }

    public int count() {
        return this.count;
    }

    public static boolean refused(ModelMesh modelMesh) {
        return ModelMeshes.refused.contains(modelMesh) || !ModelMeshes.available();
    }
}

