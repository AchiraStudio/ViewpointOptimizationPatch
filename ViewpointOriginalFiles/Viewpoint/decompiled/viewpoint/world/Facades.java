/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.BuildingDef
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.areas.IsoBuilding
 *  zombie.iso.areas.IsoRoom
 *  zombie.iso.sprite.IsoSprite
 *  zombie.iso.sprite.IsoSpriteManager
 *  zombie.util.list.PZArrayList
 */
package viewpoint.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import viewpoint.world.FacadeColours;
import zombie.core.properties.PropertyContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.list.PZArrayList;

public final class Facades {
    public static final int SOLID = 0;
    public static final int WINDOW = 1;
    public static final int DOOR = 2;
    public static final int NEUTRAL = 9210501;
    private static final int KINDS = 6;
    private static final IdentityHashMap<BuildingDef, Average> buildingColours = new IdentityHashMap();
    private static final IdentityHashMap<RoomDef, Average> roomColours = new IdentityHashMap();
    private static final IdentityHashMap<IsoRoom, String[]> roomTiles = new IdentityHashMap();
    private static final int FACADE_REACH = 64;
    private static final int COLUMN_SEARCH = 6;
    private static final IdentityHashMap<BuildingDef, HashMap<Integer, String>> dominant = new IdentityHashMap();

    static int fill(IsoGridSquare isoGridSquare, IsoGridSquare isoGridSquare2, IsoSprite isoSprite) {
        IsoRoom isoRoom;
        IsoRoom isoRoom2 = isoGridSquare.getRoom();
        IsoRoom isoRoom3 = isoRoom = isoGridSquare2 == null ? null : isoGridSquare2.getRoom();
        if (isoRoom != null) {
            return isoRoom == isoRoom2 ? -1 : Facades.roomColour(isoRoom);
        }
        if (isoRoom2 != null) {
            return Facades.buildingColour(isoRoom2);
        }
        return isoSprite.getName() != null && isoSprite.getName().contains("interior") ? 9210501 : -1;
    }

    static IsoSprite roomTile(IsoGridSquare isoGridSquare, boolean bl, int n) {
        Object object;
        IsoRoom isoRoom;
        IsoRoom isoRoom2 = isoRoom = isoGridSquare == null ? null : isoGridSquare.getRoom();
        if (isoRoom == null) {
            return null;
        }
        String[] stringArray = roomTiles.get(isoRoom);
        if (stringArray == null) {
            stringArray = new String[6];
            object = isoRoom.getSquares();
            for (int i = 0; i < ((ArrayList)object).size(); ++i) {
                PZArrayList pZArrayList = ((IsoGridSquare)((ArrayList)object).get(i)).getObjects();
                for (int j = 0; j < pZArrayList.size(); ++j) {
                    int n2;
                    IsoSprite isoSprite = ((IsoObject)pZArrayList.get(j)).getSprite();
                    int n3 = n2 = isoSprite == null || isoSprite.getName() == null ? -1 : Facades.kindOf(isoSprite);
                    if (n2 < 0 || stringArray[n2] != null) continue;
                    stringArray[n2] = isoSprite.getName();
                }
            }
            roomTiles.put(isoRoom, stringArray);
        }
        object = stringArray[n * 2 + (bl ? 1 : 0)];
        return object != null && IsoSpriteManager.instance.getNamedMap().containsKey(object) ? IsoSpriteManager.instance.getSprite((String)object) : null;
    }

    static IsoSprite facadeTile(IsoGridSquare isoGridSquare, boolean bl, int n) {
        int n2;
        IsoRoom isoRoom = isoGridSquare.getRoom();
        if (isoRoom == null) {
            return null;
        }
        int n3 = n * 2 + (bl ? 1 : 0);
        IsoCell isoCell = isoGridSquare.getCell();
        int n4 = isoGridSquare.getX();
        int n5 = isoGridSquare.getY();
        int n6 = isoGridSquare.getZ();
        int n7 = -1;
        for (n2 = 1; n2 <= 64; ++n2) {
            IsoGridSquare isoGridSquare2;
            IsoGridSquare isoGridSquare3 = isoGridSquare2 = bl ? isoCell.getGridSquare(n4, n5 + n2, n6) : isoCell.getGridSquare(n4 + n2, n5, n6);
            if (isoGridSquare2 == null) break;
            if (isoGridSquare2.getRoom() == null) {
                n7 = bl ? n5 + n2 : n4 + n2;
                break;
            }
            if (isoGridSquare2.getRoom().building != isoRoom.building) break;
        }
        if (n7 >= 0) {
            block1: for (n2 = 0; n2 <= 6; ++n2) {
                for (int i = 1; i >= -1; i -= 2) {
                    IsoSprite isoSprite;
                    int n8 = (bl ? n4 : n5) + n2 * i;
                    IsoGridSquare isoGridSquare4 = bl ? isoCell.getGridSquare(n8, n7, n6) : isoCell.getGridSquare(n7, n8, n6);
                    IsoSprite isoSprite2 = isoSprite = isoGridSquare4 == null ? null : Facades.wallOfSlot(isoGridSquare4, n3);
                    if (isoSprite != null) {
                        return isoSprite;
                    }
                    if (n2 == 0) continue block1;
                }
            }
        }
        return Facades.dominantFacade(isoRoom, n6, n3);
    }

    private static IsoSprite wallOfSlot(IsoGridSquare isoGridSquare, int n) {
        PZArrayList pZArrayList = isoGridSquare.getObjects();
        for (int i = 0; i < pZArrayList.size(); ++i) {
            IsoSprite isoSprite = ((IsoObject)pZArrayList.get(i)).getSprite();
            if (isoSprite == null || isoSprite.getName() == null || Facades.kindOf(isoSprite) != n) continue;
            return isoSprite;
        }
        return null;
    }

    private static IsoSprite dominantFacade(IsoRoom isoRoom, int n, int n2) {
        int n3;
        IsoBuilding isoBuilding = isoRoom.building;
        if (isoBuilding == null || isoBuilding.def == null) {
            return null;
        }
        HashMap hashMap = dominant.computeIfAbsent(isoBuilding.def, buildingDef -> new HashMap());
        String string = (String)hashMap.get(n3 = n * 6 + n2);
        if (string == null && !hashMap.containsKey(n3)) {
            HashMap<String, Integer> hashMap2 = new HashMap<String, Integer>();
            for (int i = 0; i < isoBuilding.rooms.size(); ++i) {
                ArrayList arrayList = ((IsoRoom)isoBuilding.rooms.get(i)).getSquares();
                for (int j = 0; j < arrayList.size(); ++j) {
                    IsoSprite isoSprite;
                    IsoGridSquare isoGridSquare = (IsoGridSquare)arrayList.get(j);
                    if (isoGridSquare.getZ() != n) continue;
                    IsoGridSquare isoGridSquare2 = n2 % 2 == 1 ? isoGridSquare.getS() : isoGridSquare.getE();
                    IsoSprite isoSprite2 = isoSprite = isoGridSquare2 == null || isoGridSquare2.getRoom() != null ? null : Facades.wallOfSlot(isoGridSquare2, n2);
                    if (isoSprite == null) continue;
                    hashMap2.merge(isoSprite.getName(), 1, Integer::sum);
                }
            }
            string = hashMap2.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
            hashMap.put(n3, string);
        }
        return string != null && IsoSpriteManager.instance.getNamedMap().containsKey(string) ? IsoSpriteManager.instance.getSprite(string) : null;
    }

    static void clear() {
        roomTiles.clear();
    }

    static void forgetColours() {
        buildingColours.clear();
        roomColours.clear();
        dominant.clear();
    }

    private static int buildingColour(IsoRoom isoRoom) {
        int n;
        Average average;
        IsoBuilding isoBuilding = isoRoom.building;
        BuildingDef buildingDef = isoBuilding == null ? null : isoBuilding.def;
        Average average2 = average = buildingDef == null ? null : buildingColours.get(buildingDef);
        if (average == null) {
            average = new Average();
            if (isoBuilding == null) {
                Facades.facadeAround(isoRoom, average);
            } else {
                for (n = 0; n < isoBuilding.rooms.size(); ++n) {
                    Facades.facadeAround((IsoRoom)isoBuilding.rooms.get(n), average);
                }
            }
            if (buildingDef != null) {
                buildingColours.put(buildingDef, average);
            }
        }
        return (n = average.colour()) < 0 ? 9210501 : n;
    }

    private static void facadeAround(IsoRoom isoRoom, Average average) {
        ArrayList arrayList = isoRoom.getSquares();
        for (int i = 0; i < arrayList.size(); ++i) {
            IsoGridSquare isoGridSquare = (IsoGridSquare)arrayList.get(i);
            IsoGridSquare isoGridSquare2 = isoGridSquare.getS();
            IsoGridSquare isoGridSquare3 = isoGridSquare.getE();
            if (isoGridSquare2 != null && isoGridSquare2.getRoom() == null) {
                Facades.collect(isoGridSquare2, average);
            }
            if (isoGridSquare3 == null || isoGridSquare3.getRoom() != null) continue;
            Facades.collect(isoGridSquare3, average);
        }
    }

    private static int roomColour(IsoRoom isoRoom) {
        int n;
        Average average;
        RoomDef roomDef = isoRoom.def;
        Average average2 = average = roomDef == null ? null : roomColours.get(roomDef);
        if (average == null) {
            average = new Average();
            ArrayList arrayList = isoRoom.getSquares();
            for (int i = 0; i < arrayList.size(); ++i) {
                Facades.collect((IsoGridSquare)arrayList.get(i), average);
            }
            if (roomDef != null) {
                roomColours.put(roomDef, average);
            }
        }
        return (n = average.colour()) < 0 ? 9210501 : n;
    }

    private static void collect(IsoGridSquare isoGridSquare, Average average) {
        PZArrayList pZArrayList = isoGridSquare.getObjects();
        for (int i = 0; i < pZArrayList.size(); ++i) {
            int n;
            IsoSprite isoSprite = ((IsoObject)pZArrayList.get(i)).getSprite();
            if (isoSprite == null || isoSprite.getName() == null || Facades.kindOf(isoSprite) < 0 || (n = FacadeColours.of(isoSprite.getName())) < 0) continue;
            average.add(n);
        }
    }

    static int kindOf(IsoSprite isoSprite) {
        PropertyContainer propertyContainer = isoSprite.getProperties();
        if (propertyContainer.has(IsoFlagType.WindowN)) {
            return 3;
        }
        if (propertyContainer.has(IsoFlagType.WindowW)) {
            return 2;
        }
        if (propertyContainer.has(IsoFlagType.DoorWallN)) {
            return 5;
        }
        if (propertyContainer.has(IsoFlagType.DoorWallW)) {
            return 4;
        }
        if (propertyContainer.has(IsoFlagType.WallNW)) {
            return -1;
        }
        if (propertyContainer.has(IsoFlagType.WallN)) {
            return 1;
        }
        if (propertyContainer.has(IsoFlagType.WallW)) {
            return 0;
        }
        return -1;
    }

    private Facades() {
    }

    private static final class Average {
        long r;
        long g;
        long b;
        long n;

        private Average() {
        }

        void add(int n) {
            this.r += (long)(n >> 16 & 0xFF);
            this.g += (long)(n >> 8 & 0xFF);
            this.b += (long)(n & 0xFF);
            ++this.n;
        }

        int colour() {
            return this.n == 0L ? -1 : (int)(this.r / this.n) << 16 | (int)(this.g / this.n) << 8 | (int)(this.b / this.n);
        }
    }
}

