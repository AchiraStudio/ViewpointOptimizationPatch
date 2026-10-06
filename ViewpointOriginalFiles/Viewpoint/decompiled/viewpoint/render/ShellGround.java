/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  imgui.ImGui
 */
package viewpoint.render;

import imgui.ImGui;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.Pool;
import viewpoint.platform.Profile;
import viewpoint.render.FarGpu;
import viewpoint.render.FloorBaker;
import viewpoint.render.FloorFilter;
import viewpoint.render.FloorSlices;
import viewpoint.render.GroundPage;
import viewpoint.render.SceneData;

public final class ShellGround {
    private static final String LODS = "World/Levels of detail";
    private static final String WORK = "World/Far world";
    public static final LiveSettings.Toggle BAKED = LiveSettings.toggle("lod.groundBaked", "Shell ground baked", "World/Levels of detail", true);
    public static final LiveSettings.Number TEXELS_FULL = LiveSettings.number("lod.groundTexelsFull", "Full shell ground, texels per tile", "World/Levels of detail", 1.0f, 16.0f, 1.0f, 8.0f);
    public static final LiveSettings.Number TEXELS_LITE = LiveSettings.number("lod.groundTexelsLite", "Light shell ground, texels per tile (0: none)", "World/Levels of detail", 0.0f, 8.0f, 1.0f, 2.0f);
    private static final LiveSettings.Number PAGES = LiveSettings.number("far.groundBakesPerFrame", "Ground pages baked a frame", "World/Far world", 1.0f, 16.0f, 1.0f, 2.0f);
    private static final LiveSettings.Number BUDGET = LiveSettings.number("far.groundBakeBudgetMs", "Ground bake budget (ms)", "World/Far world", 0.25f, 10.0f, 0.25f, 1.0f);
    private static final int KEEP_FRAMES = 60;
    private static final byte MASK_LITE = 1;
    private static final long ARRAY_BYTES = 0x1000000L;
    private static final double RECENT = 0.02;
    private final FloorBaker baker = new FloorBaker();
    private FloorSlices full;
    private FloorSlices lite;
    private final IdentityHashMap<GroundPage, Slot> slots = new IdentityHashMap();
    private final ArrayList<Slot> waiting = new ArrayList();
    private long frame;
    private int bakeFilter = -1;
    private int blocksFull;
    private int blocksLite;
    private int bakedFull;
    private int bakedLite;
    private double recentBakes;
    private double recentBakeMs;

    public static long pageBytes(int n) {
        return n <= 0 ? 0L : new FloorSlices(66 * n, ShellGround.mips(n)).sliceBytes();
    }

    void update(SceneData sceneData) {
        ++this.frame;
        this.tiers();
        this.see(sceneData);
        long l = System.nanoTime();
        int n = this.bake(l + (long)(BUDGET.get() * 1000000.0f));
        this.recentBakes += ((double)n - this.recentBakes) * 0.02;
        this.recentBakeMs += ((double)(System.nanoTime() - l) * 1.0E-6 - this.recentBakeMs) * 0.02;
        this.give(sceneData);
        this.forget();
    }

    private void tiers() {
        boolean bl = this.bakeFilter != FloorFilter.bakeFilter();
        this.bakeFilter = FloorFilter.bakeFilter();
        this.full = this.tier(this.full, TEXELS_FULL.getInt(), false, bl);
        this.lite = this.tier(this.lite, TEXELS_LITE.getInt(), true, bl);
    }

    private FloorSlices tier(FloorSlices floorSlices, int n, boolean bl, boolean bl2) {
        int n2 = 66 * n;
        if (floorSlices != null && floorSlices.size == n2 && !bl2) {
            return floorSlices;
        }
        if (floorSlices != null) {
            floorSlices.clear();
            for (Slot slot : this.slots.values()) {
                slot.slice = slot.lite == bl ? -1 : slot.slice;
            }
        }
        if (n <= 0) {
            return null;
        }
        return new FloorSlices(n2, ShellGround.mips(n), (int)Math.max(1L, 0x1000000L / ShellGround.pageBytes(n)));
    }

    private void see(SceneData sceneData) {
        this.waiting.clear();
        this.blocksLite = 0;
        this.blocksFull = 0;
        for (int i = 0; i < sceneData.shellCount; ++i) {
            FarGpu.BlockDraw blockDraw = sceneData.shells[i];
            if (blockDraw.lite) {
                ++this.blocksLite;
            } else {
                ++this.blocksFull;
            }
            if (blockDraw.ground == null) continue;
            Slot slot = this.slots.computeIfAbsent(blockDraw.ground, Slot::new);
            slot.seen = this.frame;
            slot.lite = blockDraw.lite;
            slot.distance = blockDraw.distance;
            if (slot.slice >= 0) continue;
            this.waiting.add(slot);
        }
    }

    private int bake(long l) {
        this.waiting.sort((slot, slot2) -> Float.compare(slot.distance, slot2.distance));
        int n = 0;
        for (Slot slot3 : this.waiting) {
            FloorSlices floorSlices;
            FloorSlices floorSlices2 = floorSlices = slot3.lite ? this.lite : this.full;
            if (n >= PAGES.getInt() || n > 0 && System.nanoTime() > l) break;
            if (floorSlices == null) continue;
            slot3.slice = floorSlices.take(Long.MAX_VALUE);
            this.baker.bake(slot3.page, floorSlices, slot3.slice);
            ++n;
        }
        return n;
    }

    private void give(SceneData sceneData) {
        this.bakedLite = 0;
        this.bakedFull = 0;
        for (int i = 0; i < sceneData.shellCount; ++i) {
            Slot slot;
            FarGpu.BlockDraw blockDraw = sceneData.shells[i];
            Slot slot2 = slot = blockDraw.ground == null ? null : this.slots.get(blockDraw.ground);
            FloorSlices floorSlices = slot == null ? null : (slot.lite ? this.lite : this.full);
            blockDraw.groundArray = null;
            if (slot == null) continue;
            if (slot.slice < 0 || floorSlices == null) {
                if (blockDraw.maskIndex < 0) continue;
                sceneData.shellMask[blockDraw.maskIndex * 3] = 1;
                continue;
            }
            blockDraw.groundArray = floorSlices.array(slot.slice);
            blockDraw.groundTexture = floorSlices.texture(slot.slice);
            blockDraw.groundLayer = FloorSlices.layer(slot.slice);
            this.bakedFull += slot.lite ? 0 : 1;
            this.bakedLite += slot.lite ? 1 : 0;
        }
    }

    private void forget() {
        Iterator<Slot> iterator = this.slots.values().iterator();
        while (iterator.hasNext()) {
            FloorSlices floorSlices;
            Slot slot = iterator.next();
            if (this.frame - slot.seen <= 60L) continue;
            FloorSlices floorSlices2 = floorSlices = slot.lite ? this.lite : this.full;
            if (floorSlices != null) {
                floorSlices.free(slot.slice);
            }
            iterator.remove();
        }
    }

    private static int mips(int n) {
        return 32 - Integer.numberOfLeadingZeros(66 * n);
    }

    String report() {
        return String.format("blocks drawn full %d lite %d, from pages %d + %d, pages held %d, %.0f MiB, baked/frame %.2f, %.2f ms", this.blocksFull, this.blocksLite, this.bakedFull, this.bakedLite, this.slots.size(), (double)this.madeBytes() / 1048576.0, this.recentBakes, this.recentBakeMs);
    }

    private long madeBytes() {
        return (this.full == null ? 0L : this.full.madeBytes()) + (this.lite == null ? 0L : this.lite.madeBytes());
    }

    void drawFigures() {
        ImGui.separator();
        ImGui.textUnformatted((String)String.format("Blocks drawn: full %d (%d from pages), lite %d (%d from pages)", this.blocksFull, this.bakedFull, this.blocksLite, this.bakedLite));
        ImGui.textUnformatted((String)String.format("Shell: %.2fM vertices, pool %d of %d MiB, %d blocks waiting to upload", (double)Profile.shellVertices / 1000000.0, Pool.SHELL.used() >> 20, Pool.SHELL.cap() >> 20, Profile.shellUploads));
        ImGui.textUnformatted((String)String.format("Ground pages: %d held, %.0f MiB made, %d waiting", this.slots.size(), (double)this.madeBytes() / 1048576.0, this.waiting.size()));
        ImGui.textUnformatted((String)String.format("Ground bakes: %.2f a frame, %.2f ms a frame", this.recentBakes, this.recentBakeMs));
        ImGui.textUnformatted((String)String.format("GPU ms: far %.2f, g-buffer %.2f, shadow %.2f", Profile.recentGpuMs(5), Profile.recentGpuMs(3), Profile.recentGpuMs(2)));
        ImGui.textUnformatted((String)String.format("CPU ms: far pass %.2f, floors and ground bake %.2f, far world (main) %.2f", Profile.recentCpuMs(5), Profile.recentCpuMs(1), Profile.recentFarMainMs));
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip((String)"Switch \"Shell ground baked\" and compare: the G-buffer holds the shell's band, the shadow maps the far shadow map; the ground bake runs in the floors pass.");
        }
    }

    private static final class Slot {
        final GroundPage page;
        boolean lite;
        int slice = -1;
        long seen;
        float distance;

        Slot(GroundPage groundPage) {
            this.page = groundPage;
        }
    }
}

