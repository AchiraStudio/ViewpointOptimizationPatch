/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import viewpoint.platform.ShaderPacks;
import viewpoint.platform.Shaders;

final class ShaderWatch {
    private static final long PERIOD_MS = 1000L;
    private static volatile Set<String> files = Set.of();
    private static volatile Map<String, Long> stamps = Map.of();
    private static boolean started;

    static void watch(Set<String> set) {
        files = Set.copyOf(set);
        if (!started) {
            started = true;
            Thread thread = new Thread(ShaderWatch::run, "Viewpoint shader watch");
            thread.setDaemon(true);
            thread.start();
        }
    }

    static Map<String, Long> stamps() {
        return stamps;
    }

    private static void run() {
        try {
            while (true) {
                HashMap<String, Long> hashMap = new HashMap<String, Long>();
                ShaderPacks.Active active = ShaderPacks.drawn();
                for (String string : files) {
                    hashMap.put(string, Shaders.modified(string, active));
                }
                stamps = hashMap;
                Thread.sleep(1000L);
            }
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    private ShaderWatch() {
    }
}

