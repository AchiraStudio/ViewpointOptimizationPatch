/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import viewpoint.platform.PerformanceOverlay;

final class GpuBusy {
    private static final int RUN_SAMPLES = 30;
    private static volatile float busy = Float.NaN;
    private static volatile boolean started;

    static float percent() {
        if (!started) {
            started = true;
            if (System.getProperty("os.name", "").startsWith("Windows")) {
                Thread thread = new Thread(GpuBusy::run, "Viewpoint overlay GPU");
                thread.setDaemon(true);
                thread.start();
            }
        }
        return busy;
    }

    private static void run() {
        String string = "\\GPU Engine(pid_" + ProcessHandle.current().pid() + "_*engtype_3D)\\Utilization Percentage";
        try {
            while (true) {
                if (PerformanceOverlay.mode() == PerformanceOverlay.Mode.OFF) {
                    busy = Float.NaN;
                    Thread.sleep(1000L);
                    continue;
                }
                if (!GpuBusy.sample(string) && Float.isNaN(busy) && PerformanceOverlay.mode() != PerformanceOverlay.Mode.OFF) break;
            }
            System.out.println("[Viewpoint] overlay: the GPU's busy share cannot be read (typeperf " + string + " gave nothing)");
            return;
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static boolean sample(String string) throws InterruptedException {
        Process process;
        ProcessBuilder processBuilder = new ProcessBuilder("typeperf", string, "-si", "1", "-sc", Integer.toString(30));
        processBuilder.redirectErrorStream(true);
        boolean bl = false;
        try {
            process = processBuilder.start();
        }
        catch (IOException iOException) {
            return false;
        }
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));){
            String string2 = bufferedReader.readLine();
            while (string2 != null) {
                float f = GpuBusy.sum(string2);
                if (!Float.isNaN(f)) {
                    busy = Math.min(100.0f, f);
                    bl = true;
                }
                if (PerformanceOverlay.mode() == PerformanceOverlay.Mode.OFF) {
                    break;
                }
                string2 = bufferedReader.readLine();
            }
        }
        catch (IOException iOException) {
        }
        finally {
            process.destroy();
            process.waitFor();
        }
        return bl;
    }

    static float sum(String string) {
        String[] stringArray = string.split("\",\"");
        if (stringArray.length < 2 || !string.startsWith("\"") || string.startsWith("\"(PDH")) {
            return Float.NaN;
        }
        float f = 0.0f;
        for (int i = 1; i < stringArray.length; ++i) {
            String string2 = stringArray[i].replace("\"", "").trim();
            try {
                f += string2.isEmpty() ? 0.0f : Float.parseFloat(string2);
                continue;
            }
            catch (NumberFormatException numberFormatException) {
                return Float.NaN;
            }
        }
        return f;
    }

    private GpuBusy() {
    }
}

