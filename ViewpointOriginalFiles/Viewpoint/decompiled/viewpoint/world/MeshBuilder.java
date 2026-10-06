/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  zombie.tileDepth.TileGeometryFile$Box
 *  zombie.tileDepth.TileGeometryFile$Cylinder
 *  zombie.tileDepth.TileGeometryFile$Geometry
 *  zombie.tileDepth.TileGeometryFile$Polygon
 */
package viewpoint.world;

import java.util.Arrays;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import viewpoint.world.EarClip;
import viewpoint.world.TileMesh;
import zombie.tileDepth.TileGeometryFile;

public final class MeshBuilder {
    private static final int CYLINDER_SEGMENTS = 12;
    private static final Vector3f TO_ISO_CAMERA = new Vector3f(1.0f, 0.8164966f, 1.0f).normalize();
    private static final float UV_INSET = 1.0f;
    private static final float ON_FLOOR = 0.002f;
    private float[] data = new float[288];
    private int floats;
    private final Matrix4f m = new Matrix4f();
    private final Vector3f normal = new Vector3f();
    private boolean inset;
    private float insetX;
    private float insetY;
    private final Vector3f scratch = new Vector3f();
    private final Vector3f[] p = MeshBuilder.newVectors(8);
    private final Vector3f[] q = MeshBuilder.newVectors(8);

    TileMesh build() {
        return new TileMesh(Arrays.copyOf(this.data, this.floats));
    }

    void add(TileGeometryFile.Geometry geometry) {
        if (geometry.isBox()) {
            this.box(geometry.asBox());
        } else if (geometry.isCylinder()) {
            this.cylinder(geometry.asCylinder());
        } else if (geometry.isPolygon()) {
            this.polygon(geometry.asPolygon());
        }
    }

    void box(TileGeometryFile.Box box) {
        this.transform(box.translate, box.rotate);
        this.box(box.min, box.max);
    }

    void box(Vector3f vector3f, Vector3f vector3f2) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 2; ++j) {
                this.normal.set(0.0f).setComponent(i, j == 0 ? -1.0f : 1.0f);
                boolean bl = this.face(false);
                int n = 1 << (i + 1) % 3;
                int n2 = 1 << (i + 2) % 3;
                int n3 = j == 0 ? 0 : 1 << i;
                int[] nArray = new int[]{n3, n3 | n, n3 | n | n2, n3 | n2};
                for (int k = 0; k < 4; ++k) {
                    this.corner(nArray[k], vector3f, vector3f2, this.p[k]);
                    this.corner(bl ? nArray[k] ^ 1 << i : nArray[k], vector3f, vector3f2, this.q[k]);
                }
                if (this.restsOnFloor(4)) continue;
                this.insetAround(4);
                this.quad();
            }
        }
    }

    private boolean restsOnFloor(int n) {
        if (this.normal.y > -0.99f) {
            return false;
        }
        for (int i = 0; i < n; ++i) {
            if (!(Math.abs(this.p[i].y) > 0.002f)) continue;
            return false;
        }
        return true;
    }

    void cylinder(TileGeometryFile.Cylinder cylinder) {
        this.transform(cylinder.translate, cylinder.rotate);
        float f = cylinder.height / 2.0f;
        float f2 = cylinder.radius1;
        for (int i = 0; i < 12; ++i) {
            double d = Math.PI * 2 * (double)i / 12.0;
            double d2 = Math.PI * 2 * (double)(i + 1) / 12.0;
            float f3 = f2 * (float)Math.cos(d);
            float f4 = f2 * (float)Math.sin(d);
            float f5 = f2 * (float)Math.cos(d2);
            float f6 = f2 * (float)Math.sin(d2);
            this.normal.set(f3 + f5, f4 + f6, 0.0f).normalize();
            float f7 = this.face(false) ? -1.0f : 1.0f;
            this.local(this.p[0], this.q[0], f3, f4, -f, f7, f7, 1.0f);
            this.local(this.p[1], this.q[1], f5, f6, -f, f7, f7, 1.0f);
            this.local(this.p[2], this.q[2], f5, f6, f, f7, f7, 1.0f);
            this.local(this.p[3], this.q[3], f3, f4, f, f7, f7, 1.0f);
            this.inset = false;
            this.quad();
            for (int j = -1; j <= 1; j += 2) {
                this.normal.set(0.0f, 0.0f, (float)j);
                f7 = this.face(false) ? -1.0f : 1.0f;
                this.local(this.p[0], this.q[0], 0.0f, 0.0f, (float)j * f, 1.0f, 1.0f, f7);
                this.local(this.p[1], this.q[1], f3, f4, (float)j * f, 1.0f, 1.0f, f7);
                this.local(this.p[2], this.q[2], f5, f6, (float)j * f, 1.0f, 1.0f, f7);
                if (this.restsOnFloor(3)) continue;
                this.insetAround(1);
                this.triangle(0, 1, 2);
            }
        }
    }

    void polygon(TileGeometryFile.Polygon polygon) {
        int n;
        this.transform(polygon.translate, polygon.rotate);
        int n2 = polygon.points.size() / 2;
        if (n2 < 3) {
            return;
        }
        float[] fArray = new float[n2 * 2];
        for (n = 0; n < fArray.length; ++n) {
            fArray[n] = polygon.points.getQuick(n);
        }
        this.normal.set(0.0f, 0.0f, 1.0f);
        this.face(true);
        this.insetY = 0.0f;
        this.insetX = 0.0f;
        for (n = 0; n < n2; ++n) {
            this.m.transformPosition(this.scratch.set(fArray[n * 2], fArray[n * 2 + 1], 0.0f));
            this.insetX += MeshBuilder.frameX(this.scratch) / (float)n2;
            this.insetY += MeshBuilder.frameY(this.scratch) / (float)n2;
        }
        this.inset = true;
        int[] nArray = EarClip.triangulate(fArray);
        for (int i = 0; i < nArray.length; i += 3) {
            for (int j = 0; j < 3; ++j) {
                int n3 = nArray[i + j];
                this.local(this.p[j], this.q[j], fArray[n3 * 2], fArray[n3 * 2 + 1], 0.0f, 1.0f, 1.0f, 1.0f);
            }
            this.triangle(0, 1, 2);
        }
    }

    void floor() {
        this.m.identity();
        this.normal.set(0.0f, 1.0f, 0.0f);
        this.face(true);
        this.local(this.p[0], this.q[0], -0.5f, 0.0f, -0.5f, 1.0f, 1.0f, 1.0f);
        this.local(this.p[1], this.q[1], 0.5f, 0.0f, -0.5f, 1.0f, 1.0f, 1.0f);
        this.local(this.p[2], this.q[2], 0.5f, 0.0f, 0.5f, 1.0f, 1.0f, 1.0f);
        this.local(this.p[3], this.q[3], -0.5f, 0.0f, 0.5f, 1.0f, 1.0f, 1.0f);
        this.insetAround(4);
        this.quad();
    }

    void crossed(float f, float f2, int n) {
        this.m.identity();
        this.normal.set(0.0f, 0.5f, 0.0f);
        this.inset = false;
        float f3 = f * 0.70710677f;
        for (int i = 0; i < n; ++i) {
            double d = -0.7853981633974483 + (double)i * Math.PI / (double)n;
            float f4 = (float)Math.cos(d) * f;
            float f5 = (float)Math.sin(d) * f;
            this.p[0].set(-f4, 0.0f, -f5);
            this.p[1].set(f4, 0.0f, f5);
            this.p[2].set(f4, f2, f5);
            this.p[3].set(-f4, f2, -f5);
            this.q[0].set(-f3, 0.0f, f3);
            this.q[1].set(f3, 0.0f, -f3);
            this.q[2].set(f3, f2, -f3);
            this.q[3].set(-f3, f2, f3);
            this.quad();
        }
    }

    void edgeQuad(int n, float f) {
        this.normal.set(0.0f).setComponent(n, 1.0f);
        int n2 = n == 2 ? 0 : 2;
        for (int i = 0; i < 4; ++i) {
            this.p[i].set(0.0f, i < 2 ? 0.0f : 2.4494896f, 0.0f).setComponent(n2, i == 0 || i == 3 ? -0.5f : 0.5f).setComponent(n, -0.5f);
            this.q[i].set((Vector3fc)this.p[i]).setComponent(n, f);
        }
        this.insetAround(4);
        this.quad();
    }

    private boolean face(boolean bl) {
        this.m.transformDirection(this.normal).normalize();
        return this.normal.dot((Vector3fc)TO_ISO_CAMERA) <= 0.0f;
    }

    private void transform(Vector3f vector3f, Vector3f vector3f2) {
        this.m.translation((Vector3fc)vector3f).rotateXYZ((float)Math.toRadians(vector3f2.x), (float)Math.toRadians(vector3f2.y), (float)Math.toRadians(vector3f2.z));
    }

    private void corner(int n, Vector3f vector3f, Vector3f vector3f2, Vector3f vector3f3) {
        vector3f3.set((n & 1) == 0 ? vector3f.x : vector3f2.x, (n & 2) == 0 ? vector3f.y : vector3f2.y, (n & 4) == 0 ? vector3f.z : vector3f2.z);
        this.m.transformPosition(vector3f3);
    }

    private void local(Vector3f vector3f, Vector3f vector3f2, float f, float f2, float f3, float f4, float f5, float f6) {
        this.m.transformPosition(vector3f.set(f, f2, f3));
        this.m.transformPosition(vector3f2.set(f * f4, f2 * f5, f3 * f6));
    }

    private void insetAround(int n) {
        this.insetY = 0.0f;
        this.insetX = 0.0f;
        for (int i = 0; i < n; ++i) {
            this.insetX += MeshBuilder.frameX(this.q[i]) / (float)n;
            this.insetY += MeshBuilder.frameY(this.q[i]) / (float)n;
        }
        this.inset = true;
    }

    private static float frameX(Vector3f vector3f) {
        return 32.0f * (vector3f.x - vector3f.z + 1.0f);
    }

    private static float frameY(Vector3f vector3f) {
        return 112.0f + 16.0f * (vector3f.x + vector3f.z) - 39.191833f * vector3f.y;
    }

    private void quad() {
        this.triangle(0, 1, 2);
        this.triangle(0, 2, 3);
    }

    private void triangle(int n, int n2, int n3) {
        this.vertex(this.p[n], this.q[n]);
        this.vertex(this.p[n2], this.q[n2]);
        this.vertex(this.p[n3], this.q[n3]);
    }

    private void vertex(Vector3f vector3f, Vector3f vector3f2) {
        float f;
        float f2;
        float f3;
        if (this.floats + 8 > this.data.length) {
            this.data = Arrays.copyOf(this.data, this.data.length * 2);
        }
        this.data[this.floats++] = vector3f.x;
        this.data[this.floats++] = vector3f.y;
        this.data[this.floats++] = vector3f.z;
        float f4 = MeshBuilder.frameX(vector3f2);
        float f5 = MeshBuilder.frameY(vector3f2);
        if (this.inset && (f3 = (float)Math.sqrt((f2 = this.insetX - f4) * f2 + (f = this.insetY - f5) * f)) > 1.0E-4f) {
            float f6 = Math.min(1.0f, f3 * 0.5f) / f3;
            f4 += f2 * f6;
            f5 += f * f6;
        }
        this.data[this.floats++] = f4;
        this.data[this.floats++] = f5;
        this.data[this.floats++] = this.normal.x;
        this.data[this.floats++] = this.normal.y;
        this.data[this.floats++] = this.normal.z;
    }

    private static Vector3f[] newVectors(int n) {
        Vector3f[] vector3fArray = new Vector3f[n];
        for (int i = 0; i < n; ++i) {
            vector3fArray[i] = new Vector3f();
        }
        return vector3fArray;
    }
}

