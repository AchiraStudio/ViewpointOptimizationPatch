/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 */
package viewpoint.platform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import zombie.core.Core;

public final class BuildPin {
    public static final String BUILD = "Build 42.21.0";
    public static final String SHA256 = "E1A69EB743EDE60B213A0FE7F8B83D4FCAB773036D256CC4543A336F3B058A33";
    private static Boolean supported;
    private static boolean refusalReported;

    public static synchronized boolean supported() {
        if (supported == null) {
            supported = BuildPin.check(BuildPin.gameJar());
        }
        return supported;
    }

    public static void reportRefused() {
        if (!refusalReported) {
            refusalReported = true;
            System.out.println("[Viewpoint] first person stays off: this is not the game build the mod supports (see the line at load)");
        }
    }

    static boolean check(File file) {
        try {
            String string = BuildPin.sha256(file);
            if (SHA256.equals(string)) {
                System.out.println("[Viewpoint] game build: Build 42.21.0, as pinned");
                return true;
            }
            System.out.println("[Viewpoint] unsupported game build: " + String.valueOf(file) + " has SHA-256 " + string + ", the mod supports E1A69EB743EDE60B213A0FE7F8B83D4FCAB773036D256CC4543A336F3B058A33 (Build 42.21.0); first person stays off");
            return false;
        }
        catch (IOException iOException) {
            System.out.println("[Viewpoint] cannot read the game's JAR " + String.valueOf(file) + " to check its build (" + String.valueOf(iOException) + "); first person stays off");
            return false;
        }
    }

    static String sha256(File file) throws IOException {
        MessageDigest messageDigest = BuildPin.sha256Digest();
        try (InputStream inputStream = Files.newInputStream(file.toPath(), new OpenOption[0]);){
            byte[] byArray = new byte[65536];
            int n = inputStream.read(byArray);
            while (n > 0) {
                messageDigest.update(byArray, 0, n);
                n = inputStream.read(byArray);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(messageDigest.digest());
    }

    static File gameJar() {
        try {
            return new File(Core.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        }
        catch (Exception | LinkageError throwable) {
            return new File("projectzomboid.jar");
        }
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException("every Java runtime has SHA-256", noSuchAlgorithmException);
        }
    }

    private BuildPin() {
    }
}

