package viewpoint.platform;

import java.util.LinkedHashMap;
import java.util.Map;
import org.lwjgl.opengl.GL11;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.GpuScore;

public final class GraphicsPresets {
    static final String[] NAMES = new String[]{"Potato", "Low", "Default", "High", "Ultra", "Custom"};
    static final int DEFAULT = 2;
    static final int CUSTOM = 5;
    static final Map<String, String[]> TABLE = new LinkedHashMap<String, String[]>();
    private static LiveSettings.Choice preset;
    private static int applied;

    private static void row(String string, String ... stringArray) {
        TABLE.put(string, stringArray);
    }

    static void start() {
        int defaultPreset = 2;
        try {
            String renderer = GL11.glGetString(7937);
            if (renderer != null && GpuScore.integrated(renderer)) {
                defaultPreset = 0;
                System.out.println("[Viewpoint] Integrated GPU detected (" + renderer + "): auto-selecting Potato (Vanilla) preset for smooth 60+ FPS performance.");
            }
        } catch (Throwable t) {
        }
        preset = LiveSettings.choice("graphics.preset", "Preset", "Graphics/Preset", NAMES, defaultPreset);
        preset.describe("Sets the mode and the settings that trade the world's detail for speed together; changing any of them makes it Custom. The shader pack's options have their own presets.");
    }

    static boolean follow() {
        boolean bl;
        int n = preset.get();
        if (n == 5) {
            applied = 5;
            return false;
        }
        boolean bl2 = applied < 0;
        boolean bl3 = bl = bl2 && !GraphicsPresets.matches(n);
        if (bl) {
            System.out.println("[Viewpoint] graphics preset " + NAMES[n] + ": its values changed, written again");
        }
        if (bl || !bl2 && n != applied) {
            applied = n;
            for (Map.Entry<String, String[]> entry : TABLE.entrySet()) {
                LiveSettings.put(entry.getKey(), entry.getValue()[n]);
            }
            return true;
        }
        applied = n;
        return GraphicsPresets.stray(n);
    }

    static int current() {
        return preset.get();
    }

    private static boolean stray(int n) {
        if (GraphicsPresets.matches(n)) {
            return false;
        }
        preset.set(5);
        applied = 5;
        return true;
    }

    static boolean matches(int n) {
        for (Map.Entry<String, String[]> entry : TABLE.entrySet()) {
            if (GraphicsPresets.holds(entry.getKey(), entry.getValue()[n], n)) continue;
            return false;
        }
        return true;
    }

    private static boolean holds(String string, String string2, int n) {
        LiveSettings.Setting setting = LiveSettings.find(string);
        if (setting != null) {
            return setting.matches(string2);
        }
        String string3 = LiveSettings.value(string);
        return string3 == null ? n == 2 : string3.trim().equalsIgnoreCase(string2) || GraphicsPresets.same(string3, string2);
    }

    private static boolean same(String string, String string2) {
        try {
            return Float.parseFloat(string.trim()) == Float.parseFloat(string2.trim());
        }
        catch (NumberFormatException numberFormatException) {
            return false;
        }
    }

    private GraphicsPresets() {
    }

    static {
        GraphicsPresets.row("graphics.mode", "Vanilla", "Modern", "Modern", "Modern", "Modern");
        GraphicsPresets.row("graphics.pack", "vivid", "vivid", "vivid", "vivid", "vivid");
        GraphicsPresets.row("lod.farBlocks", "8", "16", "24", "32", "48");
        GraphicsPresets.row("lod.boxesFineBlocks", "2", "3", "3", "6", "8");
        GraphicsPresets.row("lod.boxesMidBlocks", "4", "6", "6", "12", "16");
        GraphicsPresets.row("lod.boxesCoarseBlocks", "8", "12", "12", "24", "32");
        GraphicsPresets.row("lod.shellFullBlocks", "1", "2", "3", "6", "8");
        GraphicsPresets.row("lod.shellLiteBlocks", "4", "6", "10", "16", "24");
        GraphicsPresets.row("lod.treeShare", "0.25", "0.375", "0.5", "0.75", "1.0");
        GraphicsPresets.row("lod.treesFadeBlocks", "6", "8", "12", "20", "24");
        GraphicsPresets.row("lod.treesGoneBlocks", "12", "16", "36", "56", "64");
        GraphicsPresets.row("lod.interiorFurniture", "false", "false", "false", "false", "true");
        GraphicsPresets.row("lod.groundTexelsFull", "4", "4", "8", "8", "16");
        GraphicsPresets.row("lod.groundTexelsLite", "0", "1", "2", "2", "4");
        GraphicsPresets.row("lod.fullRateCharacters", "8", "16", "32", "64", "128");
        GraphicsPresets.row("lod.packRing", "0", "3", "6", "8", "10");
        GraphicsPresets.row("floors.texelsPerTile", "32", "32", "48", "64", "64");
        GraphicsPresets.row("floors.detailChunks", "4", "6", "8", "8", "10");
        GraphicsPresets.row("floors.filter", "Trilinear", "Trilinear", "Trilinear", "Trilinear", "Trilinear");
        GraphicsPresets.row("sprites.filter", "Trilinear", "Anisotropic", "Nearest, mipmapped", "Anisotropic", "Anisotropic");
        GraphicsPresets.row("models.filter", "Trilinear", "Anisotropic", "Nearest, mipmapped", "Anisotropic", "Anisotropic");
        GraphicsPresets.row("far.shellUploadMb", "2.0", "3.0", "4.0", "6.0", "8.0");
        GraphicsPresets.row("far.cellUploadMb", "1.0", "1.5", "2.0", "3.0", "4.0");
        GraphicsPresets.row("memory.nearMeshMiB", "384", "384", "512", "640", "768");
        GraphicsPresets.row("memory.lightAtlasMiB", "48", "48", "64", "96", "128");
        GraphicsPresets.row("memory.shellMiB", "192", "384", "640", "1536", "2048");
        GraphicsPresets.row("memory.farMiB", "64", "96", "160", "256", "384");
        GraphicsPresets.row("memory.modelMeshMiB", "160", "192", "256", "384", "512");
        GraphicsPresets.row("memory.cpuCacheMiB", "160", "192", "256", "384", "512");
        GraphicsPresets.row("memory.floorMiB", "128", "256", "384", "768", "1024");
        GraphicsPresets.row("memory.packMiB", "64", "128", "512", "768", "1024");
        GraphicsPresets.row("memory.minecraftMiB", "2048", "3072", "4096", "6144", "8192");
        GraphicsPresets.row("memory.corpseMiB", "256", "256", "256", "512", "1024");
        GraphicsPresets.row("corpses.models", "false", "false", "true", "true", "true");
        GraphicsPresets.row("corpses.limit", "40", "40", "40", "40", "40");
        GraphicsPresets.row("corpses.most", "256", "256", "256", "512", "512");
        GraphicsPresets.row("corpses.every", "false", "false", "false", "false", "true");
        GraphicsPresets.row("corpses.inView", "true", "true", "true", "true", "false");
        applied = -1;
    }
}
