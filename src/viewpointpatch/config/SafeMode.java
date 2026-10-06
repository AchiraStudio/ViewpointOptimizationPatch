package viewpointpatch.config;

import viewpointpatch.diagnostics.PatchLogger;

public final class SafeMode {
    public static volatile boolean globalSafeMode = false;
    public static volatile boolean retirementDisabled = false;
    public static volatile boolean floorOptimizationDisabled = false;

    private SafeMode() {}

    public static boolean isRetirementOptimized() {
        return !globalSafeMode && !retirementDisabled;
    }

    public static boolean isFloorOptimizationEnabled() {
        return !globalSafeMode && !floorOptimizationDisabled;
    }

    public static void disableRetirement(String reason) {
        if (!retirementDisabled) {
            retirementDisabled = true;
            PatchLogger.warn("Retirement optimization disabled: " + reason + ". Falling back to vanilla engine retirement.");
        }
    }

    public static void triggerGlobalSafeMode(String reason) {
        if (!globalSafeMode) {
            globalSafeMode = true;
            retirementDisabled = true;
            floorOptimizationDisabled = true;
            PatchLogger.error("GLOBAL SAFE MODE ACTIVATED: " + reason);
            PatchLogger.warn("All experimental optimizations deactivated. Engine operating with vanilla safety guarantees.");
        }
    }
}

