/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 *  zombie.core.textures.Texture
 *  zombie.iso.IsoDirections
 *  zombie.iso.LotHeader
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.SpriteDetails.IsoObjectType
 *  zombie.iso.sprite.IsoSprite
 *  zombie.iso.sprite.IsoSpriteManager
 */
package viewpoint.far;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import viewpoint.far.BiomeTrees;
import viewpoint.far.FarColours;
import viewpoint.far.FarTrees;
import viewpoint.far.TreeSeasons;
import viewpoint.render.FloorArt;
import viewpoint.render.GroundTiles;
import viewpoint.render.TreeAtlas;
import viewpoint.world.FacadeColours;
import viewpoint.world.TileMesh;
import viewpoint.world.TileMeshes;
import viewpoint.world.WorldMesher;
import zombie.core.Core;
import zombie.core.properties.PropertyContainer;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.iso.IsoDirections;
import zombie.iso.LotHeader;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;

final class FarTiles {
    static final byte NONE = 0;
    static final byte FLOOR = 1;
    static final byte WALL = 2;
    static final byte TREE = 3;
    static final byte SOLID = 4;
    static final byte S_NONE = 0;
    static final byte S_WALL = 1;
    static final byte S_FLOOR = 2;
    static final byte S_THING = 3;
    static final byte S_TREE = 4;
    static final byte S_PLANT = 5;
    static final byte ROOF = 1;
    static final byte SOLID_FLOOR = 2;
    static final byte WATER = 4;
    static final byte TREE_SPOT = 8;
    static final byte PLACEHOLDER = 16;
    static final byte LEAVES = 32;
    private static final float OBJECT_OFFSET_X = 32.0f;
    private static final float OBJECT_OFFSET_Y = 96.0f;
    static final float LOW = 0.5f;
    static final int CARD_LEFT = 0;
    static final int CARD_RIGHT = 1;
    static final int CARD_BOTTOM = 2;
    static final int CARD_TOP = 3;
    static final Tile EMPTY = new Tile(0, 0.0f, -1);
    static final Tile SPOT = new Tile(0, 0.0f, -1, 0, null, null, null, 0, 0, 8, null);
    static final Tile PLACEHOLDER_SPOT = new Tile(0, 0.0f, -1, 0, null, null, null, 0, 0, 24, null);
    private static final float SHELL_LOW = 0.2f;
    private static final float THIN = 0.35f;
    private static final HashMap<String, Tile> byName = new HashMap();
    private static final IdentityHashMap<LotHeader, Tile[]> byHeader = new IdentityHashMap();
    private static final IdentityHashMap<LotHeader, Integer> filled = new IdentityHashMap();

    static Tile[] table(LotHeader lotHeader, long l) {
        int n;
        Tile[] tileArray = byHeader.get(lotHeader);
        Integer n2 = filled.get(lotHeader);
        if (tileArray != null && n2 == null) {
            return tileArray;
        }
        ArrayList arrayList = lotHeader.tilesUsed;
        if (tileArray == null) {
            tileArray = new Tile[arrayList.size()];
            byHeader.put(lotHeader, tileArray);
        }
        int n3 = n = n2 == null ? 0 : n2;
        while (n < tileArray.length) {
            tileArray[n] = FarTiles.of((String)arrayList.get(n));
            if ((++n & 0x1F) != 0 || n >= tileArray.length || System.nanoTime() <= l) continue;
            filled.put(lotHeader, n);
            return null;
        }
        filled.remove(lotHeader);
        return tileArray;
    }

    static Tile of(String string) {
        Tile tile = byName.get(string);
        if (tile != null) {
            return tile;
        }
        try {
            boolean bl = string != null && (string.startsWith("vegetation_trees") || string.startsWith("jumbo_tree_01"));
            tile = bl ? PLACEHOLDER_SPOT : FarTiles.classify(string);
        }
        catch (RuntimeException runtimeException) {
            tile = EMPTY;
        }
        byName.put(string, tile);
        return tile;
    }

    static Map<String, Tile> byName() {
        return Collections.unmodifiableMap(byName);
    }

    static void forget() {
        byHeader.clear();
        filled.clear();
        TreeSeasons.forget();
        BiomeTrees.forget();
    }

    private static Tile classify(String string) {
        int n;
        int n2;
        TileMesh tileMesh;
        float[] fArray;
        IsoSprite isoSprite;
        IsoSprite isoSprite2 = isoSprite = string == null ? null : (IsoSprite)IsoSpriteManager.instance.getNamedMap().get(string);
        if (isoSprite == null) {
            return EMPTY;
        }
        boolean bl = isoSprite.getType() == IsoObjectType.tree;
        Texture texture = isoSprite.getTextureForCurrentFrame(IsoDirections.N);
        boolean bl2 = texture != null && texture.isReady() && texture.getTextureId() != null;
        TextureID textureID = bl2 ? texture.getTextureId() : null;
        float[] fArray2 = fArray = bl2 ? WorldMesher.textureMapping(texture) : null;
        TileMesh tileMesh2 = texture == null ? null : (tileMesh = TileMeshes.peek(isoSprite, texture, (IsoSprite)(bl ? isoSprite : null)));
        if (isoSprite.solidfloor || isoSprite.getProperties().has(IsoFlagType.FloorOverlay)) {
            Tile tile = new Tile(1, 0.0f, FarColours.of(string), 2, tileMesh == null || tileMesh.vertCount == 0 ? null : textureID, fArray, tileMesh, 0, 0, (byte)((isoSprite.solidfloor ? 2 : 0) | (isoSprite.getProperties().has(IsoFlagType.water) ? 4 : 0)), null);
            tile.floor = bl2 ? FarTiles.floorLayer(isoSprite, texture) : null;
            tile.floorName = string;
            return tile;
        }
        PropertyContainer propertyContainer = isoSprite.getProperties();
        boolean bl3 = propertyContainer.has(IsoFlagType.WallNW);
        int n3 = bl3 || propertyContainer.has(IsoFlagType.WallN) ? 0 : (propertyContainer.has(IsoFlagType.WindowN) ? 1 : (n2 = propertyContainer.has(IsoFlagType.DoorWallN) ? 2 : -1));
        int n4 = bl3 || propertyContainer.has(IsoFlagType.WallW) ? 0 : (propertyContainer.has(IsoFlagType.WindowW) ? 1 : (n = propertyContainer.has(IsoFlagType.DoorWallW) ? 2 : -1));
        if (!(bl || n2 < 0 && n < 0)) {
            int n5 = FacadeColours.of(string);
            return new Tile(2, 1.0f, n5 >= 0 ? n5 : 9210501, 1, tileMesh == null || tileMesh.vertCount == 0 ? null : textureID, fArray, tileMesh, (byte)n2, (byte)n, 0, null);
        }
        if (texture == null) {
            return bl ? SPOT : EMPTY;
        }
        return FarTiles.classifyThing(string, isoSprite, texture, bl, textureID, fArray, tileMesh, propertyContainer);
    }

    private static float[] floorLayer(IsoSprite isoSprite, Texture texture) {
        float[] fArray = new float[14];
        FloorArt.numbers(fArray, texture, -32.0f * (float)Core.tileScale + (float)isoSprite.soffX + texture.getOffsetX(), -96.0f * (float)Core.tileScale + (float)isoSprite.soffY + texture.getOffsetY(), 1.0f, 1.0f, isoSprite.tintMod.r, isoSprite.tintMod.g, isoSprite.tintMod.b, 1.0f);
        return fArray;
    }

    static GroundTiles ground(Tile[] tileArray) {
        TextureID[] textureIDArray = new TextureID[tileArray.length];
        float[] fArray = new float[tileArray.length * 14];
        for (int i = 0; i < tileArray.length; ++i) {
            Tile tile = tileArray[i];
            if (tile == null || tile.shell != 2 || tile.floor == null) continue;
            textureIDArray[i] = tile.page;
            System.arraycopy(tile.floor, 0, fArray, i * 14, 14);
        }
        return new GroundTiles(textureIDArray, fArray);
    }

    private static Tile classifyThing(String string, IsoSprite isoSprite, Texture texture, boolean bl, TextureID textureID, float[] fArray, TileMesh tileMesh, PropertyContainer propertyContainer) {
        float[] fArray2;
        boolean bl2;
        float[] fArray3 = tileMesh.data;
        float f = 0.0f;
        float f2 = Float.MAX_VALUE;
        float f3 = -3.4028235E38f;
        float f4 = Float.MAX_VALUE;
        float f5 = -3.4028235E38f;
        int n = 0;
        while (n + 2 < fArray3.length) {
            f2 = Math.min(f2, fArray3[n]);
            f3 = Math.max(f3, fArray3[n]);
            f = Math.max(f, fArray3[n + 1]);
            f4 = Math.min(f4, fArray3[n + 2]);
            f5 = Math.max(f5, fArray3[n + 2]);
            n += 8;
        }
        float f6 = f / 2.4494896f;
        boolean bl3 = Math.max(f3 - f2, f5 - f4) < 0.35f;
        boolean bl4 = bl2 = !bl && TileMeshes.isPlant(isoSprite);
        byte by = f6 < 0.5f || !bl && bl3 || bl2 ? (byte)0 : (bl ? (byte)3 : 4);
        IsoObjectType isoObjectType = isoSprite.getType();
        boolean bl5 = isoSprite.cutN || propertyContainer.has(IsoFlagType.collideN) || isoObjectType == IsoObjectType.doorN || isoObjectType == IsoObjectType.windowFN;
        boolean bl6 = isoSprite.cutW || propertyContainer.has(IsoFlagType.collideW) || isoObjectType == IsoObjectType.doorW || isoObjectType == IsoObjectType.windowFW;
        boolean bl7 = string.startsWith("roofs_");
        float[] fArray4 = fArray2 = bl2 ? TileMeshes.plantCard(texture) : null;
        byte by2 = bl ? (byte)4 : (fArray2 != null ? (byte)5 : (bl7 || f6 >= 0.2f && (!bl3 || bl5 || bl6) ? (byte)3 : 0));
        boolean bl8 = bl && TreeSeasons.seasonal(string);
        Tile tile = new Tile(by, f6, FarColours.of(string), tileMesh.vertCount == 0 ? (byte)0 : by2, textureID, fArray, bl ? TileMeshes.crossed(texture, 2) : tileMesh, (byte)(bl5 ? 1 : 0), (byte)(bl6 ? 1 : 0), (byte)((bl7 ? 1 : 0) | (bl ? 8 : 0)), bl8 ? string : null);
        tile.card = fArray2;
        if (bl8) {
            TreeSeasons.add(tile, texture);
        }
        if (by == 3) {
            FarTrees.add(tile, texture);
        }
        return tile;
    }

    private FarTiles() {
    }

    static final class Tile {
        final byte kind;
        final float height;
        final int colour;
        final byte shell;
        final TextureID page;
        final float[] map;
        final TileMesh mesh;
        final byte north;
        final byte west;
        final byte flags;
        final String tree;
        float[] floor;
        String floorName;
        float[] card;
        volatile Tile leaves;
        volatile Tile[] seasons;
        int treeKind = -1;
        volatile TreeAtlas.Slot billboard;

        Tile(byte by, float f, int n) {
            this(by, f, n, 0, null, null, null, 0, 0, 0, null);
        }

        Tile(byte by, float f, int n, byte by2, TextureID textureID, float[] fArray, TileMesh tileMesh, byte by3, byte by4, byte by5, String string) {
            this.kind = by;
            this.height = f;
            this.colour = n;
            this.shell = textureID == null ? (byte)0 : by2;
            this.page = textureID;
            this.map = fArray;
            this.mesh = tileMesh;
            this.north = by3;
            this.west = by4;
            this.flags = by5;
            this.tree = string;
        }

        Tile inSeason(int n) {
            Tile[] tileArray = this.seasons;
            Tile tile = tileArray == null ? null : tileArray[n];
            return tile != null ? tile : this;
        }
    }
}

