/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.skinnedmodel.model.ModelSlotRenderData
 */
package viewpoint.models;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import viewpoint.models.KeptSnapshots;
import viewpoint.models.ModelCapture;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.Profile;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;

public final class CharacterRate {
    private static final float NEAR = 8.0f;
    private static final float[] REACH = new float[]{16.0f, 32.0f, 64.0f};
    private static final int[] INTERVAL = new int[]{2, 3, 4, 6};
    private static final LiveSettings.Number FULL = LiveSettings.number("lod.fullRateCharacters", "Characters animated every frame, the nearest", "World/Levels of detail", 0.0f, 256.0f, 8.0f, 32.0f);
    private static final LiveSettings.Toggle POSED_KEPT = LiveSettings.toggle("lod.keptSnapshotsPosed", "Zombies drawn in each new pose without a fresh snapshot", "World/Levels of detail", true);
    private static final Map<IsoGameCharacter, Entry> entries = new IdentityHashMap<IsoGameCharacter, Entry>();
    private static long frame;
    private static float fullReach;
    private static float[] distances;
    private static int distanceCount;
    private static AnimationPlayer skipped;
    private static int fresh;
    private static int again;
    private static int posedAgain;

    static void begin() {
        ++frame;
        distanceCount = 0;
        posedAgain = 0;
        again = 0;
        fresh = 0;
    }

    static void drawn(ModelCapture.Pose pose) {
        fresh += pose == ModelCapture.Pose.SNAPSHOT ? 1 : 0;
        again += pose == ModelCapture.Pose.LAST ? 1 : 0;
        posedAgain += pose == ModelCapture.Pose.PLAYER ? 1 : 0;
    }

    static ModelCapture.Pose pose(IsoGameCharacter isoGameCharacter2, ModelManager.ModelSlot modelSlot, float f, boolean bl) {
        int n;
        boolean bl2;
        Entry entry = entries.computeIfAbsent(isoGameCharacter2, isoGameCharacter -> new Entry());
        entry.seen = frame;
        boolean bl3 = bl2 = isoGameCharacter2 instanceof IsoZombie && !bl && !isoGameCharacter2.isSeatedInVehicle() && !isoGameCharacter2.isRagdollSimulationActive() && !isoGameCharacter2.isOutlineHighlight(0);
        if (bl2) {
            distances = distanceCount < distances.length ? distances : Arrays.copyOf(distances, distanceCount * 2);
            CharacterRate.distances[CharacterRate.distanceCount++] = f;
        }
        int n2 = n = bl2 && f > 8.0f && f > fullReach ? CharacterRate.interval(f) : 1;
        if (entry.interval == 1 && n > 1) {
            entry.posedAt = frame - (long)((System.identityHashCode((Object)isoGameCharacter2) & 0xFF) % n);
        }
        entry.interval = n;
        if (entry.kept == null || entry.kept.modelSlot != modelSlot || isoGameCharacter2.getTextureCreator() != null) {
            return ModelCapture.Pose.SNAPSHOT;
        }
        if (!entry.posed && n > 1) {
            return ModelCapture.Pose.LAST;
        }
        if (!(bl2 && POSED_KEPT.get() && ModelCapture.posable(entry.kept, modelSlot))) {
            return ModelCapture.Pose.SNAPSHOT;
        }
        entry.posed = false;
        return ModelCapture.Pose.PLAYER;
    }

    static ModelSlotRenderData kept(IsoGameCharacter isoGameCharacter) {
        return CharacterRate.entries.get((Object)((Object)isoGameCharacter)).kept;
    }

    static void took(IsoGameCharacter isoGameCharacter, ModelSlotRenderData modelSlotRenderData) {
        Entry entry = entries.get((Object)isoGameCharacter);
        if (entry.kept != null) {
            KeptSnapshots.replace(entry.kept);
        }
        entry.kept = modelSlotRenderData;
        entry.lift = modelSlotRenderData.z - isoGameCharacter.getZ();
        entry.posed = false;
        KeptSnapshots.keep(modelSlotRenderData);
    }

    static float lift(IsoGameCharacter isoGameCharacter) {
        return CharacterRate.entries.get((Object)((Object)isoGameCharacter)).lift;
    }

    static void end() {
        Profile.snapshots(fresh, again, posedAgain);
        int n = FULL.getInt();
        if (distanceCount > n) {
            Arrays.sort(distances, 0, distanceCount);
            fullReach = n == 0 ? 0.0f : distances[n - 1];
        } else {
            fullReach = Float.MAX_VALUE;
        }
        Iterator<Entry> iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (entry.seen == frame) continue;
            if (entry.kept != null) {
                KeptSnapshots.replace(entry.kept);
            }
            iterator.remove();
        }
    }

    public static void clear() {
        for (Entry entry : entries.values()) {
            if (entry.kept == null) continue;
            KeptSnapshots.replace(entry.kept);
        }
        entries.clear();
        fullReach = Float.MAX_VALUE;
    }

    public static void updating(ModelManager.ModelSlot modelSlot) {
        Entry entry;
        Entry entry2 = entry = modelSlot.character == null ? null : entries.get((Object)modelSlot.character);
        if (entry == null) {
            return;
        }
        long l = frame + 1L;
        if (entry.interval == 1 || l - entry.posedAt >= (long)entry.interval) {
            entry.posedAt = l;
            entry.posed = true;
            return;
        }
        AnimationPlayer animationPlayer = modelSlot.character.getAnimationPlayer();
        if (animationPlayer != null && animationPlayer.updateBones && !animationPlayer.isRagdolling() && !animationPlayer.getMultiTrack().containsAnyRagdollTracks()) {
            animationPlayer.updateBones = false;
            skipped = animationPlayer;
        }
    }

    public static void updated() {
        if (skipped != null) {
            CharacterRate.skipped.updateBones = true;
            skipped = null;
        }
    }

    private static int interval(float f) {
        int n;
        for (n = 0; n < REACH.length && f > REACH[n]; ++n) {
        }
        return INTERVAL[n];
    }

    private CharacterRate() {
    }

    static {
        fullReach = Float.MAX_VALUE;
        distances = new float[256];
    }

    private static final class Entry {
        int interval = 1;
        long posedAt;
        boolean posed;
        long seen;
        ModelSlotRenderData kept;
        float lift;

        private Entry() {
        }
    }
}

