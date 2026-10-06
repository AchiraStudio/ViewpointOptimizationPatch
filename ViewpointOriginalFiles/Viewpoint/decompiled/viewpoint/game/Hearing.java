/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  fmod.fmod.SoundListener
 *  fmod.javafmod
 *  zombie.network.GameServer
 *  zombie.scripting.objects.CharacterTrait
 */
package viewpoint.game;

import fmod.fmod.SoundListener;
import fmod.javafmod;
import viewpoint.core.View;
import viewpoint.input.FreeCam;
import viewpoint.input.Look;
import zombie.characters.IsoPlayer;
import zombie.network.GameServer;
import zombie.scripting.objects.CharacterTrait;

public final class Hearing {
    private static final float LEVEL_UNITS = 3.0f;
    private static final int FIRST = 0;
    static final float ZOOMED_IN = 0.0f;
    private static final float[] forward = new float[3];
    private static final float[] up = new float[3];

    public static void listen(SoundListener soundListener) {
        if (soundListener.index != 0 || !View.enabled || GameServer.server) {
            return;
        }
        FreeCam.Place place = FreeCam.centre();
        IsoPlayer isoPlayer = IsoPlayer.players[0];
        boolean bl = place != null && isoPlayer != null && !isoPlayer.hasTrait(CharacterTrait.DEAF);
        float f = bl ? (float)place.x() : soundListener.x;
        float f2 = bl ? (float)place.y() : soundListener.y;
        float f3 = (bl ? (float)place.z() : soundListener.z) * 3.0f;
        Hearing.orient(place != null ? place.yaw() : Look.yaw, place != null ? place.pitch() : Look.pitch, forward, up);
        javafmod.FMOD_Studio_Listener3D((int)0, (float)f, (float)f2, (float)f3, (float)0.0f, (float)0.0f, (float)0.0f, (float)forward[0], (float)forward[1], (float)forward[2], (float)up[0], (float)up[1], (float)up[2]);
    }

    public static float cameraZoom(float f) {
        return View.enabled ? 0.0f : f;
    }

    static void orient(float f, float f2, float[] fArray, float[] fArray2) {
        float f3 = (float)Math.cos(f2);
        float f4 = (float)Math.cos(f) * f3;
        float f5 = (float)Math.sin(f) * f3;
        float f6 = (float)Math.sin(f2) * 3.0f / 2.4494896f;
        float f7 = (float)Math.sqrt(f4 * f4 + f5 * f5 + f6 * f6);
        fArray[0] = f4 / f7;
        fArray[1] = f5 / f7;
        fArray[2] = f6 / f7;
        float f8 = -fArray[2] * fArray[0];
        float f9 = -fArray[2] * fArray[1];
        float f10 = 1.0f - fArray[2] * fArray[2];
        float f11 = (float)Math.sqrt(f8 * f8 + f9 * f9 + f10 * f10);
        fArray2[0] = f8 / f11;
        fArray2[1] = f9 / f11;
        fArray2[2] = f10 / f11;
    }

    private Hearing() {
    }
}

