package viewpointpatch.gpu;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import viewpointpatch.diagnostics.PatchLogger;

public final class GpuProfiler {
    private static volatile GpuCapabilities cachedCapabilities = null;

    // Regex patterns for device classification
    private static final Pattern NVIDIA_PATTERN = Pattern.compile("\\b(RTX|GTX|GT|MX)\\s*(\\d{3,4})\\s*(TI|SUPER)?\\b");
    private static final Pattern AMD_RX_PATTERN = Pattern.compile("\\bRX\\s*(\\d{3,4})\\s*(XTX|XT|GRE)?\\b");
    private static final Pattern AMD_APU_PATTERN = Pattern.compile("\\b(6[68]0M|7[468]0M|8[89]0M|VEGA\\s*\\d+)\\b");
    private static final Pattern INTEL_ARC_DISCRETE = Pattern.compile("\\bARC\\s*([AB]\\d{3})\\b");

    private GpuProfiler() {}

    public static synchronized GpuCapabilities detect() {
        if (cachedCapabilities != null) {
            return cachedCapabilities;
        }

        String rawRenderer = null;
        String rawVendor = null;
        String rawVersion = null;
        int maxTex = 2048;
        boolean syncSupported = false;
        int vramMb = -1;

        try {
            rawRenderer = GL11.glGetString(GL11.GL_RENDERER); // 7937
            rawVendor = GL11.glGetString(GL11.GL_VENDOR);     // 7936
            rawVersion = GL11.glGetString(GL11.GL_VERSION);   // 7938
            maxTex = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        } catch (Throwable t) {
            PatchLogger.warn("Failed basic OpenGL context string queries: " + t.getMessage(), t);
        }

        // Test sync object availability safely
        try {
            // GL32.glFenceSync check
            syncSupported = checkSyncSupport(rawVersion);
        } catch (Throwable t) {
            syncSupported = false;
        }

        GpuVendor vendor = GpuVendor.fromStrings(rawVendor, rawRenderer);
        vramMb = probeVramMb(vendor);

        boolean isIntegrated = determineIntegrated(vendor, rawRenderer);
        GpuTier tier = determineTier(vendor, rawRenderer, isIntegrated, vramMb);

        cachedCapabilities = new GpuCapabilities(
            vendor, tier, rawVendor, rawRenderer, rawVersion, maxTex, syncSupported, vramMb, isIntegrated
        );

        PatchLogger.info("--------------------------------------------------");
        PatchLogger.info("Active GPU Probe Completed:");
        PatchLogger.info("  Vendor:   " + cachedCapabilities.vendor + " (" + cachedCapabilities.rawVendor + ")");
        PatchLogger.info("  Renderer: " + cachedCapabilities.rawRenderer);
        PatchLogger.info("  OpenGL:   " + cachedCapabilities.rawVersion);
        PatchLogger.info("  Tier:     " + cachedCapabilities.tier.name() + " (" + cachedCapabilities.tier.getDescription() + ")");
        PatchLogger.info("  Sync:     " + (cachedCapabilities.syncSupported ? "Supported (GL32 Fence)" : "UNSUPPORTED"));
        if (cachedCapabilities.vramMb > 0) {
            PatchLogger.info("  VRAM:     ~" + cachedCapabilities.vramMb + " MB");
        }
        PatchLogger.info("--------------------------------------------------");

        return cachedCapabilities;
    }

    private static boolean checkSyncSupport(String glVersion) {
        if (glVersion == null) return false;
        try {
            // Match major.minor
            String[] parts = glVersion.trim().split(" ")[0].split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                return (major > 3) || (major == 3 && minor >= 2);
            }
        } catch (Throwable ignored) {}
        return true; // Default assume modern OpenGL on PZ B42
    }

    private static int probeVramMb(GpuVendor vendor) {
        try {
            if (vendor == GpuVendor.NVIDIA) {
                // GL_NVX_gpu_memory_info: GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX = 0x9048 (in KB)
                int kb = GL11.glGetInteger(0x9048);
                if (kb > 0) return kb / 1024;
            } else if (vendor == GpuVendor.AMD) {
                // GL_ATI_meminfo: VBO_FREE_MEMORY_ATI = 0x87FC (in KB)
                int kb = GL11.glGetInteger(0x87FC);
                if (kb > 0) return kb / 1024;
            }
        } catch (Throwable ignored) {
            // Extension not supported or query unavailable
        }
        return -1;
    }

    private static boolean determineIntegrated(GpuVendor vendor, String renderer) {
        if (renderer == null) return false;
        String norm = renderer.toUpperCase(Locale.ROOT);

        if (vendor == GpuVendor.INTEL) {
            // Discrete Arc cards are typically Arc A580, A750, A770, B580
            Matcher m = INTEL_ARC_DISCRETE.matcher(norm);
            if (m.find()) {
                String model = m.group(1);
                // A580, A750, A770, B570, B580 are dedicated cards
                if (model.startsWith("A5") || model.startsWith("A7") || model.startsWith("B5")) {
                    return false;
                }
            }
            return true; // UHD, HD, Iris Xe, Core Ultra Arc iGPU
        }

        if (vendor == GpuVendor.AMD) {
            if (AMD_APU_PATTERN.matcher(norm).find()) return true;
            if (norm.contains("VEGA") && !norm.contains("VEGA 56") && !norm.contains("VEGA 64")) return true;
            if (norm.contains("GRAPHICS") && !norm.contains("RX")) return true;
            return false;
        }

        return false;
    }

    private static GpuTier determineTier(GpuVendor vendor, String renderer, boolean isIntegrated, int vramMb) {
        if (renderer == null || vendor == GpuVendor.UNKNOWN || vendor == GpuVendor.SOFTWARE) {
            return GpuTier.SAFE;
        }

        String norm = renderer.toUpperCase(Locale.ROOT);

        if (vendor == GpuVendor.INTEL) {
            if (!isIntegrated) {
                return GpuTier.MAINSTREAM; // Discrete Intel Arc A580/A750/A770/B580
            }
            if (norm.contains("IRIS XE") || norm.contains("ARC") || norm.contains("ULTRA")) {
                return GpuTier.INTEGRATED;
            }
            return GpuTier.LOW_POWER; // Intel HD / UHD older graphics
        }

        if (vendor == GpuVendor.AMD) {
            if (isIntegrated) {
                if (norm.contains("680M") || norm.contains("780M") || norm.contains("880M") || norm.contains("890M")) {
                    return GpuTier.INTEGRATED;
                }
                return GpuTier.LOW_POWER;
            }

            Matcher rx = AMD_RX_PATTERN.matcher(norm);
            if (rx.find()) {
                try {
                    int model = Integer.parseInt(rx.group(1));
                    if (model >= 9060 || model >= 7800 || (model >= 6800 && model <= 6950)) {
                        return GpuTier.HIGH_END;
                    }
                    if (model >= 5500 || model == 580 || model == 590 || model >= 6600 || model >= 7600) {
                        return GpuTier.MAINSTREAM;
                    }
                    return GpuTier.LOW_POWER; // RX 550, 6400, etc.
                } catch (NumberFormatException ignored) {}
            }
            return GpuTier.MAINSTREAM;
        }

        if (vendor == GpuVendor.NVIDIA) {
            Matcher nv = NVIDIA_PATTERN.matcher(norm);
            if (nv.find()) {
                String prefix = nv.group(1);
                try {
                    int model = Integer.parseInt(nv.group(2));
                    if ("GT".equals(prefix) || "MX".equals(prefix) || model <= 750) {
                        return GpuTier.LOW_POWER;
                    }
                    // RTX 3070+, 4070+, 50-series are High-End
                    if ("RTX".equals(prefix) && ((model >= 3070 && model < 4000) || model >= 4070)) {
                        return GpuTier.HIGH_END;
                    }
                    return GpuTier.MAINSTREAM;
                } catch (NumberFormatException ignored) {}
            }
            return GpuTier.MAINSTREAM;
        }

        return GpuTier.SAFE;
    }
}

