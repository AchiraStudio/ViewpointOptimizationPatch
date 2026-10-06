/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL42
 */
package viewpoint.render;

import java.util.ArrayList;
import java.util.Arrays;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL42;
import viewpoint.render.Retirement;

final class FloorSlices {
    static final int LAYERS = 32;
    final int size;
    final int mips;
    final int perArray;
    private final ArrayList<int[]> arrays = new ArrayList();
    private boolean[] used = new boolean[0];
    private int inUse;

    FloorSlices(int n, int n2) {
        this(n, n2, 32);
    }

    FloorSlices(int n, int n2, int n3) {
        this.size = n;
        this.mips = n2;
        this.perArray = Math.max(1, Math.min(32, n3));
    }

    long sliceBytes() {
        long l = 0L;
        for (int i = 0; i < this.mips; ++i) {
            long l2 = Math.max(1, this.size >> i);
            l += l2 * l2 * 4L;
        }
        return l;
    }

    long arrayBytes() {
        return this.sliceBytes() * (long)this.perArray;
    }

    long madeBytes() {
        int n = 0;
        for (int[] nArray : this.arrays) {
            n += nArray != null ? 1 : 0;
        }
        return (long)n * this.arrayBytes();
    }

    int inUse() {
        return this.inUse;
    }

    int take(long l) {
        int n;
        for (n = 0; n < this.used.length; ++n) {
            if (this.used[n] || n % 32 >= this.perArray || this.arrays.get(n / 32) == null) continue;
            return this.claim(n);
        }
        if (this.madeBytes() + this.arrayBytes() > l) {
            return -1;
        }
        n = this.arrays.indexOf(null);
        n = n < 0 ? this.arrays.size() : n;
        int n2 = this.make();
        if (n == this.arrays.size()) {
            this.arrays.add(new int[]{n2, 0});
            this.used = Arrays.copyOf(this.used, this.arrays.size() * 32);
        } else {
            this.arrays.set(n, new int[]{n2, 0});
        }
        return this.claim(n * 32);
    }

    private int claim(int n) {
        this.used[n] = true;
        int[] nArray = this.arrays.get(n / 32);
        nArray[1] = nArray[1] + 1;
        ++this.inUse;
        return n;
    }

    void free(int n) {
        int[] nArray;
        if (n < 0 || !this.used[n]) {
            return;
        }
        this.used[n] = false;
        --this.inUse;
        (nArray = this.arrays.get((int)(n / 32)))[1] = nArray[1] - 1;
        if ((nArray = this.arrays.get((int)(n / 32)))[1] == 0) {
            FloorSlices.letGo(nArray[0]);
            this.arrays.set(n / 32, null);
        }
    }

    Object array(int n) {
        return this.arrays.get(n / 32);
    }

    int texture(int n) {
        return this.arrays.get(n / 32)[0];
    }

    static int layer(int n) {
        return n % 32;
    }

    void clear() {
        for (int[] nArray : this.arrays) {
            if (nArray == null) continue;
            FloorSlices.letGo(nArray[0]);
        }
        this.arrays.clear();
        this.used = new boolean[0];
        this.inUse = 0;
    }

    private static void letGo(int n) {
        Retirement.retireDrawing(() -> GL11.glDeleteTextures((int)n));
    }

    private int make() {
        int n = GL11.glGetInteger((int)35869);
        int n2 = GL11.glGenTextures();
        GL11.glBindTexture((int)35866, (int)n2);
        GL42.glTexStorage3D((int)35866, (int)this.mips, (int)32856, (int)this.size, (int)this.size, (int)this.perArray);
        GL11.glTexParameteri((int)35866, (int)10241, (int)9987);
        GL11.glTexParameteri((int)35866, (int)10240, (int)9729);
        GL11.glTexParameteri((int)35866, (int)10242, (int)33071);
        GL11.glTexParameteri((int)35866, (int)10243, (int)33071);
        GL11.glTexParameteri((int)35866, (int)33085, (int)(this.mips - 1));
        if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            float f = GL11.glGetFloat((int)34047);
            GL11.glTexParameterf((int)35866, (int)34046, (float)Math.min(16.0f, f));
        }
        GL11.glBindTexture((int)35866, (int)n);
        System.out.println("[Viewpoint] floor pages: an array of " + this.perArray + " slices of " + this.size + " texels made (" + (this.arrayBytes() >> 20) + " MB)");
        return n2;
    }
}

