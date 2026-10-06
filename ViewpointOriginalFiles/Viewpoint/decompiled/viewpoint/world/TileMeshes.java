/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.textures.Texture
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.SpriteDetails.IsoObjectType
 *  zombie.iso.sprite.IsoSprite
 *  zombie.tileDepth.TileDepthTextureAssignmentManager
 *  zombie.tileDepth.TileGeometryFile$Geometry
 *  zombie.tileDepth.TileGeometryManager
 */
package viewpoint.world;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import viewpoint.platform.Caches;
import viewpoint.platform.LiveSettings;
import viewpoint.world.MeshBuilder;
import viewpoint.world.TileMesh;
import viewpoint.world.WorldMesher;
import zombie.core.textures.Texture;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.sprite.IsoSprite;
import zombie.tileDepth.TileDepthTextureAssignmentManager;
import zombie.tileDepth.TileGeometryFile;
import zombie.tileDepth.TileGeometryManager;

public final class TileMeshes {
    private static final String GAME = "game";
    private static final float SQUARE_PIXELS = 45.254833f;
    private static final float UNIT_PIXELS = 16.0f;
    private static final float FOOT_PIXELS = 16.0f;
    private static final int CROSSED_CARDS = 3;
    static final LiveSettings.Choice GRASS_CARDS = LiveSettings.choice("plants.grassCards", "Grass and bushes", "World/Plants", new String[]{"One card facing you", "Two crossed planes", "Three crossed planes"}, 1);
    private static final IdentityHashMap<IsoSprite, Held> cache;
    private static long bytes;
    private static final TileMesh FLOOR;
    private static final TileMesh WALL_N;
    private static final TileMesh WALL_W;
    private static final TileMesh WALL_NW;
    private static final float WALL_ART_W = -0.46875f;
    private static final float WALL_ART_N = -0.4375f;

    static TileMesh floorMesh() {
        return FLOOR;
    }

    public static TileMesh get(IsoSprite isoSprite, Texture texture, IsoSprite isoSprite2) {
        Held held = cache.get(isoSprite);
        if (held == null) {
            held = new Held(TileMeshes.create(isoSprite, texture, isoSprite2));
            cache.put(isoSprite, held);
            bytes += TileMeshes.size(held.mesh);
        }
        held.used = Caches.tick();
        return held.mesh;
    }

    private static long size(TileMesh tileMesh) {
        return (long)tileMesh.data.length * 4L + 64L;
    }

    public static TileMesh peek(IsoSprite isoSprite, Texture texture, IsoSprite isoSprite2) {
        Held held = cache.get(isoSprite);
        return held != null ? held.mesh : TileMeshes.create(isoSprite, texture, isoSprite2);
    }

    private static TileMesh create(IsoSprite isoSprite, Texture texture, IsoSprite isoSprite2) {
        boolean bl;
        if (isoSprite2 != null || TileMeshes.isPlant(isoSprite)) {
            return TileMeshes.crossed(texture, 3);
        }
        if (isoSprite.solidfloor) {
            return FLOOR;
        }
        IsoObjectType isoObjectType = isoSprite.getType();
        boolean bl2 = isoObjectType == IsoObjectType.windowFN || isoObjectType == IsoObjectType.windowFW || isoObjectType == IsoObjectType.doorN || isoObjectType == IsoObjectType.doorW || isoObjectType == IsoObjectType.curtainN || isoObjectType == IsoObjectType.curtainS || isoObjectType == IsoObjectType.curtainW || isoObjectType == IsoObjectType.curtainE;
        boolean bl3 = !bl2 && TileMeshes.edge(isoSprite, IsoFlagType.WallN, IsoFlagType.WindowN, IsoFlagType.DoorWallN);
        boolean bl4 = bl = !bl2 && TileMeshes.edge(isoSprite, IsoFlagType.WallW, IsoFlagType.WindowW, IsoFlagType.DoorWallW);
        if (!bl2 && isoSprite.getProperties().has(IsoFlagType.WallNW) || bl3 && bl) {
            return WALL_NW;
        }
        if (bl3) {
            return WALL_N;
        }
        if (bl) {
            return WALL_W;
        }
        ArrayList<TileGeometryFile.Geometry> arrayList = TileMeshes.geometryFor(isoSprite);
        if (arrayList != null && !arrayList.isEmpty()) {
            MeshBuilder meshBuilder = new MeshBuilder();
            for (int i = 0; i < arrayList.size(); ++i) {
                meshBuilder.add(arrayList.get(i));
            }
            return meshBuilder.build();
        }
        return TileMeshes.edgeQuads(isoSprite);
    }

    private static TileMesh edgeQuads(IsoSprite isoSprite) {
        boolean bl;
        boolean bl2 = isoSprite.cutN || TileMeshes.edge(isoSprite, IsoFlagType.DoorWallN, IsoFlagType.WindowN, IsoFlagType.collideN, IsoFlagType.HoppableN, IsoFlagType.TallHoppableN, IsoFlagType.transparentN);
        boolean bl3 = bl = isoSprite.cutW || TileMeshes.edge(isoSprite, IsoFlagType.DoorWallW, IsoFlagType.WindowW, IsoFlagType.collideW, IsoFlagType.HoppableW, IsoFlagType.TallHoppableW, IsoFlagType.transparentW);
        if (bl2 && bl) {
            return WALL_NW;
        }
        if (bl2) {
            return WALL_N;
        }
        if (bl) {
            return WALL_W;
        }
        return TileMesh.EMPTY;
    }

    private static ArrayList<TileGeometryFile.Geometry> geometryFor(IsoSprite isoSprite) {
        ArrayList<TileGeometryFile.Geometry> arrayList = TileMeshes.lookup(isoSprite.tilesetName, isoSprite.tileSheetIndex);
        if ((arrayList == null || arrayList.isEmpty()) && isoSprite.name != null) {
            int n;
            String string = TileDepthTextureAssignmentManager.getInstance().getAssignedTileName(GAME, isoSprite.name);
            int n2 = n = string == null ? -1 : string.lastIndexOf(95);
            if (n > 0) {
                try {
                    arrayList = TileMeshes.lookup(string.substring(0, n), Integer.parseInt(string.substring(n + 1)));
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            }
        }
        return arrayList;
    }

    private static ArrayList<TileGeometryFile.Geometry> lookup(String string, int n) {
        if (string == null || n < 0) {
            return null;
        }
        return TileGeometryManager.getInstance().getGeometry(GAME, string, n % 8, n / 8);
    }

    private static boolean edge(IsoSprite isoSprite, IsoFlagType ... isoFlagTypeArray) {
        for (IsoFlagType isoFlagType : isoFlagTypeArray) {
            if (!isoSprite.getProperties().has(isoFlagType)) continue;
            return true;
        }
        return false;
    }

    static int plantCards(IsoSprite isoSprite, IsoSprite isoSprite2) {
        if (isoSprite2 == null && !TileMeshes.isPlant(isoSprite)) {
            return -1;
        }
        if (TileMeshes.tree(isoSprite, isoSprite2)) {
            return 3;
        }
        int n = GRASS_CARDS.get();
        return n == 0 ? 0 : n + 1;
    }

    static int plantFlags(IsoSprite isoSprite, IsoSprite isoSprite2, int n) {
        return n > 0 && !TileMeshes.tree(isoSprite, isoSprite2) ? 4096 : 0;
    }

    private static boolean tree(IsoSprite isoSprite, IsoSprite isoSprite2) {
        return (isoSprite2 != null ? isoSprite2 : isoSprite).getType() == IsoObjectType.tree;
    }

    public static boolean isPlant(IsoSprite isoSprite) {
        return isoSprite.getType() == IsoObjectType.tree || isoSprite.getProperties().has(IsoFlagType.vegitation) || isoSprite.getProperties().has(IsoFlagType.canBeCut);
    }

    public static TileMesh crossed(Texture texture, int n) {
        MeshBuilder meshBuilder = new MeshBuilder();
        meshBuilder.crossed(TileMeshes.plantHalfWidth(texture), TileMeshes.plantHeight(texture), n);
        return meshBuilder.build();
    }

    public static float plantHalfWidth(Texture texture) {
        return (float)texture.getWidthOrig() / WorldMesher.pixelScale(texture) / 90.50967f;
    }

    public static float plantHeight(Texture texture) {
        return ((float)texture.getHeightOrig() / WorldMesher.pixelScale(texture) - 16.0f) / 39.191833f;
    }

    public static float[] plantCard(Texture texture) {
        float f = WorldMesher.pixelScale(texture);
        float f2 = 1.0f / (f * 45.254833f);
        float f3 = 1.0f / (f * 16.0f * 2.4494896f);
        float f4 = (float)texture.getWidthOrig() * 0.5f;
        float f5 = (float)texture.getHeightOrig() - 16.0f * f;
        float f6 = texture.getOffsetX();
        float f7 = texture.getOffsetY();
        return new float[]{(f6 - f4) * f2, (f6 + (float)texture.getWidth() - f4) * f2, (f5 - f7 - (float)texture.getHeight()) * f3, (f5 - f7) * f3};
    }

    private static TileMesh floor() {
        MeshBuilder meshBuilder = new MeshBuilder();
        meshBuilder.floor();
        return meshBuilder.build();
    }

    static TileMesh wall(boolean bl) {
        return bl ? WALL_N : WALL_W;
    }

    private static TileMesh wall(boolean bl, boolean bl2) {
        MeshBuilder meshBuilder = new MeshBuilder();
        if (bl) {
            meshBuilder.edgeQuad(2, -0.4375f);
        }
        if (bl2) {
            meshBuilder.edgeQuad(0, -0.46875f);
        }
        return meshBuilder.build();
    }

    private TileMeshes() {
    }

    static {
        GRASS_CARDS.describe("How the grass, bushes and small plants round you stand: one card turned to face you, or two or three planes crossed through the plant, which look the same from every side.");
        cache = new IdentityHashMap();
        Caches.register(new Caches.Cache(){

            @Override
            public long bytes() {
                return bytes;
            }

            @Override
            public void list(Caches.Survey survey) {
                for (Held held : cache.values()) {
                    survey.add(held.used, TileMeshes.size(held.mesh));
                }
            }

            @Override
            public void evictBefore(long l) {
                Iterator<Held> iterator = cache.values().iterator();
                while (iterator.hasNext()) {
                    Held held = iterator.next();
                    if (held.used >= l) continue;
                    bytes -= TileMeshes.size(held.mesh);
                    iterator.remove();
                }
            }
        });
        FLOOR = TileMeshes.floor();
        WALL_N = TileMeshes.wall(true, false);
        WALL_W = TileMeshes.wall(false, true);
        WALL_NW = TileMeshes.wall(true, true);
    }

    private static final class Held {
        final TileMesh mesh;
        long used;

        Held(TileMesh tileMesh) {
            this.mesh = tileMesh;
        }
    }
}

