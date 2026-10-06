package viewpointpatch.performance;

import viewpointpatch.config.PatchConfig;

public final class FrameTimeController {
    private static long lastFrameNano = 0;
    private static int smoothFrameCount = 0;
    private static int cooldownFrames = 0;

    private static final int STABILIZATION_WINDOW = 120; // 120 consecutive smooth frames to step quality up
    private static final int THROTTLE_COOLDOWN = 30;     // 30 frames cooldown after throttling down

    // Telemetry tracking
    public static long totalFrames = 0;
    public static double lastFrameMs = 16.67;
    public static double rollingAvgMs = 16.67;
    public static double peakFrameMs = 0.0;

    private FrameTimeController() {}

    public static void onFrame() {
        long now = System.nanoTime();
        if (lastFrameNano == 0) {
            lastFrameNano = now;
            return;
        }

        double frameMs = (now - lastFrameNano) / 1_000_000.0;
        lastFrameNano = now;
        totalFrames++;
        lastFrameMs = frameMs;

        // Ignore absurd outliers from pause/minimization (> 1000ms)
        if (frameMs > 1000.0) return;

        // Exponential rolling average (smoothing factor alpha = 0.05)
        rollingAvgMs = (rollingAvgMs * 0.95) + (frameMs * 0.05);
        if (frameMs > peakFrameMs) {
            peakFrameMs = frameMs;
        }

        if (cooldownFrames > 0) {
            cooldownFrames--;
            return;
        }

        double target = PatchConfig.targetFrameMs;
        double spikeThreshold = target * 1.35; // e.g. 22.5ms for 60 FPS
        double smoothThreshold = target * 0.90; // e.g. 15.0ms for 60 FPS

        if (frameMs > spikeThreshold) {
            // Frame hitch / stall detected! Throttle background budgets immediately
            smoothFrameCount = 0;
            cooldownFrames = THROTTLE_COOLDOWN;
            AdaptiveQuality.throttleDown();
        } else if (frameMs < smoothThreshold) {
            // Sustained smooth performance
            smoothFrameCount++;
            if (smoothFrameCount >= STABILIZATION_WINDOW) {
                smoothFrameCount = 0;
                AdaptiveQuality.recoverUp();
            }
        } else {
            // In the nominal zone [smoothThreshold, spikeThreshold]
            smoothFrameCount = Math.max(0, smoothFrameCount - 1);
        }
    }
}

