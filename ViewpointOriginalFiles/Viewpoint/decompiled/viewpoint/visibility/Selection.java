/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.visibility;

import java.util.Arrays;
import viewpoint.visibility.Portals;
import viewpoint.visibility.SectorGraph;

final class Selection {
    static final int BUDGET = 65536;
    private final Portals portals = new Portals();
    private boolean[] reached = new boolean[0];
    private boolean[] seen = new boolean[0];
    private int[] queue = new int[0];
    boolean narrowed;

    Selection() {
    }

    boolean select(SectorGraph sectorGraph, byte[] byArray, int[] nArray, Portals.View view, boolean[] blArray) {
        if (this.reached.length < sectorGraph.sectors) {
            this.reached = new boolean[sectorGraph.sectors];
            this.seen = new boolean[sectorGraph.sectors];
            this.queue = new int[sectorGraph.sectors];
        }
        Arrays.fill(blArray, 0, sectorGraph.sectors, false);
        if (!this.flood(sectorGraph, byArray, nArray)) {
            return false;
        }
        this.narrowed = this.portals.run(sectorGraph, byArray, nArray, view, this.seen);
        for (int i = 0; i < sectorGraph.sectors; ++i) {
            boolean bl = i == nArray[0] || i == nArray[1];
            blArray[i] = sectorGraph.hideable[i] && (!this.reached[i] || this.narrowed && !bl && !this.seen[i]);
        }
        return true;
    }

    private boolean flood(SectorGraph sectorGraph, byte[] byArray, int[] nArray) {
        int n;
        Arrays.fill(this.reached, 0, sectorGraph.sectors, false);
        int n2 = 0;
        int n3 = 0;
        for (int n4 : nArray) {
            n2 = n4 >= 0 ? this.seed(n4, n2) : n2;
        }
        for (n = 0; n < sectorGraph.sectors; ++n) {
            if (!sectorGraph.exterior[n]) continue;
            n2 = this.seed(n, n2);
        }
        for (n = 0; n < sectorGraph.openings; ++n) {
            if (sectorGraph.openDoor[n] < 0 || byArray[sectorGraph.openDoor[n]] != 2) continue;
            n2 = this.seed(sectorGraph.openA[n], n2);
            n2 = this.seed(sectorGraph.openB[n], n2);
        }
        for (n = 0; n < n2; ++n) {
            int n5 = this.queue[n];
            for (int i = sectorGraph.adjacentStart[n5]; i < sectorGraph.adjacentStart[n5 + 1]; ++i) {
                int n4;
                if (++n3 > 65536) {
                    return false;
                }
                n4 = sectorGraph.adjacent[i];
                if (sectorGraph.openDoor[n4] >= 0 && byArray[sectorGraph.openDoor[n4]] == 1) continue;
                n2 = this.seed(sectorGraph.openA[n4] == n5 ? sectorGraph.openB[n4] : sectorGraph.openA[n4], n2);
            }
        }
        return true;
    }

    private int seed(int n, int n2) {
        if (this.reached[n]) {
            return n2;
        }
        this.reached[n] = true;
        this.queue[n2] = n;
        return n2 + 1;
    }
}

