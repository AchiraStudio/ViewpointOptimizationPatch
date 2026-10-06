/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import viewpoint.iris.IrisOptions;
import viewpoint.iris.IrisPack;
import viewpoint.platform.Graphics;
import viewpoint.platform.IrisPacks;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.SettingsWindow;

final class IrisSettings {
    private static final String CUSTOM = "Custom";
    static final SettingsWindow.Button APPLY = SettingsWindow.button("Graphics/Shader options", "Apply", "Links the shader pack again with the options as they are now: it takes seconds.");
    private static final SettingsWindow.Button RESCAN = SettingsWindow.button("Graphics/Shaders", "Rescan", "Finds the packs dropped into Zomboid/viewpoint-shaderpacks since the game started.");
    private static final Map<String, Map<String, LiveSettings.Setting>> settings = new HashMap<String, Map<String, LiveSettings.Setting>>();
    private static final Map<String, LiveSettings.Choice> profiles = new HashMap<String, LiveSettings.Choice>();
    private static final Map<String, Integer> applied = new HashMap<String, Integer>();
    private static File folder;
    private static IrisPack published;
    private static Map<String, String> publishedValues;

    private IrisSettings() {
    }

    static List<IrisPack> start(File file, BooleanSupplier booleanSupplier) {
        folder = file;
        IrisPacks.discover(folder);
        APPLY.shownWhen(booleanSupplier);
        return IrisSettings.declareNew();
    }

    private static List<IrisPack> declareNew() {
        List<IrisPack> list = IrisPacks.all();
        for (IrisPack irisPack : list) {
            if (settings.containsKey(irisPack.id)) continue;
            IrisSettings.declare(irisPack);
        }
        return list;
    }

    private static void declare(IrisPack irisPack) {
        BooleanSupplier booleanSupplier = () -> Graphics.irisInUse() == irisPack;
        LinkedHashMap<String, LiveSettings.Setting> linkedHashMap = new LinkedHashMap<String, LiveSettings.Setting>();
        settings.put(irisPack.id, linkedHashMap);
        ArrayList<String> arrayList = new ArrayList<String>(irisPack.defaults.profiles.keySet());
        if (!arrayList.isEmpty()) {
            arrayList.add(CUSTOM);
            int n = arrayList.indexOf(CUSTOM);
            for (int i = 0; i < arrayList.size() - 1; ++i) {
                n = IrisSettings.matches(irisPack, irisPack.profile((String)arrayList.get(i)), IrisSettings.defaults(irisPack)) ? i : n;
            }
            LiveSettings.Choice choice = LiveSettings.choice("minecraftProfile." + irisPack.id, "Profile", "Graphics/Shader options", arrayList.toArray(new String[0]), n);
            choice.describe("The pack's own profiles of its options; changing any of them makes it Custom.");
            String[] stringArray = arrayList.toArray(new String[0]);
            for (int i = 0; i < stringArray.length - 1; ++i) {
                stringArray[i] = irisPack.label("profile." + stringArray[i], stringArray[i]);
            }
            choice.shownAs(stringArray);
            choice.shownWhen(booleanSupplier);
            profiles.put(irisPack.id, choice);
        }
        if (irisPack.defaults.screens.containsKey("")) {
            IrisSettings.screen(irisPack, "", "Graphics/Shader options", booleanSupplier, linkedHashMap, new HashSet<String>());
        } else {
            irisPack.options.all().forEach(option -> IrisSettings.option(irisPack, option, "Graphics/Shader options", booleanSupplier, linkedHashMap));
        }
        irisPack.options.all().forEach(option -> IrisSettings.option(irisPack, option, "Graphics/Shader options", () -> false, linkedHashMap));
    }

    private static void screen(IrisPack irisPack, String string, String string2, BooleanSupplier booleanSupplier, Map<String, LiveSettings.Setting> map, Set<String> set) {
        List<String> list = irisPack.defaults.screens.get(string);
        if (list == null || !set.add(string)) {
            return;
        }
        for (String string3 : list) {
            if (string3.startsWith("[") && string3.endsWith("]")) {
                String string4 = string3.substring(1, string3.length() - 1);
                String string5 = irisPack.label("screen." + (String)string4, string4);
                String string6 = string2 + " > " + string5;
                IrisSettings.screen(irisPack, string4, string6, booleanSupplier, map, set);
                continue;
            }
            if (string3.equals("*")) {
                for (IrisOptions.Option option : irisPack.options.all()) {
                    IrisSettings.option(irisPack, option, string2, booleanSupplier, map);
                }
                continue;
            }
            if (irisPack.options.get(string3) == null) continue;
            IrisSettings.option(irisPack, irisPack.options.get(string3), string2, booleanSupplier, map);
        }
    }

    private static void option(IrisPack irisPack, IrisOptions.Option option, String string, BooleanSupplier booleanSupplier, Map<String, LiveSettings.Setting> map) {
        if (map.containsKey(option.name())) {
            return;
        }
        String string2 = IrisSettings.key(irisPack, option.name());
        String string3 = irisPack.label("option." + option.name(), option.name());
        LiveSettings.Toggle toggle = option.kind() == IrisOptions.Kind.TOGGLE ? LiveSettings.toggle(string2, string3, string, option.fallback().equals("true")) : LiveSettings.choice(string2, string3, string, option.values().toArray(new String[0]), option.values().indexOf(option.fallback()));
        toggle.describe(irisPack.label("option." + option.name() + ".comment", ""));
        if (toggle instanceof LiveSettings.Choice) {
            LiveSettings.Choice choice = (LiveSettings.Choice)((Object)toggle);
            choice.shownAs(IrisSettings.shown(irisPack, option));
            if (irisPack.defaults.sliders.contains(option.name())) {
                choice.asSlider();
            }
        }
        toggle.shownWhen(booleanSupplier);
        map.put(option.name(), toggle);
    }

    static String[] shown(IrisPack irisPack, IrisOptions.Option option) {
        String string = option.name();
        String string2 = irisPack.text("prefix." + string);
        String string3 = irisPack.text("suffix." + string);
        String[] stringArray = new String[option.values().size()];
        for (int i = 0; i < stringArray.length; ++i) {
            String string4 = option.values().get(i);
            stringArray[i] = string2 + irisPack.label("value." + string + "." + string4, string4) + string3;
        }
        return stringArray;
    }

    static String key(IrisPack irisPack, String string) {
        return "minecraft." + irisPack.id + "." + string;
    }

    static boolean follow(IrisPack irisPack) {
        Map<String, String> map;
        boolean bl = irisPack != null && IrisSettings.profile(irisPack);
        Map<Object, Object> map2 = map = irisPack == null ? Map.of() : IrisSettings.values(irisPack);
        if (irisPack != published) {
            IrisSettings.publish(irisPack, map);
        }
        APPLY.status(irisPack != null && !map.equals(publishedValues) ? "Changed: Apply to see it" : "");
        return bl;
    }

    static boolean update(IrisPack irisPack) {
        if (APPLY.take() && irisPack != null) {
            IrisSettings.publish(irisPack, IrisSettings.values(irisPack));
            APPLY.status("");
        }
        if (!RESCAN.take()) {
            return false;
        }
        boolean bl = IrisPacks.discover(folder);
        IrisSettings.declareNew();
        RESCAN.status(IrisPacks.all().size() + " Minecraft shader packs");
        return bl;
    }

    private static void publish(IrisPack irisPack, Map<String, String> map) {
        published = irisPack;
        publishedValues = map;
        IrisPacks.use(irisPack, map);
    }

    private static Map<String, String> values(IrisPack irisPack) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        settings.get(irisPack.id).forEach((string, setting) -> linkedHashMap.put((String)string, setting.text()));
        return linkedHashMap;
    }

    private static boolean profile(IrisPack irisPack) {
        LiveSettings.Choice choice = profiles.get(irisPack.id);
        if (choice == null) {
            return false;
        }
        int n = choice.get();
        int n2 = choice.options.length - 1;
        Integer n3 = applied.put(irisPack.id, n);
        if (n3 != null && n != n3 && n != n2) {
            Map<String, String> map = irisPack.profile(choice.options[n]);
            for (IrisOptions.Option option : irisPack.options.all()) {
                LiveSettings.put(IrisSettings.key(irisPack, option.name()), map.getOrDefault(option.name(), option.fallback()));
            }
            return true;
        }
        if (n == n2 || IrisSettings.matches(irisPack, irisPack.profile(choice.options[n]), IrisSettings.values(irisPack))) {
            return false;
        }
        choice.set(n2);
        applied.put(irisPack.id, n2);
        return true;
    }

    private static boolean matches(IrisPack irisPack, Map<String, String> map, Map<String, String> map2) {
        for (IrisOptions.Option option : irisPack.options.all()) {
            String string = map2.get(option.name());
            if (string == null || string.equals(map.getOrDefault(option.name(), option.fallback()))) continue;
            return false;
        }
        return true;
    }

    private static Map<String, String> defaults(IrisPack irisPack) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (IrisOptions.Option option : irisPack.options.all()) {
            linkedHashMap.put(option.name(), option.fallback());
        }
        return linkedHashMap;
    }

    static {
        publishedValues = Map.of();
    }
}

