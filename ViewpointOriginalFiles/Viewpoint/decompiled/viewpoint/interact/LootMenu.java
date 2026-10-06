/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.inventory.InventoryItem
 *  zombie.inventory.ItemContainer
 *  zombie.vehicles.BaseVehicle
 */
package viewpoint.interact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import viewpoint.core.Frame;
import viewpoint.core.View;
import viewpoint.input.FreeCam;
import viewpoint.input.Look;
import viewpoint.interact.InteractActions;
import viewpoint.interact.LootActions;
import viewpoint.interact.LootRows;
import viewpoint.interact.LootTargets;
import viewpoint.platform.KeyBind;
import viewpoint.platform.KeyInput;
import viewpoint.platform.Keys;
import viewpoint.visibility.Owner;
import viewpoint.world.ChunkCache;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.input.Mouse;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.vehicles.BaseVehicle;

public final class LootMenu {
    private static final int NONE = 0;
    private static final int ONE = 1;
    private static final int ALL = 2;
    private static final int OPEN = 3;
    private static final int CLOSE = 4;
    private static final int MAX_NOTCHES = 5;
    private static volatile boolean enabled = true;
    private static final LootTargets.Target target = new LootTargets.Target();
    static final LootRows rows = new LootRows();
    static final Set<InventoryItem> asked = Collections.newSetFromMap(new IdentityHashMap());
    private static final ArrayList<InventoryItem> taking = new ArrayList();
    private static final boolean[] held = new boolean[256];
    private static final boolean[] kept = new boolean[256];
    private static boolean aimed;
    private static boolean loot;
    private static boolean shown;
    private static boolean panelDue;
    private static boolean opened;
    private static boolean cursorFree;
    private static boolean cursorLook;
    private static int pending;
    private static int scroll;
    private static boolean failed;

    public static void snapshot(Frame frame, IsoGameCharacter isoGameCharacter) {
        frame.lootSquare = null;
        frame.lootVehicle = null;
        frame.lootAnimal = null;
        if (failed) {
            return;
        }
        try {
            LootMenu.show(frame, isoGameCharacter);
        }
        catch (RuntimeException runtimeException) {
            failed = true;
            frame.lootSquare = null;
            frame.lootVehicle = null;
            frame.lootAnimal = null;
            frame.scene.targetMesh = null;
            LootMenu.close();
            System.out.println("[Viewpoint] loot menu off after an error:");
            runtimeException.printStackTrace(System.out);
        }
    }

    private static void show(Frame frame, IsoGameCharacter isoGameCharacter) {
        LootTargets.Target target;
        LootMenu.window(isoGameCharacter);
        IsoPlayer isoPlayer = LootMenu.player(isoGameCharacter);
        LootTargets.Target target2 = isoPlayer == null ? null : (target = LootTargets.aimed(frame, isoPlayer, aimed ? LootMenu.target : null));
        if (target == null) {
            LootMenu.close();
            return;
        }
        if (!aimed || !target.same(LootMenu.target)) {
            LootMenu.target.set(target);
            aimed = true;
            rows.clear();
            asked.clear();
            for (ItemContainer itemContainer : LootMenu.target.containers) {
                LootActions.explore(isoPlayer, itemContainer);
            }
        }
        InteractActions.update(isoPlayer, LootMenu.target, System.nanoTime());
        boolean bl = loot = LootMenu.target.kind == 0 || !LootMenu.target.containers.isEmpty();
        if (!loot && InteractActions.count() == 0) {
            pending = 0;
            scroll = 0;
            return;
        }
        if (isoPlayer.getCharacterActions().isEmpty()) {
            asked.clear();
        }
        rows.refresh(isoPlayer, LootMenu.target);
        rows.move(scroll);
        scroll = 0;
        LootMenu.take(isoPlayer);
        shown = true;
        panelDue = true;
        frame.lootSquare = LootMenu.target.square;
        frame.lootVehicle = LootMenu.target.kind == 3 ? (BaseVehicle)LootMenu.target.object : null;
        frame.lootAnimal = LootMenu.target.kind == 5 ? (IsoAnimal)LootMenu.target.object : null;
        frame.scene.targetMesh = LootMenu.target.square == null ? null : ChunkCache.meshOn(LootMenu.target.square);
        frame.scene.targetOwner = LootMenu.target.square == null ? 0 : LootTargets.owner(LootMenu.target);
        frame.scene.targetSquare = LootMenu.target.square == null ? -1 : Owner.square(frame.scene.targetOwner);
    }

    private static void window(IsoGameCharacter isoGameCharacter) {
        if (pending == 4) {
            pending = 0;
            opened = false;
            cursorLook = true;
            if (isoGameCharacter instanceof IsoPlayer) {
                IsoPlayer isoPlayer = (IsoPlayer)((Object)isoGameCharacter);
                LootActions.closeWindow(isoPlayer);
            }
        }
        if (opened && Look.wantCapture && !cursorFree) {
            opened = false;
        }
    }

    public static boolean keyDown(int n, boolean bl) {
        if (n < 0 || n >= held.length || !bl && !held[n]) {
            return bl;
        }
        boolean bl2 = bl && !held[n];
        LootMenu.held[n] = bl;
        if (bl2) {
            int n2 = LootMenu.asks(n);
            LootMenu.kept[n] = n2 != 0;
            pending = n2 != 0 ? n2 : pending;
        }
        int n3 = n;
        kept[n3] = kept[n3] & bl;
        return bl && !kept[n];
    }

    private static int asks(int n) {
        if (n <= 0 || !shown && !opened) {
            return 0;
        }
        KeyBind keyBind = KeyInput.menu(n);
        if (!shown) {
            return keyBind == Keys.LOOT_WINDOW ? 4 : 0;
        }
        if (loot && keyBind == Keys.LOOT_TAKE_ALL) {
            return 2;
        }
        if (loot && keyBind == Keys.LOOT_WINDOW) {
            return 3;
        }
        return keyBind == Keys.LOOT_TAKE && LootMenu.interacts() ? 1 : 0;
    }

    static boolean interacts() {
        return rows.action() >= 0 || !LootMenu.target.locked && rows.next(asked) != null;
    }

    public static void wheel() {
        int n = Mouse.getWheelState();
        if (!shown || n == 0) {
            return;
        }
        Mouse.wheelDelta = 0;
        scroll -= Integer.signum(n) * Math.min(Math.abs(n), 5);
    }

    public static boolean freeCursor() {
        boolean bl = cursorFree;
        cursorFree = false;
        return bl;
    }

    public static boolean lookAgain() {
        boolean bl = cursorLook;
        cursorLook = false;
        return bl;
    }

    public static void setEnabled(boolean bl) {
        enabled = bl;
    }

    static boolean panelDue() {
        boolean bl = panelDue;
        panelDue = false;
        shown = bl;
        return bl;
    }

    static boolean locked() {
        return LootMenu.target.locked;
    }

    static boolean loot() {
        return loot;
    }

    private static IsoPlayer player(IsoGameCharacter isoGameCharacter) {
        if (!(enabled && View.enabled && !FreeCam.active && Look.wantCapture && isoGameCharacter instanceof IsoPlayer)) {
            return null;
        }
        IsoPlayer isoPlayer = (IsoPlayer)((Object)isoGameCharacter);
        boolean bl = isoPlayer == IsoPlayer.players[0] && isoPlayer.isLocalPlayer() && !isoPlayer.isDead() && isoPlayer.getVehicle() == null && !isoPlayer.isAiming();
        return bl ? isoPlayer : null;
    }

    private static void take(IsoPlayer isoPlayer) {
        int n = pending;
        pending = 0;
        if (n == 1 && rows.action() >= 0) {
            InteractActions.run(isoPlayer, rows.action());
            return;
        }
        if (n == 3) {
            LootActions.openWindow(isoPlayer, LootMenu.target.kind == 0 ? null : LootMenu.target.containers.get(0));
            opened = true;
            cursorFree = true;
            return;
        }
        if (n != 1 && n != 2 || LootMenu.target.locked) {
            return;
        }
        if (n == 1) {
            InventoryItem inventoryItem = rows.next(asked);
            if (inventoryItem != null) {
                asked.add(inventoryItem);
                LootActions.take(isoPlayer, inventoryItem);
            }
            return;
        }
        taking.clear();
        rows.all(asked, taking);
        if (!taking.isEmpty()) {
            asked.addAll(taking);
            LootActions.takeAll(isoPlayer, taking);
        }
        taking.clear();
    }

    private static void close() {
        shown = false;
        loot = false;
        pending = 0;
        scroll = 0;
        if (aimed) {
            aimed = false;
            target.clear();
            rows.clear();
            asked.clear();
            InteractActions.forget();
        }
    }

    private LootMenu() {
    }
}

