/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.util.Arrays;
import viewpoint.light.LightLayout;

final class LightFlood {
    static final double STRAIGHT = 1.0;
    static final double DIAGONAL = Math.sqrt(2.0);
    static final double LEVEL = 3.0;
    static final double CLOSED_DOOR = 0.15;
    static final double CURTAIN = 0.33;
    private final LightLayout layout;
    private final double[] value;
    private final double[] cost;
    private final double[] transmission;
    private final int[] touched;
    private int touchedCount;
    private int[] heap = new int[256];
    private int[] entrySquare = new int[256];
    private double[] entryValue = new double[256];
    private int heapSize;
    private int entries;

    LightFlood(LightLayout lightLayout) {
        this.layout = lightLayout;
        this.value = new double[lightLayout.capacity()];
        this.cost = new double[lightLayout.capacity()];
        this.transmission = new double[lightLayout.capacity()];
        this.touched = new int[lightLayout.capacity()];
        Arrays.fill(this.value, -1.0);
    }

    void run(int[] nArray, int n, double d) {
        int n2;
        int n3;
        for (n3 = 0; n3 < this.touchedCount; ++n3) {
            this.value[this.touched[n3]] = -1.0;
        }
        this.entries = 0;
        this.heapSize = 0;
        this.touchedCount = 0;
        for (n3 = 0; n3 < n; ++n3) {
            n2 = nArray[n3];
            if (n2 < 0 || !this.layout.exists(n2) || !(this.value[n2] < 1.0)) continue;
            this.reach(n2, 0.0, 1.0, 1.0);
        }
        while (this.heapSize > 0) {
            n3 = this.pop();
            if (this.entryValue[n3] < this.value[n2 = this.entrySquare[n3]]) continue;
            for (int i = 0; i < 10; ++i) {
                this.step(n2, i, d);
            }
        }
    }

    int touchedCount() {
        return this.touchedCount;
    }

    int touched(int n) {
        return this.touched[n];
    }

    double cost(int n) {
        return this.cost[n];
    }

    double transmission(int n) {
        return this.transmission[n];
    }

    private void step(int n, int n2, double d) {
        int n3 = this.layout.neighbour(n, n2);
        if (n3 < 0 || !this.layout.exists(n3)) {
            return;
        }
        int n4 = this.layout.pass(n, n2);
        int n5 = this.layout.pass(n3, LightLayout.REVERSE[n2]);
        if (n4 == 4 && n5 == 4) {
            return;
        }
        double d2 = this.cost[n] + (n2 >= 8 ? 3.0 : ((n2 & 1) != 0 ? DIAGONAL : 1.0));
        if (d2 >= d) {
            return;
        }
        int n6 = n4 != 4 ? n4 : n5;
        double d3 = this.transmission[n] * (n6 == 3 ? 0.15 : 1.0) * this.curtain(n, n3, n2);
        double d4 = d3 * (1.0 - d2 / d);
        if (d4 > this.value[n3]) {
            this.reach(n3, d2, d3, d4);
        }
    }

    private double curtain(int n, int n2, int n3) {
        boolean bl = switch (n3) {
            case 0 -> {
                if (this.layout.has(n, 4) || this.layout.has(n2, 16)) {
                    yield true;
                }
                yield false;
            }
            case 2 -> {
                if (this.layout.has(n, 8) || this.layout.has(n2, 32)) {
                    yield true;
                }
                yield false;
            }
            case 4 -> {
                if (this.layout.has(n, 16) || this.layout.has(n2, 4)) {
                    yield true;
                }
                yield false;
            }
            case 6 -> {
                if (this.layout.has(n, 32) || this.layout.has(n2, 8)) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
        return bl ? 0.33 : 1.0;
    }

    private void reach(int n, double d, double d2, double d3) {
        if (this.value[n] < 0.0) {
            this.touched[this.touchedCount++] = n;
        }
        this.value[n] = d3;
        this.cost[n] = d;
        this.transmission[n] = d2;
        this.push(n, d3);
    }

    private void push(int n, double d) {
        if (this.entries == this.entrySquare.length) {
            this.entrySquare = Arrays.copyOf(this.entrySquare, this.entries * 2);
            this.entryValue = Arrays.copyOf(this.entryValue, this.entries * 2);
        }
        if (this.heapSize == this.heap.length) {
            this.heap = Arrays.copyOf(this.heap, this.heapSize * 2);
        }
        int n2 = this.entries++;
        this.entrySquare[n2] = n;
        this.entryValue[n2] = d;
        int n3 = this.heapSize++;
        while (n3 > 0 && this.entryValue[this.heap[(n3 - 1) / 2]] < d) {
            this.heap[n3] = this.heap[(n3 - 1) / 2];
            n3 = (n3 - 1) / 2;
        }
        this.heap[n3] = n2;
    }

    private int pop() {
        int n;
        int n2 = this.heap[0];
        int n3 = this.heap[--this.heapSize];
        int n4 = 0;
        while ((n = n4 * 2 + 1) < this.heapSize) {
            if (n + 1 < this.heapSize && this.entryValue[this.heap[n + 1]] > this.entryValue[this.heap[n]]) {
                ++n;
            }
            if (this.entryValue[this.heap[n]] <= this.entryValue[n3]) break;
            this.heap[n4] = this.heap[n];
            n4 = n;
        }
        this.heap[n4] = n3;
        return n2;
    }
}

