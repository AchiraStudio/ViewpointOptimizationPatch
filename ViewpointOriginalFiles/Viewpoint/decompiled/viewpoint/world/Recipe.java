/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoObject
 *  zombie.iso.objects.IsoWorldInventoryObject
 */
package viewpoint.world;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import viewpoint.packs.PackBind;
import viewpoint.render.FloorArt;
import viewpoint.render.PackModel;
import viewpoint.world.BodyCards;
import viewpoint.world.Cook;
import viewpoint.world.TileMesh;
import zombie.core.textures.TextureID;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoWorldInventoryObject;

public final class Recipe {
    public static final int FACE_FRONT = 0;
    public static final int FACE_BACK = 1;
    public static final int FACE_CAPS = 2;
    final int level;
    final ArrayList<Batch> batches = new ArrayList();
    final IdentityHashMap<TextureID, Batch> pages = new IdentityHashMap();
    boolean pending;
    boolean picturePending;
    ArrayList<Op> models;
    ArrayList<PackModel> waiting;
    ArrayList<IsoObject> modelled;
    ArrayList<IsoWorldInventoryObject> modelledItems;
    BodyCards bodies;
    FloorArt floor;
    private long fingerprint;
    static final long SEED = -7046029254386353131L;

    public Recipe(int n) {
        this.level = n;
    }

    long fingerprint() {
        if (this.fingerprint != 0L) {
            return this.fingerprint;
        }
        long l = Recipe.mix(-7046029254386353131L, this.level);
        for (Batch batch : this.batches) {
            l = Recipe.mix(l, System.identityHashCode(batch.page));
            l = Recipe.mix(l, batch.ops.size());
            for (Op op : batch.ops) {
                l = Recipe.mix(l, (long)op.kind << 32 | (long)op.face & 0xFFFFFFFFL);
                l = Recipe.mix(l, (long)op.flags << 32 | (long)op.lift & 0xFFFFFFFFL);
                l = Recipe.mix(l, op.mesh == null ? 0L : op.mesh.hash);
                l = Recipe.mix(l, op.map);
                l = Recipe.mix(l, op.raw);
                l = Recipe.mix(l, op.card);
                l = Recipe.mix(l, (long)Float.floatToRawIntBits(op.dx) << 32 | (long)Float.floatToRawIntBits(op.dy) & 0xFFFFFFFFL);
                l = Recipe.mix(l, (long)Float.floatToRawIntBits(op.height) << 32 | (long)Float.floatToRawIntBits(op.fill) & 0xFFFFFFFFL);
                l = Recipe.mix(l, (long)Float.floatToRawIntBits(op.dirX) << 32 | (long)Float.floatToRawIntBits(op.dirZ) & 0xFFFFFFFFL);
                l = Recipe.mix(l, op.owner);
            }
        }
        l = this.mixModels(l);
        l ^= l >>> 33;
        l *= -49064778989728563L;
        this.fingerprint = (l ^= l >>> 33) == 0L ? 1L : l;
        return this.fingerprint;
    }

    private long mixModels(long l) {
        if (this.models == null) {
            return l;
        }
        l = Recipe.mix(l, this.models.size());
        for (Op op : this.models) {
            l = Recipe.mix(l, (long)op.model.index << 32 | (long)op.square & 0xFFFFFFFFL);
            l = Recipe.mix(l, (long)op.flags << 32 | (long)op.owner & 0xFFFFFFFFL);
            l = Recipe.mix(l, new float[]{op.dx, op.dy, op.height, op.cos, op.sin, op.scale});
        }
        return l;
    }

    static long mix(long l, long l2) {
        l = (l ^ l2) * -7046029254386353131L;
        return l ^ l >>> 29;
    }

    static long mix(long l, float[] fArray) {
        if (fArray == null) {
            return Recipe.mix(l, -1L);
        }
        l = Recipe.mix(l, fArray.length);
        for (float f : fArray) {
            l = Recipe.mix(l, Float.floatToRawIntBits(f));
        }
        return l;
    }

    public static void place(Recipe recipe, TextureID textureID, TileMesh tileMesh, float[] fArray, float f, float f2, float f3, int n) {
        Recipe.placed((Recipe)recipe, (TextureID)textureID, (int)0, (TileMesh)tileMesh, (float[])fArray, (float)f, (float)f2, (float)f3).flags = n;
    }

    public static void placeFace(Recipe recipe, TextureID textureID, TileMesh tileMesh, float[] fArray, float f, float f2, float f3, float f4, float f5, int n, float f6) {
        Op op = Recipe.placed(recipe, textureID, 1, tileMesh, fArray, f, f2, f3);
        op.dirX = f4;
        op.dirZ = f5;
        op.face = n;
        op.fill = f6;
    }

    public static void model(Recipe recipe, PackBind packBind, boolean bl, float f, float f2, float f3, int n, int n2, int n3) {
        Op op = new Op();
        op.kind = 5;
        op.model = packBind.model;
        op.dx = f + packBind.x;
        op.dy = f2 + packBind.y;
        op.height = f3 + packBind.z;
        op.cos = bl ? packBind.cos : 1.0f;
        op.sin = bl ? packBind.sin : 0.0f;
        op.scale = packBind.scale;
        op.square = n;
        op.flags = n2;
        op.owner = n3;
        if (recipe.models == null) {
            recipe.models = new ArrayList();
        }
        recipe.models.add(op);
    }

    static void placeCaps(Recipe recipe, TextureID textureID, TileMesh tileMesh, float[] fArray, float f, float f2, float f3) {
        Recipe.placed(recipe, textureID, 2, tileMesh, fArray, f, f2, f3);
    }

    public static void bakedFloor(Recipe recipe, float f, float f2, float f3, float f4, float f5, float[] fArray) {
        Batch batch = null;
        for (Batch batch2 : recipe.batches) {
            batch = batch2.floor ? batch2 : batch;
        }
        if (batch == null) {
            batch = new Batch();
            batch.floor = true;
            recipe.batches.add(batch);
        }
        Op op = new Op();
        op.kind = 3;
        op.raw = Recipe.floorVertices(f, f2, f3, f4, f5, fArray);
        batch.ops.add(op);
    }

    public static void billboard(Recipe recipe, TextureID textureID, float f, float f2, float f3, float f4, float f5, float[] fArray, int n) {
        Recipe.card(recipe, textureID, f, f2, f3, -f4, f4, 0.0f, f5, fArray, 0x80 | n);
    }

    public static void card(Recipe recipe, TextureID textureID, float f, float f2, float f3, float f4, float f5, float f6, float f7, float[] fArray, int n) {
        float[] fArray2 = new float[84];
        int[] nArray = new int[]{0, 1, 2, 0, 2, 3};
        int n2 = 0;
        for (int n3 : nArray) {
            boolean bl = n3 == 1 || n3 == 2;
            boolean bl2 = n3 >= 2;
            n2 = Cook.put(fArray2, n2, -f, f3 + (bl2 ? f7 : f6), -f2, bl ? fArray[2] : fArray[0], bl2 ? fArray[1] : fArray[3], fArray[0], fArray[1], fArray[2], fArray[3], 0.0f, 0.5f, 0.0f, bl ? f5 : f4, n);
        }
        Recipe.placed((Recipe)recipe, (TextureID)textureID, (int)3, null, null, (float)f, (float)f2, (float)f3).raw = fArray2;
    }

    static float[] floorVertices(float f, float f2, float f3, float f4, float f5, float[] fArray) {
        float[] fArray2 = new float[84];
        int[] nArray = new int[]{0, 1, 2, 0, 2, 3};
        int n = 0;
        for (int n2 : nArray) {
            boolean bl = n2 == 1 || n2 == 2;
            boolean bl2 = n2 >= 2;
            n = Cook.put(fArray2, n, -(bl ? f3 : f), f5, -(bl2 ? f4 : f2), bl ? fArray[2] : fArray[0], bl2 ? fArray[3] : fArray[1], 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 68);
        }
        return fArray2;
    }

    private static Op placed(Recipe recipe, TextureID textureID, int n, TileMesh tileMesh, float[] fArray, float f, float f2, float f3) {
        Batch batch = recipe.pages.get(textureID);
        if (batch == null) {
            batch = new Batch();
            batch.page = textureID;
            recipe.pages.put(textureID, batch);
            recipe.batches.add(batch);
        }
        Op op = new Op();
        op.kind = n;
        op.mesh = tileMesh;
        op.map = fArray;
        op.dx = f;
        op.dy = f2;
        op.height = f3;
        batch.ops.add(op);
        return op;
    }

    static final class Batch {
        TextureID page;
        boolean floor;
        final ArrayList<Op> ops = new ArrayList();

        Batch() {
        }
    }

    static final class Op {
        static final int EMIT = 0;
        static final int FACE = 1;
        static final int CAPS = 2;
        static final int RAW = 3;
        static final int PLANT = 4;
        static final int MODEL = 5;
        int kind;
        int face;
        int flags;
        int lift;
        int owner = -1;
        TileMesh mesh;
        float[] map;
        float[] raw;
        float[] card;
        float dx;
        float dy;
        float height;
        float dirX;
        float dirZ;
        float fill;
        PackModel model;
        float cos;
        float sin;
        float scale;
        int square;

        Op() {
        }
    }
}

