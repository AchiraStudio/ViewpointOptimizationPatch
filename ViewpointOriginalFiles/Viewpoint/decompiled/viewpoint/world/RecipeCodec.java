/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.world;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import viewpoint.render.PackModel;
import viewpoint.render.PackModels;
import viewpoint.world.Cook;
import viewpoint.world.Recipe;
import viewpoint.world.TileMesh;
import zombie.core.textures.TextureID;

final class RecipeCodec {
    static final int VERSION = 3;
    static final int OLDEST = 2;
    private static final int UV_FIRST = 3;
    private static final int UV_COUNT = 6;

    static void write(Recipe recipe, DataOutputStream dataOutputStream) throws IOException {
        ArrayList<TileMesh> arrayList = new ArrayList<TileMesh>();
        IdentityHashMap<TileMesh, Integer> identityHashMap = new IdentityHashMap<TileMesh, Integer>();
        for (Recipe.Batch object : recipe.batches) {
            for (Recipe.Op op : object.ops) {
                if (op.mesh == null || identityHashMap.containsKey(op.mesh)) continue;
                identityHashMap.put(op.mesh, arrayList.size());
                arrayList.add(op.mesh);
            }
        }
        dataOutputStream.writeInt(3);
        dataOutputStream.writeInt(recipe.level);
        dataOutputStream.writeInt(arrayList.size());
        for (TileMesh tileMesh : arrayList) {
            RecipeCodec.writeFloats(dataOutputStream, tileMesh.data);
        }
        dataOutputStream.writeInt(recipe.batches.size());
        for (Recipe.Batch batch : recipe.batches) {
            dataOutputStream.writeUTF(RecipeCodec.pageName(batch.page));
            dataOutputStream.writeInt(batch.ops.size());
            for (Recipe.Op op : batch.ops) {
                RecipeCodec.writeOp(op, op.mesh == null ? -1 : (Integer)identityHashMap.get(op.mesh), dataOutputStream);
            }
        }
        dataOutputStream.writeInt(recipe.models == null ? 0 : recipe.models.size());
        for (int i = 0; recipe.models != null && i < recipe.models.size(); ++i) {
            Recipe.Op op = recipe.models.get(i);
            dataOutputStream.writeUTF(op.model.obj.getPath());
            RecipeCodec.writeOp(op, -1, dataOutputStream);
            dataOutputStream.writeFloat(op.cos);
            dataOutputStream.writeFloat(op.sin);
            dataOutputStream.writeFloat(op.scale);
            dataOutputStream.writeInt(op.square);
        }
    }

    static Decoded read(DataInputStream dataInputStream) throws IOException {
        int n;
        int n2;
        int n3 = dataInputStream.readInt();
        if (n3 < 2 || n3 > 3) {
            throw new IOException("recipe format " + n3 + ", this build reads 2 to 3");
        }
        Recipe recipe = new Recipe(dataInputStream.readInt());
        TileMesh[] tileMeshArray = new TileMesh[dataInputStream.readInt()];
        for (n2 = 0; n2 < tileMeshArray.length; ++n2) {
            tileMeshArray[n2] = new TileMesh(RecipeCodec.readFloats(dataInputStream));
        }
        n2 = dataInputStream.readInt();
        ArrayList<String> arrayList = new ArrayList<String>(n2);
        for (n = 0; n < n2; ++n) {
            Recipe.Batch batch = new Recipe.Batch();
            arrayList.add(dataInputStream.readUTF());
            int n4 = dataInputStream.readInt();
            for (int i = 0; i < n4; ++i) {
                batch.ops.add(RecipeCodec.readOp(dataInputStream, tileMeshArray));
            }
            recipe.batches.add(batch);
        }
        n = n3 >= 3 ? dataInputStream.readInt() : 0;
        for (int i = 0; i < n; ++i) {
            PackModel packModel = PackModels.of(new File(dataInputStream.readUTF()));
            Recipe.Op op = RecipeCodec.readOp(dataInputStream, tileMeshArray);
            op.model = packModel;
            op.cos = dataInputStream.readFloat();
            op.sin = dataInputStream.readFloat();
            op.scale = dataInputStream.readFloat();
            op.square = dataInputStream.readInt();
            if (recipe.models == null) {
                recipe.models = new ArrayList();
            }
            recipe.models.add(op);
        }
        PackModels.publish();
        return new Decoded(recipe, arrayList);
    }

    private static void writeOp(Recipe.Op op, int n, DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeByte(op.kind);
        dataOutputStream.writeByte(op.face);
        dataOutputStream.writeInt(op.flags);
        dataOutputStream.writeInt(op.lift);
        dataOutputStream.writeInt(n);
        RecipeCodec.writeFloats(dataOutputStream, op.map);
        RecipeCodec.writeFloats(dataOutputStream, op.raw);
        RecipeCodec.writeFloats(dataOutputStream, op.card);
        dataOutputStream.writeFloat(op.dx);
        dataOutputStream.writeFloat(op.dy);
        dataOutputStream.writeFloat(op.height);
        dataOutputStream.writeFloat(op.dirX);
        dataOutputStream.writeFloat(op.dirZ);
        dataOutputStream.writeFloat(op.fill);
    }

    private static Recipe.Op readOp(DataInputStream dataInputStream, TileMesh[] tileMeshArray) throws IOException {
        Recipe.Op op = new Recipe.Op();
        op.kind = dataInputStream.readByte();
        op.face = dataInputStream.readByte();
        op.flags = dataInputStream.readInt();
        op.lift = dataInputStream.readInt();
        int n = dataInputStream.readInt();
        op.mesh = n < 0 ? null : tileMeshArray[n];
        op.map = RecipeCodec.readFloats(dataInputStream);
        op.raw = RecipeCodec.readFloats(dataInputStream);
        op.card = RecipeCodec.readFloats(dataInputStream);
        op.dx = dataInputStream.readFloat();
        op.dy = dataInputStream.readFloat();
        op.height = dataInputStream.readFloat();
        op.dirX = dataInputStream.readFloat();
        op.dirZ = dataInputStream.readFloat();
        op.fill = dataInputStream.readFloat();
        return op;
    }

    private static void writeFloats(DataOutputStream dataOutputStream, float[] fArray) throws IOException {
        if (fArray == null) {
            dataOutputStream.writeInt(-1);
            return;
        }
        dataOutputStream.writeInt(fArray.length);
        for (float f : fArray) {
            dataOutputStream.writeFloat(f);
        }
    }

    private static float[] readFloats(DataInputStream dataInputStream) throws IOException {
        int n = dataInputStream.readInt();
        if (n < 0) {
            return null;
        }
        float[] fArray = new float[n];
        for (int i = 0; i < n; ++i) {
            fArray[i] = dataInputStream.readFloat();
        }
        return fArray;
    }

    static String recipeDigest(Recipe recipe, List<String> list) {
        Object object;
        int n;
        MessageDigest messageDigest = RecipeCodec.sha256();
        RecipeCodec.update(messageDigest, recipe.level);
        for (n = 0; n < recipe.batches.size(); ++n) {
            object = recipe.batches.get(n);
            RecipeCodec.update(messageDigest, list.get(n));
            RecipeCodec.update(messageDigest, ((Recipe.Batch)object).ops.size());
            for (Recipe.Op op : ((Recipe.Batch)object).ops) {
                RecipeCodec.updateOp(messageDigest, op);
            }
        }
        for (n = 0; recipe.models != null && n < recipe.models.size(); ++n) {
            object = recipe.models.get(n);
            RecipeCodec.update(messageDigest, ((Recipe.Op)object).model.obj.getPath());
            RecipeCodec.updateOp(messageDigest, (Recipe.Op)object);
            RecipeCodec.update(messageDigest, new float[]{((Recipe.Op)object).cos, ((Recipe.Op)object).sin, ((Recipe.Op)object).scale}, false);
            RecipeCodec.update(messageDigest, ((Recipe.Op)object).square);
        }
        return HexFormat.of().withUpperCase().formatHex(messageDigest.digest());
    }

    static String cookDigest(Cook.Cooked cooked, List<String> list) {
        MessageDigest messageDigest = RecipeCodec.sha256();
        if (cooked != null) {
            for (int i = 0; i < cooked.first().length; ++i) {
                RecipeCodec.update(messageDigest, list.get(i));
                RecipeCodec.update(messageDigest, cooked.first()[i]);
                RecipeCodec.update(messageDigest, cooked.count()[i]);
                RecipeCodec.update(messageDigest, cooked.plantFirst()[i]);
                RecipeCodec.update(messageDigest, cooked.plantCount()[i]);
            }
            RecipeCodec.update(messageDigest, cooked.plantBase());
            FloatBuffer floatBuffer = cooked.verts();
            int n = floatBuffer.position() + cooked.modelBase() * 14;
            for (int i = floatBuffer.position(); i < floatBuffer.limit(); ++i) {
                boolean bl;
                boolean bl2 = bl = i >= n && (i - n) % 14 == 8;
                if (bl) {
                    RecipeCodec.update(messageDigest, PackModels.get((int)Float.floatToRawIntBits((float)floatBuffer.get((int)i))).obj.getPath());
                    continue;
                }
                RecipeCodec.update(messageDigest, Float.floatToRawIntBits(floatBuffer.get(i)));
            }
        }
        return HexFormat.of().withUpperCase().formatHex(messageDigest.digest());
    }

    private static void updateOp(MessageDigest messageDigest, Recipe.Op op) {
        RecipeCodec.update(messageDigest, op.kind);
        RecipeCodec.update(messageDigest, op.face);
        RecipeCodec.update(messageDigest, op.flags);
        RecipeCodec.update(messageDigest, op.lift);
        RecipeCodec.update(messageDigest, op.mesh == null ? null : op.mesh.data, false);
        RecipeCodec.update(messageDigest, op.map, false);
        RecipeCodec.update(messageDigest, op.raw, true);
        RecipeCodec.update(messageDigest, op.card, false);
        RecipeCodec.update(messageDigest, Float.floatToRawIntBits(op.dx));
        RecipeCodec.update(messageDigest, Float.floatToRawIntBits(op.dy));
        RecipeCodec.update(messageDigest, Float.floatToRawIntBits(op.height));
        RecipeCodec.update(messageDigest, Float.floatToRawIntBits(op.dirX));
        RecipeCodec.update(messageDigest, Float.floatToRawIntBits(op.dirZ));
        RecipeCodec.update(messageDigest, Float.floatToRawIntBits(op.fill));
    }

    private static void update(MessageDigest messageDigest, float[] fArray, boolean bl) {
        if (fArray == null) {
            RecipeCodec.update(messageDigest, -1);
            return;
        }
        RecipeCodec.update(messageDigest, fArray.length);
        for (int i = 0; i < fArray.length; ++i) {
            boolean bl2;
            int n = i % 14;
            boolean bl3 = bl2 = bl && n >= 3 && n < 9;
            if (bl2) continue;
            RecipeCodec.update(messageDigest, Float.floatToRawIntBits(fArray[i]));
        }
    }

    private static void update(MessageDigest messageDigest, int n) {
        messageDigest.update((byte)(n >>> 24));
        messageDigest.update((byte)(n >>> 16));
        messageDigest.update((byte)(n >>> 8));
        messageDigest.update((byte)n);
    }

    private static void update(MessageDigest messageDigest, String string) {
        byte[] byArray = string.getBytes(StandardCharsets.UTF_8);
        RecipeCodec.update(messageDigest, byArray.length);
        messageDigest.update(byArray);
    }

    static String pageName(TextureID textureID) {
        String string = textureID == null ? null : textureID.getPathFileName();
        return string == null ? "(generated)" : string;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException("every Java runtime has SHA-256", noSuchAlgorithmException);
        }
    }

    private RecipeCodec() {
    }

    record Decoded(Recipe recipe, List<String> pages) {
    }
}

