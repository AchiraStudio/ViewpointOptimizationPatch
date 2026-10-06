/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.render.FrameContext;

final class BlurPass {
    private final GlProgram program = GlProgram.create("blur", "screen.vert", "volume_blur.frag");

    BlurPass() {
        this.program.sampler("uVolume", 0);
        this.program.sampler("uDepth", 12);
    }

    void run(FrameContext frameContext, int n, int n2, int n3, int n4, int n5) {
        this.program.use();
        frameContext.camera(this.program);
        Gl.bind(12, n);
        GL13.glActiveTexture((int)33984);
        int n6 = this.program.location("uStep");
        GL30.glBindFramebuffer((int)36160, (int)n5);
        GL11.glBindTexture((int)3553, (int)n2);
        GL20.glUniform2f((int)n6, (float)(1.0f / (float)frameContext.halfWidth), (float)0.0f);
        Gl.screenTriangle();
        GL30.glBindFramebuffer((int)36160, (int)n3);
        GL11.glBindTexture((int)3553, (int)n4);
        GL20.glUniform2f((int)n6, (float)0.0f, (float)(1.0f / (float)frameContext.halfHeight));
        Gl.screenTriangle();
    }
}

