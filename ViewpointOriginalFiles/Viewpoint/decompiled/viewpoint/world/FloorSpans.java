/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoGridSquare
 */
package viewpoint.world;

import viewpoint.render.FloorArt;
import viewpoint.visibility.Edges;
import viewpoint.visibility.Owner;
import viewpoint.world.Recipe;
import viewpoint.world.WorldMesher;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;

final class FloorSpans {
    private static final int SIZE = 8;
    private static final int SQUARES = 64;
    private static final int[] group = new int[64];
    private static final boolean[] present = new boolean[64];
    private static final boolean[] covered = new boolean[64];
    private static final int[] rectangles = new int[256];

    static void emit(Recipe.Batch batch, IsoChunk isoChunk, int n, FloorArt floorArt, float f) {
        int n2;
        for (n2 = 0; n2 < 64; ++n2) {
            FloorSpans.present[n2] = floorArt.present(n2);
        }
        FloorSpans.groups(isoChunk, n);
        n2 = FloorSpans.rectangles(present, group, rectangles);
        for (int i = 0; i < n2; ++i) {
            int n3 = rectangles[i * 4];
            int n4 = rectangles[i * 4 + 1];
            WorldMesher.owner = Owner.of(n4 * 8 + n3, 3);
            WorldMesher.raw(batch, FloorSpans.rectangle(n3, n4, rectangles[i * 4 + 2], rectangles[i * 4 + 3], f));
        }
        WorldMesher.owner = -1;
    }

    static int rectangles(boolean[] blArray, int[] nArray, int[] nArray2) {
        boolean[] blArray2 = new boolean[64];
        int n = 0;
        for (int i = 0; i < 64; ++i) {
            int n2;
            if (blArray2[i] || !blArray[i]) continue;
            int n3 = i % 8;
            int n4 = i / 8;
            int n5 = n4 + 1;
            for (n2 = n3 + 1; n2 < 8 && FloorSpans.joins(i, n4 * 8 + n2, blArray, nArray, blArray2); ++n2) {
            }
            while (n5 < 8 && FloorSpans.rowJoins(i, n3, n2, n5, blArray, nArray, blArray2)) {
                ++n5;
            }
            for (int j = n4; j < n5; ++j) {
                for (int k = n3; k < n2; ++k) {
                    blArray2[j * 8 + k] = true;
                }
            }
            nArray2[n * 4] = n3;
            nArray2[n * 4 + 1] = n4;
            nArray2[n * 4 + 2] = n2;
            nArray2[n * 4 + 3] = n5;
            ++n;
        }
        return n;
    }

    private static boolean joins(int n, int n2, boolean[] blArray, int[] nArray, boolean[] blArray2) {
        return !blArray2[n2] && blArray[n2] && nArray[n2] == nArray[n];
    }

    private static boolean rowJoins(int n, int n2, int n3, int n4, boolean[] blArray, int[] nArray, boolean[] blArray2) {
        for (int i = n2; i < n3; ++i) {
            if (FloorSpans.joins(n, n4 * 8 + i, blArray, nArray, blArray2)) continue;
            return false;
        }
        return true;
    }

    private static void groups(IsoChunk isoChunk, int n) {
        int n2;
        for (n2 = 0; n2 < 64; ++n2) {
            FloorSpans.group[n2] = n2;
        }
        for (n2 = 0; n2 < 64; ++n2) {
            int n3 = n2 % 8;
            int n4 = n2 / 8;
            if (!present[n2]) continue;
            if (n3 > 0 && present[n2 - 1] && FloorSpans.sameSectors(isoChunk, n, n3, n4, false)) {
                FloorSpans.union(n2, n2 - 1);
            }
            if (n4 <= 0 || !present[n2 - 8] || !FloorSpans.sameSectors(isoChunk, n, n3, n4, true)) continue;
            FloorSpans.union(n2, n2 - 8);
        }
        for (n2 = 0; n2 < 64; ++n2) {
            FloorSpans.group[n2] = FloorSpans.find(n2);
        }
    }

    private static boolean sameSectors(IsoChunk isoChunk, int n, int n2, int n3, boolean bl) {
        int n4;
        int n5 = bl ? n2 : n2 - 1;
        int n6 = n4 = bl ? n3 - 1 : n3;
        if (!FloorSpans.joined(isoChunk.getGridSquare(n2, n3, n), isoChunk.getGridSquare(n5, n4, n), bl)) {
            return false;
        }
        IsoGridSquare isoGridSquare = isoChunk.getGridSquare(n2, n3, n - 1);
        IsoGridSquare isoGridSquare2 = isoChunk.getGridSquare(n5, n4, n - 1);
        return isoGridSquare == null && isoGridSquare2 == null || FloorSpans.joined(isoGridSquare, isoGridSquare2, bl);
    }

    private static boolean joined(IsoGridSquare isoGridSquare, IsoGridSquare isoGridSquare2, boolean bl) {
        return isoGridSquare != null && isoGridSquare2 != null && FloorSpans.room(isoGridSquare) == FloorSpans.room(isoGridSquare2) && Edges.edge(isoGridSquare, bl) == 0;
    }

    private static long room(IsoGridSquare isoGridSquare) {
        return isoGridSquare.getRoom() != null && !isoGridSquare.isOutside() ? isoGridSquare.getRoomID() : -1L;
    }

    private static int find(int n) {
        while (group[n] != n) {
            FloorSpans.group[n] = group[group[n]];
            n = group[n];
        }
        return n;
    }

    private static void union(int n, int n2) {
        FloorSpans.group[FloorSpans.find((int)n)] = FloorSpans.find(n2);
    }

    private static float[] rectangle(int n, int n2, int n3, int n4, float f) {
        float f2 = 10.0f;
        return Recipe.floorVertices(n, n2, n3, n4, f, new float[]{(float)(n + 1) / f2, (float)(n2 + 1) / f2, (float)(n3 + 1) / f2, (float)(n4 + 1) / f2});
    }

    private FloorSpans() {
    }
}

