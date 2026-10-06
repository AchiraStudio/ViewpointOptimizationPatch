/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import viewpoint.platform.GraphicsPresets;
import viewpoint.platform.Hardware;
import viewpoint.platform.LoadModel;
import viewpoint.platform.PackOption;
import viewpoint.platform.PackPreset;
import viewpoint.platform.ShaderPack;
import viewpoint.platform.ShaderPacks;

final class PresetAdvice {
    static final int POTATO = 0;
    static final int LOW = 1;
    static final int DEFAULT = 2;
    static final int HIGH = 3;
    static final int ULTRA = 4;
    private static final double LOW_FPS = 45.0;
    private static final double FPS = 60.0;
    private static final double VRAM_ROOM = 0.9;
    private static final long[] RAM_NEEDS = new long[]{5500L, 7000L, 14000L, 28000L};
    private static final long[] HEAP_NEEDS = new long[]{1800L, 2800L, 2800L, 3900L};
    private static final int[] THREAD_NEEDS = new int[]{2, 4, 8, 12};
    private static final int THREADS_PER_WORKER = 4;
    private static final int MAX_WORKERS = 4;

    static Advice of(Hardware hardware) {
        int n;
        ArrayList<String> arrayList = new ArrayList<String>();
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        LoadModel.Load load = LoadModel.budget(hardware);
        for (n = 1; n <= 4; ++n) {
            LoadModel.Values values = PresetAdvice.values(n);
            LoadModel.Load load2 = LoadModel.estimate(values, hardware);
            n2 = load2.gpuMs() <= 1000.0 / (n == 1 ? 45.0 : 60.0) ? n : n2;
            n3 = load2.vramMiB() <= load.vramMiB() * 0.9 ? n : n3;
            boolean bl = LoadModel.stress(load2, load, LoadModel.Part.CPU, values, hardware) <= 1.0 && LoadModel.stress(load2, load, LoadModel.Part.MEMORY, values, hardware) <= 1.0;
            n4 = bl ? n : n4;
        }
        n = PresetAdvice.needed(hardware.ramMiB(), RAM_NEEDS);
        int n5 = PresetAdvice.needed(hardware.heapMiB(), HEAP_NEEDS);
        int n6 = PresetAdvice.needed(hardware.threads(), THREAD_NEEDS);
        double d = LoadModel.score(hardware);
        arrayList.add(String.format(Locale.ROOT, "Graphics card (GPU): %s, %s at %d x %d: %s", hardware.renderer().replaceAll("/.*", ""), Double.isNaN(hardware.gpuScore()) ? "not one we know, judged by its memory" : "about " + Math.round(d * 100.0) + "% of an RTX 3060", hardware.width(), hardware.height(), PresetAdvice.allows(n2)));
        arrayList.add(String.format(Locale.ROOT, "Video memory (VRAM): %s: %s", hardware.vramMiB() > 0 ? PresetAdvice.gib(hardware.vramMiB()) + ", " + PresetAdvice.gib(hardware.vramFreeMiB()) + " free" : (hardware.vramFreeMiB() > 0 ? PresetAdvice.gib(hardware.vramFreeMiB()) + " free" : "not told by the driver"), PresetAdvice.allows(n3)));
        arrayList.add(String.format(Locale.ROOT, "Memory (RAM): %s, %s free; the game's heap %s: %s", PresetAdvice.gib(hardware.ramMiB()), PresetAdvice.gib(hardware.ramFreeMiB()), PresetAdvice.gib(hardware.heapMiB()), PresetAdvice.allows(Math.min(Math.min(n, n5), n4))));
        arrayList.add(String.format(Locale.ROOT, "Processor (CPU): %d threads: %s", hardware.threads(), PresetAdvice.allows(n6)));
        int n7 = Math.min(Math.min(n2, n3), Math.min(Math.min(n, n5), Math.min(n6, n4)));
        int n8 = Math.max(1, Math.min(4, hardware.threads() / 4));
        return new Advice(n7, List.copyOf(arrayList), n8);
    }

    static LoadModel.Values values(int n) {
        PackPreset packPreset;
        HashMap<Object, String> hashMap = new HashMap<Object, String>();
        for (Map.Entry<String, String[]> object2 : GraphicsPresets.TABLE.entrySet()) {
            hashMap.put(object2.getKey(), object2.getValue()[n]);
        }
        boolean bl = "Vanilla".equals(hashMap.get("graphics.mode"));
        ShaderPack shaderPack = ShaderPacks.find(bl ? "normal" : (String)hashMap.get("graphics.pack"));
        PackPreset packPreset2 = packPreset = shaderPack == null ? null : PresetAdvice.presetNamed(shaderPack, GraphicsPresets.NAMES[n]);
        if (packPreset != null) {
            for (PackOption packOption : shaderPack.options) {
                hashMap.put("pack." + shaderPack.id + "." + packOption.id, packPreset.valueFor(packOption));
            }
        }
        return hashMap::get;
    }

    static PackPreset presetNamed(ShaderPack shaderPack, String string) {
        for (PackPreset packPreset : shaderPack.presets) {
            if (!packPreset.id.equalsIgnoreCase(string) && !packPreset.label.equalsIgnoreCase(string)) continue;
            return packPreset;
        }
        return null;
    }

    private static int needed(long l, long[] lArray) {
        int n = 0;
        for (int i = 0; i < lArray.length; ++i) {
            n = l >= lArray[i] ? 1 + i : n;
        }
        return n;
    }

    private static int needed(int n, int[] nArray) {
        long[] lArray = new long[nArray.length];
        for (int i = 0; i < nArray.length; ++i) {
            lArray[i] = nArray[i];
        }
        return PresetAdvice.needed((long)n, lArray);
    }

    private static String allows(int n) {
        return n == 4 ? "any preset" : "up to " + GraphicsPresets.NAMES[n];
    }

    private static String gib(long l) {
        return String.format(Locale.ROOT, "%.1f GB", (double)l / 1024.0);
    }

    private PresetAdvice() {
    }

    record Advice(int preset, List<String> reasons, int farWorkers) {
    }
}

