/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GLCapabilities
 */
package viewpoint.platform;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import viewpoint.platform.GpuScore;

record Hardware(String renderer, String vendor, int vramMiB, int vramFreeMiB, long ramMiB, long ramFreeMiB, long heapMiB, int threads, int width, int height) {
    private static final long MIB = 0x100000L;
    private static final long KIB = 1024L;

    static Hardware probe(int n, int n2) {
        Object object;
        GLCapabilities gLCapabilities = GL.getCapabilities();
        int n3 = 0;
        int n4 = 0;
        if (gLCapabilities.GL_NVX_gpu_memory_info) {
            n3 = (int)((long)GL11.glGetInteger((int)36935) / 1024L);
            n4 = (int)((long)GL11.glGetInteger((int)36937) / 1024L);
        } else if (gLCapabilities.GL_ATI_meminfo) {
            object = new int[4];
            GL11.glGetIntegerv((int)34812, (int[])object);
            n4 = (int)((long)object[0] / 1024L);
        }
        object = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        return new Hardware(String.valueOf(GL11.glGetString((int)7937)), String.valueOf(GL11.glGetString((int)7936)), n3, n4, object.getTotalMemorySize() / 0x100000L, object.getFreeMemorySize() / 0x100000L, Runtime.getRuntime().maxMemory() / 0x100000L, Runtime.getRuntime().availableProcessors(), n, n2);
    }

    double gpuScore() {
        return GpuScore.of(this.renderer);
    }

    boolean integrated() {
        return GpuScore.integrated(this.renderer);
    }

    double pixels() {
        return Math.max(0.25, (double)this.width * (double)this.height / 2073600.0);
    }

    String describe() {
        return String.format(Locale.ROOT, "%s (%s), video memory %s, memory %d MiB (%d free), heap %d MiB, %d threads, %d x %d", this.renderer, this.vendor, this.vramMiB > 0 ? this.vramMiB + " MiB (" + this.vramFreeMiB + " free)" : (this.vramFreeMiB > 0 ? this.vramFreeMiB + " MiB free" : "?"), this.ramMiB, this.ramFreeMiB, this.heapMiB, this.threads, this.width, this.height);
    }
}

