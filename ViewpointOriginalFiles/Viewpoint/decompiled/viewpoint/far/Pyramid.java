/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

final class Pyramid {
    private static final ConcurrentHashMap<String, Pyramid> pyramids = new ConcurrentHashMap();
    private ZipFile zip;
    private int boundsX;
    private int boundsY;

    static Pyramid of(String string) {
        return pyramids.computeIfAbsent(string, Pyramid::new);
    }

    private Pyramid(String string) {
        block10: {
            try {
                File file = new File(string, "pyramid.zip");
                if (!file.isFile()) {
                    return;
                }
                this.zip = new ZipFile(file);
                ZipEntry zipEntry = this.zip.getEntry("pyramid.txt");
                if (zipEntry == null) break block10;
                try (InputStream inputStream = this.zip.getInputStream(zipEntry);){
                    for (String string2 : new String(inputStream.readAllBytes()).split("\\R")) {
                        if (!string2.startsWith("bounds=")) continue;
                        String[] stringArray = string2.substring(7).trim().split("\\s+");
                        this.boundsX = Integer.parseInt(stringArray[0]);
                        this.boundsY = Integer.parseInt(stringArray[1]);
                    }
                }
            }
            catch (Exception exception) {
                System.out.println("[Viewpoint] far world: no map image in " + string + ": " + String.valueOf(exception));
                this.zip = null;
            }
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    int[] image(int n, int n2) {
        int n3 = n * 256 - this.boundsX;
        int n4 = n2 * 256 - this.boundsY;
        if (this.zip == null) return null;
        if (n3 < 0) return null;
        if (n4 < 0) return null;
        if (n3 % 256 != 0) return null;
        if (n4 % 256 != 0) {
            return null;
        }
        ZipEntry zipEntry = this.zip.getEntry("0/tile" + n3 / 256 + "x" + n4 / 256 + ".png");
        if (zipEntry == null) {
            return null;
        }
        try (InputStream inputStream = this.zip.getInputStream(zipEntry);){
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage == null || bufferedImage.getWidth() != 256 || bufferedImage.getHeight() != 256) {
                int[] nArray2 = null;
                return nArray2;
            }
            int[] nArray = bufferedImage.getRGB(0, 0, 256, 256, null, 0, 256);
            return nArray;
        }
        catch (Exception exception) {
            return null;
        }
    }
}

