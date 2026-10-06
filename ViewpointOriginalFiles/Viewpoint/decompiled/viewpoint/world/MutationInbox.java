/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class MutationInbox {
    static final int GEOMETRY = 1;
    static final int ALL_LEVELS = Integer.MIN_VALUE;
    static final int CAPACITY = 4096;
    private volatile Thread owner;
    private final ConcurrentLinkedQueue<int[]> foreign = new ConcurrentLinkedQueue();
    private final AtomicInteger foreignCount = new AtomicInteger();
    private final AtomicBoolean foreignOverflow = new AtomicBoolean();
    private final HashMap<Long, Waiting> waiting = new HashMap();
    private Waiting last;
    private long stamp;
    private boolean overflow;
    private int signals;

    MutationInbox() {
    }

    void record(int n, int n2, int n3, int n4) {
        if (Thread.currentThread() == this.owner) {
            this.add(n, n2, n3, n4);
        } else if (this.foreignCount.incrementAndGet() <= 4096) {
            this.foreign.add(new int[]{n, n2, n3, n4});
        } else {
            this.foreignCount.decrementAndGet();
            this.foreignOverflow.set(true);
        }
    }

    long stamp() {
        return this.stamp;
    }

    boolean drain(ArrayList<Waiting> arrayList) {
        int[] nArray;
        this.owner = Thread.currentThread();
        while ((nArray = this.foreign.poll()) != null) {
            this.foreignCount.decrementAndGet();
            this.add(nArray[0], nArray[1], nArray[2], nArray[3]);
        }
        boolean bl = !this.foreignOverflow.getAndSet(false) && !this.overflow;
        arrayList.clear();
        if (bl) {
            arrayList.addAll(this.waiting.values());
        }
        this.waiting.clear();
        this.last = null;
        this.overflow = false;
        return bl;
    }

    void clear() {
        this.foreign.clear();
        this.foreignCount.set(0);
        this.foreignOverflow.set(false);
        this.waiting.clear();
        this.last = null;
        this.overflow = false;
        this.signals = 0;
    }

    int takeSignalCount() {
        int n = this.signals;
        this.signals = 0;
        return n;
    }

    private void add(int n, int n2, int n3, int n4) {
        Waiting waiting;
        ++this.stamp;
        ++this.signals;
        if (this.overflow) {
            return;
        }
        Waiting waiting2 = waiting = this.last != null && this.last.chunkX == n && this.last.chunkY == n2 && this.last.level == n3 ? this.last : this.waiting.get(MutationInbox.key(n, n2, n3));
        if (waiting == null && this.waiting.size() == 4096) {
            this.overflow = true;
            this.waiting.clear();
            this.last = null;
            return;
        }
        if (waiting == null) {
            waiting = new Waiting(n, n2, n3);
            this.waiting.put(MutationInbox.key(n, n2, n3), waiting);
        }
        waiting.kinds |= n4;
        if ((n4 & 1) != 0) {
            waiting.geometryStamp = this.stamp;
        }
        this.last = waiting;
    }

    private static long key(int n, int n2, int n3) {
        return ((long)n & 0xFFFFFFL) << 36 | ((long)n2 & 0xFFFFFFL) << 12 | (n3 == Integer.MIN_VALUE ? 4095L : (long)n3 & 0x7FFL);
    }

    static final class Waiting {
        final int chunkX;
        final int chunkY;
        final int level;
        int kinds;
        long geometryStamp;

        Waiting(int n, int n2, int n3) {
            this.chunkX = n;
            this.chunkY = n2;
            this.level = n3;
        }
    }
}

