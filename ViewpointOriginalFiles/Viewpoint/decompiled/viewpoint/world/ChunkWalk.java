/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoCell
 */
package viewpoint.world;

import java.util.Arrays;
import viewpoint.core.Frame;
import viewpoint.core.View;
import viewpoint.input.ThirdPerson;
import viewpoint.packs.ModelPacks;
import viewpoint.platform.LiveSettings;
import viewpoint.world.ChunkBudget;
import viewpoint.world.ChunkBuilds;
import viewpoint.world.ChunkCache;
import viewpoint.world.DrawList;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;

final class ChunkWalk {
    private static final double LOOK_SLACK = Math.toRadians(23.0);
    private static final float LEVEL_RADIUS = 7.0f;
    private static final int NEAR_RING = 2;
    private static final LiveSettings.Number AT_ONCE = LiveSettings.number("chunks.atOnceRing", "Rebuilt at once (chunks around)", "World/Chunks", 0.0f, 2.0f, 1.0f, 1.0f);
    private static final int BASEMENT_RING = 3;
    static final int TRANSLUCENT_RING = 6;
    private static final int RAIN_RING = 6;
    private static final float LOOK_AHEAD = 1.5f;
    private static float lastCamX = Float.NaN;
    private static float lastCamY;
    private static float velocityX;
    private static float velocityY;
    private static long lastNanos;
    private final Frame frame;
    private final IsoCell cell;
    private final IsoChunkMap map;
    private final int playerIndex;
    private final int playerLevel;
    private final int lowest;
    private final int pcx;
    private final int pcy;
    private final int radius;
    private final int stride;
    private final long now;
    private final boolean sun;
    private final float forwardX;
    private final float forwardY;
    private final float forwardUp;
    private final float eyeUp;
    private final float aheadX;
    private final float aheadY;
    private final float fogged;
    private final double cone;
    private int waiting;
    private IsoChunk chunk;
    private int cx;
    private int cy;
    private int ring;
    private boolean near;
    private float centreX;
    private float centreY;
    private float d2;
    private float aheadDX;
    private float aheadDY;

    static int walk(Frame frame, IsoCell isoCell, int n, int n2, long l, boolean bl) {
        ChunkWalk.track(frame, l);
        return new ChunkWalk(frame, isoCell, n, n2, l, bl).run();
    }

    static void clear() {
        lastCamX = Float.NaN;
        velocityY = 0.0f;
        velocityX = 0.0f;
    }

    private static void track(Frame frame, long l) {
        float f = (float)(l - lastNanos) * 1.0E-9f;
        if (!Float.isNaN(lastCamX) && f > 0.001f && f < 0.5f) {
            float f2 = (frame.camX - lastCamX) / f;
            float f3 = (frame.camY - lastCamY) / f;
            if (f2 * f2 + f3 * f3 > 2500.0f) {
                f3 = 0.0f;
                f2 = 0.0f;
            }
            float f4 = Math.min(1.0f, f * 4.0f);
            velocityX += (f2 - velocityX) * f4;
            velocityY += (f3 - velocityY) * f4;
        }
        lastCamX = frame.camX;
        lastCamY = frame.camY;
        lastNanos = l;
    }

    private ChunkWalk(Frame frame, IsoCell isoCell, int n, int n2, long l, boolean bl) {
        this.frame = frame;
        this.cell = isoCell;
        this.playerIndex = n;
        this.playerLevel = n2;
        this.now = l;
        this.sun = bl;
        this.map = isoCell.getChunkMap(n);
        this.pcx = Math.floorDiv((int)Math.floor(frame.camX), 8);
        this.pcy = Math.floorDiv((int)Math.floor(frame.camY), 8);
        this.lowest = n2 < 0 ? n2 - 1 : 0;
        float f = (float)Math.cos(frame.viewPitch);
        this.forwardX = (float)Math.cos(frame.viewYaw) * f;
        this.forwardY = (float)Math.sin(frame.viewYaw) * f;
        this.forwardUp = (float)Math.sin(frame.viewPitch);
        this.cone = View.halfDiagonal(ThirdPerson.active) + LOOK_SLACK;
        this.eyeUp = frame.camZ * 2.4494896f + 1.5f;
        this.aheadX = frame.camX + ChunkWalk.clamp(velocityX * 1.5f, -40.0f, 40.0f);
        this.aheadY = frame.camY + ChunkWalk.clamp(velocityY * 1.5f, -40.0f, 40.0f);
        this.fogged = frame.scene.nearEnd() + 8.0f;
        this.radius = View.radiusChunks();
        this.stride = this.radius * 2 + 1;
    }

    private int run() {
        Arrays.fill(this.frame.scene.farMask, 0, this.stride * this.stride, (byte)0);
        this.frame.scene.farMaskX = this.pcx - this.radius;
        this.frame.scene.farMaskY = this.pcy - this.radius;
        this.frame.scene.farMaskSize = this.stride;
        for (int i = -this.radius; i <= this.radius; ++i) {
            for (int j = -this.radius; j <= this.radius; ++j) {
                this.visit(j, i);
            }
        }
        return this.waiting;
    }

    private void visit(int n, int n2) {
        int n3;
        this.cx = this.pcx + n;
        this.cy = this.pcy + n2;
        this.centreX = ((float)this.cx + 0.5f) * 8.0f - this.frame.camX;
        this.centreY = ((float)this.cy + 0.5f) * 8.0f - this.frame.camY;
        this.d2 = this.centreX * this.centreX + this.centreY * this.centreY;
        this.chunk = this.map.getChunkForGridSquare(this.cx * 8, this.cy * 8);
        if (this.chunk == null || !this.chunk.loaded) {
            return;
        }
        if (this.d2 > this.fogged * this.fogged) {
            this.leave(n, n2);
            return;
        }
        ChunkCache.Entry entry = ChunkCache.entryFor(this.chunk);
        float f = entry.leaveStart != 0L ? this.leftShare(entry) : (entry.lastSeenFrame != ChunkCache.frameCounter - 1L ? 0.0f : -1.0f);
        entry.leaveStart = 0L;
        entry.lastSeenFrame = ChunkCache.frameCounter;
        this.ring = Math.max(Math.abs(n), Math.abs(n2));
        this.near = this.ring <= 2;
        this.aheadDX = ((float)this.cx + 0.5f) * 8.0f - this.aheadX;
        this.aheadDY = ((float)this.cy + 0.5f) * 8.0f - this.aheadY;
        int n4 = n3 = this.ring <= 3 ? this.chunk.minLevel : Math.max(this.lowest, this.chunk.minLevel);
        while (n3 <= this.chunk.maxLevel) {
            ChunkCache.Level level = entry.levels[n3 + 32];
            if (level == null) {
                ChunkCache.Level level2 = new ChunkCache.Level();
                entry.levels[n3 + 32] = level2;
                level = level2;
            }
            if (f >= 0.0f && level.data != null) {
                ChunkWalk.dropCrossFade(level);
                level.fadeStart = this.now - (long)(f * 6.0E8f);
                level.fadeNanos = 600000000L;
            }
            float f2 = ((float)n3 + 0.5f) * 2.4494896f - this.eyeUp;
            boolean bl = this.near || this.inView(this.centreX, this.centreY, f2, this.forwardX, this.forwardY, this.forwardUp);
            this.refresh(level, n3, bl, f2);
            float f3 = ChunkCache.fadeOf(level, this.now);
            if (n3 == 0 && level.data != null) {
                float f4 = level.fading != null ? 1.0f : Math.min(f3, 1.0f);
                this.frame.scene.farMask[(n2 + this.radius) * this.stride + n + this.radius] = (byte)Math.round(f4 * 255.0f);
            }
            if (level.data != null || level.fading != null) {
                this.list(level, n3, bl, f2, f3);
            }
            ++n3;
        }
    }

    private void leave(int n, int n2) {
        int n3;
        float f;
        ChunkCache.Entry entry = ChunkCache.existing(this.chunk);
        if (entry == null) {
            return;
        }
        if (entry.leaveStart == 0L) {
            if (entry.lastSeenFrame != ChunkCache.frameCounter - 1L) {
                return;
            }
            ChunkCache.Level level = entry.levels[32];
            float f2 = level == null || level.data == null || level.fading != null ? 1.0f : ChunkCache.fadeOf(level, this.now);
            entry.leaveStart = this.now - (long)((1.0f - f2) * 6.0E8f);
        }
        if ((f = this.leftShare(entry)) <= 0.0f) {
            entry.leaveStart = 0L;
            return;
        }
        entry.lastSeenFrame = ChunkCache.frameCounter;
        this.ring = Math.max(Math.abs(n), Math.abs(n2));
        this.near = this.ring <= 2;
        int n4 = n3 = this.ring <= 3 ? this.chunk.minLevel : Math.max(this.lowest, this.chunk.minLevel);
        while (n3 <= this.chunk.maxLevel) {
            ChunkCache.Level level = entry.levels[n3 + 32];
            if (level != null && level.data != null) {
                boolean bl;
                ChunkWalk.dropCrossFade(level);
                float f3 = ((float)n3 + 0.5f) * 2.4494896f - this.eyeUp;
                boolean bl2 = bl = this.near || this.inView(this.centreX, this.centreY, f3, this.forwardX, this.forwardY, this.forwardUp);
                if (n3 == 0) {
                    this.frame.scene.farMask[(n2 + this.radius) * this.stride + n + this.radius] = (byte)Math.round(f * 255.0f);
                }
                this.list(level, n3, bl, f3, f);
            }
            ++n3;
        }
    }

    private float leftShare(ChunkCache.Entry entry) {
        return Math.max(0.0f, 1.0f - (float)(this.now - entry.leaveStart) / 6.0E8f);
    }

    private static void dropCrossFade(ChunkCache.Level level) {
        ChunkBudget.letGo(level.fading);
        level.fading = null;
        level.fadingModelled = null;
        level.fadingModelledItems = null;
        level.fadingBodies = null;
    }

    private void refresh(ChunkCache.Level level, int n, boolean bl, float f) {
        boolean bl2;
        boolean bl3 = this.ring <= 5 || this.ring == 6 && level.data != null && level.gatheredModelsNear;
        int n2 = ModelPacks.ring();
        boolean bl4 = level.packsNear = n2 >= 0 && (this.ring <= n2 || this.ring == n2 + 1 && level.data != null && level.gatheredPacksNear);
        if (level.data != null && (level.gatheredModelsNear != bl3 || level.gatheredPacksNear != level.packsNear)) {
            if (!level.dirty) {
                level.reason = 3;
            }
            level.dirty = true;
            level.retryFrame = 0L;
        }
        boolean bl5 = level.dirty && ChunkCache.frameCounter >= level.retryFrame;
        boolean bl6 = bl2 = this.ring <= AT_ONCE.getInt() && (level.data != null || n == this.playerLevel);
        if (bl5 && bl2) {
            ChunkBuilds.rebuild(level, this.chunk, n, this.playerIndex, bl3, this.cell, this.now, 150000000L, true);
        } else if (bl5 && (bl || !ChunkBudget.tight)) {
            ChunkBuilds.queue(level, this.chunk, n, bl3, (bl ? 0.0f : 1.0E7f) + this.aheadDX * this.aheadDX + this.aheadDY * this.aheadDY + f * f);
            if (bl) {
                ++this.waiting;
            }
        }
    }

    private void list(ChunkCache.Level level, int n, boolean bl, float f, float f2) {
        byte by = (byte)((bl ? 1 : 0) | (bl && this.ring <= 6 ? 4 : 0) | (this.ring <= 6 ? 64 : 0));
        if (bl) {
            this.models(level);
        }
        if (level.bodies != null) {
            level.bodies.list(level.fading != null ? 1.0f : Math.min(f2, 1.0f), level.fading == null || level.fadingBodies != null);
        }
        float f3 = -((float)(this.cx * 8) - this.frame.camX);
        float f4 = -this.frame.camZ * 2.4494896f;
        float f5 = -((float)(this.cy * 8) - this.frame.camY);
        float f6 = this.d2 + f * f;
        if (!bl) {
            ChunkBudget.outOfView(level, f6);
        }
        if (level.data != null) {
            DrawList.candidate(level.data, f3, f4, f5, by, f6, this.sun, this.centreX, this.centreY, n, this.eyeUp, 0.0f, f2 >= 1.0f ? 2.0f : f2, this.cx, this.cy, level.bodies);
        }
        if (level.fading != null) {
            DrawList.candidate(level.fading, f3, f4, f5, by, f6, this.sun, this.centreX, this.centreY, n, this.eyeUp, f2, 2.0f, this.cx, this.cy, level.fadingBodies);
        }
    }

    private void models(ChunkCache.Level level) {
        if (level.modelsNear && level.modelled != null) {
            this.frame.modelObjects.addAll(level.modelled);
        }
        if (level.modelsNear && level.modelledItems != null) {
            this.frame.modelItems.addAll(level.modelledItems);
        }
        if (level.fading != null && !level.modelsNear && level.fadingModelled != null) {
            this.frame.modelObjects.addAll(level.fadingModelled);
        }
        if (level.fading != null && !level.modelsNear && level.fadingModelledItems != null) {
            this.frame.modelItems.addAll(level.fadingModelledItems);
        }
    }

    private boolean inView(float f, float f2, float f3, float f4, float f5, float f6) {
        float f7;
        if ((f7 = (float)Math.sqrt((f -= ThirdPerson.offsetX) * f + (f2 -= ThirdPerson.offsetY) * f2 + (f3 -= ThirdPerson.offsetUp) * f3)) <= 7.0f) {
            return true;
        }
        double d = Math.max(-1.0, Math.min(1.0, (double)((f * f4 + f2 * f5 + f3 * f6) / f7)));
        return Math.acos(d) <= this.cone + Math.asin(7.0f / f7);
    }

    private static float clamp(float f, float f2, float f3) {
        return f < f2 ? f2 : Math.min(f, f3);
    }
}

