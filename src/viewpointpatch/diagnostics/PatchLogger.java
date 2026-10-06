package viewpointpatch.diagnostics;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class PatchLogger {
    private static final String PREFIX = "[ViewpointPatch]";
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    public static volatile boolean debugEnabled = false;

    private PatchLogger() {}

    private static String timestamp() {
        return TIME_FORMAT.format(new Date());
    }

    public static void info(String message) {
        System.out.println(PREFIX + " " + message);
    }

    public static void warn(String message) {
        System.out.println(PREFIX + "[WARN] " + message);
    }

    public static void warn(String message, Throwable t) {
        System.out.println(PREFIX + "[WARN] " + message + (t != null ? ": " + t.getMessage() : ""));
        if (t != null && debugEnabled) {
            t.printStackTrace(System.out);
        }
    }

    public static void error(String message) {
        System.err.println(PREFIX + "[ERROR] " + message);
    }

    public static void error(String message, Throwable t) {
        System.err.println(PREFIX + "[ERROR] " + message);
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }

    public static void debug(String message) {
        if (debugEnabled) {
            System.out.println(PREFIX + "[DEBUG] " + message);
        }
    }
}

