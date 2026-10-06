/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.util.Random;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

final class Noise2D {
    static final int SIZE = 128;
    private static final long SEED = 5314517L;

    static int create() {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)65536);
        byte[] byArray = new byte[65536];
        new Random(5314517L).nextBytes(byArray);
        byteBuffer.put(byArray).flip();
        int n = GL11.glGenTextures();
        GL11.glBindTexture((int)3553, (int)n);
        GL11.glTexImage2D((int)3553, (int)0, (int)32856, (int)128, (int)128, (int)0, (int)6408, (int)5121, (ByteBuffer)byteBuffer);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)10497);
        GL11.glTexParameteri((int)3553, (int)10243, (int)10497);
        GL11.glTexParameteri((int)3553, (int)33085, (int)0);
        return n;
    }

    private Noise2D() {
    }
}

