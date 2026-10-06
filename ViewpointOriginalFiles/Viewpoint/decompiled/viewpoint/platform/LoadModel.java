/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import viewpoint.platform.Hardware;

final class LoadModel {
    static final double FRAME_MS = 16.666666666666668;
    private static final double GEOMETRY_MS = 2.3;
    private static final double SCREEN_MS = 1.25;
    private static final double VANILLA_SCREEN_MS = 0.45;
    private static final double SHELL_MS = 0.4;
    private static final double BOXES_MS = 0.1;
    private static final double TREES_MS = 0.2;
    private static final double CASCADE_MS = 0.25;
    private static final double FAR_MAPS_MS = 0.1;
    private static final double LAMP_MS = 0.05;
    private static final double FLASHLIGHT_MS = 0.05;
    private static final double AIR_MS = 0.35;
    private static final double AIR_STEP_MS = 0.035;
    private static final double CLOUD_MS = 0.05;
    private static final double CLOUD_BODY_MS = 0.2;
    private static final double TAA_MS = 0.4;
    private static final double BLUR_MS = 0.12;
    private static final double BLUR_TAP_MS = 0.02;
    private static final double DOF_MS = 0.15;
    private static final double DOF_TAP_MS = 0.015;
    private static final double FLARE_MS = 0.08;
    private static final double BOUNCE_MS = 0.7;
    private static final double NO_GODRAYS = 0.7;
    private static final double REF_SHELL = 12.0;
    private static final double REF_FAR = 32.0;
    private static final double REF_TREES = 48.0;
    private static final double REF_SHARE = 0.5;
    private static final double REF_CASCADE = 2048.0;
    private static final double REF_LAMP = 512.0;
    private static final double REF_CLOUD_RES = 50.0;
    private static final double REF_CLOUD_STEPS = 40.0;
    private static final double REF_FLOOR_CHUNKS = 8.0;
    private static final double REF_FLOOR_TEXELS = 48.0;
    private static final double TARGETS_MIB = 220.0;
    private static final double VANILLA_TARGETS_MIB = 90.0;
    private static final double NEAR_MIB = 250.0;
    private static final double LIGHT_MIB = 20.0;
    private static final double SHELL_MIB = 500.0;
    private static final double FAR_CELLS_MIB = 200.0;
    private static final double FLOORS_MIB = 320.0;
    private static final double MODELS_MIB = 40.0;
    private static final double FAR_MAPS_MIB = 32.0;
    private static final double MIB = 1048576.0;
    private static final double[] CASCADE_LAYERS = new double[]{3.56, 7.12, 8.12};
    private static final double NATIVE_MIB = 150.0;
    private static final double FAR_DATA_MIB = 150.0;
    private static final double LIGHT_HEAP_MIB = 100.0;
    private static final double CACHE_MIB = 95.0;
    private static final double MIN_REACH = 0.3;
    private static final double GAME_HEAP_MIB = 1800.0;
    private static final double MIN_HEAP_MIB = 256.0;
    private static final double RENDER_MS = 1.5;
    private static final double GBUFFER_CPU_MS = 1.8;
    private static final double FAR_CPU_MS = 0.7;
    private static final double FLOORS_CPU_MS = 0.2;
    private static final double SHADOW_CPU_MS = 0.9;
    private static final double FAR_SHADOW_CPU_MS = 0.15;
    private static final double LAMP_CPU_MS = 0.1;
    private static final double MAIN_MS = 0.8;
    private static final double MAIN_FAR_MS = 0.35;
    private static final double CHARACTERS_MS = 0.2;
    private static final double PACKS_CPU_MS = 0.2;
    private static final double GAME_MAIN_MS = 6.0;
    private static final double GAME_RENDER_MS = 2.0;
    private static final double REF_CHARACTERS = 32.0;
    private static final double REF_RING = 6.0;

    static Load estimate(Values values, Hardware hardware) {
        Settings settings = new Settings(values);
        double d = LoadModel.vram(settings, hardware);
        return new Load(LoadModel.cpu(settings, hardware), LoadModel.ram(settings, hardware, d), LoadModel.gpu(settings, hardware), d);
    }

    static Load budget(Hardware hardware) {
        double d = LoadModel.slower(hardware.threads());
        double d2 = Math.max(16.666666666666668 - 2.0 * d, 1.0);
        double d3 = hardware.vramFreeMiB() > 0 ? (double)hardware.vramFreeMiB() : (hardware.vramMiB() > 0 ? (double)hardware.vramMiB() * 0.6 : (hardware.integrated() ? 1024.0 : 2048.0));
        return new Load(d2, Math.max((double)hardware.ramFreeMiB(), 256.0), 16.666666666666668, d3);
    }

    static double stress(Load load, Load load2, Part part, Values values, Hardware hardware) {
        double d = load.of(part) / Math.max(load2.of(part), 0.001);
        if (part == Part.MEMORY) {
            double d2 = Math.max((double)hardware.heapMiB() - 1800.0, 256.0);
            return Math.max(d, LoadModel.heap(new Settings(values)) / d2);
        }
        if (part == Part.CPU) {
            double d3 = LoadModel.slower(hardware.threads());
            double d4 = Math.max(16.666666666666668 - 6.0 * d3, 1.0);
            return Math.max(d, LoadModel.main(new Settings(values)) * d3 / d4);
        }
        return d;
    }

    static double score(Hardware hardware) {
        double d = hardware.gpuScore();
        if (!Double.isNaN(d)) {
            return d;
        }
        int n = hardware.vramMiB();
        return hardware.integrated() ? 0.1 : (n >= 8000 ? 1.0 : (n >= 6000 ? 0.8 : (n >= 3500 ? 0.45 : 0.3)));
    }

    static double slower(int n) {
        return n >= 12 ? 1.0 : (n >= 8 ? 1.15 : (n >= 6 ? 1.3 : (n >= 4 ? 1.6 : 2.2)));
    }

    private static double gpu(Settings settings, Hardware hardware) {
        double d = hardware.pixels();
        double d2 = 2.3 * (0.5 + 0.5 * d) + LoadModel.far(settings);
        if (!settings.modern()) {
            return (d2 + (0.45 + (settings.on("antialiasing", true) ? 0.4 : 0.0)) * d) / LoadModel.score(hardware);
        }
        double d3 = 1.25 + LoadModel.air(settings) + LoadModel.clouds(settings) + LoadModel.effects(settings);
        return (d2 + LoadModel.shadows(settings) + d3 * d) / LoadModel.score(hardware);
    }

    private static double far(Settings settings) {
        double d = LoadModel.square(settings.num("lod.shellLiteBlocks", 10.0) / 12.0);
        double d2 = LoadModel.square(settings.num("lod.farBlocks", 24.0) / 32.0);
        double d3 = settings.num("lod.treeShare", 0.5) / 0.5 * settings.num("lod.treesGoneBlocks", 36.0) / 48.0;
        return 0.4 * d + 0.1 * d2 + 0.2 * d3;
    }

    private static double shadows(Settings settings) {
        double d = settings.on("sunShadows", true) ? 0.25 * settings.num("shadowCascades", 3.0) * Math.pow(settings.num("shadowResolution", 2048.0) / 2048.0, 1.5) : 0.0;
        double d2 = settings.num("lampShadows", 2.0) * 0.05 * LoadModel.square(settings.num("lampShadowResolution", 512.0) / 512.0);
        return d + d2 + (settings.on("farShadows", true) ? 0.1 : 0.0) + (settings.on("flashlightShadow", true) ? 0.05 : 0.0);
    }

    private static double air(Settings settings) {
        if (!settings.on("volumetrics", true)) {
            return 0.0;
        }
        double d = 0.35 + 0.035 * settings.num("volumetricSteps", 16.0);
        return settings.on("godrays", true) ? d : d * 0.7;
    }

    private static double clouds(Settings settings) {
        if (!settings.on("clouds", true)) {
            return 0.0;
        }
        double d = LoadModel.square(settings.num("cloudResolution", 50.0) / 50.0);
        return 0.05 + 0.2 * d * settings.num("cloudSteps", 30.0) / 40.0;
    }

    private static double effects(Settings settings) {
        double d = settings.on("antialiasing", true) ? 0.4 : 0.0;
        double d2 = settings.on("motionBlur", true) ? 0.12 + 0.02 * settings.num("motionBlurSamples", 6.0) : 0.0;
        double d3 = settings.on("depthOfField", false) ? 0.15 + 0.015 * settings.num("dofSamples", 24.0) : 0.0;
        return d + d2 + d3 + (settings.on("lensFlare", true) ? 0.08 : 0.0) + (settings.on("bounceLight", false) ? 0.7 : 0.0);
    }

    private static double vram(Settings settings, Hardware hardware) {
        double d = LoadModel.square(settings.num("floors.detailChunks", 8.0) / 8.0) * LoadModel.square(settings.num("floors.texelsPerTile", 48.0) / 48.0);
        double d2 = LoadModel.buffers(settings) + Math.min(settings.num("memory.lightAtlasMiB", 64.0), 20.0) + Math.min(settings.num("memory.floorMiB", 384.0), 320.0 * d);
        double d3 = (settings.modern() ? 220.0 : 90.0) * hardware.pixels();
        return d3 + d2 + (settings.modern() ? LoadModel.shadowMemory(settings) : 0.0);
    }

    private static double buffers(Settings settings) {
        double d = LoadModel.square(settings.num("lod.farBlocks", 24.0) / 32.0);
        double d2 = LoadModel.square(settings.num("lod.shellLiteBlocks", 10.0) / 12.0);
        return Math.min(settings.num("memory.nearMeshMiB", 512.0), 250.0) + Math.min(settings.num("memory.shellMiB", 640.0), 500.0 * d2) + Math.min(settings.num("memory.farMiB", 160.0), 200.0 * d) + Math.min(settings.num("memory.modelMeshMiB", 256.0), 40.0);
    }

    private static double shadowMemory(Settings settings) {
        double d = 0.0;
        if (settings.on("sunShadows", true)) {
            int n = (int)Math.max(1.0, Math.min(3.0, settings.num("shadowCascades", 3.0)));
            d = LoadModel.square(settings.num("shadowResolution", 2048.0)) * 4.0 * CASCADE_LAYERS[n - 1] / 1048576.0;
        }
        double d2 = settings.num("lampShadows", 2.0) * 12.0 * LoadModel.square(settings.num("lampShadowResolution", 512.0)) * 4.0 / 1048576.0;
        return d + d2 + (settings.on("farShadows", true) ? 32.0 : 0.0);
    }

    private static double ram(Settings settings, Hardware hardware, double d) {
        double d2 = Math.max(0.0, d - LoadModel.budget(hardware).vramMiB());
        return LoadModel.buffers(settings) + 150.0 + d2;
    }

    private static double heap(Settings settings) {
        double d = LoadModel.square(settings.num("lod.farBlocks", 24.0) / 32.0);
        return Math.min(settings.num("memory.cpuCacheMiB", 256.0), 95.0 * Math.max(d, 0.3)) + 150.0 * d + 100.0;
    }

    private static double cpu(Settings settings, Hardware hardware) {
        double d = LoadModel.square(settings.num("lod.farBlocks", 24.0) / 32.0);
        double d2 = LoadModel.square(settings.num("lod.shellLiteBlocks", 10.0) / 12.0);
        double d3 = 3.5 + 0.7 * (d + d2) / 2.0;
        if (settings.modern() && settings.on("sunShadows", true)) {
            d3 += 0.9 * settings.num("shadowCascades", 3.0) / 3.0 + (settings.on("farShadows", true) ? 0.15 : 0.0);
        }
        if (settings.modern()) {
            d3 += 0.1 * settings.num("lampShadows", 2.0);
        }
        return d3 * LoadModel.slower(hardware.threads());
    }

    private static double main(Settings settings) {
        double d = LoadModel.square(settings.num("lod.farBlocks", 24.0) / 32.0);
        return 0.8 + 0.35 * d + 0.2 * settings.num("lod.fullRateCharacters", 32.0) / 32.0 + 0.2 * LoadModel.square(settings.num("lod.packRing", 6.0) / 6.0);
    }

    private static double square(double d) {
        return d * d;
    }

    private LoadModel() {
    }

    private record Settings(Values values) {
        boolean modern() {
            return !"Vanilla".equalsIgnoreCase(this.text("graphics.mode"));
        }

        double num(String string, double d) {
            try {
                String string2 = this.text(string.indexOf(46) < 0 ? this.option(string) : string);
                return string2 == null ? d : Double.parseDouble(string2.trim());
            }
            catch (NumberFormatException numberFormatException) {
                return d;
            }
        }

        boolean on(String string, boolean bl) {
            String string2 = this.text(this.option(string));
            return string2 == null ? bl : Boolean.parseBoolean(string2.trim());
        }

        private String option(String string) {
            String string2 = this.modern() ? this.text("graphics.pack") : "normal";
            return "pack." + (string2 == null ? "vivid" : string2) + "." + string;
        }

        private String text(String string) {
            return this.values.get(string);
        }
    }

    static interface Values {
        public String get(String var1);
    }

    record Load(double cpuMs, double ramMiB, double gpuMs, double vramMiB) {
        double of(Part part) {
            return switch (part.ordinal()) {
                default -> throw new IncompatibleClassChangeError();
                case 0 -> this.cpuMs;
                case 1 -> this.ramMiB;
                case 2 -> this.gpuMs;
                case 3 -> this.vramMiB;
            };
        }
    }

    static enum Part {
        CPU("Processor (CPU)"),
        MEMORY("Memory (RAM)"),
        GPU("Graphics card (GPU)"),
        VIDEO_MEMORY("Video memory (VRAM)");

        final String label;

        private Part(String string2) {
            this.label = string2;
        }
    }
}

