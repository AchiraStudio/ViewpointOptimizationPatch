/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.visibility;

import java.util.Arrays;
import java.util.HashMap;
import viewpoint.platform.LongMap;
import viewpoint.visibility.Fragment;

final class SectorGraph {
    static final byte NORTH_EDGE = 0;
    static final byte WEST_EDGE = 1;
    static final byte FLOOR = 2;
    final HashMap<Long, Fragment> sources;
    private final LongMap<Integer> leafIndex;
    private final int[] sectorOf;
    final int sectors;
    final int beyond;
    final boolean[] exterior;
    final boolean[] hideable;
    final int openings;
    final int[] openA;
    final int[] openB;
    final int[] openDoor;
    final byte[] openKind;
    final float[] openPlace;
    final int[] adjacentStart;
    final int[] adjacent;
    final long[] doorLeaf;
    final int[] doorSlot;
    final boolean[] doorWindow;
    final float[] doorPlace;

    SectorGraph(HashMap<Long, Fragment> hashMap, LongMap<Integer> longMap, int[] nArray, boolean[] blArray, boolean[] blArray2, Openings openings, long[] lArray, int[] nArray2, boolean[] blArray3, float[] fArray) {
        int n;
        this.sources = hashMap;
        this.leafIndex = longMap;
        this.sectorOf = nArray;
        this.sectors = blArray.length;
        this.beyond = this.sectors - 1;
        this.exterior = blArray;
        this.hideable = blArray2;
        this.openings = openings.count;
        this.openA = openings.a;
        this.openB = openings.b;
        this.openDoor = openings.door;
        this.openKind = openings.kind;
        this.openPlace = openings.place;
        this.doorLeaf = lArray;
        this.doorSlot = nArray2;
        this.doorWindow = blArray3;
        this.doorPlace = fArray;
        this.adjacentStart = new int[this.sectors + 1];
        for (n = 0; n < this.openings; ++n) {
            int n2 = this.openA[n] + 1;
            this.adjacentStart[n2] = this.adjacentStart[n2] + 1;
            int n3 = this.openB[n] + 1;
            this.adjacentStart[n3] = this.adjacentStart[n3] + 1;
        }
        for (n = 0; n < this.sectors; ++n) {
            int n4 = n + 1;
            this.adjacentStart[n4] = this.adjacentStart[n4] + this.adjacentStart[n];
        }
        this.adjacent = new int[this.adjacentStart[this.sectors]];
        int[] nArray3 = (int[])this.adjacentStart.clone();
        int n5 = 0;
        while (n5 < this.openings) {
            int n6 = this.openA[n5];
            int n7 = nArray3[n6];
            nArray3[n6] = n7 + 1;
            this.adjacent[n7] = n5;
            int n8 = this.openB[n5];
            int n9 = nArray3[n8];
            nArray3[n8] = n9 + 1;
            this.adjacent[n9] = n5++;
        }
    }

    int sector(long l, int n) {
        Integer n2 = this.leafIndex.get(l);
        return n2 == null ? -1 : this.sectorOf[n2 * 64 + n];
    }

    static final class Openings {
        int count;
        int[] a = new int[256];
        int[] b = new int[256];
        int[] door = new int[256];
        byte[] kind = new byte[256];
        float[] place = new float[1280];

        Openings() {
        }

        int add(int n, int n2, int n3, byte by) {
            if (this.count == this.a.length) {
                this.a = Arrays.copyOf(this.a, this.count * 2);
                this.b = Arrays.copyOf(this.b, this.count * 2);
                this.door = Arrays.copyOf(this.door, this.count * 2);
                this.kind = Arrays.copyOf(this.kind, this.count * 2);
                this.place = Arrays.copyOf(this.place, this.count * 10);
            }
            this.a[this.count] = n;
            this.b[this.count] = n2;
            this.door[this.count] = n3;
            this.kind[this.count] = by;
            return this.count++;
        }
    }
}

