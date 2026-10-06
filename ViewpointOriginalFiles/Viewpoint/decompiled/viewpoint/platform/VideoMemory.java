/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL
 *  org.lwjgl.opengl.GL11
 */
package viewpoint.platform;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public final class VideoMemory {
    private static final long KIB = 1024L;
    private static final long SAMPLE_NANOS = 500000000L;
    private static Boolean available;
    private static long lastSample;
    private static int count;
    private static int freeKib;
    private static int totalKib;
    private static int lineEvictions;
    private static long evictedKib;
    private static long lineEvictedKib;

    public static void sample(long l) {
        if (available == null) {
            available = GL.getCapabilities().GL_NVX_gpu_memory_info;
            int n = totalKib = available != false ? GL11.glGetInteger((int)36935) : 0;
        }
        if (!available.booleanValue() || lastSample != 0L && l - lastSample < 500000000L) {
            return;
        }
        int n = GL11.glGetInteger((int)36938);
        long l2 = Integer.toUnsignedLong(GL11.glGetInteger((int)36939));
        freeKib = GL11.glGetInteger((int)36937);
        if (lastSample != 0L && n > count) {
            lineEvictions += n - count;
            lineEvictedKib += l2 - evictedKib;
            System.out.println(String.format("[Viewpoint] video memory: %d evictions (%d MiB) in %.1f s, %d MiB of %d free", n - count, (l2 - evictedKib) / 1024L, (double)(l - lastSample) * 1.0E-9, (long)freeKib / 1024L, (long)totalKib / 1024L));
        }
        count = n;
        evictedKib = l2;
        lastSample = l;
    }

    static String report() {
        if (available == null || !available.booleanValue()) {
            return " | video memory: the driver does not tell it";
        }
        String string = String.format(" | video memory MiB free %d of %d, evictions %d (%d MiB)", (long)freeKib / 1024L, (long)totalKib / 1024L, lineEvictions, lineEvictedKib / 1024L);
        lineEvictions = 0;
        lineEvictedKib = 0L;
        return string;
    }

    private VideoMemory() {
    }
}

