/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoGridSquare
 */
package viewpoint.light;

import java.util.ArrayList;
import java.util.Arrays;
import viewpoint.core.Frame;
import viewpoint.light.LightSources;
import viewpoint.light.ShadowLamps;
import viewpoint.light.SourceRegister;
import viewpoint.platform.ShaderPacks;
import viewpoint.render.SceneData;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;

public final class Lights {
    private static final int RADIUS = 24;
    private static final int CANDIDATES = 512;
    private static final SourceRegister.Entry[] found = new SourceRegister.Entry[512];
    private static final float[] distance = new float[512];
    private static final Integer[] order = new Integer[512];
    private static final ArrayList<SourceRegister.Entry> on = new ArrayList();
    private static final boolean[] casts = new boolean[48];
    private static final float[] strength = new float[48];
    private static final int[] lampRank = new int[48];
    private static final int[] lampX = new int[48];
    private static final int[] lampY = new int[48];
    private static final int[] lampZ = new int[48];
    private static final float[] lampReach = new float[48];
    private static final float[] lampDistance = new float[48];
    private static final float[] lampStrength = new float[48];
    private static final long[] lampKey = new long[48];
    private static final boolean[] lampCasts = new boolean[48];

    public static void snapshot(Frame frame, IsoCell isoCell) {
        frame.scene.lightCount = 0;
        int n3 = Lights.registered(frame);
        Arrays.sort(order, 0, n3, (n, n2) -> Float.compare(distance[n], distance[n2]));
        int n4 = Math.min(n3, 48);
        frame.scene.lampShadowCount = Lights.shadows(n4, ShaderPacks.drawn().pipeline.lampShadows);
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < n4; ++j) {
                if (casts[j] != (i == 0)) continue;
                Lights.put(frame, isoCell, j);
            }
        }
        Arrays.fill(found, 0, n3, null);
    }

    private static int shadows(int n, int n2) {
        int n3;
        Arrays.fill(casts, false);
        int n4 = 0;
        for (n3 = 0; n3 < n; ++n3) {
            SourceRegister.Lamp lamp = Lights.found[Lights.order[n3].intValue()].lamp;
            if (lamp == null) continue;
            Lights.lampRank[n4] = n3;
            Lights.lampX[n4] = lamp.x();
            Lights.lampY[n4] = lamp.y();
            Lights.lampZ[n4] = lamp.z();
            Lights.lampReach[n4] = lamp.radius();
            Lights.lampKey[n4] = Lights.key(lamp);
            Lights.lampDistance[n4] = distance[order[n3]];
            ++n4;
        }
        n3 = ShadowLamps.choose(n4, lampX, lampY, lampZ, lampReach, lampKey, lampDistance, n2, lampCasts, lampStrength);
        for (int i = 0; i < n4; ++i) {
            Lights.casts[Lights.lampRank[i]] = lampCasts[i];
            Lights.strength[Lights.lampRank[i]] = lampStrength[i];
        }
        return n3;
    }

    private static void put(Frame frame, IsoCell isoCell, int n) {
        int n2 = order[n];
        float f = Lights.smoothstep(24.0f, 18.0f, distance[n2]) * Math.min(1.0f, (float)(48 - n) / 8.0f);
        SourceRegister.Lamp lamp = Lights.found[n2].lamp;
        Lights.put(frame, isoCell, found[n2], f);
        SceneData sceneData = frame.scene;
        int n3 = sceneData.lightCount - 1;
        sceneData.lights[n3 * 10 + 9] = casts[n] ? strength[n] : 0.0f;
        sceneData.lightKeys[n3] = lamp == null ? -1L : Lights.key(lamp);
    }

    private static long key(SourceRegister.Lamp lamp) {
        return (long)(lamp.x() & 0xFFFFFF) << 40 | (long)(lamp.y() & 0xFFFFFF) << 16 | (long)((lamp.z() & 0xFF) << 8) | (long)(lamp.radius() & 0xFF);
    }

    private static int registered(Frame frame) {
        LightSources.register().on(on);
        int n = 0;
        for (int i = 0; i < on.size() && n < 512; ++i) {
            float f;
            SourceRegister.Entry entry = on.get(i);
            if (entry.lamp != null) {
                f = Lights.distance((float)entry.lamp.x() + 0.5f, (float)entry.lamp.y() + 0.5f, entry.lamp.z(), frame);
            } else {
                float f2 = Math.max((float)entry.room.x(), Math.min((float)(entry.room.x() + entry.room.width()), frame.camX));
                float f3 = Math.max((float)entry.room.y(), Math.min((float)(entry.room.y() + entry.room.height()), frame.camY));
                f = Lights.distance(f2, f3, entry.room.z(), frame);
            }
            if (!(f < 24.0f) || entry.lamp != null && entry.lamp.radius() <= 0) continue;
            n = Lights.candidate(entry, f, n);
        }
        on.clear();
        return n;
    }

    private static int candidate(SourceRegister.Entry entry, float f, int n) {
        Lights.found[n] = entry;
        Lights.distance[n] = f;
        Lights.order[n] = n;
        return n + 1;
    }

    private static float distance(float f, float f2, int n, Frame frame) {
        float f3 = f - frame.camX;
        float f4 = f2 - frame.camY;
        float f5 = ((float)n - frame.camZ) * 2.0f;
        return (float)Math.sqrt(f3 * f3 + f4 * f4 + f5 * f5);
    }

    private static void put(Frame frame, IsoCell isoCell, SourceRegister.Entry entry, float f) {
        SourceRegister.Lamp lamp = entry.lamp;
        if (lamp != null) {
            Lights.put(frame, isoCell, lamp.x(), lamp.y(), lamp.z(), lamp.radius(), lamp.r(), lamp.g(), lamp.b(), f);
            return;
        }
        SourceRegister.RoomLight roomLight = entry.room;
        float f2 = 7.0f + 0.5f * (float)Math.hypot(roomLight.width(), roomLight.height());
        int n = Lights.put(frame, (float)roomLight.x() + 0.5f * (float)roomLight.width(), (float)roomLight.y() + 0.5f * (float)roomLight.height(), roomLight.z(), f2, f);
        frame.scene.lights[n + 4] = 0.8980392f;
        frame.scene.lights[n + 5] = 0.84705883f;
        frame.scene.lights[n + 6] = 0.81960785f;
        frame.scene.lights[n + 8] = 0.0f;
    }

    private static void put(Frame frame, IsoCell isoCell, int n, int n2, int n3, float f, float f2, float f3, float f4, float f5) {
        int n4 = Lights.put(frame, (float)n + 0.5f, (float)n2 + 0.5f, n3, f, f5);
        frame.scene.lights[n4 + 4] = f2;
        frame.scene.lights[n4 + 5] = f3;
        frame.scene.lights[n4 + 6] = f4;
        IsoGridSquare isoGridSquare = isoCell.getGridSquare(n, n2, n3);
        frame.scene.lights[n4 + 8] = isoGridSquare == null || isoGridSquare.isOutside() ? 1.0f : 0.0f;
    }

    private static int put(Frame frame, float f, float f2, int n, float f3, float f4) {
        SceneData sceneData = frame.scene;
        int n2 = sceneData.lightCount * 10;
        sceneData.lights[n2] = -(f - frame.camX);
        sceneData.lights[n2 + 1] = ((float)n - frame.camZ) * 2.4494896f + 2.1f;
        sceneData.lights[n2 + 2] = -(f2 - frame.camY);
        sceneData.lights[n2 + 3] = f3;
        sceneData.lights[n2 + 7] = f4;
        ++sceneData.lightCount;
        return n2;
    }

    private static float smoothstep(float f, float f2, float f3) {
        float f4 = Math.max(0.0f, Math.min(1.0f, (f3 - f) / (f2 - f)));
        return f4 * f4 * (3.0f - 2.0f * f4);
    }

    private Lights() {
    }
}

