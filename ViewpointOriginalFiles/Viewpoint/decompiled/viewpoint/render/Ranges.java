/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import java.util.Map;
import java.util.TreeMap;

final class Ranges {
    private final TreeMap<Integer, Integer> free = new TreeMap();

    Ranges() {
    }

    void add(int n, int n2) {
        Integer n3;
        Map.Entry<Integer, Integer> entry = this.free.floorEntry(n);
        if (entry != null && entry.getKey() + entry.getValue() == n) {
            n = entry.getKey();
            n2 += entry.getValue().intValue();
        }
        if ((n3 = this.free.remove(n + n2)) != null) {
            n2 += n3.intValue();
        }
        this.free.put(n, n2);
    }

    int take(int n) {
        for (Map.Entry<Integer, Integer> entry : this.free.entrySet()) {
            if (entry.getValue() < n) continue;
            int n2 = entry.getKey();
            int n3 = entry.getValue() - n;
            this.free.remove(n2);
            if (n3 > 0) {
                this.free.put(n2 + n, n3);
            }
            return n2;
        }
        return -1;
    }
}

