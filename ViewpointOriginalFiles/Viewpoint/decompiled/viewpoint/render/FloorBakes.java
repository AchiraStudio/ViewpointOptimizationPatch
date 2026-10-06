/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import java.util.ArrayList;
import java.util.Locale;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.LongMap;
import viewpoint.platform.Pool;
import viewpoint.render.FloorBaker;
import viewpoint.render.FloorFilter;
import viewpoint.render.FloorPage;
import viewpoint.render.FloorSlices;
import viewpoint.render.SceneData;

final class FloorBakes {
    private static final String SECTION = "World/Floors";
    private static final LiveSettings.Number PAGES = LiveSettings.number("floors.bakePagesPerFrame", "Near pages baked a frame", "World/Floors", 1.0f, 16.0f, 1.0f, 2.0f);
    private static final LiveSettings.Number BUDGET = LiveSettings.number("floors.bakeBudgetMs", "Bake budget (ms)", "World/Floors", 0.5f, 20.0f, 0.5f, 2.0f);
    private static final LiveSettings.Number TEXELS = LiveSettings.number("floors.texelsPerTile", "Near texels per tile", "World/Floors", 32.0f, 128.0f, 16.0f, 48.0f);
    private static final LiveSettings.Number RING = LiveSettings.number("floors.detailChunks", "Near floors within (chunks)", "World/Floors", 1.0f, 16.0f, 1.0f, 8.0f);
    private static final LiveSettings.Number LOW_PAGES = LiveSettings.number("floors.lowPagesPerFrame", "Distant pages baked a frame", "World/Floors", 1.0f, 64.0f, 1.0f, 4.0f);
    private static final int LOW_TEXELS = 16;
    private static final int MIPS = 5;
    private static final long FORGET_FRAMES = 600L;
    private static final long APPEAR_NANOS = 600000000L;
    private static final float SHARE_SCALE = 0.4f;
    private final FloorBaker baker = new FloorBaker();
    private final LongMap<Slot> slots = new LongMap(256);
    private final ArrayList<Slot> inView = new ArrayList();
    private final ArrayList<Slot> waiting = new ArrayList();
    private FloorSlices detail;
    private FloorSlices low;
    private long frame;
    private int[] meshSlice = new int[512];
    private float[] meshShare = new float[512];
    static final int LOW = 0x100000;
    private int bakeFilter = -1;
    private Slot nearest;
    private int waitingLow;
    private int waitingDetail;
    private int bakedDetail;
    private int bakedLow;
    private int frames;
    private double bakeMs;

    FloorBakes() {
    }

    void update(SceneData sceneData) {
        ++this.frame;
        ++this.frames;
        this.tiers();
        this.see(sceneData);
        long l = System.nanoTime();
        long l2 = l + (long)((double)BUDGET.get() * 1000000.0);
        this.bakeLow(l2);
        this.bakeDetail(l2);
        this.bakeMs += (double)(System.nanoTime() - l) * 1.0E-6;
        if (this.frame % 60L == 0L) {
            this.forget();
        }
        this.slices(sceneData);
        Pool.FLOORS.use(this.detail.madeBytes() + this.low.madeBytes());
    }

    private void tiers() {
        int n = TEXELS.getInt() * 10;
        boolean bl = this.bakeFilter != FloorFilter.bakeFilter();
        this.bakeFilter = FloorFilter.bakeFilter();
        if (this.low != null && bl) {
            this.low.clear();
            this.low = null;
            for (Slot slot : this.slots.values()) {
                slot.low = -1;
                slot.lowPage = null;
            }
        }
        if (this.low == null) {
            this.low = new FloorSlices(160, 5);
        }
        if (this.detail == null || this.detail.size != n || bl) {
            if (this.detail != null) {
                this.detail.clear();
                this.baker.clearScratch();
                for (int i = 0; i < this.slots.capacity(); ++i) {
                    Slot slot;
                    slot = this.slots.valueAt(i);
                    if (slot == null) continue;
                    slot.detail = -1;
                    slot.detailPage = null;
                }
            }
            this.detail = new FloorSlices(n, 5);
        }
    }

    private void see(SceneData sceneData) {
        this.inView.clear();
        this.nearest = null;
        for (int i = 0; i < sceneData.meshCount; ++i) {
            FloorPage floorPage = sceneData.meshFloor[i];
            if (floorPage == null || (sceneData.meshFlags[i] & 5) == 0) continue;
            Slot slot = this.slots.get(floorPage.key());
            if (slot == null) {
                slot = new Slot();
                this.slots.put(floorPage.key(), slot);
            }
            slot.page = floorPage;
            if (slot.seen == this.frame) continue;
            slot.seen = this.frame;
            float f = sceneData.meshOffsets[i * 3] - 4.0f;
            float f2 = sceneData.meshOffsets[i * 3 + 2] - 4.0f;
            slot.distance = Math.max(Math.abs(f), Math.abs(f2)) / 8.0f;
            this.inView.add(slot);
            this.nearest = this.nearest == null || slot.distance < this.nearest.distance ? slot : this.nearest;
        }
    }

    private void bakeLow(long l) {
        this.waiting.clear();
        for (Slot slot3 : this.inView) {
            if (slot3.low >= 0 && slot3.lowPage == slot3.page) continue;
            this.waiting.add(slot3);
        }
        this.waiting.sort((slot, slot2) -> slot.low < 0 != slot2.low < 0 ? (slot.low < 0 ? -1 : 1) : Float.compare(slot.distance, slot2.distance));
        this.waitingLow = this.waiting.size();
        int n = LOW_PAGES.getInt();
        for (int i = 0; i < this.waiting.size() && i < n && (i == 0 || System.nanoTime() < l); ++i) {
            Slot slot4 = this.waiting.get(i);
            if (slot4.low < 0 && (slot4.low = this.take(this.low, FloorBakes.lowBytes(), slot4, false)) < 0) break;
            slot4.lowPage = slot4.page;
            this.baker.bake(slot4.lowPage, this.low, slot4.low);
            ++this.bakedLow;
        }
    }

    private void bakeDetail(long l) {
        this.waiting.clear();
        float f = RING.get();
        for (Slot slot3 : this.inView) {
            if (!(slot3.distance <= f) || slot3.detail >= 0 && slot3.detailPage == slot3.page) continue;
            this.waiting.add(slot3);
        }
        this.waiting.sort((slot, slot2) -> Float.compare(slot.distance, slot2.distance));
        this.waitingDetail = this.waiting.size();
        int n = PAGES.getInt();
        for (int i = 0; i < this.waiting.size() && i < n && System.nanoTime() < l; ++i) {
            Slot slot4 = this.waiting.get(i);
            if (slot4.detail < 0 && (slot4.detail = this.take(this.detail, FloorBakes.detailBytes(), slot4, true)) < 0) break;
            slot4.detailPage = slot4.page;
            this.baker.bake(slot4.detailPage, this.detail, slot4.detail);
            ++this.bakedDetail;
        }
    }

    private int take(FloorSlices floorSlices, long l, Slot slot, boolean bl) {
        int n = floorSlices.take(l);
        if (n >= 0) {
            return n;
        }
        Slot slot2 = FloorBakes.giver(this.slots, slot, bl, this.frame);
        if (slot2 == null) {
            return -1;
        }
        if (bl) {
            n = slot2.detail;
            slot2.detail = -1;
            slot2.detailPage = null;
        } else {
            n = slot2.low;
            slot2.low = -1;
            slot2.lowPage = null;
        }
        return n;
    }

    static Slot giver(LongMap<Slot> longMap, Slot slot, boolean bl, long l) {
        Slot slot2 = null;
        for (int i = 0; i < longMap.capacity(); ++i) {
            boolean bl2;
            int n;
            Slot slot3 = longMap.valueAt(i);
            int n2 = slot3 == null ? -1 : (n = bl ? slot3.detail : slot3.low);
            if (n < 0 || slot3 == slot) continue;
            boolean bl3 = bl2 = slot2 == null || slot3.seen < slot2.seen || slot3.seen == slot2.seen && slot3.distance > slot2.distance;
            if (!bl2 || slot3.seen >= l && !(slot3.distance > slot.distance)) continue;
            slot2 = slot3;
        }
        return slot2;
    }

    private void forget() {
        for (int i = 0; i < this.slots.capacity(); ++i) {
            Slot slot = this.slots.valueAt(i);
            if (slot == null || this.frame - slot.seen <= 600L) continue;
            this.detail.free(slot.detail);
            this.low.free(slot.low);
            this.slots.removeAt(i);
        }
    }

    private void slices(SceneData sceneData) {
        if (this.meshSlice.length < sceneData.meshCount) {
            this.meshSlice = new int[sceneData.meshes.length];
            this.meshShare = new float[sceneData.meshes.length];
        }
        long l = System.nanoTime();
        for (int i = 0; i < sceneData.meshCount; ++i) {
            Slot slot;
            FloorPage floorPage = sceneData.meshFloor[i];
            Slot slot2 = slot = floorPage == null ? null : this.slots.get(floorPage.key());
            this.meshSlice[i] = slot == null ? -1 : (slot.detail >= 0 ? slot.detail : (slot.low >= 0 ? slot.low | 0x100000 : -1));
            this.meshShare[i] = FloorBakes.share(slot, this.meshSlice[i], l);
        }
    }

    int meshSlice(int n) {
        return this.meshSlice[n];
    }

    float meshShare(int n) {
        return this.meshShare[n];
    }

    private static float share(Slot slot, int n, long l) {
        if (slot == null || n < 0) {
            if (slot != null) {
                slot.appeared = 0L;
            }
            return 1.0f;
        }
        if (slot.appeared == 0L) {
            slot.appeared = l;
        }
        return Math.min(1.0f, (float)(l - slot.appeared) / 6.0E8f);
    }

    int texture(int n) {
        return (n & 0x100000) != 0 ? this.low.texture(n & 0xFFEFFFFF) : this.detail.texture(n);
    }

    Object array(int n) {
        return (n & 0x100000) != 0 ? this.low.array(n & 0xFFEFFFFF) : this.detail.array(n);
    }

    static int layer(int n) {
        return FloorSlices.layer(n & 0xFFEFFFFF);
    }

    static float recordLayer(int n, float f) {
        return ((n & 0x100000) != 0 ? -1.0f - (float)FloorBakes.layer(n) : (float)FloorBakes.layer(n)) + 0.4f * (1.0f - f);
    }

    int tierSize(int n) {
        return (n & 0x100000) != 0 ? this.low.size : this.detail.size;
    }

    FloorPage nearestPage() {
        return this.nearest == null ? null : this.nearest.page;
    }

    int nearestSlice() {
        return this.nearest == null ? -1 : (this.nearest.detail >= 0 ? this.nearest.detail : (this.nearest.low >= 0 ? this.nearest.low | 0x100000 : -1));
    }

    String figures() {
        return this.detail == null ? "No floor in view yet." : String.format("Levels in view with a floor: %d. Slices: near %d, distant %d (%d MiB of %d). Waiting: near %d, distant %d.", this.inView.size(), this.detail.inUse(), this.low.inUse(), this.detail.madeBytes() + this.low.madeBytes() >> 20, Pool.FLOORS.cap() >> 20, this.waitingDetail, this.waitingLow);
    }

    private static long lowBytes() {
        return Pool.FLOORS.cap() / 4L;
    }

    private static long detailBytes() {
        return Pool.FLOORS.cap() - FloorBakes.lowBytes();
    }

    String report() {
        String string = this.frames == 0 || this.detail == null ? "none" : String.format(Locale.ROOT, "baked/frame near %.2f far %.2f, %.2f ms; slices near %d far %d", (double)this.bakedDetail / (double)this.frames, (double)this.bakedLow / (double)this.frames, this.bakeMs / (double)this.frames, this.detail.inUse(), this.low.inUse());
        this.frames = 0;
        this.bakedLow = 0;
        this.bakedDetail = 0;
        this.bakeMs = 0.0;
        return string;
    }

    static final class Slot {
        FloorPage page;
        FloorPage lowPage;
        FloorPage detailPage;
        int low = -1;
        int detail = -1;
        long seen;
        float distance;
        long appeared;

        Slot() {
        }
    }
}

