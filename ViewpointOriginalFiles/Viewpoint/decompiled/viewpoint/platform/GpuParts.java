/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL33
 */
package viewpoint.platform;

import java.util.Arrays;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

public final class GpuParts {
    public static final int WORLD = 0;
    public static final int MODELS = 1;
    public static final int BAND = 2;
    public static final int NEAR_CASCADE = 3;
    public static final int MID_CASCADE = 4;
    public static final int FAR_CASCADE = 5;
    public static final int FLASH_MAP = 6;
    public static final int RAIN_MAP = 7;
    public static final int FAR_MAP = 8;
    public static final int FAR_SHELLS = 9;
    public static final int FAR_BOXES = 10;
    public static final int FAR_TREES = 11;
    public static final int CLOUDS = 12;
    public static final int LIGHT = 13;
    public static final int GLASS = 14;
    public static final int COMPOSITE = 15;
    public static final int TAA = 16;
    public static final int LAMP_MAPS = 17;
    public static final int FAR_LAMPS = 18;
    public static final int FAR_WIDE_MAP = 19;
    public static final int WEATHER_MAP = 20;
    public static final int PACK_PASSES = 21;
    public static final int DISTANT_RAIN = 24;
    private static final String[] NAMES = new String[]{"world", "models", "band", "near cascade", "mid cascade", "far cascade", "flashlight map", "rain map", "far map", "far shells", "far boxes", "far trees", "clouds", "light", "glass", "composite", "taa", "lamp maps", "far lamps", "far wide map", "weather map", "pack translucent", "pack composite", "pack final", "distant rain"};
    private static final int COUNT = NAMES.length;
    private static final int SLOTS = 4;
    private static int[][] queries;
    private static final int[] begun;
    private static final int[] ended;
    private static int slot;
    private static int frames;
    private static final double[] ms;
    private static final int[] runs;

    public static void begin(int n) {
        if (queries == null) {
            for (int[] nArray : queries = new int[4][COUNT * 2]) {
                GL15.glGenQueries((int[])nArray);
            }
        }
        GL33.glQueryCounter((int)queries[slot][n * 2], (int)36392);
        int n2 = slot;
        begun[n2] = begun[n2] | 1 << n;
    }

    public static void end(int n) {
        if ((begun[slot] & 1 << n) == 0) {
            return;
        }
        GL33.glQueryCounter((int)queries[slot][n * 2 + 1], (int)36392);
        int n2 = slot;
        ended[n2] = ended[n2] | 1 << n;
    }

    static void frameEnded() {
        if (queries == null) {
            return;
        }
        int n = ended[slot = (slot + 1) % 4];
        if (GpuParts.available(n)) {
            for (int i = 0; i < COUNT; ++i) {
                if ((n & 1 << i) == 0) continue;
                long l = GL33.glGetQueryObjectui64((int)queries[slot][i * 2], (int)34918);
                long l2 = GL33.glGetQueryObjectui64((int)queries[slot][i * 2 + 1], (int)34918);
                int n2 = i;
                ms[n2] = ms[n2] + (double)(l2 - l) * 1.0E-6;
                int n3 = i;
                runs[n3] = runs[n3] + 1;
            }
            ++frames;
        }
        GpuParts.begun[GpuParts.slot] = 0;
        GpuParts.ended[GpuParts.slot] = 0;
    }

    private static boolean available(int n) {
        for (int i = 0; i < COUNT; ++i) {
            if ((n & 1 << i) == 0 || GL15.glGetQueryObjecti((int)queries[slot][i * 2 + 1], (int)34919) != 0) continue;
            return false;
        }
        return true;
    }

    static String report() {
        StringBuilder stringBuilder = new StringBuilder(" | gpu parts ms/frame:");
        boolean bl = false;
        for (int i = 0; i < COUNT; ++i) {
            if (runs[i] == 0 || frames == 0) continue;
            stringBuilder.append(bl ? ", " : " ").append(String.format("%s %.2f", NAMES[i], ms[i] / (double)frames));
            bl = true;
            if (runs[i] >= frames) continue;
            stringBuilder.append(String.format(" (%.2f x %.0f%%)", ms[i] / (double)runs[i], 100.0 * (double)runs[i] / (double)frames));
        }
        Arrays.fill(ms, 0.0);
        Arrays.fill(runs, 0);
        frames = 0;
        return bl ? stringBuilder.toString() : "";
    }

    private GpuParts() {
    }

    static {
        begun = new int[4];
        ended = new int[4];
        ms = new double[COUNT];
        runs = new int[COUNT];
    }
}

