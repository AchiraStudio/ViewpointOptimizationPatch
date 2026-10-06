/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import viewpoint.platform.GraphicsPresets;
import viewpoint.platform.Hardware;
import viewpoint.platform.ImGuiFrame;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.LoadModel;
import viewpoint.platform.OnboardingArt;
import viewpoint.platform.PresetAdvice;
import viewpoint.platform.SettingsWindow;

public final class Onboarding {
    private static final LiveSettings.Toggle FINISHED = LiveSettings.toggle("onboarding.finished", "Setup finished", "Debug/Views", false);
    private static final SettingsWindow.Button SHOW = SettingsWindow.button("Debug/Setup", "Show the setup", "The setup players go through before first person, from its first step. Its Ok saves it as finished and turns first person on.");
    private static volatile boolean shown;
    private static final ConcurrentLinkedQueue<String[]> edits;
    private static volatile boolean finishing;
    private static final AtomicBoolean restart;
    private static volatile Hardware hardware;
    private static volatile PresetAdvice.Advice advice;
    private static boolean enter;
    private static boolean unavailableSaid;

    public static boolean shown() {
        return shown;
    }

    public static boolean finished() {
        return FINISHED.get();
    }

    public static boolean blocks() {
        if (FINISHED.get()) {
            return false;
        }
        if (!ImGuiFrame.available()) {
            if (!unavailableSaid) {
                unavailableSaid = true;
                System.out.println("[Viewpoint] setup cannot show (the game's ImGui is on, or the window failed): first person goes on without it");
            }
            return false;
        }
        return true;
    }

    public static void playStarted() {
        if (!FINISHED.get()) {
            Onboarding.show();
        }
    }

    public static void show() {
        if (!shown) {
            OnboardingArt.preload();
            shown = true;
            System.out.println("[Viewpoint] setup shown" + (FINISHED.get() ? "" : ": first person waits until it is finished"));
        }
    }

    static boolean update() {
        boolean bl = false;
        SHOW.status(FINISHED.get() ? "finished" : "not finished");
        if (SHOW.take()) {
            restart.set(true);
            Onboarding.show();
        }
        String[] stringArray = edits.poll();
        while (stringArray != null) {
            LiveSettings.put(stringArray[0], stringArray[1]);
            bl = true;
            stringArray = edits.poll();
        }
        if (finishing) {
            finishing = false;
            FINISHED.set(true);
            shown = false;
            enter = true;
            bl = true;
            System.out.println("[Viewpoint] setup finished: first person turns on");
        }
        return bl;
    }

    public static boolean takeEnter() {
        boolean bl = enter;
        enter = false;
        return bl;
    }

    static void put(String string, String string2) {
        edits.add(new String[]{string, string2});
    }

    static void putAll(Map<String, String> map) {
        map.forEach(Onboarding::put);
    }

    static PresetAdvice.Advice check(int n, int n2) {
        Hardware hardware = Hardware.probe(n, n2);
        PresetAdvice.Advice advice = PresetAdvice.of(hardware);
        Onboarding.hardware = hardware;
        Onboarding.advice = advice;
        System.out.println("[Viewpoint] setup's hardware check: " + hardware.describe() + "; advised " + GraphicsPresets.NAMES[advice.preset()] + ", far world workers " + advice.farWorkers());
        return advice;
    }

    static Hardware hardware() {
        return hardware;
    }

    static PresetAdvice.Advice advice() {
        return advice;
    }

    static boolean fromStart() {
        return restart.getAndSet(false);
    }

    static void close() {
        shown = false;
    }

    static void finish() {
        finishing = true;
    }

    static LoadModel.Values values() {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        for (LiveSettings.Setting setting : LiveSettings.all()) {
            hashMap.put(setting.key, setting.text());
        }
        return string -> {
            String string2 = hashMap.containsKey(string) ? (String)hashMap.get(string) : LiveSettings.value(string);
            String[] stringArray = string2 == null ? GraphicsPresets.TABLE.get(string) : null;
            return stringArray != null ? stringArray[2] : string2;
        };
    }

    private Onboarding() {
    }

    static {
        FINISHED.shownWhen(() -> false);
        edits = new ConcurrentLinkedQueue();
        restart = new AtomicBoolean();
    }
}

