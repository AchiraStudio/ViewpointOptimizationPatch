/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.system.MemoryUtil
 */
package viewpoint.world;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.lwjgl.system.MemoryUtil;
import viewpoint.render.ChunkMeshData;
import viewpoint.world.Recipe;
import zombie.core.textures.TextureID;

public final class Cook {
    private static final float ON_WALL = 0.04f;
    private static final float ON_FLOOR = 0.006f;
    private static final ThreadLocal<Cooker> COOKER = ThreadLocal.withInitial(Cooker::new);

    static ChunkMeshData mesh(Recipe recipe) {
        Cooked cooked = Cook.vertices(recipe);
        if (cooked == null) {
            return null;
        }
        ChunkMeshData chunkMeshData = new ChunkMeshData(cooked.pages(), cooked.first(), cooked.count(), cooked.verts(), recipe.level, cooked.runs(), cooked.pageRuns());
        chunkMeshData.plants(cooked.plantBase(), cooked.plantFirst(), cooked.plantCount());
        chunkMeshData.models(cooked.modelBase(), cooked.modelRuns());
        for (int i = 0; i < recipe.batches.size(); ++i) {
            chunkMeshData.floorPage = recipe.batches.get((int)i).floor ? i : chunkMeshData.floorPage;
        }
        return chunkMeshData;
    }

    public static Cooked vertices(Recipe recipe) {
        Object object;
        Cooker cooker = COOKER.get();
        cooker.n = 0;
        cooker.plantFloats = 0;
        cooker.runCount = 0;
        cooker.owned = false;
        int n = recipe.batches.size();
        TextureID[] textureIDArray = new TextureID[n];
        int[] nArray = new int[n];
        int[] nArray2 = new int[n];
        int[] nArray3 = new int[n + 1];
        int[] nArray4 = new int[n];
        int[] nArray5 = new int[n];
        for (int i = 0; i < n; ++i) {
            object = recipe.batches.get(i);
            textureIDArray[i] = ((Recipe.Batch)object).page;
            nArray[i] = cooker.n / 14;
            nArray3[i] = cooker.runCount;
            nArray4[i] = cooker.plantFloats / 14;
            for (int j = 0; j < ((Recipe.Batch)object).ops.size(); ++j) {
                Recipe.Op op = ((Recipe.Batch)object).ops.get(j);
                if (op.kind == 4) {
                    cooker.plant(op);
                    continue;
                }
                int n2 = cooker.n;
                cooker.run(op);
                cooker.claim(op.owner, n2 / 14, cooker.n / 14, nArray3[i]);
            }
            nArray2[i] = cooker.n / 14 - nArray[i];
            nArray5[i] = cooker.plantFloats / 14 - nArray4[i];
        }
        nArray3[n] = cooker.runCount;
        int[] nArray6 = cooker.models(recipe.models);
        if (cooker.n == 0 && cooker.plantFloats == 0 && nArray6 == null) {
            return null;
        }
        object = MemoryUtil.memAllocFloat((int)(cooker.n + cooker.plantFloats + cooker.modelFloats)).put(cooker.out, 0, cooker.n).put(cooker.plants, 0, cooker.plantFloats).put(cooker.instances, 0, cooker.modelFloats).flip();
        return new Cooked(textureIDArray, nArray, nArray2, (FloatBuffer)object, cooker.owned ? Arrays.copyOf(cooker.runs, cooker.runCount * 3) : null, (int[])(cooker.owned ? nArray3 : null), cooker.n / 14, nArray4, nArray5, (cooker.n + cooker.plantFloats) / 14, nArray6);
    }

    static int put(float[] fArray, int n, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, float f13, int n2) {
        fArray[n++] = f;
        fArray[n++] = f2;
        fArray[n++] = f3;
        fArray[n++] = f4;
        fArray[n++] = f5;
        fArray[n++] = f6;
        fArray[n++] = f7;
        fArray[n++] = f8;
        fArray[n++] = f9;
        fArray[n++] = f10;
        fArray[n++] = f11;
        fArray[n++] = f12;
        fArray[n++] = f13;
        fArray[n++] = Float.intBitsToFloat(n2);
        return n;
    }

    private static boolean onWall(float[] fArray, int n) {
        boolean bl = true;
        boolean bl2 = true;
        for (int i = n; i < n + 24; i += 8) {
            if (Math.abs(fArray[i + 6]) > 0.3f) {
                return false;
            }
            bl &= Math.abs(fArray[i + 2] + 0.5f) < 0.04f;
            bl2 &= Math.abs(fArray[i] + 0.5f) < 0.04f;
        }
        return bl || bl2;
    }

    private static boolean onFloor(float[] fArray, int n) {
        for (int i = n; i < n + 24; i += 8) {
            if (!(Math.abs(fArray[i + 6]) < 0.9f) && !(Math.abs(fArray[i + 1]) > 0.006f)) continue;
            return false;
        }
        return true;
    }

    private Cook() {
    }

    public record Cooked(TextureID[] pages, int[] first, int[] count, FloatBuffer verts, int[] runs, int[] pageRuns, int plantBase, int[] plantFirst, int[] plantCount, int modelBase, int[] modelRuns) {
    }

    private static final class Cooker {
        float[] out = new float[114688];
        int n;
        int[] runs = new int[768];
        int runCount;
        boolean owned;
        float[] plants = new float[14336];
        int plantFloats;
        float[] instances = new float[3584];
        int modelFloats;

        private Cooker() {
        }

        int[] models(List<Recipe.Op> list) {
            this.modelFloats = 0;
            if (list == null || list.isEmpty()) {
                return null;
            }
            Recipe.Op[] opArray = list.toArray(new Recipe.Op[0]);
            Arrays.sort(opArray, Comparator.comparingInt(op -> op.model.index));
            if (opArray.length * 14 > this.instances.length) {
                this.instances = new float[opArray.length * 14 * 2];
            }
            int[] nArray = new int[opArray.length * 3];
            int n = 0;
            for (int i = 0; i < opArray.length; ++i) {
                Recipe.Op op2 = opArray[i];
                int n2 = this.modelFloats;
                this.instances[n2] = -op2.dx;
                this.instances[n2 + 1] = op2.height;
                this.instances[n2 + 2] = -op2.dy;
                this.instances[n2 + 3] = op2.cos;
                this.instances[n2 + 4] = op2.sin;
                this.instances[n2 + 5] = op2.scale;
                this.instances[n2 + 6] = Float.intBitsToFloat(op2.flags);
                this.instances[n2 + 7] = Float.intBitsToFloat(op2.square);
                this.instances[n2 + 8] = Float.intBitsToFloat(op2.model.index);
                Arrays.fill(this.instances, n2 + 9, n2 + 14, 0.0f);
                this.modelFloats += 14;
                if (n > 0 && nArray[(n - 1) * 3] == op2.model.index) {
                    int n3 = (n - 1) * 3 + 2;
                    nArray[n3] = nArray[n3] + 1;
                    continue;
                }
                nArray[n * 3] = op2.model.index;
                nArray[n * 3 + 1] = i;
                nArray[n * 3 + 2] = 1;
                ++n;
            }
            return Arrays.copyOf(nArray, n * 3);
        }

        void plant(Recipe.Op op) {
            int n = op.face;
            int n2 = Math.max(1, n);
            if (this.plantFloats + n2 * 14 > this.plants.length) {
                this.plants = Arrays.copyOf(this.plants, Math.max(this.plants.length * 2, this.plantFloats + n2 * 14));
            }
            for (int i = 0; i < n2; ++i) {
                int n3 = op.flags | (n == 0 ? 512 : (i | n << 3) << 16);
                int n4 = this.plantFloats;
                this.plants[n4++] = -op.dx;
                this.plants[n4++] = op.height;
                this.plants[n4++] = -op.dy;
                this.plants[n4++] = Float.intBitsToFloat(n3);
                System.arraycopy(op.map, 0, this.plants, n4, 4);
                System.arraycopy(op.card, 0, this.plants, n4 + 4, 4);
                Arrays.fill(this.plants, n4 + 8, this.plantFloats + 14, 0.0f);
                this.plantFloats += 14;
            }
        }

        void claim(int n, int n2, int n3, int n4) {
            if (n3 == n2) {
                return;
            }
            int n5 = (this.runCount - 1) * 3;
            if (this.runCount > n4 && this.runs[n5] == n && this.runs[n5 + 1] + this.runs[n5 + 2] == n2) {
                int n6 = n5 + 2;
                this.runs[n6] = this.runs[n6] + (n3 - n2);
                return;
            }
            if (this.runCount * 3 == this.runs.length) {
                this.runs = Arrays.copyOf(this.runs, this.runs.length * 2);
            }
            this.runs[this.runCount * 3] = n;
            this.runs[this.runCount * 3 + 1] = n2;
            this.runs[this.runCount * 3 + 2] = n3 - n2;
            ++this.runCount;
            this.owned |= n != -1;
        }

        void run(Recipe.Op op) {
            switch (op.kind) {
                case 0: {
                    this.emit(op);
                    break;
                }
                case 1: {
                    this.face(op);
                    break;
                }
                case 2: {
                    this.caps(op);
                    break;
                }
                default: {
                    this.reserve(op.raw.length / 14);
                    System.arraycopy(op.raw, 0, this.out, this.n, op.raw.length);
                    this.n += op.raw.length;
                }
            }
        }

        void reserve(int n) {
            int n2 = n * 14;
            if (this.n + n2 > this.out.length) {
                this.out = Arrays.copyOf(this.out, Math.max(this.out.length * 2, this.n + n2));
            }
        }

        private void caps(Recipe.Op op) {
            float[] fArray = op.map;
            float[] fArray2 = op.mesh.data;
            float f = op.dx;
            float f2 = op.dy;
            float f3 = op.height;
            this.reserve(op.mesh.vertCount);
            for (int i = 0; i < fArray2.length; i += 24) {
                float f4 = (fArray2[i + 5] + fArray2[i + 8 + 5] + fArray2[i + 16 + 5]) / 3.0f;
                float f5 = (fArray2[i + 7] + fArray2[i + 8 + 7] + fArray2[i + 16 + 7]) / 3.0f;
                if (Math.abs(f5) > 0.2f || Math.abs(f4) > 0.2f) continue;
                for (int j = i; j < i + 24; j += 8) {
                    this.n = Cook.put(this.out, this.n, -(f + fArray2[j]), f3 + fArray2[j + 1], -(f2 + fArray2[j + 2]), fArray[4] + fArray2[j + 3] * fArray[6], fArray[5] + fArray2[j + 4] * fArray[7], fArray[0], fArray[1], fArray[2], fArray[3], -fArray2[j + 5], fArray2[j + 6], -fArray2[j + 7], 0.0f, op.lift);
                }
            }
        }

        private void face(Recipe.Op op) {
            float[] fArray = op.map;
            float[] fArray2 = op.mesh.data;
            float f = op.dx;
            float f2 = op.dy;
            float f3 = op.height;
            float f4 = op.dirX;
            float f5 = op.dirZ;
            float f6 = op.fill;
            int n = op.face;
            int n2 = op.flags;
            this.reserve(op.mesh.vertCount);
            for (int i = 0; i < fArray2.length; i += 24) {
                boolean bl;
                float f7 = (fArray2[i + 5] + fArray2[i + 8 + 5] + fArray2[i + 16 + 5]) / 3.0f;
                float f8 = (fArray2[i + 7] + fArray2[i + 8 + 7] + fArray2[i + 16 + 7]) / 3.0f;
                float f9 = Math.abs(f7 * f4 + f8 * f5);
                float f10 = Math.abs(f7 * f5 + f8 * f4);
                boolean bl2 = f9 > 0.2f && (f5 > 0.5f ? f9 >= f10 : f9 > f10) ? true : (bl = false);
                if (n != 2 ? !bl : bl) continue;
                float f11 = n == 1 ? -1.0f : 1.0f;
                for (int j = i; j < i + 24; j += 8) {
                    float f12 = n == 2 ? -fArray2[j + 5] : -f4 * f11;
                    float f13 = n == 2 ? fArray2[j + 6] : 0.0f;
                    float f14 = n == 2 ? -fArray2[j + 7] : -f5 * f11;
                    this.n = Cook.put(this.out, this.n, -(f + fArray2[j]), f3 + fArray2[j + 1], -(f2 + fArray2[j + 2]), fArray[4] + fArray2[j + 3] * fArray[6], fArray[5] + fArray2[j + 4] * fArray[7], fArray[0], fArray[1], fArray[2], fArray[3], f12, f13, f14, n == 1 ? f6 : 0.0f, (n == 2 ? 0 : 1) + (n == 1 ? 8 : 0) + n2 + op.lift);
                }
            }
        }

        private void emit(Recipe.Op op) {
            float[] fArray = op.map;
            float[] fArray2 = op.mesh.data;
            float f = op.dx;
            float f2 = op.dy;
            float f3 = op.height;
            float f4 = fArray[8];
            float f5 = fArray[9];
            float f6 = fArray[10];
            int n = op.flags;
            this.reserve(op.mesh.vertCount);
            for (int i = 0; i < fArray2.length; i += 24) {
                int n2 = n == 0 && (Cook.onWall(fArray2, i) || Cook.onFloor(fArray2, i)) ? 2 : n;
                for (int j = i; j < i + 24; j += 8) {
                    this.n = Cook.put(this.out, this.n, -(f + fArray2[j]), f3 + fArray2[j + 1], -(f2 + fArray2[j + 2]), fArray[4] + fArray2[j + 3] * f4 * f5, fArray[5] + fArray2[j + 4] * f4 * f6, fArray[0], fArray[1], fArray[2], fArray[3], -fArray2[j + 5], fArray2[j + 6], -fArray2[j + 7], 0.0f, n2 + op.lift);
                }
            }
        }
    }
}

