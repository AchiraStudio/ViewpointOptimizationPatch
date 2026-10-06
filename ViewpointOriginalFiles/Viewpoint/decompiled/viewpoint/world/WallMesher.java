/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.textures.Texture
 *  zombie.iso.IsoDirections
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.sprite.IsoSprite
 */
package viewpoint.world;

import viewpoint.visibility.Owner;
import viewpoint.world.Facades;
import viewpoint.world.Recipe;
import viewpoint.world.TileMesh;
import viewpoint.world.TileMeshes;
import viewpoint.world.WorldMesher;
import zombie.core.properties.PropertyContainer;
import zombie.core.textures.Texture;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;

final class WallMesher {
    static boolean wall(IsoSprite isoSprite, TileMesh tileMesh, Texture texture, IsoGridSquare isoGridSquare, float f, float f2, float f3, boolean[] blArray) {
        int n;
        int n2;
        if (isoSprite.getName() == null) {
            return false;
        }
        PropertyContainer propertyContainer = isoSprite.getProperties();
        boolean bl = propertyContainer.has(IsoFlagType.WallNW);
        int n3 = bl || propertyContainer.has(IsoFlagType.WallN) ? 0 : (propertyContainer.has(IsoFlagType.WindowN) ? 1 : (n2 = propertyContainer.has(IsoFlagType.DoorWallN) ? 2 : -1));
        int n4 = bl || propertyContainer.has(IsoFlagType.WallW) ? 0 : (propertyContainer.has(IsoFlagType.WindowW) ? 1 : (n = propertyContainer.has(IsoFlagType.DoorWallW) ? 2 : -1));
        if (n2 < 0 && n < 0) {
            return false;
        }
        Recipe.Batch batch = WorldMesher.batch(texture.getTextureId());
        if (n2 >= 0) {
            WorldMesher.owner = Owner.of(WorldMesher.square, 1);
            WorldMesher.emitFace(batch, tileMesh, texture, f, f2, f3, 0.0f, 1.0f, 0, 0.0f, WallMesher.glass(n2));
            WallMesher.back(isoSprite, tileMesh, texture, isoGridSquare, isoGridSquare.getN(), true, n2, 0.0f, 1.0f, f, f2, f3, blArray);
        }
        if (n >= 0) {
            WorldMesher.owner = Owner.of(WorldMesher.square, 2);
            WorldMesher.emitFace(batch, tileMesh, texture, f, f2, f3, 1.0f, 0.0f, 0, 0.0f, WallMesher.glass(n));
            WallMesher.back(isoSprite, tileMesh, texture, isoGridSquare, isoGridSquare.getW(), false, n, 1.0f, 0.0f, f, f2, f3, blArray);
        }
        if (n2 >= 0 && n >= 0) {
            WorldMesher.owner = -1;
            WallMesher.emitCaps(batch, tileMesh, texture, f, f2, f3);
        } else {
            WorldMesher.emitFace(batch, tileMesh, texture, f, f2, f3, n2 >= 0 ? 0.0f : 1.0f, n2 >= 0 ? 1.0f : 0.0f, 2, 0.0f);
        }
        return true;
    }

    private static void emitCaps(Recipe.Batch batch, TileMesh tileMesh, Texture texture, float f, float f2, float f3) {
        WorldMesher.op(batch, 2, tileMesh, texture, f, f2, f3);
    }

    private static void back(IsoSprite isoSprite, TileMesh tileMesh, Texture texture, IsoGridSquare isoGridSquare, IsoGridSquare isoGridSquare2, boolean bl, int n, float f, float f2, float f3, float f4, float f5, boolean[] blArray) {
        Texture texture2;
        int n2 = Facades.fill(isoGridSquare, isoGridSquare2, isoSprite);
        if (n2 < 0) {
            WorldMesher.emitFace(WorldMesher.batch(texture.getTextureId()), tileMesh, texture, f3, f4, f5, f, f2, 1, 0.0f, WallMesher.glass(n));
            return;
        }
        IsoSprite isoSprite2 = Facades.roomTile(isoGridSquare2, bl, n);
        if (isoSprite2 == null && (isoGridSquare2 == null || isoGridSquare2.getRoom() == null)) {
            isoSprite2 = Facades.facadeTile(isoGridSquare, bl, n);
        }
        if (isoSprite2 != null && (texture2 = isoSprite2.getTextureForCurrentFrame(IsoDirections.N)) != null) {
            if (!texture2.isReady() || texture2.getTextureId() == null) {
                blArray[0] = true;
                return;
            }
            TileMesh tileMesh2 = TileMeshes.get(isoSprite2, texture2, null);
            if (tileMesh2.vertCount > 0) {
                WorldMesher.emitFace(WorldMesher.batch(texture2.getTextureId()), tileMesh2, texture2, f3, f4, f5, f, f2, 1, 0.0f, WallMesher.glass(n));
                return;
            }
        }
        WorldMesher.emitFace(WorldMesher.batch(texture.getTextureId()), tileMesh, texture, f3, f4, f5, f, f2, 1, (float)n2 + 1.0f, WallMesher.glass(n));
    }

    private static int glass(int n) {
        return n == 1 ? 1024 : 0;
    }

    private WallMesher() {
    }
}

