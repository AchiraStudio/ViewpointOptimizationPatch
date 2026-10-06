/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL45
 *  org.lwjgl.system.MemoryUtil
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryUtil;
import viewpoint.render.ArtShelves;
import viewpoint.render.PackData;
import viewpoint.render.PackModel;

final class PackArt {
    static final int SIZE = 2048;
    static final int GUTTER = 8;
    static final long PAGE_BYTES = PackArt.pageBytes();
    private static final ArrayList<Integer> textures = new ArrayList();
    private static final ArrayList<ArtShelves> shelves = new ArrayList();

    static boolean put(PackModel packModel, PackData packData, long l) {
        int n = packData.width() + 8;
        int n2 = packData.height() + 8;
        for (int i = 0; i < shelves.size(); ++i) {
            int[] nArray = shelves.get(i).take(n, n2);
            if (nArray == null) continue;
            PackArt.upload(packModel, packData, i, nArray);
            return true;
        }
        if (l < PAGE_BYTES || n > 2048 || n2 > 2048) {
            return false;
        }
        textures.add(PackArt.page());
        shelves.add(new ArtShelves(2048, 8));
        PackArt.upload(packModel, packData, shelves.size() - 1, shelves.get(shelves.size() - 1).take(n, n2));
        return true;
    }

    static void free(PackModel packModel) {
        shelves.get(packModel.artPage).give(packModel.artX, packModel.artY, packModel.artW + 8);
    }

    static int texture(int n) {
        return textures.get(n);
    }

    static long bytes() {
        return (long)textures.size() * PAGE_BYTES;
    }

    static int pages() {
        return textures.size();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void upload(PackModel packModel, PackData packData, int n, int[] nArray) {
        packModel.artPage = n;
        packModel.artX = nArray[0];
        packModel.artY = nArray[1];
        packModel.artW = packData.width();
        packModel.artH = packData.height();
        packModel.artWidth = packData.artWidth();
        packModel.artHeight = packData.artHeight();
        int n2 = packData.width() + 8;
        int n3 = packData.height() + 8;
        ByteBuffer byteBuffer = MemoryUtil.memCalloc((int)(n2 * n3 * 4));
        try {
            GL11.glPixelStorei((int)3317, (int)4);
            for (int i = 0; i < 4; ++i) {
                int n4 = n2 >> i;
                int n5 = n3 >> i;
                int n6 = packData.width() >> i;
                int n7 = packData.height() >> i;
                byte[] byArray = packData.levels()[i];
                byteBuffer.clear();
                MemoryUtil.memSet((ByteBuffer)byteBuffer, (int)0);
                for (int j = 0; j < n7; ++j) {
                    byteBuffer.position(j * n4 * 4);
                    byteBuffer.put(byArray, j * n6 * 4, n6 * 4);
                }
                byteBuffer.position(0).limit(n4 * n5 * 4);
                GL45.glTextureSubImage2D((int)textures.get(n), (int)i, (int)(nArray[0] >> i), (int)(nArray[1] >> i), (int)n4, (int)n5, (int)6408, (int)5121, (ByteBuffer)byteBuffer);
            }
        }
        finally {
            MemoryUtil.memFree((ByteBuffer)byteBuffer);
        }
    }

    private static int page() {
        int n = GL45.glCreateTextures((int)3553);
        GL45.glTextureStorage2D((int)n, (int)4, (int)32856, (int)2048, (int)2048);
        GL45.glTextureParameteri((int)n, (int)33085, (int)3);
        System.out.println("[Viewpoint] model packs: art page " + (textures.size() + 1) + " made");
        return n;
    }

    private static long pageBytes() {
        long l = 0L;
        for (int i = 0; i < 4; ++i) {
            l += (long)(2048 >> i) * (long)(2048 >> i) * 4L;
        }
        return l;
    }

    private PackArt() {
    }
}

