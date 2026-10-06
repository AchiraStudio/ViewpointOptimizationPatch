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
        long waitMicros = SafeRetirement.totalWaitTimeNs / 1_000L;

        String farLod = viewpointpatch.profile.ProfileManager.getLiveSetting("lod.farBlocks");
        String bakeMs = viewpointpatch.profile.ProfileManager.getLiveSetting("floors.bakeBudgetMs");
        String uploadMb = viewpointpatch.profile.ProfileManager.getLiveSetting("far.cellUploadMb");

        PatchLogger.info("======================= TELEMETRY REPORT =======================");
        PatchLogger.info(String.format(
            java.util.Locale.ROOT,
            "  Frame Pacing:   ~%.1f FPS (Avg: %.2f ms | Peak: %.1f ms | Total Frames: %d)",
            fps, FrameTimeController.rollingAvgMs, FrameTimeController.peakFrameMs, FrameTimeController.totalFrames
        ));
        PatchLogger.info(String.format(
            java.util.Locale.ROOT,
            "  GPU Retirement: Queue: %d (Peak: %d, Avg: %.1f) | Cleared: %d | Soft-Waits: %d | WaitTime: %d us",
            SafeRetirement.currentQueueDepth, SafeRetirement.peakQueueDepth, SafeRetirement.rollingQueueDepth,
            SafeRetirement.fencesSuccessfullyReclaimed, SafeRetirement.softCapWaitsSatisfied, waitMicros
        ));
        PatchLogger.info(String.format(
            java.util.Locale.ROOT,
            "  Adaptive State: Level: %d | Far LOD: %s blocks | Bake: %s ms | Upload: %s MB",
            AdaptiveQuality.getCurrentLevel(),
            farLod != null ? farLod : "default",
            bakeMs != null ? bakeMs : "default",
            uploadMb != null ? uploadMb : "default"
        ));
        PatchLogger.info("=================================================================");
    }
}

