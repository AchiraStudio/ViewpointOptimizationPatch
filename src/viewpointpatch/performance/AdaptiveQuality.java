package viewpointpatch.performance;

import java.lang.reflect.Method;
import viewpoint.platform.LiveSettings;
import viewpointpatch.config.PatchConfig;
import viewpointpatch.diagnostics.PatchLogger;
import viewpointpatch.profile.HardwareProfile;
import viewpointpatch.profile.ProfileManager;

public final class AdaptiveQuality {
    // Throttling levels: 0 (nominal/highest), 1 (light throttle), 2 (moderate), 3 (heavy throttle)
    public static final int LEVEL_NOMINAL = 0;
    public static final int LEVEL_LIGHT = 1;
    public static final int LEVEL_MODERATE = 2;
    public static final int LEVEL_HEAVY = 3;

    private static volatile int currentLevel = LEVEL_NOMINAL;
    private static Method putMethod;

    private AdaptiveQuality() {}

    private static void setLiveSetting(String key, String val) {
        try {
            if (putMethod == null) {
                putMethod = LiveSettings.class.getDeclaredMethod("put", String.class, String.class);
                putMethod.setAccessible(true);
            }
            putMethod.invoke(null, key, val);
        } catch (Throwable ignored) {}
    }

    public static int getCurrentLevel() {
        return currentLevel;
    }

    public static void throttleDown() {
        if (!PatchConfig.adaptiveBudgetEnabled) return;
        if (currentLevel < LEVEL_HEAVY) {
            currentLevel++;
            applyBudgetsForLevel(currentLevel);
            PatchLogger.debug("Adaptive throttling activated: Level " + currentLevel);
        }
    }

    public static void recoverUp() {
        if (!PatchConfig.adaptiveBudgetEnabled) return;
        if (currentLevel > LEVEL_NOMINAL) {
            currentLevel--;
            applyBudgetsForLevel(currentLevel);
            PatchLogger.debug("Performance stabilized. Relaxing budgets: Level " + currentLevel);
        }
    }

    private static void applyBudgetsForLevel(int level) {
        HardwareProfile profile = ProfileManager.getActiveProfile();
        if (profile == null) return;

        float bakeMult = 1.0f;
        float uploadMult = 1.0f;
        int farDelta = 0;

        switch (level) {
            case LEVEL_LIGHT:
                bakeMult = 0.75f;
                uploadMult = 0.75f;
                farDelta = -2;
                break;
            case LEVEL_MODERATE:
                bakeMult = 0.5f;
                uploadMult = 0.5f;
                farDelta = -4;
                break;
            case LEVEL_HEAVY:
                bakeMult = 0.35f;
                uploadMult = 0.35f;
                farDelta = -8;
                break;
            case LEVEL_NOMINAL:
            default:
                bakeMult = 1.0f;
                uploadMult = 1.0f;
                farDelta = 0;
                break;
        }

        // Apply dynamically safe per-frame budgets (does not trigger memory reallocations)
        float bakeMs = Math.max(0.5f, profile.bakeBudgetMs * bakeMult);
        float cellMb = Math.max(0.5f, profile.cellUploadMb * uploadMult);
        float shellMb = Math.max(1.0f, profile.shellUploadMb * uploadMult);

        setLiveSetting("floors.bakeBudgetMs", String.format(java.util.Locale.ROOT, "%.2f", bakeMs));
        setLiveSetting("far.cellUploadMb", String.format(java.util.Locale.ROOT, "%.2f", cellMb));
        setLiveSetting("far.shellUploadMb", String.format(java.util.Locale.ROOT, "%.2f", shellMb));

        if (PatchConfig.dynamicLodEnabled) {
            int dynamicFar = Math.max(8, profile.defaultFarBlocks + farDelta);
            setLiveSetting("lod.farBlocks", String.valueOf(dynamicFar));
        }
    }
}

