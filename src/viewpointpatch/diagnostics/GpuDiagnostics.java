package viewpointpatch.diagnostics;

import viewpointpatch.performance.AdaptiveQuality;
import viewpointpatch.performance.FrameTimeController;
import viewpointpatch.render.SafeRetirement;

public final class GpuDiagnostics {
    private static long lastReportNano = 0;
    private static final long REPORT_INTERVAL_NS = 30_000_000_000L; // 30 seconds

    private GpuDiagnostics() {}

    public static void tick() {
        long now = System.nanoTime();
        if (now - lastReportNano >= REPORT_INTERVAL_NS) {
            lastReportNano = now;
            reportStatus();
        }
    }

    public static void reportStatus() {
        if (FrameTimeController.totalFrames < 60) return;

        double fps = 1000.0 / Math.max(1.0, FrameTimeController.rollingAvgMs);
        PatchLogger.info(String.format(
            java.util.Locale.ROOT,
            "Telemetry: ~%.1f FPS (avg: %.2f ms, peak: %.1f ms) | Adaptive Level: %d | Fences Reclaimed: %d | Soft-Cap Cleared: %d",
            fps,
            FrameTimeController.rollingAvgMs,
            FrameTimeController.peakFrameMs,
            AdaptiveQuality.getCurrentLevel(),
            SafeRetirement.fencesSuccessfullyReclaimed,
            SafeRetirement.softCapWaitsSatisfied
        ));
    }
}

