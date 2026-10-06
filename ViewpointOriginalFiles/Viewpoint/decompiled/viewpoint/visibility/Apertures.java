/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.objects.IsoDoor
 *  zombie.iso.objects.IsoThumpable
 *  zombie.util.list.PZArrayList
 */
package viewpoint.visibility;

import java.util.Arrays;
import viewpoint.render.SceneData;
import viewpoint.visibility.Capture;
import viewpoint.visibility.Glass;
import viewpoint.visibility.Portals;
import viewpoint.visibility.SectorGraph;
import viewpoint.visibility.Topology;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.util.list.PZArrayList;

final class Apertures {
    static final byte OPEN = 0;
    static final byte CLOSED = 1;
    static final byte UNKNOWN = 2;
    private static Topology.Leaf[] leaves = new Topology.Leaf[0];
    private static SectorGraph leavesOf;
    private static int leavesAt;
    static int windows;
    static int windowsClosed;

    static void resolve(SectorGraph sectorGraph, Topology topology, byte[] byArray, SceneData sceneData, Portals.View view) {
        Apertures.index(sectorGraph, topology);
        windowsClosed = 0;
        windows = 0;
        for (int i = 0; i < sectorGraph.doorLeaf.length; ++i) {
            Topology.Leaf leaf = leaves[i];
            int n = sectorGraph.doorSlot[i];
            boolean bl = Glass.opaque(sceneData, sectorGraph.doorPlace, i * 5, view);
            if (sectorGraph.doorWindow[i]) {
                boolean bl2 = bl && leaf != null && leaf.glazed(n);
                ++windows;
                windowsClosed += bl2 ? 1 : 0;
                byArray[i] = bl2 ? (byte)1 : 0;
                continue;
            }
            byArray[i] = Apertures.state(leaf == null ? null : leaf.doors[n], bl);
        }
    }

    static void clear() {
        Arrays.fill(leaves, null);
        leavesOf = null;
    }

    private static void index(SectorGraph sectorGraph, Topology topology) {
        if (sectorGraph == leavesOf && topology.generation() == leavesAt) {
            return;
        }
        int n = sectorGraph.doorLeaf.length;
        leaves = leaves.length >= n ? leaves : new Topology.Leaf[n + n / 2];
        Arrays.fill(leaves, n, leaves.length, null);
        for (int i = 0; i < n; ++i) {
            Apertures.leaves[i] = topology.leaf(sectorGraph.doorLeaf[i]);
        }
        leavesOf = sectorGraph;
        leavesAt = topology.generation();
    }

    static byte state(IsoObject isoObject, boolean bl) {
        try {
            if (isoObject == null || !Apertures.inWorld(isoObject)) {
                return 2;
            }
            if (isoObject instanceof IsoDoor) {
                IsoDoor isoDoor = (IsoDoor)isoObject;
                return isoDoor.IsOpen() || isoDoor.isDestroyed() || Apertures.seeThrough(isoObject, bl) ? (byte)0 : 1;
            }
            if (isoObject instanceof IsoThumpable) {
                IsoThumpable isoThumpable = (IsoThumpable)isoObject;
                return !isoThumpable.isDoor() || isoThumpable.IsOpen() || isoThumpable.isDestroyed() || Apertures.seeThrough(isoObject, bl) ? (byte)0 : 1;
            }
            return 2;
        }
        catch (RuntimeException runtimeException) {
            return 2;
        }
    }

    private static boolean inWorld(IsoObject isoObject) {
        IsoGridSquare isoGridSquare = isoObject.getSquare();
        if (isoGridSquare == null) {
            return false;
        }
        PZArrayList pZArrayList = isoGridSquare.getObjects();
        for (int i = 0; i < pZArrayList.size(); ++i) {
            if (pZArrayList.get(i) != isoObject) continue;
            return true;
        }
        return false;
    }

    private static boolean seeThrough(IsoObject isoObject, boolean bl) {
        boolean bl2 = isoObject.getProperties() != null && isoObject.getProperties().has("doorTrans");
        return bl2 && (!bl || Capture.drawnAsModel(isoObject));
    }

    private Apertures() {
    }
}

