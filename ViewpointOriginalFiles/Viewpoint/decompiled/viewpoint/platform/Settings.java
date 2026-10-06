/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.ZomboidFileSystem
 */
package viewpoint.platform;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.Properties;
import viewpoint.platform.Pool;
import zombie.ZomboidFileSystem;

public final class Settings {
    public static final String FILE = "viewpoint.properties";
    public static final String DEV = "dev";
    public static final String SHADER_DIR = "shaderDir";
    public static final String ROOMS = "rooms";
    private static final String OLD_FILE = "pzfp.properties";
    private static final String OLD_WIDTH = "chunkGridWidth";
    private static final String OLD_FAR = "farRadius";
    public static volatile boolean dev;
    public static volatile boolean rooms;
    public static volatile String shaderDir;

    public static synchronized void load() {
        Object object;
        File file = new File(ZomboidFileSystem.instance.getCacheDir(), FILE);
        File file2 = new File(ZomboidFileSystem.instance.getCacheDir(), OLD_FILE);
        if (!file.exists() && file2.isFile()) {
            Settings.carryOver(file2, file);
        }
        Properties properties = new Properties();
        if (file.isFile()) {
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
                System.out.println("[Viewpoint] could not read " + String.valueOf(file) + ": " + String.valueOf(exception));
            }
        }
        if (!file.exists()) {
            try {
                object = new FileOutputStream(file);
                try {
                    properties.store((OutputStream)object, "Viewpoint. dev=true: developer mode; shaderDir: where dev mode reads the shaders from (see Settings.java). The memory pools' caps are in the settings window (Delete).");
                }
                finally {
                    ((OutputStream)object).close();
                }
            }
            catch (Exception exception) {
                System.out.println("[Viewpoint] could not write " + String.valueOf(file) + ": " + String.valueOf(exception));
            }
        }
        if (properties.getProperty(OLD_WIDTH) != null) {
            System.out.println("[Viewpoint] chunkGridWidth in viewpoint.properties is no longer read: the game keeps its own chunk grid, and the far world covers the distance");
        }
        if (properties.getProperty(OLD_FAR) != null) {
            System.out.println("[Viewpoint] farRadius in viewpoint.properties is no longer read: the far world's reach is in the settings window (World, Levels of detail)");
        }
        for (Closeable closeable : Pool.ALL) {
            ((Pool)((Object)closeable)).carryOver(properties.getProperty(((Pool)((Object)closeable)).setting));
        }
        dev = Boolean.parseBoolean(System.getProperty("viewpoint.dev", properties.getProperty(DEV, "false")).trim());
        rooms = Boolean.parseBoolean(properties.getProperty(ROOMS, "true").trim());
        object = System.getProperty("viewpoint.shaderDir", properties.getProperty(SHADER_DIR));
        String string = shaderDir = object == null || ((String)object).isBlank() ? null : ((String)object).trim();
        if (dev) {
            System.out.println("[Viewpoint] dev mode on" + (String)(shaderDir == null ? "" : ", shaders from " + shaderDir));
        }
    }

    private static void carryOver(File file, File file2) {
        try {
            Files.copy(file.toPath(), file2.toPath(), new CopyOption[0]);
            System.out.println("[Viewpoint] settings carried over from " + String.valueOf(file));
        }
        catch (IOException iOException) {
            System.out.println("[Viewpoint] could not copy " + String.valueOf(file) + ": " + String.valueOf(iOException));
        }
    }

    private Settings() {
    }

    static {
        rooms = true;
    }
}

