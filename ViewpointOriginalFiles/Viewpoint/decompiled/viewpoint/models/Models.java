/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.skinnedmodel.model.IsoObjectAnimations
 *  zombie.core.skinnedmodel.model.IsoObjectModelDrawer
 *  zombie.core.skinnedmodel.model.WorldItemModelDrawer
 *  zombie.core.textures.ColorInfo
 *  zombie.inventory.InventoryItem
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.SpriteModel
 *  zombie.iso.objects.IsoWorldInventoryObject
 */
package viewpoint.models;

import java.util.ArrayList;
import viewpoint.core.Frame;
import viewpoint.models.RigidCapture;
import viewpoint.visibility.Edges;
import viewpoint.visibility.Rooms;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.model.IsoObjectModelDrawer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteModel;
import zombie.iso.objects.IsoWorldInventoryObject;

public final class Models {
    public static boolean capturing;
    private static ArrayList<TextureDraw.GenericDrawer> sink;
    private static final ColorInfo UNLIT;
    private static final float OFF_THE_ATLAS = 0.0f;

    public static boolean capture(TextureDraw.GenericDrawer genericDrawer) {
        if (!capturing || !(genericDrawer instanceof IsoObjectModelDrawer) && !(genericDrawer instanceof WorldItemModelDrawer)) {
            return false;
        }
        sink.add(genericDrawer);
        return true;
    }

    public static void snapshot(Frame frame, int n) {
        int n2;
        IsoGridSquare isoGridSquare;
        IsoObject isoObject;
        int n3;
        sink = frame.drawers;
        for (n3 = 0; n3 < frame.modelObjects.size(); ++n3) {
            isoObject = frame.modelObjects.get(n3);
            isoGridSquare = isoObject.getSquare();
            if (isoGridSquare == null || isoObject.getSpriteModel() == null) continue;
            if (Models.roomHidden(isoObject, isoGridSquare)) {
                Rooms.leftOut();
                continue;
            }
            n2 = sink.size();
            Models.object(isoObject, isoGridSquare, n);
            Models.own(frame, n2, isoGridSquare, null);
        }
        for (n3 = 0; n3 < frame.modelItems.size(); ++n3) {
            isoObject = frame.modelItems.get(n3);
            isoGridSquare = isoObject.getSquare();
            if (isoGridSquare == null || isoObject.getItem() == null) continue;
            if (Rooms.hides(isoGridSquare.getX(), isoGridSquare.getY(), isoGridSquare.getX(), isoGridSquare.getY(), isoGridSquare.getZ(), isoGridSquare.getZ())) {
                Rooms.leftOut();
                continue;
            }
            n2 = sink.size();
            Models.item((IsoWorldInventoryObject)isoObject, isoGridSquare);
            Models.own(frame, n2, isoGridSquare, isoObject.getItem());
        }
        sink = null;
    }

    private static boolean roomHidden(IsoObject isoObject, IsoGridSquare isoGridSquare) {
        int n = isoObject.sprite == null ? -1 : Edges.ownerKind(isoObject.sprite);
        int n2 = isoGridSquare.getX();
        int n3 = isoGridSquare.getY();
        int n4 = isoGridSquare.getZ();
        return switch (n) {
            case 0 -> Rooms.hides(n2, n3, n2, n3, n4, n4);
            case 1 -> Rooms.hides(n2, n3 - 1, n2, n3, n4, n4);
            case 2 -> Rooms.hides(n2 - 1, n3, n2, n3, n4, n4);
            default -> false;
        };
    }

    private static void own(Frame frame, int n, IsoGridSquare isoGridSquare, InventoryItem inventoryItem) {
        int n2 = frame.scene.models.count();
        for (int i = n; i < sink.size(); ++i) {
            TextureDraw.GenericDrawer genericDrawer = sink.get(i);
            if (genericDrawer instanceof IsoObjectModelDrawer) {
                IsoObjectModelDrawer isoObjectModelDrawer = (IsoObjectModelDrawer)genericDrawer;
                RigidCapture.object(frame, isoObjectModelDrawer, isoGridSquare);
                continue;
            }
            genericDrawer = sink.get(i);
            if (!(genericDrawer instanceof WorldItemModelDrawer)) continue;
            WorldItemModelDrawer worldItemModelDrawer = (WorldItemModelDrawer)genericDrawer;
            RigidCapture.item(frame, worldItemModelDrawer, isoGridSquare, inventoryItem);
        }
        if (isoGridSquare == frame.lootSquare) {
            frame.scene.models.target(n2);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void object(IsoObject isoObject, IsoGridSquare isoGridSquare, int n) {
        SpriteModel spriteModel = isoObject.getSpriteModel();
        float f = isoObject.sprite != null && isoObject.sprite.getProperties().isSurfaceOffset() ? (float)isoObject.sprite.getProperties().getSurface() : 0.0f;
        AnimationPlayer animationPlayer = IsoObjectAnimations.getInstance().getAnimationPlayer(isoObject);
        ColorInfo colorInfo = isoGridSquare.getLightInfo(n);
        if (colorInfo == null) {
            isoGridSquare.cacheLightInfo();
            colorInfo = isoGridSquare.getLightInfo(n);
            if (colorInfo == null) {
                colorInfo = UNLIT;
            }
        }
        capturing = true;
        try {
            if (animationPlayer == null) {
                IsoObjectModelDrawer.renderMain((SpriteModel)spriteModel, (float)((float)isoGridSquare.getX() + 0.5f), (float)((float)isoGridSquare.getY() + 0.5f), (float)isoGridSquare.getZ(), (ColorInfo)colorInfo, (float)(isoObject.getRenderYOffset() + f));
            } else {
                IsoObjectModelDrawer.renderMain((SpriteModel)spriteModel, (float)((float)isoGridSquare.getX() + 0.5f), (float)((float)isoGridSquare.getY() + 0.5f), (float)isoGridSquare.getZ(), (ColorInfo)colorInfo, (float)(isoObject.getRenderYOffset() + f), (AnimationPlayer)animationPlayer);
            }
        }
        finally {
            capturing = false;
        }
    }

    private static void item(IsoWorldInventoryObject isoWorldInventoryObject, IsoGridSquare isoGridSquare) {
        capturing = true;
        try {
            WorldItemModelDrawer.renderMain((InventoryItem)isoWorldInventoryObject.getItem(), (IsoGridSquare)isoGridSquare, (IsoGridSquare)isoWorldInventoryObject.getRenderSquare(), (float)(isoWorldInventoryObject.getX() + isoWorldInventoryObject.xoff), (float)(isoWorldInventoryObject.getY() + isoWorldInventoryObject.yoff), (float)(isoWorldInventoryObject.getZ() + isoWorldInventoryObject.zoff), (float)0.0f, (float)0.0f, (boolean)false);
        }
        finally {
            capturing = false;
        }
    }

    public static void release(Frame frame) {
        for (int i = 0; i < frame.drawers.size(); ++i) {
            frame.drawers.get(i).postRender();
        }
        frame.drawers.clear();
    }

    private Models() {
    }

    static {
        UNLIT = new ColorInfo(0.6f, 0.6f, 0.6f, 1.0f);
    }
}

