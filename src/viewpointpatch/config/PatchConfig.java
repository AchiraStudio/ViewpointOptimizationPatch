package viewpointpatch.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import viewpointpatch.diagnostics.PatchLogger;

public final class PatchConfig {
    public static volatile boolean patchEnabled = true;
    public static volatile boolean safeMode = false;
    public static volatile boolean retirementEnabled = true;
    public static volatile boolean floorFilterEnabled = true;
    public static volatile boolean adaptiveBudgetEnabled = true;
    public static volatile boolean dynamicLodEnabled = true;
    public static volatile double targetFrameMs = 16.67; // 60 FPS default

    private static volatile boolean loaded = false;

    private PatchConfig() {}

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;

        try {
            File zomboidDir = new File(System.getProperty("user.home"), "Zomboid");
            File propFile = new File(zomboidDir, "viewpoint-live.properties");
            if (propFile.isFile()) {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(propFile)) {
                    props.load(in);
                }
                parseProperties(props);
            }
        } catch (Throwable t) {
            PatchLogger.warn("Could not read patch configuration from properties: " + t.getMessage());
        }

        if (safeMode) {
            SafeMode.triggerGlobalSafeMode("Configuration patch.safeMode=true");
        }
        if (!retirementEnabled) {
            SafeMode.disableRetirement("Configuration optimization.retirement=false");
        }
        if (!floorFilterEnabled) {
            SafeMode.floorOptimizationDisabled = true;
        }

        PatchLogger.info("Config loaded: SafeMode=" + safeMode + ", AdaptiveBudget=" + adaptiveBudgetEnabled + ", DynamicLod=" + dynamicLodEnabled);
    }

    private static void parseProperties(Properties props) {
        String val;
        if ((val = props.getProperty("patch.enabled")) != null) {
            patchEnabled = Boolean.parseBoolean(val.trim());
        }
        if ((val = props.getProperty("patch.safeMode")) != null) {
            safeMode = Boolean.parseBoolean(val.trim());
        }
        if ((val = props.getProperty("optimization.retirement")) != null) {
            retirementEnabled = Boolean.parseBoolean(val.trim());
        }
        if ((val = props.getProperty("optimization.floorFilter")) != null) {
            floorFilterEnabled = Boolean.parseBoolean(val.trim());
        }
        if ((val = props.getProperty("optimization.adaptiveBudget")) != null) {
            adaptiveBudgetEnabled = Boolean.parseBoolean(val.trim());
        }
        if ((val = props.getProperty("optimization.dynamicLod")) != null) {
            dynamicLodEnabled = Boolean.parseBoolean(val.trim());
        }
        if ((val = props.getProperty("patch.targetFps")) != null) {
            try {
                double fps = Double.parseDouble(val.trim());
                if (fps > 10.0 && fps <= 360.0) {
                    targetFrameMs = 1000.0 / fps;
                }
            } catch (NumberFormatException ignored) {}
        }
    }
}
