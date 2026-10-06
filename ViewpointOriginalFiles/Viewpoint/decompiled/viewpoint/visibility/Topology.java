/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoObject
 */
package viewpoint.visibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import viewpoint.visibility.Capture;
import viewpoint.visibility.Fragment;
import viewpoint.visibility.GraphBuilder;
import viewpoint.visibility.SectorGraph;
import zombie.iso.IsoChunk;
import zombie.iso.IsoObject;

final class Topology {
    private static final long MIN_INTERVAL = 200000000L;
    private final HashMap<Long, Leaf> leaves = new HashMap();
    private long lastStart;
    private final IsoObject[] captured = new IsoObject[128];
    private final long[] capturedGlazed = new long[2];
    private int generation;
    private boolean changed;
    private boolean edited;
    private SectorGraph published;
    private Future<SectorGraph> job;
    volatile long compileNanos;
    private static volatile boolean compileFailed;
    private static final ExecutorService WORKER;

    Topology() {
    }

    void capture(IsoChunk isoChunk, int n) {
        long l = Fragment.key(isoChunk.wx, isoChunk.wy, n);
        Leaf leaf = this.leaves.get(l);
        if (leaf == null) {
            leaf = new Leaf();
            this.leaves.put(l, leaf);
            ++this.generation;
        }
        Fragment fragment = Capture.capture(isoChunk, n, this.captured, this.capturedGlazed);
        System.arraycopy(this.captured, 0, leaf.doors, 0, this.captured.length);
        System.arraycopy(this.capturedGlazed, 0, leaf.glazed, 0, this.capturedGlazed.length);
        if (fragment.sameLayout(leaf.fragment)) {
            return;
        }
        if (this.published != null && leaf.fragment != null && this.published.sources.get(l) == leaf.fragment) {
            this.edited = true;
        }
        leaf.fragment = fragment;
        this.changed = true;
    }

    void forget(int n, int n2) {
        Iterator<Leaf> iterator = this.leaves.values().iterator();
        while (iterator.hasNext()) {
            Fragment fragment = iterator.next().fragment;
            if (fragment == null || fragment.chunkX != n || fragment.chunkY != n2) continue;
            iterator.remove();
            this.changed = true;
            ++this.generation;
        }
    }

    void clear() {
        this.leaves.clear();
        ++this.generation;
        this.published = null;
        this.edited = false;
        this.changed = false;
        if (this.job != null) {
            this.job.cancel(false);
            this.job = null;
        }
    }

    void update() {
        Object object;
        if (this.job != null && this.job.isDone()) {
            object = this.finished();
            if (object != null && this.current((SectorGraph)object)) {
                this.published = object;
                this.edited = false;
            } else {
                this.changed = true;
            }
            this.job = null;
        }
        if (this.changed && this.job == null && System.nanoTime() - this.lastStart >= 200000000L) {
            this.changed = false;
            this.lastStart = System.nanoTime();
            object = new ArrayList(this.leaves.size());
            for (Leaf leaf : this.leaves.values()) {
                if (leaf.fragment == null) continue;
                ((ArrayList)object).add(leaf.fragment);
            }
            this.job = WORKER.submit(() -> this.lambda$update$0((ArrayList)object));
        }
    }

    private SectorGraph finished() {
        try {
            return this.job.get();
        }
        catch (Exception exception) {
            if (!compileFailed) {
                compileFailed = true;
                System.out.println("[Viewpoint] rooms: the sector graph failed to compile (nothing is hidden):");
                exception.printStackTrace(System.out);
            }
            return null;
        }
    }

    private boolean current(SectorGraph sectorGraph) {
        for (Fragment fragment : sectorGraph.sources.values()) {
            Leaf leaf = this.leaves.get(fragment.key());
            if (leaf == null || leaf.fragment == fragment) continue;
            return false;
        }
        return true;
    }

    SectorGraph graph() {
        return this.edited ? null : this.published;
    }

    Leaf leaf(long l) {
        return this.leaves.get(l);
    }

    int generation() {
        return this.generation;
    }

    int levels() {
        return this.leaves.size();
    }

    private /* synthetic */ SectorGraph lambda$update$0(ArrayList arrayList) throws Exception {
        long l = System.nanoTime();
        SectorGraph sectorGraph = GraphBuilder.compile(arrayList);
        this.compileNanos = System.nanoTime() - l;
        return sectorGraph;
    }

    static {
        WORKER = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Viewpoint rooms");
            thread.setDaemon(true);
            thread.setPriority(4);
            return thread;
        });
    }

    static final class Leaf {
        Fragment fragment;
        final IsoObject[] doors = new IsoObject[128];
        final long[] glazed = new long[2];

        Leaf() {
        }

        boolean glazed(int n) {
            return (this.glazed[n & 1] >>> (n >> 1) & 1L) != 0L;
        }
    }
}

