/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.visibility;

import java.util.Arrays;

final class Fragment {
    static final int SIZE = 8;
    static final int SQUARES = 64;
    final int chunkX;
    final int chunkY;
    final int level;
    final boolean bottom;
    final long known;
    final long floor;
    final long stairs;
    final long roofed;
    final long northWall;
    final long westWall;
    final long northOpening;
    final long westOpening;
    final long northDoor;
    final long westDoor;
    final long northWindow;
    final long westWindow;
    private final long[] rooms;
    static final int KNOWN = 0;
    static final int FLOOR = 1;
    static final int STAIRS = 2;
    static final int ROOFED = 3;
    static final int NORTH_WALL = 4;
    static final int WEST_WALL = 5;
    static final int NORTH_OPENING = 6;
    static final int WEST_OPENING = 7;
    static final int NORTH_DOOR = 8;
    static final int WEST_DOOR = 9;
    static final int NORTH_WINDOW = 10;
    static final int WEST_WINDOW = 11;
    static final int MASKS = 12;

    Fragment(int n, int n2, int n3, boolean bl, long[] lArray, long[] lArray2) {
        this.chunkX = n;
        this.chunkY = n2;
        this.level = n3;
        this.bottom = bl;
        this.known = lArray[0];
        this.floor = lArray[1];
        this.stairs = lArray[2];
        this.roofed = lArray[3];
        this.northWall = lArray[4];
        this.westWall = lArray[5];
        this.northOpening = lArray[6];
        this.westOpening = lArray[7];
        this.northDoor = lArray[8];
        this.westDoor = lArray[9];
        this.northWindow = lArray[10];
        this.westWindow = lArray[11];
        this.rooms = (long[])lArray2.clone();
    }

    long room(int n) {
        return this.rooms[n];
    }

    boolean has(long l, int n) {
        return (l >>> n & 1L) != 0L;
    }

    static long key(int n, int n2, int n3) {
        return (long)(n & 0xFFFFFF) << 40 | (long)(n2 & 0xFFFFFF) << 16 | (long)(n3 + 32 & 0xFFFF);
    }

    long key() {
        return Fragment.key(this.chunkX, this.chunkY, this.level);
    }

    boolean sameLayout(Fragment fragment) {
        return fragment != null && fragment.chunkX == this.chunkX && fragment.chunkY == this.chunkY && fragment.level == this.level && fragment.bottom == this.bottom && fragment.known == this.known && fragment.floor == this.floor && fragment.stairs == this.stairs && fragment.roofed == this.roofed && fragment.northWall == this.northWall && fragment.westWall == this.westWall && fragment.northOpening == this.northOpening && fragment.westOpening == this.westOpening && fragment.northDoor == this.northDoor && fragment.westDoor == this.westDoor && fragment.northWindow == this.northWindow && fragment.westWindow == this.westWindow && Arrays.equals(fragment.rooms, this.rooms);
    }
}

