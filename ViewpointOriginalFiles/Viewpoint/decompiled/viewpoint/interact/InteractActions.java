/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  se.krka.kahlua.vm.KahluaTable
 *  zombie.characters.CharacterTimedActions.BaseAction
 *  zombie.inventory.ItemContainer
 *  zombie.inventory.types.InventoryContainer
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.sprite.IsoSprite
 *  zombie.vehicles.BaseVehicle
 *  zombie.vehicles.VehiclePart
 */
package viewpoint.interact;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import se.krka.kahlua.vm.KahluaTable;
import viewpoint.interact.LootActions;
import viewpoint.interact.LootTargets;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

final class InteractActions {
    private static final long REST = 150000000L;
    private static final float FUEL_REACH = 4.0f;
    private static final ArrayList<String> labels = new ArrayList();
    private static final ArrayList<Boolean> enabled = new ArrayList();
    private static final HashSet<String> told = new HashSet();
    private static final IdentityHashMap<IsoObject, IsoSprite> passed = new IdentityHashMap();
    private static String title;
    private static int version;
    private static IsoGridSquare passedFrom;
    private static int passedCarrying;
    private static Object aimed;
    private static IsoSprite gatheredSprite;
    private static VehiclePart gatheredPart;
    private static VehiclePart gatheredWindow;
    private static IsoPlayer runner;
    private static long aimedAt;
    private static long gatheredAt;
    private static int runAt;
    private static boolean gathered;
    private static boolean ran;
    private static boolean gatheredNear;

    static void update(IsoPlayer isoPlayer, LootTargets.Target target, long l) {
        block9: {
            IsoSprite isoSprite;
            IsoObject isoObject;
            block8: {
                boolean bl;
                IsoSprite isoSprite2;
                IsoObject isoObject2 = target == null ? null : (isoObject = target.kind == 0 ? null : target.object);
                if (isoObject != aimed) {
                    aimed = isoObject;
                    aimedAt = l;
                    ran = false;
                    gathered = false;
                    InteractActions.clear();
                }
                if (isoObject == null || l - aimedAt < 150000000L) {
                    return;
                }
                if (isoObject instanceof BaseVehicle || !(isoObject instanceof IsoObject)) {
                    isoSprite2 = null;
                } else {
                    IsoObject isoObject3 = isoObject;
                    isoSprite2 = isoObject3.getSprite();
                }
                isoSprite = isoSprite2;
                boolean bl2 = bl = ran && isoPlayer.getCharacterActions().isEmpty();
                if (!gathered || bl || isoSprite != gatheredSprite) break block8;
                if (!(isoObject instanceof BaseVehicle)) break block9;
                BaseVehicle baseVehicle = (BaseVehicle)isoObject;
                if (l - gatheredAt < 150000000L || !InteractActions.movedRound(isoPlayer, baseVehicle)) break block9;
            }
            gathered = InteractActions.gather(isoPlayer, target);
            gatheredSprite = isoSprite;
            gatheredAt = l;
            ran = false;
            InteractActions.gatheredWhere(isoPlayer, isoObject);
            if (!gathered) {
                aimedAt = l;
            }
        }
    }

    private static boolean movedRound(IsoPlayer isoPlayer, BaseVehicle baseVehicle) {
        return baseVehicle.getUseablePart((IsoGameCharacter)((Object)isoPlayer)) != gatheredPart || baseVehicle.getClosestWindow((IsoGameCharacter)((Object)isoPlayer)) != gatheredWindow || isoPlayer.DistToProper((IsoObject)baseVehicle) < 4.0f != gatheredNear;
    }

    private static void gatheredWhere(IsoPlayer isoPlayer, Object object) {
        BaseVehicle baseVehicle;
        BaseVehicle baseVehicle2 = object instanceof BaseVehicle ? (baseVehicle = (BaseVehicle)object) : null;
        gatheredPart = baseVehicle2 == null ? null : baseVehicle2.getUseablePart((IsoGameCharacter)((Object)isoPlayer));
        gatheredWindow = baseVehicle2 == null ? null : baseVehicle2.getClosestWindow((IsoGameCharacter)((Object)isoPlayer));
        gatheredNear = baseVehicle2 != null && isoPlayer.DistToProper((IsoObject)baseVehicle2) < 4.0f;
    }

    static boolean passes(IsoObject isoObject) {
        IsoSprite isoSprite = passed.get(isoObject);
        return isoSprite != null && isoSprite == isoObject.getSprite();
    }

    static void standing(IsoPlayer isoPlayer, IsoGridSquare isoGridSquare) {
        if (passed.isEmpty()) {
            return;
        }
        if (isoGridSquare != passedFrom) {
            passed.keySet().removeIf(isoObject -> !InteractActions.near(isoObject.getSquare(), isoGridSquare));
            passedFrom = isoGridSquare;
        }
        if (isoPlayer.getCharacterActions().isEmpty() && InteractActions.carried(isoPlayer.getInventory()) != passedCarrying) {
            passed.clear();
        }
    }

    static void forget() {
        aimed = null;
        gatheredSprite = null;
        gatheredWindow = null;
        gatheredPart = null;
        ran = false;
        gathered = false;
        InteractActions.clear();
    }

    static void run(IsoPlayer isoPlayer, int n) {
        if (n >= 0 && n < labels.size() && enabled.get(n).booleanValue()) {
            LootActions.call("ViewpointInteract.run", new Object[]{isoPlayer, (double)(n + 1)});
            ran = true;
            runner = isoPlayer;
            runAt = n;
        }
    }

    static float progress(int n) {
        if (n != runAt || runner.getCharacterActions().isEmpty()) {
            return 0.0f;
        }
        BaseAction baseAction = (BaseAction)runner.getCharacterActions().get(0);
        return baseAction.isPathfinding() ? 0.0f : Math.min(1.0f, baseAction.getJobDelta());
    }

    static int count() {
        return labels.size();
    }

    static String label(int n) {
        return labels.get(n);
    }

    static boolean enabled(int n) {
        return enabled.get(n);
    }

    static String title() {
        return title;
    }

    static int version() {
        return version;
    }

    private static boolean gather(IsoPlayer isoPlayer, LootTargets.Target target) {
        int n;
        int n2;
        String string;
        Object object;
        BaseVehicle baseVehicle;
        Object object2 = target.object;
        if (object2 instanceof BaseVehicle) {
            baseVehicle = (BaseVehicle)object2;
            v0 = LootActions.call("ViewpointInteract.harvestVehicle", new Object[]{isoPlayer, baseVehicle});
        } else {
            v0 = object = LootActions.call("ViewpointInteract.harvest", new Object[]{isoPlayer, target.object});
        }
        if (!(object instanceof KahluaTable)) {
            return true;
        }
        baseVehicle = (KahluaTable)object;
        object2 = target.object.getClass().getSimpleName();
        Object object3 = baseVehicle.rawget((Object)"why");
        if (object3 != null) {
            InteractActions.tell((String)object2 + String.valueOf(object3), (String)object2 + ": none gathered now, " + String.valueOf(object3));
            return false;
        }
        InteractActions.clear();
        Object object4 = baseVehicle.rawget((Object)"title");
        title = object4 instanceof String ? (string = (String)object4) : null;
        string = (KahluaTable)baseVehicle.rawget((Object)"labels");
        KahluaTable kahluaTable = (KahluaTable)baseVehicle.rawget((Object)"enabled");
        for (n2 = 1; string != null && n2 <= string.len(); ++n2) {
            labels.add(String.valueOf(string.rawget(n2)));
            enabled.add(kahluaTable != null && Boolean.TRUE.equals(kahluaTable.rawget(n2)));
        }
        Object object5 = baseVehicle.rawget((Object)"seen");
        if (object5 instanceof Double) {
            Double d = (Double)object5;
            n = d.intValue();
        } else {
            n = 0;
        }
        n2 = n;
        InteractActions.tell((String)((Object)(labels.isEmpty() ? (String)object2 + " none" : object2)), (String)object2 + ": " + labels.size() + " actions of its menu's " + n2 + " options, first " + String.valueOf(labels));
        if (labels.isEmpty() && target.kind == 4) {
            if (passed.isEmpty()) {
                passedCarrying = InteractActions.carried(isoPlayer.getInventory());
            }
            passed.put(target.object, target.object.getSprite());
        }
        return true;
    }

    private static boolean near(IsoGridSquare isoGridSquare, IsoGridSquare isoGridSquare2) {
        return isoGridSquare != null && isoGridSquare.z == isoGridSquare2.z && Math.abs(isoGridSquare.x - isoGridSquare2.x) <= 1 && Math.abs(isoGridSquare.y - isoGridSquare2.y) <= 1;
    }

    private static int carried(ItemContainer itemContainer) {
        ArrayList arrayList = itemContainer.getItems();
        int n = arrayList.size();
        for (int i = 0; i < arrayList.size(); ++i) {
            InventoryContainer inventoryContainer;
            Object e = arrayList.get(i);
            if (!(e instanceof InventoryContainer) || (inventoryContainer = (InventoryContainer)e).getInventory() == null) continue;
            n += InteractActions.carried(inventoryContainer.getInventory());
        }
        return n;
    }

    private static void tell(String string, String string2) {
        if (told.add(string)) {
            System.out.println("[Viewpoint] interaction menu: " + string2);
        }
    }

    private static void clear() {
        labels.clear();
        enabled.clear();
        title = null;
        runner = null;
        runAt = -1;
        ++version;
    }

    private InteractActions() {
    }

    static {
        runAt = -1;
    }
}

