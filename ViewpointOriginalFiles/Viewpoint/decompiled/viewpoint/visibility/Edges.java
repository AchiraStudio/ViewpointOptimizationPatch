/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.objects.GridSquareEdgeFacingDirection
 *  zombie.iso.sprite.IsoSprite
 *  zombie.util.list.PZArrayList
 */
package viewpoint.visibility;

import zombie.core.properties.PropertyContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.GridSquareEdgeFacingDirection;
import zombie.iso.sprite.IsoSprite;
import zombie.util.list.PZArrayList;

public final class Edges {
    public static final int OPEN = 0;
    public static final int WALL = 1;
    public static final int OPENING = 2;
    public static final int DOOR = 3;
    public static final int WINDOW = 4;

    public static int ownerKind(IsoSprite isoSprite) {
        boolean bl;
        if (isoSprite.solidfloor) {
            return 3;
        }
        PropertyContainer propertyContainer = isoSprite.getProperties();
        boolean bl2 = propertyContainer.has(IsoFlagType.doorN) || propertyContainer.has(IsoFlagType.WindowN) || propertyContainer.has(IsoFlagType.windowN) || propertyContainer.has(IsoFlagType.attachedN) || propertyContainer.has(IsoFlagType.collideN) || propertyContainer.has(IsoFlagType.transparentN) || propertyContainer.has(IsoFlagType.WallNTrans) || propertyContainer.has(IsoFlagType.HoppableN) || propertyContainer.has(IsoFlagType.TallHoppableN) || propertyContainer.has(IsoFlagType.DoorWallN) || propertyContainer.has(IsoFlagType.WallN);
        boolean bl3 = bl = propertyContainer.has(IsoFlagType.doorW) || propertyContainer.has(IsoFlagType.WindowW) || propertyContainer.has(IsoFlagType.windowW) || propertyContainer.has(IsoFlagType.attachedW) || propertyContainer.has(IsoFlagType.collideW) || propertyContainer.has(IsoFlagType.transparentW) || propertyContainer.has(IsoFlagType.WallWTrans) || propertyContainer.has(IsoFlagType.HoppableW) || propertyContainer.has(IsoFlagType.TallHoppableW) || propertyContainer.has(IsoFlagType.DoorWallW) || propertyContainer.has(IsoFlagType.WallW);
        if (bl2 && bl || propertyContainer.has(IsoFlagType.WallNW) || propertyContainer.has(IsoFlagType.attachedNW)) {
            return -1;
        }
        return bl2 ? 1 : (bl ? 2 : 0);
    }

    public static GridSquareEdgeFacingDirection facing(boolean bl) {
        return bl ? GridSquareEdgeFacingDirection.NORTH_SOUTH : GridSquareEdgeFacingDirection.EAST_WEST;
    }

    public static int edge(IsoGridSquare isoGridSquare, boolean bl) {
        GridSquareEdgeFacingDirection gridSquareEdgeFacingDirection = Edges.facing(bl);
        if (isoGridSquare.getDoor(gridSquareEdgeFacingDirection) != null) {
            return 3;
        }
        if (isoGridSquare.getWindow(gridSquareEdgeFacingDirection) != null) {
            return 4;
        }
        if (isoGridSquare.getWindowFrame(gridSquareEdgeFacingDirection) != null) {
            return 2;
        }
        boolean bl2 = false;
        boolean bl3 = false;
        boolean bl4 = false;
        PZArrayList pZArrayList = isoGridSquare.getObjects();
        for (int i = 0; i < pZArrayList.size(); ++i) {
            IsoSprite isoSprite = ((IsoObject)pZArrayList.get(i)).getSprite();
            if (isoSprite == null) continue;
            PropertyContainer propertyContainer = isoSprite.getProperties();
            bl2 |= propertyContainer.has(IsoFlagType.WallNW) || propertyContainer.has(bl ? IsoFlagType.WallN : IsoFlagType.WallW);
            bl3 |= propertyContainer.has(bl ? IsoFlagType.WindowN : IsoFlagType.WindowW) || propertyContainer.has(bl ? IsoFlagType.windowN : IsoFlagType.windowW) || propertyContainer.has(bl ? IsoFlagType.DoorWallN : IsoFlagType.DoorWallW) || propertyContainer.has(bl ? IsoFlagType.doorN : IsoFlagType.doorW);
            bl4 |= propertyContainer.has(bl ? IsoFlagType.transparentN : IsoFlagType.transparentW) || propertyContainer.has(bl ? IsoFlagType.WallNTrans : IsoFlagType.WallWTrans) || propertyContainer.has(bl ? IsoFlagType.HoppableN : IsoFlagType.HoppableW) || propertyContainer.has(bl ? IsoFlagType.TallHoppableN : IsoFlagType.TallHoppableW);
        }
        return bl3 ? 2 : (bl2 && !bl4 ? 1 : 0);
    }

    private Edges() {
    }
}

