/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  zombie.ZomboidFileSystem
 */
package viewpoint;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import zombie.ZomboidFileSystem;

final class Screenshot {
    static volatile boolean requested;

    static void captureIfRequested() {
        if (!requested) {
            return;
        }
        requested = false;
        int[] nArray = new int[2];
        ByteBuffer byteBuffer = Screenshot.read(nArray);
        Thread thread = new Thread(() -> Screenshot.write(byteBuffer, nArray[0], nArray[1]), "Viewpoint screenshot");
        thread.setDaemon(true);
        thread.start();
    }

    static ByteBuffer read(int[] nArray) {
        int[] nArray2 = new int[4];
        GL11.glGetIntegerv((int)2978, (int[])nArray2);
        nArray[0] = nArray2[2];
        nArray[1] = nArray2[3];
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)(nArray[0] * nArray[1] * 4));
        GL11.glPixelStorei((int)3333, (int)1);
        GL11.glReadPixels((int)nArray2[0], (int)nArray2[1], (int)nArray[0], (int)nArray[1], (int)6408, (int)5121, (ByteBuffer)byteBuffer);
        return byteBuffer;
    }

    static BufferedImage image(ByteBuffer byteBuffer, int n, int n2) {
        BufferedImage bufferedImage = new BufferedImage(n, n2, 1);
        for (int i = 0; i < n2; ++i) {
            for (int j = 0; j < n; ++j) {
                int n3 = (i * n + j) * 4;
                int n4 = (byteBuffer.get(n3) & 0xFF) << 16 | (byteBuffer.get(n3 + 1) & 0xFF) << 8 | byteBuffer.get(n3 + 2) & 0xFF;
                bufferedImage.setRGB(j, n2 - 1 - i, n4);
            }
        }
        return bufferedImage;
    }

    static File folder() {
        File file = new File(ZomboidFileSystem.instance.getCacheDir(), "viewpoint-shots");
        file.mkdirs();
        return file;
    }

    private static void write(ByteBuffer byteBuffer, int n, int n2) {
        try {
            File file = new File(Screenshot.folder(), "shot-" + System.currentTimeMillis() + ".png");
            ImageIO.write((RenderedImage)Screenshot.image(byteBuffer, n, n2), "png", file);
            System.out.println("[Viewpoint] screenshot " + String.valueOf(file));
        }
        catch (Exception exception) {
            System.out.println("[Viewpoint] screenshot failed: " + String.valueOf(exception));
        }
    }

    private Screenshot() {
    }
}

