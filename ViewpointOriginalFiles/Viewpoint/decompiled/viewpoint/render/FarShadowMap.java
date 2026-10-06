/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL33
 */
package viewpoint.render;

import java.util.Arrays;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import viewpoint.platform.Gl;
import viewpoint.render.SceneData;

final class FarShadowMap {
    static final int NEAR = 0;
    static final int SHELL = 1;
    static final int INSIDES = 2;
    static final int BOXES = 3;
    static final int TREES = 4;
    private static final String[] GROUPS = new String[]{"near", "shell", "insides", "boxes", "trees"};
    private final int size;
    private final int unit;
    private final int format;
    final float recentre;
    final Matrix4f matrix = new Matrix4f();
    final Matrix4f now = new Matrix4f();
    float centreSceneX;
    float centreSceneY;
    float centreSceneZ;
    int age = Integer.MAX_VALUE;
    boolean valid;
    long drawnFar;
    long drawnNear;
    float reach;
    float depth;
    private final Matrix4f lightView = new Matrix4f();
    private int fbo;
    private int texture;
    private int centreX;
    private int centreY;
    private float sunX;
    private float sunY;
    private float sunZ;
    private double originX;
    private double originY;
    private double originZ;
    private final int[] pieces = new int[GROUPS.length];
    private final int[] queries = new int[GROUPS.length];
    private final long[] vertices = new long[GROUPS.length];
    private final boolean[] asked = new boolean[GROUPS.length];

    FarShadowMap(int n, float f, float f2, float f3, int n2, int n3) {
        this.size = n;
        this.reach = f;
        this.depth = f2;
        this.recentre = f3;
        this.unit = n2;
        this.format = n3;
    }

    void span(float f, float f2) {
        if (f != this.reach || f2 != this.depth) {
            this.reach = f;
            this.depth = f2;
            this.valid = false;
        }
    }

    boolean moved(SceneData sceneData, double d, double d2) {
        boolean bl = Math.abs(d - (double)this.centreX) > (double)this.recentre || Math.abs(d2 - (double)this.centreY) > (double)this.recentre;
        return bl || sceneData.sunX * this.sunX + sceneData.sunY * this.sunY + sceneData.sunZ * this.sunZ < 0.99995f;
    }

    void begin(SceneData sceneData, double d, double d2) {
        if (this.fbo == 0) {
            this.makeTarget();
        }
        this.age = 0;
        this.valid = true;
        this.centreX = (int)Math.round(d / (double)this.recentre) * (int)this.recentre;
        this.centreY = (int)Math.round(d2 / (double)this.recentre) * (int)this.recentre;
        this.sunX = sceneData.sunX;
        this.sunY = sceneData.sunY;
        this.sunZ = sceneData.sunZ;
        this.originX = sceneData.originX;
        this.originY = sceneData.originY;
        this.originZ = sceneData.originZ;
        this.centreSceneX = (float)(d - (double)this.centreX);
        this.centreSceneY = (float)(-sceneData.originY);
        this.centreSceneZ = (float)(d2 - (double)this.centreY);
        boolean bl = Math.abs(sceneData.sunY) > 0.99f;
        this.lightView.setLookAt(this.centreSceneX + sceneData.sunX * this.depth * 0.5f, this.centreSceneY + sceneData.sunY * this.depth * 0.5f, this.centreSceneZ + sceneData.sunZ * this.depth * 0.5f, this.centreSceneX, this.centreSceneY, this.centreSceneZ, 0.0f, bl ? 0.0f : 1.0f, bl ? 1.0f : 0.0f);
        this.matrix.setOrtho(-this.reach, this.reach, -this.reach, this.reach, 0.0f, this.depth).mul((Matrix4fc)this.lightView);
        GL30.glBindFramebuffer((int)36160, (int)this.fbo);
        GL11.glViewport((int)0, (int)0, (int)this.size, (int)this.size);
        GL11.glEnable((int)2929);
        GL11.glDepthFunc((int)513);
        GL11.glDepthMask((boolean)true);
        GL11.glClear((int)256);
        GL11.glEnable((int)32823);
        GL11.glPolygonOffset((float)1.5f, (float)2.0f);
        Arrays.fill(this.pieces, 0);
        Arrays.fill(this.vertices, 0L);
        Arrays.fill(this.asked, false);
    }

    void count(int n) {
        if (this.queries[0] == 0) {
            GL15.glGenQueries((int[])this.queries);
        }
        GL15.glBeginQuery((int)35092, (int)this.queries[n]);
        this.asked[n] = true;
    }

    void counted(int n, int n2, long l) {
        GL15.glEndQuery((int)35092);
        int n3 = n;
        this.pieces[n3] = this.pieces[n3] + n2;
        int n4 = n;
        this.vertices[n4] = this.vertices[n4] + l;
    }

    String report() {
        if (!this.valid) {
            return "none";
        }
        StringBuilder stringBuilder = new StringBuilder(String.format("about %d,%d, %.0f squares each way", this.centreX, this.centreY, Float.valueOf(this.reach)));
        for (int i = 0; i < GROUPS.length; ++i) {
            stringBuilder.append(String.format(", %s %d (%.2fM vertices, %s samples)", GROUPS[i], this.pieces[i], (double)this.vertices[i] / 1000000.0, this.samples(i)));
        }
        return stringBuilder.toString();
    }

    private String samples(int n) {
        if (!this.asked[n]) {
            return "no";
        }
        if (GL15.glGetQueryObjecti((int)this.queries[n], (int)34919) == 0) {
            return "pending";
        }
        return String.format("%.2fM", (double)GL33.glGetQueryObjecti64((int)this.queries[n], (int)34918) / 1000000.0);
    }

    void end() {
        GL30.glBindVertexArray((int)0);
        GL11.glDisable((int)32823);
    }

    void carry(SceneData sceneData) {
        this.now.set((Matrix4fc)this.matrix).translate((float)(sceneData.originX - this.originX), (float)(sceneData.originY - this.originY), (float)(sceneData.originZ - this.originZ));
    }

    Matrix4f textureMatrix(Matrix4f matrix4f) {
        return matrix4f.translation(0.5f, 0.5f, 0.5f).scale(0.5f).mul((Matrix4fc)this.now);
    }

    float texelSquares() {
        return this.valid ? 2.0f * this.reach / (float)this.size : 0.0f;
    }

    float texelStep() {
        return 1.0f / (float)this.size;
    }

    void bind(boolean bl) {
        Gl.bind(this.unit, bl ? this.texture : 0);
    }

    private void makeTarget() {
        GL13.glActiveTexture((int)(33984 + this.unit));
        this.texture = Gl.texture(this.format, this.size, this.size, 6402, 5126, 9729);
        GL11.glTexParameteri((int)3553, (int)34892, (int)34894);
        GL11.glTexParameteri((int)3553, (int)34893, (int)515);
        this.fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer((int)36160, (int)this.fbo);
        GL30.glFramebufferTexture2D((int)36160, (int)36096, (int)3553, (int)this.texture, (int)0);
        GL11.glDrawBuffer((int)0);
        GL11.glReadBuffer((int)0);
        GL13.glActiveTexture((int)33984);
    }
}

