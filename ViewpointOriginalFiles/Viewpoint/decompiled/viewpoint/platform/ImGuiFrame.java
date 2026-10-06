/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  imgui.ImGui
 *  imgui.ImGuiIO
 *  imgui.gl3.ImGuiImplGl3
 *  imgui.internal.ImGuiContext
 *  org.lwjgl.glfw.GLFW
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL30
 *  zombie.core.Core
 */
package viewpoint.platform;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imgui.internal.ImGuiContext;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjglx.opengl.Display;
import zombie.core.Core;

final class ImGuiFrame {
    private static ImGuiContext context;
    private static ImGuiImplGl3 backend;
    private static volatile boolean failed;
    private static double lastTime;
    private static final int[] w;
    private static final int[] h;
    private static final int[] fw;
    private static final int[] fh;
    private static final double[] cx;
    private static final double[] cy;
    static volatile boolean wantsMouse;

    static boolean available() {
        return !failed && !Core.isImGui();
    }

    static void draw(Contents contents) {
        if (failed || Core.isImGui()) {
            wantsMouse = false;
            return;
        }
        ImGuiContext imGuiContext = ImGui.getCurrentContext();
        try {
            if (context == null) {
                ImGuiFrame.create();
            } else {
                ImGui.setCurrentContext((ImGuiContext)context);
            }
            ImGuiFrame.input();
            ImGui.newFrame();
            contents.draw();
            ImGui.render();
            wantsMouse = ImGui.getIO().getWantCaptureMouse();
            int n = GL11.glGetInteger((int)36006);
            GL30.glBindFramebuffer((int)36009, (int)0);
            backend.renderDrawData(ImGui.getDrawData());
            GL30.glBindFramebuffer((int)36009, (int)n);
        }
        catch (LinkageError | RuntimeException throwable) {
            failed = true;
            wantsMouse = false;
            System.out.println("[Viewpoint] settings window off after an error: " + String.valueOf(throwable));
        }
        finally {
            if (imGuiContext != null && imGuiContext.ptr != 0L) {
                ImGui.setCurrentContext((ImGuiContext)imGuiContext);
            }
        }
    }

    private static void create() {
        context = ImGui.createContext();
        ImGui.setCurrentContext((ImGuiContext)context);
        ImGuiIO imGuiIO = ImGui.getIO();
        imGuiIO.setIniFilename(null);
        ImGui.styleColorsDark();
        backend = new ImGuiImplGl3();
        backend.init("#version 130");
        lastTime = GLFW.glfwGetTime();
        System.out.println("[Viewpoint] settings window: ImGui ready");
    }

    private static void input() {
        long l = Display.getWindow();
        ImGuiIO imGuiIO = ImGui.getIO();
        GLFW.glfwGetWindowSize((long)l, (int[])w, (int[])h);
        GLFW.glfwGetFramebufferSize((long)l, (int[])fw, (int[])fh);
        imGuiIO.setDisplaySize((float)w[0], (float)h[0]);
        if (w[0] > 0 && h[0] > 0) {
            imGuiIO.setDisplayFramebufferScale((float)fw[0] / (float)w[0], (float)fh[0] / (float)h[0]);
        }
        imGuiIO.setFontGlobalScale(Math.max(1.0f, (float)h[0] / 1080.0f));
        double d = GLFW.glfwGetTime();
        imGuiIO.setDeltaTime((float)Math.max(1.0E-4, d - lastTime));
        lastTime = d;
        boolean bl = GLFW.glfwGetInputMode((long)l, (int)208897) != 212995;
        GLFW.glfwGetCursorPos((long)l, (double[])cx, (double[])cy);
        imGuiIO.setMousePos(bl ? (float)cx[0] : -3.4028235E38f, bl ? (float)cy[0] : -3.4028235E38f);
        for (int i = 0; i < 3; ++i) {
            imGuiIO.setMouseDown(i, bl && GLFW.glfwGetMouseButton((long)l, (int)i) == 1);
        }
    }

    private ImGuiFrame() {
    }

    static {
        w = new int[1];
        h = new int[1];
        fw = new int[1];
        fh = new int[1];
        cx = new double[1];
        cy = new double[1];
    }

    static interface Contents {
        public void draw();
    }
}

