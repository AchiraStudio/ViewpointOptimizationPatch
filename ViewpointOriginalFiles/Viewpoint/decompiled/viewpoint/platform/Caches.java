/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.ArrayList;
import java.util.Arrays;
import viewpoint.platform.Pool;

public final class Caches {
    static final int MIN_AGE = 60;
    private static final int INDEX_BITS = 24;
    private static final ArrayList<Cache> caches = new ArrayList();
    private static final Survey survey = new Survey();
    private static long tick;

    public static <T extends Cache> T register(T t) {
        caches.add(t);
        return t;
    }

    public static long tick() {
        return tick;
    }

    public static void balance() {
        ++tick;
        Pool.CPU.use(Caches.total());
        if (!Pool.CPU.over(0.9)) {
            return;
        }
        Caches.survey.count = 0;
        for (Cache cache : caches) {
            cache.list(survey);
        }
        long l = Caches.cutoff(survey, Pool.CPU.excess(0.8), tick - 60L);
        for (Cache cache : caches) {
            cache.evictBefore(l);
        }
        Pool.CPU.use(Caches.total());
    }

    static long cutoff(Survey survey, long l, long l2) {
        long l3;
        Arrays.sort(survey.keys, 0, survey.count);
        long l4 = 0L;
        long l5 = Long.MIN_VALUE;
        for (int i = 0; i < survey.count && l4 < l && (l3 = survey.keys[i] >> 24) < l2; l4 += survey.sizes[(int)(survey.keys[i] & 0xFFFFFFL)], ++i) {
            l5 = l3 + 1L;
        }
        return l5;
    }

    private static long total() {
        long l = 0L;
        for (Cache cache : caches) {
            l += cache.bytes();
        }
        return l;
    }

    private Caches() {
    }

    public static final class Survey {
        private long[] keys = new long[1024];
        private long[] sizes = new long[1024];
        private int count;

        public void add(long l, long l2) {
            if (this.count == this.keys.length) {
                this.keys = Arrays.copyOf(this.keys, this.count * 2);
                this.sizes = Arrays.copyOf(this.sizes, this.count * 2);
            }
            this.keys[this.count] = l << 24 | (long)this.count;
            this.sizes[this.count] = l2;
            ++this.count;
        }
    }

    public static interface Cache {
        public long bytes();

        public void list(Survey var1);

        public void evictBefore(long var1);
    }
}

