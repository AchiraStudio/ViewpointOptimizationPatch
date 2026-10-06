/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.core;

public final class CameraSquares {
    public static final int NORTH = 1;
    public static final int WEST = 2;
    public static final int FLOOR = 4;
    public static final int LEVELS = 2;
    public boolean on;
    public boolean seated;
    public int x;
    public int y;
    public int level;
    public int side;
    public float vehicleX;
    public float vehicleY;
    public float nearCorner;
    public byte[] blocks = new byte[0];

    public int at(int n, int n2, int n3) {
        if (n < 0 || n2 < 0 || n3 < 0 || n >= this.side || n2 >= this.side || n3 >= 2) {
            return -1;
        }
        return this.blocks[(n3 * this.side + n2) * this.side + n];
    }
}

