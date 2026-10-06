/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.IsoObjectPicker$ClickObject
 *  zombie.iso.objects.IsoCurtain
 *  zombie.iso.objects.IsoDeadBody
 *  zombie.iso.objects.IsoDoor
 *  zombie.iso.objects.IsoThumpable
 *  zombie.iso.objects.IsoTree
 *  zombie.iso.objects.IsoWindow
 *  zombie.iso.objects.IsoWindowFrame
 *  zombie.iso.objects.IsoWorldInventoryObject
 *  zombie.iso.sprite.IsoSprite
 *  zombie.vehicles.BaseVehicle
 */
package viewpoint.interact;

import viewpoint.render.MousePick;
import viewpoint.visibility.Edges;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.input.Mouse;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.IsoSprite;
import zombie.vehicles.BaseVehicle;

public final class MouseTargets {
    public static final int DOOR = 0;
    public static final int WINDOW = 1;
    public static final int WINDOW_FRAME = 2;
    public static final int THUMPABLE = 3;
    public static final int HOPPABLE = 4;
    public static final int CORPSE = 5;
    public static final int TREE = 6;
    private static final float BACK = 0.02f;
    private static final float EDGE = 0.08f;
    private static final float ON_FLOOR = 0.03f;
    private static final float BODY_REACH = 0.6f;
    private static final float BODY_LEVELS = 0.8f;
    private static final float ITEM_REACH = 0.35f;
    private static final int STILL = 3;
    private static final IsoObjectPicker.ClickObject click = new IsoObjectPicker.ClickObject();
    private static MousePick.Hit lastHit;
    private static IsoObject lastObject;
    private static IsoGridSquare lastSquare;

    public static void frame(boolean bl) {
        if (!bl) {
            MousePick.ask = null;
            return;
        }
        int n = Mouse.getXA();
        int n2 = Mouse.getYA();
        int n3 = Core.getInstance().getScreenWidth();
        int n4 = Core.getInstance().getScreenHeight();
        MousePick.Ask ask = MousePick.ask;
        if (ask == null || ask.x() != n || ask.y() != n2 || ask.screenWidth() != n3 || ask.screenHeight() != n4) {
            MousePick.ask = new MousePick.Ask(n, n2, n3, n4);
        }
    }

    public static boolean on() {
        return MousePick.ask != null;
    }

    public static IsoObjectPicker.ClickObject contextPick(int n, int n2) {
        IsoObject isoObject = MouseTargets.object();
        if (isoObject == null || isoObject.getSquare() == null) {
            return null;
        }
        MouseTargets.click.square = isoObject.getSquare();
        MouseTargets.click.tile = isoObject;
        MouseTargets.click.x = n;
        MouseTargets.click.y = n2;
        MouseTargets.click.height = 1;
        MouseTargets.click.width = 1;
        MouseTargets.click.ly = 0;
        MouseTargets.click.lx = 0;
        MouseTargets.click.scaleY = 1.0f;
        MouseTargets.click.scaleX = 1.0f;
        MouseTargets.click.flip = false;
        MouseTargets.click.score = 0;
        MouseTargets.click.z = MouseTargets.click.square.getZ();
        return click;
    }

    public static IsoObject picked(int n) {
        IsoObject isoObject = MouseTargets.object();
        if (n == 5) {
            return isoObject instanceof IsoDeadBody ? isoObject : (lastSquare == null ? null : lastSquare.getDeadBody());
        }
        boolean bl = switch (n) {
            case 0 -> {
                IsoThumpable var3_2;
                if (isoObject instanceof IsoDoor || isoObject instanceof IsoThumpable && (var3_2 = (IsoThumpable)isoObject).isDoor()) {
                    yield true;
                }
                yield false;
            }
            case 1 -> isoObject instanceof IsoWindow;
            case 2 -> isoObject instanceof IsoWindowFrame;
            case 3 -> isoObject instanceof IsoThumpable;
            case 4 -> {
                if (isoObject != null && isoObject.isHoppable()) {
                    yield true;
                }
                yield false;
            }
            case 6 -> isoObject instanceof IsoTree;
            default -> false;
        };
        return bl ? isoObject : null;
    }

    public static BaseVehicle vehicle() {
        MouseTargets.object();
        return lastSquare == null ? null : lastSquare.getVehicleContainer();
    }

    public static Double world(boolean bl) {
        MousePick.Hit hit = MouseTargets.fresh();
        return hit == null ? null : Double.valueOf(bl ? hit.x() : hit.y());
    }

    private static MousePick.Hit fresh() {
        MousePick.Hit hit = MousePick.hit;
        if (hit == null || Math.abs(hit.ask().x() - Mouse.getXA()) > 3 || Math.abs(hit.ask().y() - Mouse.getYA()) > 3) {
            return null;
        }
        return hit;
    }

    private static IsoObject object() {
        MousePick.Hit hit = MouseTargets.fresh();
        if (hit != lastHit) {
            lastHit = hit;
            lastSquare = null;
            lastObject = hit == null ? null : MouseTargets.find(hit, IsoWorld.instance.getCell());
        }
        return lastObject;
    }

    private static IsoObject find(MousePick.Hit hit, IsoCell isoCell) {
        IsoObject isoObject;
        IsoGridSquare isoGridSquare;
        double d = hit.x() - (double)(hit.dirX() * 0.02f);
        double d2 = hit.y() - (double)(hit.dirY() * 0.02f);
        double d3 = hit.z() - (double)(hit.dirUp() * 0.02f / 2.4494896f);
        int n = (int)Math.floor(d);
        int n2 = (int)Math.floor(d2);
        int n3 = (int)Math.floor(d3);
        lastSquare = isoGridSquare = isoCell == null ? null : isoCell.getGridSquare(n, n2, n3);
        if (isoGridSquare == null) {
            return null;
        }
        IsoMovingObject isoMovingObject = MouseTargets.body(isoCell, d, d2, d3, n, n2, n3);
        if (isoMovingObject != null) {
            return isoMovingObject;
        }
        double d4 = d - (double)n;
        double d5 = d2 - (double)n2;
        Object object = d4 < (double)0.08f ? MouseTargets.edge(isoGridSquare, 2) : (isoObject = d4 > 0.9200000017881393 ? MouseTargets.edge(isoCell.getGridSquare(n + 1, n2, n3), 2) : null);
        Object object2 = isoObject != null ? isoObject : (d5 < (double)0.08f ? MouseTargets.edge(isoGridSquare, 1) : (isoObject = d5 > 0.9200000017881393 ? MouseTargets.edge(isoCell.getGridSquare(n, n2 + 1, n3), 1) : null));
        if (isoObject != null) {
            return isoObject;
        }
        return d3 - (double)n3 > (double)0.03f ? MouseTargets.contents(isoGridSquare) : MouseTargets.onFloor(isoGridSquare, d, d2);
    }

    private static IsoMovingObject body(IsoCell isoCell, double d, double d2, double d3, int n, int n2, int n3) {
        IsoMovingObject isoMovingObject = null;
        double d4 = 0.36f;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                IsoGridSquare isoGridSquare = isoCell.getGridSquare(n + j, n2 + i, n3);
                for (int k = 0; isoGridSquare != null && k < isoGridSquare.getMovingObjects().size(); ++k) {
                    IsoMovingObject isoMovingObject2 = (IsoMovingObject)((Object)isoGridSquare.getMovingObjects().get(k));
                    double d5 = (double)isoMovingObject2.getX() - d;
                    double d6 = (double)isoMovingObject2.getY() - d2;
                    double d7 = d3 - (double)isoMovingObject2.getZ();
                    double d8 = d5 * d5 + d6 * d6;
                    if (isoMovingObject2 == IsoPlayer.players[0] || isoMovingObject2 instanceof BaseVehicle || !(d7 >= (double)-0.03f) || !(d7 <= (double)0.8f) || !(d8 < d4)) continue;
                    isoMovingObject = isoMovingObject2;
                    d4 = d8;
                }
            }
        }
        return isoMovingObject;
    }

    private static IsoObject edge(IsoGridSquare isoGridSquare, int n) {
        int n2;
        IsoObject isoObject = null;
        int n3 = n2 = isoGridSquare == null ? -1 : isoGridSquare.getObjects().size() - 1;
        while (n2 >= 0) {
            IsoObject isoObject2 = (IsoObject)isoGridSquare.getObjects().get(n2);
            IsoSprite isoSprite = isoObject2.getSprite();
            if (isoSprite != null && Edges.ownerKind(isoSprite) == n) {
                if (isoObject2 instanceof IsoDoor || isoObject2 instanceof IsoWindow || isoObject2 instanceof IsoWindowFrame || isoObject2 instanceof IsoCurtain || isoObject2 instanceof IsoThumpable) {
                    return isoObject2;
                }
                isoObject = isoObject == null ? isoObject2 : isoObject;
            }
            --n2;
        }
        return isoObject;
    }

    private static IsoObject contents(IsoGridSquare isoGridSquare) {
        for (int i = isoGridSquare.getObjects().size() - 1; i >= 0; --i) {
            IsoObject isoObject = (IsoObject)isoGridSquare.getObjects().get(i);
            IsoSprite isoSprite = isoObject.getSprite();
            if (isoSprite == null || isoSprite.solidfloor || Edges.ownerKind(isoSprite) != 0) continue;
            return isoObject;
        }
        return MouseTargets.floor(isoGridSquare);
    }

    private static IsoObject onFloor(IsoGridSquare isoGridSquare, double d, double d2) {
        IsoWorldInventoryObject isoWorldInventoryObject = null;
        double d3 = 0.1224999949336052;
        for (IsoWorldInventoryObject isoWorldInventoryObject2 : isoGridSquare.getWorldObjects()) {
            double d4;
            double d5 = (double)isoWorldInventoryObject2.getWorldPosX() - d;
            double d6 = d5 * d5 + (d4 = (double)isoWorldInventoryObject2.getWorldPosY() - d2) * d4;
            if (!(d6 < d3)) continue;
            isoWorldInventoryObject = isoWorldInventoryObject2;
            d3 = d6;
        }
        if (isoWorldInventoryObject != null) {
            return isoWorldInventoryObject;
        }
        IsoDeadBody isoDeadBody = isoGridSquare.getDeadBody();
        return isoDeadBody != null ? isoDeadBody : MouseTargets.floor(isoGridSquare);
    }

    private static IsoObject floor(IsoGridSquare isoGridSquare) {
        for (int i = 0; i < isoGridSquare.getObjects().size(); ++i) {
            IsoObject isoObject = (IsoObject)isoGridSquare.getObjects().get(i);
            if (isoObject.getSprite() == null || !isoObject.getSprite().solidfloor) continue;
            return isoObject;
        }
        return isoGridSquare.getObjects().isEmpty() ? null : (IsoObject)isoGridSquare.getObjects().get(0);
    }

    private MouseTargets() {
    }
}

