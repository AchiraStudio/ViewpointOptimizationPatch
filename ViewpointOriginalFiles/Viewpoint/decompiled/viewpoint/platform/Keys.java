/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.Lua.LuaManager
 */
package viewpoint.platform;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import viewpoint.platform.Build;
import viewpoint.platform.KeyBind;
import viewpoint.platform.KeyChord;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.SettingsWindow;
import zombie.Lua.LuaManager;

public final class Keys {
    private static final String VIEWS = "Keys/Views";
    private static final String LOOT = "Keys/Loot menu";
    private static final String CAMERA = "Keys/Free camera";
    private static final String DEBUG = "Keys/Debug";
    private static final List<KeyBind> ALL = new ArrayList<KeyBind>();
    public static final KeyBind FIRST_PERSON = Keys.press("keys.firstPerson", "First person on or off", "Keys/Views", "O", "The first-person view on or off.");
    public static final KeyBind THIRD_PERSON = Keys.press("keys.thirdPerson", "First or third person", "Keys/Views", "SHIFT+O", "While the view is on: the camera over the shoulder, or back to the eyes.");
    public static final KeyBind SETTINGS = Keys.press("keys.settings", "Viewpoint settings window", "Keys/Views", "DELETE", "Shows or hides this window.");
    public static final KeyBind SCREENSHOT = Keys.press("keys.screenshot", "Viewpoint screenshot", "Keys/Views", "F9", "Saves the view to Zomboid/viewpoint-shots.");
    public static final KeyBind LOOT_TAKE = Keys.menu("keys.lootTake", "Loot menu: take or do", "F", "Takes the item selected in the loot menu, or does the action selected; the wheel selects. The game's Interact stays the game's.");
    public static final KeyBind LOOT_TAKE_ALL = Keys.menu("keys.lootTakeAll", "Loot menu: take all", "R", "Takes everything the loot menu shows.");
    public static final KeyBind LOOT_WINDOW = Keys.menu("keys.lootWindow", "Loot menu: open the loot window", "TAB", "Opens the game's own loot window on what the loot menu shows, with the cursor free; again to close it.");
    public static final KeyBind FREE_CAMERA = Keys.press("keys.freeCamera", "Free camera on or off", "Keys/Free camera", "INSERT+O", "While the view is on: a camera flying anywhere, the world still round the player.");
    public static final KeyBind PHYSICAL_CAMERA = Keys.press("keys.physicalCamera", "Physical free camera on or off", "Keys/Free camera", "SHIFT+INSERT+O", "A free camera the world is centred on: what is drawn, its detail and the sound follow it.");
    public static final KeyBind CAMERA_FORWARD = Keys.hold("keys.cameraForward", "Free camera: forward", "W");
    public static final KeyBind CAMERA_BACK = Keys.hold("keys.cameraBack", "Free camera: back", "S");
    public static final KeyBind CAMERA_LEFT = Keys.hold("keys.cameraLeft", "Free camera: left", "A");
    public static final KeyBind CAMERA_RIGHT = Keys.hold("keys.cameraRight", "Free camera: right", "D");
    public static final KeyBind CAMERA_UP = Keys.hold("keys.cameraUp", "Free camera: up", "SPACE, E");
    public static final KeyBind CAMERA_DOWN = Keys.hold("keys.cameraDown", "Free camera: down", "LSHIFT, Q");
    public static final KeyBind CAMERA_FAST = Keys.hold("keys.cameraFast", "Free camera: four times as fast", "LMENU");
    public static final KeyBind CAMERA_HOME = Keys.press("keys.cameraHome", "Free camera: back to the player", "Keys/Free camera", "NEXT", "The free camera back at the player's eyes.");
    public static final KeyBind LOD_VIEW = Keys.debug("keys.lodView", "Level-of-detail view", "END", "Every surface tinted by its level of detail (Debug, Views).");
    public static final KeyBind DEBUG_VIEW = Keys.dev("keys.debugView", "Next debug view", "F9", "Normal, the art only, the light only, the volumetric light, the bounce light, the lamps' shadows, the sun's maps.");
    public static final KeyBind ISOLATE = Keys.dev("keys.isolate", "Next effect off", "F10", "The renderer's effects off one at a time.");
    public static final KeyBind ROOMS = Keys.dev("keys.rooms", "Rooms' culling", "F11", "The culling of the rooms no one can see: on, frozen, off.");
    public static final KeyBind OVERLAY = Keys.dev("keys.overlay", "Performance overlay", "APOSTROPHE", "The overlay's next mode: full, compact, the graph alone, off.");
    public static final KeyBind MESH_RECORD = Keys.dev("keys.meshRecord", "Record the mesher", "F8", "Records the mesher round the player (TESTING.md).");
    public static final KeyBind MESH_VERIFY = Keys.dev("keys.meshVerify", "Check the mesher", "SHIFT+F8", "Checks the meshes round the player against the newest recording (TESTING.md).");
    private static final Map<String, KeyBind> MOD_OPTIONS = Map.of("lootInteract", LOOT_TAKE, "lootTakeAll", LOOT_TAKE_ALL, "lootOpenWindow", LOOT_WINDOW);
    private static boolean started;

    private static KeyBind press(String string, String string2, String string3, String string4, String string5) {
        return Keys.declare(string, string2, string3, KeyBind.Kind.PRESS, false, string4, string5);
    }

    private static KeyBind menu(String string, String string2, String string3, String string4) {
        return Keys.declare(string, string2, LOOT, KeyBind.Kind.MENU, false, string3, string4);
    }

    private static KeyBind hold(String string, String string2, String string3) {
        return Keys.declare(string, string2, CAMERA, KeyBind.Kind.HOLD, false, string3, "Held while the free camera flies.");
    }

    private static KeyBind debug(String string, String string2, String string3, String string4) {
        return Keys.declare(string, string2, DEBUG, KeyBind.Kind.PRESS, false, Keys.unlessRelease(string3, Build.RELEASE), string4);
    }

    private static KeyBind dev(String string, String string2, String string3, String string4) {
        return Keys.declare(string, string2, DEBUG, KeyBind.Kind.PRESS, true, Keys.unlessRelease(string3, Build.RELEASE), "Dev mode only. " + string4);
    }

    static String unlessRelease(String string, boolean bl) {
        return bl ? "" : string;
    }

    static boolean debug(KeyBind keyBind) {
        return keyBind.section.equals(DEBUG);
    }

    private static KeyBind declare(String string, String string2, String string3, KeyBind.Kind kind, boolean bl, String string4, String string5) {
        KeyBind keyBind = LiveSettings.add(new KeyBind(string, string2, string3, kind, bl, string4));
        keyBind.describe(string5);
        ALL.add(keyBind);
        return keyBind;
    }

    static List<KeyBind> all() {
        return ALL;
    }

    static void choose(KeyBind keyBind, List<KeyChord> list) {
        for (KeyBind keyBind2 : ALL) {
            List<KeyChord> list2 = Keys.without(keyBind, list, keyBind2, keyBind2.chords());
            if (keyBind2 == keyBind || list2.equals(keyBind2.chords())) continue;
            keyBind2.set(list2);
            System.out.println("[Viewpoint] key " + keyBind2.key + " freed of " + KeyChord.text(list) + ": " + (list2.isEmpty() ? "none" : KeyChord.text(list2)));
        }
        keyBind.set(list);
        System.out.println("[Viewpoint] key " + keyBind.key + ": " + (list.isEmpty() ? "none" : KeyChord.text(list)));
    }

    static List<KeyChord> without(KeyBind keyBind, List<KeyChord> list, KeyBind keyBind2, List<KeyChord> list2) {
        if (keyBind2 == keyBind || !Keys.rivals(keyBind, keyBind2)) {
            return list2;
        }
        ArrayList<KeyChord> arrayList = new ArrayList<KeyChord>(list2);
        arrayList.removeAll(list);
        return List.copyOf(arrayList);
    }

    static boolean rivals(KeyBind keyBind, KeyBind keyBind2) {
        boolean bl = keyBind.kind == KeyBind.Kind.MENU && keyBind2.kind == KeyBind.Kind.HOLD || keyBind.kind == KeyBind.Kind.HOLD && keyBind2.kind == KeyBind.Kind.MENU;
        return keyBind.dev == keyBind2.dev && !bl;
    }

    static void start() {
        if (started) {
            return;
        }
        started = true;
        File file = new File(LuaManager.getLuaCacheDir(), "ModOptions.ini");
        if (!file.isFile()) {
            return;
        }
        try {
            for (Map.Entry<KeyBind, String> entry : Keys.carried(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)).entrySet()) {
                LiveSettings.carryOver(entry.getKey().key, entry.getValue());
            }
        }
        catch (IOException | RuntimeException exception) {
            System.out.println("[Viewpoint] could not read the loot menu's keys from " + String.valueOf(file) + ": " + String.valueOf(exception));
        }
    }

    static Map<KeyBind, String> carried(List<String> list) {
        LinkedHashMap<KeyBind, String> linkedHashMap = new LinkedHashMap<KeyBind, String>();
        for (String string : list) {
            String[] stringArray = string.trim().split("\\|");
            boolean bl = stringArray.length == 4 && stringArray[0].equals("keybind") && stringArray[1].equals("Viewpoint");
            KeyBind keyBind = bl ? MOD_OPTIONS.get(stringArray[2]) : null;
            if (keyBind == null || !stringArray[3].matches("\\d{1,3}")) continue;
            int n = Integer.parseInt(stringArray[3]);
            linkedHashMap.put(keyBind, n == 0 ? "" : KeyChord.name(n));
        }
        return linkedHashMap;
    }

    public static int optionCount() {
        return Keys.options().size();
    }

    public static String option(int n) {
        return Keys.options().get((int)n).key;
    }

    public static String label(String string) {
        return Keys.find((String)string).label;
    }

    public static String tooltip(String string) {
        return Keys.find(string).tooltip();
    }

    public static String group(String string) {
        return SettingsWindow.group(Keys.find((String)string).section);
    }

    public static boolean loot(String string) {
        return Keys.find((String)string).kind == KeyBind.Kind.MENU;
    }

    public static String text(String string) {
        return Keys.find(string).text();
    }

    public static String fallbackText(String string) {
        return KeyChord.text(Keys.find((String)string).fallback);
    }

    public static String display(String string) {
        List<KeyChord> list = KeyChord.parse(string);
        return list.isEmpty() ? "NONE" : KeyChord.display(list, KeyChord::gameName);
    }

    public static int trigger(String string) {
        List<KeyChord> list = KeyChord.parse(string);
        return list.isEmpty() ? 0 : list.get(0).key();
    }

    public static boolean holds(String string, String string2) {
        List<KeyChord> list = KeyChord.parse(string);
        return !list.isEmpty() && list.get(0).held().contains(KeyChord.parse(string2).get(0).key());
    }

    public static String without(String string, String string2, String string3, String string4) {
        return KeyChord.text(Keys.without(Keys.find(string), KeyChord.parse(string2), Keys.find(string3), KeyChord.parse(string4)));
    }

    public static void set(String string, String string2) {
        Keys.choose(Keys.find(string), KeyChord.parse(string2));
        SettingsWindow.edited();
    }

    private static List<KeyBind> options() {
        ArrayList<KeyBind> arrayList = new ArrayList<KeyBind>();
        for (KeyBind keyBind : ALL) {
            if (Keys.debug(keyBind)) continue;
            arrayList.add(keyBind);
        }
        return arrayList;
    }

    private static KeyBind find(String string) {
        for (KeyBind keyBind : ALL) {
            if (!keyBind.key.equals(string)) continue;
            return keyBind;
        }
        throw new IllegalArgumentException("no key binding " + string);
    }

    private Keys() {
    }
}

