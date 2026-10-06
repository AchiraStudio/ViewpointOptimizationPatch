/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.inventory.InventoryItem
 */
package viewpoint.light;

import java.util.ArrayList;
import viewpoint.core.Frame;
import viewpoint.render.SceneData;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;

public final class Flashlight {
    static boolean active;
    private static final ArrayList<InventoryItem> items;

    public static void snapshot(Frame frame, IsoGameCharacter isoGameCharacter) {
        Object object;
        int n;
        SceneData sceneData = frame.scene;
        sceneData.flashOn = false;
        items.clear();
        isoGameCharacter.getActiveLightItems(items);
        Object object2 = null;
        for (n = 0; n < items.size(); ++n) {
            object = items.get(n);
            if (!object.isTorchCone() || object2 != null && !(object.getLightStrength() > object2.getLightStrength())) continue;
            object2 = object;
        }
        items.clear();
        n = isoGameCharacter instanceof IsoPlayer && (object = (IsoPlayer)((Object)isoGameCharacter)).getVehicle() != null && !((IsoPlayer)((Object)object)).isAiming() ? 1 : 0;
        boolean bl = active = object2 != null && !isoGameCharacter.isDead() && n == 0;
        if (!active) {
            return;
        }
        sceneData.flashOn = true;
        sceneData.flashStrength = object2.getLightStrength();
        sceneData.flashRange = Math.max(4.0f, (float)object2.getLightDistance());
        sceneData.flashCos = Math.max(0.2f, Math.min(0.97f, object2.getTorchDot()));
    }

    private Flashlight() {
    }

    static {
        items = new ArrayList();
    }
}

