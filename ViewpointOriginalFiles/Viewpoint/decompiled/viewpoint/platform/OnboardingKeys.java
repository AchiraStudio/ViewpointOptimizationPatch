/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  imgui.ImGui
 */
package viewpoint.platform;

import imgui.ImGui;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import viewpoint.platform.KeyBind;
import viewpoint.platform.KeyInput;
import viewpoint.platform.Keys;
import viewpoint.platform.OnboardingWindow;
import viewpoint.platform.Settings;
import viewpoint.platform.SettingsWindow;

final class OnboardingKeys {
    private static final float WIDE = 820.0f;
    private static final float KEY_WIDTH = 170.0f;
    private static final String[][] MOUSE = new String[][]{{"Middle button", "Free the cursor to click on things, or look around again"}, {"Wheel", "Choose in the loot menu while it shows"}, {"Right button", "The game's own menu on what the cursor points at, with the cursor free"}};

    static void draw() {
        boolean bl;
        ImGui.textWrapped((String)"These are Viewpoint's own keys; the game's are unchanged. Click a key to change it, then press the new one, with Shift, Ctrl or Alt held if you like; Esc keeps it as it was. The settings window and the game's Mod Options have them too.");
        ImGui.spacing();
        float f = OnboardingWindow.scale();
        Map<String, List<KeyBind>> map = OnboardingKeys.groups();
        ArrayList<String> arrayList = new ArrayList<String>(map.keySet());
        boolean bl2 = bl = ImGui.getContentRegionAvailX() >= 820.0f * f;
        if (!bl || !ImGui.beginTable((String)"columns", (int)2, (int)32768)) {
            arrayList.forEach(string -> OnboardingKeys.group(string, (List)map.get(string), f));
            OnboardingKeys.mouse();
            return;
        }
        int n = (arrayList.size() + 1) / 2;
        ImGui.tableNextRow();
        ImGui.tableNextColumn();
        arrayList.subList(0, n).forEach(string -> OnboardingKeys.group(string, (List)map.get(string), f));
        ImGui.tableNextColumn();
        arrayList.subList(n, arrayList.size()).forEach(string -> OnboardingKeys.group(string, (List)map.get(string), f));
        OnboardingKeys.mouse();
        ImGui.endTable();
    }

    private static Map<String, List<KeyBind>> groups() {
        LinkedHashMap<String, List<KeyBind>> linkedHashMap = new LinkedHashMap<String, List<KeyBind>>();
        for (KeyBind keyBind : Keys.all()) {
            if (Keys.debug(keyBind) && !Settings.dev) continue;
            linkedHashMap.computeIfAbsent(SettingsWindow.group(keyBind.section), string -> new ArrayList()).add(keyBind);
        }
        return linkedHashMap;
    }

    private static void group(String string, List<KeyBind> list, float f) {
        OnboardingWindow.title(string);
        int n = 24768;
        if (ImGui.beginTable((String)string, (int)3, (int)n)) {
            ImGui.tableSetupColumn((String)"Action", (int)8);
            ImGui.tableSetupColumn((String)"Key", (int)16, (float)(170.0f * f));
            ImGui.tableSetupColumn((String)"Default", (int)16);
            for (KeyBind keyBind : list) {
                OnboardingKeys.row(keyBind, f);
            }
            ImGui.endTable();
        }
        ImGui.spacing();
    }

    private static void row(KeyBind keyBind, float f) {
        String string;
        boolean bl;
        ImGui.tableNextRow();
        ImGui.tableNextColumn();
        ImGui.alignTextToFramePadding();
        ImGui.textUnformatted((String)keyBind.label);
        if (ImGui.isItemHovered() && !keyBind.tooltip().isEmpty()) {
            ImGui.setTooltip((String)keyBind.tooltip());
        }
        ImGui.tableNextColumn();
        boolean bl2 = bl = KeyInput.capturing() == keyBind;
        String string2 = bl ? "Press a key..." : (string = keyBind.bound() ? keyBind.windowText() : "None");
        if (ImGui.button((String)(string + "##" + keyBind.key), (float)(170.0f * f), (float)0.0f)) {
            KeyInput.capture(keyBind, false);
        }
        ImGui.tableNextColumn();
        boolean bl3 = keyBind.chords().equals(keyBind.fallback);
        ImGui.beginDisabled((boolean)bl3);
        if (ImGui.smallButton((String)("Default##" + keyBind.key))) {
            Keys.choose(keyBind, keyBind.fallback);
            SettingsWindow.edited();
        }
        ImGui.endDisabled();
    }

    private static void mouse() {
        OnboardingWindow.title("Mouse");
        for (String[] stringArray : MOUSE) {
            OnboardingWindow.bullet(stringArray[0] + ": " + stringArray[1]);
        }
    }

    private OnboardingKeys() {
    }
}

