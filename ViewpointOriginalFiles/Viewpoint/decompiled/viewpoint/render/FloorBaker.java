/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL14
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL42
 *  org.lwjgl.opengl.GL43
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.render.FloorBakeSource;
import viewpoint.render.FloorFilter;
import viewpoint.render.FloorLayers;
import viewpoint.render.FloorPage;
import viewpoint.render.FloorSlices;
import viewpoint.render.FrameStream;
import viewpoint.render.Meshes;
import zombie.core.textures.TextureID;

final class FloorBaker {
    private static final int FLOATS = 16;
    private final GlProgram program = GlProgram.create("floor bake", "floor_bake.vert", "floor_bake.frag");
    private int vao;
    private int framebuffer;
    private final HashMap<Integer, Integer> scratch = new HashMap();
    private FloatBuffer layers = BufferUtils.createFloatBuffer((int)8192);
    private TextureID[] runPages = new TextureID[64];
    private int[] runFirst = new int[64];
    private int[] runCount = new int[64];
    private int runs;
    private final int[] viewport = new int[4];
    private final float[] clearColour = new float[4];
    int layersBaked;

    FloorBaker() {
    }

    void bake(FloorBakeSource floorBakeSource, FloorSlices floorSlices, int n2) {
        this.collect(floorBakeSource);
        if (this.vao == 0) {
            this.init();
        }
        int n3 = GL11.glGetInteger((int)36006);
        GL11.glGetIntegerv((int)2978, (int[])this.viewport);
        GL11.glGetFloatv((int)3106, (float[])this.clearColour);
        int n4 = GL11.glGetInteger((int)32969);
        int n5 = GL11.glGetInteger((int)32968);
        int n6 = GL11.glGetInteger((int)32971);
        int n7 = GL11.glGetInteger((int)32970);
        int n8 = this.scratch.computeIfAbsent(floorSlices.size, n -> FloorBaker.scratchPage(n, floorSlices.mips));
        GL30.glBindFramebuffer((int)36009, (int)this.framebuffer);
        GL30.glFramebufferTexture2D((int)36009, (int)36064, (int)3553, (int)n8, (int)0);
        GL11.glViewport((int)0, (int)0, (int)floorSlices.size, (int)floorSlices.size);
        GL11.glDisable((int)2929);
        GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        GL11.glClear((int)16384);
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)1, (int)771);
        if (this.runs > 0) {
            this.draw(floorSlices, floorBakeSource.tiles());
        }
        GL30.glBindFramebuffer((int)36009, (int)n3);
        GL11.glBindTexture((int)3553, (int)n8);
        GL30.glGenerateMipmap((int)3553);
        GL11.glBindTexture((int)3553, (int)0);
        FloorBaker.copy(n8, floorSlices, n2);
        GL11.glViewport((int)this.viewport[0], (int)this.viewport[1], (int)this.viewport[2], (int)this.viewport[3]);
        GL11.glClearColor((float)this.clearColour[0], (float)this.clearColour[1], (float)this.clearColour[2], (float)this.clearColour[3]);
        GL14.glBlendFuncSeparate((int)n4, (int)n5, (int)n6, (int)n7);
        Gl.resetState();
    }

    private static void copy(int n, FloorSlices floorSlices, int n2) {
        for (int i = 0; i < floorSlices.mips; ++i) {
            int n3 = Math.max(1, floorSlices.size >> i);
            GL43.glCopyImageSubData((int)n, (int)3553, (int)i, (int)0, (int)0, (int)0, (int)floorSlices.texture(n2), (int)35866, (int)i, (int)0, (int)0, (int)FloorSlices.layer(n2), (int)n3, (int)n3, (int)1);
        }
    }

    private void draw(FloorSlices floorSlices, int n) {
        int n2;
        long l = FrameStream.put(this.layers, 16);
        this.program.use();
        this.program.setInt("uSource", 0);
        this.program.set("uTexelsPerTile", (float)floorSlices.size / (float)n);
        this.program.set("uTiles", n);
        this.program.setInt("uBakeFilter", FloorFilter.bakeFilter());
        GL30.glBindVertexArray((int)this.vao);
        GL43.glBindVertexBuffer((int)0, (int)FrameStream.buffer(), (long)l, (int)64);
        for (n2 = 0; n2 < this.runs; ++n2) {
            Meshes.bindPage(this.runPages[n2]);
            GL42.glDrawArraysInstancedBaseInstance((int)4, (int)0, (int)6, (int)this.runCount[n2], (int)this.runFirst[n2]);
        }
        GL30.glBindVertexArray((int)0);
        GL20.glUseProgram((int)0);
        for (n2 = 0; n2 < this.runs; ++n2) {
            this.runPages[n2] = null;
        }
    }

    private void collect(FloorBakeSource floorBakeSource) {
        this.layers.clear();
        this.runs = 0;
        int n = 0;
        int n2 = floorBakeSource.tiles();
        for (int i = 0; i < 2; ++i) {
            boolean bl = i == 0;
            for (int j = 0; j < n2; ++j) {
                int n3 = bl ? n2 - 1 - j : j;
                for (int k = 0; k < n2; ++k) {
                    FloorLayers floorLayers = floorBakeSource.artAt(k, n3);
                    if (floorLayers == null) continue;
                    int n4 = FloorPage.squareAt(k, n3);
                    for (int i2 = floorLayers.first(n4); i2 < floorLayers.end(n4); ++i2) {
                        if (floorLayers.has(i2, 2) != bl) continue;
                        this.add(floorLayers, i2, k - 1, n3 - 1, n++);
                    }
                }
            }
        }
        this.layers.flip();
        this.layersBaked += n;
    }

    private void add(FloorLayers floorLayers, int n, int n2, int n3, int n4) {
        int n5;
        if (this.layers.remaining() < 16) {
            FloatBuffer floatBuffer = BufferUtils.createFloatBuffer((int)(this.layers.capacity() * 2));
            this.layers.flip();
            this.layers = floatBuffer.put(this.layers);
        }
        for (n5 = 0; n5 <= 3; ++n5) {
            this.layers.put(floorLayers.number(n, n5));
        }
        for (n5 = 6; n5 <= 13; ++n5) {
            this.layers.put(floorLayers.number(n, n5));
        }
        this.layers.put(n2).put(n3);
        this.layers.put(floorLayers.has(n, 2) ? 1.0f : 0.0f).put(floorLayers.has(n, 1) ? 1.0f : 0.0f);
        TextureID textureID = floorLayers.atlas(n);
        if (this.runs > 0 && this.runPages[this.runs - 1] == textureID) {
            int n6 = this.runs - 1;
            this.runCount[n6] = this.runCount[n6] + 1;
            return;
        }
        if (this.runs == this.runPages.length) {
            this.runPages = Arrays.copyOf(this.runPages, this.runs * 2);
            this.runFirst = Arrays.copyOf(this.runFirst, this.runs * 2);
            this.runCount = Arrays.copyOf(this.runCount, this.runs * 2);
        }
        this.runPages[this.runs] = textureID;
        this.runFirst[this.runs] = n4;
        this.runCount[this.runs] = 1;
        ++this.runs;
    }

    private void init() {
        this.framebuffer = GL30.glGenFramebuffers();
        this.vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray((int)this.vao);
        int[] nArray = new int[]{4, 4, 4, 2, 2};
        int n = 0;
        for (int i = 0; i < nArray.length; ++i) {
            GL20.glEnableVertexAttribArray((int)i);
            GL43.glVertexAttribFormat((int)i, (int)nArray[i], (int)5126, (boolean)false, (int)(n * 4));
            GL43.glVertexAttribBinding((int)i, (int)0);
            n += nArray[i];
        }
        GL43.glVertexBindingDivisor((int)0, (int)1);
        GL30.glBindVertexArray((int)0);
    }

    private static int scratchPage(int n, int n2) {
        int n3 = GL11.glGenTextures();
        GL11.glBindTexture((int)3553, (int)n3);
        GL42.glTexStorage2D((int)3553, (int)n2, (int)32856, (int)n, (int)n);
        GL11.glTexParameteri((int)3553, (int)33085, (int)(n2 - 1));
        GL11.glTexParameteri((int)3553, (int)10241, (int)9987);
        GL11.glBindTexture((int)3553, (int)0);
        return n3;
    }

    void clearScratch() {
        for (int n : this.scratch.values()) {
            GL11.glDeleteTextures((int)n);
        }
        this.scratch.clear();
    }
}

