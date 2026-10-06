/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.visibility;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import viewpoint.platform.Caches;
import viewpoint.render.ChunkMeshData;
import viewpoint.render.SceneData;
import viewpoint.visibility.Fragment;
import viewpoint.visibility.Owner;
import viewpoint.visibility.SectorGraph;

final class DrawPlans {
    private static final int MAX_COMMANDS = 16;
    private static final long KEEP_FRAMES = 240L;
    private static final IdentityHashMap<ChunkMeshData, Resolved> resolved = new IdentityHashMap();
    private static final int[] found = new int[65];
    private static boolean[] marked = new boolean[0];
    private static long bytes;
    static long culledVertices;
    static long plansMade;
    static long plansKept;

    static void plan(SceneData sceneData, int n, ChunkMeshData chunkMeshData2, int n2, int n3, SectorGraph sectorGraph, boolean[] blArray) {
        long l;
        if (chunkMeshData2.runs == null && !chunkMeshData2.hasModels()) {
            return;
        }
        Resolved resolved = DrawPlans.resolved.computeIfAbsent(chunkMeshData2, chunkMeshData -> new Resolved());
        resolved.used = Caches.tick();
        if (resolved.graph != sectorGraph) {
            bytes -= DrawPlans.size(resolved);
            DrawPlans.resolve(resolved, chunkMeshData2, n2, n3, sectorGraph);
            bytes += DrawPlans.size(resolved);
        }
        if (resolved.squares != null) {
            sceneData.meshModelSquares[n] = DrawPlans.visibleSquares(resolved.squares, blArray);
        }
        if (chunkMeshData2.runs == null || !resolved.canHide) {
            return;
        }
        int n4 = sceneData.planEntries;
        long l2 = l = resolved.sectors == null ? 0L : DrawPlans.hiddenBits(resolved.sectors, blArray);
        if (resolved.planned && l == resolved.planHidden) {
            for (int i = 0; i < resolved.entryCount * 3; i += 3) {
                sceneData.addPlanEntry(resolved.entries[i], resolved.entries[i + 1], resolved.entries[i + 2]);
            }
            ++plansKept;
        } else {
            resolved.left = DrawPlans.leaveOut(sceneData, chunkMeshData2, resolved, blArray);
            DrawPlans.keep(resolved, sceneData, n4, l);
            ++plansMade;
        }
        if (resolved.left == 0L) {
            sceneData.planEntries = n4;
        } else {
            sceneData.meshPlan[n] = n4;
            sceneData.meshPlanLength[n] = sceneData.planEntries - n4;
            culledVertices += resolved.left;
        }
    }

    private static long leaveOut(SceneData sceneData, ChunkMeshData chunkMeshData, Resolved resolved, boolean[] blArray) {
        long l = 0L;
        for (int i = 0; i < chunkMeshData.pages.length; ++i) {
            int n = sceneData.planEntries;
            int n2 = 0;
            int n3 = -1;
            int n4 = 0;
            for (int j = chunkMeshData.pageRuns[i]; j < chunkMeshData.pageRuns[i + 1]; ++j) {
                int n5 = chunkMeshData.runs[j * 3 + 1];
                int n6 = chunkMeshData.runs[j * 3 + 2];
                if (resolved.a[j] >= 0 && blArray[resolved.a[j]] && blArray[resolved.b[j]]) {
                    l += (long)n6;
                    continue;
                }
                if (n3 >= 0 && n3 + n4 == n5) {
                    n4 += n6;
                    continue;
                }
                n2 += n3 >= 0 ? sceneData.addPlanEntry(i, n3, n4) : 0;
                n3 = n5;
                n4 = n6;
            }
            if ((n2 += n3 >= 0 ? sceneData.addPlanEntry(i, n3, n4) : 0) <= 16) continue;
            sceneData.planEntries = n;
            sceneData.addPlanEntry(i, chunkMeshData.first[i], chunkMeshData.count[i]);
        }
        return l;
    }

    private static void keep(Resolved resolved, SceneData sceneData, int n, long l) {
        resolved.planned = resolved.sectors != null;
        resolved.planHidden = l;
        int n2 = resolved.entryCount = resolved.planned && resolved.left > 0L ? sceneData.planEntries - n : 0;
        if (resolved.entries.length < resolved.entryCount * 3) {
            bytes -= DrawPlans.size(resolved);
            resolved.entries = new int[resolved.entryCount * 3];
            bytes += DrawPlans.size(resolved);
        }
        System.arraycopy(sceneData.plan, n * 3, resolved.entries, 0, resolved.entryCount * 3);
    }

    static long visibleSquares(int[] nArray, boolean[] blArray) {
        long l = -1L;
        for (int i = 0; i < nArray.length; ++i) {
            l &= nArray[i] >= 0 && blArray[nArray[i]] ? 1L << i ^ 0xFFFFFFFFFFFFFFFFL : -1L;
        }
        return l;
    }

    private static long hiddenBits(int[] nArray, boolean[] blArray) {
        long l = 0L;
        for (int i = 0; i < nArray.length; ++i) {
            l |= blArray[nArray[i]] ? 1L << i : 0L;
        }
        return l;
    }

    private static void resolve(Resolved resolved, ChunkMeshData chunkMeshData, int n, int n2, SectorGraph sectorGraph) {
        int n3 = chunkMeshData.runs == null ? 0 : chunkMeshData.runs.length / 3;
        int n4 = chunkMeshData.level;
        long l = Fragment.key(n, n2, n4);
        resolved.graph = sectorGraph;
        resolved.squares = chunkMeshData.hasModels() ? DrawPlans.squares(resolved.squares, l, sectorGraph) : null;
        resolved.a = resolved.a != null && resolved.a.length == n3 ? resolved.a : new int[n3];
        resolved.b = resolved.b != null && resolved.b.length == n3 ? resolved.b : new int[n3];
        resolved.canHide = false;
        for (int i = 0; i < n3; ++i) {
            int n5;
            int n6;
            int n7 = chunkMeshData.runs[i * 3];
            int n8 = Owner.square(n7);
            int n9 = n8 % 8;
            int n10 = n8 / 8;
            int n11 = n6 = n7 == -1 ? -1 : sectorGraph.sector(l, n8);
            switch (n7 == -1 ? 0 : Owner.kind(n7)) {
                case 1: {
                    n11 = n10 > 0 ? sectorGraph.sector(l, n8 - 8) : sectorGraph.sector(Fragment.key(n, n2 - 1, n4), n8 + 64 - 8);
                    break;
                }
                case 2: {
                    n11 = n9 > 0 ? sectorGraph.sector(l, n8 - 1) : sectorGraph.sector(Fragment.key(n - 1, n2, n4), n8 + 8 - 1);
                    break;
                }
                case 3: {
                    n5 = sectorGraph.sector(Fragment.key(n, n2, n4 - 1), n8);
                    n11 = n5 >= 0 ? n5 : n6;
                    break;
                }
            }
            n5 = n6 >= 0 && n11 >= 0 ? 1 : 0;
            resolved.a[i] = n5 != 0 ? n6 : -1;
            resolved.b[i] = n5 != 0 ? n11 : -1;
            resolved.canHide = resolved.canHide | (n5 != 0 && sectorGraph.hideable[n6] && sectorGraph.hideable[n11]);
        }
        resolved.sectors = resolved.canHide ? DrawPlans.hideBy(resolved, sectorGraph) : null;
        resolved.planned = false;
    }

    private static int[] squares(int[] nArray, long l, SectorGraph sectorGraph) {
        int[] nArray2 = nArray != null ? nArray : new int[64];
        for (int i = 0; i < 64; ++i) {
            int n = sectorGraph.sector(l, i);
            nArray2[i] = n >= 0 && sectorGraph.hideable[n] ? n : -1;
        }
        return nArray2;
    }

    private static int[] hideBy(Resolved resolved, SectorGraph sectorGraph) {
        int n;
        if (marked.length < sectorGraph.sectors) {
            marked = new boolean[sectorGraph.sectors];
        }
        int n2 = 0;
        for (n = 0; n < resolved.a.length && n2 < found.length; ++n) {
            int n3 = resolved.a[n];
            int n4 = resolved.b[n];
            if (n3 < 0 || !sectorGraph.hideable[n3] || !sectorGraph.hideable[n4]) continue;
            n2 = DrawPlans.mark(n4, DrawPlans.mark(n3, n2));
        }
        for (n = 0; n < n2; ++n) {
            DrawPlans.marked[DrawPlans.found[n]] = false;
        }
        return n2 <= 64 ? Arrays.copyOf(found, n2) : null;
    }

    private static int mark(int n, int n2) {
        if (n2 == found.length || marked[n]) {
            return n2;
        }
        DrawPlans.marked[n] = true;
        DrawPlans.found[n2] = n;
        return n2 + 1;
    }

    private static void sweep(long l) {
        Iterator<Resolved> iterator = resolved.values().iterator();
        while (iterator.hasNext()) {
            Resolved resolved = iterator.next();
            if (resolved.used >= l) continue;
            bytes -= DrawPlans.size(resolved);
            iterator.remove();
        }
    }

    static void sweep() {
        DrawPlans.sweep(Caches.tick() - 240L);
    }

    static void clear() {
        resolved.clear();
        bytes = 0L;
    }

    private static long size(Resolved resolved) {
        long l = resolved.sectors == null ? 0L : 4L * (long)resolved.sectors.length;
        long l2 = resolved.squares == null ? 0L : 4L * (long)resolved.squares.length;
        return (resolved.a == null ? 0L : 8L * (long)resolved.a.length) + l + l2 + 4L * (long)resolved.entries.length + 112L;
    }

    private DrawPlans() {
    }

    static {
        Caches.register(new Caches.Cache(){

            @Override
            public long bytes() {
                return bytes;
            }

            @Override
            public void list(Caches.Survey survey) {
                for (Resolved resolved : DrawPlans.resolved.values()) {
                    survey.add(resolved.used, DrawPlans.size(resolved));
                }
            }

            @Override
            public void evictBefore(long l) {
                DrawPlans.sweep(l);
            }
        });
    }

    private static final class Resolved {
        SectorGraph graph;
        int[] a;
        int[] b;
        int[] sectors;
        boolean canHide;
        long used;
        boolean planned;
        long planHidden;
        long left;
        int[] entries = new int[0];
        int entryCount;
        int[] squares;

        private Resolved() {
        }
    }
}

