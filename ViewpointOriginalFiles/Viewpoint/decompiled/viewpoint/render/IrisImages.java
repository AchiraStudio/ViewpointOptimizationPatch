/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL42
 *  org.lwjgl.opengl.GL43
 *  org.lwjgl.opengl.GL44
 */
package viewpoint.render;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import viewpoint.iris.ShadersProperties;
import viewpoint.render.IrisFormats;
import viewpoint.render.IrisProgram;

final class IrisImages {
    final Map<String, Image> images = new LinkedHashMap<String, Image>();
    final Map<Integer, Integer> buffers = new LinkedHashMap<Integer, Integer>();
    final List<String> notes = new ArrayList<String>();
    long bytes;

    IrisImages() {
    }

    void load(ShadersProperties shadersProperties, int n, int n2) {
        GL13.glActiveTexture((int)34079);
        for (Map.Entry<String, String> entry : shadersProperties.images.entrySet()) {
            try {
                this.image(entry.getKey(), ShadersProperties.words(entry.getValue()), n, n2);
            }
            catch (RuntimeException runtimeException) {
                this.notes.add("image." + entry.getKey() + ": " + runtimeException.getMessage());
            }
        }
        for (Map.Entry<Object, String> entry : shadersProperties.bufferObjects.entrySet()) {
            long l;
            List<String> list = ShadersProperties.words(entry.getValue());
            long l2 = l = list.isEmpty() ? 0L : Long.parseLong(list.get(0));
            if (list.size() >= 4 && list.get(1).equalsIgnoreCase("true")) {
                l = (long)((float)l * Float.parseFloat(list.get(2)) * (float)n * Float.parseFloat(list.get(3)) * (float)n2);
            }
            int n3 = GL15.glGenBuffers();
            GL15.glBindBuffer((int)37074, (int)n3);
            GL15.glBufferData((int)37074, (long)l, (int)35050);
            GL43.glClearBufferData((int)37074, (int)33330, (int)36244, (int)5121, (ByteBuffer)null);
            GL15.glBindBuffer((int)37074, (int)0);
            this.buffers.put((Integer)entry.getKey(), n3);
            this.bytes += l;
        }
        GL13.glActiveTexture((int)33984);
    }

    private void image(String string, List<String> list, int n, int n2) {
        int n3;
        IrisFormats.Format format = IrisFormats.format(list.get(2));
        if (format == null || list.size() < 7) {
            throw new IllegalArgumentException("cannot read " + String.join((CharSequence)" ", list));
        }
        boolean bl = list.get(5).equalsIgnoreCase("true");
        int n4 = list.size() - 6;
        int[] nArray = new int[3];
        for (n3 = 0; n3 < 3; ++n3) {
            float f;
            float f2 = f = n3 < n4 ? Float.parseFloat(list.get(6 + n3)) : 1.0f;
            nArray[n3] = bl && n3 < 2 ? Math.max(1, Math.round(f * (float)(n3 == 0 ? n : n2))) : (int)f;
        }
        n3 = n4 >= 3 ? 32879 : (n4 == 2 ? 3553 : 3552);
        int n5 = GL11.glGenTextures();
        GL11.glBindTexture((int)n3, (int)n5);
        int n6 = IrisFormats.pixelFormat(list.get(1));
        int n7 = IrisFormats.pixelType(list.get(3));
        if (n3 == 32879) {
            GL42.glTexStorage3D((int)n3, (int)1, (int)format.internal(), (int)nArray[0], (int)nArray[1], (int)nArray[2]);
        } else if (n3 == 3553) {
            GL42.glTexStorage2D((int)n3, (int)1, (int)format.internal(), (int)nArray[0], (int)nArray[1]);
        } else {
            GL42.glTexStorage1D((int)n3, (int)1, (int)format.internal(), (int)nArray[0]);
        }
        int n8 = format.integer() ? 9728 : 9729;
        GL11.glTexParameteri((int)n3, (int)10241, (int)n8);
        GL11.glTexParameteri((int)n3, (int)10240, (int)n8);
        GL11.glTexParameteri((int)n3, (int)10242, (int)33071);
        GL11.glTexParameteri((int)n3, (int)10243, (int)33071);
        GL11.glTexParameteri((int)n3, (int)32882, (int)33071);
        GL11.glBindTexture((int)n3, (int)0);
        Image image = new Image(string, list.get(0), n5, n3, format, nArray, list.get(4).equalsIgnoreCase("true"), n6, n7);
        this.images.put(string, image);
        GL44.glClearTexImage((int)n5, (int)0, (int)n6, (int)n7, (ByteBuffer)null);
        this.bytes += (long)nArray[0] * (long)nArray[1] * (long)nArray[2] * (long)format.bytes();
    }

    void frame() {
        for (Image object : this.images.values()) {
            if (!object.clear()) continue;
            GL44.glClearTexImage((int)object.texture(), (int)0, (int)object.pixels(), (int)object.type(), (ByteBuffer)null);
        }
        for (Map.Entry entry : this.buffers.entrySet()) {
            GL30.glBindBufferBase((int)37074, (int)((Integer)entry.getKey()), (int)((Integer)entry.getValue()));
        }
    }

    void bind(IrisProgram irisProgram) {
        for (Map.Entry<String, Integer> entry : irisProgram.images.entrySet()) {
            Image image = this.images.get(entry.getKey());
            if (image == null) continue;
            GL42.glBindImageTexture((int)entry.getValue(), (int)image.texture(), (int)0, (image.target() == 32879 ? 1 : 0) != 0, (int)0, (int)35002, (int)image.format().internal());
        }
    }

    int sampler(String string, int n) {
        for (Image image : this.images.values()) {
            if (!image.sampler().equals(string) || image.target() != n) continue;
            return image.texture();
        }
        return 0;
    }

    void release() {
        this.images.values().forEach(image -> GL11.glDeleteTextures((int)image.texture()));
        this.buffers.values().forEach(GL15::glDeleteBuffers);
        this.images.clear();
        this.buffers.clear();
        this.bytes = 0L;
    }

    record Image(String name, String sampler, int texture, int target, IrisFormats.Format format, int[] size, boolean clear, int pixels, int type) {
    }
}

