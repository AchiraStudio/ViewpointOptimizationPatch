/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  imgui.ImDrawList
 *  imgui.ImFont
 *  imgui.ImGui
 *  imgui.ImGuiIO
 *  imgui.type.ImBoolean
 */
package viewpoint.platform;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.type.ImBoolean;
import java.util.List;
import viewpoint.platform.GraphicsPresets;
import viewpoint.platform.Keys;
import viewpoint.platform.Onboarding;
import viewpoint.platform.OnboardingArt;
import viewpoint.platform.OnboardingCamera;
import viewpoint.platform.OnboardingGraphics;
import viewpoint.platform.OnboardingKeys;
import viewpoint.platform.PackPreset;
import viewpoint.platform.PackPresets;
import viewpoint.platform.PresetAdvice;
import viewpoint.platform.ShaderPack;
import viewpoint.platform.ShaderPacks;

final class OnboardingWindow {
    private static final float SHARE = 0.92f;
    private static final float MAX_WIDTH = 1080.0f;
    private static final float MAX_HEIGHT = 780.0f;
    private static final float TEXT = 1.2f;
    private static final float TITLE = 1.5f;
    private static final float LOGO = 0.62f;
    private static final float BANNER_SHARE = 0.6f;
    private static final float BUTTON_WIDTH = 130.0f;
    private static final float DOT = 4.5f;
    private static final float COMBO_WIDTH = 230.0f;
    private static final float BIG_TEXT = 1.3f;
    private static final float BIG_WIDTH = 300.0f;
    private static final float BIG_HEIGHT = 1.9f;
    private static final float ROUNDING = 6.0f;
    private static final double PULSE = 3.0;
    private static final float[] ACCENT_DIM = new float[]{0.82f, 0.6f, 0.22f};
    private static final float[] ACCENT_BRIGHT = new float[]{0.97f, 0.74f, 0.32f};
    private static final float[] ACCENT_HOVER = new float[]{1.0f, 0.82f, 0.42f};
    private static final float[] ACCENT_PRESSED = new float[]{0.78f, 0.54f, 0.16f};
    private static final float[] ON_ACCENT = new float[]{0.08f, 0.06f, 0.03f};
    static final int ACCENT_COLOURS = 4;
    private static final int FLAGS = 302;
    static final int DIM = -1728053248;
    static final int ACCENT = -11683598;
    static final int FAINT = 0x40FFFFFF;
    private static final String[] PRESET_TEXT = new String[]{"The plain Vanilla look and the shortest view: integrated graphics and old cards.", "Shaders with the lightest effects and a short view: a GTX 1050 Ti or RX 570, 2 to 3 GB of video memory.", "Shaders out to 1.5 km: a GTX 1650 or RX 580, 4 GB of video memory, 8 GB of memory.", "Shaders with sharper shadows, more effects, out to 3 km: an RTX 2060 to 3060, 6 GB of video memory and up.", "Everything, out to 4 km: an RTX 3070 or better, 10 GB of video memory and up."};
    private static final ImBoolean open = new ImBoolean(true);
    private static Step step = Step.WELCOME;
    private static float scale = 1.0f;

    static void draw() {
        ImGuiIO imGuiIO = ImGui.getIO();
        float f = imGuiIO.getDisplaySizeX();
        float f2 = imGuiIO.getDisplaySizeY();
        scale = imGuiIO.getFontGlobalScale() * 1.2f;
        if (Onboarding.fromStart()) {
            step = Step.WELCOME;
        }
        float f3 = Math.min(f * 0.92f, 1080.0f * scale);
        float f4 = Math.min(f2 * 0.92f, 780.0f * scale);
        ImGui.getBackgroundDrawList().addRectFilled(0.0f, 0.0f, f, f2, -1728053248);
        ImGui.setNextWindowPos((float)((f - f3) / 2.0f), (float)((f2 - f4) / 2.0f), (int)1);
        ImGui.setNextWindowSize((float)f3, (float)f4, (int)1);
        open.set(true);
        ImFont imFont = ImGui.getFont();
        imFont.setScale(1.2f);
        ImGui.pushFont((ImFont)imFont);
        if (ImGui.begin((String)"Project Viewpoint###viewpoint-setup", (ImBoolean)open, (int)302)) {
            OnboardingWindow.header();
            float f5 = ImGui.getFrameHeightWithSpacing() + ImGui.getStyle().getItemSpacingY() * 2.0f;
            if (ImGui.beginChild((String)"step", (float)0.0f, (float)(-f5), (boolean)false)) {
                OnboardingWindow.content();
            }
            ImGui.endChild();
            OnboardingWindow.footer();
        }
        ImGui.end();
        imFont.setScale(1.0f);
        ImGui.popFont();
        if (!open.get()) {
            Onboarding.close();
        }
    }

    static float scale() {
        return scale;
    }

    static void title(String string) {
        ImFont imFont = ImGui.getFont();
        imFont.setScale(1.8000001f);
        ImGui.pushFont((ImFont)imFont);
        ImGui.textUnformatted((String)string);
        imFont.setScale(1.2f);
        ImGui.popFont();
    }

    static void bullet(String string) {
        ImGui.bullet();
        ImGui.sameLine();
        ImGui.textWrapped((String)string.replace("%", "%%"));
    }

    private static void header() {
        OnboardingWindow.title(OnboardingWindow.step.title);
        ImGui.sameLine();
        Step[] stepArray = Step.values();
        float f = 15.75f * scale;
        float f2 = ImGui.getWindowContentRegionMaxX();
        float f3 = ImGui.getWindowPosX() + f2 - f * (float)stepArray.length;
        float f4 = ImGui.getCursorScreenPosY() + ImGui.getTextLineHeight() * 0.75f;
        ImDrawList imDrawList = ImGui.getWindowDrawList();
        for (Step step : stepArray) {
            float f5 = f3 + f * ((float)step.ordinal() + 0.5f);
            imDrawList.addCircleFilled(f5, f4, 4.5f * scale, step.ordinal() <= OnboardingWindow.step.ordinal() ? -11683598 : 0x40FFFFFF);
        }
        ImGui.newLine();
        ImGui.separator();
        ImGui.spacing();
    }

    private static void content() {
        switch (step.ordinal()) {
            case 0: {
                OnboardingWindow.welcome();
                break;
            }
            case 1: {
                OnboardingWindow.preset();
                break;
            }
            case 2: {
                OnboardingGraphics.draw();
                break;
            }
            case 3: {
                OnboardingKeys.draw();
                break;
            }
            case 4: {
                OnboardingCamera.draw();
                break;
            }
            case 5: {
                OnboardingWindow.done();
            }
        }
    }

    private static void footer() {
        ImGui.separator();
        float f = 130.0f * scale;
        Step step = OnboardingWindow.step;
        if (step != Step.WELCOME) {
            if (ImGui.button((String)"Back", (float)f, (float)0.0f)) {
                OnboardingWindow.step = Step.values()[step.ordinal() - 1];
            }
            ImGui.sameLine();
        }
        ImGui.setCursorPosX((float)(ImGui.getWindowContentRegionMaxX() - f));
        boolean bl = OnboardingWindow.step == Step.PRESET && Onboarding.advice() == null;
        ImGui.beginDisabled((boolean)bl);
        if (ImGui.button((String)(OnboardingWindow.step == Step.DONE ? "Ok" : "Next step"), (float)f, (float)0.0f)) {
            if (OnboardingWindow.step == Step.DONE) {
                Onboarding.finish();
                OnboardingWindow.step = Step.WELCOME;
            } else {
                OnboardingWindow.step = Step.values()[OnboardingWindow.step.ordinal() + 1];
            }
        }
        ImGui.endDisabled();
        if (bl && ImGui.isItemHovered((int)512)) {
            ImGui.setTooltip((String)"Check your hardware first.");
        }
    }

    private static void welcome() {
        float f = ImGui.getContentRegionAvailX();
        float f2 = ImGui.getContentRegionAvailY() * 0.6f;
        OnboardingArt.Picture picture = OnboardingArt.banner();
        OnboardingArt.Picture picture2 = OnboardingArt.logo();
        float f3 = picture == null ? 2.2260869f : picture.aspect();
        float f4 = Math.min(f / f3, f2);
        float f5 = f4 * f3;
        ImGui.setCursorPosX((float)(ImGui.getCursorPosX() + (f - f5) / 2.0f));
        float f6 = ImGui.getCursorScreenPosX();
        float f7 = ImGui.getCursorScreenPosY();
        if (picture == null) {
            ImGui.dummy((float)f5, (float)f4);
        } else {
            ImGui.image((int)picture.texture(), (float)f5, (float)f4);
        }
        if (picture2 != null) {
            float f8 = f5 * 0.62f;
            float f9 = f8 / picture2.aspect();
            float f10 = f6 + (f5 - f8) / 2.0f;
            float f11 = f7 + (f4 - f9) / 2.0f;
            ImGui.getWindowDrawList().addImage(picture2.texture(), f10, f11, f10 + f8, f11 + f9);
        }
        ImGui.spacing();
        ImGui.spacing();
        OnboardingWindow.title("Welcome to Project Viewpoint");
        ImGui.textWrapped((String)"Viewpoint turns Project Zomboid into a true 3D game. You can experience the classic gameplay through your character's own eyes or over the shoulder, with the whole world drawn out to the horizon.");
        ImGui.spacing();
        ImGui.textWrapped((String)("Drawing that world asks much more of your computer than the game alone. So before your first step, this setup will check your hardware, pick the graphics settings that better suits it, and help you set up the keys. It only takes a minute, and everything can be changed later in the settings window (" + Keys.SETTINGS.windowText() + ")."));
    }

    private static void preset() {
        ImGui.textWrapped((String)"Quality presets set how far and how finely the world is drawn and which effects run. Click the button below to let us pick the better preset for your computer. Don't worry, you can then choose another if you want to.");
        ImGui.spacing();
        ImGui.spacing();
        PresetAdvice.Advice advice = Onboarding.advice();
        int n = GraphicsPresets.current();
        if (advice == null) {
            if (OnboardingWindow.bigButton("CHECK MY HARDWARE")) {
                OnboardingWindow.check();
            }
            ImGui.spacing();
            ImGui.spacing();
            ImGui.spacing();
            OnboardingWindow.presetLine(null, n, null);
            return;
        }
        OnboardingWindow.presetLine(advice, n, "CHECK AGAIN");
        ImGui.spacing();
        OnboardingWindow.found(advice, n);
    }

    private static void presetLine(PresetAdvice.Advice advice, int n, String string) {
        float f = ImGui.getStyle().getItemSpacingX();
        float f2 = ImGui.getStyle().getFramePaddingX();
        float f3 = string == null ? 0.0f : f + ImGui.calcTextSize((String)string).x + 2.0f * f2;
        OnboardingWindow.centre(ImGui.calcTextSize((String)"Preset").x + f + 230.0f * scale + f3);
        ImGui.alignTextToFramePadding();
        ImGui.textUnformatted((String)"Preset");
        ImGui.sameLine();
        ImGui.beginDisabled((advice == null ? 1 : 0) != 0);
        ImGui.setNextItemWidth((float)(230.0f * scale));
        if (ImGui.beginCombo((String)"##preset", (String)OnboardingWindow.presetName(n, advice))) {
            for (int i = 0; i < 5; ++i) {
                if (!ImGui.selectable((String)OnboardingWindow.presetName(i, advice), (i == n ? 1 : 0) != 0)) continue;
                OnboardingWindow.choose(i);
            }
            ImGui.endCombo();
        }
        ImGui.endDisabled();
        if (string != null) {
            ImGui.sameLine();
            OnboardingWindow.pushAccent(1.0f);
            if (ImGui.button((String)string)) {
                OnboardingWindow.check();
            }
            ImGui.popStyleColor((int)4);
        }
    }

    private static void check() {
        ImGuiIO imGuiIO = ImGui.getIO();
        PresetAdvice.Advice advice = Onboarding.check(Math.round(imGuiIO.getDisplaySizeX() * imGuiIO.getDisplayFramebufferScaleX()), Math.round(imGuiIO.getDisplaySizeY() * imGuiIO.getDisplayFramebufferScaleY()));
        OnboardingWindow.choose(advice.preset());
        Onboarding.put("far.workers", Integer.toString(advice.farWorkers()));
    }

    private static boolean bigButton(String string) {
        ImFont imFont = ImGui.getFont();
        imFont.setScale(1.5600001f);
        ImGui.pushFont((ImFont)imFont);
        float f = Math.max(ImGui.calcTextSize((String)string).x + 4.0f * ImGui.getStyle().getFramePaddingX(), 300.0f * scale);
        OnboardingWindow.centre(f);
        ImGui.pushStyleVar((int)12, (float)(6.0f * scale));
        OnboardingWindow.pushAccent((float)(0.5 + 0.5 * Math.sin(ImGui.getTime() * 3.0)));
        boolean bl = ImGui.button((String)string, (float)f, (float)(ImGui.getFrameHeight() * 1.9f));
        ImGui.popStyleColor((int)4);
        ImGui.popStyleVar();
        imFont.setScale(1.2f);
        ImGui.popFont();
        return bl;
    }

    static void pushAccent(float f) {
        float[] fArray = new float[3];
        for (int i = 0; i < fArray.length; ++i) {
            fArray[i] = ACCENT_DIM[i] + (ACCENT_BRIGHT[i] - ACCENT_DIM[i]) * f;
        }
        ImGui.pushStyleColor((int)21, (float)fArray[0], (float)fArray[1], (float)fArray[2], (float)1.0f);
        ImGui.pushStyleColor((int)22, (float)ACCENT_HOVER[0], (float)ACCENT_HOVER[1], (float)ACCENT_HOVER[2], (float)1.0f);
        ImGui.pushStyleColor((int)23, (float)ACCENT_PRESSED[0], (float)ACCENT_PRESSED[1], (float)ACCENT_PRESSED[2], (float)1.0f);
        ImGui.pushStyleColor((int)0, (float)ON_ACCENT[0], (float)ON_ACCENT[1], (float)ON_ACCENT[2], (float)1.0f);
    }

    private static void centre(float f) {
        ImGui.setCursorPosX((float)(ImGui.getCursorPosX() + Math.max(0.0f, (ImGui.getContentRegionAvailX() - f) / 2.0f)));
    }

    private static void centred(String string) {
        float f = ImGui.calcTextSize((String)string).x;
        if (f > ImGui.getContentRegionAvailX()) {
            ImGui.textWrapped((String)string);
            return;
        }
        OnboardingWindow.centre(f);
        ImGui.textDisabled((String)string);
    }

    private static void found(PresetAdvice.Advice advice, int n) {
        for (String string : advice.reasons()) {
            OnboardingWindow.bullet(string);
        }
        ImGui.spacing();
        String string = "Recommended preset: " + GraphicsPresets.NAMES[advice.preset()];
        OnboardingWindow.centre(ImGui.calcTextSize((String)string).x);
        ImGui.textColored((int)-11683598, (String)string);
        ImGui.spacing();
        ImGui.textWrapped((String)OnboardingWindow.presetText(n));
    }

    static String presetText(int n) {
        return n >= 0 && n < PRESET_TEXT.length ? GraphicsPresets.NAMES[n] + ": " + PRESET_TEXT[n] : "Custom: your own settings.";
    }

    private static String presetName(int n, PresetAdvice.Advice advice) {
        String string = GraphicsPresets.NAMES[n];
        return advice != null && advice.preset() == n ? string + " (recommended)" : string;
    }

    static void choose(int n) {
        Onboarding.put("graphics.preset", GraphicsPresets.NAMES[n]);
        for (String string : List.of("vivid", "normal")) {
            ShaderPack shaderPack = ShaderPacks.find(string);
            PackPreset packPreset = shaderPack == null ? null : PresetAdvice.presetNamed(shaderPack, GraphicsPresets.NAMES[n]);
            if (packPreset == null) continue;
            Onboarding.put(PackPresets.key(shaderPack), packPreset.label);
        }
    }

    private static void done() {
        OnboardingWindow.title("You're all set!");
        ImGui.textWrapped((String)"Press Ok to save the setup and step into first person.");
        ImGui.spacing();
        OnboardingWindow.bullet(Keys.FIRST_PERSON.windowText() + ": toggle first person");
        OnboardingWindow.bullet(Keys.THIRD_PERSON.windowText() + ": toggle between third/first person");
        OnboardingWindow.bullet("Middle mouse button: toggle between controlling the cursor/camera");
        OnboardingWindow.bullet(Keys.SETTINGS.windowText() + ": toggle the settings menu");
        ImGui.spacing();
        ImGui.textWrapped((String)"Enjoy, but remember: THIS IS HOW YOU DIED.");
    }

    private OnboardingWindow() {
    }

    private static enum Step {
        WELCOME("Welcome to Project Viewpoint"),
        PRESET("Graphics Quality"),
        GRAPHICS("Graphics Settings"),
        KEYS("Controls"),
        CAMERA("Camera"),
        DONE("That's it!");

        final String title;

        private Step(String string2) {
            this.title = string2;
        }
    }
}

