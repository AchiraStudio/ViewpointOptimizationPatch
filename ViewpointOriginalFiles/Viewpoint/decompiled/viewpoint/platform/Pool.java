/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import viewpoint.platform.LiveSettings;

public final class Pool {
    public static final double FULL = 0.9;
    public static final double ROOMY = 0.8;
    private static final long MIB = 0x100000L;
    private static final int MIN_MIB = 16;
    private static final int RANGE = 8;
    private static final int STEP_MIB = 16;
    private static final String SECTION = "World/Memory";
    public static final Pool NEAR_MESHES = new Pool("near meshes", "memory.nearMeshMiB", 512);
    public static final Pool LIGHT_GRIDS = new Pool("light grids", "memory.lightAtlasMiB", 64);
    public static final Pool SHELL = new Pool("shell", "memory.shellMiB", 640);
    public static final Pool FAR = new Pool("far cells", "memory.farMiB", 160);
    public static final Pool MODEL_MESHES = new Pool("model meshes", "memory.modelMeshMiB", 256);
    public static final Pool CPU = new Pool("cpu caches", "memory.cpuCacheMiB", 256);
    public static final Pool FLOORS = new Pool("floor pages", "memory.floorMiB", 384);
    public static final Pool PACKS = new Pool("model packs", "memory.packMiB", 512);
    public static final Pool IRIS = new Pool("Minecraft shaders", "memory.minecraftMiB", 4096);
    public static final Pool CORPSES = new Pool("posed corpses", "memory.corpseMiB", 256);
    static final Pool[] ALL = new Pool[]{NEAR_MESHES, LIGHT_GRIDS, SHELL, FAR, MODEL_MESHES, CPU, FLOORS, PACKS, IRIS, CORPSES};
    final String name;
    final String setting;
    private final LiveSettings.Number chosen;
    private volatile long limit = Long.MAX_VALUE;
    private volatile long used;

    private Pool(String string, String string2, int n) {
        this.name = string;
        this.setting = string2;
        this.chosen = LiveSettings.number(string2, "Cap: " + string + " (MiB)", SECTION, 16.0f, n * 8, 16.0f, n);
        this.chosen.describe("The most this pool holds; lowered, it takes full effect after a restart.");
    }

    public void alsoIn(String string) {
        this.chosen.alsoIn(string);
    }

    public void describe(String string) {
        this.chosen.describe(string);
    }

    public long cap() {
        return Math.min((long)this.chosen.getInt() * 0x100000L, this.limit);
    }

    public void limit(long l) {
        this.limit = l;
    }

    public void use(long l) {
        this.used = l;
    }

    public long used() {
        return this.used;
    }

    public boolean over(double d) {
        return (double)this.used > (double)this.cap() * d;
    }

    public long excess(double d) {
        return Math.max(0L, this.used - (long)((double)this.cap() * d));
    }

    void set(String string) {
        this.chosen.set(string == null ? this.chosen.fallback : this.parse(string));
    }

    void carryOver(String string) {
        if (string != null) {
            LiveSettings.carryOver(this.setting, Float.toString(this.parse(string)));
        }
    }

    private float parse(String string) {
        try {
            return Integer.parseInt(string.trim());
        }
        catch (NumberFormatException numberFormatException) {
            System.out.println("[Viewpoint] " + this.setting + " is not a number of MiB: " + string);
            return this.chosen.fallback;
        }
    }

    static String report() {
        StringBuilder stringBuilder = new StringBuilder(" | memory MiB used/cap:");
        for (Pool pool : ALL) {
            stringBuilder.append(String.format(" %s %.0f/%d,", pool.name, (double)pool.used / 1048576.0, pool.cap() / 0x100000L));
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }
}

