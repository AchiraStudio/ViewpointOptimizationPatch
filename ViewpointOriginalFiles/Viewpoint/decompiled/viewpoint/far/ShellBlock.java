/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.util.Arrays;
import viewpoint.core.Frame;
import viewpoint.far.Dissolve;
import viewpoint.far.ShellInteriors;
import viewpoint.render.FarGpu;
import viewpoint.render.GroundPage;
import viewpoint.render.SceneData;

final class ShellBlock {
    final int bx;
    final int by;
    Piece piece;
    Piece previous;
    Piece coming;
    final Dissolve fade = new Dissolve();
    int generation;
    int want;
    int[] interiors = ShellInteriors.NONE;
    int[] wantInteriors = ShellInteriors.NONE;
    boolean busy;
    long seen;
    float distance;
    float top;

    ShellBlock(int n, int n2) {
        this.bx = n;
        this.by = n2;
    }

    int tier() {
        return this.piece == null ? -1 : this.piece.tier;
    }

    int builtTier() {
        return this.coming != null ? this.coming.tier : this.tier();
    }

    void emit(SceneData sceneData, Frame frame, float f) {
        int n;
        int n2 = this.bx - sceneData.shellMaskX;
        int n3 = this.by - sceneData.shellMaskY;
        int n4 = n = n2 >= 0 && n3 >= 0 && n2 < 53 && n3 < 53 ? n3 * 53 + n2 : -1;
        if (this.piece != null) {
            this.draw((SceneData)sceneData, (Piece)this.piece, (Frame)frame, (float)0.0f, (float)Dissolve.top((float)f)).maskIndex = n;
        }
        if (this.previous != null) {
            this.draw(sceneData, this.previous, frame, f, 2.0f);
        }
        if (n >= 0) {
            sceneData.shellMask[n * 3] = this.piece == null ? (byte)0 : this.piece.mask();
            sceneData.shellMask[n * 3 + 1] = Dissolve.share(f);
            sceneData.shellMask[n * 3 + 2] = this.previous == null ? (byte)0 : this.previous.mask();
        }
    }

    private FarGpu.BlockDraw draw(SceneData sceneData, Piece piece, Frame frame, float f, float f2) {
        FarGpu.BlockDraw blockDraw;
        if (sceneData.shellCount == sceneData.shells.length) {
            sceneData.shells = Arrays.copyOf(sceneData.shells, sceneData.shells.length * 2);
        }
        if ((blockDraw = sceneData.shells[sceneData.shellCount]) == null) {
            blockDraw = sceneData.shells[sceneData.shellCount] = new FarGpu.BlockDraw();
        }
        ++sceneData.shellCount;
        blockDraw.shell = piece.shell;
        blockDraw.inside = piece.inside;
        blockDraw.ground = piece.ground;
        blockDraw.lite = piece.tier == 1;
        blockDraw.distance = this.distance;
        blockDraw.maskIndex = -1;
        blockDraw.fadeLo = f;
        blockDraw.fadeHi = f2;
        int n = 4;
        blockDraw.worldX = Math.floorDiv(this.bx, n) * 256;
        blockDraw.worldY = Math.floorDiv(this.by, n) * 256;
        blockDraw.blockX = this.bx * 64 - blockDraw.worldX;
        blockDraw.blockY = this.by * 64 - blockDraw.worldY;
        blockDraw.originX = (float)((double)frame.camX - (double)blockDraw.worldX);
        blockDraw.originY = -frame.camZ * 2.4494896f;
        blockDraw.originZ = (float)((double)frame.camY - (double)blockDraw.worldY);
        blockDraw.top = this.top;
        return blockDraw;
    }

    static final class Piece {
        final FarGpu.Shell shell;
        final FarGpu.Mesh inside;
        final GroundPage ground;
        final int tier;

        Piece(FarGpu.Shell shell, FarGpu.Mesh mesh, GroundPage groundPage, int n) {
            this.shell = shell;
            this.inside = mesh;
            this.ground = groundPage;
            this.tier = n;
        }

        long bytes() {
            return (this.shell == null ? 0L : this.shell.bytes()) + (this.inside == null ? 0L : this.inside.bytes()) + (this.ground == null ? 0L : this.ground.bytes());
        }

        boolean uploaded() {
            return !(this.shell != null && !this.shell.uploaded() || this.inside != null && !this.inside.uploaded());
        }

        int vertices() {
            return this.shell == null ? 0 : this.shell.vertices;
        }

        byte mask() {
            return (byte)(this.tier == 0 ? 2 : (this.ground != null ? 3 : 1));
        }

        void retire() {
            FarGpu.retire(this.shell);
            FarGpu.retire(this.inside);
        }
    }
}

