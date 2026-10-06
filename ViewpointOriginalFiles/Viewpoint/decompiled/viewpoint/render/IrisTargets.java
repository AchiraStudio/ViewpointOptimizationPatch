/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL44
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL44;
import viewpoint.render.IrisFormats;

final class IrisTargets {
    static final int COLORTEX = 32;
    final Buffer[] buffers = new Buffer[32];
    int width;
    int height;
    int depth;
    int depthOpaque;
    int depthNoHand;
    int farDepth;
    int farDepthOpaque;
    int tint;
    private final Map<String, Integer> fbos = new HashMap<String, Integer>();
    private int copyRead;
    private int copyDraw;
    long bytes;

    IrisTargets() {
        for (int i = 0; i < 32; ++i) {
            float[] fArray;
            this.buffers[i] = new Buffer();
            Buffer buffer = this.buffers[i];
            if (i == 1) {
                float[] fArray2 = new float[4];
                fArray2[0] = 1.0f;
                fArray2[1] = 1.0f;
                fArray2[2] = 1.0f;
                fArray = fArray2;
                fArray2[3] = 1.0f;
            } else {
                float[] fArray3 = new float[4];
                fArray3[0] = 0.0f;
                fArray3[1] = 0.0f;
                fArray3[2] = 0.0f;
                fArray = fArray3;
                fArray3[3] = i == 0 ? 1.0f : 0.0f;
            }
            buffer.clearColor = fArray;
        }
    }

    boolean ensure(int n, int n2) {
        if (n == this.width && n2 == this.height && this.depth != 0) {
            return true;
        }
        GL13.glActiveTexture((int)34079);
        this.release();
        this.width = n;
        this.height = n2;
        this.bytes = 0L;
        for (Buffer buffer : this.buffers) {
            if (!buffer.used) continue;
            buffer.width = buffer.fixed ? (int)buffer.scaleX : Math.max(1, Math.round((float)n * buffer.scaleX));
            buffer.height = buffer.fixed ? (int)buffer.scaleY : Math.max(1, Math.round((float)n2 * buffer.scaleY));
            for (int i = 0; i < 2; ++i) {
                buffer.texture[i] = IrisTargets.colour(buffer.format, buffer.width, buffer.height, buffer.mipmap);
            }
            buffer.read = 0;
            this.bytes += 2L * (long)buffer.width * (long)buffer.height * (long)buffer.format.bytes() * (long)(buffer.mipmap ? 4 : 3) / 3L;
        }
        this.depth = IrisTargets.depthTexture(n, n2);
        this.depthOpaque = IrisTargets.depthTexture(n, n2);
        this.depthNoHand = IrisTargets.depthTexture(n, n2);
        this.farDepth = IrisTargets.depthTexture(n, n2);
        this.farDepthOpaque = IrisTargets.depthTexture(n, n2);
        this.tint = IrisTargets.colour(IrisFormats.format("RGBA8"), n, n2, false);
        this.bytes += 5L * (long)n * (long)n2 * 4L + (long)n * (long)n2 * 4L;
        this.copyRead = GL30.glGenFramebuffers();
        this.copyDraw = GL30.glGenFramebuffers();
        GL13.glActiveTexture((int)33984);
        return GL11.glGetError() == 0;
    }

    int framebuffer(List<Integer> list, boolean bl, int n, int n2) {
        StringBuilder stringBuilder = new StringBuilder(bl ? "o" : "c").append(n).append(':').append(n2);
        for (int n3 : list) {
            stringBuilder.append(',').append(n3).append(bl ? this.buffers[n3].other() : this.buffers[n3].current());
        }
        return this.fbos.computeIfAbsent(stringBuilder.toString(), string -> this.make(list, bl, n, n2));
    }

    private int make(List<Integer> list, boolean bl, int n, int n2) {
        int n3;
        int n4 = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer((int)36160, (int)n4);
        int n5 = Math.max(list.size(), n2 + 1);
        IntBuffer intBuffer = BufferUtils.createIntBuffer((int)Math.max(n5, 1));
        for (n3 = 0; n3 < list.size(); ++n3) {
            Buffer buffer = this.buffers[list.get(n3)];
            int n6 = bl ? buffer.other() : buffer.current();
            GL30.glFramebufferTexture2D((int)36160, (int)(36064 + n3), (int)3553, (int)n6, (int)0);
            intBuffer.put(36064 + n3);
        }
        if (n2 >= 0) {
            GL30.glFramebufferTexture2D((int)36160, (int)(36064 + n2), (int)3553, (int)this.tint, (int)0);
            for (n3 = list.size(); n3 < n2; ++n3) {
                intBuffer.put(0);
            }
            intBuffer.put(36064 + n2);
        }
        int n7 = n == 1 ? this.depth : (n3 = n == 2 ? this.farDepth : 0);
        if (n3 != 0) {
            GL30.glFramebufferTexture2D((int)36160, (int)36096, (int)3553, (int)n3, (int)0);
        }
        intBuffer.flip();
        if (intBuffer.remaining() == 0) {
            GL11.glDrawBuffer((int)0);
        } else {
            GL20.glDrawBuffers((IntBuffer)intBuffer);
        }
        return n4;
    }

    void flip(int n) {
        Buffer buffer = this.buffers[n];
        buffer.read = 1 - buffer.read;
        this.mipmaps(n);
    }

    void mipmaps(int n) {
        GL13.glActiveTexture((int)34079);
        Buffer buffer = this.buffers[n];
        if (buffer.mipmap) {
            GL11.glBindTexture((int)3553, (int)buffer.current());
            GL30.glGenerateMipmap((int)3553);
            GL11.glBindTexture((int)3553, (int)0);
        }
        GL13.glActiveTexture((int)33984);
    }

    void clear(float f, float f2, float f3) {
        for (int i = 0; i < 32; ++i) {
            float[] fArray;
            Buffer buffer = this.buffers[i];
            if (!buffer.used || !buffer.clear) continue;
            if (i == 0 && buffer.clearColor[0] == 0.0f && buffer.clearColor[1] == 0.0f && buffer.clearColor[2] == 0.0f) {
                float[] fArray2 = new float[4];
                fArray2[0] = f;
                fArray2[1] = f2;
                fArray2[2] = f3;
                fArray = fArray2;
                fArray2[3] = 1.0f;
            } else {
                fArray = buffer.clearColor;
            }
            float[] fArray3 = fArray;
            GL30.glBindFramebuffer((int)36160, (int)this.framebuffer(List.of(Integer.valueOf(i)), false, 0, -1));
            GL11.glViewport((int)0, (int)0, (int)buffer.width, (int)buffer.height);
            if (buffer.format.integer()) {
                GL30.glClearBufferuiv((int)6144, (int)0, (int[])new int[]{(int)fArray3[0], (int)fArray3[1], (int)fArray3[2], (int)fArray3[3]});
                continue;
            }
            GL11.glClearColor((float)fArray3[0], (float)fArray3[1], (float)fArray3[2], (float)fArray3[3]);
            GL11.glClear((int)16384);
        }
    }

    void copyDepth(int n, int n2) {
        GL30.glBindFramebuffer((int)36008, (int)this.copyRead);
        GL30.glFramebufferTexture2D((int)36008, (int)36096, (int)3553, (int)n, (int)0);
        GL30.glBindFramebuffer((int)36009, (int)this.copyDraw);
        GL30.glFramebufferTexture2D((int)36009, (int)36096, (int)3553, (int)n2, (int)0);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)this.width, (int)this.height, (int)0, (int)0, (int)this.width, (int)this.height, (int)256, (int)9728);
    }

    void release() {
        for (int n : this.fbos.values()) {
            GL30.glDeleteFramebuffers((int)n);
        }
        this.fbos.clear();
        for (Buffer buffer : this.buffers) {
            for (int i = 0; i < 2; ++i) {
                if (buffer.texture[i] == 0) continue;
                GL11.glDeleteTextures((int)buffer.texture[i]);
                buffer.texture[i] = 0;
            }
        }
        for (int n : new int[]{this.depth, this.depthOpaque, this.depthNoHand, this.farDepth, this.farDepthOpaque, this.tint}) {
            if (n == 0) continue;
            GL11.glDeleteTextures((int)n);
        }
        if (this.copyRead != 0) {
            GL30.glDeleteFramebuffers((int)this.copyRead);
            GL30.glDeleteFramebuffers((int)this.copyDraw);
        }
        this.copyDraw = 0;
        this.copyRead = 0;
        this.tint = 0;
        this.farDepthOpaque = 0;
        this.farDepth = 0;
        this.depthNoHand = 0;
        this.depthOpaque = 0;
        this.depth = 0;
        this.height = 0;
        this.width = 0;
    }

    static int colour(IrisFormats.Format format, int n, int n2, boolean bl) {
        int n3 = GL11.glGenTextures();
        GL11.glBindTexture((int)3553, (int)n3);
        GL11.glTexImage2D((int)3553, (int)0, (int)format.internal(), (int)n, (int)n2, (int)0, (int)format.pixels(), (int)format.type(), (ByteBuffer)null);
        GL44.glClearTexImage((int)n3, (int)0, (int)format.pixels(), (int)format.type(), (ByteBuffer)null);
        int n4 = format.integer() ? 9728 : 9729;
        GL11.glTexParameteri((int)3553, (int)10241, (int)(bl && !format.integer() ? 9987 : n4));
        GL11.glTexParameteri((int)3553, (int)10240, (int)n4);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        if (bl) {
            GL30.glGenerateMipmap((int)3553);
        }
        GL11.glBindTexture((int)3553, (int)0);
        return n3;
    }

    static int depthTexture(int n, int n2) {
        int n3 = GL11.glGenTextures();
        GL11.glBindTexture((int)3553, (int)n3);
        GL11.glTexImage2D((int)3553, (int)0, (int)36012, (int)n, (int)n2, (int)0, (int)6402, (int)5126, (ByteBuffer)null);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glTexParameteri((int)3553, (int)34892, (int)0);
        GL11.glBindTexture((int)3553, (int)0);
        return n3;
    }

    static final class Buffer {
        final int[] texture = new int[2];
        int read;
        IrisFormats.Format format = IrisFormats.format("RGBA8");
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        boolean fixed;
        int width;
        int height;
        boolean clear = true;
        float[] clearColor;
        boolean mipmap;
        boolean used;

        Buffer() {
        }

        int current() {
            return this.texture[this.read];
        }

        int other() {
            return this.texture[1 - this.read];
        }
    }
}

