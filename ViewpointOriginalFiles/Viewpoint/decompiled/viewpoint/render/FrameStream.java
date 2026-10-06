/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL44
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL44;
import viewpoint.render.Retirement;

final class FrameStream {
    private static final long START_BYTES = 0x800000L;
    private static int buffer;
    private static int textureAlignment;
    private static long size;
    private static long head;
    private static long frameBytes;

    static int buffer() {
        return buffer;
    }

    static void frameStarted() {
        frameBytes = 0L;
    }

    static long put(FloatBuffer floatBuffer, int n) {
        long l = FrameStream.reserve((long)floatBuffer.remaining() * 4L, n);
        GL15.glBindBuffer((int)36663, (int)buffer);
        GL15.glBufferSubData((int)36663, (long)l, (FloatBuffer)floatBuffer);
        GL15.glBindBuffer((int)36663, (int)0);
        return l;
    }

    static long put(IntBuffer intBuffer, int n) {
        long l = FrameStream.reserve((long)intBuffer.remaining() * 4L, n);
        GL15.glBindBuffer((int)36663, (int)buffer);
        GL15.glBufferSubData((int)36663, (long)l, (IntBuffer)intBuffer);
        GL15.glBindBuffer((int)36663, (int)0);
        return l;
    }

    static long put(ByteBuffer byteBuffer, int n) {
        long l = FrameStream.reserve(byteBuffer.remaining(), n);
        GL15.glBindBuffer((int)36663, (int)buffer);
        GL15.glBufferSubData((int)36663, (long)l, (ByteBuffer)byteBuffer);
        GL15.glBindBuffer((int)36663, (int)0);
        return l;
    }

    static int textureAlignment() {
        if (textureAlignment == 0) {
            textureAlignment = Math.max(4, GL11.glGetInteger((int)37279));
        }
        return textureAlignment;
    }

    private static long reserve(long l, int n) {
        long l2;
        long l3 = frameBytes + l + (long)n;
        if (buffer == 0 || l3 * 4L > size) {
            FrameStream.grow(Math.max(0x800000L, Long.highestOneBit(l3 * 4L) * 2L));
        }
        if ((l2 = (head + (long)n - 1L) / (long)n * (long)n) + l > size) {
            l2 = 0L;
        }
        head = l2 + l;
        frameBytes += l + (long)n;
        return l2;
    }

    private static void grow(long l) {
        if (buffer != 0) {
            int n = buffer;
            Retirement.retireDrawing(() -> GL15.glDeleteBuffers((int)n));
            System.out.println("[Viewpoint] frame stream grown to " + (l >> 20) + " MB");
        }
        buffer = GL15.glGenBuffers();
        GL15.glBindBuffer((int)36663, (int)buffer);
        GL44.glBufferStorage((int)36663, (long)l, (int)256);
        GL15.glBindBuffer((int)36663, (int)0);
        size = l;
        head = 0L;
    }

    private FrameStream() {
    }
}

