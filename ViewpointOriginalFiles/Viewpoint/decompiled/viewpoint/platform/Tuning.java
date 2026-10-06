/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.ZomboidFileSystem
 */
package viewpoint.platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Properties;
import viewpoint.platform.Settings;
import zombie.ZomboidFileSystem;

public final class Tuning {
    private static final String FILE = "viewpoint-tuning.properties";
    public static float volumeSun = 1.5f;
    public static float airDensity = 0.0028f;
    public static float mistDensity = 0.02f;
    public static float dust = 20.0f;
    public static float ambientHaze = 0.85f;
    public static float lampHalo = 8.0f;
    public static float farHaze = 4000.0f;
    public static float farTreeGrowth = 0.25f;
    public static float farTreeVariation = 0.18f;
    public static float farTreeRelief = 0.3f;
    public static float farBareCrown = 0.35f;
    public static float farWindowGlow = 1.2f;
    public static float farLampGlow = 3.0f;
    public static float farLampSize = 0.6f;
    public static float farLampPixels = 2.5f;
    public static float lampShadowFloor = 0.3f;
    public static float bloomStrength = 0.1f;
    public static float exposure = 1.0f;
    public static float historyWeight = 0.9f;
    public static float sharpen = 0.12f;
    public static float giRadius = 1.2f;
    public static float giStrength = 0.7f;
    public static float sunBounce = 2.0f;
    public static float flashIntensity = 13.0f;
    public static float flashHot = 0.25f;
    public static float flashScatter = 2.5f;
    public static float handRight = 0.14f;
    public static float handDown = 0.25f;
    public static float handForward = 0.05f;
    public static float flashAim = 5.0f;
    public static float rainFall = 9.0f;
    public static float snowFall = 1.9f;
    public static float rainExposure = 0.09f;
    public static float rainLampGain = 0.6f;
    public static float vehicleGlass = 0.4f;
    public static float targetOutlineEdge = 2.0f;
    public static float deviceTextHeight = 1.2f;
    public static float vehicleTextHeight = 1.8f;
    public static float headTextHeight = 2.0f;
    public static float subtitleRise = 0.04f;
    private static long nextPoll;
    private static long fileStamp;

    public static void poll(long l) {
        Object object;
        long l2;
        if (!Settings.dev || l < nextPoll) {
            return;
        }
        nextPoll = l + 1000000000L;
        File file = new File(ZomboidFileSystem.instance.getCacheDir(), FILE);
        if (!file.isFile()) {
            Tuning.write(file);
        }
        if ((l2 = file.lastModified()) == fileStamp) {
            return;
        }
        fileStamp = l2;
        Properties properties = new Properties();
        try {
            object = new FileInputStream(file);
            try {
                properties.load((InputStream)object);
            }
            finally {
                ((InputStream)object).close();
            }
        }
        catch (Exception exception) {
            System.out.println("[Viewpoint] tuning: could not read " + String.valueOf(file) + ": " + String.valueOf(exception));
            return;
        }
        object = new StringBuilder();
        for (Field field : Tuning.fields()) {
            String string = properties.getProperty(field.getName());
            if (string == null) continue;
            try {
                float f = Float.parseFloat(string.trim());
                float f2 = field.getFloat(null);
                if (f == f2) continue;
                field.setFloat(null, f);
                ((StringBuilder)object).append(' ').append(field.getName()).append(' ').append(f2).append(" -> ").append(f);
            }
            catch (IllegalAccessException | NumberFormatException exception) {
                System.out.println("[Viewpoint] tuning: " + field.getName() + " = " + string + " is not a number");
            }
        }
        if (((StringBuilder)object).length() > 0) {
            System.out.println("[Viewpoint] tuning:" + String.valueOf(object));
        }
    }

    private static void write(File file) {
        Properties properties = new Properties();
        for (Field field : Tuning.fields()) {
            try {
                properties.setProperty(field.getName(), Float.toString(field.getFloat(null)));
            }
            catch (IllegalAccessException illegalAccessException) {
                return;
            }
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(file);){
            properties.store(fileOutputStream, "Viewpoint look constants (dev mode). Saved changes apply within a second; see Tuning.java for what each does.");
        }
        catch (Exception exception) {
            System.out.println("[Viewpoint] tuning: could not write " + String.valueOf(file) + ": " + String.valueOf(exception));
        }
    }

    private static Field[] fields() {
        return (Field[])Arrays.stream(Tuning.class.getDeclaredFields()).filter(field -> field.getType() == Float.TYPE && Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())).toArray(Field[]::new);
    }

    private Tuning() {
    }

    static {
        fileStamp = -1L;
    }
}

