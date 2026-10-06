/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL31
 *  org.lwjgl.system.MemoryUtil
 */
package viewpoint.render;

import java.nio.FloatBuffer;
import java.util.Arrays;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;
import viewpoint.platform.GlProgram;

public final class TreeKinds {
    public static final int DISPLAYS = 5;
    private static final int KIND_TEXELS = 6;
    private static final int FLOATS = 24;
    private static float[] kinds = new float[6144];
    private static int count;
    private static volatile float[] published;
    private static volatile byte[] displays;
    private static float[] uploaded;
    private static int buffer;
    private static int texture;
    private static FloatBuffer displayValues;

    public static void set(int n, int[] nArray, boolean[] blArray, float f, float f2) {
        if (n * 24 >= kinds.length) {
            kinds = Arrays.copyOf(kinds, Math.max(kinds.length * 2, (n + 1) * 24));
        }
        int n2 = n * 24;
        TreeKinds.kinds[n2] = f;
        TreeKinds.kinds[n2 + 1] = f2;
        for (int i = 0; i < 5; ++i) {
            int n3 = nArray[i];
            int n4 = n2 + 4 * (1 + i);
            TreeKinds.kinds[n4] = (float)(n3 >> 16 & 0xFF) / 255.0f;
            TreeKinds.kinds[n4 + 1] = (float)(n3 >> 8 & 0xFF) / 255.0f;
            TreeKinds.kinds[n4 + 2] = (float)(n3 & 0xFF) / 255.0f;
            TreeKinds.kinds[n4 + 3] = blArray[i] ? 1.0f : 0.0f;
        }
        count = Math.max(count, n + 1);
        published = Arrays.copyOf(kinds, count * 24);
    }

    public static void displays(byte[] byArray) {
        displays = byArray;
    }

    static boolean bind() {
        float[] fArray = published;
        if (fArray == null || displays == null) {
            return false;
        }
        if (fArray != uploaded) {
            TreeKinds.upload(fArray);
        }
        GL13.glActiveTexture((int)34028);
        GL11.glBindTexture((int)35882, (int)texture);
        GL13.glActiveTexture((int)33984);
        return true;
    }

    static void uniforms(GlProgram glProgram) {
        byte[] byArray = displays;
        if (displayValues == null || displayValues.capacity() != byArray.length) {
            displayValues = BufferUtils.createFloatBuffer((int)byArray.length);
        }
        displayValues.clear();
        for (byte by : byArray) {
            displayValues.put(by);
        }
        glProgram.setFloats("uDisplays", displayValues.flip());
    }

    static void unbind() {
        GL13.glActiveTexture((int)34028);
        GL11.glBindTexture((int)35882, (int)0);
        GL13.glActiveTexture((int)33984);
    }

    private static void upload(float[] fArray) {
        if (buffer == 0) {
            buffer = GL15.glGenBuffers();
            texture = GL11.glGenTextures();
        }
        FloatBuffer floatBuffer = MemoryUtil.memAllocFloat((int)fArray.length).put(fArray).flip();
        GL15.glBindBuffer((int)35882, (int)buffer);
        GL15.glBufferData((int)35882, (FloatBuffer)floatBuffer, (int)35044);
        GL15.glBindBuffer((int)35882, (int)0);
        MemoryUtil.memFree((FloatBuffer)floatBuffer);
        GL13.glActiveTexture((int)34028);
        GL11.glBindTexture((int)35882, (int)texture);
        GL31.glTexBuffer((int)35882, (int)34836, (int)buffer);
        GL13.glActiveTexture((int)33984);
        uploaded = fArray;
    }

    private TreeKinds() {
    }
}

