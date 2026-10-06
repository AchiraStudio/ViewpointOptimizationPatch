/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 *  zombie.core.skinnedmodel.model.ItemModelRenderer
 *  zombie.core.textures.Texture
 *  zombie.inventory.InventoryItem
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.IsoWallBloodSplat
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.SpriteModel
 *  zombie.iso.objects.IsoTree
 *  zombie.iso.objects.IsoWindow
 *  zombie.iso.objects.IsoWorldInventoryObject
 *  zombie.iso.sprite.IsoSprite
 *  zombie.iso.sprite.IsoSpriteInstance
 *  zombie.scripting.ScriptManager
 *  zombie.util.list.PZArrayList
 */
package viewpoint.world;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import viewpoint.visibility.Edges;
import viewpoint.visibility.Owner;
import viewpoint.world.BodyCards;
import viewpoint.world.FloorDecals;
import viewpoint.world.FloorGather;
import viewpoint.world.FloorSpans;
import viewpoint.world.PackGather;
import viewpoint.world.Recipe;
import viewpoint.world.TileMesh;
import viewpoint.world.TileMeshes;
import viewpoint.world.WallMesher;
import zombie.core.Core;
import zombie.core.properties.PropertyContainer;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWallBloodSplat;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteModel;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.scripting.ScriptManager;
import zombie.util.list.PZArrayList;

public final class WorldMesher {
    private static final int MAX_LAYER = 3;
    private static final float STOREY_PIXELS = 96.0f;
    private static boolean baked;
    static int lift;
    static int owner;
    static int square;
    private static Recipe recipe;
    private static final IdentityHashMap<TextureID, Recipe.Batch> byPage;
    private static final IdentityHashMap<Texture, float[]> mappings;
    private static final IdentityHashMap<Texture, float[]> plantCards;
    private static final boolean[] pending;
    static boolean picturePending;
    private static final ArrayList<IsoObject> lastModelled;
    static final ArrayList<IsoWorldInventoryObject> lastModelledItems;

    static Recipe gather(IsoChunk isoChunk, int n, int n2, boolean bl, boolean bl2) {
        recipe = new Recipe(n);
        byPage.clear();
        mappings.clear();
        plantCards.clear();
        lastModelled.clear();
        lastModelledItems.clear();
        BodyCards.begin();
        WorldMesher.pending[0] = false;
        picturePending = false;
        baked = FloorGather.BAKED.get();
        PackGather.begin(recipe, bl2);
        FloorGather.bloodOf(isoChunk, n);
        float f = (float)n * 2.4494896f;
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                IsoGridSquare isoGridSquare = isoChunk.getGridSquare(j, i, n);
                if (isoGridSquare == null) continue;
                square = i * 8 + j;
                WorldMesher.square(isoGridSquare, (float)j + 0.5f, (float)i + 0.5f, f, pending, bl);
            }
        }
        owner = -1;
        Recipe recipe = WorldMesher.recipe;
        recipe.pending = pending[0];
        recipe.picturePending = picturePending;
        recipe.modelled = lastModelled.isEmpty() ? null : new ArrayList<IsoObject>(lastModelled);
        recipe.modelledItems = lastModelledItems.isEmpty() ? null : new ArrayList<IsoWorldInventoryObject>(lastModelledItems);
        recipe.bodies = BodyCards.end();
        recipe.floor = FloorGather.build();
        if (baked && recipe.floor != null) {
            Recipe.Batch batch = new Recipe.Batch();
            batch.floor = true;
            recipe.batches.add(batch);
            FloorSpans.emit(batch, isoChunk, n, recipe.floor, f);
        }
        WorldMesher.recipe = null;
        PackGather.end();
        byPage.clear();
        mappings.clear();
        plantCards.clear();
        return recipe;
    }

    private static void square(IsoGridSquare isoGridSquare, float f, float f2, float f3, boolean[] blArray, boolean bl) {
        int n = 0;
        PZArrayList pZArrayList = isoGridSquare.getObjects();
        for (int i = 0; i < pZArrayList.size(); ++i) {
            IsoSprite isoSprite;
            Texture texture;
            IsoObject isoObject = (IsoObject)pZArrayList.get(i);
            IsoSprite isoSprite2 = isoObject.getSprite();
            float f4 = f3 + WorldMesher.rise(isoObject);
            if (isoSprite2 == null || PackGather.object(isoObject, isoSprite2, f, f2, f4, square)) continue;
            if (bl && WorldMesher.hasModel(isoObject)) {
                lastModelled.add(isoObject);
                continue;
            }
            boolean bl2 = isoObject.hasProperty(IsoFlagType.water);
            if (!bl2 && FloorGather.capture(isoGridSquare, square, isoObject, blArray) && baked || (texture = (isoSprite = isoObject.isUseSnowSprite() ? isoSprite2.getSnowSprite() : isoSprite2).getTextureForCurrentFrame(isoObject.getDir(), isoObject)) == null) continue;
            if (!texture.isReady() || texture.getTextureId() == null) {
                blArray[0] = true;
                continue;
            }
            boolean bl3 = isoObject instanceof IsoTree;
            int n2 = TileMeshes.plantCards(isoSprite2, (IsoSprite)(bl3 ? isoSprite2 : null));
            if (n2 >= 0) {
                owner = Owner.of(square, 0);
                int n3 = TileMeshes.plantFlags(isoSprite2, (IsoSprite)(bl3 ? isoSprite2 : null), n2);
                WorldMesher.plant(texture, f, f2, f4, n2, n3);
                WorldMesher.plantParts(isoObject, n2, n3, f, f2, f4);
                continue;
            }
            TileMesh tileMesh = TileMeshes.get(isoSprite2, texture, null);
            if (tileMesh.vertCount == 0) continue;
            lift = isoSprite2.solidfloor ? 16 * Math.min(n++, 3) : 0;
            owner = Owner.of(square, bl3 ? 0 : Edges.ownerKind(isoSprite2));
            boolean bl4 = WallMesher.wall(isoSprite2, tileMesh, texture, isoGridSquare, f, f2, f4, blArray);
            if (!bl4) {
                int n4 = isoSprite2.solidfloor ? 4 : (WorldMesher.glass(isoObject, isoSprite2) ? 1024 : 0);
                WorldMesher.emit(WorldMesher.batch(texture.getTextureId()), tileMesh, texture, f, f2, f4, n4 | (bl2 ? 2048 : 0));
            }
            WorldMesher.overlays(isoObject, isoSprite2, tileMesh, bl4, f, f2, f4, blArray);
            lift = 0;
        }
        owner = Owner.of(square, 0);
        FloorDecals.onSquare(isoGridSquare, f, f2, f3, blArray, bl);
        if (baked) {
            FloorGather.blood(square, blArray);
        }
    }

    private static void plantParts(IsoObject isoObject, int n, int n2, float f, float f2, float f3) {
        ArrayList arrayList = isoObject.getAttachedAnimSprite();
        for (int i = 0; arrayList != null && i < arrayList.size(); ++i) {
            IsoSpriteInstance isoSpriteInstance = (IsoSpriteInstance)arrayList.get(i);
            WorldMesher.plantPart(isoObject, isoSpriteInstance == null ? null : isoSpriteInstance.parentSprite, n, n2, f, f2, f3);
        }
        WorldMesher.plantPart(isoObject, isoObject.getOverlaySprite(), n, n2, f, f2, f3);
    }

    private static void plantPart(IsoObject isoObject, IsoSprite isoSprite, int n, int n2, float f, float f2, float f3) {
        Texture texture;
        if (PackGather.part(isoSprite, f, f2, f3, square)) {
            return;
        }
        Texture texture2 = texture = isoSprite == null ? null : isoSprite.getTextureForCurrentFrame(isoObject.getDir(), isoObject);
        if (texture != null && texture.isReady() && texture.getTextureId() != null) {
            WorldMesher.plant(texture, f, f2, f3, n, n2 | 2);
        }
    }

    private static void plant(Texture texture, float f, float f2, float f3, int n, int n2) {
        Recipe.Op op = WorldMesher.op(WorldMesher.batch(texture.getTextureId()), 4, null, texture, f, f2, f3);
        op.face = n;
        op.flags = n2;
        op.card = WorldMesher.plantCard(texture);
    }

    private static float[] plantCard(Texture texture) {
        float[] fArray = plantCards.get(texture);
        if (fArray == null) {
            fArray = TileMeshes.plantCard(texture);
            plantCards.put(texture, fArray);
        }
        return fArray;
    }

    private static void overlays(IsoObject isoObject, IsoSprite isoSprite, TileMesh tileMesh, boolean bl, float f, float f2, float f3, boolean[] blArray) {
        IsoSpriteInstance isoSpriteInstance;
        int n;
        ArrayList arrayList = isoObject.getAttachedAnimSprite();
        if (arrayList != null) {
            for (n = 0; n < arrayList.size(); ++n) {
                isoSpriteInstance = (IsoSpriteInstance)arrayList.get(n);
                if (isoSpriteInstance == null || isoSpriteInstance.parentSprite == null) continue;
                WorldMesher.overlay(isoObject, isoSprite, isoSpriteInstance.parentSprite, tileMesh, bl, 0, f, f2, f3, blArray);
            }
        }
        if (isoObject.getOverlaySprite() != null) {
            WorldMesher.overlay(isoObject, isoSprite, isoObject.getOverlaySprite(), tileMesh, bl, 0, f, f2, f3, blArray);
        }
        if (bl && isoObject.wallBloodSplats != null && Core.getInstance().getOptionBloodDecals() != 0) {
            for (n = 0; n < isoObject.wallBloodSplats.size(); ++n) {
                isoSpriteInstance = (IsoWallBloodSplat)isoObject.wallBloodSplats.get(n);
                if (isoSpriteInstance == null || isoSpriteInstance.sprite == null) continue;
                WorldMesher.overlay(isoObject, isoSprite, isoSpriteInstance.sprite, tileMesh, true, isoSpriteInstance.isNorthSprite() ? 1 : (isoSpriteInstance.isWestSprite() ? 2 : 0), f, f2, f3, blArray);
            }
        }
    }

    private static void overlay(IsoObject isoObject, IsoSprite isoSprite, IsoSprite isoSprite2, TileMesh tileMesh, boolean bl, int n, float f, float f2, float f3, boolean[] blArray) {
        boolean bl2;
        if (PackGather.part(isoSprite2, f, f2, f3, square)) {
            return;
        }
        Texture texture = isoSprite2.getTextureForCurrentFrame(isoObject.getDir(), isoObject);
        if (texture == null) {
            return;
        }
        if (!texture.isReady() || texture.getTextureId() == null) {
            blArray[0] = true;
            return;
        }
        Recipe.Batch batch = WorldMesher.batch(texture.getTextureId());
        if (!bl) {
            WorldMesher.emit(batch, tileMesh, texture, f, f2, f3, 2);
            return;
        }
        PropertyContainer propertyContainer = isoSprite.getProperties();
        PropertyContainer propertyContainer2 = isoSprite2.getProperties();
        boolean bl3 = propertyContainer.has(IsoFlagType.WallNW) || propertyContainer.has(IsoFlagType.WallN) || propertyContainer.has(IsoFlagType.WindowN) || propertyContainer.has(IsoFlagType.DoorWallN);
        boolean bl4 = propertyContainer.has(IsoFlagType.WallNW) || propertyContainer.has(IsoFlagType.WallW) || propertyContainer.has(IsoFlagType.WindowW) || propertyContainer.has(IsoFlagType.DoorWallW);
        boolean bl5 = n == 1 || n == 0 && (propertyContainer2.has(IsoFlagType.attachedN) || propertyContainer2.has(IsoFlagType.WindowN));
        boolean bl6 = bl2 = n == 2 || n == 0 && (propertyContainer2.has(IsoFlagType.attachedW) || propertyContainer2.has(IsoFlagType.WindowW));
        if (!bl5 && !bl2) {
            bl2 = true;
            bl5 = true;
        }
        if (bl5 && bl3) {
            owner = Owner.of(square, 1);
            WorldMesher.emitFace(batch, tileMesh, texture, f, f2, f3, 0.0f, 1.0f, 0, 0.0f, 2);
        }
        if (bl2 && bl4) {
            owner = Owner.of(square, 2);
            WorldMesher.emitFace(batch, tileMesh, texture, f, f2, f3, 1.0f, 0.0f, 0, 0.0f, 2);
        }
    }

    public static float pixelScale(Texture texture) {
        return texture.getWidthOrig() == 64 && texture.getHeightOrig() == 128 ? 1.0f : 2.0f;
    }

    public static float rise(IsoObject isoObject) {
        return isoObject.getRenderYOffset() / 96.0f * 2.4494896f;
    }

    static Recipe.Batch batch(TextureID textureID) {
        Recipe.Batch batch = byPage.get(textureID);
        if (batch == null) {
            batch = new Recipe.Batch();
            batch.page = textureID;
            byPage.put(textureID, batch);
            WorldMesher.recipe.batches.add(batch);
        }
        return batch;
    }

    private static void emit(Recipe.Batch batch, TileMesh tileMesh, Texture texture, float f, float f2, float f3) {
        WorldMesher.emit(batch, tileMesh, texture, f, f2, f3, 0);
    }

    private static float[] mapping(Texture texture) {
        float[] fArray = mappings.get(texture);
        if (fArray == null) {
            fArray = WorldMesher.textureMapping(texture);
            mappings.put(texture, fArray);
        }
        return fArray;
    }

    public static float[] textureMapping(Texture texture) {
        float f = WorldMesher.pixelScale(texture);
        float f2 = 1.0f / (float)texture.getWidthHW();
        float f3 = 1.0f / (float)texture.getHeightHW();
        return new float[]{texture.getXStart() + 0.5f * f2, texture.getYStart() + 0.5f * f3, texture.getXEnd() - 0.5f * f2, texture.getYEnd() - 0.5f * f3, texture.getXStart() + ((float)texture.getWidthOrig() * 0.5f - 32.0f * f - texture.getOffsetX()) * f2, texture.getYStart() + ((float)texture.getHeightOrig() - 128.0f * f - texture.getOffsetY()) * f3, f * f2, f * f3, f, f2, f3};
    }

    static Recipe.Op op(Recipe.Batch batch, int n, TileMesh tileMesh, Texture texture, float f, float f2, float f3) {
        Recipe.Op op = new Recipe.Op();
        op.kind = n;
        op.mesh = tileMesh;
        op.map = WorldMesher.mapping(texture);
        op.dx = f;
        op.dy = f2;
        op.height = f3;
        op.lift = lift;
        op.owner = owner;
        batch.ops.add(op);
        return op;
    }

    static void raw(Recipe.Batch batch, float[] fArray) {
        Recipe.Op op = new Recipe.Op();
        op.kind = 3;
        op.raw = fArray;
        op.owner = owner;
        batch.ops.add(op);
    }

    static void emitFace(Recipe.Batch batch, TileMesh tileMesh, Texture texture, float f, float f2, float f3, float f4, float f5, int n, float f6) {
        WorldMesher.emitFace(batch, tileMesh, texture, f, f2, f3, f4, f5, n, f6, 0);
    }

    static void emitFace(Recipe.Batch batch, TileMesh tileMesh, Texture texture, float f, float f2, float f3, float f4, float f5, int n, float f6, int n2) {
        Recipe.Op op = WorldMesher.op(batch, 1, tileMesh, texture, f, f2, f3);
        op.dirX = f4;
        op.dirZ = f5;
        op.face = n;
        op.fill = f6;
        op.flags = n2;
    }

    static void emit(Recipe.Batch batch, TileMesh tileMesh, Texture texture, float f, float f2, float f3, int n) {
        WorldMesher.op((Recipe.Batch)batch, (int)0, (TileMesh)tileMesh, (Texture)texture, (float)f, (float)f2, (float)f3).flags = n;
    }

    private static boolean glass(IsoObject isoObject, IsoSprite isoSprite) {
        PropertyContainer propertyContainer = isoSprite.getProperties();
        return isoObject instanceof IsoWindow || propertyContainer.has(IsoFlagType.windowN) || propertyContainer.has(IsoFlagType.windowW) || propertyContainer.has(IsoFlagType.WallNTrans) || propertyContainer.has(IsoFlagType.WallWTrans) || propertyContainer.has(IsoFlagType.transparentN) || propertyContainer.has(IsoFlagType.transparentW) || propertyContainer.has("WallNWTrans") || propertyContainer.has("doorTrans");
    }

    public static boolean hasModel(IsoObject isoObject) {
        SpriteModel spriteModel = isoObject.getSpriteModel();
        return spriteModel != null && spriteModel.modelScriptName != null && ScriptManager.instance.getModelScript(spriteModel.modelScriptName) != null;
    }

    public static boolean hasModel(InventoryItem inventoryItem) {
        return inventoryItem != null && Core.getInstance().isOption3DGroundItem() && inventoryItem.getScriptItem().isWorldRender() != false && ItemModelRenderer.itemHasModel((InventoryItem)inventoryItem);
    }

    private WorldMesher() {
    }

    static {
        owner = -1;
        byPage = new IdentityHashMap();
        mappings = new IdentityHashMap();
        plantCards = new IdentityHashMap();
        pending = new boolean[1];
        lastModelled = new ArrayList();
        lastModelledItems = new ArrayList();
    }
}

