/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL43
 *  org.lwjgl.opengl.GLCapabilities
 *  org.lwjgl.opengl.GLDebugMessageCallback
 *  org.lwjgl.opengl.GLDebugMessageCallbackI
 */
package viewpoint.platform;

import java.util.ArrayDeque;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.GLDebugMessageCallbackI;

public final class GlDebug {
    private static final int MAX_LOGGED = 20;
    private static GLDebugMessageCallback callback;
    private static boolean groups;
    private static boolean listening;
    private static int logged;
    private static final ArrayDeque<String> labels;
    public static volatile int errors;

    public static void install() {
        GLCapabilities gLCapabilities = GL.getCapabilities();
        if (!gLCapabilities.OpenGL43 && !gLCapabilities.GL_KHR_debug) {
            System.out.println("[Viewpoint] GL debug output not available on this driver");
            return;
        }
        groups = true;
        if (callback != null) {
            return;
        }
        callback = GLDebugMessageCallback.create((n, n2, n3, n4, n5, l, l2) -> {
            if (!listening || n2 != 33356 && n4 != 37190) {
                return;
            }
            ++errors;
            if (logged++ < 20) {
                System.out.println("[Viewpoint] GL error in " + (labels.isEmpty() ? "(no pass)" : String.join((CharSequence)"/", labels)) + ": " + GLDebugMessageCallback.getMessage((int)n5, (long)l));
            }
        });
        GL43.glDebugMessageCallback((GLDebugMessageCallbackI)callback, (long)0L);
        GL11.glEnable((int)37600);
        GL11.glEnable((int)33346);
        System.out.println("[Viewpoint] GL debug output on");
    }

    public static void listen(boolean bl) {
        listening = bl;
    }

    public static void push(String string) {
        labels.addLast(string);
        if (groups) {
            GL43.glPushDebugGroup((int)33354, (int)0, (CharSequence)string);
        }
    }

    public static void pop() {
        if (labels.pollLast() != null && groups) {
            GL43.glPopDebugGroup();
        }
    }

    public static void popAll() {
        while (!labels.isEmpty()) {
            GlDebug.pop();
        }
    }

    private GlDebug() {
    }

    static {
        labels = new ArrayDeque();
    }
}

