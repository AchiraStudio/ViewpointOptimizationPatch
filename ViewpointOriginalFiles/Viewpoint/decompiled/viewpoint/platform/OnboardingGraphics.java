/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  imgui.ImDrawList
 *  imgui.ImGui
 */
package viewpoint.platform;

import imgui.ImDrawList;
import imgui.ImGui;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.IntConsumer;
import viewpoint.platform.GraphicsPresets;
import viewpoint.platform.Hardware;
import viewpoint.platform.LoadModel;
import viewpoint.platform.Onboarding;
import viewpoint.platform.OnboardingRows;
import viewpoint.platform.OnboardingWindow;
import viewpoint.platform.PresetAdvice;
import viewpoint.platform.ShaderPack;
import viewpoint.platform.ShaderPacks;

final class OnboardingGraphics {
    private static final float WIDE = 760.0f;
    private static final float VALUE_WIDTH = 200.0f;
    private static final float ROWS_SHARE = 0.56f;
    private static final float SWATCH = 5.0f;
    private static final float BAR_HEIGHT = 0.5f;
    private static final double EASY = 0.6;
    private static final double TIGHT = 0.9;
    private static final float[] GREEN = new float[]{0.3f, 0.78f, 0.36f};
    private static final float[] YELLOW = new float[]{0.95f, 0.78f, 0.22f};
    private static final float[] RED = new float[]{0.9f, 0.26f, 0.22f};
    private static final int PRESET = -1;
    private static List<OnboardingRows.Row> rows = List.of();
    private static String rowsFor = "";
    private static int pointed = -1;

    static void draw() {
        LoadModel.Values values;
        ShaderPack shaderPack;
        boolean bl;
        String string = (bl ? "vanilla " : "modern ") + ((shaderPack = ShaderPacks.find((bl = "Vanilla".equalsIgnoreCase((values = Onboarding.values()).get("graphics.mode"))) ? "normal" : values.get("graphics.pack"))) == null ? "" : shaderPack.id);
        if (!string.equals(rowsFor) && shaderPack != null) {
            rows = OnboardingRows.all(shaderPack, bl);
            rowsFor = string;
            pointed = Math.min(pointed, rows.size() - 1);
        }
        float f = OnboardingWindow.scale();
        float f2 = ImGui.getContentRegionAvailX();
        if (f2 >= 760.0f * f) {
            if (ImGui.beginChild((String)"rows", (float)(f2 * 0.56f), (float)0.0f, (boolean)false)) {
                OnboardingGraphics.list(values, f);
            }
            ImGui.endChild();
            ImGui.sameLine();
            if (ImGui.beginChild((String)"panel", (float)0.0f, (float)0.0f, (boolean)false)) {
                OnboardingGraphics.panel(values);
            }
            ImGui.endChild();
        } else {
            OnboardingGraphics.list(values, f);
            ImGui.separator();
            OnboardingGraphics.panel(values);
        }
    }

    private static void list(LoadModel.Values values, float f) {
        int n2 = 24768;
        if (!ImGui.beginTable((String)"settings", (int)2, (int)n2)) {
            return;
        }
        ImGui.tableSetupColumn((String)"Setting", (int)8);
        ImGui.tableSetupColumn((String)"Value", (int)16, (float)(200.0f * f));
        int n3 = GraphicsPresets.current();
        OnboardingGraphics.name(-1, "Preset", false);
        OnboardingGraphics.picker(OnboardingGraphics.presets(), n3 == 5 ? -1 : n3, OnboardingWindow::choose, f);
        ImGui.popID();
        for (int i = 0; i < rows.size(); ++i) {
            OnboardingRows.Row row = rows.get(i);
            OnboardingGraphics.name(i, row.label(), row.off());
            if (row.off()) {
                ImGui.alignTextToFramePadding();
                ImGui.textDisabled((String)"Modern only");
            } else {
                List<String> list = row.levels().stream().map(OnboardingRows.Level::label).toList();
                OnboardingGraphics.picker(list, row.current(values), n -> Onboarding.putAll(row.levels().get(n).writes()), f);
            }
            ImGui.popID();
        }
        ImGui.endTable();
    }

    private static void name(int n, String string, boolean bl) {
        float f = ImGui.getFrameHeightWithSpacing();
        ImGui.pushID((int)n);
        ImGui.tableNextRow((int)0, (float)f);
        ImGui.tableNextColumn();
        ImGui.alignTextToFramePadding();
        int n2 = 18;
        ImGui.beginDisabled((boolean)bl);
        if (ImGui.selectable((String)string, (pointed == n ? 1 : 0) != 0, (int)n2, (float)0.0f, (float)(f - ImGui.getStyle().getItemSpacingY()))) {
            pointed = n;
        }
        ImGui.endDisabled();
        if (ImGui.isItemHovered((int)512)) {
            pointed = n;
        }
        ImGui.tableNextColumn();
    }

    private static List<String> presets() {
        PresetAdvice.Advice advice = Onboarding.advice();
        String[] stringArray = new String[5];
        for (int i = 0; i < stringArray.length; ++i) {
            boolean bl = advice != null && advice.preset() == i;
            stringArray[i] = GraphicsPresets.NAMES[i] + (bl ? " (advised)" : "");
        }
        return List.of(stringArray);
    }

    private static void picker(List<String> list, int n, IntConsumer intConsumer, float f) {
        int n2 = list.size() - 1;
        int n3 = n2 / 2;
        ImGui.beginDisabled((n == 0 ? 1 : 0) != 0);
        if (ImGui.arrowButton((String)"less", (int)0)) {
            intConsumer.accept(n < 0 ? n3 : n - 1);
        }
        ImGui.endDisabled();
        ImGui.sameLine();
        String string = n < 0 ? "Custom" : list.get(n);
        float f2 = ImGui.getFrameHeight();
        float f3 = 200.0f * f - 2.0f * (f2 + ImGui.getStyle().getItemSpacingX());
        float f4 = ImGui.calcTextSize((String)string).x;
        ImGui.setCursorPosX((float)(ImGui.getCursorPosX() + Math.max(0.0f, (f3 - f4) / 2.0f)));
        ImGui.alignTextToFramePadding();
        ImGui.textUnformatted((String)string);
        ImGui.sameLine((float)(200.0f * f - f2 + ImGui.getStyle().getCellPaddingX()));
        ImGui.beginDisabled((n == n2 ? 1 : 0) != 0);
        if (ImGui.arrowButton((String)"more", (int)1)) {
            intConsumer.accept(n < 0 ? n3 : n + 1);
        }
        ImGui.endDisabled();
    }

    private static void panel(LoadModel.Values values) {
        LoadModel.Load load;
        Record record;
        Hardware hardware = Onboarding.hardware();
        if (pointed == -1 || rows.isEmpty()) {
            OnboardingGraphics.preset(values, hardware);
        } else {
            record = rows.get(Math.min(pointed, rows.size() - 1));
            OnboardingWindow.title(((OnboardingRows.Row)record).label());
            ImGui.textWrapped((String)((OnboardingRows.Row)record).description());
            ImGui.spacing();
            load = ((OnboardingRows.Row)record).levels().isEmpty() ? null : ((OnboardingRows.Row)record).levels().get(0);
            LoadModel.Part[] partArray = load == null ? null : arg_0 -> OnboardingGraphics.lambda$panel$0((OnboardingRows.Level)((Object)load), values, arg_0);
            OnboardingGraphics.parts(((OnboardingRows.Row)record).parts(), (LoadModel.Values)partArray, values, hardware, "at this level");
        }
        ImGui.spacing();
        ImGui.separator();
        ImGui.textUnformatted((String)"Estimated load:");
        if (hardware == null) {
            ImGui.textDisabled((String)"Check your hardware on the step before to see it.");
            return;
        }
        record = LoadModel.estimate(values, hardware);
        load = LoadModel.budget(hardware);
        for (LoadModel.Part part : LoadModel.Part.values()) {
            OnboardingGraphics.meter(part.label, OnboardingGraphics.describe(part, (LoadModel.Load)record, load), LoadModel.stress((LoadModel.Load)record, load, part, values, hardware));
        }
        ImGui.spacing();
        ImGui.textWrapped((String)String.format(Locale.ROOT, "Estimated for %d x %d from our measurements on Ryzen 5 5600 / RTX 3060.", hardware.width(), hardware.height()));
    }

    private static void preset(LoadModel.Values values, Hardware hardware) {
        OnboardingWindow.title("Preset");
        ImGui.textWrapped((String)"Sets every setting below at once, and changing any of them makes it Custom.");
        ImGui.spacing();
        ImGui.textWrapped((String)OnboardingWindow.presetText(GraphicsPresets.current()));
        PresetAdvice.Advice advice = Onboarding.advice();
        if (advice != null) {
            ImGui.textColored((int)-11683598, (String)("Recommended: " + GraphicsPresets.NAMES[advice.preset()]));
        }
        ImGui.spacing();
        LoadModel.Values values2 = PresetAdvice.values(0);
        LoadModel.Values values3 = string -> values2.get(string) != null ? values2.get(string) : values.get(string);
        OnboardingGraphics.parts(EnumSet.allOf(LoadModel.Part.class), values3, values, hardware, "over Potato");
    }

    private static void parts(Set<LoadModel.Part> set, LoadModel.Values values, LoadModel.Values values2, Hardware hardware, String string) {
        ImDrawList imDrawList = ImGui.getWindowDrawList();
        float f = 5.0f * OnboardingWindow.scale();
        LoadModel.Load load = null;
        LoadModel.Load load2 = null;
        if (hardware != null && values != null) {
            load = LoadModel.estimate(values2, hardware);
            load2 = LoadModel.estimate(values, hardware);
        }
        for (LoadModel.Part part : LoadModel.Part.values()) {
            String string2;
            boolean bl = set.contains((Object)part);
            float f2 = ImGui.getCursorScreenPosX() + f;
            float f3 = ImGui.getCursorScreenPosY() + ImGui.getTextLineHeight() / 2.0f;
            imDrawList.addRectFilled(f2 - f, f3 - f, f2 + f, f3 + f, bl ? -11683598 : 0x40FFFFFF);
            ImGui.dummy((float)(f * 2.0f), (float)ImGui.getTextLineHeight());
            ImGui.sameLine();
            String string3 = string2 = load == null || !bl ? "" : OnboardingGraphics.added(part, load.of(part) - load2.of(part), string);
            if (bl) {
                ImGui.textUnformatted((String)(part.label + string2));
                continue;
            }
            ImGui.textDisabled((String)part.label);
        }
    }

    private static String added(LoadModel.Part part, double d, String string) {
        if (Math.abs(d) < 0.05) {
            return "";
        }
        boolean bl = part == LoadModel.Part.CPU || part == LoadModel.Part.GPU;
        return String.format(Locale.ROOT, bl ? ": %+.1f ms %s" : ": %+.0f MB %s", d, string);
    }

    private static String describe(LoadModel.Part part, LoadModel.Load load, LoadModel.Load load2) {
        return switch (part) {
            default -> throw new IncompatibleClassChangeError();
            case LoadModel.Part.CPU -> String.format(Locale.ROOT, "%.1f ms/frame", load.cpuMs());
            case LoadModel.Part.MEMORY -> String.format(Locale.ROOT, "%.1f of %.1f GB free", load.ramMiB() / 1024.0, load2.ramMiB() / 1024.0);
            case LoadModel.Part.GPU -> String.format(Locale.ROOT, "%.1f ms, about %d fps", load.gpuMs(), Math.round(1000.0 / load.gpuMs()));
            case LoadModel.Part.VIDEO_MEMORY -> String.format(Locale.ROOT, "%.1f of %.1f GB free", load.vramMiB() / 1024.0, load2.vramMiB() / 1024.0);
        };
    }

    private static void meter(String string, String string2, double d) {
        float[] fArray = OnboardingGraphics.colour(d);
        float f = ImGui.getCursorPosX();
        float f2 = ImGui.getContentRegionAvailX();
        float f3 = ImGui.calcTextSize((String)string2).x;
        ImGui.textUnformatted((String)string);
        if (ImGui.calcTextSize((String)string).x + f3 + 2.0f * ImGui.getStyle().getItemSpacingX() <= f2) {
            ImGui.sameLine((float)(f + f2 - f3));
        }
        ImGui.pushStyleColor((int)0, (float)fArray[0], (float)fArray[1], (float)fArray[2], (float)1.0f);
        ImGui.textUnformatted((String)string2);
        ImGui.popStyleColor();
        ImGui.pushStyleColor((int)42, (float)fArray[0], (float)fArray[1], (float)fArray[2], (float)1.0f);
        ImGui.progressBar((float)((float)Math.min(d, 1.0)), (float)-1.0f, (float)(ImGui.getTextLineHeight() * 0.5f), (String)"");
        ImGui.popStyleColor();
    }

    static float[] colour(double d) {
        if (d <= 0.6) {
            return (float[])GREEN.clone();
        }
        boolean bl = d <= 0.9;
        float f = (float)Math.min(1.0, bl ? (d - 0.6) / 0.30000000000000004 : (d - 0.9) / 0.09999999999999998 / 2.0);
        float[] fArray = bl ? GREEN : YELLOW;
        float[] fArray2 = bl ? YELLOW : RED;
        float[] fArray3 = new float[3];
        for (int i = 0; i < 3; ++i) {
            fArray3[i] = fArray[i] + (fArray2[i] - fArray[i]) * f;
        }
        return fArray3;
    }

    private OnboardingGraphics() {
    }

    private static /* synthetic */ String lambda$panel$0(OnboardingRows.Level level, LoadModel.Values values, String string) {
        return level.writes().getOrDefault(string, values.get(string));
    }
}

