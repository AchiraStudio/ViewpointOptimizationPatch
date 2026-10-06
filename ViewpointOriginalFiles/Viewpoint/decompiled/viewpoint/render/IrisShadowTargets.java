/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import viewpoint.render.IrisFormats;
import viewpoint.render.IrisTargets;

final class IrisShadowTargets {
    static final int COLORS = 8;
    int size;
    int depth;
    int depthOpaque;
    final int[] colors = new int[8];
    final IrisFormats.Format[] formats = new IrisFormats.Format[8];
    final float[][] clearColors = new float[8][];
    final boolean[] used = new boolean[8];
    final boolean[] clear = new boolean[8];
    final boolean[] mipmap = new boolean[8];
    long bytes;
    private final Map<String, Integer> fbos = new HashMap<String, Integer>();
    private int copyRead;
    private int copyDraw;

    IrisShadowTargets() {
        for (int i = 0; i < 8; ++i) {
            this.formats[i] = IrisFormats.format("RGBA8");
            this.clearColors[i] = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
            this.clear[i] = true;
        }
    }

    void ensure(int n) {
        if (n == this.size && this.depth != 0) {
            return;
        }
        GL13.glActiveTexture((int)34079);
        this.release();
        this.size = n;
        this.depth = IrisTargets.depthTexture(this.size, this.size);
        this.depthOpaque = IrisTargets.depthTexture(this.size, this.size);
        for (int n2 : new int[]{this.depth, this.depthOpaque}) {
            GL11.glBindTexture((int)3553, (int)n2);
            GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
            GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        }
        this.bytes = 2L * (long)this.size * (long)this.size * 4L;
        for (int i = 0; i < 8; ++i) {
            if (!this.used[i]) continue;
            IrisFormats.Format format = this.formats[i];
            this.colors[i] = IrisTargets.colour(format, this.size, this.size, this.mipmap[i]);
            this.bytes += (long)this.size * (long)this.size * (long)format.bytes();
        }
        GL11.glBindTexture((int)3553, (int)0);
        this.copyRead = GL30.glGenFramebuffers();
        this.copyDraw = GL30.glGenFramebuffers();
        GL13.glActiveTexture((int)33984);
    }

    int framebuffer(List<Integer> list) {
        return this.fbos.computeIfAbsent(list.toString(), string -> {
            int n = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer((int)36160, (int)n);
            GL30.glFramebufferTexture2D((int)36160, (int)36096, (int)3553, (int)this.depth, (int)0);
            IntBuffer intBuffer = BufferUtils.createIntBuffer((int)Math.max(list.size(), 1));
            for (int i = 0; i < list.size(); ++i) {
                int n2 = (Integer)list.get(i);
                if (n2 < 8 && this.colors[n2] != 0) {
                    GL30.glFramebufferTexture2D((int)36160, (int)(36064 + i), (int)3553, (int)this.colors[n2], (int)0);
                    intBuffer.put(36064 + i);
                    continue;
                }
                intBuffer.put(0);
            }
            intBuffer.flip();
            if (list.isEmpty()) {
                GL11.glDrawBuffer((int)0);
            } else {
                GL20.glDrawBuffers((IntBuffer)intBuffer);
            }
            return n;
        });
    }

    void clear() {
        for (int i = 0; i < 8; ++i) {
            if (!this.used[i] || !this.clear[i]) continue;
            GL30.glBindFramebuffer((int)36160, (int)this.framebuffer(List.of(Integer.valueOf(i))));
            float[] fArray = this.clearColors[i];
            GL11.glClearColor((float)fArray[0], (float)fArray[1], (float)fArray[2], (float)fArray[3]);
            GL11.glClear((int)16384);
        }
        GL30.glBindFramebuffer((int)36160, (int)this.framebuffer(List.of()));
        GL11.glClearDepth((double)1.0);
        GL11.glClear((int)256);
    }

    void copyOpaque() {
        GL30.glBindFramebuffer((int)36008, (int)this.copyRead);
        GL30.glFramebufferTexture2D((int)36008, (int)36096, (int)3553, (int)this.depth, (int)0);
        GL30.glBindFramebuffer((int)36009, (int)this.copyDraw);
        GL30.glFramebufferTexture2D((int)36009, (int)36096, (int)3553, (int)this.depthOpaque, (int)0);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)this.size, (int)this.size, (int)0, (int)0, (int)this.size, (int)this.size, (int)256, (int)9728);
    }

    void mipmaps() {
        GL13.glActiveTexture((int)34079);
        for (int i = 0; i < 8; ++i) {
            if (!this.used[i] || !this.mipmap[i]) continue;
            GL11.glBindTexture((int)3553, (int)this.colors[i]);
            GL30.glGenerateMipmap((int)3553);
        }
        GL11.glBindTexture((int)3553, (int)0);
        GL13.glActiveTexture((int)33984);
    }

    void release() {
        for (int n : this.fbos.values()) {
            GL30.glDeleteFramebuffers((int)n);
        }
        this.fbos.clear();
        for (int i = 0; i < 8; ++i) {
            if (this.colors[i] == 0) continue;
            GL11.glDeleteTextures((int)this.colors[i]);
            this.colors[i] = 0;
        }
        if (this.depth != 0) {
            GL11.glDeleteTextures((int)this.depth);
            GL11.glDeleteTextures((int)this.depthOpaque);
            GL30.glDeleteFramebuffers((int)this.copyRead);
            GL30.glDeleteFramebuffers((int)this.copyDraw);
        }
        this.size = 0;
        this.copyDraw = 0;
        this.copyRead = 0;
        this.depthOpaque = 0;
        this.depth = 0;
    }
}

