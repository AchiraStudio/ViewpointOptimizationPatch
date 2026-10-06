/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL33
 */
package viewpoint.render;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL33;
import viewpoint.platform.GlProgram;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.LongMap;

final class TextureFilter {
    static final int NEAREST = 0;
    static final int NEAREST_MIPMAPPED = 1;
    static final int BILINEAR = 2;
    static final int TRILINEAR = 3;
    static final int ANISOTROPIC = 4;
    static final int LANCZOS = 5;
    private static final String[] OPTIONS = new String[]{"Nearest", "Nearest, mipmapped", "Bilinear", "Trilinear", "Anisotropic", "Lanczos"};
    private static final float MAX_ANISOTROPY = 16.0f;
    private static final float ALL_LEVELS = 1000.0f;
    private static final float ATLAS_LEVELS = 3.0f;
    private static final String SECTION = "World/Texture filtering";
    static final TextureFilter FLOORS = new TextureFilter("floors.filter", "Floors", "World/Texture filtering", "uFloorFilter", 0, 1000.0f, 33071);
    static final TextureFilter SPRITES = new TextureFilter("sprites.filter", "Walls and objects", "World/Texture filtering", "uSpriteFilter", 1, 3.0f, 33071);
    static final TextureFilter MODELS = new TextureFilter("models.filter", "3D models", "World/Texture filtering", "uModelFilter", 1, 1000.0f, 10497);
    private static final LongMap<Boolean> hasMips = new LongMap();
    private final LiveSettings.Choice choice;
    private final String uniform;
    private final float maxLevel;
    private final int wrap;
    private int[] mipped;
    private int[] flat;

    private TextureFilter(String string, String string2, String string3, String string4, int n, float f, int n2) {
        this.choice = LiveSettings.choice(string, string2, string3, OPTIONS, n);
        this.uniform = string4;
        this.maxLevel = f;
        this.wrap = n2;
    }

    int get() {
        return this.choice.get();
    }

    void apply(GlProgram glProgram) {
        if (glProgram.has(this.uniform)) {
            glProgram.setInt(this.uniform, this.get());
        }
    }

    int sampler(int n) {
        return this.sampler(TextureFilter.hasMips(n));
    }

    int sampler(boolean bl) {
        if (this.mipped == null) {
            this.mipped = new int[]{this.make(9728, 9728, false), this.make(9986, 9728, false), this.make(9729, 9729, false), this.make(9987, 9729, false), this.make(9987, 9729, true), this.make(9987, 9729, true)};
            int n = this.make(9728, 9728, false);
            int n2 = this.make(9729, 9729, false);
            this.flat = new int[]{n, n, n2, n2, n2, n2};
        }
        return (bl ? this.mipped : this.flat)[this.get()];
    }

    private int make(int n, int n2, boolean bl) {
        int n3 = GL33.glGenSamplers();
        GL33.glSamplerParameteri((int)n3, (int)10241, (int)n);
        GL33.glSamplerParameteri((int)n3, (int)10240, (int)n2);
        GL33.glSamplerParameteri((int)n3, (int)10242, (int)this.wrap);
        GL33.glSamplerParameteri((int)n3, (int)10243, (int)this.wrap);
        if (this.maxLevel < 1000.0f) {
            GL33.glSamplerParameterf((int)n3, (int)33083, (float)this.maxLevel);
        }
        if (bl && GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            float f = GL11.glGetFloat((int)34047);
            GL33.glSamplerParameterf((int)n3, (int)34046, (float)Math.min(16.0f, f));
        }
        return n3;
    }

    private static boolean hasMips(int n) {
        Boolean bl = hasMips.get(n);
        if (bl == null) {
            int n2 = Math.max(GL11.glGetTexLevelParameteri((int)3553, (int)0, (int)4096), GL11.glGetTexLevelParameteri((int)3553, (int)0, (int)4097));
            int n3 = Math.min(31 - Integer.numberOfLeadingZeros(Math.max(n2, 1)), GL11.glGetTexParameteri((int)3553, (int)33085));
            bl = n3 > 0 && GL11.glGetTexLevelParameteri((int)3553, (int)n3, (int)4096) > 0;
            hasMips.put(n, bl);
        }
        return bl;
    }
}

