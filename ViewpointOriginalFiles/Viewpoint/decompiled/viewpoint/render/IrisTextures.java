/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL12
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import viewpoint.iris.PackSource;
import viewpoint.iris.ShadersProperties;
import viewpoint.render.IrisFormats;
import zombie.core.textures.PNGDecoder;

final class IrisTextures {
    private static final int IDAT = 1229209940;
    final Map<String, Texture> byStage = new HashMap<String, Texture>();
    final Map<String, Texture> byName = new HashMap<String, Texture>();
    final List<String> notes = new ArrayList<String>();
    Texture noise;
    Texture normals;
    Texture specular;
    Texture lightmap;
    Texture white;
    Texture clouds;
    long bytes;

    IrisTextures() {
    }

    void load(PackSource packSource, ShadersProperties shadersProperties, int n) {
        GL13.glActiveTexture((int)34079);
        this.white = IrisTextures.solid(255, 255, 255, 255);
        this.normals = IrisTextures.solid(128, 128, 255, 255);
        this.specular = IrisTextures.solid(0, 0, 0, 0);
        this.lightmap = this.lightmapTexture();
        this.clouds = this.cloudMask();
        String string = shadersProperties.flag("texture.noise", null);
        this.noise = string == null ? this.randomNoise(n) : this.read(packSource, string, "texture.noise");
        for (Map.Entry<String, String> entry : shadersProperties.textures.entrySet()) {
            String string2 = entry.getKey().replaceAll("\\.\\d+$", "");
            this.byStage.put(string2, this.read(packSource, entry.getValue(), "texture." + entry.getKey()));
        }
        for (Map.Entry<String, String> entry : shadersProperties.customTextures.entrySet()) {
            this.byName.put(entry.getKey(), this.read(packSource, entry.getValue(), "customTexture." + entry.getKey()));
        }
        GL13.glActiveTexture((int)33984);
    }

    Texture custom(String string, String string2) {
        Texture texture = this.byStage.get(string + "." + string2);
        return texture != null ? texture : this.byName.get(string2);
    }

    private Texture read(PackSource packSource, String string, String string2) {
        List<String> list = ShadersProperties.words(string);
        try {
            if (list.isEmpty()) {
                throw new IOException("no path");
            }
            String string3 = list.get(0);
            if (string3.startsWith("minecraft:")) {
                return string3.endsWith("clouds.png") ? this.clouds : this.white;
            }
            byte[] byArray = packSource.readBytes(string3.startsWith("/") ? string3.substring(1) : string3);
            if (byArray == null) {
                throw new IOException("no file " + string3);
            }
            Texture texture = list.size() > 1 ? this.raw(byArray, list) : this.png(byArray);
            IrisTextures.filtering(texture, packSource.readBytes((string3.startsWith("/") ? string3.substring(1) : string3) + ".mcmeta"));
            return texture;
        }
        catch (IOException | RuntimeException exception) {
            this.notes.add(string2 + ": " + exception.getMessage() + " (reads white)");
            return this.white;
        }
    }

    private static void filtering(Texture texture, byte[] byArray) {
        String string = byArray == null ? "" : new String(byArray, StandardCharsets.UTF_8).replaceAll("\\s", "");
        int n = string.contains("\"blur\":true") ? 9729 : 9728;
        int n2 = string.contains("\"clamp\":true") ? 33071 : 10497;
        GL11.glBindTexture((int)texture.target(), (int)texture.id());
        GL11.glTexParameteri((int)texture.target(), (int)10241, (int)n);
        GL11.glTexParameteri((int)texture.target(), (int)10240, (int)n);
        GL11.glTexParameteri((int)texture.target(), (int)10242, (int)n2);
        GL11.glTexParameteri((int)texture.target(), (int)10243, (int)n2);
        GL11.glTexParameteri((int)texture.target(), (int)32882, (int)n2);
        GL11.glBindTexture((int)texture.target(), (int)0);
    }

    private Texture png(byte[] byArray) throws IOException {
        PNGDecoder pNGDecoder = new PNGDecoder(new ByteArrayInputStream(byArray), false);
        int n = pNGDecoder.getWidth();
        int n2 = pNGDecoder.getHeight();
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)(n * n2 * 4));
        pNGDecoder.decode(byteBuffer, n * 4, n2, PNGDecoder.Format.RGBA, 1229209940);
        byteBuffer.flip();
        int n3 = IrisTextures.texture2D(32856, n, n2, 6408, 5121, byteBuffer, 10497);
        this.bytes += (long)n * (long)n2 * 4L;
        return new Texture(3553, n3);
    }

    private Texture raw(byte[] byArray, List<String> list) throws IOException {
        int n;
        String string = list.get(1).toUpperCase(Locale.ROOT);
        IrisFormats.Format format = IrisFormats.format(list.get(2));
        int n2 = string.equals("TEXTURE_3D") ? 3 : (n = string.equals("TEXTURE_1D") ? 1 : 2);
        if (format == null || list.size() < 3 + n + 2) {
            throw new IOException("cannot read the raw texture's line " + String.join((CharSequence)" ", list));
        }
        int n3 = Integer.parseInt(list.get(3));
        int n4 = n > 1 ? Integer.parseInt(list.get(4)) : 1;
        int n5 = n > 2 ? Integer.parseInt(list.get(5)) : 1;
        int n6 = IrisFormats.pixelFormat(list.get(3 + n));
        int n7 = IrisFormats.pixelType(list.get(4 + n));
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)byArray.length).put(byArray).flip();
        int n8 = GL11.glGenTextures();
        int n9 = n == 3 ? 32879 : (n == 1 ? 3552 : 3553);
        GL11.glBindTexture((int)n9, (int)n8);
        GL11.glPixelStorei((int)3317, (int)1);
        if (n == 3) {
            GL12.glTexImage3D((int)n9, (int)0, (int)format.internal(), (int)n3, (int)n4, (int)n5, (int)0, (int)n6, (int)n7, (ByteBuffer)byteBuffer);
        } else if (n == 1) {
            GL11.glTexImage1D((int)n9, (int)0, (int)format.internal(), (int)n3, (int)0, (int)n6, (int)n7, (ByteBuffer)byteBuffer);
        } else {
            GL11.glTexImage2D((int)n9, (int)0, (int)format.internal(), (int)n3, (int)n4, (int)0, (int)n6, (int)n7, (ByteBuffer)byteBuffer);
        }
        GL11.glPixelStorei((int)3317, (int)4);
        GL11.glTexParameteri((int)n9, (int)10241, (int)9729);
        GL11.glTexParameteri((int)n9, (int)10240, (int)9729);
        GL11.glTexParameteri((int)n9, (int)10242, (int)10497);
        GL11.glTexParameteri((int)n9, (int)10243, (int)10497);
        GL11.glTexParameteri((int)n9, (int)32882, (int)10497);
        GL11.glBindTexture((int)n9, (int)0);
        this.bytes += (long)byArray.length;
        return new Texture(n9, n8);
    }

    private Texture randomNoise(int n) {
        Random random = new Random(1L);
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)(n * n * 4));
        for (int i = 0; i < n * n * 4; ++i) {
            byteBuffer.put((byte)random.nextInt(256));
        }
        byteBuffer.flip();
        this.bytes += (long)n * (long)n * 4L;
        return new Texture(3553, IrisTextures.texture2D(32856, n, n, 6408, 5121, byteBuffer, 10497));
    }

    private Texture lightmapTexture() {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)1024);
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                float f = (float)j / 15.0f;
                float f2 = (float)i / 15.0f;
                byteBuffer.put((byte)(255.0f * Math.max(f, f2))).put((byte)(255.0f * Math.max(f * 0.85f, f2))).put((byte)(255.0f * Math.max(f * 0.7f, f2))).put((byte)-1);
            }
        }
        byteBuffer.flip();
        return new Texture(3553, IrisTextures.texture2D(32856, 16, 16, 6408, 5121, byteBuffer, 33071));
    }

    private Texture cloudMask() {
        int n;
        int n2 = 256;
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)(n2 * n2 * 4));
        Random random = new Random(7L);
        float[] fArray = new float[256];
        for (n = 0; n < fArray.length; ++n) {
            fArray[n] = random.nextFloat();
        }
        for (n = 0; n < n2; ++n) {
            for (int i = 0; i < n2; ++i) {
                float f = fArray[n / 16 * 16 + i / 16];
                byte by = (byte)(f > 0.55f ? 255 : 0);
                byteBuffer.put(by).put(by).put(by).put(by);
            }
        }
        byteBuffer.flip();
        return new Texture(3553, IrisTextures.texture2D(32856, n2, n2, 6408, 5121, byteBuffer, 10497));
    }

    private static Texture solid(int n, int n2, int n3, int n4) {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)4).put((byte)n).put((byte)n2).put((byte)n3).put((byte)n4);
        byteBuffer.flip();
        return new Texture(3553, IrisTextures.texture2D(32856, 1, 1, 6408, 5121, byteBuffer, 10497));
    }

    private static int texture2D(int n, int n2, int n3, int n4, int n5, ByteBuffer byteBuffer, int n6) {
        int n7 = GL11.glGenTextures();
        GL11.glBindTexture((int)3553, (int)n7);
        GL11.glTexImage2D((int)3553, (int)0, (int)n, (int)n2, (int)n3, (int)0, (int)n4, (int)n5, (ByteBuffer)byteBuffer);
        GL30.glGenerateMipmap((int)3553);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9987);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)n6);
        GL11.glTexParameteri((int)3553, (int)10243, (int)n6);
        GL11.glBindTexture((int)3553, (int)0);
        return n7;
    }

    void release() {
        HashSet<Integer> hashSet = new HashSet<Integer>();
        for (Texture texture2 : new Texture[]{this.noise, this.normals, this.specular, this.lightmap, this.white, this.clouds}) {
            if (texture2 == null) continue;
            hashSet.add(texture2.id());
        }
        this.byStage.values().forEach(texture -> hashSet.add(texture.id()));
        this.byName.values().forEach(texture -> hashSet.add(texture.id()));
        hashSet.forEach(GL11::glDeleteTextures);
        this.byStage.clear();
        this.byName.clear();
        this.bytes = 0L;
    }

    record Texture(int target, int id) {
    }
}

