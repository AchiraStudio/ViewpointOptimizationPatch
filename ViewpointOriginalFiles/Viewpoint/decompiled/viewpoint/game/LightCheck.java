/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.MainThread
 */
package viewpoint.game;

import java.util.IdentityHashMap;
import java.util.Iterator;
import viewpoint.core.View;
import viewpoint.light.Visibility;
import viewpoint.platform.Caches;
import viewpoint.platform.LiveSettings;
import zombie.MainThread;
import zombie.iso.objects.IsoLightSwitch;

public final class LightCheck {
    public static final LiveSettings.Toggle KEPT = LiveSettings.toggle("game.switchPowerKept", "Light switches' power asked 4 times a second", "Game/Fixes", true);
    public static final LiveSettings.Toggle THROTTLED = LiveSettings.toggle("debug.lightCheckThrottled", "The game's light check 10 times a second", "Debug/Profiling", false);
    private static final long KEEP_NANOS = 250000000L;
    private static final long THROTTLE_NANOS = 100000000L;
    private static final long KEEP_FRAMES = 240L;
    private static final long ENTRY_BYTES = 64L;
    private static final int NONE = -1;
    private static volatile long lastCheck;
    private static final IdentityHashMap<IsoLightSwitch, Kept> kept;
    private static int answer;
    private static IsoLightSwitch asking;
    private static long nextSweep;

    public static boolean skipCheck() {
        long l = System.nanoTime();
        if (THROTTLED.get() && View.enabled && l - lastCheck < 100000000L) {
            return true;
        }
        lastCheck = l;
        return false;
    }

    public static boolean skipPower(IsoLightSwitch isoLightSwitch) {
        if (Thread.currentThread() != MainThread.mainThread) {
            return false;
        }
        answer = -1;
        asking = null;
        if (!(KEPT.get() && View.enabled && Visibility.lightingUpdate)) {
            return false;
        }
        LightCheck.sweepWhenDue();
        Kept kept = LightCheck.kept.get((Object)isoLightSwitch);
        long l = System.nanoTime();
        if (kept == null || l - kept.asked >= 250000000L) {
            asking = isoLightSwitch;
            return false;
        }
        kept.used = Caches.tick();
        answer = kept.powered ? 1 : 0;
        return true;
    }

    public static boolean power(IsoLightSwitch isoLightSwitch, boolean bl) {
        if (Thread.currentThread() != MainThread.mainThread) {
            return bl;
        }
        if (answer != -1) {
            boolean bl2 = answer == 1;
            answer = -1;
            return bl2;
        }
        if (asking == isoLightSwitch) {
            asking = null;
            LightCheck.keep(isoLightSwitch, bl);
        }
        return bl;
    }

    public static void clear() {
        kept.clear();
        answer = -1;
        asking = null;
    }

    private static void keep(IsoLightSwitch isoLightSwitch, boolean bl) {
        long l = System.nanoTime();
        Kept kept = LightCheck.kept.get((Object)isoLightSwitch);
        if (kept == null) {
            kept = new Kept();
            kept.asked = l - (long)(System.identityHashCode((Object)isoLightSwitch) & 0xFFFF) * 3814L;
            LightCheck.kept.put(isoLightSwitch, kept);
        } else {
            kept.asked = l;
        }
        kept.powered = bl;
        kept.used = Caches.tick();
    }

    private static void sweepWhenDue() {
        if (Caches.tick() >= nextSweep) {
            nextSweep = Caches.tick() + 240L;
            LightCheck.sweep(Caches.tick() - 240L);
        }
    }

    private static void sweep(long l) {
        Iterator<Kept> iterator = kept.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().used >= l) continue;
            iterator.remove();
        }
    }

    private LightCheck() {
    }

    static {
        KEPT.describe("In first person, the game's light check asks each lamp's switch whether power reaches it every frame; with the grid off that searches round every lamp. Kept for a quarter of a second, power turning on or off reaches the lamps that much later.");
        THROTTLED.describe("Measuring only: in first person, the game's whole light check at most every 0.1 s. Switches flipped and shots' flashes reach the lamps (and the zombies' sight) up to that late.");
        kept = new IdentityHashMap();
        answer = -1;
        Caches.register(new Caches.Cache(){

            @Override
            public long bytes() {
                return (long)kept.size() * 64L;
            }

            @Override
            public void list(Caches.Survey survey) {
                for (Kept kept : LightCheck.kept.values()) {
                    survey.add(kept.used, 64L);
                }
            }

            @Override
            public void evictBefore(long l) {
                LightCheck.sweep(l);
            }
        });
    }

    private static final class Kept {
        boolean powered;
        long asked;
        long used;

        private Kept() {
        }
    }
}

