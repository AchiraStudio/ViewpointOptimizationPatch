/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.visibility;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import viewpoint.platform.LongMap;
import viewpoint.visibility.Fragment;
import viewpoint.visibility.SectorGraph;

final class GraphBuilder {
    private final Fragment[] leaves;
    private final LongMap<Integer> index;
    private final int[] parent;
    private final boolean[] uncertain;
    private final int beyondNode;
    private final SectorGraph.Openings raw = new SectorGraph.Openings();
    private long[] doorLeaf = new long[64];
    private int[] doorSlot = new int[64];
    private boolean[] doorWindow = new boolean[64];
    private float[] doorPlace = new float[320];
    private int doors;

    static SectorGraph compile(Collection<Fragment> collection) {
        GraphBuilder graphBuilder = new GraphBuilder(collection.toArray(new Fragment[0]));
        graphBuilder.link();
        return graphBuilder.build();
    }

    private GraphBuilder(Fragment[] fragmentArray) {
        int n;
        this.leaves = fragmentArray;
        this.index = new LongMap(fragmentArray.length);
        for (n = 0; n < fragmentArray.length; ++n) {
            this.index.put(fragmentArray[n].key(), n);
        }
        this.beyondNode = fragmentArray.length * 64;
        this.parent = new int[this.beyondNode + 1];
        this.uncertain = new boolean[this.beyondNode + 1];
        for (n = 0; n < this.parent.length; ++n) {
            this.parent[n] = n;
        }
    }

    private int leaf(int n, int n2, int n3) {
        Integer n4 = this.index.get(Fragment.key(n, n2, n3));
        return n4 == null ? -1 : n4;
    }

    private void link() {
        for (int i = 0; i < this.leaves.length; ++i) {
            Fragment fragment = this.leaves[i];
            int n = this.leaf(fragment.chunkX - 1, fragment.chunkY, fragment.level);
            int n2 = this.leaf(fragment.chunkX, fragment.chunkY - 1, fragment.level);
            int n3 = this.leaf(fragment.chunkX + 1, fragment.chunkY, fragment.level);
            int n4 = this.leaf(fragment.chunkX, fragment.chunkY + 1, fragment.level);
            int n5 = this.leaf(fragment.chunkX, fragment.chunkY, fragment.level + 1);
            int n6 = this.leaf(fragment.chunkX, fragment.chunkY, fragment.level - 1);
            for (int j = 0; j < 64; ++j) {
                if (!fragment.has(fragment.known, j)) continue;
                int n7 = i * 64 + j;
                int n8 = j % 8;
                int n9 = j / 8;
                this.edge(fragment, n7, j, true, n9 > 0 ? i : n2, n9 > 0 ? j - 8 : j + 64 - 8);
                this.edge(fragment, n7, j, false, n8 > 0 ? i : n, n8 > 0 ? j - 1 : j + 8 - 1);
                int n10 = n7;
                this.uncertain[n10] = this.uncertain[n10] | !this.known(n8 < 7 ? i : n3, n8 < 7 ? j + 1 : j - n8);
                int n11 = n7;
                this.uncertain[n11] = this.uncertain[n11] | !this.known(n9 < 7 ? i : n4, n9 < 7 ? j + 8 : j % 8);
                this.above(fragment, n7, j, n5);
                this.below(fragment, n7, j, n6);
            }
        }
    }

    private boolean known(int n, int n2) {
        return n >= 0 && this.leaves[n].has(this.leaves[n].known, n2);
    }

    private void edge(Fragment fragment, int n, int n2, boolean bl, int n3, int n4) {
        int n5;
        int n6;
        boolean bl2;
        boolean bl3;
        boolean bl4 = fragment.has(bl ? fragment.northWindow : fragment.westWindow, n2);
        boolean bl5 = bl4 || fragment.has(bl ? fragment.northDoor : fragment.westDoor, n2) ? true : (bl3 = false);
        boolean bl6 = bl3 || fragment.has(bl ? fragment.northOpening : fragment.westOpening, n2) ? true : (bl2 = false);
        if (!bl2 && fragment.has(bl ? fragment.northWall : fragment.westWall, n2)) {
            return;
        }
        Fragment fragment2 = n3 < 0 ? null : this.leaves[n3];
        int n7 = n6 = fragment2 != null && fragment2.has(fragment2.known, n4) ? n3 * 64 + n4 : -1;
        if (n6 < 0 && !bl2) {
            this.uncertain[n] = true;
            return;
        }
        if (n6 >= 0 && !bl2 && fragment.room(n2) == fragment2.room(n4)) {
            this.union(n, n6);
            return;
        }
        int n8 = fragment.chunkX * 8 + n2 % 8;
        int n9 = fragment.chunkY * 8 + n2 / 8;
        int n10 = bl3 ? this.door(fragment.key(), n2 * 2 + (bl ? 0 : 1), bl4) : (n5 = -1);
        if (bl3) {
            this.placeDoor(n5, n8, n9, bl ? (float)(n8 + 1) : (float)n8, bl ? (float)n9 : (float)(n9 + 1), fragment.level);
        }
        int n11 = this.raw.add(n, n6 < 0 ? this.beyondNode : n6, n5, bl ? (byte)0 : 1);
        this.place(n11, n8, n9, bl ? (float)(n8 + 1) : (float)n8, bl ? (float)n9 : (float)(n9 + 1), fragment.level);
    }

    private void above(Fragment fragment, int n, int n2, int n3) {
        boolean bl;
        Fragment fragment2 = n3 < 0 ? null : this.leaves[n3];
        boolean bl2 = fragment2 != null && fragment2.has(fragment2.known, n2);
        boolean bl3 = bl = fragment.has(fragment.stairs, n2) || bl2 && fragment2.has(fragment2.stairs, n2);
        if (!bl && (bl2 ? fragment2.has(fragment2.floor, n2) : fragment.has(fragment.roofed, n2))) {
            return;
        }
        if (!bl2) {
            this.uncertain[n] = true;
            return;
        }
        int n4 = n3 * 64 + n2;
        if (fragment.room(n2) == fragment2.room(n2)) {
            this.union(n, n4);
            return;
        }
        int n5 = fragment.chunkX * 8 + n2 % 8;
        int n6 = fragment.chunkY * 8 + n2 / 8;
        this.place(this.raw.add(n, n4, -1, (byte)2), n5, n6, n5 + 1, n6 + 1, fragment.level + 1);
    }

    private void below(Fragment fragment, int n, int n2, int n3) {
        Fragment fragment2;
        Fragment fragment3 = fragment2 = n3 < 0 ? null : this.leaves[n3];
        if (fragment.bottom || fragment2 != null && fragment2.has(fragment2.known, n2)) {
            return;
        }
        if (!fragment.has(fragment.floor, n2) || fragment.has(fragment.stairs, n2)) {
            this.uncertain[n] = true;
        }
    }

    private void place(int n, float f, float f2, float f3, float f4, int n2) {
        int n3 = n * 5;
        this.raw.place[n3] = f;
        this.raw.place[n3 + 1] = f2;
        this.raw.place[n3 + 2] = f3;
        this.raw.place[n3 + 3] = f4;
        this.raw.place[n3 + 4] = n2;
    }

    private int door(long l, int n, boolean bl) {
        if (this.doors == this.doorLeaf.length) {
            this.doorLeaf = Arrays.copyOf(this.doorLeaf, this.doors * 2);
            this.doorSlot = Arrays.copyOf(this.doorSlot, this.doors * 2);
            this.doorWindow = Arrays.copyOf(this.doorWindow, this.doors * 2);
            this.doorPlace = Arrays.copyOf(this.doorPlace, this.doors * 10);
        }
        this.doorLeaf[this.doors] = l;
        this.doorSlot[this.doors] = n;
        this.doorWindow[this.doors] = bl;
        return this.doors++;
    }

    private void placeDoor(int n, float f, float f2, float f3, float f4, int n2) {
        int n3 = n * 5;
        this.doorPlace[n3] = f;
        this.doorPlace[n3 + 1] = f2;
        this.doorPlace[n3 + 2] = f3;
        this.doorPlace[n3 + 3] = f4;
        this.doorPlace[n3 + 4] = n2;
    }

    private int find(int n) {
        while (this.parent[n] != n) {
            this.parent[n] = this.parent[this.parent[n]];
            n = this.parent[n];
        }
        return n;
    }

    private void union(int n, int n2) {
        int n3;
        int n4 = this.find(n);
        if (n4 != (n3 = this.find(n2))) {
            this.parent[Math.max((int)n4, (int)n3)] = Math.min(n4, n3);
        }
    }

    private SectorGraph build() {
        int n;
        int[] nArray = new int[this.beyondNode + 1];
        int[] nArray2 = new int[this.beyondNode + 1];
        Arrays.fill(nArray, -1);
        Arrays.fill(nArray2, -1);
        int n2 = 0;
        for (int i = 0; i < this.beyondNode; ++i) {
            if (!this.leaves[i / 64].has(this.leaves[i / 64].known, i % 64)) continue;
            int n3 = this.find(i);
            nArray2[n3] = nArray2[n3] < 0 ? n2++ : nArray2[n3];
            nArray[i] = nArray2[n3];
        }
        nArray[this.beyondNode] = n2++;
        boolean[] blArray = new boolean[n2];
        boolean[] blArray2 = new boolean[n2];
        boolean[] blArray3 = new boolean[n2];
        blArray[n2 - 1] = true;
        for (int i = 0; i < this.beyondNode; ++i) {
            n = nArray[i];
            if (n < 0) continue;
            boolean bl = this.leaves[i / 64].room(i % 64) < 0L;
            int n4 = n;
            blArray[n4] = blArray[n4] | bl;
            int n5 = n;
            blArray2[n5] = blArray2[n5] | !bl;
            int n6 = n;
            blArray3[n6] = blArray3[n6] | this.uncertain[i];
        }
        boolean[] blArray4 = new boolean[n2];
        for (n = 0; n < n2; ++n) {
            blArray4[n] = blArray2[n] && !blArray[n] && !blArray3[n];
        }
        HashMap<Long, Fragment> hashMap = new HashMap<Long, Fragment>(this.leaves.length * 2);
        for (Fragment fragment : this.leaves) {
            hashMap.put(fragment.key(), fragment);
        }
        return new SectorGraph(hashMap, this.index, Arrays.copyOf(nArray, this.beyondNode), blArray, blArray4, this.merged(nArray), Arrays.copyOf(this.doorLeaf, this.doors), Arrays.copyOf(this.doorSlot, this.doors), Arrays.copyOf(this.doorWindow, this.doors), Arrays.copyOf(this.doorPlace, this.doors * 5));
    }

    private SectorGraph.Openings merged(int[] nArray) {
        SectorGraph.Openings openings = new SectorGraph.Openings();
        HashMap<Span, Integer> hashMap = new HashMap<Span, Integer>();
        for (int i = 0; i < this.raw.count; ++i) {
            int n;
            Integer n2;
            int n3 = nArray[this.raw.a[i]];
            int n4 = nArray[this.raw.b[i]];
            if (n3 == n4) continue;
            int n5 = i * 5;
            float[] fArray = this.raw.place;
            byte by = this.raw.kind[i];
            float f = by == 0 ? fArray[n5 + 1] : (by == 1 ? fArray[n5] : 0.0f);
            Span span = this.raw.door[i] >= 0 ? null : new Span(n3, n4, by, f, fArray[n5 + 4]);
            Integer n6 = n2 = span == null ? null : (Integer)hashMap.get(span);
            if (n2 != null) {
                n = n2 * 5;
                openings.place[n] = Math.min(openings.place[n], fArray[n5]);
                openings.place[n + 1] = Math.min(openings.place[n + 1], fArray[n5 + 1]);
                openings.place[n + 2] = Math.max(openings.place[n + 2], fArray[n5 + 2]);
                openings.place[n + 3] = Math.max(openings.place[n + 3], fArray[n5 + 3]);
                continue;
            }
            n = openings.add(n3, n4, this.raw.door[i], by);
            System.arraycopy(fArray, n5, openings.place, n * 5, 5);
            if (span == null) continue;
            hashMap.put(span, n);
        }
        return openings;
    }

    private record Span(int a, int b, byte kind, float line, float level) {
    }
}

