/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.packs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import zombie.core.textures.PNGDecoder;

final class ArtReader {
    ArtReader() {
    }

    static Art read(File file) throws IOException {
        PNGDecoder pNGDecoder = new PNGDecoder(new ByteArrayInputStream(Files.readAllBytes(file.toPath())), false);
        int n = pNGDecoder.getWidth();
        int n2 = pNGDecoder.getHeight();
        int n3 = ArtReader.padded(n);
        int n4 = ArtReader.padded(n2);
        byte[] byArray = new byte[n3 * n4 * 4];
        pNGDecoder.decode(ByteBuffer.wrap(byArray), n3 * 4, n2, PNGDecoder.Format.RGBA, 1229209940);
        return ArtReader.levels(byArray, n, n2);
    }

    static Art levels(int[] nArray, int n, int n2) {
        int n3 = ArtReader.padded(n);
        byte[] byArray = new byte[n3 * ArtReader.padded(n2) * 4];
        for (int i = 0; i < n2; ++i) {
            for (int j = 0; j < n; ++j) {
                int n4 = nArray[i * n + j];
                int n5 = (i * n3 + j) * 4;
                byArray[n5] = (byte)(n4 >> 16);
                byArray[n5 + 1] = (byte)(n4 >> 8);
                byArray[n5 + 2] = (byte)n4;
                byArray[n5 + 3] = (byte)(n4 >>> 24);
            }
        }
        return ArtReader.levels(byArray, n, n2);
    }

    private static Art levels(byte[] byArray, int n, int n2) {
        int n3 = ArtReader.padded(n);
        int n4 = ArtReader.padded(n2);
        for (int i = 0; i < byArray.length; i += 4) {
            if (byArray[i + 3] != 0) continue;
            byArray[i + 2] = 0;
            byArray[i + 1] = 0;
            byArray[i] = 0;
        }
        byte[][] byArrayArray = new byte[4][];
        byArrayArray[0] = byArray;
        for (int i = 1; i < 4; ++i) {
            byArrayArray[i] = ArtReader.half(byArrayArray[i - 1], n3 >> i - 1, n4 >> i - 1);
        }
        return new Art(n, n2, n3, n4, byArrayArray);
    }

    private static byte[] half(byte[] byArray, int n, int n2) {
        int n3 = n / 2;
        int n4 = n2 / 2;
        byte[] byArray2 = new byte[n3 * n4 * 4];
        for (int i = 0; i < n4; ++i) {
            for (int j = 0; j < n3; ++j) {
                for (int k = 0; k < 4; ++k) {
                    int n5 = (i * 2 * n + j * 2) * 4 + k;
                    int n6 = n5 + n * 4;
                    int n7 = (byArray[n5] & 0xFF) + (byArray[n5 + 4] & 0xFF) + (byArray[n6] & 0xFF) + (byArray[n6 + 4] & 0xFF);
                    byArray2[(i * n3 + j) * 4 + k] = (byte)((n7 + 2) / 4);
                }
            }
        }
        return byArray2;
    }

    private static int padded(int n) {
        return (Math.max(n, 1) + 8 - 1) / 8 * 8;
    }

    record Art(int width, int height, int paddedWidth, int paddedHeight, byte[][] levels) {
    }
}

