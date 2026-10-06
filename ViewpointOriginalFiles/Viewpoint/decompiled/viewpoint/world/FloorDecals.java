/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 *  zombie.core.textures.Texture
 *  zombie.inventory.InventoryItem
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoGridSquare$ResultLight
 *  zombie.iso.IsoObject
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.objects.IsoDeadBody
 *  zombie.iso.objects.IsoWorldInventoryObject
 *  zombie.iso.sprite.IsoSprite
 */
package viewpoint.world;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import viewpoint.render.CorpseCards;
import viewpoint.world.BodyCards;
import viewpoint.world.Cook;
import viewpoint.world.Recipe;
import viewpoint.world.TileMeshes;
import viewpoint.world.WorldMesher;
import zombie.core.Core;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.IsoSprite;

final class FloorDecals {
    private static final float BODY_REACH = 1.3f;
    private static final DeadBodyAtlas.BodyParams PARAMS = new DeadBodyAtlas.BodyParams();
    private static Field bodyEntry;
    private static Field bodyReady;
    private static final String SNOW_TILES = "e_newsnow_ground_1_";

    static void onSquare(IsoGridSquare isoGridSquare, float f, float f2, float f3, boolean[] blArray, boolean bl) {
        List list = isoGridSquare.getDeadBodys();
        if (list != null) {
            for (int i = 0; i < list.size(); ++i) {
                FloorDecals.body((IsoDeadBody)list.get(i), isoGridSquare, f, f2, f3, blArray, bl);
            }
        }
        FloorDecals.items(isoGridSquare, f, f2, f3, blArray, bl);
        FloorDecals.snow(isoGridSquare, f, f2, f3, blArray);
    }

    static void items(IsoGridSquare isoGridSquare, float f, float f2, float f3, boolean[] blArray, boolean bl) {
        ArrayList arrayList = isoGridSquare.getWorldObjects();
        for (int i = 0; i < arrayList.size(); ++i) {
            Texture texture;
            IsoWorldInventoryObject isoWorldInventoryObject = (IsoWorldInventoryObject)arrayList.get(i);
            InventoryItem inventoryItem = isoWorldInventoryObject.getItem();
            if (inventoryItem == null || !inventoryItem.getScriptItem().isWorldRender().booleanValue()) continue;
            if (bl && WorldMesher.hasModel(inventoryItem)) {
                WorldMesher.lastModelledItems.add(isoWorldInventoryObject);
                continue;
            }
            IsoSprite isoSprite = isoWorldInventoryObject.getSprite();
            Texture texture2 = texture = isoSprite == null ? null : isoSprite.getTextureForCurrentFrame(isoWorldInventoryObject.getForwardIsoDirection(), (IsoObject)isoWorldInventoryObject);
            if (texture == null) continue;
            if (!texture.isReady() || texture.getTextureId() == null) {
                blArray[0] = true;
                continue;
            }
            FloorDecals.decal(WorldMesher.batch(texture.getTextureId()), texture, f + (isoWorldInventoryObject.getX() + isoWorldInventoryObject.xoff - (float)isoGridSquare.getX() - 0.5f), f2 + (isoWorldInventoryObject.getY() + isoWorldInventoryObject.yoff - (float)isoGridSquare.getY() - 0.5f), f3 + isoWorldInventoryObject.zoff * 2.4494896f + 0.006f, (float)texture.getWidthOrig() / 128.0f, (float)texture.getHeightOrig() / 128.0f);
        }
    }

    private static void decal(Recipe.Batch batch, Texture texture, float f, float f2, float f3, float f4, float f5) {
        float f6 = 1.0f / (float)texture.getWidthHW();
        float f7 = 1.0f / (float)texture.getHeightHW();
        float f8 = texture.getXStart() + 0.5f * f6;
        float f9 = texture.getYStart() + 0.5f * f7;
        float f10 = texture.getXEnd() - 0.5f * f6;
        float f11 = texture.getYEnd() - 0.5f * f7;
        float[] fArray = new float[84];
        int n = 0;
        int[] nArray = new int[]{0, 1, 2, 0, 2, 3};
        for (int i = 0; i < 6; ++i) {
            int n2 = nArray[i];
            float f12 = n2 == 1 || n2 == 2 ? 1.0f : -1.0f;
            float f13 = n2 >= 2 ? 1.0f : -1.0f;
            n = Cook.put(fArray, n, -(f + f12 * f4), f3, -(f2 + f13 * f5), f12 > 0.0f ? f10 : f8, f13 > 0.0f ? f11 : f9, f8, f9, f10, f11, 0.0f, 1.0f, 0.0f, 0.0f, 2 + WorldMesher.lift);
        }
        WorldMesher.raw(batch, fArray);
    }

    static void snow(IsoGridSquare isoGridSquare, float f, float f2, float f3, boolean[] blArray) {
        IsoCell isoCell = IsoWorld.instance.currentCell;
        if (isoCell.getSnowTarget() <= 0 || !FloorDecals.snowFloor(isoGridSquare)) {
            return;
        }
        int n = isoGridSquare.getX();
        int n2 = isoGridSquare.getY();
        int n3 = isoGridSquare.getZ();
        int n4 = -1;
        int n5 = -1;
        if (isoCell.gridSquareIsSnow(n, n2, n3)) {
            n4 = 40 + Math.floorMod(n + n2, 4);
        } else {
            boolean bl = isoCell.gridSquareIsSnow(n, n2 - 1, n3);
            boolean bl2 = isoCell.gridSquareIsSnow(n, n2 + 1, n3);
            boolean bl3 = isoCell.gridSquareIsSnow(n + 1, n2, n3);
            boolean bl4 = isoCell.gridSquareIsSnow(n - 1, n2, n3);
            int n6 = (bl ? 1 : 0) + (bl2 ? 1 : 0) + (bl3 ? 1 : 0) + (bl4 ? 1 : 0);
            int n7 = Math.floorMod(n + n2, 3) * 4;
            if (n6 == 1) {
                n4 = (bl ? 28 : (bl2 ? 31 : (bl3 ? 30 : 29))) + n7;
            } else if (n6 == 2) {
                if (bl && bl2) {
                    n4 = 28 + n7;
                    n5 = 31 + n7;
                } else if (bl3 && bl4) {
                    n4 = 29 + n7;
                    n5 = 30 + n7;
                } else {
                    n4 = bl ? (bl4 ? 16 : 19) + n7 : (bl4 ? 18 : 17) + n7;
                }
            } else if (n6 == 3) {
                n4 = (!bl ? 5 : (!bl2 ? 7 : (!bl3 ? 4 : 6))) + n7;
            } else if (n6 == 4) {
                n4 = Math.floorMod(n + n2, 4);
            }
        }
        if (n4 >= 0) {
            FloorDecals.snowTile(n4, f, f2, f3, blArray);
        }
        if (n5 >= 0) {
            FloorDecals.snowTile(n5, f, f2, f3, blArray);
        }
    }

    private static void snowTile(int n, float f, float f2, float f3, boolean[] blArray) {
        Texture texture = Texture.getSharedTexture((String)(SNOW_TILES + n));
        if (texture == null) {
            return;
        }
        if (!texture.isReady() || texture.getTextureId() == null) {
            blArray[0] = true;
            return;
        }
        WorldMesher.emit(WorldMesher.batch(texture.getTextureId()), TileMeshes.floorMesh(), texture, f, f2, f3 + 0.004f, 2);
    }

    private static boolean snowFloor(IsoGridSquare isoGridSquare) {
        return isoGridSquare.getProperties().has(IsoFlagType.solidfloor) && isoGridSquare.getProperties().has(IsoFlagType.exterior) && isoGridSquare.getRoom() == null && !isoGridSquare.isInARoom() && !isoGridSquare.getProperties().has(IsoFlagType.water) && (isoGridSquare.getWater() == null || !isoGridSquare.getWater().isValid());
    }

    static void body(IsoDeadBody isoDeadBody, IsoGridSquare isoGridSquare, float f, float f2, float f3, boolean[] blArray, boolean bl) {
        Texture texture;
        PARAMS.init(isoDeadBody);
        for (IsoGridSquare.ResultLight resultLight : FloorDecals.PARAMS.lights) {
            resultLight.radius = 0;
        }
        DeadBodyAtlas.BodyTexture bodyTexture = DeadBodyAtlas.instance.getBodyTexture(PARAMS);
        Texture texture2 = texture = bodyTexture == null ? null : bodyTexture.getTexture();
        if (texture == null || !texture.isReady() || texture.getTextureId() == null || !FloorDecals.ready(bodyTexture)) {
            blArray[0] = true;
            WorldMesher.picturePending = true;
            if (bl && bodyTexture != null) {
                BodyCards.add(isoDeadBody, null, null, bodyTexture);
            }
            return;
        }
        float[] fArray = FloorDecals.card(isoDeadBody, isoGridSquare, bodyTexture, texture, f, f2, f3);
        if (bl) {
            BodyCards.add(isoDeadBody, fArray, texture.getTextureId(), bodyTexture);
        } else {
            WorldMesher.raw(WorldMesher.batch(texture.getTextureId()), CorpseCards.baked(fArray));
        }
    }

    private static float[] card(IsoDeadBody isoDeadBody, IsoGridSquare isoGridSquare, DeadBodyAtlas.BodyTexture bodyTexture, Texture texture, float f, float f2, float f3) {
        float f4 = Core.tileScale;
        float f5 = 1.0f / (float)texture.getWidthHW();
        float f6 = 1.0f / (float)texture.getHeightHW();
        float f7 = texture.getXStart() - bodyTexture.getRenderX(0.0f) * f5;
        float f8 = texture.getYStart() - bodyTexture.getRenderY(0.0f) * f6;
        float f9 = isoDeadBody.getX() - (float)isoGridSquare.getX() - 0.5f;
        float f10 = isoDeadBody.getY() - (float)isoGridSquare.getY() - 0.5f;
        float[] fArray = new float[28];
        for (int i = 0; i < 4; ++i) {
            float f11 = i == 1 || i == 2 ? 1.3f : -1.3f;
            float f12 = i >= 2 ? 1.3f : -1.3f;
            int n = i * 4;
            fArray[n] = -(f + f9 + f11);
            fArray[n + 1] = -(f2 + f10 + f12);
            fArray[n + 2] = f7 + 32.0f * f4 * (f11 - f12) * f5;
            fArray[n + 3] = f8 + 16.0f * f4 * (f11 + f12) * f6;
        }
        fArray[16] = f3 + 0.012f;
        fArray[17] = Float.intBitsToFloat(2 + WorldMesher.lift);
        fArray[19] = Float.intBitsToFloat(WorldMesher.square);
        fArray[20] = texture.getXStart() + 0.5f * f5;
        fArray[21] = texture.getYStart() + 0.5f * f6;
        fArray[22] = texture.getXEnd() - 0.5f * f5;
        fArray[23] = texture.getYEnd() - 0.5f * f6;
        return fArray;
    }

    private static boolean ready(DeadBodyAtlas.BodyTexture bodyTexture) {
        try {
            Object object;
            if (bodyEntry == null) {
                bodyEntry = DeadBodyAtlas.BodyTexture.class.getDeclaredField("entry");
                bodyEntry.setAccessible(true);
            }
            if ((object = bodyEntry.get(bodyTexture)) == null) {
                return false;
            }
            if (bodyReady == null) {
                bodyReady = object.getClass().getDeclaredField("ready");
                bodyReady.setAccessible(true);
            }
            return bodyReady.getBoolean(object);
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            return true;
        }
    }

    private FloorDecals() {
    }
}

