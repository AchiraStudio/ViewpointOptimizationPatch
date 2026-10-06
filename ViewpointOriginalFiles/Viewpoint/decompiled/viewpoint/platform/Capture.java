/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.ZomboidFileSystem
 */
package viewpoint.platform;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jdk.jfr.Category;
import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.jfr.StackTrace;
import viewpoint.platform.LiveSettings;
import zombie.ZomboidFileSystem;

public final class Capture {
    private static final LiveSettings.Number SECONDS = LiveSettings.number("capture.seconds", "Capture length (s)", "Debug/Profiling", 5.0f, 120.0f, 5.0f, 30.0f);
    private static final String DIRECTORY = "viewpoint-captures";
    private static volatile boolean requested;
    private static volatile String status;
    private static Recording recording;
    private static Path file;
    private static long lastRenderFrame;

    static void request() {
        requested = true;
    }

    static String status() {
        return status;
    }

    static void update() {
        if (recording != null) {
            Capture.follow();
        }
        if (requested) {
            requested = false;
            if (recording == null) {
                Capture.start();
            }
        }
    }

    private static void start() {
        try {
            File file = new File(ZomboidFileSystem.instance.getCacheDir(), DIRECTORY);
            file.mkdirs();
            String string = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Capture.file = new File(file, "capture-" + string + ".jfr").toPath();
            recording = new Recording(Configuration.getConfiguration("profile"));
            recording.setName("Viewpoint capture " + string);
            recording.setDuration(Duration.ofSeconds(SECONDS.getInt()));
            recording.setDestination(Capture.file);
            recording.start();
            status = "recording " + SECONDS.getInt() + " s: keep moving";
            System.out.println("[Viewpoint] capture started, " + SECONDS.getInt() + " s, to " + String.valueOf(Capture.file));
        }
        catch (Exception | LinkageError throwable) {
            recording = null;
            status = "could not start: " + throwable.getMessage();
            System.out.println("[Viewpoint] capture could not start: " + String.valueOf(throwable));
        }
    }

    private static void follow() {
        RecordingState recordingState = recording.getState();
        if (recordingState == RecordingState.STOPPED || recordingState == RecordingState.CLOSED) {
            recording.close();
            recording = null;
            status = "saved " + String.valueOf(file.getFileName()) + " (in Zomboid/viewpoint-captures)";
            System.out.println("[Viewpoint] capture saved to " + String.valueOf(file));
        } else {
            long l = (long)SECONDS.getInt() - Duration.between(recording.getStartTime(), Instant.now()).toSeconds();
            status = "recording, " + Math.max(0L, l) + " s left: keep moving";
        }
    }

    static void renderFrame(long l, double[] dArray, int n) {
        long l2 = lastRenderFrame;
        lastRenderFrame = l;
        RenderFrame renderFrame = new RenderFrame();
        if (l2 == 0L || !renderFrame.shouldCommit()) {
            return;
        }
        float f = 0.0f;
        for (double d : dArray) {
            f += (float)d;
        }
        renderFrame.frameMs = (float)(l - l2) * 1.0E-6f;
        renderFrame.oursMs = f;
        renderFrame.floors = (float)dArray[0];
        renderFrame.shadow = (float)dArray[1];
        renderFrame.gbuffer = (float)dArray[2];
        renderFrame.light = (float)dArray[3];
        renderFrame.far = (float)dArray[4];
        renderFrame.outlines = (float)dArray[5];
        renderFrame.glass = (float)dArray[6];
        renderFrame.bounce = (float)dArray[7];
        renderFrame.weather = (float)dArray[8];
        renderFrame.air = (float)dArray[9];
        renderFrame.bloom = (float)dArray[10];
        renderFrame.post = (float)dArray[11];
        renderFrame.draws = n;
        renderFrame.commit();
    }

    static void mainFrame(double d, long l, long l2, long l3, int n) {
        MainFrame mainFrame = new MainFrame();
        if (mainFrame.shouldCommit()) {
            mainFrame.frameMs = (float)d;
            mainFrame.cacheMs = (float)l * 1.0E-6f;
            mainFrame.farMs = (float)l2 * 1.0E-6f;
            mainFrame.snapshotMs = (float)l3 * 1.0E-6f;
            mainFrame.builds = n;
            mainFrame.commit();
        }
    }

    private Capture() {
    }

    static {
        status = "";
    }

    @Name(value="Viewpoint.RenderFrame")
    @Label(value="Viewpoint render frame")
    @Category(value={"Viewpoint"})
    @StackTrace(value=false)
    static final class RenderFrame
    extends Event {
        @Label(value="Frame (ms)")
        float frameMs;
        @Label(value="Ours (ms)")
        float oursMs;
        @Label(value="Floor bakes")
        float floors;
        @Label(value="Shadow")
        float shadow;
        @Label(value="G-buffer")
        float gbuffer;
        @Label(value="Sky and light")
        float light;
        @Label(value="Far world")
        float far;
        @Label(value="Outlines")
        float outlines;
        @Label(value="Glass")
        float glass;
        @Label(value="Bounce light")
        float bounce;
        @Label(value="Weather")
        float weather;
        @Label(value="Air")
        float air;
        @Label(value="Bloom")
        float bloom;
        @Label(value="Post and TAA")
        float post;
        @Label(value="Draw calls")
        int draws;

        RenderFrame() {
        }
    }

    @Name(value="Viewpoint.MainFrame")
    @Label(value="Viewpoint main frame")
    @Category(value={"Viewpoint"})
    @StackTrace(value=false)
    static final class MainFrame
    extends Event {
        @Label(value="Frame (ms)")
        float frameMs;
        @Label(value="Chunk cache (ms)")
        float cacheMs;
        @Label(value="Far world (ms)")
        float farMs;
        @Label(value="Snapshot (ms)")
        float snapshotMs;
        @Label(value="Level builds")
        int builds;

        MainFrame() {
        }
    }
}

