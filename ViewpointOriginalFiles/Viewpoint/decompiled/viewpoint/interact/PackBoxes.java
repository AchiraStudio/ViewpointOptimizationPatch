/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.sprite.IsoSprite
 */
package viewpoint.interact;

import viewpoint.packs.ModelPacks;
import viewpoint.packs.PackBind;
import viewpoint.world.ChunkCache;
import viewpoint.world.WorldMesher;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;

final class PackBoxes {
    private static final float[] placed = new float[3];

    static boolean of(IsoObject isoObject, IsoGridSquare isoGridSquare, float[] fArray) {
        float[] fArray2;
        IsoSprite isoSprite = isoObject.getSprite();
        PackBind packBind = isoSprite == null || !ChunkCache.packsOn(isoGridSquare) ? null : ModelPacks.bind(isoSprite);
        float[] fArray3 = fArray2 = packBind == null || packBind.model.state() != 3 ? null : packBind.model.bounds();
        if (fArray2 == null) {
            return false;
        }
        boolean bl = PackBind.turns(isoSprite);
        float f = (float)isoGridSquare.x + 0.5f + packBind.x;
        float f2 = (float)isoGridSquare.y + 0.5f + packBind.y;
        float f3 = (float)isoGridSquare.z * 2.4494896f + WorldMesher.rise(isoObject) + packBind.z;
        fArray[2] = Float.MAX_VALUE;
        fArray[1] = Float.MAX_VALUE;
        fArray[0] = Float.MAX_VALUE;
        fArray[5] = -3.4028235E38f;
        fArray[4] = -3.4028235E38f;
        fArray[3] = -3.4028235E38f;
        for (int i = 0; i < 8; ++i) {
            packBind.place(fArray2[(i & 1) * 3], fArray2[1 + (i >> 1 & 1) * 3], fArray2[2 + (i >> 2) * 3], bl, placed);
            float[] fArray4 = new float[]{f - placed[0], f2 - placed[2], f3 + placed[1]};
            for (int j = 0; j < 3; ++j) {
                fArray[j] = Math.min(fArray[j], fArray4[j]);
                fArray[j + 3] = Math.max(fArray[j + 3], fArray4[j]);
            }
        }
        return true;
    }

    private PackBoxes() {
    }
}

