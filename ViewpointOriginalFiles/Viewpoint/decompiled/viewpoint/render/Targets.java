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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.render.Noise2D;
import viewpoint.render.Noise3D;
import viewpoint.render.SceneData;

public final class Targets {
    static final int BLOOM_LEVELS = 5;
    int width;
    int height;
    int halfWidth;
    int halfHeight;
    public int albedo;
    public int normal;
    public int engineLight;
    public int depth;
    int velocity;
    int hdr;
    int translucentSum;
    int translucentReveal;
    int translucentFbo;
    int gbufferFbo;
    int hdrFbo;
    int lightFbo;
    public int farDepth;
    public int farFbo;
    int gi;
    int giSpare;
    int volume;
    int volumeSpare;
    int giFbo;
    int giSpareFbo;
    int volumeFbo;
    int volumeSpareFbo;
    int clouds;
    int cloudFbo;
    int cloudShare;
    int cloudWidth;
    int cloudHeight;
    int taaInput;
    int taaInputFbo;
    int rain;
    int rainFbo;
    final int[] history = new int[2];
    final int[] historyFbo = new int[2];
    final int[] bloom = new int[5];
    final int[] bloomFbo = new int[5];
    final int[] bloomWidth = new int[5];
    final int[] bloomHeight = new int[5];
    int exposure;
    int rooms;
    int water;
    int noise;
    int noise2d;
    private final ArrayList<Integer> textures = new ArrayList();
    private final ArrayList<Integer> framebuffers = new ArrayList();
    private final StringBuilder incomplete = new StringBuilder();
    private static final IntBuffer drawBuffers = BufferUtils.createIntBuffer((int)4);
    private static final ByteBuffer gridBuffer = BufferUtils.createByteBuffer((int)4096);
    private long gridsFrame = -1L;

    void createFixed() {
        int n = GL11.glGetInteger((int)32873);
        this.exposure = Gl.texture(33321, 64, 64, 6403, 5121, 9729);
        this.rooms = Gl.texture(33321, 64, 64, 6403, 5121, 9728);
        this.water = Gl.texture(33321, 64, 64, 6403, 5121, 9729);
        this.noise2d = Noise2D.create();
        GL11.glBindTexture((int)3553, (int)n);
        this.noise = Noise3D.create();
    }

    void velocity(boolean bl) {
        drawBuffers.clear();
        drawBuffers.put(36064).put(36065).put(36066);
        drawBuffers.put(bl ? 36067 : 0).flip();
        GL20.glDrawBuffers((IntBuffer)drawBuffers);
    }

    boolean ensure(int n, int n2, int n3) {
        if (this.gbufferFbo != 0 && n == this.width && n2 == this.height && n3 == this.cloudShare) {
            return true;
        }
        this.delete();
        int n4 = GL11.glGetInteger((int)32873);
        this.albedo = this.texture(32856, n, n2, 6408, 5121, 9728);
        this.normal = this.texture(34842, n, n2, 6408, 5126, 9728);
        this.engineLight = this.texture(32856, n, n2, 6408, 5121, 9728);
        this.depth = this.texture(35056, n, n2, 34041, 34042, 9728);
        this.hdr = this.texture(34842, n, n2, 6408, 5126, 9729);
        this.velocity = this.texture(34842, n, n2, 6408, 5126, 9728);
        this.gbufferFbo = this.framebuffer("g-buffer", this.depth, this.albedo, this.normal, this.engineLight, this.velocity);
        this.velocity(false);
        this.hdrFbo = this.framebuffer("hdr", this.depth, this.hdr);
        this.lightFbo = this.framebuffer("light", 0, this.hdr);
        this.translucentSum = this.texture(34842, n, n2, 6408, 5126, 9728);
        this.translucentReveal = this.texture(33325, n, n2, 6403, 5126, 9728);
        this.translucentFbo = this.framebuffer("translucent", this.depth, this.translucentSum, this.translucentReveal);
        this.farDepth = this.texture(35056, n, n2, 34041, 34042, 9728);
        this.farFbo = this.framebuffer("far", this.farDepth, this.hdr);
        this.makeReduced(n, n2, n3);
        this.taaInput = this.texture(34842, n, n2, 6408, 5126, 9729);
        this.taaInputFbo = this.framebuffer("taa input", 0, this.taaInput);
        this.rain = this.texture(32856, n, n2, 6408, 5121, 9728);
        this.rainFbo = this.framebuffer("rain", this.depth, this.rain);
        for (int i = 0; i < 2; ++i) {
            this.history[i] = this.texture(34842, n, n2, 6408, 5126, 9729);
            this.historyFbo[i] = this.framebuffer("taa history " + i, 0, this.history[i]);
        }
        this.makeBloomChain(n, n2);
        GL11.glBindTexture((int)3553, (int)n4);
        this.width = n;
        this.height = n2;
        if (this.incomplete.length() > 0) {
            System.out.println("[Viewpoint] framebuffers incomplete:" + String.valueOf(this.incomplete));
            return false;
        }
        return true;
    }

    private void makeReduced(int n, int n2, int n3) {
        int n4 = Math.max(1, n / 2);
        int n5 = Math.max(1, n2 / 2);
        this.gi = this.half(n4, n5);
        this.giFbo = this.framebuffer("gi", 0, this.gi);
        this.giSpare = this.half(n4, n5);
        this.giSpareFbo = this.framebuffer("gi spare", 0, this.giSpare);
        this.volume = this.half(n4, n5);
        this.volumeFbo = this.framebuffer("volume", 0, this.volume);
        this.volumeSpare = this.half(n4, n5);
        this.volumeSpareFbo = this.framebuffer("volume spare", 0, this.volumeSpare);
        int n6 = Math.max(1, n * n3 / 100);
        int n7 = Math.max(1, n2 * n3 / 100);
        this.clouds = this.half(n6, n7);
        this.cloudFbo = this.framebuffer("clouds", 0, this.clouds);
        this.halfWidth = n4;
        this.halfHeight = n5;
        this.cloudShare = n3;
        this.cloudWidth = n6;
        this.cloudHeight = n7;
    }

    private void makeBloomChain(int n, int n2) {
        int n3 = n;
        int n4 = n2;
        for (int i = 0; i < 5; ++i) {
            n3 = Math.max(1, n3 / 2);
            n4 = Math.max(1, n4 / 2);
            this.bloomWidth[i] = n3;
            this.bloomHeight[i] = n4;
            this.bloom[i] = this.texture(34842, n3, n4, 6408, 5126, 9729);
            this.bloomFbo[i] = this.framebuffer("bloom " + i, 0, this.bloom[i]);
        }
    }

    void uploadGrids(SceneData sceneData, long l) {
        if (this.gridsFrame == l) {
            return;
        }
        this.gridsFrame = l;
        GL13.glActiveTexture((int)34064);
        GL11.glPixelStorei((int)3317, (int)1);
        Targets.grid(this.exposure, sceneData.exposure);
        Targets.grid(this.rooms, sceneData.rooms);
        Targets.grid(this.water, sceneData.water);
        GL11.glPixelStorei((int)3317, (int)4);
        GL13.glActiveTexture((int)33984);
    }

    private static void grid(int n, byte[] byArray) {
        GL11.glBindTexture((int)3553, (int)n);
        gridBuffer.clear();
        gridBuffer.put(byArray).flip();
        GL11.glTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)64, (int)64, (int)6403, (int)5121, (ByteBuffer)gridBuffer);
    }

    private int half(int n, int n2) {
        return this.texture(34842, n, n2, 6408, 5126, 9729);
    }

    private int texture(int n, int n2, int n3, int n4, int n5, int n6) {
        int n7 = Gl.texture(n, n2, n3, n4, n5, n6);
        this.textures.add(n7);
        return n7;
    }

    private int framebuffer(String string, int n, int ... nArray) {
        int n2;
        int n3 = GL30.glGenFramebuffers();
        this.framebuffers.add(n3);
        GL30.glBindFramebuffer((int)36160, (int)n3);
        for (n2 = 0; n2 < nArray.length; ++n2) {
            GL30.glFramebufferTexture2D((int)36160, (int)(36064 + n2), (int)3553, (int)nArray[n2], (int)0);
        }
        if (n != 0) {
            GL30.glFramebufferTexture2D((int)36160, (int)33306, (int)3553, (int)n, (int)0);
        }
        if (nArray.length > 1) {
            drawBuffers.clear();
            for (n2 = 0; n2 < nArray.length; ++n2) {
                drawBuffers.put(36064 + n2);
            }
            drawBuffers.flip();
            GL20.glDrawBuffers((IntBuffer)drawBuffers);
        } else if (nArray.length == 0) {
            GL11.glDrawBuffer((int)0);
            GL11.glReadBuffer((int)0);
        }
        n2 = GL30.glCheckFramebufferStatus((int)36160);
        if (n2 != 36053) {
            this.incomplete.append(' ').append(string).append(" 0x").append(Integer.toHexString(n2));
        }
        return n3;
    }

    private void delete() {
        for (int n : this.framebuffers) {
            GL30.glDeleteFramebuffers((int)n);
        }
        for (int n : this.textures) {
            GL11.glDeleteTextures((int)n);
        }
        this.framebuffers.clear();
        this.textures.clear();
        this.incomplete.setLength(0);
    }
}

