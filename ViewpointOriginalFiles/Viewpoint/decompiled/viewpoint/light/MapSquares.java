/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.util.Arrays;

public final class MapSquares {
    static final int SIZE = 64;
    static final int LAMP_INTS = 4;
    final int bx;
    final int by;
    int levels;
    int[] bits;
    int[] lamps = new int[32];
    int lampCount;

    public MapSquares(int n, int n2, int n3) {
        this.bx = n;
        this.by = n2;
        this.levels = Math.max(1, n3);
        this.bits = new int[this.levels * 64 * 64];
    }

    public void tile(int n, int n2, int n3, long l) {
        int n4 = n - this.bx * 64;
        int n5 = n2 - this.by * 64;
        if (n4 < 0 || n5 < 0 || n4 >= 64 || n5 >= 64 || n3 < 0 || n3 >= this.levels) {
            return;
        }
        int n6 = (n3 * 64 + n5) * 64 + n4;
        this.bits[n6] = this.bits[n6] | ((int)l | 1);
        int n7 = (int)(l >>> 32);
        if (n7 != 0) {
            if ((this.lampCount + 1) * 4 > this.lamps.length) {
                this.lamps = Arrays.copyOf(this.lamps, this.lamps.length * 2);
            }
            int n8 = this.lampCount++ * 4;
            this.lamps[n8] = n;
            this.lamps[n8 + 1] = n2;
            this.lamps[n8 + 2] = n3;
            this.lamps[n8 + 3] = n7;
        }
    }

    public int levels() {
        return this.levels;
    }

    public void finish() {
        int n;
        int n2 = 0;
        for (n = this.bits.length - 1; n >= 0 && n2 == 0; --n) {
            n2 = (this.bits[n] & 1) != 0 ? n / 4096 : 0;
        }
        if (n2 + 1 < this.levels) {
            this.levels = n2 + 1;
            this.bits = Arrays.copyOf(this.bits, this.levels * 64 * 64);
        }
        for (n = 0; n < 64; n += 8) {
            for (int i = 0; i < 64; i += 8) {
                this.chunk(i, n);
            }
        }
    }

    public long bytes() {
        return (long)this.bits.length * 4L + (long)this.lamps.length * 4L + 64L;
    }

    int at(int n, int n2, int n3) {
        int n4 = n - this.bx * 64;
        int n5 = n2 - this.by * 64;
        if (n4 < 0 || n5 < 0 || n4 >= 64 || n5 >= 64 || n3 < 0 || n3 >= this.levels) {
            return -1;
        }
        int n6 = this.bits[(n3 * 64 + n5) * 64 + n4];
        return (n6 & 0x20000000) != 0 ? n6 : -1;
    }

    private void chunk(int n, int n2) {
        for (int i = 0; i < this.levels; ++i) {
            for (int j = n2; j < n2 + 8; ++j) {
                for (int k = n; k < n + 8; ++k) {
                    int n3 = this.bits[(i * 64 + j) * 64 + k];
                    if (i == 0) {
                        this.make(k, j, 0, false);
                    }
                    if ((n3 & 1) == 0) continue;
                    int n4 = (i * 64 + j) * 64 + k;
                    this.bits[n4] = this.bits[n4] | 0x40000000;
                    this.around(n, n2, k, j, i, false);
                    for (int i2 = i - 1; i2 >= 1; --i2) {
                        this.around(n, n2, k, j, i2, true);
                    }
                }
            }
        }
    }

    private void around(int n, int n2, int n3, int n4, int n5, boolean bl) {
        for (int i = Math.max(n2, n4 - 1); i <= Math.min(n2 + 8 - 1, n4 + 1); ++i) {
            for (int j = Math.max(n, n3 - 1); j <= Math.min(n + 8 - 1, n3 + 1); ++j) {
                this.make(j, i, n5, bl);
            }
        }
    }

    private void make(int n, int n2, int n3, boolean bl) {
        int n4 = (n3 * 64 + n2) * 64 + n;
        if ((this.bits[n4] & 0x20000000) == 0) {
            int n5 = n4;
            this.bits[n5] = this.bits[n5] | (0x20000000 | (bl ? 0x40000000 : 0));
        }
    }
}

