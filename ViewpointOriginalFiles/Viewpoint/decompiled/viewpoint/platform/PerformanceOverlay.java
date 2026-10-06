/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjglx.opengl.DisplayMode
 *  zombie.core.Core
 *  zombie.core.SpriteRenderer
 *  zombie.ui.TextManager
 *  zombie.ui.UIFont
 */
package viewpoint.platform;

import java.util.Locale;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.DisplayMode;
import viewpoint.platform.Build;
import viewpoint.platform.FrameTimes;
import viewpoint.platform.Keys;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.Profile;
import viewpoint.platform.Utilization;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

public final class PerformanceOverlay {
    private static final long REFRESH_NANOS = 250000000L;
    private static final LiveSettings.Number SPAN = LiveSettings.number("overlay.spanSeconds", "Overlay statistics over (s)", "Debug/Profiling", 1.0f, 30.0f, 1.0f, 5.0f);
    private static final LiveSettings.Choice MODE = LiveSettings.choice("overlay.mode", "Performance overlay", "Debug/Profiling", new String[]{"Full", "Compact", "Graph only", "Off"}, Mode.OFF.ordinal());
    private static final int BARS_FULL = 120;
    private static final int BARS_COMPACT = 80;
    private static final int TOP = 40;
    private static final int RIGHT = 15;
    private static final int PAD = 5;
    private static final float[] WHITE;
    private static final float[] GREEN;
    private static final float[] AMBER;
    private static final float[] RED;
    private static final UIFont FONT;
    private static boolean failed;
    private static long refreshedAt;
    private static String[] fullLines;
    private static String compactLine;
    private static String fpsText;
    private static String verdict;
    private static float[] fpsColour;
    private static float[] verdictColour;
    private static float budgetMs;
    private static boolean capFromDisplay;
    private static float fps;

    static Mode mode() {
        return Mode.values()[MODE.get()];
    }

    static void cycle() {
        MODE.set((MODE.get() + 1) % Mode.values().length);
    }

    static void loaded() {
        if (!Build.RELEASE && PerformanceOverlay.mode() == Mode.OFF) {
            MODE.set(Mode.FULL.ordinal());
        }
    }

    public static void draw() {
        if (Keys.OVERLAY.pressed()) {
            PerformanceOverlay.cycle();
        }
        if (PerformanceOverlay.mode() == Mode.OFF || failed) {
            return;
        }
        long l = System.nanoTime();
        Utilization.sample(l);
        if (l - refreshedAt >= 250000000L) {
            refreshedAt = l;
            PerformanceOverlay.refresh(l);
        }
        try {
            PerformanceOverlay.render();
        }
        catch (RuntimeException runtimeException) {
            failed = true;
            System.out.println("[Viewpoint] performance overlay off after an error: " + String.valueOf(runtimeException));
        }
    }

    private static Ours ours(long l) {
        if (l - Profile.lastFrameAt > 1000000000L) {
            return null;
        }
        float f = (float)Profile.lastMainOursMs;
        float f2 = (float)Profile.lastRenderMs;
        return new Ours(f, f2, f + f2, (float)Math.max(0.0, Profile.lastMainMs - (double)f));
    }

    private static void refresh(long l) {
        FrameTimes.Window window = FrameTimes.window(l, (long)SPAN.getInt() * 1000000000L);
        if (window.count() == 0) {
            fullLines = new String[]{"performance overlay: waiting for frames"};
            compactLine = "waiting for frames";
            verdict = "";
            fpsText = "";
            return;
        }
        FrameTimes.Percentiles percentiles = FrameTimes.percentiles(window);
        fps = window.lastSecond();
        capFromDisplay = PerformanceSettings.instance.isFramerateUncapped();
        int n = capFromDisplay ? PerformanceOverlay.displayRefreshRate() : PerformanceSettings.getLockFPS();
        budgetMs = 1000.0f / (float)(n > 0 ? n : 240);
        String string = Float.isNaN(Utilization.gpu) ? "n/a" : String.format(Locale.ROOT, "%.0f %%", Float.valueOf(Utilization.gpu));
        fpsText = String.format(Locale.ROOT, "%3.0f fps", Float.valueOf(fps));
        fpsColour = n > 0 ? (fps >= (float)n * 0.98f ? GREEN : (fps >= (float)n * 0.5f ? AMBER : RED)) : (fps >= 200.0f ? GREEN : (fps >= 100.0f ? AMBER : RED));
        Ours ours = PerformanceOverlay.ours(l);
        PerformanceOverlay.verdict(n, ours);
        fullLines = PerformanceOverlay.fullLines(percentiles, window, n, PerformanceOverlay.threadLine(ours, string));
        compactLine = ours != null ? String.format(Locale.ROOT, "%3.0f fps  %4.2f ms (game %.1f/ours %.1f)  main %.0f%%  GPU %s", Float.valueOf(fps), Float.valueOf(percentiles.mean()), Float.valueOf(ours.gameMain()), Float.valueOf(ours.total()), Float.valueOf(Utilization.game), string) : String.format(Locale.ROOT, "%3.0f fps  %4.2f ms (p99 %.1f)  main %.0f%%  GPU %s  %s", Float.valueOf(fps), Float.valueOf(percentiles.mean()), Float.valueOf(percentiles.p99()), Float.valueOf(Utilization.game), string, verdict);
    }

    private static int displayRefreshRate() {
        try {
            DisplayMode displayMode = Display.getDesktopDisplayMode();
            return displayMode == null ? 0 : displayMode.getFrequency();
        }
        catch (RuntimeException runtimeException) {
            return 0;
        }
    }

    private static String threadLine(Ours ours, String string) {
        if (ours != null) {
            return String.format(Locale.ROOT, "main %.0f%% (game %.1f, ours %.1f)  render %.0f%% (ours %.1f)  GPU %s", Float.valueOf(Utilization.game), Float.valueOf(ours.gameMain()), Float.valueOf(ours.main()), Float.valueOf(Utilization.render), Float.valueOf(ours.render()), string);
        }
        return String.format(Locale.ROOT, "main %.0f%%   render %.0f%%   process %.0f%%   GPU %s", Float.valueOf(Utilization.game), Float.valueOf(Utilization.render), Float.valueOf(Utilization.process), string);
    }

    private static String[] fullLines(FrameTimes.Percentiles percentiles, FrameTimes.Window window, int n, String string) {
        Runtime runtime = Runtime.getRuntime();
        String string2 = n <= 0 ? "none" : (capFromDisplay ? n + " Hz display" : n + " fps");
        return new String[]{String.format(Locale.ROOT, "   %4.2f ms   cap %s", Float.valueOf(percentiles.mean()), string2), String.format(Locale.ROOT, "p50 %.1f   p99 %.1f   p99.9 %.1f   max %.1f ms", Float.valueOf(percentiles.p50()), Float.valueOf(percentiles.p99()), Float.valueOf(percentiles.p999()), Float.valueOf(percentiles.max())), String.format(Locale.ROOT, "1%%-low %.0f fps   jitter %.1f ms   spikes %d", Float.valueOf(percentiles.p99() > 0.0f ? 1000.0f / percentiles.p99() : 0.0f), Float.valueOf(window.count() > 1 ? window.jitter() / (float)(window.count() - 1) : 0.0f), percentiles.spikes()), string, String.format(Locale.ROOT, "process %.0f%% (%d cores)   system %.0f%%   heap %.1f/%.1f GB", Float.valueOf(Utilization.process), runtime.availableProcessors(), Float.valueOf(Utilization.system), Float.valueOf((float)(runtime.totalMemory() - runtime.freeMemory()) / 1.0737418E9f), Float.valueOf((float)runtime.maxMemory() / 1.0737418E9f))};
    }

    private static void verdict(int n, Ours ours) {
        String string;
        if (n > 0 && fps >= (float)n * 0.98f) {
            verdict = "at the cap";
            verdictColour = GREEN;
            return;
        }
        float f = Float.isNaN(Utilization.gpu) ? 0.0f : Utilization.gpu;
        float f2 = Math.max(Utilization.game, Math.max(Utilization.render, f));
        String string2 = string = n > 0 ? "below cap: " : "";
        if (f2 < 90.0f) {
            verdict = (n > 0 ? "below cap, " : "") + "nothing saturated: waits/sync";
            verdictColour = RED;
            return;
        }
        verdictColour = AMBER;
        if (f2 == Utilization.game && ours == null) {
            verdict = string + "main thread bound";
        } else if (f2 == Utilization.game) {
            boolean bl = ours.main() > ours.gameMain() && ours.main() > 1.0f;
            verdict = string + String.format(Locale.ROOT, "main thread bound (%s: %.1fms)", bl ? "ours" : "game", Float.valueOf(bl ? ours.main() : ours.gameMain()));
        } else {
            verdict = f2 == Utilization.render ? string + "render bound" + (ours != null && ours.render() > 2.0f ? String.format(Locale.ROOT, " (ours: %.1fms)", Float.valueOf(ours.render())) : " (the game's)") : string + "GPU bound";
        }
    }

    private static void render() {
        TextManager textManager = TextManager.instance;
        SpriteRenderer spriteRenderer = SpriteRenderer.instance;
        int n = Math.max(13, textManager.getFontHeight(FONT) + 1);
        int n2 = Core.getInstance().getScreenWidth();
        switch (PerformanceOverlay.mode().ordinal()) {
            case 0: {
                PerformanceOverlay.renderFull(textManager, spriteRenderer, n, n2);
                break;
            }
            case 1: {
                PerformanceOverlay.renderCompact(textManager, spriteRenderer, n, n2);
                break;
            }
            case 2: {
                PerformanceOverlay.renderGraph(textManager, spriteRenderer, n2);
                break;
            }
        }
    }

    private static void renderFull(TextManager textManager, SpriteRenderer spriteRenderer, int n, int n2) {
        int n3;
        int n4;
        int n5 = 55;
        int n6 = 240;
        int n7 = 0;
        int n8 = fpsText.isEmpty() ? 0 : textManager.MeasureStringX(FONT, fpsText);
        for (n4 = 0; n4 < fullLines.length; ++n4) {
            n7 = Math.max(n7, (n4 == 0 ? n8 : 0) + textManager.MeasureStringX(FONT, fullLines[n4]));
        }
        n7 = Math.max(n7, textManager.MeasureStringX(FONT, verdict));
        n4 = Math.max(n7, n6) + 10;
        int n9 = fullLines.length + (verdict.isEmpty() ? 0 : 1);
        int n10 = n9 * n + n5 + 15;
        int n11 = n2 - n4 - 15;
        int n12 = n11 + n4 - 5;
        spriteRenderer.renderi(null, n11, 40, n4, n10, 0.0f, 0.0f, 0.0f, 0.65f, null);
        int n13 = 45;
        for (n3 = 0; n3 < fullLines.length; ++n3) {
            int n14 = n12 - textManager.MeasureStringX(FONT, fullLines[n3]);
            if (n3 == 0 && n8 > 0) {
                textManager.DrawString(FONT, (double)(n14 -= n8), (double)n13, fpsText, (double)fpsColour[0], (double)fpsColour[1], (double)fpsColour[2], 1.0);
                n14 += n8;
            }
            textManager.DrawString(FONT, (double)n14, (double)n13, fullLines[n3], 1.0, 1.0, 1.0, 1.0);
            n13 += n;
        }
        if (!verdict.isEmpty()) {
            n3 = n12 - textManager.MeasureStringX(FONT, verdict);
            textManager.DrawString(FONT, (double)n3, (double)n13, verdict, (double)verdictColour[0], (double)verdictColour[1], (double)verdictColour[2], 1.0);
        }
        PerformanceOverlay.bars(spriteRenderer, n12 - n6, 45 + n9 * n + 5, 120, n5);
    }

    private static void renderCompact(TextManager textManager, SpriteRenderer spriteRenderer, int n, int n2) {
        int n3 = 35;
        int n4 = 160;
        int n5 = textManager.MeasureStringX(FONT, compactLine);
        int n6 = Math.max(n5, n4) + 10;
        int n7 = n + n3 + 15;
        int n8 = n2 - n6 - 15;
        int n9 = n8 + n6 - 5;
        spriteRenderer.renderi(null, n8, 40, n6, n7, 0.0f, 0.0f, 0.0f, 0.65f, null);
        textManager.DrawString(FONT, (double)(n9 - n5), 45.0, compactLine, 1.0, 1.0, 1.0, 1.0);
        PerformanceOverlay.bars(spriteRenderer, n9 - n4, 45 + n + 5, 80, n3);
    }

    private static void renderGraph(TextManager textManager, SpriteRenderer spriteRenderer, int n) {
        int n2 = 45;
        int n3 = 160;
        int n4 = n3 + 10;
        int n5 = n2 + 10;
        int n6 = n - n4 - 15;
        int n7 = n6 + n4 - 5;
        int n8 = 45;
        spriteRenderer.renderi(null, n6, 40, n4, n5, 0.0f, 0.0f, 0.0f, 0.65f, null);
        PerformanceOverlay.bars(spriteRenderer, n7 - n3, n8, 80, n2);
        if (!fpsText.isEmpty()) {
            int n9 = textManager.MeasureStringX(FONT, fpsText);
            textManager.DrawString(FONT, (double)(n7 - n9 - 4), (double)(n8 + 2), fpsText, (double)fpsColour[0], (double)fpsColour[1], (double)fpsColour[2], 0.9);
        }
    }

    private static void bars(SpriteRenderer spriteRenderer, int n, int n2, int n3, int n4) {
        float f = (float)n4 / (3.0f * budgetMs);
        int n5 = FrameTimes.head;
        int n6 = Math.min(n3, Math.min(n5, 8128));
        for (int i = 0; i < n6; ++i) {
            float f2;
            int n7 = n5 - n6 + i & 0x1FFF;
            float f3 = FrameTimes.frameMs[n7];
            float f4 = f3 / budgetMs;
            float f5 = FrameTimes.gpuMs[n7];
            int n8 = Math.max(1, Math.min(n4, (int)(f3 * f)));
            float f6 = f2 = f4 > 1.1f ? 1.0f : 0.4f;
            float f7 = f4 > 2.0f ? 0.3f : (f4 > 1.1f ? 0.8f : 1.0f);
            spriteRenderer.renderi(null, n + i * 2, n2 + n4 - n8, 2, n8, f2, f7, 0.4f, 0.9f, null);
            if (!(f5 > 0.0f)) continue;
            int n9 = Math.max(1, Math.min(n4, (int)(f5 * f)));
            spriteRenderer.renderi(null, n + i * 2, n2 + n4 - n9, 1, n9, 0.4f, 0.6f, 1.0f, 0.9f, null);
        }
        spriteRenderer.renderi(null, n, n2 + n4 - (int)(budgetMs * f), n3 * 2, 1, 1.0f, 1.0f, 1.0f, 0.7f, null);
    }

    private PerformanceOverlay() {
    }

    static {
        MODE.describe("The frame times, how busy each thread and the GPU are, and the renderer's share of each frame, top right: in full, one line, the graph alone, or off.");
        WHITE = new float[]{1.0f, 1.0f, 1.0f};
        GREEN = new float[]{0.45f, 1.0f, 0.45f};
        AMBER = new float[]{1.0f, 0.8f, 0.3f};
        RED = new float[]{1.0f, 0.45f, 0.45f};
        FONT = UIFont.CodeSmall;
        fullLines = new String[0];
        compactLine = "";
        fpsText = "";
        verdict = "";
        fpsColour = WHITE;
        verdictColour = WHITE;
        budgetMs = 4.1666665f;
    }

    static enum Mode {
        FULL,
        COMPACT,
        GRAPH_ONLY,
        OFF;

    }

    private record Ours(float main, float render, float total, float gameMain) {
    }
}

