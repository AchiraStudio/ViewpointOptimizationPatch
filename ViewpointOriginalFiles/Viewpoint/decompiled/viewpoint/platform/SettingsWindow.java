/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  imgui.ImGui
 *  imgui.type.ImBoolean
 *  imgui.type.ImInt
 */
package viewpoint.platform;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import viewpoint.platform.Capture;
import viewpoint.platform.Graphics;
import viewpoint.platform.ImGuiFrame;
import viewpoint.platform.KeyBind;
import viewpoint.platform.KeyInput;
import viewpoint.platform.Keys;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.Onboarding;
import viewpoint.platform.OnboardingWindow;
import viewpoint.platform.PerformanceOverlay;
import zombie.input.Mouse;

public final class SettingsWindow {
    private static final float ITEM_WIDTH = 220.0f;
    private static final float KEY_WIDTH = 190.0f;
    static final String NESTED = " > ";
    private static final List<String> ORDER = List.of("Graphics/Preset", "Graphics/Shaders", "Graphics/Shader options", "Controls/Mouse", "Controls/View", "Controls/Camera", "Controls/Movement", "Keys/Views", "Keys/Loot menu", "Keys/Free camera", "Keys/Debug", "World/Chunks", "World/Floors", "World/Floor preview", "World/Levels of detail", "World/Far world", "World/Memory", "Debug/Views", "Debug/Profiling");
    private static final Map<String, List<Runnable>> panels = new LinkedHashMap<String, List<Runnable>>();
    private static final List<Button> buttons = new CopyOnWriteArrayList<Button>();
    private static volatile boolean shown;
    private static volatile boolean dirty;
    private static final ImBoolean open;
    private static final ImBoolean toggle;
    private static final ImInt choice;
    private static final float[] number;
    private static final int[] index;

    public static boolean shown() {
        return shown;
    }

    public static void update() {
        Graphics.start();
        Keys.start();
        boolean bl = LiveSettings.load();
        Graphics.update();
        if (bl) {
            PerformanceOverlay.loaded();
        }
        if (Keys.SETTINGS.pressed()) {
            shown = !shown;
        }
        boolean bl2 = Onboarding.update();
        if (bl || dirty || bl2) {
            boolean bl3 = dirty || bl2;
            dirty = false;
            if (Graphics.changed() || bl3) {
                LiveSettings.save();
            }
        }
        Capture.update();
    }

    static void edited() {
        dirty = true;
    }

    public static void blockGameMouse() {
        boolean bl;
        boolean bl2 = bl = Onboarding.shown() && ImGuiFrame.available();
        if (!(bl || shown && ImGuiFrame.wantsMouse)) {
            return;
        }
        Arrays.fill(Mouse.buttonDownStates, false);
        Arrays.fill(Mouse.buttonPrevStates, false);
        Mouse.wheelDelta = 0;
    }

    public static void panel(String string2, Runnable runnable) {
        panels.computeIfAbsent(string2, string -> new ArrayList()).add(runnable);
    }

    public static Button button(String string, String string2, String string3) {
        Button button = new Button(string, string2, string3);
        buttons.add(button);
        return button;
    }

    public static void render() {
        if (Onboarding.shown()) {
            ImGuiFrame.draw(OnboardingWindow::draw);
        } else if (shown) {
            ImGuiFrame.draw(SettingsWindow::contents);
        } else {
            ImGuiFrame.wantsMouse = false;
            KeyInput.cancel();
        }
    }

    private static void contents() {
        open.set(true);
        ImGui.setNextWindowPos((float)40.0f, (float)80.0f, (int)4);
        String string = Keys.SETTINGS.windowText();
        if (ImGui.begin((String)("Viewpoint" + (String)(string.isEmpty() ? "" : " (" + string + ")") + "###viewpoint"), (ImBoolean)open, (int)64)) {
            ImGui.pushItemWidth((float)(220.0f * ImGui.getIO().getFontGlobalScale()));
            SettingsWindow.tabs();
            ImGui.popItemWidth();
        }
        ImGui.end();
        if (!open.get()) {
            shown = false;
        }
    }

    private static void tabs() {
        if (!ImGui.beginTabBar((String)"tabs")) {
            return;
        }
        Map<String, List<LiveSettings.Setting>> map = SettingsWindow.byPlace();
        for (Map.Entry<String, Set<String>> entry : SettingsWindow.byTab(map.keySet()).entrySet()) {
            if (!ImGui.beginTabItem((String)entry.getKey())) continue;
            SettingsWindow.groups(entry.getValue(), map, null);
            ImGui.endTabItem();
        }
        ImGui.endTabBar();
    }

    private static void groups(Set<String> set, Map<String, List<LiveSettings.Setting>> map, String string) {
        for (String string2 : set) {
            if (!Objects.equals(SettingsWindow.parent(string2), string)) continue;
            String string3 = SettingsWindow.leaf(string2) + "##" + string2;
            if (string == null) {
                if (!ImGui.collapsingHeader((String)string3)) continue;
            } else if (!ImGui.treeNode((String)string3)) continue;
            SettingsWindow.body(string2, map.getOrDefault(string2, List.of()));
            SettingsWindow.groups(set, map, string2);
            if (string == null) continue;
            ImGui.treePop();
        }
    }

    private static void body(String string, List<LiveSettings.Setting> list) {
        for (LiveSettings.Setting object : list) {
            SettingsWindow.edit(object);
        }
        for (Button button : buttons) {
            if (!button.place.equals(string) || !button.shown.getAsBoolean()) continue;
            SettingsWindow.draw(button);
        }
        for (Runnable runnable : panels.getOrDefault(string, List.of())) {
            runnable.run();
        }
    }

    private static void capture() {
        if (ImGui.button((String)"Capture profile")) {
            Capture.request();
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip((String)"A Java Flight Recorder recording of every thread and our frames (its length above), saved to Zomboid/viewpoint-captures. Start moving first.");
        }
        ImGui.sameLine();
        ImGui.textUnformatted((String)Capture.status());
    }

    private static void draw(Button button) {
        if (ImGui.button((String)(button.label + "##" + button.place))) {
            button.press();
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip((String)button.tooltip);
        }
        ImGui.sameLine();
        ImGui.textUnformatted((String)button.status);
    }

    static String tab(String string) {
        int n = string.indexOf(47);
        return n < 0 ? string : string.substring(0, n);
    }

    static String group(String string) {
        return string.substring(string.indexOf(47) + 1);
    }

    static Map<String, Set<String>> byTab(Collection<String> collection) {
        LinkedHashMap<String, Set<String>> linkedHashMap = new LinkedHashMap<String, Set<String>>();
        for (String string2 : collection) {
            Set set = linkedHashMap.computeIfAbsent(SettingsWindow.tab(string2), string -> new LinkedHashSet());
            int n = string2.indexOf(NESTED, string2.indexOf(47) + 1);
            while (n >= 0) {
                set.add(string2.substring(0, n));
                n = string2.indexOf(NESTED, n + NESTED.length());
            }
            set.add(string2);
        }
        return linkedHashMap;
    }

    static String parent(String string) {
        int n = string.lastIndexOf(NESTED);
        return n > string.indexOf(47) ? string.substring(0, n) : null;
    }

    static String leaf(String string) {
        String string2 = SettingsWindow.group(string);
        int n = string2.lastIndexOf(NESTED);
        return n < 0 ? string2 : string2.substring(n + NESTED.length());
    }

    private static void edit(LiveSettings.Setting setting) {
        LiveSettings.Choice choice;
        Object object;
        if (!setting.shown()) {
            return;
        }
        if (setting instanceof KeyBind) {
            KeyBind keyBind = (KeyBind)setting;
            SettingsWindow.keyRow(keyBind);
            return;
        }
        boolean bl = SettingsWindow.resetButton(setting);
        ImGui.sameLine();
        if (setting instanceof LiveSettings.Number) {
            object = (LiveSettings.Number)setting;
            SettingsWindow.number[0] = ((LiveSettings.Number)object).get();
            if (ImGui.sliderFloat((String)(((LiveSettings.Number)object).label + "##" + ((LiveSettings.Number)object).key), (float[])number, (float)((LiveSettings.Number)object).min, (float)((LiveSettings.Number)object).max, (String)(((LiveSettings.Number)object).step >= 1.0f ? "%.0f" : "%.2f"))) {
                ((LiveSettings.Number)object).set(number[0]);
            }
            bl |= ImGui.isItemDeactivatedAfterEdit();
        } else if (setting instanceof LiveSettings.Toggle) {
            LiveSettings.Toggle toggle = (LiveSettings.Toggle)setting;
            SettingsWindow.toggle.set(toggle.get());
            if (ImGui.checkbox((String)(toggle.label + "##" + toggle.key), (ImBoolean)SettingsWindow.toggle)) {
                toggle.set(SettingsWindow.toggle.get());
                bl = true;
            }
        } else if (setting instanceof LiveSettings.Choice && (choice = (LiveSettings.Choice)setting).slider()) {
            SettingsWindow.index[0] = choice.get();
            String string = choice.labels()[index[0]].replace("%", "%%");
            if (ImGui.sliderInt((String)(choice.label + "##" + choice.key), (int[])index, (int)0, (int)(choice.options.length - 1), (String)string)) {
                choice.set(index[0]);
            }
            bl |= ImGui.isItemDeactivatedAfterEdit();
        } else if (setting instanceof LiveSettings.Choice) {
            LiveSettings.Choice choice2 = (LiveSettings.Choice)setting;
            SettingsWindow.choice.set(choice2.get());
            if (ImGui.combo((String)(choice2.label + "##" + choice2.key), (ImInt)SettingsWindow.choice, (String[])choice2.labels())) {
                choice2.set(SettingsWindow.choice.get());
                bl = true;
            }
        }
        if (bl) {
            dirty = true;
        }
        if (ImGui.isItemHovered()) {
            object = setting.tooltip();
            ImGui.setTooltip((String)((String)(((String)object).isEmpty() ? "" : (String)object + "\n") + setting.key + " (viewpoint-live.properties)"));
        }
    }

    private static boolean resetButton(LiveSettings.Setting setting) {
        LiveSettings.Setting setting2;
        LiveSettings.Setting setting3;
        String string = "";
        boolean bl = true;
        if (setting instanceof LiveSettings.Number) {
            LiveSettings.Number number = (LiveSettings.Number)setting;
            string = number.step >= 1.0f ? Integer.toString(Math.round(number.fallback)) : Float.toString(number.fallback);
            bl = number.matches(Float.toString(number.fallback));
        } else if (setting instanceof LiveSettings.Toggle) {
            setting3 = (LiveSettings.Toggle)setting;
            string = ((LiveSettings.Toggle)setting3).fallback ? "on" : "off";
            bl = ((LiveSettings.Toggle)setting3).get() == ((LiveSettings.Toggle)setting3).fallback;
        } else if (setting instanceof LiveSettings.Choice) {
            setting2 = (LiveSettings.Choice)setting;
            string = ((LiveSettings.Choice)setting2).labels()[((LiveSettings.Choice)setting2).fallback];
            boolean bl2 = bl = ((LiveSettings.Choice)setting2).get() == ((LiveSettings.Choice)setting2).fallback;
        }
        if (bl) {
            ImGui.beginDisabled();
        }
        boolean bl3 = ImGui.smallButton((String)("<##reset " + setting.key));
        if (bl) {
            ImGui.endDisabled();
        } else if (ImGui.isItemHovered()) {
            ImGui.setTooltip((String)("Back to its default: " + string).replace("%", "%%"));
        }
        if (!bl3) {
            return false;
        }
        if (setting instanceof LiveSettings.Number) {
            setting3 = (LiveSettings.Number)setting;
            ((LiveSettings.Number)setting3).set(((LiveSettings.Number)setting3).fallback);
        } else if (setting instanceof LiveSettings.Toggle) {
            setting2 = (LiveSettings.Toggle)setting;
            ((LiveSettings.Toggle)setting2).set(((LiveSettings.Toggle)setting2).fallback);
        } else if (setting instanceof LiveSettings.Choice) {
            LiveSettings.Choice choice = (LiveSettings.Choice)setting;
            choice.set(choice.fallback);
        }
        return true;
    }

    private static void keyRow(KeyBind keyBind) {
        String string;
        boolean bl;
        boolean bl2 = bl = KeyInput.capturing() == keyBind;
        String string2 = bl ? "Press a key (Esc: as it was)" : (string = keyBind.bound() ? keyBind.windowText() : "None");
        if (ImGui.button((String)(string + "##" + keyBind.key), (float)(190.0f * ImGui.getIO().getFontGlobalScale()), (float)0.0f)) {
            KeyInput.capture(keyBind, false);
        }
        if (ImGui.isItemHovered()) {
            String string3 = keyBind.tooltip();
            ImGui.setTooltip((String)((String)(string3.isEmpty() ? "" : string3 + "\n") + "Click, then press the key, with any it needs held (Shift+O). Names follow your keyboard layout.\n" + keyBind.key + " (viewpoint-live.properties)"));
        }
        ImGui.sameLine();
        if (ImGui.smallButton((String)("Add##" + keyBind.key))) {
            KeyInput.capture(keyBind, true);
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip((String)"Another key for it, besides those it has.");
        }
        ImGui.sameLine();
        if (ImGui.smallButton((String)("Default##" + keyBind.key))) {
            Keys.choose(keyBind, keyBind.fallback);
            dirty = true;
        }
        ImGui.sameLine();
        if (ImGui.smallButton((String)("Clear##" + keyBind.key))) {
            Keys.choose(keyBind, List.of());
            dirty = true;
        }
        ImGui.sameLine();
        ImGui.textUnformatted((String)keyBind.label);
    }

    private static Map<String, List<LiveSettings.Setting>> byPlace() {
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>(ORDER);
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        List<LiveSettings.Setting> list = LiveSettings.all();
        for (LiveSettings.Setting object2 : list) {
            if (!object2.shown()) continue;
            linkedHashSet.add(object2.section);
            linkedHashMap.computeIfAbsent(object2.section, string -> new ArrayList()).add(object2);
        }
        for (LiveSettings.Setting setting : list) {
            if (!setting.shown()) continue;
            for (String string2 : setting.also()) {
                linkedHashSet.add(string2);
                linkedHashMap.computeIfAbsent(string2, string -> new ArrayList()).add(setting);
            }
        }
        LinkedHashSet linkedHashSet2 = new LinkedHashSet();
        for (Button button : buttons) {
            if (!button.shown.getAsBoolean()) continue;
            linkedHashSet2.add(button.place);
        }
        linkedHashSet.addAll(panels.keySet());
        linkedHashSet.addAll(linkedHashSet2);
        LinkedHashMap<String, List<LiveSettings.Setting>> linkedHashMap2 = new LinkedHashMap<String, List<LiveSettings.Setting>>();
        for (String string2 : linkedHashSet) {
            if (!linkedHashMap.containsKey(string2) && !panels.containsKey(string2) && !linkedHashSet2.contains(string2)) continue;
            linkedHashMap2.put(string2, linkedHashMap.getOrDefault(string2, List.of()));
        }
        return linkedHashMap2;
    }

    private SettingsWindow() {
    }

    static {
        SettingsWindow.panel("Debug/Profiling", SettingsWindow::capture);
        open = new ImBoolean(true);
        toggle = new ImBoolean();
        choice = new ImInt();
        number = new float[1];
        index = new int[1];
    }

    public static final class Button {
        final String place;
        final String label;
        final String tooltip;
        private final AtomicBoolean pressed = new AtomicBoolean();
        private volatile String status = "";
        private volatile BooleanSupplier shown = () -> true;

        Button(String string, String string2, String string3) {
            this.place = string;
            this.label = string2;
            this.tooltip = string3;
        }

        public boolean take() {
            return this.pressed.getAndSet(false);
        }

        void press() {
            this.pressed.set(true);
        }

        public void status(String string) {
            this.status = string;
        }

        public void shownWhen(BooleanSupplier booleanSupplier) {
            this.shown = booleanSupplier;
        }
    }
}

