/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoGridSquare
 */
package viewpoint.game;

import viewpoint.input.FreeCam;
import viewpoint.platform.Keys;
import viewpoint.platform.SettingsWindow;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;

public final class Teleport {
    private static final SettingsWindow.Button TO_CAMERA = SettingsWindow.button("Debug/Free camera", "Teleport to the free camera", "You stand where the free camera is, on the first floor at or below it; with none loaded there, on the ground.");
    private static final int LOWEST = -32;

    public static void poll(IsoCell isoCell, IsoGameCharacter isoGameCharacter) {
        if (!TO_CAMERA.take()) {
            return;
        }
        FreeCam.Place place = FreeCam.place;
        if (!FreeCam.active || place == null) {
            TO_CAMERA.status("the free camera is off (" + Keys.FREE_CAMERA.display() + ")");
            return;
        }
        int n = (int)Math.floor(place.x());
        int n2 = (int)Math.floor(place.y());
        int n3 = Teleport.floorUnder(isoCell, n, n2, (int)Math.floor(place.z()));
        isoGameCharacter.teleportTo(n, n2, n3);
        TO_CAMERA.status("at " + n + ", " + n2 + ", level " + n3);
    }

    private static int floorUnder(IsoCell isoCell, int n, int n2, int n3) {
        for (int i = n3; i >= -32; --i) {
            IsoGridSquare isoGridSquare = isoCell.getGridSquare(n, n2, i);
            if (isoGridSquare == null || !isoGridSquare.isSolidFloor() && !isoGridSquare.TreatAsSolidFloor()) continue;
            return i;
        }
        return 0;
    }

    private Teleport() {
    }
}

