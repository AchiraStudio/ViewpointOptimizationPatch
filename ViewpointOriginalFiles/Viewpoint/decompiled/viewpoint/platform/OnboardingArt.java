/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.platform;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

final class OnboardingArt {
    private static final String BANNER = "/viewpoint/onboarding/banner.jpg";
    private static final String LOGO = "/viewpoint/onboarding/logo.png";
    private static volatile Texels[] read;
    private static final AtomicBoolean started;
    private static Picture banner;
    private static Picture logo;

    static void preload() {
        if (started.compareAndSet(false, true)) {
            Thread thread = new Thread(OnboardingArt::readAll, "Viewpoint setup pictures");
            thread.setDaemon(true);
            thread.start();
        }
    }

    static Picture banner() {
        OnboardingArt.ready();
        return banner;
    }

    static Picture logo() {
        OnboardingArt.ready();
        return logo;
    }

    private static void ready() {
        if (banner != null) {
            return;
        }
        OnboardingArt.preload();
        Texels[] texelsArray = read;
        if (texelsArray != null && texelsArray.length == 2) {
            banner = OnboardingArt.upload(texelsArray[0]);
            logo = OnboardingArt.upload(texelsArray[1]);
        }
    }

    private static void readAll() {
        try {
            read = new Texels[]{OnboardingArt.decode(BANNER), OnboardingArt.decode(LOGO)};
        }
        catch (Exception exception) {
            read = new Texels[0];
            System.out.println("[Viewpoint] setup's pictures not read: " + String.valueOf(exception));
        }
    }

    private static Texels decode(String string) throws Exception {
        BufferedImage bufferedImage;
        try (InputStream inputStream = OnboardingArt.class.getResourceAsStream(string);){
            if (inputStream == null) {
                throw new IllegalStateException("no " + string + " in the JAR");
            }
            bufferedImage = ImageIO.read(inputStream);
        }
        int n = bufferedImage.getWidth();
        int n2 = bufferedImage.getHeight();
        int[] nArray = bufferedImage.getRGB(0, 0, n, n2, null, 0, n);
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)(n * n2 * 4));
        for (int n3 : nArray) {
            byteBuffer.put((byte)(n3 >> 16)).put((byte)(n3 >> 8)).put((byte)n3).put((byte)(n3 >>> 24));
        }
        byteBuffer.flip();
        return new Texels(byteBuffer, n, n2);
    }

    private static Picture upload(Texels texels) {
        int n = GL11.glGetInteger((int)32873);
        int n2 = GL11.glGetInteger((int)35055);
        int n3 = GL11.glGetInteger((int)3317);
        int n4 = GL11.glGetInteger((int)3314);
        GL15.glBindBuffer((int)35052, (int)0);
        GL11.glPixelStorei((int)3317, (int)4);
        GL11.glPixelStorei((int)3314, (int)0);
        int n5 = GL11.glGenTextures();
        GL11.glBindTexture((int)3553, (int)n5);
        GL11.glTexImage2D((int)3553, (int)0, (int)32856, (int)texels.width(), (int)texels.height(), (int)0, (int)6408, (int)5121, (ByteBuffer)texels.rgba());
        GL30.glGenerateMipmap((int)3553);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9987);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glBindTexture((int)3553, (int)n);
        GL11.glPixelStorei((int)3317, (int)n3);
        GL11.glPixelStorei((int)3314, (int)n4);
        GL15.glBindBuffer((int)35052, (int)n2);
        return new Picture(n5, texels.width(), texels.height());
    }

    private OnboardingArt() {
    }

    static {
        started = new AtomicBoolean();
    }

    record Picture(int texture, int width, int height) {
        float aspect() {
            return (float)this.width / (float)this.height;
        }
    }

    private record Texels(ByteBuffer rgba, int width, int height) {
    }
}

