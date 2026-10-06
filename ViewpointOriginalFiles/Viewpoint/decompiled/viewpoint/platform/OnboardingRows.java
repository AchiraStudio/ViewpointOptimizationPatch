/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import viewpoint.platform.Graphics;
import viewpoint.platform.GraphicsPresets;
import viewpoint.platform.LoadModel;
import viewpoint.platform.PackOption;
import viewpoint.platform.PackPreset;
import viewpoint.platform.PresetAdvice;
import viewpoint.platform.ShaderPack;
import viewpoint.platform.ShaderPacks;

final class OnboardingRows {
    private static final String[] QUALITY = new String[]{"Lowest", "Low", "Medium", "High", "Ultra"};
    private static final String[] FEATURE = new String[]{"Off", "Low", "Medium", "High", "Ultra"};
    private static final String[] CORPSES = new String[]{"Off", "Off", "Medium", "High", "Ultra"};
    private static final String[] CORPSE_KEYS = new String[]{"corpses.models", "corpses.limit", "corpses.most", "corpses.every", "corpses.inView", "memory.corpseMiB"};
    private static final Set<LoadModel.Part> ALL = EnumSet.allOf(LoadModel.Part.class);
    private static final Set<LoadModel.Part> GPU = EnumSet.of(LoadModel.Part.GPU);
    private static final Set<LoadModel.Part> DRAWN = EnumSet.of(LoadModel.Part.GPU, LoadModel.Part.VIDEO_MEMORY, LoadModel.Part.CPU);
    private static final String[] SHADOWS = new String[]{"sunShadows", "shadowResolution", "shadowCascades", "farShadows", "lampShadows", "lampShadowResolution", "flashlightShadow", "shadowSamples"};
    private static final String SHADOWS_TEXT = "Sun, moon, lamp and flashlight shadows: how sharp, how far, and how many lamps cast them.";
    private static final String AIR_TEXT = "Light in the air: haze, lamp halos, the flashlight's beam and the sun's shafts.";
    private static final String CLOUDS_TEXT = "Volumetric clouds, and how finely they are drawn.";
    private static final String[][] TOGGLES = new String[][]{{"antialiasing", "Anti-aliasing (TAA)", "Smooths jagged edges and shimmer by blending each frame with the last."}, {"motionBlur", "Motion blur", "Blurs the picture as you turn and as things move."}, {"depthOfField", "Depth of field", "Blurs what lies well behind the surface you look at, as a camera's lens does."}, {"lensFlare", "Lens flare", "Glare, a halo and ghosts where the sun or a bright light shines into the view."}, {"bloom", "Bloom", "Bright lights glow over what is round them."}, {"bounceLight", "Bounce light", "Light bounced off what is on screen, and shade in corners. Costly."}};
    private static final double BLOCK_KM = 0.064;

    static List<Row> all(ShaderPack shaderPack, boolean bl) {
        ArrayList<Row> arrayList = new ArrayList<Row>();
        arrayList.add(new Row("Shader mode", "Modern draws the Vivid look: sun, lamp and flashlight shadows, clouds, light in the air, bloom and the effects below. Vanilla is a plain look close to the game's own, light on any graphics card.", List.of(OnboardingRows.level("Vanilla", "graphics.mode", "Vanilla"), OnboardingRows.level("Modern", "graphics.mode", "Modern")), DRAWN, false));
        arrayList.add(OnboardingRows.reach());
        arrayList.add(OnboardingRows.table("Far world detail", "How much of the distant world is drawn as real buildings rather than boxes, and how fine its ground is. More detail fills more video memory.", ALL, "lod.shellFullBlocks", "lod.shellLiteBlocks", "lod.boxesFineBlocks", "lod.boxesMidBlocks", "lod.boxesCoarseBlocks", "lod.groundTexelsFull", "lod.groundTexelsLite", "lod.interiorFurniture", "memory.shellMiB"));
        arrayList.add(OnboardingRows.table("Vegetation", "How many of the distant trees are drawn, and how far out they reach before they fade.", EnumSet.of(LoadModel.Part.GPU, LoadModel.Part.CPU), "lod.treeShare", "lod.treesFadeBlocks", "lod.treesGoneBlocks"));
        arrayList.add(OnboardingRows.table("Floor detail", "How sharp the baked floors and ground are, and how far round you the sharp ones reach.", EnumSet.of(LoadModel.Part.GPU, LoadModel.Part.VIDEO_MEMORY), "floors.texelsPerTile", "floors.detailChunks", "memory.floorMiB"));
        arrayList.add(OnboardingRows.corpses());
        OnboardingRows.packRows(arrayList, shaderPack, false);
        if (bl && ShaderPacks.find("vivid") != null) {
            OnboardingRows.packRows(arrayList, ShaderPacks.find("vivid"), true);
        }
        return arrayList;
    }

    private static void packRows(List<Row> list, ShaderPack shaderPack, boolean bl) {
        OnboardingRows.add(list, OnboardingRows.graded(shaderPack, "Shadows", SHADOWS_TEXT, DRAWN, SHADOWS), bl);
        OnboardingRows.add(list, OnboardingRows.graded(shaderPack, "Volumetric light", AIR_TEXT, GPU, "volumetrics", "godrays", "volumetricSteps"), bl);
        OnboardingRows.add(list, OnboardingRows.graded(shaderPack, "Clouds", CLOUDS_TEXT, GPU, "clouds", "cloudResolution", "cloudSteps"), bl);
        for (String[] stringArray : TOGGLES) {
            OnboardingRows.add(list, OnboardingRows.toggle(shaderPack, stringArray[0], stringArray[1], stringArray[2]), bl);
        }
    }

    private static void add(List<Row> list, Row row, boolean bl) {
        if (row == null || list.stream().anyMatch(row2 -> row2.label().equals(row.label()))) {
            return;
        }
        list.add(bl ? new Row(row.label(), row.description(), List.of(), row.parts(), true) : row);
    }

    private static Row reach() {
        ArrayList<Level> arrayList = new ArrayList<Level>();
        for (int i = 0; i < QUALITY.length; ++i) {
            Map<String, String> map = OnboardingRows.column(i, "lod.farBlocks", "memory.farMiB");
            double d = Double.parseDouble(map.get("lod.farBlocks")) * 0.064;
            OnboardingRows.distinct(arrayList, new Level(String.format(Locale.ROOT, "%.1f km", d), map));
        }
        return new Row("View distance", "How far past the loaded area the world is drawn, as simpler buildings, boxes and trees out to the fog. The costliest setting for the processor and memory.", arrayList, ALL, false);
    }

    private static Row corpses() {
        ArrayList<Level> arrayList = new ArrayList<Level>();
        for (int i = 0; i < CORPSES.length; ++i) {
            Map<String, String> map = OnboardingRows.column(i, CORPSE_KEYS);
            boolean bl = !Boolean.parseBoolean(map.get(CORPSE_KEYS[0]));
            OnboardingRows.distinct(arrayList, new Level(CORPSES[i], bl ? Map.of(CORPSE_KEYS[0], "false") : map));
        }
        return new Row("3D corpses", "The dead bodies within 40 squares as the game's own 3D models rather than flat pictures, as many as the level allows, with about a megabyte of memory for each. Ultra sets no count, and makes models of the bodies all round you, not only of those ahead.", arrayList, ALL, false);
    }

    private static Row table(String string, String string2, Set<LoadModel.Part> set, String ... stringArray) {
        ArrayList<Level> arrayList = new ArrayList<Level>();
        for (int i = 0; i < QUALITY.length; ++i) {
            OnboardingRows.distinct(arrayList, new Level(QUALITY[i], OnboardingRows.column(i, stringArray)));
        }
        return new Row(string, string2, arrayList, set, false);
    }

    private static Row graded(ShaderPack shaderPack, String string, String string2, Set<LoadModel.Part> set, String ... stringArray) {
        List<PackOption> list = OnboardingRows.options(shaderPack, stringArray);
        if (list.isEmpty()) {
            return null;
        }
        ArrayList<Level> arrayList = new ArrayList<Level>();
        for (int i = 0; i < FEATURE.length; ++i) {
            PackPreset packPreset = PresetAdvice.presetNamed(shaderPack, GraphicsPresets.NAMES[i]);
            if (packPreset == null) continue;
            LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
            list.forEach(packOption -> linkedHashMap.put(Graphics.key(shaderPack, packOption), packPreset.valueFor((PackOption)packOption)));
            OnboardingRows.distinct(arrayList, new Level(FEATURE[i], linkedHashMap));
        }
        return arrayList.size() < 2 ? null : new Row(string, string2, arrayList, set, false);
    }

    private static Row toggle(ShaderPack shaderPack, String string, String string2, String string3) {
        List<PackOption> list = OnboardingRows.options(shaderPack, string);
        if (list.isEmpty()) {
            return null;
        }
        String string4 = Graphics.key(shaderPack, list.get(0));
        return new Row(string2, string3, List.of(OnboardingRows.level("Off", string4, "false"), OnboardingRows.level("On", string4, "true")), GPU, false);
    }

    private static List<PackOption> options(ShaderPack shaderPack, String ... stringArray) {
        ArrayList<PackOption> arrayList = new ArrayList<PackOption>();
        for (String string : stringArray) {
            shaderPack.options.stream().filter(packOption -> packOption.id.equals(string)).findFirst().ifPresent(arrayList::add);
        }
        return arrayList;
    }

    private static Map<String, String> column(int n, String ... stringArray) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (String string : stringArray) {
            linkedHashMap.put(string, GraphicsPresets.TABLE.get(string)[n]);
        }
        return linkedHashMap;
    }

    private static void distinct(List<Level> list, Level level) {
        if (list.isEmpty() || !list.get(list.size() - 1).writes().equals(level.writes())) {
            list.add(level);
        }
    }

    private static Level level(String string, String string2, String string3) {
        return new Level(string, Map.of(string2, string3));
    }

    private static boolean holds(Level level, LoadModel.Values values) {
        for (Map.Entry<String, String> entry : level.writes().entrySet()) {
            if (OnboardingRows.same(values.get(entry.getKey()), entry.getValue())) continue;
            return false;
        }
        return true;
    }

    static boolean same(String string, String string2) {
        if (string == null) {
            return false;
        }
        if (string.trim().equalsIgnoreCase(string2.trim())) {
            return true;
        }
        try {
            return Math.abs(Double.parseDouble(string.trim()) - Double.parseDouble(string2.trim())) < 1.0E-4;
        }
        catch (NumberFormatException numberFormatException) {
            return false;
        }
    }

    private OnboardingRows() {
    }

    record Row(String label, String description, List<Level> levels, Set<LoadModel.Part> parts, boolean off) {
        int current(LoadModel.Values values) {
            for (int i = 0; i < this.levels.size(); ++i) {
                if (!OnboardingRows.holds(this.levels.get(i), values)) continue;
                return i;
            }
            return -1;
        }
    }

    record Level(String label, Map<String, String> writes) {
    }
}

