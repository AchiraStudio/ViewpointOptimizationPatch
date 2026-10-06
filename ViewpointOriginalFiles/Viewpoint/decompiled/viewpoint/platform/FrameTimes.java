/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL33
 *  org.lwjgl.opengl.GLCapabilities
 */
package viewpoint.platform;

import java.util.Arrays;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLCapabilities;

public final class FrameTimes {
    static final int RING = 8192;
    static final int READABLE = 8128;
    private static final int QUERIES = 8;
    static final float[] frameMs = new float[8192];
    static final float[] gpuMs = new float[8192];
    static final long[] frameEnd = new long[8192];
    static volatile int head;
    static volatile long renderThreadId;
    static volatile int gpuState;
    private static long lastSwap;
    private static final int[] queryIds;
    private static final long[] queryFrame;
    private static int queryNext;
    private static int activeQuery;
    private static float pendingGpuMs;

    public static void renderStarted() {
        if (gpuState < 0) {
            return;
        }
        try {
            if (!FrameTimes.timerReady()) {
                return;
            }
            FrameTimes.collect();
            int n = queryNext;
            if (queryFrame[n] != -1L) {
                return;
            }
            GL15.glBeginQuery((int)35007, (int)queryIds[n]);
            activeQuery = n;
        }
        catch (RuntimeException runtimeException) {
            gpuState = -1;
        }
    }

    private static boolean timerReady() {
        if (gpuState != 0) {
            return true;
        }
        GLCapabilities gLCapabilities = GL.getCapabilities();
        if (!gLCapabilities.OpenGL33 && !gLCapabilities.GL_ARB_timer_query) {
            gpuState = -1;
            return false;
        }
        GL15.glGenQueries((int[])queryIds);
        Arrays.fill(queryFrame, -1L);
        gpuState = 1;
        return true;
    }

    public static void renderEnded() {
        if (activeQuery < 0) {
            return;
        }
        try {
            GL15.glEndQuery((int)35007);
            FrameTimes.queryFrame[FrameTimes.activeQuery] = head;
            queryNext = (activeQuery + 1) % 8;
        }
        catch (RuntimeException runtimeException) {
            gpuState = -1;
        }
        activeQuery = -1;
    }

    private static void collect() {
        for (int i = 0; i < 8; ++i) {
            long l = queryFrame[i];
            if (l == -1L || GL15.glGetQueryObjecti((int)queryIds[i], (int)34919) == 0) continue;
            float f = (float)GL33.glGetQueryObjecti64((int)queryIds[i], (int)34918) / 1000000.0f;
            FrameTimes.queryFrame[i] = -1L;
            if (l < (long)head) {
                FrameTimes.gpuMs[(int)(l & 0x1FFFL)] = f;
                continue;
            }
            pendingGpuMs = f;
        }
    }

    public static void swapped() {
        long l = System.nanoTime();
        if (renderThreadId < 0L) {
            renderThreadId = Thread.currentThread().getId();
        }
        if (lastSwap != 0L) {
            int n = head;
            int n2 = n & 0x1FFF;
            FrameTimes.frameMs[n2] = (float)(l - lastSwap) / 1000000.0f;
            FrameTimes.frameEnd[n2] = l;
            FrameTimes.gpuMs[n2] = pendingGpuMs;
            pendingGpuMs = 0.0f;
            head = n + 1;
        }
        lastSwap = l;
    }

    static Window window(long l, long l2) {
        int n;
        long l3;
        int n2 = head;
        int n3 = Math.min(n2, 8128);
        int n4 = 0;
        int n5 = 0;
        float[] fArray = new float[n3];
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = -1.0f;
        for (int i = 1; i <= n3 && (l3 = l - frameEnd[n = n2 - i & 0x1FFF]) <= l2; ++i) {
            float f4 = frameMs[n];
            fArray[n4++] = f4;
            f += f4;
            n5 += l3 <= 1000000000L ? 1 : 0;
            f2 += f3 >= 0.0f ? Math.abs(f4 - f3) : 0.0f;
            f3 = f4;
        }
        return new Window(fArray, n4, n5, f, f2);
    }

    static Percentiles percentiles(Window window) {
        float[] fArray = Arrays.copyOf(window.ms(), window.count());
        Arrays.sort(fArray);
        float f = FrameTimes.percentile(fArray, 50.0f);
        int n = 0;
        for (int i = 0; i < window.count(); ++i) {
            if (!(window.ms()[i] > 2.0f * f)) continue;
            ++n;
        }
        return new Percentiles(window.sumMs() / (float)window.count(), f, FrameTimes.percentile(fArray, 99.0f), FrameTimes.percentile(fArray, 99.9f), fArray[fArray.length - 1], n);
    }

    static float percentile(float[] fArray, float f) {
        if (fArray.length == 0) {
            return 0.0f;
        }
        int n = (int)Math.ceil(f / 100.0f * (float)fArray.length) - 1;
        return fArray[Math.max(0, Math.min(fArray.length - 1, n))];
    }

    private FrameTimes() {
    }

    static {
        renderThreadId = -1L;
        queryIds = new int[8];
        queryFrame = new long[8];
        activeQuery = -1;
    }

    record Window(float[] ms, int count, int lastSecond, float sumMs, float jitter) {
    }

    record Percentiles(float mean, float p50, float p99, float p999, float max, int spikes) {
    }
}

