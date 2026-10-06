/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.util.Arrays;
import java.util.List;
import viewpoint.light.LightFlood;
import viewpoint.light.LightLayout;
import viewpoint.light.SourceRegister;

final class LightModel {
    static final byte SKY_REACH = 15;
    static final byte NO_SKY = 16;
    static final int ROOM_R = 229;
    static final int ROOM_G = 216;
    static final int ROOM_B = 209;
    static final int ROOM_RADIUS = 7;
    static final int MAX_RADIUS = 20;
    private static final int[] STRAIGHT = new int[]{0, 2, 4, 6};

    static int levelsReached(int n) {
        return (n - 1) / 3;
    }

    static byte[] skySteps(LightLayout lightLayout) {
        byte[] byArray = new byte[lightLayout.size()];
        LightModel.skySteps(lightLayout, byArray, new int[lightLayout.size()]);
        return byArray;
    }

    static void skySteps(LightLayout lightLayout, byte[] byArray, int[] nArray) {
        int n;
        int n2 = 0;
        int n3 = 0;
        Arrays.fill(byArray, 0, lightLayout.size(), (byte)16);
        for (n = 0; n < lightLayout.size(); ++n) {
            if (!lightLayout.exists(n) || !lightLayout.openAir(n)) continue;
            byArray[n] = 0;
            nArray[n3++] = n;
        }
        while (n2 < n3) {
            byte by;
            if ((by = (byte)(byArray[n = nArray[n2++]] + 1)) > 15) continue;
            for (int n4 : STRAIGHT) {
                int n5 = lightLayout.neighbour(n, n4);
                if (n5 < 0 || !lightLayout.exists(n5) || byArray[n5] <= by || lightLayout.pass(n, n4) == 4 && lightLayout.pass(n5, LightLayout.REVERSE[n4]) == 4) continue;
                byArray[n5] = by;
                nArray[n3++] = n5;
            }
        }
    }

    static int[] sources(LightLayout lightLayout, List<SourceRegister.Lamp> list, List<SourceRegister.RoomLight> list2) {
        int[] nArray = new int[lightLayout.size()];
        LightFlood lightFlood = new LightFlood(lightLayout);
        int[] nArray2 = new int[1];
        for (SourceRegister.Lamp object : list) {
            nArray2[0] = lightLayout.index(object.x(), object.y(), object.z());
            int roomLight = Math.min(object.radius(), 20);
            if (nArray2[0] < 0 || roomLight <= 0) continue;
            lightFlood.run(nArray2, 1, roomLight);
            LightModel.brighten(nArray, lightFlood, 255.0 * (double)object.r(), 255.0 * (double)object.g(), 255.0 * (double)object.b(), roomLight);
        }
        Object object = new int[64];
        for (SourceRegister.RoomLight roomLight : list2) {
            int n = 0;
            for (int i = roomLight.y(); i < roomLight.y() + roomLight.height(); ++i) {
                for (int j = roomLight.x(); j < roomLight.x() + roomLight.width(); ++j) {
                    if (n == ((Object)object).length) {
                        object = Arrays.copyOf((int[])object, n * 2);
                    }
                    object[n++] = lightLayout.index(j, i, roomLight.z());
                }
            }
            lightFlood.run((int[])object, n, 7.0);
            LightModel.brighten(nArray, lightFlood, 229.0, 216.0, 209.0, 7.0);
        }
        return nArray;
    }

    static int combine(int n, int n2, int n3) {
        int n4 = 0;
        for (int i = 0; i <= 16; i += 8) {
            int n5 = Math.max(0, (n >> i & 0xFF) - 16 * n2);
            n4 |= Math.max(n5, n3 >> i & 0xFF) << i;
        }
        return n4;
    }

    private static void brighten(int[] nArray, LightFlood lightFlood, double d, double d2, double d3, double d4) {
        for (int i = 0; i < lightFlood.touchedCount(); ++i) {
            int n = lightFlood.touched(i);
            nArray[n] = LightModel.brighter(nArray[n], LightModel.light(lightFlood, n, d, d2, d3, d4));
        }
    }

    static int light(LightFlood lightFlood, int n, double d, double d2, double d3, double d4) {
        double d5 = lightFlood.transmission(n);
        double d6 = 1.0 - lightFlood.cost(n) / d4;
        return (int)Math.floor(d * d5 * d6) | (int)Math.floor(d2 * d5 * d6) << 8 | (int)Math.floor(d3 * d5 * d6) << 16;
    }

    static int brighter(int n, int n2) {
        return Math.max(n & 0xFF, n2 & 0xFF) | Math.max(n >> 8 & 0xFF, n2 >> 8 & 0xFF) << 8 | Math.max(n >> 16 & 0xFF, n2 >> 16 & 0xFF) << 16;
    }

    private LightModel() {
    }
}

