/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.util.Arrays;
import viewpoint.light.LightLayouts;

final class LightLayout {
    static final int[] DX = new int[]{0, 1, 1, 1, 0, -1, -1, -1, 0, 0};
    static final int[] DY = new int[]{-1, -1, 0, 1, 1, 1, 0, -1, 0, 0};
    static final int[] DZ = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 1, -1};
    static final int[] REVERSE = new int[]{4, 5, 6, 7, 0, 1, 2, 3, 9, 8};
    static final int DIRECTIONS = 10;
    static final int UP = 8;
    static final int DOWN = 9;
    static final int CLEAR = 0;
    static final int OPEN_DOOR = 1;
    static final int WINDOW = 2;
    static final int CLOSED_DOOR = 3;
    static final int BLOCKED = 4;
    static final int EXISTS = 1;
    static final int OPEN_AIR = 2;
    static final int CURTAIN_N = 4;
    static final int CURTAIN_E = 8;
    static final int CURTAIN_S = 16;
    static final int CURTAIN_W = 32;
    static final int OUTDOORS = 64;
    int x0;
    int y0;
    int z0;
    int width;
    int height;
    int levels;
    private final byte[] flags;
    private final int[] passes;

    LightLayout(int n, int n2, int n3, int n4, int n5, int n6) {
        this(n4 * n5 * n6);
        this.box(n, n2, n3, n4, n5, n6);
    }

    LightLayout(int n) {
        this.flags = new byte[n];
        this.passes = new int[n];
    }

    void box(int n, int n2, int n3, int n4, int n5, int n6) {
        if (n4 * n5 * n6 > this.flags.length) {
            throw new IllegalArgumentException("a light layout of " + n4 + " x " + n5 + " x " + n6 + " squares is past its capacity of " + this.flags.length);
        }
        this.x0 = n;
        this.y0 = n2;
        this.z0 = n3;
        this.width = n4;
        this.height = n5;
        this.levels = n6;
        Arrays.fill(this.flags, 0, this.size(), (byte)0);
    }

    int size() {
        return this.width * this.height * this.levels;
    }

    int capacity() {
        return this.flags.length;
    }

    int index(int n, int n2, int n3) {
        int n4 = n - this.x0;
        int n5 = n2 - this.y0;
        int n6 = n3 - this.z0;
        if (n4 < 0 || n5 < 0 || n6 < 0 || n4 >= this.width || n5 >= this.height || n6 >= this.levels) {
            return -1;
        }
        return (n6 * this.height + n5) * this.width + n4;
    }

    int neighbour(int n, int n2) {
        int n3 = n % this.width;
        int n4 = n / this.width % this.height;
        int n5 = n / (this.width * this.height);
        return this.index(this.x0 + n3 + DX[n2], this.y0 + n4 + DY[n2], this.z0 + n5 + DZ[n2]);
    }

    void set(int n, int n2, int n3, int n4, int n5) {
        int n6 = this.index(n, n2, n3);
        if (n6 >= 0) {
            this.flags[n6] = (byte)(n4 | 1);
            this.passes[n6] = n5;
        }
    }

    void chunk(LightLayouts.Level level) {
        int n = level.chunkX * 8;
        int n2 = level.chunkY * 8;
        for (int i = 0; i < 64; ++i) {
            if ((level.flags[i] & 1) == 0) continue;
            this.set(n + i % 8, n2 + i / 8, level.level, level.flags[i], level.passes[i]);
        }
    }

    boolean exists(int n) {
        return (this.flags[n] & 1) != 0;
    }

    boolean openAir(int n) {
        return (this.flags[n] & 2) != 0;
    }

    boolean has(int n, int n2) {
        return (this.flags[n] & n2) != 0;
    }

    int pass(int n, int n2) {
        return LightLayout.passIn(this.passes[n], n2);
    }

    static int passIn(int n, int n2) {
        return n >> n2 * 3 & 7;
    }
}

