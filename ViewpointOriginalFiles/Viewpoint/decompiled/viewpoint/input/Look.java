/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.glfw.GLFW
 */
package viewpoint.input;

import org.lwjgl.glfw.GLFW;
import org.lwjglx.opengl.Display;
import viewpoint.input.FreeCam;
import viewpoint.platform.LiveSettings;

public final class Look {
    public static volatile float yaw;
    public static volatile float pitch;
    public static volatile boolean wantCapture;
    public static volatile boolean captured;
    private static final String SECTION = "Controls/Mouse";
    private static final LiveSettings.Number SPEED;
    private static final LiveSettings.Toggle INVERT_Y;
    private static final LiveSettings.Toggle FRESH;
    private static final float SENSITIVITY = 0.0022f;
    private static final float MAX_PITCH;
    private static final int FRAMES_WITHOUT_VIEW = 3;
    private static final double[] px;
    private static final double[] py;
    public static long readNanos;
    private static boolean hasLast;
    private static double lastX;
    private static double lastY;
    private static int framesWithoutView;

    public static void viewDrawn() {
        framesWithoutView = 0;
    }

    public static void framePresented() {
        framesWithoutView = Math.min(framesWithoutView + 1, 4);
    }

    public static boolean onUpdateMouseCursor() {
        boolean bl;
        long l = Display.getWindow();
        FreeCam.poll();
        boolean bl2 = bl = wantCapture && framesWithoutView <= 3 && GLFW.glfwGetWindowAttrib((long)l, (int)131073) == 1;
        if (!bl) {
            if (captured) {
                captured = false;
                GLFW.glfwSetInputMode((long)l, (int)208897, (int)212993);
                GLFW.glfwSetCursorPos((long)l, (double)((double)Display.getWidth() / 2.0), (double)((double)Display.getHeight() / 2.0));
            }
            return false;
        }
        if (!captured || GLFW.glfwGetInputMode((long)l, (int)208897) != 212995) {
            GLFW.glfwSetInputMode((long)l, (int)208897, (int)212995);
            if (GLFW.glfwRawMouseMotionSupported()) {
                GLFW.glfwSetInputMode((long)l, (int)208901, (int)1);
            }
            captured = true;
            hasLast = false;
        }
        Look.read(l);
        return true;
    }

    public static void readBeforeDrawing() {
        if (!captured || !FRESH.get()) {
            return;
        }
        GLFW.glfwPollEvents();
        Look.read(Display.getWindow());
    }

    private static void read(long l) {
        GLFW.glfwGetCursorPos((long)l, (double[])px, (double[])py);
        readNanos = System.nanoTime();
        double d = SPEED.get();
        double d2 = (px[0] - lastX) * d;
        double d3 = (py[0] - lastY) * d;
        if (INVERT_Y.get()) {
            d3 = -d3;
        }
        if (hasLast && !FreeCam.look(d2, d3)) {
            yaw = Look.wrap(yaw + (float)d2 * 0.0022f);
            pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch - (float)d3 * 0.0022f));
        }
        lastX = px[0];
        lastY = py[0];
        hasLast = true;
    }

    static float wrap(float f) {
        float f2 = (float)Math.PI * 2;
        return (double)f > Math.PI ? f - f2 : ((double)f < -Math.PI ? f + f2 : f);
    }

    private Look() {
    }

    static {
        SPEED = LiveSettings.number("mouse.sensitivity", "Sensitivity", SECTION, 0.1f, 4.0f, 0.05f, 1.0f);
        INVERT_Y = LiveSettings.toggle("mouse.invertY", "Invert vertical look", SECTION, false);
        FRESH = LiveSettings.toggle("mouse.readBeforeDrawing", "Read the mouse just before drawing", SECTION, true);
        SPEED.describe("How far the view turns for a move of the mouse: 1 is the mod's own speed.");
        INVERT_Y.describe("Moving the mouse forward looks down, and back looks up.");
        FRESH.describe("The view turns with the mouse as it is when the frame is drawn, not as it was a frame before: less lag, most when the game holds the frame rate back.");
        MAX_PITCH = (float)Math.toRadians(85.0);
        px = new double[1];
        py = new double[1];
        framesWithoutView = 4;
    }
}

