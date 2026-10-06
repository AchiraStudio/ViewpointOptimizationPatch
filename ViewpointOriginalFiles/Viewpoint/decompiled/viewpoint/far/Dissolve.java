/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

final class Dissolve {
    static final long APPEAR = 600000000L;
    static final long CROSS = 400000000L;
    static final long LEAVE = 600000000L;
    private long start;
    private long nanos;

    Dissolve() {
    }

    void begin(long l, long l2, float f) {
        this.nanos = l2;
        this.start = l - (long)(f * (float)l2);
    }

    float progress(long l) {
        if (this.nanos <= 0L) {
            return 1.0f;
        }
        float f = (float)(l - this.start) / (float)this.nanos;
        if (f < 1.0f) {
            return Math.max(f, 0.0f);
        }
        this.nanos = 0L;
        return 1.0f;
    }

    static float top(float f) {
        return f < 1.0f ? f : 2.0f;
    }

    static byte share(float f) {
        return (byte)Math.min(255, Math.round(f * 255.0f));
    }
}

