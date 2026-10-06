/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.properties.IsoPropertyType
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.SpriteDetails.IsoObjectType
 *  zombie.iso.sprite.IsoSprite
 *  zombie.iso.sprite.IsoSpriteManager
 */
package viewpoint.light;

import java.util.HashMap;
import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;

public final class TileLight {
    static final int HAS = 1;
    static final int TRANS = 2;
    static final int SOLID_FLOOR = 4;
    static final int TRANSPARENT_FLOOR = 8;
    static final int NO_START = 16;
    static final int COLLIDE_N = 32;
    static final int COLLIDE_W = 64;
    static final int TRANSPARENT_N = 128;
    static final int TRANSPARENT_W = 256;
    static final int DOOR_FLAG_N = 512;
    static final int DOOR_FLAG_W = 1024;
    static final int SOLID = 2048;
    static final int BLOCKSIGHT = 4096;
    static final int BLOCK_RAIN = 8192;
    static final int OPAQUE_FLOOR = 16384;
    static final int OPAQUE_CUT_N = 32768;
    static final int OPAQUE_CUT_W = 65536;
    static final int DOOR_N = 131072;
    static final int DOOR_N_SEE = 262144;
    static final int DOOR_N_OPEN = 524288;
    static final int DOOR_W = 0x100000;
    static final int DOOR_W_SEE = 0x200000;
    static final int DOOR_W_OPEN = 0x400000;
    static final int WINDOW_N = 0x800000;
    static final int WINDOW_W = 0x1000000;
    static final int EXISTS = 0x20000000;
    static final int ROOM_LOOKUP = 0x40000000;
    public static final int LAMP_RADIUS_SHIFT = 24;
    public static final int STREET = 0x40000000;
    static final int MAX_PACKED_RADIUS = 63;
    private static final int DEFAULT_RADIUS = 10;
    private static final HashMap<String, Long> byName = new HashMap();

    public static long of(String string) {
        long l;
        Long l2 = byName.get(string);
        if (l2 != null) {
            return l2;
        }
        IsoSprite isoSprite = string == null ? null : (IsoSprite)IsoSpriteManager.instance.getNamedMap().get(string);
        try {
            l = isoSprite == null ? 0L : (long)TileLight.lamp(isoSprite) << 32 | (long)TileLight.square(isoSprite) & 0xFFFFFFFFL;
        }
        catch (RuntimeException runtimeException) {
            l = isoSprite == null ? 0L : 1L;
        }
        byName.put(string, l);
        return l;
    }

    public static void forget() {
        byName.clear();
    }

    private static int square(IsoSprite isoSprite) {
        PropertyContainer propertyContainer = isoSprite.getProperties();
        if (propertyContainer.has(IsoFlagType.blueprint) || propertyContainer.has(IsoFlagType.FloorOverlay) || propertyContainer.has(IsoFlagType.WallOverlay) || propertyContainer.has(IsoPropertyType.COUNTERTOP)) {
            return 1;
        }
        int n = 1 | TileLight.flag(propertyContainer, IsoFlagType.trans, 2) | TileLight.flag(propertyContainer, IsoFlagType.solidfloor, 4) | TileLight.flag(propertyContainer, IsoFlagType.transparentFloor, 8) | TileLight.flag(propertyContainer, IsoFlagType.noStart, 16) | TileLight.flag(propertyContainer, IsoFlagType.collideN, 32) | TileLight.flag(propertyContainer, IsoFlagType.collideW, 64) | TileLight.flag(propertyContainer, IsoFlagType.transparentN, 128) | TileLight.flag(propertyContainer, IsoFlagType.transparentW, 256) | TileLight.flag(propertyContainer, IsoFlagType.doorN, 512) | TileLight.flag(propertyContainer, IsoFlagType.doorW, 1024) | TileLight.flag(propertyContainer, IsoFlagType.solid, 2048) | TileLight.flag(propertyContainer, IsoFlagType.blocksight, 4096) | TileLight.flag(propertyContainer, IsoFlagType.BlockRain, 8192);
        if (!propertyContainer.has(IsoFlagType.water) && propertyContainer.has(IsoFlagType.solidfloor) && !propertyContainer.has(IsoFlagType.transparentFloor)) {
            n |= 0x4000;
        }
        if (propertyContainer.has(IsoFlagType.cutN) && !propertyContainer.has(IsoFlagType.transparentN) && !propertyContainer.has(IsoFlagType.WallSE)) {
            n |= 0x8000;
        }
        if (propertyContainer.has(IsoFlagType.cutW) && !propertyContainer.has(IsoFlagType.transparentW) && !propertyContainer.has(IsoFlagType.WallSE)) {
            n |= 0x10000;
        }
        return n | TileLight.special(isoSprite, propertyContainer);
    }

    private static int special(IsoSprite isoSprite, PropertyContainer propertyContainer) {
        IsoObjectType isoObjectType = isoSprite.getTileType();
        if (isoObjectType == IsoObjectType.doorN || isoObjectType == IsoObjectType.doorW) {
            boolean bl;
            boolean bl2 = propertyContainer.has(IsoFlagType.open);
            boolean bl3 = bl2 || propertyContainer.has(IsoPropertyType.DOOR_TRANS);
            boolean bl4 = bl = isoObjectType == IsoObjectType.doorN != bl2;
            return bl ? 0x20000 | (bl3 ? 262144 : 0) | (bl2 ? 524288 : 0) : 0x100000 | (bl3 ? 0x200000 : 0) | (bl2 ? 0x400000 : 0);
        }
        if (isoObjectType == IsoObjectType.lightswitch) {
            return 0;
        }
        if (isoObjectType == IsoObjectType.curtainN || isoObjectType == IsoObjectType.curtainS || isoObjectType == IsoObjectType.curtainE || isoObjectType == IsoObjectType.curtainW) {
            return 0;
        }
        if (propertyContainer.has(IsoFlagType.windowN)) {
            return 0x800000;
        }
        return propertyContainer.has(IsoFlagType.windowW) ? 0x1000000 : 0;
    }

    private static int lamp(IsoSprite isoSprite) {
        PropertyContainer propertyContainer = isoSprite.getProperties();
        if (isoSprite.getTileType() != IsoObjectType.lightswitch || !propertyContainer.has(IsoPropertyType.RED_LIGHT)) {
            return 0;
        }
        int n = 10;
        if (propertyContainer.has(IsoPropertyType.LIGHT_RADIUS) && Integer.parseInt(propertyContainer.get(IsoPropertyType.LIGHT_RADIUS)) > 0) {
            n = Integer.parseInt(propertyContainer.get(IsoPropertyType.LIGHT_RADIUS));
        }
        return TileLight.channel(propertyContainer.get(IsoPropertyType.RED_LIGHT)) | TileLight.channel(propertyContainer.get(IsoPropertyType.GREEN_LIGHT)) << 8 | TileLight.channel(propertyContainer.get(IsoPropertyType.BLUE_LIGHT)) << 16 | Math.min(n, 63) << 24 | (propertyContainer.has("streetlight") ? 0x40000000 : 0);
    }

    private static int channel(String string) {
        return string == null ? 0 : Math.max(0, Math.min(255, Math.round(Float.parseFloat(string))));
    }

    private static int flag(PropertyContainer propertyContainer, IsoFlagType isoFlagType, int n) {
        return propertyContainer.has(isoFlagType) ? n : 0;
    }

    private TileLight() {
    }
}

