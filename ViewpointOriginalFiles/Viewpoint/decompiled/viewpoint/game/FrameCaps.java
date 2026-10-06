/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.SystemDisabler
 *  zombie.ZomboidFileSystem
 */
package viewpoint.game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import viewpoint.platform.BuildPin;
import zombie.GameWindow;
import zombie.SystemDisabler;
import zombie.ZomboidFileSystem;
import zombie.core.PerformanceSettings;
import zombie.gameStates.GameLoadingState;

public final class FrameCaps {
    public static final int[] FPS_TABLE = new int[]{500, 430, 400, 360, 330, 300, 244, 240, 165, 144, 120, 95, 90, 75, 60, 55, 45, 30, 24};
    public static final int STOCK_MAX_FPS = 244;
    public static final int MIN_FPS = 24;
    public static final int MAX_FPS = 500;
    public static final int MENU_SAME = 1;
    public static final int MENU_UNCAPPED = 2;
    public static final int MENU_DEFAULT_60 = 17;
    public static final int MENU_CHOICES = 2 + FPS_TABLE.length;
    public static final boolean ON = BuildPin.supported();
    private static final String FILE = "viewpoint-framecap.properties";
    private static volatile int menuIndex = 17;
    private static volatile int extraGameFps;
    private static volatile int inGameFps;
    private static volatile Boolean inGameUncapped;
    private static volatile boolean savingOptions;
    private static int rawLock;
    private static Boolean rawUncapped;
    private static File storeOverride;

    public static boolean inGame() {
        return GameWindow.isIngameState() || GameWindow.states.current instanceof GameLoadingState;
    }

    public static boolean uncappedNow(boolean bl) {
        if (savingOptions || FrameCaps.inGame() || menuIndex == 1) {
            return inGameUncapped != null ? inGameUncapped : bl;
        }
        return menuIndex == 2;
    }

    public static int lockNow(int n) {
        int n2;
        int n3 = extraGameFps > 0 ? extraGameFps : (n2 = inGameFps > 0 ? inGameFps : Math.max(1, n));
        if (savingOptions || FrameCaps.inGame() || menuIndex == 1 || menuIndex == 2) {
            return n2;
        }
        int n4 = menuIndex - 3;
        return n4 >= 0 && n4 < FPS_TABLE.length ? FPS_TABLE[n4] : 60;
    }

    public static int getMenuFramerateIndex() {
        return menuIndex;
    }

    public static void setMenuFramerateIndex(int n) {
        int n2 = Math.max(1, Math.min(MENU_CHOICES, n));
        if (n2 != menuIndex) {
            menuIndex = n2;
            FrameCaps.save();
        }
    }

    public static int getMenuFramerateChoices() {
        return MENU_CHOICES;
    }

    public static void setGameFramerate(int n) {
        PerformanceSettings.instance.setFramerateUncapped(n <= 0);
        inGameUncapped = n <= 0;
        if (n > 0) {
            int n2 = Math.max(24, Math.min(500, n));
            PerformanceSettings.setLockFPS(n2);
            inGameFps = n2;
        }
        extraGameFps = n > 244 ? Math.min(500, n) : 0;
        FrameCaps.save();
    }

    public static int getGameFramerate() {
        if (inGameUncapped != null && inGameUncapped.booleanValue()) {
            return 0;
        }
        return extraGameFps > 0 ? extraGameFps : (inGameFps > 0 ? inGameFps : PerformanceSettings.getLockFPS());
    }

    public static boolean isFramerateUncapped() {
        return inGameUncapped != null ? inGameUncapped.booleanValue() : PerformanceSettings.instance.isFramerateUncapped();
    }

    public static void beforeLoadOptions() {
        File file = new File(FrameCaps.file().getParentFile(), "options.ini");
        if (!file.isFile()) {
            return;
        }
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file));){
            String string = bufferedReader.readLine();
            while (string != null) {
                FrameCaps.readRawOption(string.trim());
                string = bufferedReader.readLine();
            }
        }
        catch (IOException iOException) {
            System.out.println("[Viewpoint] frame caps: could not read " + String.valueOf(file) + ": " + String.valueOf(iOException));
        }
    }

    private static void readRawOption(String string) {
        if (string.startsWith("frameRate=")) {
            try {
                rawLock = Integer.parseInt(string.substring("frameRate=".length()).trim());
            }
            catch (NumberFormatException numberFormatException) {
                System.out.println("[Viewpoint] frame caps: options.ini's " + string + " is no number");
            }
        } else if (string.startsWith("uncappedFPS=")) {
            rawUncapped = Boolean.parseBoolean(string.substring("uncappedFPS=".length()).trim());
        }
    }

    public static void restoreAtStartup() {
        FrameCaps.beforeLoadOptions();
        FrameCaps.afterLoadOptions();
    }

    public static void afterLoadOptions() {
        SystemDisabler.setUncappedFPS((boolean)true);
        FrameCaps.load();
        FrameCaps.applySaved();
    }

    public static void beforeSaveOptions() {
        savingOptions = true;
    }

    public static void afterSaveOptions() {
        savingOptions = false;
        FrameCaps.save();
    }

    private static void applySaved() {
        if (extraGameFps > 0) {
            PerformanceSettings.setLockFPS(extraGameFps);
            inGameFps = extraGameFps;
        } else if (rawLock >= 24 && rawLock <= 244) {
            PerformanceSettings.setLockFPS(rawLock);
            inGameFps = rawLock;
        }
        if (rawUncapped != null) {
            PerformanceSettings.instance.setFramerateUncapped(rawUncapped);
            inGameUncapped = rawUncapped;
        }
    }

    private static void load() {
        File file = FrameCaps.file();
        if (!file.isFile()) {
            return;
        }
        Properties properties = new Properties();
        try (FileReader fileReader = new FileReader(file);){
            properties.load(fileReader);
            menuIndex = Math.max(1, Math.min(MENU_CHOICES, FrameCaps.number(properties, "menuFramerateIndex", 17)));
            int n = FrameCaps.number(properties, "gameFps", 0);
            extraGameFps = n > 244 && n <= 500 ? n : 0;
            rawLock = n >= 24 && n <= 244 ? n : rawLock;
            String string = properties.getProperty("gameUncapped");
            rawUncapped = string != null ? Boolean.valueOf(Boolean.parseBoolean(string.trim())) : rawUncapped;
        }
        catch (IOException iOException) {
            System.out.println("[Viewpoint] frame caps: could not read " + String.valueOf(file) + ": " + String.valueOf(iOException));
        }
    }

    private static int number(Properties properties, String string, int n) {
        String string2 = properties.getProperty(string);
        try {
            return string2 == null ? n : Integer.parseInt(string2.trim());
        }
        catch (NumberFormatException numberFormatException) {
            System.out.println("[Viewpoint] frame caps: viewpoint-framecap.properties's " + string + " is no number: " + string2);
            return n;
        }
    }

    private static void save() {
        File file = FrameCaps.file();
        try (FileWriter fileWriter = new FileWriter(file);){
            fileWriter.write("# Viewpoint's frame rate caps (the game's Display options write this file)\n");
            fileWriter.write("# menuFramerateIndex: 1 the game's, 2 uncapped, 3.. 500 430 400 360 330 300 244 240 165 144 120 95 90 75 60 55 45 30 24\n");
            fileWriter.write("menuFramerateIndex=" + menuIndex + "\n");
            fileWriter.write("gameFps=" + (extraGameFps > 0 ? extraGameFps : inGameFps) + "\n");
            if (inGameUncapped != null) {
                fileWriter.write("gameUncapped=" + inGameUncapped + "\n");
            }
        }
        catch (IOException iOException) {
            System.out.println("[Viewpoint] frame caps: could not write " + String.valueOf(file) + ": " + String.valueOf(iOException));
        }
    }

    private static File file() {
        return storeOverride != null ? storeOverride : new File(ZomboidFileSystem.instance.getCacheDir(), FILE);
    }

    static void resetForTests(File file) {
        menuIndex = 17;
        extraGameFps = 0;
        inGameFps = 0;
        inGameUncapped = null;
        savingOptions = false;
        rawLock = -1;
        rawUncapped = null;
        storeOverride = file;
    }

    private FrameCaps() {
    }

    static {
        rawLock = -1;
    }
}

