package viewpointpatch;

import java.lang.reflect.Method;
import me.zed_0xff.zombie_buddy.Patch;
import org.lwjgl.opengl.GL11;
import viewpoint.platform.LiveSettings;

@Patch(className = "viewpoint.SceneDrawer", methodName = "drawFrame")
public class Patch_SceneDrawer {
    public static volatile boolean checked;
    public static Method putMethod;
    public static Method valueMethod;

    public static void setLiveSetting(String key, String val) {
        try {
            if (putMethod == null) {
                putMethod = LiveSettings.class.getDeclaredMethod("put", String.class, String.class);
                putMethod.setAccessible(true);
            }
            putMethod.invoke(null, key, val);
        } catch (Throwable t) {
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
            return null;
        }
    }

    @Patch.OnEnter
    public static void onEnter() {
        if (!checked) {
            checked = true;
            try {
                // Runs safely on the Render Thread inside SceneDrawer.drawFrame()
                String renderer = GL11.glGetString(7937); // GL_RENDERER
                String vendor = GL11.glGetString(7936);   // GL_VENDOR

                if (renderer != null) {
                    String norm = renderer.toUpperCase().replace("(R)", "").replace("(TM)", "");
                    boolean isIntelIntegrated = norm.contains("INTEL") && !norm.contains("ARC");
                    boolean isAmdIntegrated = norm.contains("RADEON") && !norm.contains("RX") && (norm.contains("GRAPHICS") || norm.contains("VEGA"));
                    boolean integrated = isIntelIntegrated || isAmdIntegrated;

                    System.out.println("[ViewpointOptimizationPatch] Render Thread GPU Probe: " + renderer + " (" + vendor + "), Integrated=" + integrated);

                    if (integrated) {
                        String currentPreset = getLiveSetting("graphics.preset");
                        if (currentPreset == null || "Default".equalsIgnoreCase(currentPreset.trim())) {
                            System.out.println("[ViewpointOptimizationPatch] Auto-tuning to Potato (Vanilla) preset for 60+ FPS iGPU stability.");
                            setLiveSetting("graphics.preset", "Potato");
                            setLiveSetting("graphics.mode", "Vanilla");
                            setLiveSetting("floors.filter", "Trilinear");
                            setLiveSetting("sprites.filter", "Trilinear");
                            setLiveSetting("models.filter", "Trilinear");
                            setLiveSetting("memory.shellMiB", "192");
                            setLiveSetting("memory.floorMiB", "128");
                            setLiveSetting("lod.farBlocks", "8");
                        }
                    } else {
                        // Dedicated GPU (e.g. RX 9060 XT)
                        String floorFilter = getLiveSetting("floors.filter");
                        if (floorFilter == null || "Lanczos".equalsIgnoreCase(floorFilter.trim())) {
                            System.out.println("[ViewpointOptimizationPatch] Dedicated GPU detected: Replacing heavy Lanczos floor filter with high-speed Trilinear.");
                            setLiveSetting("floors.filter", "Trilinear");
                        }
                    }
                }
            } catch (Throwable t) {
                // Silently ignore if anything fails
            }
        }
    }
}
