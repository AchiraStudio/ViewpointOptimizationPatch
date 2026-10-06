/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import viewpoint.light.GridTexels;
import viewpoint.light.LightLayout;
import viewpoint.light.LightLayouts;
import viewpoint.light.LightModel;
import viewpoint.light.LightWork;
import viewpoint.light.SourceLight;

final class LevelLight {
    private static final int RING = 12;
    final int chunkX;
    final int chunkY;
    final int level;
    final byte[] grid;
    final int[] light;
    final byte[] steps;

    private LevelLight(int n, int n2, int n3, byte[] byArray, int[] nArray, byte[] byArray2) {
        this.chunkX = n;
        this.chunkY = n2;
        this.level = n3;
        this.grid = byArray;
        this.light = nArray;
        this.steps = byArray2;
    }

    long bytes() {
        return (long)this.grid.length + (long)this.light.length * 4L + (long)this.steps.length + 64L;
    }

    static int firstSquare(int n) {
        return n * 8 - 2 - 15;
    }

    static int lastSquare(int n) {
        return LevelLight.firstSquare(n) + 12 - 1 + 30;
    }

    static LevelLight work(LightWork lightWork, int n, int n2, int n3, List<LightLayouts.Level> list, List<SourceLight> list2) {
        int n4;
        LightLayout lightLayout = lightWork.layout(LevelLight.firstSquare(n), LevelLight.firstSquare(n2), n3, LevelLight.lastSquare(n), LevelLight.lastSquare(n2), n3, list);
        LightModel.skySteps(lightLayout, lightWork.steps, lightWork.queue);
        Ring ring = new Ring(n, n2, n3, list);
        int n5 = n * 8 - 2;
        int n6 = n2 * 8 - 2;
        for (int i = 0; i < 144; ++i) {
            int n7 = n5 + i % 12;
            int n8 = n6 + i / 12;
            n4 = lightLayout.index(n7, n8, n3);
            ring.exists[i] = lightLayout.exists(n4);
            ring.outdoors[i] = !ring.exists[i] || lightLayout.has(n4, 64);
            ring.steps[i] = ring.exists[i] ? lightWork.steps[n4] : (byte)0;
            ring.light[i] = 0;
            for (SourceLight sourceLight : list2) {
                ring.light[i] = LightModel.brighter(ring.light[i], sourceLight.at(n7, n8, n3));
            }
        }
        byte[] byArray = new byte[3200];
        ring.write(ByteBuffer.wrap(byArray));
        int[] nArray = new int[64];
        byte[] byArray2 = new byte[64];
        for (n4 = 0; n4 < 64; ++n4) {
            int n9 = (n4 / 8 + 2) * 12 + n4 % 8 + 2;
            nArray[n4] = ring.light[n9];
            byArray2[n4] = ring.steps[n9];
        }
        return new LevelLight(n, n2, n3, byArray, nArray, byArray2);
    }

    private static final class Ring {
        final boolean[] exists = new boolean[144];
        final boolean[] outdoors = new boolean[144];
        final long[] rooms = new long[144];
        final int[] light = new int[144];
        final byte[] steps = new byte[144];
        final int[] corners = new int[4];
        final int[] darker = new int[4];

        Ring(int n, int n2, int n3, List<LightLayouts.Level> list) {
            int n4 = n * 8 - 2;
            int n5 = n2 * 8 - 2;
            Arrays.fill(this.rooms, -1L);
            for (LightLayouts.Level level : list) {
                if (level.level != n3 || Math.abs(level.chunkX - n) > 1 || Math.abs(level.chunkY - n2) > 1) continue;
                for (int i = 0; i < 64; ++i) {
                    int n6 = level.chunkX * 8 + i % 8 - n4;
                    int n7 = level.chunkY * 8 + i / 8 - n5;
                    if (n6 < 0 || n6 >= 12 || n7 < 0 || n7 >= 12) continue;
                    this.rooms[n7 * 12 + n6] = level.rooms[i];
                }
            }
        }

        void write(ByteBuffer byteBuffer) {
            for (int i = 0; i < 10; ++i) {
                for (int j = 0; j < 10; ++j) {
                    int n = (i + 1) * 12 + j + 1;
                    for (int k = 0; k < 4; ++k) {
                        this.corner(n, k);
                    }
                    GridTexels.light(byteBuffer, j, i, this.corners, this.outdoors[n]);
                    GridTexels.sky(byteBuffer, j, i, this.darker);
                }
            }
        }

        private void corner(int n, int n2) {
            if (!this.exists[n]) {
                this.darker[n2] = 0;
                this.corners[n2] = 0;
                return;
            }
            int n3 = n2 == 1 || n2 == 2 ? 1 : -1;
            int n4 = n2 >= 2 ? 12 : -12;
            int n5 = 0;
            int n6 = 0;
            int n7 = 0;
            int n8 = 0;
            int n9 = 0;
            for (int i = 0; i < 4; ++i) {
                int n10 = n + (i & 1) * n3 + (i >> 1) * n4;
                if (i != 0 && (!this.exists[n10] || this.rooms[n10] != this.rooms[n])) continue;
                n5 += this.light[n10] & 0xFF;
                n6 += this.light[n10] >> 8 & 0xFF;
                n7 += this.light[n10] >> 16 & 0xFF;
                n8 += Math.min(255, 16 * this.steps[n10]);
                ++n9;
            }
            this.corners[n2] = (n5 + n9 / 2) / n9 | (n6 + n9 / 2) / n9 << 8 | (n7 + n9 / 2) / n9 << 16;
            this.darker[n2] = (n8 + n9 / 2) / n9;
        }
    }
}

