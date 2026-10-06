/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 *  zombie.core.textures.Texture
 */
package viewpoint.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.Tuning;
import viewpoint.render.FrameContext;
import viewpoint.render.SceneData;
import viewpoint.render.SkyPass;
import viewpoint.render.Targets;
import viewpoint.render.WorldRenderer;
import zombie.core.textures.Texture;

final class PostPass {
    private final GlProgram bloom = GlProgram.create("bloom", "screen.vert", "bloom.frag");
    private final GlProgram post = GlProgram.create("post", "screen.vert", "post.frag");
    private final Targets targets;

    PostPass(Targets targets) {
        this.targets = targets;
        this.post.sampler("uColor", 0);
        this.post.sampler("uDepth", 12);
        this.post.sampler("uAlbedo", 9);
        this.post.sampler("uGi", 13);
        this.post.sampler("uVolume", 15);
        this.post.sampler("uBloom", 16);
        this.bloom.sampler("uColor", 0);
    }

    void bloom(FrameContext frameContext) {
        int n;
        this.bloom.use();
        GL11.glDisable((int)2929);
        GL11.glDisable((int)3042);
        GL13.glActiveTexture((int)33984);
        int n2 = this.bloom.location("uTexel");
        int n3 = this.bloom.location("uMode");
        for (n = 0; n < 5; ++n) {
            GL30.glBindFramebuffer((int)36160, (int)this.targets.bloomFbo[n]);
            GL11.glViewport((int)0, (int)0, (int)this.targets.bloomWidth[n], (int)this.targets.bloomHeight[n]);
            GL11.glBindTexture((int)3553, (int)(n == 0 ? this.targets.hdr : this.targets.bloom[n - 1]));
            GL20.glUniform2f((int)n2, (float)(1.0f / (float)(n == 0 ? frameContext.width : this.targets.bloomWidth[n - 1])), (float)(1.0f / (float)(n == 0 ? frameContext.height : this.targets.bloomHeight[n - 1])));
            GL20.glUniform1i((int)n3, (int)(n == 0 ? 0 : 1));
            Gl.screenTriangle();
        }
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)1, (int)1);
        for (n = 3; n >= 0; --n) {
            GL30.glBindFramebuffer((int)36160, (int)this.targets.bloomFbo[n]);
            GL11.glViewport((int)0, (int)0, (int)this.targets.bloomWidth[n], (int)this.targets.bloomHeight[n]);
            GL11.glBindTexture((int)3553, (int)this.targets.bloom[n + 1]);
            GL20.glUniform2f((int)n2, (float)(1.0f / (float)this.targets.bloomWidth[n + 1]), (float)(1.0f / (float)this.targets.bloomHeight[n + 1]));
            GL20.glUniform1i((int)n3, (int)2);
            Gl.screenTriangle();
        }
        GL11.glDisable((int)3042);
        GL11.glBindTexture((int)3553, (int)0);
        Texture.lastTextureID = -1;
    }

    void composite(FrameContext frameContext) {
        SceneData sceneData = frameContext.scene;
        GL11.glDisable((int)2929);
        GL11.glDisable((int)3042);
        GL11.glDisable((int)2884);
        GL11.glDepthMask((boolean)false);
        this.post.use();
        frameContext.camera(this.post);
        this.post.set("uDesaturation", sceneData.desaturation);
        this.post.setInt("uDebugView", WorldRenderer.debugView);
        this.post.set("uNight", sceneData.night);
        this.post.set("uFogColor", sceneData.fogR, sceneData.fogG, sceneData.fogB);
        this.post.set("uFogRange", sceneData.fogStart, sceneData.fogEnd);
        SkyPass.fogUniforms(this.post, sceneData);
        this.post.set("uHaze", Tuning.farHaze, sceneData.nearEnd());
        this.post.set("uGiOn", WorldRenderer.off(2) ? 0.0f : 1.0f);
        this.post.set("uVolumeOn", WorldRenderer.off(4) ? 0.0f : 1.0f);
        this.post.set("uBloomStrength", frameContext.pipeline.bloom ? Tuning.bloomStrength : 0.0f);
        this.post.set("uExposure", Tuning.exposure);
        Gl.bind(15, this.targets.volume);
        Gl.bind(16, this.targets.bloom[0]);
        this.post.set("uGiSize", frameContext.halfWidth, frameContext.halfHeight);
        Gl.bind(12, this.targets.depth);
        Gl.bind(9, this.targets.albedo);
        Gl.bind(13, this.targets.gi);
        GL13.glActiveTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)this.targets.hdr);
        Gl.screenTriangle();
        Gl.bind(12, 0);
        Gl.bind(9, 0);
        Gl.bind(13, 0);
        Gl.bind(15, 0);
        Gl.bind(16, 0);
        GL13.glActiveTexture((int)33984);
        GL11.glBindTexture((int)3553, (int)0);
    }
}

