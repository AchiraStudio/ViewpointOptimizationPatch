package viewpointpatch;

import me.zed_0xff.zombie_buddy.Patch;
import viewpointpatch.config.PatchConfig;
import viewpointpatch.diagnostics.FailureTracker;
import viewpointpatch.diagnostics.GpuDiagnostics;
import viewpointpatch.diagnostics.PatchLogger;
import viewpointpatch.gpu.GpuCapabilities;
import viewpointpatch.gpu.GpuProfiler;
import viewpointpatch.performance.FrameTimeController;
import viewpointpatch.profile.ProfileManager;

@Patch(className = "viewpoint.SceneDrawer", methodName = "drawFrame")
public class Patch_SceneDrawer {
    private static volatile boolean probed = false;

    @Patch.OnEnter
    public static void onEnter() {
        if (!probed) {
            probed = true;
            try {
                PatchConfig.load();
                GpuCapabilities caps = GpuProfiler.detect();
                ProfileManager.applyProfile(caps);
            } catch (Throwable t) {
                FailureTracker.recordFailure("GpuProbe", t);
                PatchLogger.error("Failed during render-thread GPU probe: " + t.getMessage(), t);
            }
        }

        try {
            FrameTimeController.onFrame();
            GpuDiagnostics.tick();
        } catch (Throwable ignored) {}
    }
}
