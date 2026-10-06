/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.visibility;

import java.util.Arrays;
import viewpoint.visibility.SectorGraph;

final class Portals {
    static final int BUDGET = 65536;
    private static final float CLOSE = 0.75f;
    private static final float NEAR = 0.05f;
    private static final float SIDE = 0.05f;
    private static final int MAX_RECTS = 6;
    private float[] rects = new float[4096];
    private int[] rectSector = new int[1024];
    private int[] rectNext = new int[1024];
    private int rectCount;
    private int[] head = new int[0];
    private int[] count = new int[0];
    private int[] queue = new int[1024];
    private final float[] corners = new float[12];
    private final float[] clipped = new float[24];
    final float[] projected = new float[4];

    Portals() {
    }

    boolean run(SectorGraph sectorGraph, byte[] byArray, int[] nArray, View view, boolean[] blArray) {
        int n;
        int n2;
        int n3;
        if (this.head.length < sectorGraph.sectors) {
            this.head = new int[sectorGraph.sectors];
            this.count = new int[sectorGraph.sectors];
        }
        Arrays.fill(this.head, 0, sectorGraph.sectors, -1);
        Arrays.fill(this.count, 0, sectorGraph.sectors, 0);
        this.rectCount = 0;
        int n4 = 0;
        int n5 = 0;
        for (int bl2 : nArray) {
            if (bl2 < 0 || sectorGraph.exterior[bl2]) continue;
            n4 = this.admit(bl2, -view.tanX, view.tanX, -view.tanY, view.tanY, n4);
        }
        for (n3 = 0; n3 < sectorGraph.openings; ++n3) {
            boolean f2;
            n2 = sectorGraph.openA[n3];
            n = sectorGraph.openB[n3];
            boolean f = sectorGraph.exterior[n2] && !sectorGraph.exterior[n];
            boolean bl = f2 = sectorGraph.exterior[n] && !sectorGraph.exterior[n2];
            if (!f && !f2 || !Portals.passable(sectorGraph, byArray, n3)) continue;
            n4 = this.through(sectorGraph, n3, f, view, -view.tanX, view.tanX, -view.tanY, view.tanY, n4);
        }
        for (n3 = 0; n3 < n4; ++n3) {
            n2 = this.queue[n3];
            n = this.rectSector[n2];
            float f = this.rects[n2 * 4];
            float f2 = this.rects[n2 * 4 + 1];
            float f3 = this.rects[n2 * 4 + 2];
            float f4 = this.rects[n2 * 4 + 3];
            for (int i = sectorGraph.adjacentStart[n]; i < sectorGraph.adjacentStart[n + 1]; ++i) {
                int n6;
                if (++n5 > 65536) {
                    return false;
                }
                int n7 = sectorGraph.adjacent[i];
                boolean bl = sectorGraph.openA[n7] == n;
                int n8 = n6 = bl ? sectorGraph.openB[n7] : sectorGraph.openA[n7];
                if (sectorGraph.exterior[n6] || !Portals.passable(sectorGraph, byArray, n7)) continue;
                n4 = this.through(sectorGraph, n7, bl, view, f, f2, f3, f4, n4);
            }
        }
        for (n3 = 0; n3 < sectorGraph.sectors; ++n3) {
            blArray[n3] = this.head[n3] >= 0;
        }
        return true;
    }

    private int through(SectorGraph sectorGraph, int n, boolean bl, View view, float f, float f2, float f3, float f4, int n2) {
        int n3;
        int n4 = n3 = bl ? sectorGraph.openB[n] : sectorGraph.openA[n];
        if (Portals.close(sectorGraph, n, view)) {
            return this.admit(n3, f, f2, f3, f4, n2);
        }
        if (Portals.onSide(sectorGraph, n, bl, view) && this.project(sectorGraph, n, view, f, f2, f3, f4)) {
            return this.admit(n3, this.projected[0], this.projected[1], this.projected[2], this.projected[3], n2);
        }
        return n2;
    }

    private static boolean passable(SectorGraph sectorGraph, byte[] byArray, int n) {
        return sectorGraph.openDoor[n] < 0 || byArray[sectorGraph.openDoor[n]] != 1;
    }

    static boolean onSide(SectorGraph sectorGraph, int n, boolean bl, View view) {
        float f;
        float[] fArray = sectorGraph.openPlace;
        int n2 = n * 5;
        switch (sectorGraph.openKind[n]) {
            case 0: {
                float f2 = view.y - fArray[n2 + 1];
                break;
            }
            case 1: {
                float f2 = view.x - fArray[n2];
                break;
            }
            default: {
                float f2 = f = fArray[n2 + 4] * 2.4494896f - view.h;
            }
        }
        return bl ? f >= -0.05f : f <= 0.05f;
    }

    private static boolean close(SectorGraph sectorGraph, int n, View view) {
        float[] fArray = sectorGraph.openPlace;
        int n2 = n * 5;
        float f = 2.4494896f;
        float f2 = fArray[n2 + 4] * f;
        float f3 = Portals.clamp(view.x, Math.min(fArray[n2], fArray[n2 + 2]), Math.max(fArray[n2], fArray[n2 + 2]));
        float f4 = Portals.clamp(view.y, Math.min(fArray[n2 + 1], fArray[n2 + 3]), Math.max(fArray[n2 + 1], fArray[n2 + 3]));
        float f5 = sectorGraph.openKind[n] == 2 ? f2 : Portals.clamp(view.h, f2, f2 + f);
        float f6 = view.x - f3;
        float f7 = view.y - f4;
        float f8 = view.h - f5;
        return f6 * f6 + f7 * f7 + f8 * f8 < 0.5625f;
    }

    boolean project(SectorGraph sectorGraph, int n, View view, float f, float f2, float f3, float f4) {
        this.outlineOf(sectorGraph, n);
        int n2 = this.clip(view);
        if (n2 == 0) {
            return false;
        }
        float f5 = Float.MAX_VALUE;
        float f6 = -3.4028235E38f;
        float f7 = Float.MAX_VALUE;
        float f8 = -3.4028235E38f;
        for (int i = 0; i < n2; ++i) {
            float f9 = this.clipped[i * 3] - view.x;
            float f10 = this.clipped[i * 3 + 1] - view.y;
            float f11 = this.clipped[i * 3 + 2] - view.h;
            float f12 = f9 * view.forward[0] + f10 * view.forward[1] + f11 * view.forward[2];
            float f13 = (f9 * view.right[0] + f10 * view.right[1] + f11 * view.right[2]) / f12;
            float f14 = (f9 * view.up[0] + f10 * view.up[1] + f11 * view.up[2]) / f12;
            f5 = Math.min(f5, f13);
            f6 = Math.max(f6, f13);
            f7 = Math.min(f7, f14);
            f8 = Math.max(f8, f14);
        }
        this.projected[0] = Math.max(f5, f);
        this.projected[1] = Math.min(f6, f2);
        this.projected[2] = Math.max(f7, f3);
        this.projected[3] = Math.min(f8, f4);
        return this.projected[0] < this.projected[1] && this.projected[2] < this.projected[3];
    }

    private void outlineOf(SectorGraph sectorGraph, int n) {
        float[] fArray = sectorGraph.openPlace;
        int n2 = n * 5;
        float f = 2.4494896f;
        float f2 = fArray[n2 + 4] * f;
        boolean bl = sectorGraph.openKind[n] == 2;
        float[] fArray2 = this.corners;
        fArray2[0] = fArray[n2];
        fArray2[1] = fArray[n2 + 1];
        fArray2[2] = f2;
        fArray2[3] = fArray[n2 + 2];
        fArray2[4] = bl ? fArray[n2 + 1] : fArray[n2 + 3];
        fArray2[5] = f2;
        fArray2[6] = fArray[n2 + 2];
        fArray2[7] = fArray[n2 + 3];
        fArray2[8] = bl ? f2 : f2 + f;
        fArray2[9] = fArray[n2];
        fArray2[10] = bl ? fArray[n2 + 3] : fArray[n2 + 1];
        fArray2[11] = bl ? f2 : f2 + f;
    }

    private int clip(View view) {
        int n = 0;
        for (int i = 0; i < 4; ++i) {
            int n2 = (i + 1) % 4;
            float f = this.ahead(view, i) - 0.05f;
            float f2 = this.ahead(view, n2) - 0.05f;
            if (f >= 0.0f) {
                System.arraycopy(this.corners, i * 3, this.clipped, n * 3, 3);
                ++n;
            }
            if (f >= 0.0f == f2 >= 0.0f) continue;
            float f3 = f / (f - f2);
            for (int j = 0; j < 3; ++j) {
                this.clipped[n * 3 + j] = this.corners[i * 3 + j] + (this.corners[n2 * 3 + j] - this.corners[i * 3 + j]) * f3;
            }
            ++n;
        }
        return n;
    }

    private float ahead(View view, int n) {
        return (this.corners[n * 3] - view.x) * view.forward[0] + (this.corners[n * 3 + 1] - view.y) * view.forward[1] + (this.corners[n * 3 + 2] - view.h) * view.forward[2];
    }

    private int admit(int n, float f, float f2, float f3, float f4, int n2) {
        int n3 = this.head[n];
        while (n3 >= 0) {
            if (this.rects[n3 * 4] <= f && this.rects[n3 * 4 + 1] >= f2 && this.rects[n3 * 4 + 2] <= f3 && this.rects[n3 * 4 + 3] >= f4) {
                return n2;
            }
            n3 = this.rectNext[n3];
        }
        if (this.count[n] >= 6) {
            n3 = this.head[n];
            f = Math.min(f, this.rects[n3 * 4]);
            f2 = Math.max(f2, this.rects[n3 * 4 + 1]);
            f3 = Math.min(f3, this.rects[n3 * 4 + 2]);
            f4 = Math.max(f4, this.rects[n3 * 4 + 3]);
        }
        n3 = this.rectCount++;
        this.grow();
        this.rects[n3 * 4] = f;
        this.rects[n3 * 4 + 1] = f2;
        this.rects[n3 * 4 + 2] = f3;
        this.rects[n3 * 4 + 3] = f4;
        this.rectSector[n3] = n;
        this.rectNext[n3] = this.head[n];
        this.head[n] = n3;
        int n4 = n;
        this.count[n4] = this.count[n4] + 1;
        if (n2 == this.queue.length) {
            this.queue = Arrays.copyOf(this.queue, n2 * 2);
        }
        this.queue[n2] = n3;
        return n2 + 1;
    }

    private void grow() {
        if (this.rectCount > this.rectSector.length) {
            this.rects = Arrays.copyOf(this.rects, this.rectSector.length * 8);
            this.rectSector = Arrays.copyOf(this.rectSector, this.rectSector.length * 2);
            this.rectNext = Arrays.copyOf(this.rectNext, this.rectNext.length * 2);
        }
    }

    private static float clamp(float f, float f2, float f3) {
        return f < f2 ? f2 : Math.min(f, f3);
    }

    static final class View {
        float x;
        float y;
        float h;
        final float[] forward = new float[3];
        final float[] right = new float[3];
        final float[] up = new float[3];
        float tanX;
        float tanY;

        View() {
        }

        void look(float f, float f2) {
            float f3 = (float)Math.cos(f2);
            this.forward[0] = (float)Math.cos(f) * f3;
            this.forward[1] = (float)Math.sin(f) * f3;
            this.forward[2] = (float)Math.sin(f2);
            float f4 = this.forward[1];
            float f5 = -this.forward[0];
            float f6 = (float)Math.sqrt(f4 * f4 + f5 * f5);
            this.right[0] = f6 > 1.0E-6f ? f4 / f6 : 1.0f;
            this.right[1] = f6 > 1.0E-6f ? f5 / f6 : 0.0f;
            this.right[2] = 0.0f;
            this.up[0] = this.right[1] * this.forward[2];
            this.up[1] = -this.right[0] * this.forward[2];
            this.up[2] = this.right[0] * this.forward[1] - this.right[1] * this.forward[0];
        }
    }
}

