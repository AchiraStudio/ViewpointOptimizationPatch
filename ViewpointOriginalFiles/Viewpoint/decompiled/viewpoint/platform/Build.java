/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public final class Build {
    private static final String FILE = "/viewpoint/build.properties";
    public static final boolean RELEASE = Build.read();

    private static boolean read() {
        Properties properties = new Properties();
        try (InputStream inputStream = Build.class.getResourceAsStream(FILE);){
            if (inputStream == null) {
                throw new IllegalStateException("no /viewpoint/build.properties in the mod's JAR");
            }
            properties.load(inputStream);
        }
        catch (IOException iOException) {
            throw new UncheckedIOException(iOException);
        }
        return Boolean.parseBoolean(properties.getProperty("release"));
    }

    private Build() {
    }
}

