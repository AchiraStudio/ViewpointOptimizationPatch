/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.models;

import java.util.Arrays;

final class CorpsePicks {
    static final float MARGIN = 0.15f;
    static final float FRONT = 2.0f;
    static final int SWAPS_PER_FRAME = 4;
    private static long[] order = new long[64];
    private static float[] scores = new float[64];

    static void pick(Bodies bodies, Limits limits) {
        int n;
        int n2;
        int n3 = CorpsePicks.rank(bodies, limits);
        int n4 = 0;
        for (n2 = 0; n2 < bodies.count; ++n2) {
            bodies.picked[n2] = bodies.picked[n2] && CorpsePicks.allowed(bodies, n2, limits);
            n4 += bodies.picked[n2] ? 1 : 0;
        }
        for (n2 = n3 - 1; n2 >= 0 && n4 > limits.most(); --n2) {
            n = (int)order[n2];
            if (!bodies.picked[n]) continue;
            bodies.picked[n] = false;
            --n4;
        }
        for (n2 = 0; n2 < n3 && n4 < limits.most(); ++n2) {
            n = (int)order[n2];
            if (bodies.picked[n]) continue;
            bodies.picked[n] = true;
            ++n4;
        }
        CorpsePicks.swap(bodies, n3, limits);
        CorpsePicks.progress(bodies, limits);
    }

    private static int rank(Bodies bodies, Limits limits) {
        if (order.length < bodies.count) {
            order = new long[bodies.count * 2];
            scores = new float[bodies.count * 2];
        }
        int n = 0;
        for (int i = 0; i < bodies.count; ++i) {
            if (!CorpsePicks.allowed(bodies, i, limits)) continue;
            CorpsePicks.scores[i] = CorpsePicks.score(bodies, i, limits);
            CorpsePicks.order[n++] = (long)Float.floatToIntBits(scores[i]) << 32 | (long)i;
        }
        Arrays.sort(order, 0, n);
        return n;
    }

    private static boolean allowed(Bodies bodies, int n, Limits limits) {
        return bodies.eligible[n] && bodies.distance[n] < limits.limit();
    }

    private static float score(Bodies bodies, int n, Limits limits) {
        return (bodies.distance[n] + 2.0f) * (float)Math.pow(bodies.random[n], limits.randomness());
    }

    private static void swap(Bodies bodies, int n, Limits limits) {
        int n2 = 0;
        for (int i = 0; i < 4; ++i) {
            while (n2 < n && bodies.picked[(int)order[n2]]) {
                ++n2;
            }
            int n3 = CorpsePicks.worstPicked(bodies);
            if (n2 == n || n3 < 0) {
                return;
            }
            int n4 = (int)order[n2];
            if (scores[n4] * 1.15f >= scores[n3]) {
                return;
            }
            bodies.picked[n3] = false;
            bodies.picked[n4] = true;
        }
    }

    private static int worstPicked(Bodies bodies) {
        int n = -1;
        for (int i = 0; i < bodies.count; ++i) {
            if (!bodies.picked[i] || n >= 0 && !(scores[i] > scores[n])) continue;
            n = i;
        }
        return n;
    }

    private static void progress(Bodies bodies, Limits limits) {
        for (int i = 0; i < bodies.count; ++i) {
            float f = bodies.progress[i];
            f = !bodies.ready[i] ? 0.0f : (bodies.picked[i] ? (bodies.fresh[i] && f == 0.0f ? 1.0f : Math.min(1.0f, f + limits.step())) : Math.max(0.0f, f - limits.step()));
            bodies.progress[i] = f;
            bodies.share[i] = bodies.eligible[i] ? Math.min(CorpsePicks.distanceShare(bodies.distance[i], limits), f) : 0.0f;
        }
    }

    static float distanceShare(float f, Limits limits) {
        if (limits.band() <= 0.0f) {
            return f < limits.limit() ? 1.0f : 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, (limits.limit() - f) / limits.band()));
    }

    private CorpsePicks() {
    }

    static final class Bodies {
        int count;
        float[] distance = new float[0];
        float[] random = new float[0];
        float[] progress = new float[0];
        float[] share = new float[0];
        boolean[] ready = new boolean[0];
        boolean[] fresh = new boolean[0];
        boolean[] eligible = new boolean[0];
        boolean[] picked = new boolean[0];

        Bodies() {
        }

        void size(int n) {
            if (this.distance.length < n) {
                int n2 = Math.max(n, this.distance.length * 2);
                this.distance = Arrays.copyOf(this.distance, n2);
                this.random = Arrays.copyOf(this.random, n2);
                this.progress = Arrays.copyOf(this.progress, n2);
                this.share = Arrays.copyOf(this.share, n2);
                this.ready = Arrays.copyOf(this.ready, n2);
                this.fresh = Arrays.copyOf(this.fresh, n2);
                this.eligible = Arrays.copyOf(this.eligible, n2);
                this.picked = Arrays.copyOf(this.picked, n2);
            }
            this.count = n;
        }
    }

    record Limits(float limit, float band, int most, float randomness, float step) {
    }
}

