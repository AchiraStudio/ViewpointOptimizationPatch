/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL14
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL33
 *  org.lwjgl.opengl.GL40
 */
package viewpoint.platform;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import viewpoint.platform.Profile;

public final class Gl {
    public static final int UNIT_LIGHT = 5;
    public static final int UNIT_EXPOSURE = 8;
    public static final int UNIT_ALBEDO = 9;
    public static final int UNIT_NORMAL = 10;
    public static final int UNIT_ENGINE_LIGHT = 11;
    public static final int UNIT_DEPTH = 12;
    public static final int UNIT_GI = 13;
    public static final int UNIT_WEATHER = 14;
    public static final int UNIT_VOLUME = 15;
    public static final int UNIT_BLOOM = 16;
    public static final int UNIT_CLOUDS = 17;
    public static final int UNIT_NOISE = 18;
    public static final int UNIT_HISTORY = 19;
    public static final int UNIT_FLASH = 21;
    public static final int UNIT_FLASH_DEPTH = 22;
    public static final int UNIT_ROOMS = 23;
    public static final int UNIT_RAIN = 24;
    public static final int UNIT_FAR_TOP = 25;
    public static final int UNIT_FAR_SIDE = 26;
    public static final int UNIT_FAR_MASK = 27;
    public static final int UNIT_FAR_SHELL = 28;
    public static final int UNIT_FAR_DEPTH = 29;
    public static final int UNIT_MODEL_PALETTES = 31;
    public static final int UNIT_VEHICLE = 32;
    public static final int UNIT_VELOCITY = 39;
    public static final int UNIT_MODEL_DRAWS = 40;
    public static final int UNIT_FLOOR_PAGES = 41;
    public static final int UNIT_TREE_ATLAS = 42;
    public static final int UNIT_FAR_TREES = 43;
    public static final int UNIT_TREE_KINDS = 44;
    public static final int UNIT_BAND_LIGHT = 45;
    public static final int UNIT_CELL_LIGHT = 46;
    public static final int UNIT_CELL_TABLE = 47;
    public static final int UNIT_PLANT_SLOTS = 48;
    public static final int UNIT_LAMP_SHADOW = 49;
    public static final int UNIT_TARGET_MASK = 51;
    public static final int UNIT_PASS = 52;
    public static final int UNIT_MODEL_TEXTURES = 64;
    public static final int UNIT_WATER = 80;
    public static final int UNIT_NOISE_2D = 84;
    public static final int UNIT_DISTANT_RAIN = 85;
    public static final int UNIT_TRANSLUCENT_SUM = 86;
    public static final int UNIT_TRANSLUCENT_REVEAL = 87;
    public static final int UNIT_PACK_TABLE = 88;
    public static final int UNIT_MESH_RECORDS = 89;
    public static final int UNIT_CORPSE_CARDS = 90;
    public static final int MODEL_TEXTURES = 16;
    public static final int MODEL_SLOT_SHIFT = 24;
    public static final int[] UNIT_CASCADE = new int[]{6, 20, 7, 30, 50};
    public static final int[] UNIT_SHADOW_DEPTH = new int[]{81, 82, 83};
    private static int emptyVao;
    private static int triangles;

    public static void init() {
        emptyVao = GL30.glGenVertexArrays();
    }

    public static void patches(boolean bl) {
        if (bl) {
            GL40.glPatchParameteri((int)36466, (int)3);
        }
        triangles = bl ? 14 : 4;
    }

    public static int triangles() {
        return triangles;
    }

    public static void bind(int n, int n2) {
        GL13.glActiveTexture((int)(33984 + n));
        GL11.glBindTexture((int)3553, (int)n2);
    }

    public static void screenTriangle() {
        GL30.glBindVertexArray((int)emptyVao);
        GL11.glDrawArrays((int)4, (int)0, (int)3);
        GL30.glBindVertexArray((int)0);
        ++Profile.draws;
    }

    public static void generated(int n) {
        Gl.generated(0, n);
    }

    public static void generated(int n, int n2) {
        GL30.glBindVertexArray((int)emptyVao);
        GL11.glDrawArrays((int)4, (int)n, (int)n2);
        GL30.glBindVertexArray((int)0);
        ++Profile.draws;
    }

    public static int texture(int n, int n2, int n3, int n4, int n5, int n6) {
        int n7 = GL11.glGenTextures();
        GL11.glBindTexture((int)3553, (int)n7);
        GL11.glTexImage2D((int)3553, (int)0, (int)n, (int)n2, (int)n3, (int)0, (int)n4, (int)n5, (ByteBuffer)null);
        GL11.glTexParameteri((int)3553, (int)10241, (int)n6);
        GL11.glTexParameteri((int)3553, (int)10240, (int)n6);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        return n7;
    }

    public static void resetState() {
        GL11.glDisable((int)3089);
        GL11.glDisable((int)3042);
        GL14.glBlendEquation((int)32774);
        GL11.glDisable((int)3008);
        GL11.glDisable((int)2884);
        GL11.glFrontFace((int)2305);
        GL11.glDisable((int)2960);
        GL11.glStencilMask((int)255);
        GL11.glDisable((int)32823);
        GL11.glDisable((int)32926);
        GL11.glDisable((int)36281);
        for (int i = 0; i < 8; ++i) {
            GL11.glDisable((int)(12288 + i));
        }
        GL11.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GL11.glDepthRange((double)0.0, (double)1.0);
        GL11.glDepthMask((boolean)true);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)513);
        GL33.glBindSampler((int)0, (int)0);
        GL30.glBindVertexArray((int)0);
        GL13.glActiveTexture((int)33984);
    }

    public static float wrap(double d, double d2) {
        return (float)(d - Math.floor(d / d2) * d2);
    }

    private Gl() {
    }

    static {
        triangles = 4;
    }
}

