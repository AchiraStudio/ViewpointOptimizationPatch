/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  zombie.core.textures.Texture
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import viewpoint.platform.Gl;
import viewpoint.platform.GlProgram;
import viewpoint.platform.Tuning;
import viewpoint.render.FlashShadow;
import viewpoint.render.FrameContext;
import viewpoint.render.LampShadows;
import viewpoint.render.ModelBatches;
import viewpoint.render.ModelDraws;
import viewpoint.render.ModelPass;
import viewpoint.render.ShadowPass;
import viewpoint.render.SurfacePass;
import viewpoint.render.TextureFilter;
import viewpoint.render.VehicleMaterials;
import zombie.core.textures.Texture;

final class VehiclePass {
    private static final String[] SAMPLERS = new String[]{"uRustMap", "uZones", "uLamps", "uDamage1Overlay", "uDamage1Shell", "uDamage2Overlay", "uDamage2Shell"};
    private final ModelPass models;
    private final GlProgram gbuffer = GlProgram.create("vehicle", "model.vert", "vehicle_gbuffer.frag");
    private final GlProgram shadow = GlProgram.create("vehicle shadow", "model.vert", "vehicle_shadow.frag");
    private final GlProgram glass = GlProgram.create("vehicle glass", "model.vert", "vehicle_glass.frag");
    private final FloatBuffer switches = BufferUtils.createFloatBuffer((int)192);
    private final int blank;

    VehiclePass(ModelPass modelPass) {
        this.models = modelPass;
        for (GlProgram glProgram : new GlProgram[]{this.gbuffer, this.shadow, this.glass}) {
            VehiclePass.samplers(glProgram);
        }
        ShadowPass.samplers(this.glass);
        FlashShadow.sampler(this.glass);
        LampShadows.sampler(this.glass);
        int n = GL11.glGetInteger((int)32873);
        this.blank = Gl.texture(32856, 1, 1, 6408, 5121, 9728);
        GL11.glTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)1, (int)1, (int)6408, (int)5121, (ByteBuffer)BufferUtils.createByteBuffer((int)4));
        GL11.glBindTexture((int)3553, (int)n);
    }

    static void samplers(GlProgram glProgram) {
        ModelPass.samplers(glProgram);
        for (int i = 0; i < SAMPLERS.length; ++i) {
            glProgram.sampler(SAMPLERS[i], 32 + i);
        }
    }

    void iris(GlProgram glProgram, ModelDraws modelDraws, boolean bl) {
        glProgram.setInt("uIrisGlass", bl ? 1 : 0);
        glProgram.set("uIrisGlassAlpha", Tuning.vehicleGlass);
        glProgram.setInt("uIrisUntinted", 1);
        TextureFilter.MODELS.apply(glProgram);
        this.models.batches.draw(n -> modelDraws.material[n] >= 0 && this.models.shown(modelDraws, n), n -> this.material(glProgram, modelDraws, n));
        VehiclePass.unbind();
    }

    int gbuffer(FrameContext frameContext) {
        ModelDraws modelDraws = frameContext.scene.models;
        this.gbuffer.use();
        TextureFilter.MODELS.apply(this.gbuffer);
        this.gbuffer.set("uViewProjection", frameContext.viewProjection);
        this.gbuffer.set("uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        ModelPass.views(this.gbuffer, frameContext);
        int n2 = this.models.batches.draw(n -> modelDraws.material[n] >= 0 && this.models.shown(modelDraws, n), n -> this.material(this.gbuffer, modelDraws, n));
        VehiclePass.unbind();
        return n2;
    }

    void shadow(ModelDraws modelDraws, Matrix4f matrix4f, int[] nArray, int n2, ModelBatches.Filter filter, boolean bl) {
        this.shadow.use();
        this.shadow.set("uViewProjection", matrix4f);
        this.shadow.setInt("uShadowPalette", 1);
        this.shadow.setInt("uGlass", bl ? 1 : 0);
        this.models.batches.draw(nArray, n2, n -> modelDraws.material[n] >= 0 && filter.take(n), n -> this.material(this.shadow, modelDraws, n));
        VehiclePass.unbind();
    }

    void glass(FrameContext frameContext, ShadowPass shadowPass) {
        ModelDraws modelDraws = frameContext.scene.models;
        this.glass.use();
        this.glass.set("uViewProjection", frameContext.viewProjection);
        this.glass.set("uEye", frameContext.eyeX, frameContext.eyeY, frameContext.eyeZ);
        shadowPass.sunUniforms(this.glass, frameContext);
        SurfacePass.lamps(this.glass, frameContext.scene);
        shadowPass.lamps.uniforms(this.glass, frameContext.scene);
        SurfacePass.torches(this.glass, frameContext.scene);
        shadowPass.flash.uniforms(this.glass, frameContext);
        this.glass.set("uFogColor", frameContext.scene.fogR, frameContext.scene.fogG, frameContext.scene.fogB);
        this.glass.set("uGlassAlpha", Tuning.vehicleGlass);
        shadowPass.bind(true);
        GL11.glDepthMask((boolean)false);
        this.models.batches.draw(n -> modelDraws.material[n] >= 0 && this.models.shown(modelDraws, n), n -> this.material(this.glass, modelDraws, n));
        GL11.glDepthMask((boolean)true);
        shadowPass.bind(false);
        VehiclePass.unbind();
    }

    private void material(GlProgram glProgram, ModelDraws modelDraws, int n) {
        VehicleMaterials vehicleMaterials = modelDraws.vehicles;
        int n2 = modelDraws.material[n] * 197;
        this.switches.clear();
        this.switches.put(vehicleMaterials.values, n2, 192).flip();
        glProgram.setMatrices("uSwitches", this.switches);
        glProgram.set("uRust", vehicleMaterials.values[n2 + 192]);
        glProgram.set("uPaint", vehicleMaterials.values[n2 + 193], vehicleMaterials.values[n2 + 193 + 1], vehicleMaterials.values[n2 + 193 + 2]);
        glProgram.setInt("uKind", (int)vehicleMaterials.values[n2 + 196]);
        for (int i = 0; i < 7; ++i) {
            Texture texture = vehicleMaterials.textures[modelDraws.material[n] * 7 + i];
            GL13.glActiveTexture((int)(34016 + i));
            if (texture != null && texture.isReady()) {
                Texture.lastTextureID = -1;
                texture.bind();
                continue;
            }
            GL11.glBindTexture((int)3553, (int)this.blank);
        }
        this.models.surface(n);
    }

    private static void unbind() {
        for (int i = 0; i < 7; ++i) {
            Gl.bind(32 + i, 0);
        }
        GL13.glActiveTexture((int)33984);
        Texture.lastTextureID = -1;
    }
}

