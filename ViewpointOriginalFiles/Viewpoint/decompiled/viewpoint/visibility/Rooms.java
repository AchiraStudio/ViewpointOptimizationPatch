/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.visibility;

import viewpoint.core.Frame;
import viewpoint.platform.Keys;
import viewpoint.platform.Profile;
import viewpoint.platform.Settings;
import viewpoint.render.ChunkMeshData;
import viewpoint.render.SceneData;
import viewpoint.visibility.Apertures;
import viewpoint.visibility.DrawPlans;
import viewpoint.visibility.Fragment;
import viewpoint.visibility.Portals;
import viewpoint.visibility.SectorGraph;
import viewpoint.visibility.Selection;
import viewpoint.visibility.Topology;
import zombie.iso.IsoChunk;

public final class Rooms {
    public static final int ON = 0;
    public static final int FROZEN = 1;
    public static final int OFF = 2;
    private static final String[] MODES = new String[]{"on", "frozen", "off"};
    private static final float MARGIN = (float)Math.toRadians(25.0);
    private static final int MAX_TESTED_SQUARES = 64;
    private static int mode = 0;
    private static final Topology topology = new Topology();
    private static final Selection selection = new Selection();
    private static final Portals.View view = new Portals.View();
    private static SectorGraph graph;
    private static boolean[] hidden;
    private static byte[] doors;
    private static final int[] viewers;
    private static boolean hiding;
    private static boolean captureFailed;
    private static volatile String state;
    private static volatile long selectNanos;
    private static volatile long hiddenSum;
    private static volatile long culledSum;
    private static volatile long modelsSum;
    private static volatile long madeSum;
    private static volatile long keptSum;
    private static volatile long frames;
    private static int modelsLeftOut;
    private static volatile int sectors;
    private static volatile int hideable;

    public static void captured(IsoChunk isoChunk, int n) {
        block3: {
            if (!Settings.rooms) {
                return;
            }
            try {
                topology.capture(isoChunk, n);
            }
            catch (RuntimeException runtimeException) {
                if (captureFailed) break block3;
                captureFailed = true;
                System.out.println("[Viewpoint] rooms: a level's layout could not be read (it hides nothing):");
                runtimeException.printStackTrace(System.out);
            }
        }
    }

    public static void forget(int n, int n2) {
        topology.forget(n, n2);
    }

    public static void clear() {
        topology.clear();
        DrawPlans.clear();
        Apertures.clear();
        graph = null;
        hiding = false;
    }

    public static void cycle() {
        mode = (mode + 1) % MODES.length;
        System.out.println("[Viewpoint] rooms' culling " + MODES[mode]);
    }

    public static void select(Frame frame) {
        long l = System.nanoTime();
        DrawPlans.plansKept = 0L;
        DrawPlans.plansMade = 0L;
        DrawPlans.culledVertices = 0L;
        if (!Settings.rooms) {
            hiding = false;
            state = "off";
            return;
        }
        topology.update();
        if (mode == 1 && hiding) {
            state = "frozen";
        } else if (mode == 2) {
            hiding = false;
            state = "off (" + Keys.ROOMS.display() + ")";
        } else {
            hiding = Rooms.choose(frame);
        }
        selectNanos += System.nanoTime() - l;
        ++frames;
    }

    private static boolean choose(Frame frame) {
        SectorGraph sectorGraph = topology.graph();
        if (sectorGraph == null) {
            state = topology.levels() == 0 ? "no levels" : "compiling";
            return false;
        }
        Rooms.look(frame);
        Rooms.viewers[0] = Rooms.sector(sectorGraph, frame.camX, frame.camY, (int)Math.floor(frame.camZ + 0.01f));
        Rooms.viewers[1] = Rooms.sector(sectorGraph, Rooms.view.x, Rooms.view.y, (int)Math.floor(Rooms.view.h / 2.4494896f));
        if (viewers[0] < 0 && viewers[1] < 0) {
            state = "eye in no sector";
            return false;
        }
        if (hidden.length < sectorGraph.sectors) {
            hidden = new boolean[sectorGraph.sectors + sectorGraph.sectors / 2];
        }
        if (doors.length < sectorGraph.doorLeaf.length) {
            doors = new byte[sectorGraph.doorLeaf.length + sectorGraph.doorLeaf.length / 2];
        }
        Apertures.resolve(sectorGraph, topology, doors, frame.scene, view);
        if (!selection.select(sectorGraph, doors, viewers, view, hidden)) {
            state = "budget out";
            return false;
        }
        graph = sectorGraph;
        state = Rooms.selection.narrowed ? "on" : "on, portal budget out";
        sectors = sectorGraph.sectors;
        int n = 0;
        int n2 = 0;
        for (int i = 0; i < sectorGraph.sectors; ++i) {
            n += hidden[i] ? 1 : 0;
            n2 += sectorGraph.hideable[i] ? 1 : 0;
        }
        hiddenSum += (long)n;
        hideable = n2;
        return true;
    }

    private static int sector(SectorGraph sectorGraph, float f, float f2, int n) {
        int n2 = (int)Math.floor(f);
        int n3 = (int)Math.floor(f2);
        long l = Fragment.key(Math.floorDiv(n2, 8), Math.floorDiv(n3, 8), n);
        return sectorGraph.sector(l, Math.floorMod(n3, 8) * 8 + Math.floorMod(n2, 8));
    }

    private static void look(Frame frame) {
        Rooms.view.x = frame.camX - frame.scene.viewerX;
        Rooms.view.y = frame.camY - frame.scene.viewerZ;
        Rooms.view.h = frame.camZ * 2.4494896f + frame.scene.viewerY;
        view.look(frame.viewYaw, frame.viewPitch);
        Rooms.view.tanX = Rooms.widen(1.0f / frame.scene.projection.m00());
        Rooms.view.tanY = Rooms.widen(1.0f / frame.scene.projection.m11());
    }

    private static float widen(float f) {
        return (float)Math.tan(Math.min(Math.atan(f) + (double)MARGIN, Math.toRadians(85.0)));
    }

    public static boolean hides(float f, float f2, float f3, float f4, int n, int n2) {
        int n3 = (int)Math.floor(f);
        int n4 = (int)Math.floor(f2);
        int n5 = (int)Math.floor(f3);
        int n6 = (int)Math.floor(f4);
        if (!hiding || (long)(n5 - n3 + 1) * (long)(n6 - n4 + 1) * (long)(n2 - n + 1) > 64L) {
            return false;
        }
        for (int i = n; i <= n2; ++i) {
            for (int j = n4; j <= n6; ++j) {
                for (int k = n3; k <= n5; ++k) {
                    int n7 = Rooms.sector(graph, k, j, i);
                    if (n7 >= 0 && n7 < hidden.length && hidden[n7]) continue;
                    return false;
                }
            }
        }
        return true;
    }

    public static void leftOut() {
        ++modelsLeftOut;
    }

    public static void plan(SceneData sceneData, int n, ChunkMeshData chunkMeshData, int n2, int n3) {
        if (hiding) {
            DrawPlans.plan(sceneData, n, chunkMeshData, n2, n3, graph, hidden);
        }
    }

    public static void sweep() {
        DrawPlans.sweep();
    }

    public static void endFrame() {
        culledSum += DrawPlans.culledVertices;
        madeSum += DrawPlans.plansMade;
        keptSum += DrawPlans.plansKept;
        modelsSum += (long)modelsLeftOut;
        modelsLeftOut = 0;
    }

    private static String report() {
        long l = Math.max(frames, 1L);
        String string = String.format("%s; levels %d, sectors %d (%d may hide), hidden %.0f, vertices left out %.0fk, models left out %.0f, plans made %.1f kept %.1f, windows closed %d of %d, select %.2f ms, compile %.1f ms", state, topology.levels(), sectors, hideable, (double)hiddenSum / (double)l, (double)culledSum / 1000.0 / (double)l, (double)modelsSum / (double)l, (double)madeSum / (double)l, (double)keptSum / (double)l, Apertures.windowsClosed, Apertures.windows, (double)selectNanos * 1.0E-6 / (double)l, (double)Rooms.topology.compileNanos * 1.0E-6);
        frames = 0L;
        keptSum = 0L;
        madeSum = 0L;
        modelsSum = 0L;
        culledSum = 0L;
        hiddenSum = 0L;
        selectNanos = 0L;
        return string;
    }

    private Rooms() {
    }

    static {
        hidden = new boolean[0];
        doors = new byte[0];
        viewers = new int[2];
        state = "off";
        Profile.roomsReport = Rooms::report;
    }
}

