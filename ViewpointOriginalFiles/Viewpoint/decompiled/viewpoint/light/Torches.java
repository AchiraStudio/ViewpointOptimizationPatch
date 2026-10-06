/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.network.GameClient
 */
package viewpoint.light;

import java.util.ArrayList;
import viewpoint.core.Frame;
import viewpoint.render.SceneData;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.iso.LightingJNI;
import zombie.network.GameClient;

public final class Torches {
    private static final int PER_PLAYER = 4;
    private static final int VEHICLE_IDS = 4096;
    private static final float VEHICLE_HEIGHT = 0.65f;
    private static final float HAND_HEIGHT = 1.1f;
    private static final float NEAR = 24.0f;
    private static final float NARROWEST = 0.97f;
    private static final float WIDEST = 0.2f;
    private static final ArrayList<IsoGameCharacter.TorchInfo> listed = new ArrayList();
    private static final IsoGameCharacter.TorchInfo[] kept = new IsoGameCharacter.TorchInfo[8];
    private static final float[] distance = new float[8];

    /*
     * Unable to fully structure code
     */
    public static void snapshot(Frame var0, IsoGameCharacter var1_1, int var2_2) {
        var3_3 = var0.scene;
        var3_3.torchCount = 0;
        LightingJNI.getTorches(Torches.listed);
        if (!(var1_1 instanceof IsoPlayer)) ** GOTO lbl-1000
        var5_4 = (IsoPlayer)var1_1;
        if (GameClient.client) {
            v0 = var5_4.onlineId + 1;
        } else lbl-1000:
        // 2 sources

        {
            v0 = var2_2;
        }
        var4_6 = v0;
        var5_5 = 0;
        for (var6_7 = 0; var6_7 < Torches.listed.size(); ++var6_7) {
            var7_8 = Torches.listed.get(var6_7);
            var8_9 = var7_8.cone != false && var7_8.id > var4_6 * 4 && var7_8.id <= (var4_6 + 1) * 4;
            var9_10 = var7_8.x - var0.camX;
            var10_11 = var7_8.y - var0.camY;
            var11_12 = (var7_8.z - var0.camZ) * 2.0f;
            var12_13 = (float)Math.sqrt(var9_10 * var9_10 + var10_11 * var10_11 + var11_12 * var11_12) - var7_8.dist;
            if (var8_9 || !(var7_8.dist > 0.0f) || !(var12_13 <= 24.0f)) continue;
            var5_5 = Torches.keep(var7_8, var12_13, var5_5);
        }
        Torches.listed.clear();
        for (var6_7 = 0; var6_7 < var5_5; ++var6_7) {
            Torches.write(var3_3, Torches.kept[var6_7], var0);
            Torches.kept[var6_7] = null;
        }
    }

    private static int keep(IsoGameCharacter.TorchInfo torchInfo, float f, int n) {
        int n2;
        for (n2 = n; n2 > 0 && distance[n2 - 1] > f; --n2) {
        }
        if (n2 == 8) {
            return n;
        }
        int n3 = Math.min(n, 7);
        System.arraycopy(distance, n2, distance, n2 + 1, n3 - n2);
        System.arraycopy(kept, n2, kept, n2 + 1, n3 - n2);
        Torches.distance[n2] = f;
        Torches.kept[n2] = torchInfo;
        return n3 + 1;
    }

    private static void write(SceneData sceneData, IsoGameCharacter.TorchInfo torchInfo, Frame frame) {
        int n = sceneData.torchCount * 11;
        float f = torchInfo.id >= 4096 ? 0.65f : 1.1f;
        float f2 = (float)Math.sqrt(torchInfo.angleX * torchInfo.angleX + torchInfo.angleY * torchInfo.angleY);
        boolean bl = torchInfo.cone && f2 > 0.0f;
        sceneData.torches[n] = -(torchInfo.x - frame.camX);
        sceneData.torches[n + 1] = (torchInfo.z - frame.camZ) * 2.4494896f + f;
        sceneData.torches[n + 2] = -(torchInfo.y - frame.camY);
        sceneData.torches[n + 3] = torchInfo.dist;
        sceneData.torches[n + 4] = bl ? -torchInfo.angleX / f2 : 0.0f;
        sceneData.torches[n + 5] = 0.0f;
        sceneData.torches[n + 6] = bl ? -torchInfo.angleY / f2 : 0.0f;
        sceneData.torches[n + 7] = bl ? Math.max(0.2f, Math.min(0.97f, torchInfo.dot)) : -1.0f;
        sceneData.torches[n + 8] = torchInfo.r * torchInfo.strength;
        sceneData.torches[n + 9] = torchInfo.g * torchInfo.strength;
        sceneData.torches[n + 10] = torchInfo.b * torchInfo.strength;
        ++sceneData.torchCount;
    }

    private Torches() {
    }
}

