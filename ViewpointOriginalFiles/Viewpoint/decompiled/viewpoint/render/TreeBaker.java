/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL14
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL42
 */
package viewpoint.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.render.Meshes;
import viewpoint.render.TreeAtlas;
import zombie.core.textures.TextureID;

final class TreeBaker {
    private static final int BAKES_PER_FRAME = 32;
    private final GlProgram program = GlProgram.create("tree bake", "tree_bake.vert", "tree_bake.frag");
    private int texture;
    private int framebuffer;
    private final int[] viewport = new int[4];
    private final float[] clearColour = new float[4];

    TreeBaker() {
        this.program.sampler("uSource", 0);
    }

    void bake() {
        TreeAtlas.Slot slot;
        if (TreeAtlas.PENDING.isEmpty()) {
            return;
        }
        if (this.texture == 0) {
            this.init();
        }
        int n = GL11.glGetInteger((int)36006);
        GL11.glGetIntegerv((int)2978, (int[])this.viewport);
        GL11.glGetFloatv((int)3106, (float[])this.clearColour);
        int n2 = GL11.glGetInteger((int)32969);
        int n3 = GL11.glGetInteger((int)32968);
        int n4 = GL11.glGetInteger((int)32971);
        int n5 = GL11.glGetInteger((int)32970);
        GL30.glBindFramebuffer((int)36009, (int)this.framebuffer);
        GL11.glDisable((int)2929);
        GL11.glEnable((int)3089);
        GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)1, (int)771);
        this.program.use();
        GL30.glBindVertexArray((int)0);
        for (int i = 0; i < 32 && (slot = TreeAtlas.PENDING.poll()) != null; ++i) {
            GL11.glViewport((int)slot.x(), (int)slot.y(), (int)64, (int)128);
            GL11.glScissor((int)slot.x(), (int)slot.y(), (int)64, (int)128);
            GL11.glClear((int)16384);
            this.sprite(slot, slot.trunkPage, slot.trunkMap);
            if (slot.leavesPage == null) continue;
            this.sprite(slot, slot.leavesPage, slot.leavesMap);
        }
        GL20.glUseProgram((int)0);
        GL11.glDisable((int)3089);
        GL30.glBindFramebuffer((int)36009, (int)n);
        GL13.glActiveTexture((int)34026);
        GL11.glBindTexture((int)3553, (int)this.texture);
        GL30.glGenerateMipmap((int)3553);
        GL11.glBindTexture((int)3553, (int)0);
        GL13.glActiveTexture((int)33984);
        GL11.glViewport((int)this.viewport[0], (int)this.viewport[1], (int)this.viewport[2], (int)this.viewport[3]);
        GL11.glClearColor((float)this.clearColour[0], (float)this.clearColour[1], (float)this.clearColour[2], (float)this.clearColour[3]);
        GL14.glBlendFuncSeparate((int)n2, (int)n3, (int)n4, (int)n5);
        Gl.resetState();
    }

    private void sprite(TreeAtlas.Slot slot, TextureID textureID, float[] fArray) {
        float f = 0.5f * fArray[9];
        float f2 = 0.5f * fArray[10];
        float f3 = fArray[0] - f;
        float f4 = fArray[1] - f2;
        float f5 = fArray[2] + f;
        float f6 = fArray[3] + f2;
        float f7 = 32.0f - slot.width * 0.5f;
        float f8 = 128.0f - slot.height;
        float f9 = slot.height - 16.0f;
        this.program.set("uRect", ((f3 - fArray[4]) / fArray[6] - f7) / slot.width, ((f4 - fArray[5]) / fArray[7] - f8) / f9, ((f5 - fArray[4]) / fArray[6] - f7) / slot.width, ((f6 - fArray[5]) / fArray[7] - f8) / f9);
        this.program.set("uUv", f3, f4, f5, f6);
        Meshes.bindPage(textureID);
        Gl.generated(6);
    }

    void bind(boolean bl) {
        Gl.bind(42, bl ? this.texture : 0);
    }

    private void init() {
        GL13.glActiveTexture((int)34026);
        this.texture = GL11.glGenTextures();
        GL11.glBindTexture((int)3553, (int)this.texture);
        GL42.glTexStorage2D((int)3553, (int)7, (int)32856, (int)2048, (int)2048);
        GL11.glTexParameteri((int)3553, (int)33085, (int)6);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9987);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glBindTexture((int)3553, (int)0);
        GL13.glActiveTexture((int)33984);
        this.framebuffer = GL30.glGenFramebuffers();
        int n = GL11.glGetInteger((int)36006);
        GL30.glBindFramebuffer((int)36009, (int)this.framebuffer);
        GL30.glFramebufferTexture2D((int)36009, (int)36064, (int)3553, (int)this.texture, (int)0);
        GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        GL11.glClear((int)16384);
        GL30.glBindFramebuffer((int)36009, (int)n);
    }
}

