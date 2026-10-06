/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.util.List;
import viewpoint.light.LightFlood;
import viewpoint.light.LightLayout;
import viewpoint.light.LightLayouts;

final class LightWork {
    private static final int START = 20800;
    LightLayout layout;
    LightFlood flood;
    byte[] steps;
    int[] queue;
    int[] values;
    int[] starts = new int[64];

    LightWork() {
    }

    LightLayout layout(int n, int n2, int n3, int n4, int n5, int n6, List<LightLayouts.Level> list) {
        int n7 = n4 - n + 1;
        int n8 = n5 - n2 + 1;
        int n9 = n6 - n3 + 1;
        if (this.layout == null || n7 * n8 * n9 > this.layout.capacity()) {
            int n10 = Math.max(20800, n7 * n8 * n9);
            this.layout = new LightLayout(n10);
            this.flood = new LightFlood(this.layout);
            this.steps = new byte[n10];
            this.queue = new int[n10];
            this.values = new int[n10];
        }
        this.layout.box(n, n2, n3, n7, n8, n9);
        for (LightLayouts.Level level : list) {
            this.layout.chunk(level);
        }
        return this.layout;
    }

    int[] starts(int n) {
        if (this.starts.length < n) {
            this.starts = new int[n];
        }
        return this.starts;
    }
}

