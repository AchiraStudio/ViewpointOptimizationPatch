/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import viewpoint.platform.Graphics;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.PackOption;
import viewpoint.platform.PackPreset;
import viewpoint.platform.ShaderPack;

final class PackPresets {
    private static final String CUSTOM = "Custom";
    private static final Map<String, LiveSettings.Choice> choices = new HashMap<String, LiveSettings.Choice>();
    private static final Map<String, Integer> applied = new HashMap<String, Integer>();

    static void declare(ShaderPack shaderPack, String string, BooleanSupplier booleanSupplier) {
        if (shaderPack.presets.isEmpty()) {
            return;
        }
        String[] stringArray = new String[shaderPack.presets.size() + 1];
        int n = 0;
        for (int i = 0; i < shaderPack.presets.size(); ++i) {
            stringArray[i] = shaderPack.presets.get((int)i).label;
            n = shaderPack.presets.get((int)i).id.equals(shaderPack.firstPreset) ? i : n;
        }
        stringArray[shaderPack.presets.size()] = CUSTOM;
        LiveSettings.Choice choice = LiveSettings.choice(PackPresets.key(shaderPack), "Quality", string, stringArray, n);
        choice.describe("The pack's own presets of its options; changing any of them makes it Custom.");
        choice.shownWhen(booleanSupplier);
        choices.put(shaderPack.id, choice);
    }

    static String key(ShaderPack shaderPack) {
        return "packPreset." + shaderPack.id;
    }

    static boolean follow(ShaderPack shaderPack, Map<String, LiveSettings.Setting> map) {
        boolean bl;
        LiveSettings.Choice choice = choices.get(shaderPack.id);
        if (choice == null) {
            return false;
        }
        int n = choice.get();
        int n2 = shaderPack.presets.size();
        Integer n3 = applied.get(shaderPack.id);
        applied.put(shaderPack.id, n);
        if (n == n2) {
            return false;
        }
        PackPreset packPreset = shaderPack.presets.get(n);
        boolean bl2 = bl = n3 == null && !PackPresets.matches(packPreset, shaderPack, map);
        if (bl) {
            System.out.println("[Viewpoint] " + shaderPack.id + "'s preset " + packPreset.label + ": its values changed, written again");
        }
        if (bl || n3 != null && n != n3) {
            for (PackOption packOption : shaderPack.options) {
                LiveSettings.put(Graphics.key(shaderPack, packOption), packPreset.valueFor(packOption));
            }
            return true;
        }
        if (PackPresets.matches(packPreset, shaderPack, map)) {
            return false;
        }
        choice.set(n2);
        applied.put(shaderPack.id, n2);
        return true;
    }

    static boolean matches(PackPreset packPreset, ShaderPack shaderPack, Map<String, LiveSettings.Setting> map) {
        for (PackOption packOption : shaderPack.options) {
            if (map.get(packOption.id).matches(packPreset.valueFor(packOption))) continue;
            return false;
        }
        return true;
    }

    private PackPresets() {
    }
}

