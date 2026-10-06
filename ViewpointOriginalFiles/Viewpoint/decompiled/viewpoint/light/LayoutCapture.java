/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.LosUtil$TestResults
 *  zombie.iso.SpriteDetails.IsoObjectType
 *  zombie.iso.objects.IsoCurtain
 */
package viewpoint.light;

import java.util.List;
import viewpoint.light.LightLayout;
import viewpoint.light.LightLayouts;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.LosUtil;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.objects.IsoCurtain;

final class LayoutCapture {
    static LightLayouts.Level level(IsoChunk isoChunk, int n) {
        byte[] byArray = new byte[64];
        int[] nArray = new int[64];
        long[] lArray = new long[64];
        boolean bl = false;
        for (int i = 0; i < 64; ++i) {
            IsoGridSquare isoGridSquare = isoChunk.getGridSquare(i % 8, i / 8, n);
            lArray[i] = -1L;
            if (isoGridSquare == null) continue;
            byArray[i] = (byte)(LayoutCapture.flags(isoGridSquare) | 1);
            nArray[i] = LayoutCapture.passes(isoGridSquare);
            lArray[i] = isoGridSquare.getRoomID();
            bl = true;
        }
        return bl ? new LightLayouts.Level(isoChunk.wx, isoChunk.wy, n, byArray, nArray, lArray) : null;
    }

    static LightLayouts.Level facing(IsoCell isoCell, LightLayouts.Level level, int n, int n2) {
        int[] nArray = null;
        int n3 = level.chunkX * 8;
        int n4 = level.chunkY * 8;
        int n5 = n * 8;
        int n6 = n2 * 8;
        for (int i = 0; i < 64; ++i) {
            int n7;
            boolean bl;
            int n8 = n3 + i % 8;
            int n9 = n4 + i / 8;
            boolean bl2 = bl = n8 >= n5 - 1 && n8 <= n5 + 8 && n9 >= n6 - 1 && n9 <= n6 + 8;
            if (!bl || (level.flags[i] & 1) == 0) continue;
            IsoGridSquare isoGridSquare = isoCell.getGridSquare(n8, n9, level.level);
            int n10 = n7 = isoGridSquare == null ? level.passes[i] : LayoutCapture.passes(isoGridSquare);
            if (n7 == level.passes[i]) continue;
            nArray = nArray == null ? (int[])level.passes.clone() : nArray;
            nArray[i] = n7;
        }
        return nArray == null ? level : new LightLayouts.Level(level.chunkX, level.chunkY, level.level, level.flags, nArray, level.rooms);
    }

    static LightLayouts.Level edges(IsoCell isoCell, LightLayouts.Level level, LightLayouts.Level level2) {
        if (level == null || level2 == null) {
            return level;
        }
        int[] nArray = null;
        int n = level.chunkX * 8;
        int n2 = level.chunkY * 8;
        for (int i = 0; i < 64; ++i) {
            boolean bl;
            int n3 = i % 8;
            int n4 = i / 8;
            boolean bl2 = bl = n3 == 0 || n4 == 0 || n3 == 7 || n4 == 7;
            if (!bl || (level.flags[i] & 1) == 0 || (level2.flags[i] & 1) == 0) continue;
            for (int j = 0; j < 8; ++j) {
                int n5 = n3 + LightLayout.DX[j];
                int n6 = n4 + LightLayout.DY[j];
                boolean bl3 = n5 < 0 || n6 < 0 || n5 >= 8 || n6 >= 8;
                int n7 = nArray == null ? level.passes[i] : nArray[i];
                int n8 = 7 << j * 3;
                if (!bl3 || isoCell.getGridSquare(n + n5, n2 + n6, level.level) != null || (n7 & n8) == (level2.passes[i] & n8)) continue;
                nArray = nArray == null ? (int[])level.passes.clone() : nArray;
                nArray[i] = n7 & ~n8 | level2.passes[i] & n8;
            }
        }
        return nArray == null ? level : new LightLayouts.Level(level.chunkX, level.chunkY, level.level, level.flags, nArray, level.rooms);
    }

    private static int flags(IsoGridSquare isoGridSquare) {
        int n = (isoGridSquare.getOpenAir() ? 2 : 0) | (isoGridSquare.isOutside() && !isoGridSquare.haveRoof ? 64 : 0);
        List list = isoGridSquare.getSpecialObjects();
        for (int i = 0; i < list.size(); ++i) {
            IsoCurtain isoCurtain;
            Object e = list.get(i);
            if (!(e instanceof IsoCurtain) || (isoCurtain = (IsoCurtain)e).IsOpen()) continue;
            n |= LayoutCapture.curtain(isoCurtain.getType());
        }
        return n;
    }

    private static int curtain(IsoObjectType isoObjectType) {
        return isoObjectType == IsoObjectType.curtainN ? 4 : (isoObjectType == IsoObjectType.curtainE ? 8 : (isoObjectType == IsoObjectType.curtainS ? 16 : (isoObjectType == IsoObjectType.curtainW ? 32 : 0)));
    }

    static int passes(IsoGridSquare isoGridSquare) {
        int n = 0;
        for (int i = 0; i < 10; ++i) {
            LosUtil.TestResults testResults = isoGridSquare.testVisionAdjacent(LightLayout.DX[i], LightLayout.DY[i], LightLayout.DZ[i], true, false);
            int n2 = switch (testResults) {
                default -> throw new IncompatibleClassChangeError();
                case LosUtil.TestResults.Clear -> 0;
                case LosUtil.TestResults.ClearThroughOpenDoor -> 1;
                case LosUtil.TestResults.ClearThroughWindow -> 2;
                case LosUtil.TestResults.ClearThroughClosedDoor -> 3;
                case LosUtil.TestResults.Blocked -> 4;
            };
            n |= n2 << i * 3;
        }
        return n;
    }

    private LayoutCapture() {
    }
}

