/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector4f
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL32
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import viewpoint.render.FrameContext;

public final class MousePick {
    public static volatile Ask ask;
    public static volatile Hit hit;
    public static volatile Ask aim;
    public static volatile Hit aimHit;
    private static final FloatBuffer depth;
    private static final Matrix4f inverse;
    private static final Vector4f point;
    private static int buffer;
    private static long fence;
    private static Ask reading;
    private static boolean readingAim;
    private static float pixelX;
    private static float pixelY;
    private static float width;
    private static float height;
    private static float eyeX;
    private static float eyeY;
    private static float eyeZ;
    private static double originX;
    private static double originY;
    private static double originZ;

    static void read(FrameContext frameContext, int n, Matrix4f matrix4f) {
        Ask ask;
        MousePick.collect();
        Ask ask2 = MousePick.ask;
        Ask ask3 = aim;
        if (ask2 == null) {
            hit = null;
        }
        if (ask3 == null) {
            aimHit = null;
        }
        Ask ask4 = ask = ask2 != null ? ask2 : ask3;
        if (ask == null) {
            return;
        }
        if (fence != 0L || ask.screenWidth() <= 0 || ask.screenHeight() <= 0) {
            return;
        }
        width = frameContext.width;
        height = frameContext.height;
        int n2 = (int)Math.min(width - 1.0f, (float)Math.max(0L, (long)ask.x() * (long)frameContext.width / (long)ask.screenWidth()));
        int n3 = (int)Math.min(height - 1.0f, (float)Math.max(0L, (long)(frameContext.height - 1) - (long)ask.y() * (long)frameContext.height / (long)ask.screenHeight()));
        pixelX = (float)n2 + 0.5f;
        pixelY = (float)n3 + 0.5f;
        inverse.set((Matrix4fc)matrix4f);
        originX = frameContext.scene.originX;
        originY = frameContext.scene.originY;
        originZ = frameContext.scene.originZ;
        eyeX = frameContext.eyeX;
        eyeY = frameContext.eyeY;
        eyeZ = frameContext.eyeZ;
        reading = ask;
        boolean bl = readingAim = ask2 == null;
        if (buffer == 0) {
            buffer = GL15.glGenBuffers();
            GL15.glBindBuffer((int)35051, (int)buffer);
            GL15.glBufferData((int)35051, (long)4L, (int)35041);
        }
        GL30.glBindFramebuffer((int)36008, (int)n);
        GL15.glBindBuffer((int)35051, (int)buffer);
        GL11.glReadPixels((int)n2, (int)n3, (int)1, (int)1, (int)6402, (int)5126, (long)0L);
        GL15.glBindBuffer((int)35051, (int)0);
        fence = GL32.glFenceSync((int)37143, (int)0);
    }

    private static void collect() {
        if (fence == 0L) {
            return;
        }
        int n = GL32.glClientWaitSync((long)fence, (int)0, (long)0L);
        if (n != 37146 && n != 37148) {
            return;
        }
        GL32.glDeleteSync((long)fence);
        fence = 0L;
        GL15.glBindBuffer((int)35051, (int)buffer);
        depth.clear();
        GL15.glGetBufferSubData((int)35051, (long)0L, (FloatBuffer)depth);
        GL15.glBindBuffer((int)35051, (int)0);
        float f = depth.get(0);
        if (f >= 1.0f) {
            MousePick.publish(null);
            return;
        }
        point.set(pixelX / width * 2.0f - 1.0f, pixelY / height * 2.0f - 1.0f, f * 2.0f - 1.0f, 1.0f);
        inverse.transform(point);
        float f2 = MousePick.point.x / MousePick.point.w;
        float f3 = MousePick.point.y / MousePick.point.w;
        float f4 = MousePick.point.z / MousePick.point.w;
        float f5 = f2 - eyeX;
        float f6 = f3 - eyeY;
        float f7 = f4 - eyeZ;
        float f8 = (float)Math.sqrt(f5 * f5 + f6 * f6 + f7 * f7);
        if (f8 <= 0.0f) {
            return;
        }
        MousePick.publish(new Hit(reading, -((double)f2 + originX), -((double)f4 + originZ), ((double)f3 + originY) / 2.4494895935058594, -f5 / f8, -f7 / f8, f6 / f8));
    }

    private static void publish(Hit hit) {
        if (readingAim) {
            aimHit = hit;
        } else {
            MousePick.hit = hit;
        }
    }

    private MousePick() {
    }

    static {
        depth = BufferUtils.createFloatBuffer((int)1);
        inverse = new Matrix4f();
        point = new Vector4f();
    }

    public record Ask(int x, int y, int screenWidth, int screenHeight) {
    }

    public record Hit(Ask ask, double x, double y, double z, float dirX, float dirY, float dirUp) {
    }
}

