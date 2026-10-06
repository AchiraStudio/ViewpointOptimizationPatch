package viewpointpatch;

import java.lang.reflect.Method;
import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.platform.LiveSettings;
import viewpointpatch.diagnostics.FailureTracker;
import viewpointpatch.diagnostics.PatchLogger;
import viewpointpatch.gpu.GpuCapabilities;
import viewpointpatch.gpu.GpuProfiler;

@Patch(className = "viewpoint.SceneDrawer", methodName = "drawFrame")
public class Patch_SceneDrawer {
    private static volatile boolean probed = false;
    private static Method putMethod;
    private static Method valueMethod;

    public static void setLiveSetting(String key, String val) {
        try {
            if (putMethod == null) {
                putMethod = LiveSettings.class.getDeclaredMethod("put", String.class, String.class);
                putMethod.setAccessible(true);
            }
            putMethod.invoke(null, key, val);
        } catch (Throwable t) {
            FailureTracker.recordFailure("LiveSettings", t);
        }
    }

    public static String getLiveSetting(String key) {
        try {
            if (valueMethod == null) {
                valueMethod = LiveSettings.class.getDeclaredMethod("value", String.class);
                valueMethod.setAccessible(true);
            }
            return (String) valueMethod.invoke(null, key);
        } catch (Throwable t) {
            FailureTracker.recordFailure("LiveSettings", t);
            return null;
        }
    }

    @Patch.OnEnter
    public static void onEnter() {
        if (!probed) {
            probed = true;
            try {
                // Safe hardware probe on the render thread
                GpuCapabilities caps = GpuProfiler.detect();

                // Check floor filter setting: default to fast Trilinear if unset or Lanczos
                String floorFilter = getLiveSetting("floors.filter");
                if (floorFilter == null || "Lanczos".equalsIgnoreCase(floorFilter.trim())) {
                    PatchLogger.info("Setting floor filter to Trilinear for stutter-free texture baking.");
                    setLiveSetting("floors.filter", "Trilinear");
                }
            } catch (Throwable t) {
                FailureTracker.recordFailure("GpuProbe", t);
                PatchLogger.error("Failed during render-thread GPU probe: " + t.getMessage(), t);
            }
        }
    }
}
