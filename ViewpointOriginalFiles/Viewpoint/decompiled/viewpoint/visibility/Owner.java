/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.visibility;

public final class Owner {
    public static final int CONTENTS = 0;
    public static final int NORTH = 1;
    public static final int WEST = 2;
    public static final int FLOOR = 3;
    public static final int NONE = -1;

    public static int of(int n, int n2) {
        return n2 == -1 ? -1 : n << 2 | n2;
    }

    public static int square(int n) {
        return n >> 2;
    }

    public static int kind(int n) {
        return n & 3;
    }

    private Owner() {
    }
}

