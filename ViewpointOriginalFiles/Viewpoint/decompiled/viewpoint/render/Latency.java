/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import java.util.ArrayDeque;
import viewpoint.platform.Profile;
import viewpoint.render.Retirement;

public final class Latency {
    static final int MAX_FENCES = 8;
    private static final Latency ON_GPU = new Latency(new Retirement.GlFences());
    private static volatile long newestSnapshot;
    private final Retirement.Fences fences;
    private final ArrayDeque<Long> presented = new ArrayDeque();
    private boolean viewBuilt;
    private int frames;
    private int lookFrames;
    private int maxBehind;
    private double lookMs;
    private double snapshotMs;
    private double newerSum;
    private double behindSum;

    public static void snapshotTaken(long l) {
        newestSnapshot = l;
    }

    Latency(Retirement.Fences fences) {
        this.fences = fences;
    }

    public static void viewBuilt(long l, long l2, long l3, long l4) {
        ON_GPU.measure(l, l2, l3, l4, newestSnapshot);
    }

    public static void presenting() {
        ON_GPU.fencePresented();
    }

    void measure(long l, long l2, long l3, long l4, long l5) {
        this.viewBuilt = true;
        int n = this.behind();
        ++this.frames;
        this.snapshotMs += (double)(l - l4) * 1.0E-6;
        this.newerSum += (double)Math.max(0L, l5 - l3);
        this.behindSum += (double)n;
        this.maxBehind = Math.max(this.maxBehind, n);
        if (l2 != 0L) {
            ++this.lookFrames;
            this.lookMs += (double)(l - l2) * 1.0E-6;
        }
    }

    void fencePresented() {
        long l;
        if (!this.viewBuilt) {
            return;
        }
        this.viewBuilt = false;
        if (this.presented.size() >= 8) {
            this.fences.delete(this.presented.pollFirst());
        }
        if ((l = this.fences.place()) != 0L) {
            this.presented.addLast(l);
        }
    }

    int behind() {
        while (!this.presented.isEmpty() && this.fences.passed(this.presented.peekFirst())) {
            this.fences.delete(this.presented.pollFirst());
        }
        return this.presented.size();
    }

    String report() {
        if (this.frames == 0) {
            return "no frames";
        }
        String string = String.format("look %s at the view, snapshot %.1f ms old (%.2f newer taken), GPU %.2f frames behind (at most %d)", this.lookFrames == 0 ? "not looking" : String.format("%.1f ms old", this.lookMs / (double)this.lookFrames), this.snapshotMs / (double)this.frames, this.newerSum / (double)this.frames, this.behindSum / (double)this.frames, this.maxBehind);
        this.maxBehind = 0;
        this.lookFrames = 0;
        this.frames = 0;
        this.behindSum = 0.0;
        this.newerSum = 0.0;
        this.snapshotMs = 0.0;
        this.lookMs = 0.0;
        return string;
    }

    static {
        Profile.latencyReport = ON_GPU::report;
    }
}

