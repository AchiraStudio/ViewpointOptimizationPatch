/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.ZomboidFileSystem
 */
package viewpoint.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.SettingsWindow;
import zombie.ZomboidFileSystem;

public final class SeatedEye {
    private static final String PLACE = "Debug/Seated eye";
    private static final String FILE = "viewpoint-seated-eye.properties";
    private static final LiveSettings.Number HEIGHT = LiveSettings.number("debug.seatedEye", "Eye up or down", "Debug/Seated eye", -1.0f, 1.0f, 0.01f, 0.0f);
    private static final SettingsWindow.Button SAVE = SettingsWindow.button("Debug/Seated eye", "Save for this vehicle", "Keeps the height above for the kind of vehicle you sit in: you sit at it whenever you get into one again.");
    private static Properties saved;
    private static String vehicle;
    private static long seatedFrame;

    public static float offset(String string, long l) {
        if (!string.equals(vehicle) || l != seatedFrame + 1L) {
            vehicle = string;
            float f = SeatedEye.savedHeight(string);
            HEIGHT.set(Float.isNaN(f) ? 0.0f : f);
            SAVE.take();
            SAVE.status(string + (String)(Float.isNaN(f) ? ": none saved" : ": saved " + f));
        }
        seatedFrame = l;
        if (SAVE.take()) {
            String string2 = Float.toString(HEIGHT.get());
            SeatedEye.saved().setProperty(string, string2);
            SeatedEye.write();
            SAVE.status(string + ": saved " + string2);
        }
        return HEIGHT.get();
    }

    private static float savedHeight(String string) {
        String string2 = SeatedEye.saved().getProperty(string);
        try {
            return string2 == null ? Float.NaN : Float.parseFloat(string2.trim());
        }
        catch (NumberFormatException numberFormatException) {
            return Float.NaN;
        }
    }

    private static Properties saved() {
        if (saved == null) {
            saved = new Properties();
            File file = SeatedEye.file();
            if (file.isFile()) {
                try (FileInputStream fileInputStream = new FileInputStream(file);){
                    saved.load(fileInputStream);
                }
                catch (Exception exception) {
                    System.out.println("[Viewpoint] could not read " + String.valueOf(file) + ": " + String.valueOf(exception));
                }
            }
        }
        return saved;
    }

    private static void write() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(SeatedEye.file());){
            saved.store(fileOutputStream, "Viewpoint's seated eye: scene units up from the head, by vehicle script (Debug, Seated eye)");
        }
        catch (Exception exception) {
            System.out.println("[Viewpoint] could not write " + String.valueOf(SeatedEye.file()) + ": " + String.valueOf(exception));
        }
    }

    private static File file() {
        return new File(ZomboidFileSystem.instance.getCacheDir(), FILE);
    }

    private SeatedEye() {
    }

    static {
        HEIGHT.describe("Seated in first person, how far the view is moved up (or down) from your head. Sitting in a vehicle sets it to the height saved for that kind of vehicle, or to 0.");
        seatedFrame = Long.MIN_VALUE;
    }
}

