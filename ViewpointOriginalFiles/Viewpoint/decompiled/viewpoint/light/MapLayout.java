/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import viewpoint.light.LightLayout;
import viewpoint.light.LightLayouts;
import viewpoint.light.MapSquares;

public final class MapLayout {
    private static final int SIZE = 64;
    private static final int NO_ROOM = -1;
    private final MapSquares[] around;
    private final MapSquares centre;
    private final long[][] rooms;
    private final boolean[] roofed;
    private final boolean[] exterior;

    private MapLayout(MapSquares[] mapSquaresArray, long[][] lArray) {
        this.around = mapSquaresArray;
        this.centre = mapSquaresArray[4];
        this.rooms = lArray;
        this.roofed = new boolean[this.centre.levels * 64 * 64];
        this.exterior = new boolean[this.centre.levels * 64 * 64];
    }

    public static Block build(MapSquares[] mapSquaresArray, long[][] lArray) {
        MapLayout mapLayout = new MapLayout(mapSquaresArray, lArray);
        MapSquares mapSquares = mapLayout.centre;
        mapLayout.roofsAndRooms();
        ArrayList<LightLayouts.Level> arrayList = new ArrayList<LightLayouts.Level>();
        for (int i = 0; i < mapSquares.levels; ++i) {
            for (int j = 0; j < 64; j += 8) {
                for (int k = 0; k < 64; k += 8) {
                    LightLayouts.Level level = mapLayout.level(k, j, i);
                    if (level == null) continue;
                    arrayList.add(level);
                }
            }
        }
        return new Block(mapSquares.bx, mapSquares.by, arrayList, Arrays.copyOf(mapSquares.lamps, mapSquares.lampCount * 4));
    }

    private void roofsAndRooms() {
        int n = this.centre.levels;
        for (int i = 0; i < 64; ++i) {
            for (int j = 0; j < 64; ++j) {
                int n2;
                int n3;
                int n4 = this.centre.bx * 64 + j;
                int n5 = this.centre.by * 64 + i;
                for (n3 = n - 1; n3 > 0; --n3) {
                    n2 = this.centre.at(n4, n5, n3);
                    if (n2 < 0 || (n2 & 0x2004) == 0) continue;
                    for (int k = n3 - 1; k >= 0; --k) {
                        this.roofed[(k * 64 + i) * 64 + j] = true;
                    }
                    break;
                }
                for (n3 = 0; n3 < n; ++n3) {
                    n2 = (n3 * 64 + i) * 64 + j;
                    this.exterior[n2] = this.room(j, i, n3) == -1L && !this.roofed[n2];
                }
            }
        }
    }

    private long room(int n, int n2, int n3) {
        int n4 = this.centre.bits[(n3 * 64 + n2) * 64 + n];
        if ((n4 & 0x40000000) == 0 || this.rooms == null || n3 >= this.rooms.length || this.rooms[n3] == null) {
            return -1L;
        }
        return this.rooms[n3][n2 * 64 + n];
    }

    private LightLayouts.Level level(int n, int n2, int n3) {
        byte[] byArray = new byte[64];
        int[] nArray = new int[64];
        long[] lArray = new long[64];
        boolean bl = false;
        for (int i = 0; i < 64; ++i) {
            int n4 = n + i % 8;
            int n5 = n2 + i / 8;
            int n6 = this.centre.bx * 64 + n4;
            int n7 = this.centre.by * 64 + n5;
            int n8 = this.centre.at(n6, n7, n3);
            lArray[i] = -1L;
            if (n8 < 0) continue;
            bl = true;
            lArray[i] = this.room(n4, n5, n3);
            byArray[i] = (byte)(this.flags(n4, n5, n3) | 1);
            for (int j = 0; j < 10; ++j) {
                int n9 = i;
                nArray[n9] = nArray[n9] | this.test(n6, n7, n3, LightLayout.DX[j], LightLayout.DY[j], LightLayout.DZ[j]) << j * 3;
            }
        }
        return bl ? new LightLayouts.Level(Math.floorDiv(this.centre.bx * 64 + n, 8), Math.floorDiv(this.centre.by * 64 + n2, 8), n3, byArray, nArray, lArray) : null;
    }

    private int flags(int n, int n2, int n3) {
        return (this.openAir(n, n2, n3) ? 2 : 0) | (this.exterior[(n3 * 64 + n2) * 64 + n] ? 64 : 0);
    }

    private boolean openAir(int n, int n2, int n3) {
        int n4 = this.centre.bx * 64 + n;
        int n5 = this.centre.by * 64 + n2;
        for (int i = n3; i < this.centre.levels; ++i) {
            if (i > n3 && this.centre.at(n4, n5, i) < 0) {
                return true;
            }
            if (this.exterior[(i * 64 + n2) * 64 + n]) continue;
            return false;
        }
        return true;
    }

    private int test(int n, int n2, int n3, int n4, int n5, int n6) {
        if (n4 == 0 || n5 == 0) {
            return this.adjacent(n, n2, n3, n4, n5, n6);
        }
        int n7 = this.diagonal(n, n2, n3, n4, n5);
        if (n7 != 4 && this.at(n + n4, n2 + n5, n3) >= 0) {
            n7 = this.diagonal(n + n4, n2 + n5, n3, -n4, -n5);
        }
        return n7;
    }

    private int diagonal(int n, int n2, int n3, int n4, int n5) {
        int n6 = this.adjacent(n, n2, n3, n4, 0, 0);
        if (n6 == 4) {
            return n6;
        }
        int n7 = this.adjacent(n, n2, n3, 0, n5, 0);
        if (n7 == 4) {
            return n7;
        }
        return n6 != 2 && n7 != 2 ? this.adjacent(n, n2, n3, n4, n5, 0) : 2;
    }

    private int adjacent(int n, int n2, int n3, int n4, int n5, int n6) {
        int n7 = this.at(n, n2, n3);
        int n8 = this.at(n + n4, n2 + n5, n3 + n6);
        int n9 = 0;
        if (n8 >= 0 && n6 == 0) {
            int n10 = MapLayout.special(n7, true, n4, n5);
            int n11 = MapLayout.special(n8, false, n4, n5);
            if (n10 == 4 || n11 == 4) {
                return 4;
            }
            n9 = n11 != 0 ? n11 : n10;
        } else if (n6 > 0 && n8 >= 0 && this.outside(n, n2, n3 + n6) && !this.outside(n, n2, n3)) {
            n9 = 4;
        }
        return this.visionBlocked(n, n2, n3, n + n4, n2 + n5, n3 + n6) ? 4 : n9;
    }

    private static int special(int n, boolean bl, int n2, int n3) {
        boolean bl2;
        boolean bl3 = bl ? n3 < 0 : (bl2 = n3 > 0);
        boolean bl4 = bl ? n2 < 0 : n2 > 0;
        int n4 = 0;
        if (bl2 && (n & 0x20000) != 0) {
            if ((n & 0x40000) == 0) {
                return 4;
            }
            int n5 = n4 = (n & 0x80000) != 0 ? 1 : 3;
        }
        if (bl4 && (n & 0x100000) != 0) {
            if ((n & 0x200000) == 0) {
                return 4;
            }
            int n6 = n4 = (n & 0x400000) != 0 ? 1 : 3;
        }
        if (bl2 && (n & 0x800000) != 0 || bl4 && (n & 0x1000000) != 0) {
            n4 = 2;
        }
        return n4;
    }

    private boolean visionBlocked(int n, int n2, int n3, int n4, int n5, int n6) {
        boolean bl;
        int n7 = this.at(n, n2, n3);
        int n8 = this.at(n4, n5, n6);
        if (n8 < 0 || n7 < 0) {
            return false;
        }
        if (((n7 | n8) & 2) != 0) {
            return false;
        }
        if (n3 != n6 && this.betweenLevels(n7, n8, n, n2, n3, n4, n5, n6)) {
            return true;
        }
        boolean bl2 = bl = n5 < n2 && MapLayout.cut(n7, 32, 128, 32768) && (n7 & 0x200) == 0 || n4 < n && MapLayout.cut(n7, 64, 256, 65536) && (n7 & 0x400) == 0 || n5 > n2 && MapLayout.cut(n8, 32, 128, 32768) && (n8 & 0x200) == 0 || n4 > n && MapLayout.cut(n8, 64, 256, 65536) && (n8 & 0x400) == 0;
        if (bl || (n8 & 0x1800) != 0) {
            return true;
        }
        return n4 != n && n5 != n2 && (this.visionBlocked(n, n2, n3, n, n5, n3) || this.visionBlocked(n, n2, n3, n4, n2, n3) || this.visionBlocked(n4, n5, n6, n, n5, n3) || this.visionBlocked(n4, n5, n6, n4, n2, n3));
    }

    private boolean betweenLevels(int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8) {
        if (n8 > n5) {
            int n9 = this.at(n3, n4, n8);
            return MapLayout.opaqueFloor(n2) || (n & 0x10) != 0 || n9 >= 0 && MapLayout.opaqueFloor(n9);
        }
        int n10 = this.at(n6, n7, n5);
        return MapLayout.opaqueFloor(n) || (n & 0x10) != 0 || n10 >= 0 && MapLayout.opaqueFloor(n10);
    }

    private static boolean cut(int n, int n2, int n3, int n4) {
        return (n & n2) != 0 && ((n & n3) == 0 || (n & n4) != 0);
    }

    private static boolean opaqueFloor(int n) {
        return (n & 4) != 0 && ((n & 8) == 0 || (n & 0x4000) != 0);
    }

    private boolean outside(int n, int n2, int n3) {
        int n4 = n - this.centre.bx * 64;
        int n5 = n2 - this.centre.by * 64;
        return n3 >= 0 && n3 < this.centre.levels && this.exterior[(n3 * 64 + n5) * 64 + n4];
    }

    private int at(int n, int n2, int n3) {
        int n4 = Math.floorDiv(n, 64) - this.centre.bx;
        int n5 = Math.floorDiv(n2, 64) - this.centre.by;
        if (n4 < -1 || n4 > 1 || n5 < -1 || n5 > 1) {
            return -1;
        }
        MapSquares mapSquares = this.around[(n5 + 1) * 3 + n4 + 1];
        return mapSquares == null ? -1 : mapSquares.at(n, n2, n3);
    }

    public static final class Block {
        public final int bx;
        public final int by;
        final List<LightLayouts.Level> levels;
        final int[] lamps;

        Block(int n, int n2, List<LightLayouts.Level> list, int[] nArray) {
            this.bx = n;
            this.by = n2;
            this.levels = list;
            this.lamps = nArray;
        }
    }
}

