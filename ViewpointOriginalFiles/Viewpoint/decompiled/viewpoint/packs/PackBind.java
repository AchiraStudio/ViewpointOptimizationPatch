/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.SpriteDetails.IsoObjectType
 *  zombie.iso.sprite.IsoSprite
 */
package viewpoint.packs;

import viewpoint.render.PackModel;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.sprite.IsoSprite;

public final class PackBind {
    public final PackModel model;
    public final float cos;
    public final float sin;
    public final float x;
    public final float y;
    public final float z;
    public final float scale;

    public PackBind(PackModel packModel, float f, float f2, float f3, float f4, float f5) {
        double d = Math.toRadians(f);
        this.model = packModel;
        this.cos = (float)Math.cos(d);
        this.sin = (float)Math.sin(d);
        this.x = f2;
        this.y = f3;
        this.z = f4;
        this.scale = f5;
    }

    public static boolean turns(IsoSprite isoSprite) {
        IsoObjectType isoObjectType = isoSprite.getType();
        return isoObjectType != IsoObjectType.stairsBN && isoObjectType != IsoObjectType.stairsMN && isoObjectType != IsoObjectType.stairsTN && isoObjectType != IsoObjectType.stairsBW && isoObjectType != IsoObjectType.stairsMW && isoObjectType != IsoObjectType.stairsTW;
    }

    public void place(float f, float f2, float f3, boolean bl, float[] fArray) {
        float f4 = f * this.scale;
        float f5 = f3 * this.scale;
        float f6 = bl ? this.cos : 1.0f;
        float f7 = bl ? this.sin : 0.0f;
        fArray[0] = f4 * f6 - f5 * f7;
        fArray[1] = f2 * this.scale;
        fArray[2] = f4 * f7 + f5 * f6;
    }
}

