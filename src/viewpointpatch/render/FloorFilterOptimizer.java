package viewpointpatch.render;

import java.lang.reflect.Method;
import viewpoint.platform.LiveSettings;
import viewpointpatch.config.SafeMode;
import viewpointpatch.diagnostics.FailureTracker;
import viewpointpatch.diagnostics.PatchLogger;

public final class FloorFilterOptimizer {
    private static Method valueMethod;
    private static volatile boolean initialized = false;

    private FloorFilterOptimizer() {}

    private static String getLiveSetting(String key) {
        try {
            if (valueMethod == null) {
                valueMethod = LiveSettings.class.getDeclaredMethod("value", String.class);
                valueMethod.setAccessible(true);
            }
            return (String) valueMethod.invoke(null, key);
        } catch (Throwable t) {
            FailureTracker.recordFailure("FloorFilter", t);
            return null;
        }
    }

    /**
     * Replaces heavy 36-tap Lanczos floor baking filter (ret = 2) with high-speed Linear filter (ret = 1)
     * if the floor filter is not explicitly set to Lanczos.
     */
    public static int optimizeFilter(int originalFilter) {
        if (!SafeMode.isFloorOptimizationEnabled()) {
            return originalFilter;
        }

        // 2 = BAKE_LANCZOS
        if (originalFilter == 2) {
            try {
                String filter = getLiveSetting("floors.filter");
                if (filter != null && !"Lanczos".equalsIgnoreCase(filter.trim())) {
                    return 1; // 1 = BAKE_LINEAR (smooth, zero stutter on chunk loading)
                }
            } catch (Throwable t) {
                FailureTracker.recordFailure("FloorFilter", t);
            }
        }
        return originalFilter;
    }
}

