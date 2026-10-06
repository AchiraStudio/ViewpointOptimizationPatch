/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import viewpoint.platform.FrameTimes;
import viewpoint.platform.GpuBusy;
import viewpoint.platform.PerformanceOverlay;

final class Utilization {
    private static final long SAMPLE_NANOS = 500000000L;
    private static final ThreadMXBean THREADS;
    private static final OperatingSystemMXBean OS;
    static volatile float game;
    static volatile float render;
    static volatile float process;
    static volatile float system;
    static volatile float gpu;
    private static long sampledAt;
    private static long gameCpu;
    private static long renderCpu;
    private static boolean samplerStarted;

    static void sample(long l) {
        if (l - sampledAt < 500000000L) {
            return;
        }
        Utilization.threads(l - sampledAt);
        sampledAt = l;
        gpu = GpuBusy.percent();
        if (!samplerStarted && OS != null) {
            samplerStarted = true;
            Thread thread = new Thread(Utilization::runSampler, "Viewpoint overlay CPU");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private static void threads(long l) {
        long l2 = 0L;
        long l3 = 0L;
        try {
            if (!THREADS.isThreadCpuTimeEnabled()) {
                THREADS.setThreadCpuTimeEnabled(true);
            }
            l2 = THREADS.getThreadCpuTime(Thread.currentThread().getId());
            long l4 = FrameTimes.renderThreadId;
            l3 = l4 >= 0L ? THREADS.getThreadCpuTime(l4) : 0L;
        }
        catch (SecurityException | UnsupportedOperationException runtimeException) {
            // empty catch block
        }
        if (sampledAt != 0L && l > 0L) {
            if (l2 > 0L && gameCpu > 0L) {
                game = Math.min(100.0f, 100.0f * (float)(l2 - gameCpu) / (float)l);
            }
            if (l3 > 0L && renderCpu > 0L) {
                render = Math.min(100.0f, 100.0f * (float)(l3 - renderCpu) / (float)l);
            }
        }
        gameCpu = l2;
        renderCpu = l3;
    }

    private static void runSampler() {
        try {
            while (true) {
                Thread.sleep(500L);
                if (PerformanceOverlay.mode() == PerformanceOverlay.Mode.OFF) continue;
                double d = OS.getProcessCpuLoad();
                double d2 = OS.getCpuLoad();
                if (d >= 0.0) {
                    process = (float)(d * 100.0);
                }
                if (!(d2 >= 0.0)) continue;
                system = (float)(d2 * 100.0);
            }
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    private Utilization() {
    }

    static {
        OperatingSystemMXBean operatingSystemMXBean;
        THREADS = ManagementFactory.getThreadMXBean();
        java.lang.management.OperatingSystemMXBean operatingSystemMXBean2 = ManagementFactory.getOperatingSystemMXBean();
        OS = operatingSystemMXBean2 instanceof OperatingSystemMXBean ? (operatingSystemMXBean = (OperatingSystemMXBean)operatingSystemMXBean2) : null;
    }
}

