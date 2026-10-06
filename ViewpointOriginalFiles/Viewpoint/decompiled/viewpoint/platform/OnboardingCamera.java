/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  imgui.ImGui
 */
package viewpoint.platform;

import imgui.ImGui;
import java.util.Locale;
import viewpoint.platform.Keys;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.OnboardingWindow;
import viewpoint.platform.SettingsWindow;

final class OnboardingCamera {
    static final String FOV = "view.verticalFov";
    static final String THIRD_PERSON_FOV = "view.thirdPersonVerticalFov";
    static final String HEAD_MOVEMENT = "camera.headMovement";
    static final float[] LEVELS = new float[]{0.0f, 0.33f, 0.66f, 1.0f};
    static final String[] LEVEL_NAMES = new String[]{"Off", "Low", "Medium", "High"};
    private static final float NEAR = 0.005f;
    private static final float SLIDER_WIDTH = 320.0f;
    private static final float LEVEL_WIDTH = 110.0f;
    private static final int[] degrees = new int[1];

    static void draw() {
        float f = OnboardingWindow.scale();
        ImGui.textWrapped((String)("How much you see, and how the view moves as you walk. Both can be changed later in the settings window (" + Keys.SETTINGS.windowText() + "), under Controls."));
        ImGui.spacing();
        OnboardingWindow.title("Field of view");
        ImGui.textWrapped((String)"How tall the view is, in degrees from the bottom of the screen to its top. Wider shows more round you and makes things look farther away; narrower brings them closer.");
        ImGui.spacing();
        OnboardingCamera.fov(OnboardingCamera.number(FOV), f);
        OnboardingCamera.fov(OnboardingCamera.number(THIRD_PERSON_FOV), f);
        ImGui.spacing();
        ImGui.spacing();
        OnboardingWindow.title("Head movement");
        ImGui.textWrapped((String)"How much the view moves with your character's head as they stand, walk and turn. Off keeps it steady; High follows every step and sway. Lower it if the motion makes you feel unwell.");
        ImGui.spacing();
        OnboardingCamera.head(OnboardingCamera.number(HEAD_MOVEMENT), f);
    }

    private static void fov(LiveSettings.Number number, float f) {
        OnboardingCamera.degrees[0] = number.getInt();
        ImGui.setNextItemWidth((float)(320.0f * f));
        if (ImGui.sliderInt((String)(number.label + "##" + number.key), (int[])degrees, (int)((int)number.min), (int)((int)number.max))) {
            number.set(degrees[0]);
        }
        if (ImGui.isItemDeactivatedAfterEdit()) {
            SettingsWindow.edited();
        }
        ImGui.sameLine();
        ImGui.beginDisabled((number.getInt() == Math.round(number.fallback) ? 1 : 0) != 0);
        if (ImGui.smallButton((String)("Default##" + number.key))) {
            number.set(number.fallback);
            SettingsWindow.edited();
        }
        ImGui.endDisabled();
    }

    private static void head(LiveSettings.Number number, float f) {
        int n = OnboardingCamera.level(number.get());
        for (int i = 0; i < LEVELS.length; ++i) {
            boolean bl;
            if (i > 0) {
                ImGui.sameLine();
            }
            boolean bl2 = bl = i == n;
            if (bl) {
                OnboardingWindow.pushAccent(1.0f);
            }
            if (ImGui.button((String)(LEVEL_NAMES[i] + "##head"), (float)(110.0f * f), (float)0.0f) && !bl) {
                number.set(LEVELS[i]);
                SettingsWindow.edited();
            }
            if (!bl) continue;
            ImGui.popStyleColor((int)4);
        }
        if (n < 0) {
            ImGui.sameLine();
            ImGui.alignTextToFramePadding();
            ImGui.textDisabled((String)String.format(Locale.ROOT, "Custom (%.2f)", Float.valueOf(number.get())));
        }
    }

    static int level(float f) {
        for (int i = 0; i < LEVELS.length; ++i) {
            if (!(Math.abs(f - LEVELS[i]) < 0.005f)) continue;
            return i;
        }
        return -1;
    }

    private static LiveSettings.Number number(String string) {
        LiveSettings.Setting setting = LiveSettings.find(string);
        if (setting instanceof LiveSettings.Number) {
            LiveSettings.Number number = (LiveSettings.Number)setting;
            return number;
        }
        throw new IllegalStateException(string + " is not declared as the setup draws");
    }

    private OnboardingCamera() {
    }
}

