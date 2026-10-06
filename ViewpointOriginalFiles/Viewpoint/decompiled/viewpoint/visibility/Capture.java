/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.SpriteModel
 *  zombie.iso.objects.IsoWindow
 *  zombie.scripting.ScriptManager
 */
package viewpoint.visibility;

import java.util.Arrays;
import viewpoint.visibility.Edges;
import viewpoint.visibility.Fragment;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteModel;
import zombie.iso.objects.IsoWindow;
import zombie.scripting.ScriptManager;

final class Capture {
    static Fragment capture(IsoChunk isoChunk, int n, IsoObject[] isoObjectArray, long[] lArray) {
        long[] lArray2 = new long[12];
        long[] lArray3 = new long[64];
        Arrays.fill(isoObjectArray, null);
        lArray[1] = 0L;
        lArray[0] = 0L;
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                int n2 = i * 8 + j;
                IsoGridSquare isoGridSquare = isoChunk.getGridSquare(j, i, n);
                lArray3[n2] = -1L;
                if (isoGridSquare == null) continue;
                Capture.square(isoGridSquare, n2, lArray2, lArray3, isoObjectArray, lArray);
            }
        }
        return new Fragment(isoChunk.wx, isoChunk.wy, n, n <= isoChunk.minLevel, lArray2, lArray3);
    }

    private static void square(IsoGridSquare isoGridSquare, int n, long[] lArray, long[] lArray2, IsoObject[] isoObjectArray, long[] lArray3) {
        long l = 1L << n;
        lArray[0] = lArray[0] | l;
        if (isoGridSquare.isSolidFloor() || isoGridSquare.TreatAsSolidFloor()) {
            lArray[1] = lArray[1] | l;
        }
        if (isoGridSquare.HasStairs() || isoGridSquare.HasStairsBelow()) {
            lArray[2] = lArray[2] | l;
        }
        if (isoGridSquare.haveRoof) {
            lArray[3] = lArray[3] | l;
        }
        if (isoGridSquare.getRoom() != null && !isoGridSquare.isOutside()) {
            lArray2[n] = isoGridSquare.getRoomID();
        }
        Capture.edge(isoGridSquare, n, true, lArray, isoObjectArray, lArray3);
        Capture.edge(isoGridSquare, n, false, lArray, isoObjectArray, lArray3);
    }

    private static void edge(IsoGridSquare isoGridSquare, int n, boolean bl, long[] lArray, IsoObject[] isoObjectArray, long[] lArray2) {
        long l = 1L << n;
        switch (Edges.edge(isoGridSquare, bl)) {
            case 3: {
                int n2 = bl ? 8 : 9;
                lArray[n2] = lArray[n2] | l;
                isoObjectArray[n * 2 + (bl ? 0 : 1)] = isoGridSquare.getDoor(Edges.facing(bl));
                break;
            }
            case 4: {
                int n3 = bl ? 10 : 11;
                lArray[n3] = lArray[n3] | l;
                int n4 = bl ? 0 : 1;
                lArray2[n4] = lArray2[n4] | (Capture.glazed(isoGridSquare.getWindow(Edges.facing(bl))) ? l : 0L);
                break;
            }
            case 2: {
                int n5 = bl ? 6 : 7;
                lArray[n5] = lArray[n5] | l;
                break;
            }
            case 1: {
                int n6 = bl ? 4 : 5;
                lArray[n6] = lArray[n6] | l;
                break;
            }
        }
    }

    private static boolean glazed(IsoWindow isoWindow) {
        return isoWindow != null && !isoWindow.IsOpen() && !isoWindow.isSmashed() && !isoWindow.isGlassRemoved() && !isoWindow.isDestroyed() && !Capture.drawnAsModel((IsoObject)isoWindow);
    }

    static boolean drawnAsModel(IsoObject isoObject) {
        SpriteModel spriteModel = isoObject.getSpriteModel();
        return spriteModel != null && spriteModel.modelScriptName != null && ScriptManager.instance.getModelScript(spriteModel.modelScriptName) != null;
    }

    private Capture() {
    }
}

