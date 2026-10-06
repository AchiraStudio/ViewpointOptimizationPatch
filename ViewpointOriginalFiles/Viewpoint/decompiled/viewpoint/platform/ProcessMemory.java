/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

final class ProcessMemory {
    private static final long MIB = 0x100000L;
    private static final long SAMPLE_NANOS = 500000000L;
    private static final long JUMP_BYTES = 0x10000000L;
    private static final OperatingSystemMXBean OS = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
    private static final List<GarbageCollectorMXBean> COLLECTORS = ManagementFactory.getGarbageCollectorMXBeans();
    private static final BufferPoolMXBean DIRECT = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).stream().filter(bufferPoolMXBean -> bufferPoolMXBean.getName().equals("direct")).findFirst().orElse(null);
    private static long lastSample;
    private static long committed;
    private static long peak;
    private static final long[] gcCount;
    private static final long[] gcMs;

    static void sample(long l) {
        if (lastSample != 0L && l - lastSample < 500000000L) {
            return;
        }
        long l2 = OS.getCommittedVirtualMemorySize();
        if (committed > 0L && l2 - committed >= 0x10000000L) {
            System.out.println(String.format("[Viewpoint] process memory jumped %+d MiB in %.1f s: %d MiB committed, %d MiB of the system's commit left", (l2 - committed) / 0x100000L, (double)(l - lastSample) * 1.0E-9, l2 / 0x100000L, OS.getFreeSwapSpaceSize() / 0x100000L));
        }
        lastSample = l;
        committed = l2;
        peak = Math.max(peak, l2);
    }

    static String report() {
        Runtime runtime = Runtime.getRuntime();
        long l = runtime.totalMemory();
        long l2 = l - runtime.freeMemory();
        StringBuilder stringBuilder = new StringBuilder(String.format(" | process MiB committed %d (peak %d), heap used %d of %d, direct buffers %d | system MiB free physical %d, commit left %d | gc", committed / 0x100000L, peak / 0x100000L, l2 / 0x100000L, l / 0x100000L, DIRECT == null ? 0L : DIRECT.getMemoryUsed() / 0x100000L, OS.getFreeMemorySize() / 0x100000L, OS.getFreeSwapSpaceSize() / 0x100000L));
        for (int i = 0; i < COLLECTORS.size(); ++i) {
            GarbageCollectorMXBean garbageCollectorMXBean = COLLECTORS.get(i);
            long l3 = garbageCollectorMXBean.getCollectionCount();
            long l4 = garbageCollectorMXBean.getCollectionTime();
            stringBuilder.append(String.format(" %s %d in %d ms,", garbageCollectorMXBean.getName(), l3 - gcCount[i], l4 - gcMs[i]));
            ProcessMemory.gcCount[i] = l3;
            ProcessMemory.gcMs[i] = l4;
        }
        if (!COLLECTORS.isEmpty()) {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
        peak = committed;
        return stringBuilder.toString();
    }

    private ProcessMemory() {
    }

    static {
        gcCount = new long[COLLECTORS.size()];
        gcMs = new long[COLLECTORS.size()];
    }
}

