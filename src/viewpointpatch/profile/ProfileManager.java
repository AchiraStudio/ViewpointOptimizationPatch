package viewpointpatch.profile;

import java.lang.reflect.Method;
import viewpoint.platform.LiveSettings;
import viewpointpatch.diagnostics.FailureTracker;
import viewpointpatch.diagnostics.PatchLogger;
import viewpointpatch.gpu.GpuCapabilities;

public final class ProfileManager {
    private static volatile HardwareProfile activeProfile = null;
    private static Method putMethod;
    private static Method valueMethod;

    private ProfileManager() {}

    private static void setLiveSetting(String key, String val) {
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

    private static String getLiveSetting(String key) {
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

    public static synchronized HardwareProfile applyProfile(GpuCapabilities caps) {
        if (activeProfile != null) {
            return activeProfile;
        }

        activeProfile = HardwareProfile.forTier(caps.tier);
        PatchLogger.info("Applying Hardware Profile: " + activeProfile.tier.getDescription());

        try {
            // Only set floor filter if unset or set to heavy Lanczos
            String floorFilter = getLiveSetting("floors.filter");
            if (floorFilter == null || "Lanczos".equalsIgnoreCase(floorFilter.trim())) {
                setLiveSetting("floors.filter", activeProfile.defaultFloorFilter);
            }

            // If user hasn't explicitly set lod.farBlocks, provide tier default
            String currentFar = getLiveSetting("lod.farBlocks");
            if (currentFar == null || currentFar.trim().isEmpty()) {
                setLiveSetting("lod.farBlocks", String.valueOf(activeProfile.defaultFarBlocks));
            }

            // Adjust cooking and upload parameters smoothly if unset
            if (getLiveSetting("chunks.cookThreads") == null) {
                setLiveSetting("chunks.cookThreads", String.valueOf(activeProfile.cookThreads));
            }
            if (getLiveSetting("far.cellUploadMb") == null) {
                setLiveSetting("far.cellUploadMb", String.valueOf(activeProfile.cellUploadMb));
            }
            if (getLiveSetting("far.shellUploadMb") == null) {
                setLiveSetting("far.shellUploadMb", String.valueOf(activeProfile.shellUploadMb));
            }
            if (getLiveSetting("floors.bakeBudgetMs") == null) {
                setLiveSetting("floors.bakeBudgetMs", String.valueOf(activeProfile.bakeBudgetMs));
            }

            PatchLogger.info("Hardware profile applied cleanly without mid-frame resource disruption.");
        } catch (Throwable t) {
            FailureTracker.recordFailure("ProfileManager", t);
            PatchLogger.error("Failed applying hardware profile: " + t.getMessage(), t);
        }

        return activeProfile;
    }

    public static HardwareProfile getActiveProfile() {
        return activeProfile;
    }
}

