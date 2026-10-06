/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 *  zombie.core.math.PZMath
 *  zombie.core.textures.ColorInfo
 *  zombie.core.textures.Texture
 *  zombie.debug.DebugOptions
 *  zombie.iso.IsoDirections
 *  zombie.iso.IsoFloorBloodSplat
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.sprite.IsoSprite
 *  zombie.iso.sprite.IsoSpriteInstance
 *  zombie.iso.sprite.IsoSpriteManager
 */
package viewpoint.world;

import java.util.ArrayList;
import viewpoint.platform.LiveSettings;
import viewpoint.render.FloorArt;
import zombie.GameTime;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoFloorBloodSplat;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;

final class FloorGather {
    static final LiveSettings.Toggle BAKED = LiveSettings.toggle("floors.baked", "Floors baked", "World/Floors", true);
    private static final float ISO_HALF_WIDTH = 32.0f;
    private static final float ISO_HALF_HEIGHT = 16.0f;
    private static final float ISO_LEVEL_HEIGHT = 96.0f;
    private static final float BLOOD_ALPHA = 0.27f;
    private static final float BLOOD_AGED = 0.2f;
    private static final float BLOOD_AGED_ALPHA = 0.25f;
    private static final float BLOOD_HOURS = 72.0f;
    private static final float VARY_DARK = 42367.543f;
    private static final float VARY_RED = 6367.123f;
    private static final float VARY_BLUE = 23367.133f;
    private static final float VARY_RANGE = 1000.0f;
    private static final float MOST_DARK = 0.25f;
    private static final int THINNED = 10;
    private static final float[] values = new float[14];
    private static final FloorArt.Builder art = new FloorArt.Builder();
    private static final ArrayList<ArrayList<IsoFloorBloodSplat>> bloodBySquare = new ArrayList();

    static boolean capture(IsoGridSquare isoGridSquare, int n, IsoObject isoObject, boolean[] blArray) {
        if (!FloorGather.isFloor(isoObject)) {
            return false;
        }
        art.square(n);
        FloorGather.object(art, isoGridSquare, isoObject, blArray);
        return true;
    }

    static FloorArt build() {
        FloorArt floorArt = art.build();
        art.begin();
        return floorArt;
    }

    static void bloodOf(IsoChunk isoChunk, int n) {
        for (ArrayList<IsoFloorBloodSplat> arrayList : bloodBySquare) {
            arrayList.clear();
        }
        int n2 = Core.getInstance().getOptionBloodDecals();
        if (n2 == 0 || !DebugOptions.instance.terrain.renderTiles.bloodDecals.getValue()) {
            return;
        }
        for (int i = 0; i < isoChunk.floorBloodSplats.size(); ++i) {
            IsoFloorBloodSplat isoFloorBloodSplat = (IsoFloorBloodSplat)isoChunk.floorBloodSplats.get(i);
            int n3 = PZMath.fastfloor((float)isoFloorBloodSplat.x);
            int n4 = PZMath.fastfloor((float)isoFloorBloodSplat.y);
            if (isoFloorBloodSplat.index >= 1 && isoFloorBloodSplat.index <= 10 && IsoChunk.renderByIndex[n2 - 1][isoFloorBloodSplat.index - 1] == 0 || PZMath.fastfloor((float)isoFloorBloodSplat.z) != n || isoFloorBloodSplat.type < 0 || isoFloorBloodSplat.type >= IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length || n3 < 0 || n3 >= 8 || n4 < 0 || n4 >= 8) continue;
            bloodBySquare.get(n4 * 8 + n3).add(isoFloorBloodSplat);
        }
    }

    static void blood(int n, boolean[] blArray) {
        ArrayList<IsoFloorBloodSplat> arrayList = bloodBySquare.get(n);
        if (arrayList.isEmpty()) {
            return;
        }
        art.square(n);
        float f = (float)GameTime.getInstance().getWorldAgeHours();
        for (int i = 0; i < arrayList.size(); ++i) {
            IsoFloorBloodSplat isoFloorBloodSplat = arrayList.get(i);
            Texture texture = FloorGather.bloodSprite(IsoFloorBloodSplat.FLOOR_BLOOD_TYPES[isoFloorBloodSplat.type]).getTextureForCurrentFrame(IsoDirections.N);
            if (texture == null) continue;
            if (!texture.isReady() || texture.getTextureId() == null) {
                blArray[0] = true;
                continue;
            }
            float f2 = isoFloorBloodSplat.x - (float)PZMath.fastfloor((float)isoFloorBloodSplat.x);
            float f3 = isoFloorBloodSplat.y - (float)PZMath.fastfloor((float)isoFloorBloodSplat.y);
            float f4 = isoFloorBloodSplat.z - (float)PZMath.fastfloor((float)isoFloorBloodSplat.z);
            float f5 = Core.tileScale;
            float f6 = (f2 - f3) * 32.0f * f5 - (float)texture.getWidth() * 0.5f * f5 + texture.getOffsetX();
            float f7 = (f2 + f3) * 16.0f * f5 - f4 * 96.0f * f5 - (float)texture.getHeight() * 0.5f * f5 + texture.getOffsetY();
            FloorGather.bloodTint(isoFloorBloodSplat, f);
            FloorArt.numbers(values, texture, f6, f7, 1.0f, 1.0f, values[10], values[11], values[12], values[13]);
            art.layer(texture.getTextureId(), values, 0);
        }
    }

    private static IsoSprite bloodSprite(String string) {
        IsoSprite isoSprite = (IsoSprite)IsoFloorBloodSplat.spriteMap.get(string);
        if (isoSprite == null) {
            isoSprite = IsoSprite.CreateSprite((IsoSpriteManager)IsoSpriteManager.instance);
            isoSprite.LoadFramesPageSimple(string, string, string, string);
            IsoFloorBloodSplat.spriteMap.put(string, isoSprite);
        }
        return isoSprite;
    }

    private static void bloodTint(IsoFloorBloodSplat isoFloorBloodSplat, float f) {
        float f2;
        float f3 = (isoFloorBloodSplat.x + isoFloorBloodSplat.y / isoFloorBloodSplat.x) * (float)(isoFloorBloodSplat.type + 1);
        float f4 = f3 * isoFloorBloodSplat.x / isoFloorBloodSplat.y * (float)(isoFloorBloodSplat.type + 1) / (f3 + isoFloorBloodSplat.y);
        float f5 = f4 * f3 * f4 * isoFloorBloodSplat.x / (isoFloorBloodSplat.y + 2.0f);
        f3 = Math.min(0.25f, f3 * 42367.543f % 1000.0f / 1000.0f);
        f4 = f4 * 6367.123f % 1000.0f / 1000.0f;
        f5 = f5 * 23367.133f % 1000.0f / 1000.0f;
        float f6 = 1.0f - f3 * 2.0f + f4 / 3.0f;
        float f7 = f2 = 1.0f - f3 * 2.0f - f5 / 3.0f;
        float f8 = f - isoFloorBloodSplat.worldAge;
        float f9 = f8 >= 0.0f && f8 < 72.0f ? 1.0f - f8 / 72.0f : 0.0f;
        float f10 = 0.2f + f9 * 0.8f;
        FloorGather.values[10] = f6 * f10;
        FloorGather.values[11] = f2 * f10;
        FloorGather.values[12] = f7 * f10;
        FloorGather.values[13] = 0.27f * (0.25f + f9 * 0.75f);
    }

    static boolean isFloor(IsoObject isoObject) {
        if (isoObject == null || !isoObject.getDoRender()) {
            return false;
        }
        IsoSprite isoSprite = isoObject.getSprite();
        return isoSprite != null && (isoSprite.solidfloor || isoSprite.renderLayer == 1) && !isoSprite.getProperties().has(IsoFlagType.invisible) && isoSprite.getProperties().getSlopedSurfaceDirection() == null && !isoSprite.hasActiveModel();
    }

    static void object(FloorArt.Builder builder, IsoGridSquare isoGridSquare, IsoObject isoObject, boolean[] blArray) {
        ColorInfo colorInfo = isoObject.getCustomColor();
        boolean bl = isoObject == isoGridSquare.getFloor() && !isoObject.hasProperty(IsoFlagType.water);
        FloorGather.layer(builder, isoObject, isoObject.getSprite(), null, colorInfo, bl, blArray);
        FloorGather.layer(builder, isoObject, isoObject.getOverlaySprite(), null, isoObject.getOverlaySpriteColor(), false, blArray);
        ArrayList arrayList = isoObject.getAttachedAnimSprite();
        if (arrayList == null) {
            return;
        }
        for (int i = 0; i < arrayList.size(); ++i) {
            IsoSpriteInstance isoSpriteInstance = (IsoSpriteInstance)arrayList.get(i);
            if (isoSpriteInstance == null) continue;
            FloorGather.layer(builder, isoObject, isoSpriteInstance.parentSprite, isoSpriteInstance, colorInfo, false, blArray);
        }
    }

    private static void layer(FloorArt.Builder builder, IsoObject isoObject, IsoSprite isoSprite, IsoSpriteInstance isoSpriteInstance, ColorInfo colorInfo, boolean bl, boolean[] blArray) {
        float f;
        if (isoSprite == null) {
            return;
        }
        IsoSpriteInstance isoSpriteInstance2 = isoSpriteInstance == null ? isoSprite.def : isoSpriteInstance;
        int n = isoSpriteInstance2 == null ? 0 : (int)isoSpriteInstance2.frame;
        Texture texture = isoSprite.getTextureForFrame(n, isoObject.getForwardIsoDirection(), isoObject.isUseSnowSprite());
        float f2 = isoSpriteInstance2 == null ? 1.0f : isoSpriteInstance2.scaleX;
        float f3 = f = isoSpriteInstance2 == null ? 1.0f : isoSpriteInstance2.scaleY;
        if (texture == null || f2 <= 0.0f || f <= 0.0f) {
            return;
        }
        if (!texture.isReady() || texture.getTextureId() == null) {
            blArray[0] = true;
            return;
        }
        float f4 = 32.0f * (float)Core.tileScale;
        float f5 = 16.0f * (float)Core.tileScale;
        float f6 = isoSpriteInstance2 == null ? 0.0f : isoSpriteInstance2.offX;
        float f7 = isoSpriteInstance2 == null ? 0.0f : isoSpriteInstance2.offY;
        float f8 = isoSpriteInstance2 == null ? 0.0f : isoSpriteInstance2.offZ;
        float f9 = -isoObject.offsetX + (float)isoSprite.soffX + texture.getOffsetX() * f2 + (f6 - f7) * f4;
        float f10 = -isoObject.offsetY - isoObject.getRenderYOffset() * (float)Core.tileScale + (float)isoSprite.soffY + texture.getOffsetY() * f + (f6 + f7) * f5 - f8 * 96.0f * (float)Core.tileScale;
        FloorGather.tint(colorInfo, isoSprite, isoSpriteInstance2);
        FloorArt.numbers(values, texture, f9, f10, f2, f, values[10], values[11], values[12], values[13]);
        int n2 = (isoSpriteInstance2 != null && isoSpriteInstance2.flip ? 1 : 0) | (bl ? 2 : 0);
        builder.layer(texture.getTextureId(), values, n2);
    }

    private static void tint(ColorInfo colorInfo, IsoSprite isoSprite, IsoSpriteInstance isoSpriteInstance) {
        float f;
        float f2 = (colorInfo == null ? 1.0f : colorInfo.r) * isoSprite.tintMod.r;
        float f3 = (colorInfo == null ? 1.0f : colorInfo.g) * isoSprite.tintMod.g;
        float f4 = (colorInfo == null ? 1.0f : colorInfo.b) * isoSprite.tintMod.b;
        float f5 = f = colorInfo == null ? 1.0f : colorInfo.a;
        if (isoSpriteInstance != null) {
            f2 *= isoSpriteInstance.tintr;
            f3 *= isoSpriteInstance.tintg;
            f4 *= isoSpriteInstance.tintb;
            if (!isoSpriteInstance.copyTargetAlpha) {
                f *= isoSpriteInstance.alpha;
            }
        }
        FloorGather.values[10] = f2;
        FloorGather.values[11] = f3;
        FloorGather.values[12] = f4;
        FloorGather.values[13] = f;
    }

    private FloorGather() {
    }

    static {
        for (int i = 0; i < 64; ++i) {
            bloodBySquare.add(new ArrayList());
        }
    }
}

