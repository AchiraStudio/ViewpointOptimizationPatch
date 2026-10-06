/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

final class Hitches {
    static final double SLOW_MS = 50.0;
    private static final int MAX_LINES = 10;
    private static final long GAP_NANOS = 1000000000L;
    private static long lastRender;
    private static int renderSlow;
    private static int renderLines;
    private static double renderWorst;
    private static volatile int mainSlow;
    private static volatile int mainLines;
    private static volatile double mainWorst;

    static void render(long l, double[] dArray, String[] stringArray) {
        long l2 = l - lastRender;
        boolean bl = lastRender != 0L && l2 < 1000000000L;
        lastRender = l;
        if (!bl || (double)l2 * 1.0E-6 < 50.0) {
            return;
        }
        double d = (double)l2 * 1.0E-6;
        double d2 = 0.0;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < dArray.length; ++i) {
            d2 += dArray[i];
            if (!(dArray[i] >= 0.5)) continue;
            stringBuilder.append(String.format(" %s %.1f", stringArray[i], dArray[i]));
        }
        ++renderSlow;
        renderWorst = Math.max(renderWorst, d);
        if (renderLines++ < 10) {
            System.out.println(String.format("[Viewpoint] slow frame on the render thread: %.1f ms, our passes %.1f (%s), the rest %.1f (the game's own drawing, or waiting for the main thread)", d, d2, stringBuilder.length() == 0 ? "none over 0.5" : stringBuilder.substring(1), d - d2));
        }
    }

    static void main(double d, double d2, double d3, double d4, int n) {
        ++mainSlow;
        mainWorst = Math.max(mainWorst, d);
        double d5 = d2 + d3 + d4;
        if (mainLines++ < 10) {
            System.out.println(String.format("[Viewpoint] slow frame on the main thread: %.1f ms, ours %.1f (chunk cache %.1f: %d builds; far world %.1f; snapshot %.1f), the rest %.1f (the game's update, the collector's pauses)", d, d5, d2, n, d3, d4, d - d5));
        }
    }

    static String report() {
        String string = String.format(" | frames over %.0f ms: render %d (worst %.0f), main %d (worst %.0f)", 50.0, renderSlow, renderWorst, mainSlow, mainWorst);
        renderLines = 0;
        renderSlow = 0;
        renderWorst = 0.0;
        mainLines = 0;
        mainSlow = 0;
        mainWorst = 0.0;
        return string;
    }

    private Hitches() {
    }
}

