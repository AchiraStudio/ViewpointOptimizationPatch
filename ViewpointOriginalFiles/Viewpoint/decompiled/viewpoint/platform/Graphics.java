/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  imgui.ImGui
 *  zombie.ZomboidFileSystem
 */
package viewpoint.platform;

import imgui.ImGui;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import viewpoint.iris.IrisPack;
import viewpoint.platform.GraphicsPresets;
import viewpoint.platform.IrisPacks;
import viewpoint.platform.IrisSettings;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.PackOption;
import viewpoint.platform.PackPresets;
import viewpoint.platform.SettingsWindow;
import viewpoint.platform.ShaderPack;
import viewpoint.platform.ShaderPacks;
import zombie.ZomboidFileSystem;

public final class Graphics {
    static final String PRESET = "Graphics/Preset";
    static final String SHADERS = "Graphics/Shaders";
    static final String OPTIONS = "Graphics/Shader options";
    private static final String[] MODES = new String[]{"Vanilla", "Modern"};
    private static final String[] FORMER_MODES = new String[]{"Normal", "Pretty"};
    private static final int VANILLA = 0;
    private static final int MODERN = 1;
    private static final float WRAP = 440.0f;
    private static volatile LiveSettings.Choice mode;
    private static volatile LiveSettings.Choice pack;
    private static volatile List<ShaderPack> modern;
    private static volatile List<IrisPack> iris;
    private static final Map<String, Map<String, LiveSettings.Setting>> options;
    private static boolean started;
    private static boolean replaced;
    private static volatile String leftOut;

    static void start() {
        if (!started) {
            Graphics.start(new File(ZomboidFileSystem.instance.getCacheDir(), "viewpoint-shaderpacks"));
        }
    }

    static void start(File file) {
        started = true;
        ShaderPacks.discover(file);
        List<ShaderPack> list = ShaderPacks.all();
        modern = list.stream().filter(shaderPack -> !shaderPack.id.equals("normal")).toList();
        GraphicsPresets.start();
        mode = LiveSettings.choice("graphics.mode", "Mode", SHADERS, MODES, FORMER_MODES, 1);
        mode.describe("Vanilla: the plain look, quick on any GPU. Modern: the shader pack below.");
        iris = IrisSettings.start(file, () -> Graphics.irisInUse() != null);
        Graphics.declarePack();
        for (ShaderPack shaderPack2 : list) {
            LinkedHashMap<String, LiveSettings.Setting> linkedHashMap = new LinkedHashMap<String, LiveSettings.Setting>();
            BooleanSupplier booleanSupplier = () -> Graphics.inUse() == shaderPack2 && Graphics.irisInUse() == null;
            PackPresets.declare(shaderPack2, OPTIONS, booleanSupplier);
            for (PackOption packOption : shaderPack2.options) {
                LiveSettings.Setting setting = packOption.declare(Graphics.key(shaderPack2, packOption), OPTIONS);
                setting.describe(packOption.tooltip);
                setting.shownWhen(booleanSupplier);
                linkedHashMap.put(packOption.id, setting);
            }
            options.put(shaderPack2.id, linkedHashMap);
        }
        StringBuilder stringBuilder = new StringBuilder();
        ShaderPacks.leftOut().forEach((string, string2) -> stringBuilder.append((String)string).append(": ").append((String)string2).append('\n'));
        IrisPacks.leftOut().forEach((string, string2) -> stringBuilder.append((String)string).append(": ").append((String)string2).append('\n'));
        leftOut = stringBuilder.toString().strip();
        SettingsWindow.panel(SHADERS, Graphics::panel);
    }

    private static void declarePack() {
        ArrayList<String> arrayList = new ArrayList<String>(modern.stream().map(shaderPack -> shaderPack.id).toList());
        arrayList.addAll(iris.stream().map(irisPack -> irisPack.id).toList());
        if (pack != null) {
            LiveSettings.remove(pack);
        }
        LiveSettings.Choice choice = LiveSettings.choice("graphics.pack", "Shader pack", SHADERS, arrayList.toArray(new String[0]), arrayList.indexOf("vivid"));
        choice.describe("The built-in packs, and those in Zomboid/viewpoint-shaderpacks: Viewpoint's (a folder with a pack.properties) and shader packs made for Minecraft (minecraft-).");
        choice.shownWhen(() -> mode.get() == 1);
        pack = choice;
    }

    static void update() {
        if (started && IrisSettings.update(Graphics.irisInUse())) {
            String string = pack.text();
            iris = IrisPacks.all();
            Graphics.declarePack();
            LiveSettings.put(Graphics.pack.key, string);
        }
    }

    static String key(ShaderPack shaderPack, PackOption packOption) {
        return "pack." + shaderPack.id + "." + packOption.id;
    }

    static ShaderPack inUse() {
        if (mode.get() == 0) {
            return ShaderPacks.find("normal");
        }
        return pack.get() < modern.size() ? modern.get(pack.get()) : ShaderPacks.find("default");
    }

    static IrisPack irisInUse() {
        int n = pack.get() - modern.size();
        return mode.get() == 1 && n >= 0 && n < iris.size() ? iris.get(n) : null;
    }

    static boolean changed() {
        boolean bl = !replaced && Graphics.replaceRemoved();
        replaced = true;
        bl |= GraphicsPresets.follow();
        ShaderPack shaderPack = Graphics.inUse();
        Map<String, LiveSettings.Setting> map = options.get(shaderPack.id);
        bl |= PackPresets.follow(shaderPack, map);
        ShaderPacks.use(shaderPack, packOption -> ((LiveSettings.Setting)map.get(packOption.id)).text());
        return bl |= IrisSettings.follow(Graphics.irisInUse());
    }

    static boolean replaceRemoved() {
        String string = LiveSettings.saved(Graphics.pack.key);
        boolean bl = false;
        for (Map.Entry<String, String> entry : ShaderPacks.REMOVED.entrySet()) {
            String string3 = entry.getKey();
            if (ShaderPacks.find(string3) != null) continue;
            bl |= LiveSettings.forget(string2 -> string2.startsWith("pack." + string3 + ".") || string2.equals("packPreset." + string3));
            if (string == null || !string.trim().equalsIgnoreCase(string3)) continue;
            pack.set(modern.indexOf(ShaderPacks.find(entry.getValue())));
            System.out.println("[Viewpoint] shader pack " + string3 + " is no longer built in: " + entry.getValue() + " is used instead");
            bl = true;
        }
        return bl;
    }

    private static void panel() {
        IrisPack irisPack = Graphics.irisInUse();
        if (irisPack != null) {
            Graphics.irisPanel(irisPack);
            return;
        }
        ShaderPacks.Active active = ShaderPacks.active();
        ShaderPacks.Failure failure = ShaderPacks.failure();
        ImGui.pushTextWrapPos((float)(440.0f * ImGui.getIO().getFontGlobalScale()));
        ShaderPack shaderPack = active.pack;
        ImGui.textWrapped((String)(shaderPack.name + (String)(shaderPack.author.isEmpty() ? "" : ", by " + shaderPack.author)));
        ImGui.textDisabled((String)shaderPack.description);
        if (failure != null && failure.generation() == active.generation) {
            ImGui.textColored((float)1.0f, (float)0.45f, (float)0.35f, (float)1.0f, (String)(shaderPack.name + " did not compile; drawn with " + ShaderPacks.drawn().pack.name + " meanwhile:"));
            ImGui.textWrapped((String)failure.log());
        }
        if (!leftOut.isEmpty()) {
            ImGui.textColored((float)1.0f, (float)0.75f, (float)0.35f, (float)1.0f, (String)"Left out:");
            ImGui.textWrapped((String)leftOut);
        }
        ImGui.popTextWrapPos();
    }

    private static void irisPanel(IrisPack irisPack) {
        ImGui.pushTextWrapPos((float)(440.0f * ImGui.getIO().getFontGlobalScale()));
        ImGui.textWrapped((String)(irisPack.id.substring("minecraft-".length()) + ", a shader pack made for Minecraft"));
        IrisPacks.Failure failure = IrisPacks.failure();
        IrisPacks.Active active = IrisPacks.drawn();
        if (failure != null && failure.packId().equals(irisPack.id)) {
            ImGui.textColored((float)1.0f, (float)0.45f, (float)0.35f, (float)1.0f, (String)"It cannot be drawn; Viewpoint's default pack draws meanwhile:");
            ImGui.textWrapped((String)failure.why());
        } else if (active == null || active.pack() != irisPack) {
            ImGui.textDisabled((String)"Linking: a few seconds.");
        } else {
            IrisPacks.report().forEach(ImGui::textWrapped);
        }
        if (!leftOut.isEmpty()) {
            ImGui.textColored((float)1.0f, (float)0.75f, (float)0.35f, (float)1.0f, (String)"Left out:");
            ImGui.textWrapped((String)leftOut);
        }
        ImGui.popTextWrapPos();
    }

    private Graphics() {
    }

    static {
        modern = List.of();
        iris = List.of();
        options = new HashMap<String, Map<String, LiveSettings.Setting>>();
        leftOut = "";
    }
}

