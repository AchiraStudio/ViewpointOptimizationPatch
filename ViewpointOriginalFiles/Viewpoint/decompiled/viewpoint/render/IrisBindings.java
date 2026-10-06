/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL33
 */
package viewpoint.render;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL33;
import viewpoint.render.IrisImages;
import viewpoint.render.IrisProgram;
import viewpoint.render.IrisShadowTargets;
import viewpoint.render.IrisTargets;
import viewpoint.render.IrisTextures;

final class IrisBindings {
    private final IrisTargets targets;
    private final IrisShadowTargets shadows;
    private final IrisTextures textures;
    private final IrisImages images;
    private final int compare;
    private final int plain;
    private final Set<String> unknown = new HashSet<String>();

    IrisBindings(IrisTargets irisTargets, IrisShadowTargets irisShadowTargets, IrisTextures irisTextures, IrisImages irisImages) {
        this.targets = irisTargets;
        this.shadows = irisShadowTargets;
        this.textures = irisTextures;
        this.images = irisImages;
        this.compare = GL33.glGenSamplers();
        GL33.glSamplerParameteri((int)this.compare, (int)34892, (int)34894);
        GL33.glSamplerParameteri((int)this.compare, (int)34893, (int)515);
        GL33.glSamplerParameteri((int)this.compare, (int)10241, (int)9729);
        GL33.glSamplerParameteri((int)this.compare, (int)10240, (int)9729);
        GL33.glSamplerParameteri((int)this.compare, (int)10242, (int)33071);
        GL33.glSamplerParameteri((int)this.compare, (int)10243, (int)33071);
        this.plain = 0;
    }

    static int colortex(String string) {
        return switch (string) {
            case "gcolor" -> 0;
            case "gdepth" -> 1;
            case "gnormal" -> 2;
            case "composite" -> 3;
            case "gaux1" -> 4;
            case "gaux2" -> 5;
            case "gaux3" -> 6;
            case "gaux4" -> 7;
            default -> string.matches("colortex\\d{1,2}") ? Integer.parseInt(string.substring(8)) : -1;
        };
    }

    static int shadowcolor(String string) {
        return string.equals("shadowcolor") ? 0 : (string.matches("shadowcolor\\d") ? string.charAt(11) - 48 : -1);
    }

    void bind(IrisProgram irisProgram, String string, Set<Integer> set) {
        for (Map.Entry<String, Integer> entry : irisProgram.units.entrySet()) {
            String string2 = entry.getKey();
            int n = entry.getValue();
            if (n == 0) continue;
            String string3 = irisProgram.samplerTypes.getOrDefault(string2, "");
            int n2 = string3.contains("3D") ? 32879 : (string3.contains("1D") ? 3552 : 3553);
            IrisTextures.Texture texture = this.textures.custom(string, string2);
            int n3 = IrisBindings.colortex(string2);
            GL13.glActiveTexture((int)(33984 + n));
            if (!(texture == null || n3 >= 0 && set.contains(n3))) {
                if (texture.target() != 3553) {
                    GL11.glBindTexture((int)3553, (int)this.texture(string2));
                }
                GL11.glBindTexture((int)texture.target(), (int)texture.id());
                GL33.glBindSampler((int)n, (int)this.plain);
                continue;
            }
            GL11.glBindTexture((int)n2, (int)(n2 == 3553 ? this.texture(string2) : this.images.sampler(string2, n2)));
            GL33.glBindSampler((int)n, (int)(string3.contains("Shadow") ? this.compare : this.plain));
        }
        this.images.bind(irisProgram);
        GL13.glActiveTexture((int)33984);
    }

    void unbind(IrisProgram irisProgram) {
        for (int n : irisProgram.units.values()) {
            if (n == 0) continue;
            GL13.glActiveTexture((int)(33984 + n));
            GL11.glBindTexture((int)3553, (int)0);
            GL11.glBindTexture((int)32879, (int)0);
            GL33.glBindSampler((int)n, (int)0);
        }
        GL13.glActiveTexture((int)33984);
    }

    private int texture(String string) {
        int n = IrisBindings.colortex(string);
        if (n >= 0 && n < 32 && this.targets.buffers[n].used) {
            return this.targets.buffers[n].current();
        }
        int n2 = IrisBindings.shadowcolor(string);
        if (n2 >= 0 && n2 < 8 && this.shadows.colors[n2] != 0) {
            return this.shadows.colors[n2];
        }
        return switch (string) {
            case "depthtex0", "gdepthtex" -> this.targets.depth;
            case "depthtex1" -> this.targets.depthOpaque;
            case "depthtex2" -> this.targets.depthNoHand;
            case "dhDepthTex", "dhDepthTex0" -> this.targets.farDepth;
            case "dhDepthTex1" -> this.targets.farDepthOpaque;
            case "shadowtex0", "shadow", "watershadow", "shadowtex0HW" -> this.shadows.depth;
            case "shadowtex1", "shadowtex1HW" -> this.shadows.depthOpaque;
            case "noisetex" -> this.textures.noise.id();
            case "normals" -> this.textures.normals.id();
            case "specular" -> this.textures.specular.id();
            case "lightmap" -> this.textures.lightmap.id();
            case "vp_tintTex" -> this.targets.tint;
            default -> {
                int var6_6 = this.images.sampler(string, 3553);
                if (var6_6 != 0) {
                    yield var6_6;
                }
                this.unknown.add(string);
                yield this.textures.white.id();
            }
        };
    }

    Set<String> unknown() {
        return this.unknown;
    }

    void release() {
        GL33.glDeleteSamplers((int)this.compare);
    }
}

