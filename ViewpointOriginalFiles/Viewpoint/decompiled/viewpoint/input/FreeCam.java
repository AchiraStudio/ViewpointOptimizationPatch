/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjglx.input.Keyboard
 */
package viewpoint.input;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import org.lwjglx.input.Keyboard;
import viewpoint.core.Frame;
import viewpoint.core.View;
import viewpoint.input.Camera;
import viewpoint.input.Look;
import viewpoint.platform.KeyBind;
import viewpoint.platform.KeyChord;
import viewpoint.platform.Keys;

public final class FreeCam {
    public static volatile boolean active;
    public static volatile boolean physical;
    public static volatile Place place;
    static volatile int wheel;
    private static final int NONE = 0;
    private static final int FREE = 1;
    private static final int PHYSICAL = 2;
    private static final AtomicInteger wanted;
    private static final AtomicBoolean home;
    private static final float BASE_SPEED = 12.0f;
    private static final float FAST = 4.0f;
    private static final float MIN_SPEED = 1.0f;
    private static final float MAX_SPEED = 400.0f;
    private static final float SENSITIVITY = 0.0022f;
    private static final float MAX_PITCH;
    private static double x;
    private static double y;
    private static double z;
    private static float yaw;
    private static float pitch;
    private static float speed;
    private static boolean starting;
    private static int[] ignored;
    private static long lastNanos;

    public static Place centre() {
        Place place = FreeCam.place;
        return active && physical ? place : null;
    }

    public static void keys() {
        if (Keys.PHYSICAL_CAMERA.pressed()) {
            wanted.set(2);
        } else if (Keys.FREE_CAMERA.pressed()) {
            wanted.set(1);
        }
        if (active && Keys.CAMERA_HOME.pressed()) {
            home.set(true);
        }
    }

    static void poll() {
        int n = wanted.getAndSet(0);
        if (n != 0 && View.enabled) {
            FreeCam.switchTo(!active || !physical && n != 1, n == 2);
        }
        if (!View.enabled) {
            physical = false;
            active = false;
        }
        if (home.getAndSet(false) && active) {
            starting = true;
        }
        long l = System.nanoTime();
        float f = lastNanos == 0L ? 0.0f : Math.min(0.1f, (float)(l - lastNanos) * 1.0E-9f);
        lastNanos = l;
        if (!active) {
            wheel = 0;
            return;
        }
        FreeCam.fly(f);
    }

    private static void fly(float f) {
        int n;
        for (n = 0; n < ignored.length; ++n) {
            if (ignored[n] == 0 || Keyboard.isKeyDown((int)ignored[n])) continue;
            FreeCam.ignored[n] = 0;
        }
        n = wheel;
        wheel = 0;
        speed = Math.max(1.0f, Math.min(400.0f, speed * (float)Math.pow(1.25, n)));
        IntPredicate intPredicate = FreeCam::flies;
        float f2 = speed * f * (Keys.CAMERA_FAST.down(intPredicate) ? 4.0f : 1.0f);
        float f3 = FreeCam.axis(Keys.CAMERA_FORWARD, Keys.CAMERA_BACK, intPredicate);
        float f4 = FreeCam.axis(Keys.CAMERA_RIGHT, Keys.CAMERA_LEFT, intPredicate);
        float f5 = FreeCam.axis(Keys.CAMERA_UP, Keys.CAMERA_DOWN, intPredicate);
        double d = Math.cos(yaw);
        double d2 = Math.sin(yaw);
        double d3 = Math.cos(pitch);
        double d4 = Math.sin(pitch);
        x += (d * d3 * (double)f3 - d2 * (double)f4) * (double)f2;
        y += (d2 * d3 * (double)f3 + d * (double)f4) * (double)f2;
        z += (d4 * (double)f3 + (double)f5) * (double)f2 / 2.4494895935058594;
    }

    private static float axis(KeyBind keyBind, KeyBind keyBind2, IntPredicate intPredicate) {
        return (keyBind.down(intPredicate) ? 1.0f : 0.0f) - (keyBind2.down(intPredicate) ? 1.0f : 0.0f);
    }

    private static boolean flies(int n) {
        for (int n2 : ignored) {
            if (n2 != n) continue;
            return false;
        }
        return Keyboard.isKeyDown((int)n);
    }

    private static void switchTo(boolean bl, boolean bl2) {
        if (bl && !active) {
            starting = true;
            place = null;
        }
        physical = bl && bl2;
        active = bl;
        int[] nArray = bl ? FreeCam.held(bl2 ? Keys.PHYSICAL_CAMERA : Keys.FREE_CAMERA) : (ignored = new int[]{});
        System.out.println("[Viewpoint] " + (String)(bl ? (physical ? "physical free camera on (the world centred on it; " : "free camera on (") + "mouse: look; " + Keys.CAMERA_FORWARD.display() + " " + Keys.CAMERA_LEFT.display() + " " + Keys.CAMERA_BACK.display() + " " + Keys.CAMERA_RIGHT.display() + ", up " + Keys.CAMERA_UP.display() + ", down " + Keys.CAMERA_DOWN.display() + ", fast " + Keys.CAMERA_FAST.display() + ", wheel speed; back to the player: " + Keys.CAMERA_HOME.display() + "; off: " + Keys.FREE_CAMERA.display() + " or " + Keys.PHYSICAL_CAMERA.display() + ")" : "free camera off"));
    }

    private static int[] held(KeyBind keyBind) {
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (KeyChord keyChord : keyBind.chords()) {
            for (int n : keyChord.keys()) {
                if (!Keyboard.isKeyDown((int)n) || arrayList.contains(n)) continue;
                arrayList.add(n);
            }
        }
        return arrayList.stream().mapToInt(Integer::intValue).toArray();
    }

    static boolean look(double d, double d2) {
        if (!active) {
            return false;
        }
        yaw = (double)(yaw += (float)d * 0.0022f) > Math.PI ? yaw - (float)Math.PI * 2 : ((double)yaw < -Math.PI ? yaw + (float)Math.PI * 2 : yaw);
        pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch - (float)d2 * 0.0022f));
        return true;
    }

    public static boolean view(Frame frame, float f, float f2, float f3, float[] fArray, float[] fArray2) {
        Place place;
        if (!active) {
            return false;
        }
        if (starting) {
            starting = false;
            x = frame.camX - f;
            y = frame.camY - f3;
            z = frame.camZ + f2 / 2.4494896f;
            yaw = Look.yaw;
            pitch = Look.pitch;
        }
        FreeCam.place = place = new Place(x, y, z, yaw, pitch);
        Camera.at(frame, place, fArray, fArray2);
        return true;
    }

    private FreeCam() {
    }

    static {
        wanted = new AtomicInteger(0);
        home = new AtomicBoolean();
        MAX_PITCH = (float)Math.toRadians(89.0);
        speed = 12.0f;
        ignored = new int[0];
    }

    public record Place(double x, double y, double z, float yaw, float pitch) {
    }
}

