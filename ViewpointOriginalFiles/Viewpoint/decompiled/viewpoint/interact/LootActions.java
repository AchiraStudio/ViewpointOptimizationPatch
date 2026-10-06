/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.Lua.LuaManager
 *  zombie.inventory.InventoryItem
 *  zombie.inventory.ItemContainer
 */
package viewpoint.interact;

import java.util.ArrayList;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;

final class LootActions {
    private static boolean reported;

    static void explore(IsoPlayer isoPlayer, ItemContainer itemContainer) {
        LootActions.call("ViewpointLoot.explore", new Object[]{isoPlayer, itemContainer});
    }

    static void take(IsoPlayer isoPlayer, InventoryItem inventoryItem) {
        LootActions.call("ViewpointLoot.take", new Object[]{isoPlayer, inventoryItem});
    }

    static void takeAll(IsoPlayer isoPlayer, ArrayList<InventoryItem> arrayList) {
        LootActions.call("ViewpointLoot.takeAll", new Object[]{isoPlayer, arrayList});
    }

    static void openWindow(IsoPlayer isoPlayer, ItemContainer itemContainer) {
        LootActions.call("ViewpointLoot.openWindow", new Object[]{isoPlayer, itemContainer});
    }

    static void closeWindow(IsoPlayer isoPlayer) {
        LootActions.call("ViewpointLoot.closeWindow", new Object[]{isoPlayer});
    }

    static Object call(String string, Object ... objectArray) {
        Object object = LuaManager.getFunctionObject((String)string);
        if (object == null) {
            LootActions.report(string, "not found");
            return null;
        }
        Object[] objectArray2 = LuaManager.caller.pcall(LuaManager.thread, object, objectArray);
        if (objectArray2 != null && objectArray2.length > 0 && Boolean.FALSE.equals(objectArray2[0])) {
            LootActions.report(string, objectArray2.length > 1 ? String.valueOf(objectArray2[1]) : "failed");
            return null;
        }
        return objectArray2 != null && objectArray2.length > 1 ? objectArray2[1] : null;
    }

    private static void report(String string, String string2) {
        if (!reported) {
            reported = true;
            System.out.println("[Viewpoint] loot and interaction menu: " + string + " " + string2);
        }
    }

    private LootActions() {
    }
}

