/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.inventory.ItemContainer
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.LosUtil$TestResults
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.areas.SafeHouse
 *  zombie.iso.objects.IsoBarricade
 *  zombie.iso.objects.IsoCurtain
 *  zombie.iso.objects.IsoDeadBody
 *  zombie.iso.objects.IsoDoor
 *  zombie.iso.objects.IsoThumpable
 *  zombie.iso.objects.IsoTree
 *  zombie.iso.objects.IsoWindow
 *  zombie.iso.objects.IsoWindowFrame
 *  zombie.iso.objects.IsoWorldInventoryObject
 *  zombie.iso.sprite.IsoSprite
 *  zombie.network.GameClient
 *  zombie.util.list.PZArrayList
 *  zombie.vehicles.BaseVehicle
 *  zombie.vehicles.VehiclePart
 */
package viewpoint.interact;

import java.util.ArrayList;
import viewpoint.core.Frame;
import viewpoint.input.Look;
import viewpoint.input.ThirdPerson;
import viewpoint.interact.InteractActions;
import viewpoint.interact.LootAim;
import viewpoint.interact.LootBoxes;
import viewpoint.visibility.Edges;
import viewpoint.visibility.Owner;
import viewpoint.world.TileMeshes;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.properties.PropertyContainer;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.SafeHouse;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameClient;
import zombie.util.list.PZArrayList;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

final class LootTargets {
    static final int GROUND = 0;
    static final int BODY = 1;
    static final int OBJECT = 2;
    static final int VEHICLE = 3;
    static final int THING = 4;
    static final int ANIMAL = 5;
    static final int PERSON = 6;
    private static final float ITEM_HALF = 0.2f;
    private static final float ITEM_TALL = 0.3f;
    private static final float BODY_HALF = 0.6f;
    private static final float BODY_TALL = 0.4f;
    private static final float BEHIND = 0.5f;
    private static final float CHEST = 1.0f;
    private static final float ANIMAL_LEAST = 0.15f;
    private static final float TRUNK_HALF = 0.25f;
    private static final float TRUNK_TALL = 2.4494896f;
    private static final int PERSON_REACH = 2;
    private static final float PERSON_HALF = 0.3f;
    private static final float PERSON_TALL = 1.6f;
    private static final ArrayList<Target> found = new ArrayList();
    private static final ArrayList<Target> spare = new ArrayList();
    private static final ArrayList<BaseVehicle> vehicles = new ArrayList();
    private static final ArrayList<IsoAnimal> squareAnimals = new ArrayList();
    private static final float[] ray = new float[6];

    static Target aimed(Frame frame, IsoPlayer isoPlayer, Target target) {
        int n;
        LootTargets.aim(frame);
        LootTargets.collect(isoPlayer);
        int n2 = -1;
        for (n = 0; n < found.size() && target != null; ++n) {
            n2 = found.get(n).same(target) ? n : n2;
        }
        n = LootAim.pick(ray, LootBoxes.all(), found.size(), n2, LootTargets.from(isoPlayer));
        return n < 0 ? null : found.get(n);
    }

    static int owner(Target target) {
        int n = Math.floorMod(target.square.x, 8);
        int n2 = Math.floorMod(target.square.y, 8);
        IsoSprite isoSprite = target.object == null || target.kind == 1 || target.object instanceof IsoTree ? null : target.object.getSprite();
        int n3 = isoSprite == null ? 0 : Edges.ownerKind(isoSprite);
        return Owner.of(n2 * 8 + n, n3 == -1 ? 0 : n3);
    }

    private static void collect(IsoPlayer isoPlayer) {
        for (Target target : found) {
            target.clear();
        }
        spare.addAll(found);
        found.clear();
        vehicles.clear();
        IsoGridSquare isoGridSquare = isoPlayer.getCurrentSquare();
        if (isoGridSquare == null) {
            return;
        }
        InteractActions.standing(isoPlayer, isoGridSquare);
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                IsoGridSquare isoGridSquare2 = IsoWorld.instance.currentCell.getGridSquare(isoGridSquare.x + j, isoGridSquare.y + i, isoGridSquare.z);
                if (isoGridSquare2 == null || GameClient.client && !SafeHouse.isSafehouseAllowLoot((IsoGridSquare)isoGridSquare2, (IsoPlayer)isoPlayer)) continue;
                if (LootTargets.inSight(isoGridSquare, isoGridSquare2, j, i)) {
                    LootTargets.animals(isoGridSquare2);
                }
                if (isoGridSquare2 != isoGridSquare && !isoGridSquare.canReachTo(isoGridSquare2)) {
                    if ((j != 0 || i != 1) && (j != 1 || i != 0)) continue;
                    LootTargets.across(isoGridSquare2, i == 1 ? 1 : 2);
                    continue;
                }
                LootTargets.ground(isoGridSquare2);
                LootTargets.bodies(isoGridSquare2);
                LootTargets.objects(isoGridSquare2, isoPlayer);
                LootTargets.vehicle(isoGridSquare2.getVehicleContainer(), isoPlayer);
            }
        }
        LootTargets.people(isoPlayer, isoGridSquare);
    }

    private static void people(IsoPlayer isoPlayer, IsoGridSquare isoGridSquare) {
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                IsoGridSquare isoGridSquare2 = IsoWorld.instance.currentCell.getGridSquare(isoGridSquare.x + j, isoGridSquare.y + i, isoGridSquare.z);
                ArrayList arrayList = isoGridSquare2 == null ? null : isoGridSquare2.getMovingObjects();
                for (int k = 0; arrayList != null && k < arrayList.size(); ++k) {
                    IsoPlayer isoPlayer2;
                    Object e = arrayList.get(k);
                    if (!(e instanceof IsoPlayer) || (isoPlayer2 = (IsoPlayer)((Object)e)) == isoPlayer || isoPlayer2.isAnimal() || isoPlayer2.isDead() || !isoPlayer.CanSee((IsoMovingObject)((Object)isoPlayer2))) continue;
                    float f = isoPlayer2.getX();
                    float f2 = isoPlayer2.getY();
                    float f3 = isoPlayer2.getZ() * 2.4494896f;
                    LootBoxes.grow(LootTargets.add(6, null, (IsoObject)isoPlayer2), f - 0.3f, f2 - 0.3f, f3, f + 0.3f, f2 + 0.3f, f3 + 1.6f);
                }
            }
        }
    }

    private static void ground(IsoGridSquare isoGridSquare) {
        ArrayList arrayList = isoGridSquare.getWorldObjects();
        int n = -1;
        for (int i = 0; i < arrayList.size(); ++i) {
            IsoWorldInventoryObject isoWorldInventoryObject = (IsoWorldInventoryObject)arrayList.get(i);
            if (isoWorldInventoryObject.getItem() == null) continue;
            if (n < 0) {
                n = LootTargets.add(0, isoGridSquare, null);
            }
            float f = isoWorldInventoryObject.getX() + isoWorldInventoryObject.xoff;
            float f2 = isoWorldInventoryObject.getY() + isoWorldInventoryObject.yoff;
            float f3 = (isoWorldInventoryObject.getZ() + isoWorldInventoryObject.zoff) * 2.4494896f;
            LootBoxes.grow(n, f - 0.2f, f2 - 0.2f, f3, f + 0.2f, f2 + 0.2f, f3 + 0.3f);
        }
    }

    private static void bodies(IsoGridSquare isoGridSquare) {
        ArrayList arrayList = isoGridSquare.getStaticMovingObjects();
        for (int i = 0; i < arrayList.size(); ++i) {
            IsoDeadBody isoDeadBody;
            IsoMovingObject isoMovingObject = (IsoMovingObject)((Object)arrayList.get(i));
            if (isoMovingObject.getContainer() == null) continue;
            if (isoMovingObject instanceof IsoDeadBody && (isoDeadBody = (IsoDeadBody)isoMovingObject).isAnimal()) break;
            int n = LootTargets.add(1, isoGridSquare, isoMovingObject);
            LootTargets.found.get((int)n).containers.add(isoMovingObject.getContainer());
            float f = isoMovingObject.getX();
            float f2 = isoMovingObject.getY();
            float f3 = (float)isoGridSquare.z * 2.4494896f;
            LootBoxes.grow(n, f - 0.6f, f2 - 0.6f, f3, f + 0.6f, f2 + 0.6f, f3 + 0.4f);
        }
    }

    private static void objects(IsoGridSquare isoGridSquare, IsoPlayer isoPlayer) {
        PZArrayList pZArrayList = isoGridSquare.getObjects();
        for (int i = 0; i < pZArrayList.size(); ++i) {
            IsoThumpable isoThumpable;
            IsoObject isoObject = (IsoObject)pZArrayList.get(i);
            int n = isoObject.getContainerCount();
            if (n == 0) {
                if (isoObject instanceof IsoTree) {
                    LootTargets.tree(isoGridSquare, isoObject);
                    continue;
                }
                if (!LootTargets.aimable(isoObject)) continue;
                LootBoxes.shape(LootTargets.add(4, isoGridSquare, isoObject), isoObject, isoGridSquare);
                continue;
            }
            int n2 = LootTargets.add(2, isoGridSquare, isoObject);
            Target target = found.get(n2);
            for (int j = 0; j < n; ++j) {
                target.containers.add(isoObject.getContainerByIndex(j));
            }
            target.locked = isoObject instanceof IsoThumpable && (isoThumpable = (IsoThumpable)isoObject).isLockedToCharacter((IsoGameCharacter)((Object)isoPlayer));
            LootBoxes.shape(n2, isoObject, isoGridSquare);
        }
    }

    private static void tree(IsoGridSquare isoGridSquare, IsoObject isoObject) {
        float f = (float)isoGridSquare.x + 0.5f;
        float f2 = (float)isoGridSquare.y + 0.5f;
        float f3 = (float)isoGridSquare.z * 2.4494896f;
        if (InteractActions.passes(isoObject) || Math.abs(ray[0] - f) < 0.25f && Math.abs(ray[1] - f2) < 0.25f) {
            return;
        }
        LootBoxes.grow(LootTargets.add(4, isoGridSquare, isoObject), f - 0.25f, f2 - 0.25f, f3, f + 0.25f, f2 + 0.25f, f3 + 2.4494896f);
    }

    private static void across(IsoGridSquare isoGridSquare, int n) {
        PZArrayList pZArrayList = isoGridSquare.getObjects();
        for (int i = 0; i < pZArrayList.size(); ++i) {
            IsoObject isoObject = (IsoObject)pZArrayList.get(i);
            if (isoObject.getContainerCount() != 0 || !LootTargets.aimable(isoObject) || !LootTargets.twoSided(isoObject) || Edges.ownerKind(isoObject.getSprite()) != n) continue;
            LootBoxes.shape(LootTargets.add(4, isoGridSquare, isoObject), isoObject, isoGridSquare);
        }
    }

    private static boolean twoSided(IsoObject isoObject) {
        PropertyContainer propertyContainer = isoObject.getSprite().getProperties();
        return isoObject instanceof IsoDoor || isoObject instanceof IsoWindow || isoObject instanceof IsoWindowFrame || isoObject instanceof IsoThumpable || propertyContainer.has(IsoFlagType.HoppableN) || propertyContainer.has(IsoFlagType.HoppableW) || propertyContainer.has(IsoFlagType.TallHoppableN) || propertyContainer.has(IsoFlagType.TallHoppableW);
    }

    private static void vehicle(BaseVehicle baseVehicle, IsoPlayer isoPlayer) {
        if (baseVehicle == null || vehicles.contains(baseVehicle)) {
            return;
        }
        vehicles.add(baseVehicle);
        int n = LootTargets.add(3, null, (IsoObject)baseVehicle);
        for (int i = 0; i < baseVehicle.getPartCount(); ++i) {
            VehiclePart vehiclePart = baseVehicle.getPartByIndex(i);
            if (vehiclePart.getItemContainer() == null || !baseVehicle.canAccessContainer(i, (IsoGameCharacter)((Object)isoPlayer))) continue;
            LootTargets.found.get((int)n).containers.add(vehiclePart.getItemContainer());
        }
        LootBoxes.footprint(n, baseVehicle);
    }

    private static boolean inSight(IsoGridSquare isoGridSquare, IsoGridSquare isoGridSquare2, int n, int n2) {
        return isoGridSquare2 == isoGridSquare || isoGridSquare.testVisionAdjacent(n, n2, 0, true, false) != LosUtil.TestResults.Blocked;
    }

    private static void animals(IsoGridSquare isoGridSquare) {
        isoGridSquare.getAnimals(squareAnimals);
        for (int i = 0; i < squareAnimals.size(); ++i) {
            IsoAnimal isoAnimal = squareAnimals.get(i);
            if (isoAnimal.isDead()) continue;
            int n = LootTargets.add(5, null, (IsoObject)isoAnimal);
            float f = Math.max(0.15f, isoAnimal.getWidth());
            LootBoxes.animal(n, isoAnimal.getX(), isoAnimal.getY(), isoAnimal.getZ() * 2.4494896f, f, isoAnimal.getForwardDirectionX(), isoAnimal.getForwardDirectionY());
        }
        squareAnimals.clear();
    }

    private static boolean aimable(IsoObject isoObject) {
        return LootTargets.usable(isoObject) && !InteractActions.passes(isoObject);
    }

    private static boolean usable(IsoObject isoObject) {
        Object object;
        IsoSprite isoSprite = isoObject.getSprite();
        if (isoSprite == null || isoSprite.solidfloor || isoObject instanceof IsoWorldInventoryObject || isoObject instanceof IsoTree || isoObject instanceof IsoCurtain || isoObject instanceof IsoBarricade || isoObject instanceof IsoWindowFrame && (object = (IsoWindowFrame)isoObject).hasWindow() || TileMeshes.isPlant(isoSprite)) {
            return false;
        }
        object = isoSprite.getProperties();
        if (object.has(IsoFlagType.solidfloor) || object.has(IsoFlagType.FloorOverlay) || object.has(IsoFlagType.transparentFloor) || object.has(IsoFlagType.attachedFloor) || object.has(IsoFlagType.IsFloorAttached)) {
            return false;
        }
        if (isoObject instanceof IsoWindow || isoObject instanceof IsoDoor || isoObject instanceof IsoThumpable || isoObject instanceof IsoLightSwitch || object.has(IsoFlagType.WindowN) || object.has(IsoFlagType.WindowW) || object.has(IsoFlagType.HoppableN) || object.has(IsoFlagType.HoppableW)) {
            return true;
        }
        return !object.has(IsoFlagType.WallN) && !object.has(IsoFlagType.WallW) && !object.has(IsoFlagType.WallNW) && !object.has(IsoFlagType.WallSE) && !object.has(IsoFlagType.WallNTrans) && !object.has(IsoFlagType.WallWTrans) && !object.has(IsoFlagType.WallOverlay) && !object.has(IsoFlagType.DoorWallN) && !object.has(IsoFlagType.DoorWallW);
    }

    private static int add(int n, IsoGridSquare isoGridSquare, IsoObject isoObject) {
        Target target = spare.isEmpty() ? new Target() : spare.remove(spare.size() - 1);
        target.clear();
        target.kind = n;
        target.square = isoGridSquare;
        target.object = isoObject;
        target.locked = false;
        found.add(target);
        int n2 = found.size() - 1;
        LootBoxes.empty(n2);
        return n2;
    }

    private static void aim(Frame frame) {
        float f = Look.yaw;
        float f2 = Look.pitch;
        float f3 = (float)Math.cos(f2);
        LootTargets.ray[0] = frame.camX - frame.eyeX + ThirdPerson.offsetX;
        LootTargets.ray[1] = frame.camY - frame.eyeZ + ThirdPerson.offsetY;
        LootTargets.ray[2] = frame.camZ * 2.4494896f + frame.eyeY + ThirdPerson.offsetUp;
        LootTargets.ray[3] = (float)Math.cos(f) * f3;
        LootTargets.ray[4] = (float)Math.sin(f) * f3;
        LootTargets.ray[5] = (float)Math.sin(f2);
    }

    private static float from(IsoPlayer isoPlayer) {
        float f = isoPlayer.getX() - ray[0];
        float f2 = isoPlayer.getY() - ray[1];
        float f3 = isoPlayer.getZ() * 2.4494896f + 1.0f - ray[2];
        return Math.max(0.0f, f * ray[3] + f2 * ray[4] + f3 * ray[5] - 0.5f);
    }

    private LootTargets() {
    }

    static final class Target {
        int kind;
        IsoGridSquare square;
        IsoObject object;
        boolean locked;
        final ArrayList<ItemContainer> containers = new ArrayList();

        Target() {
        }

        boolean same(Target target) {
            return target != null && this.kind == target.kind && this.square == target.square && this.object == target.object;
        }

        void set(Target target) {
            this.kind = target.kind;
            this.square = target.square;
            this.object = target.object;
            this.locked = target.locked;
            this.containers.clear();
            this.containers.addAll(target.containers);
        }

        void clear() {
            this.square = null;
            this.object = null;
            this.containers.clear();
        }
    }
}

