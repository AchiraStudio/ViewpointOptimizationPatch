/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.objects.IsoDeadBody
 */
package viewpoint.models;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import viewpoint.core.Frame;
import viewpoint.models.CorpseCapture;
import viewpoint.models.CorpsePicks;
import viewpoint.models.CorpsePose;
import viewpoint.models.CorpseView;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.Pool;
import viewpoint.platform.Profile;
import zombie.core.skinnedmodel.ModelManager;
import zombie.iso.objects.IsoDeadBody;

public final class Corpses {
    private static final String SECTION = "Debug/Corpses";
    private static final LiveSettings.Toggle MODELS = LiveSettings.toggle("corpses.models", "Corpses as 3D models", "Debug/Corpses", true);
    private static final LiveSettings.Number LIMIT = LiveSettings.number("corpses.limit", "3D corpses' distance (squares ahead, at most 40)", "Debug/Corpses", 2.0f, 40.0f, 1.0f, 40.0f);
    private static final LiveSettings.Number BAND = LiveSettings.number("corpses.band", "Fade band (squares)", "Debug/Corpses", 0.0f, 20.0f, 1.0f, 6.0f);
    private static final LiveSettings.Number MOST = LiveSettings.number("corpses.most", "Most 3D corpses", "Debug/Corpses", 0.0f, 512.0f, 1.0f, 256.0f);
    private static final LiveSettings.Toggle EVERY = LiveSettings.toggle("corpses.every", "Every corpse within reach in 3D (no count limit)", "Debug/Corpses", false);
    private static final LiveSettings.Toggle IN_VIEW = LiveSettings.toggle("corpses.inView", "3D corpses only in the cone ahead", "Debug/Corpses", true);
    private static final LiveSettings.Number RANDOMNESS = LiveSettings.number("corpses.randomness", "Pick's randomness (0: the nearest first)", "Debug/Corpses", 0.0f, 2.0f, 0.05f, 0.5f);
    private static final LiveSettings.Number SWAP_SECONDS = LiveSettings.number("corpses.swapSeconds", "Swap dissolve (seconds)", "Debug/Corpses", 0.05f, 3.0f, 0.05f, 0.6f);
    private static final LiveSettings.Number POSES = LiveSettings.number("corpses.posesPerFrame", "Corpses posed a frame", "Debug/Corpses", 1.0f, 64.0f, 1.0f, 8.0f);
    private static final LiveSettings.Toggle SHADOWS = LiveSettings.toggle("corpses.shadows", "Corpses cast shadows", "Debug/Corpses", true);
    private static final LiveSettings.Toggle TINT = LiveSettings.toggle("corpses.tint", "Tint by form: models green, dissolving yellow, cards untinted", "Debug/Corpses", false);
    private static final int FRESH_FRAMES = 60;
    private static final float MAX_FRAME_SECONDS = 0.1f;
    private static final int RESTING_FRAMES = 1800;
    private static final IdentityHashMap<IsoDeadBody, Corpse> known;
    private static final LinkedHashMap<IsoDeadBody, Corpse> resting;
    private static final ArrayList<Corpse> listed;
    private static final CorpsePicks.Bodies bodies;
    private static long frameNumber;
    private static long lastNanos;
    private static long restingBytes;
    private static long poseBytes;
    private static float camX;
    private static float camY;
    private static float camZ;
    private static float limit;
    private static boolean culling;
    private static volatile int[] counts;
    private static volatile long pickNanos;

    public static void begin(Frame frame) {
        for (Corpse corpse : listed) {
            corpse.listed = false;
        }
        listed.clear();
        frameNumber = frame.number;
        camX = frame.camX;
        camY = frame.camY;
        camZ = frame.camZ;
        limit = LIMIT.get();
        culling = IN_VIEW.get();
        CorpseView.begin(frame);
    }

    public static void listed(IsoDeadBody isoDeadBody, Object object, float f, boolean bl) {
        float f2;
        float f3;
        float f4;
        float f5;
        float f6 = isoDeadBody.getX();
        float f7 = f6 - camX;
        float f8 = (float)Math.sqrt(f7 * f7 + (f5 = (f4 = isoDeadBody.getY()) - camY) * f5 + (f3 = ((f2 = isoDeadBody.getZ()) - camZ) * 2.4494896f) * f3);
        if (f8 >= limit || culling && !CorpseView.sees(f6, f4, f2)) {
            return;
        }
        Corpse corpse = known.get(isoDeadBody);
        if (corpse == null) {
            corpse = Corpses.back(isoDeadBody);
            known.put(isoDeadBody, corpse);
        } else if (corpse.listed) {
            return;
        }
        corpse.listed = true;
        corpse.distance = f8;
        corpse.look = object;
        corpse.coverage = f;
        corpse.eligible = bl && isoDeadBody.getDoRender();
        listed.add(corpse);
    }

    public static void pick(Frame frame) {
        long l = System.nanoTime();
        float f = lastNanos == 0L ? 0.0f : Math.min(0.1f, (float)(l - lastNanos) * 1.0E-9f);
        lastNanos = l;
        frameNumber = frame.number;
        Corpses.forgetUnlisted();
        if (!MODELS.get()) {
            Corpses.letGoAll();
        } else {
            Corpses.looks();
            Corpses.choose(f);
            Corpses.poses();
            Corpses.makeRoom();
        }
        Corpses.report(System.nanoTime() - l);
    }

    public static float share(IsoDeadBody isoDeadBody) {
        Corpse corpse = known.get(isoDeadBody);
        return corpse == null || !corpse.listed ? 0.0f : corpse.share;
    }

    public static void capture(Frame frame) {
        int n = 0x21 | (SHADOWS.get() ? 2 : 0);
        boolean bl = TINT.get();
        for (Corpse corpse : listed) {
            CorpsePose corpsePose = corpse.pose;
            if (corpsePose != null && corpsePose.built() && !corpsePose.drawable()) {
                frame.scene.models.look(corpsePose.textures, corpse.distance);
                corpsePose.hold(frame);
            }
            CorpsePose corpsePose2 = corpsePose != null && corpsePose.drawable() ? corpsePose : corpse.previous;
            float f = Math.min(corpse.share, corpse.coverage);
            if (corpsePose2 == null || !(f > 0.0f)) continue;
            CorpseCapture.draw(frame, corpse.body, corpsePose2, f, n, bl);
            corpsePose2.hold(frame);
            corpsePose2.lastUsed = frameNumber;
        }
    }

    public static void release(Frame frame) {
        for (int i = 0; i < frame.corpseInstances.size(); ++i) {
            ModelManager.instance.derefModelInstance(frame.corpseInstances.get(i));
        }
        frame.corpseInstances.clear();
    }

    public static void forget() {
        Corpses.letGoAll();
        known.clear();
        listed.clear();
        lastNanos = 0L;
    }

    private static void forgetUnlisted() {
        Corpse corpse;
        Iterator<Corpse> iterator = known.values().iterator();
        while (iterator.hasNext()) {
            corpse = iterator.next();
            if (corpse.listed) continue;
            iterator.remove();
            Corpses.rest(corpse);
        }
        iterator = resting.values().iterator();
        while (iterator.hasNext()) {
            corpse = iterator.next();
            if (frameNumber - corpse.restedAt <= 1800L) break;
            iterator.remove();
            restingBytes -= corpse.restBytes;
            Corpses.letGo(corpse);
        }
    }

    private static void rest(Corpse corpse) {
        if (corpse.previous != null) {
            corpse.previous.letGo();
            corpse.previous = null;
        }
        if (corpse.pose == null || !corpse.pose.built()) {
            Corpses.letGo(corpse);
            return;
        }
        corpse.picked = false;
        corpse.share = 0.0f;
        corpse.progress = 0.0f;
        corpse.restedAt = frameNumber;
        corpse.restBytes = corpse.pose.bytes();
        restingBytes += corpse.restBytes;
        resting.put(corpse.body, corpse);
    }

    private static Corpse back(IsoDeadBody isoDeadBody) {
        Corpse corpse = (Corpse)resting.remove(isoDeadBody);
        if (corpse == null) {
            return new Corpse(isoDeadBody, frameNumber);
        }
        restingBytes -= corpse.restBytes;
        corpse.firstListed = frameNumber;
        return corpse;
    }

    private static void choose(float f) {
        Corpse corpse;
        int n;
        CorpsePicks.Limits limits = new CorpsePicks.Limits(LIMIT.get(), Math.min(BAND.get(), LIMIT.get()), Math.min(EVERY.get() ? Integer.MAX_VALUE : MOST.getInt(), Corpses.room()), RANDOMNESS.get(), f / SWAP_SECONDS.get());
        bodies.size(listed.size());
        for (n = 0; n < listed.size(); ++n) {
            corpse = listed.get(n);
            Corpses.bodies.distance[n] = corpse.distance;
            Corpses.bodies.random[n] = corpse.random;
            Corpses.bodies.ready[n] = corpse.pose != null && corpse.pose.look == corpse.look && corpse.pose.drawable() || corpse.previous != null;
            Corpses.bodies.fresh[n] = frameNumber - corpse.firstListed <= 60L;
            Corpses.bodies.eligible[n] = corpse.eligible;
            Corpses.bodies.picked[n] = corpse.picked;
            Corpses.bodies.progress[n] = corpse.progress;
        }
        CorpsePicks.pick(bodies, limits);
        for (n = 0; n < listed.size(); ++n) {
            corpse = listed.get(n);
            corpse.picked = Corpses.bodies.picked[n];
            corpse.progress = Corpses.bodies.progress[n];
            corpse.share = Corpses.bodies.share[n];
        }
    }

    private static int room() {
        if (poseBytes == 0L) {
            return Integer.MAX_VALUE;
        }
        return (int)Math.min(Integer.MAX_VALUE, (long)((double)Pool.CORPSES.cap() * 0.8) / poseBytes);
    }

    private static void looks() {
        for (Corpse corpse : listed) {
            if (corpse.pose == null || corpse.pose.look == corpse.look) continue;
            if (corpse.previous == null && corpse.picked && corpse.pose.drawable()) {
                corpse.previous = corpse.pose;
            } else {
                corpse.pose.letGo();
            }
            corpse.pose = null;
        }
    }

    private static void poses() {
        for (Corpse corpse : listed) {
            if (corpse.pose != null) {
                corpse.pose.advance();
                if (corpse.picked) {
                    corpse.pose.lastUsed = frameNumber;
                }
            }
            if (corpse.previous == null || corpse.picked && (corpse.pose == null || !corpse.pose.drawable())) continue;
            corpse.previous.letGo();
            corpse.previous = null;
        }
        for (int i = POSES.getInt(); i > 0 && !Pool.CORPSES.over(0.9); --i) {
            Corpse corpse;
            corpse = null;
            for (Corpse corpse2 : listed) {
                if (!corpse2.picked || corpse2.pose != null || corpse != null && !Corpses.before(corpse2, corpse)) continue;
                corpse = corpse2;
            }
            if (corpse == null) {
                return;
            }
            corpse.pose = new CorpsePose(corpse.body, corpse.look);
            corpse.pose.lastUsed = frameNumber;
        }
    }

    private static boolean before(Corpse corpse, Corpse corpse2) {
        boolean bl;
        if (corpse.previous != null != (corpse2.previous != null)) {
            return corpse.previous != null;
        }
        boolean bl2 = frameNumber - corpse.firstListed <= 60L;
        boolean bl3 = bl = frameNumber - corpse2.firstListed <= 60L;
        return bl2 != bl ? bl2 : corpse.distance < corpse2.distance;
    }

    private static void makeRoom() {
        Object object;
        long l = restingBytes;
        long l2 = 0L;
        int n = 0;
        for (Corpse object2 : listed) {
            l += Corpses.bytes(object2);
            if (object2.pose == null || !object2.pose.drawable()) continue;
            l2 += object2.pose.bytes();
            ++n;
        }
        poseBytes = n > 0 ? l2 / (long)n : poseBytes;
        Pool.CORPSES.use(l);
        while (Pool.CORPSES.over(0.9) && !resting.isEmpty()) {
            object = resting.values().iterator().next();
            resting.remove(((Corpse)object).body);
            restingBytes -= ((Corpse)object).restBytes;
            Corpses.letGo((Corpse)object);
            Pool.CORPSES.use(l -= ((Corpse)object).restBytes);
            if (Pool.CORPSES.over(0.8)) continue;
            return;
        }
        while (Pool.CORPSES.over(0.9)) {
            object = null;
            for (Corpse corpse : listed) {
                if (corpse.pose == null || corpse.picked || object != null && corpse.pose.lastUsed >= ((Corpse)object).pose.lastUsed) continue;
                object = corpse;
            }
            if (object == null) {
                return;
            }
            Corpses.letGo((Corpse)object);
            Pool.CORPSES.use(l -= Corpses.bytes((Corpse)object));
            if (Pool.CORPSES.over(0.8)) continue;
            return;
        }
    }

    private static void letGoAll() {
        for (Corpse corpse : known.values()) {
            Corpses.letGo(corpse);
            corpse.picked = false;
            corpse.share = 0.0f;
            corpse.progress = 0.0f;
        }
        for (Corpse corpse : resting.values()) {
            Corpses.letGo(corpse);
        }
        resting.clear();
        restingBytes = 0L;
        Pool.CORPSES.use(0L);
    }

    private static void letGo(Corpse corpse) {
        if (corpse.pose != null) {
            corpse.pose.letGo();
            corpse.pose = null;
        }
        if (corpse.previous != null) {
            corpse.previous.letGo();
            corpse.previous = null;
        }
    }

    private static long bytes(Corpse corpse) {
        return (corpse.pose == null ? 0L : corpse.pose.bytes()) + (corpse.previous == null ? 0L : corpse.previous.bytes());
    }

    private static void report(long l) {
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        for (Corpse corpse : listed) {
            n += corpse.share > 0.0f ? 1 : 0;
            n2 += corpse.share > 0.0f && corpse.share < 1.0f ? 1 : 0;
            n3 += corpse.picked && (corpse.pose == null || !corpse.pose.drawable()) ? 1 : 0;
            n4 += corpse.pose != null ? 1 : 0;
        }
        counts = new int[]{listed.size(), n, n2, n3, n4, resting.size()};
        pickNanos = l;
        Profile.corpsesReport = Corpses::describe;
    }

    private static String describe() {
        int[] nArray = counts;
        return String.format("%d listed, %d models (%d dissolving), %d waiting, %d posed, %d resting, %.2f ms", nArray[0], nArray[1], nArray[2], nArray[3], nArray[4], nArray[5], (double)pickNanos * 1.0E-6);
    }

    private static float random(IsoDeadBody isoDeadBody) {
        long l = Float.floatToIntBits(isoDeadBody.getX());
        l = l * 31L + (long)Float.floatToIntBits(isoDeadBody.getY());
        l = l * 31L + (long)Float.floatToIntBits(isoDeadBody.getZ());
        l = l * 31L + (long)Float.floatToIntBits(isoDeadBody.getAngle());
        l *= -7046029254386353131L;
        l ^= l >>> 29;
        return (float)(l >>> 40) / 1.6777216E7f;
    }

    private Corpses() {
    }

    static {
        LIMIT.describe("How far ahead a corpse may be a 3D model: the length of the cone that starts at the player and runs ahead, as wide as the screen. The chance of a corpse being a model falls along it from the player's feet, and over its last squares (Fade band) the models dissolve to the flat pictures. At most 40: the ring of chunks the models are drawn in.");
        MOST.describe("At most this many corpses are 3D models, and no more than the posed corpses' pool holds (Cap: posed corpses, below; about 1.2 MiB a corpse). With fewer, the models thin out along the whole cone rather than stopping short of its distance.");
        EVERY.describe("For a strong PC: every corpse in the cone, within the distance above, is a 3D model, however many lie there, in place of Most 3D corpses. The posed corpses' pool still bounds them, about 1.2 MiB a corpse: raise its cap (below) for a big pile.");
        IN_VIEW.describe("On, a corpse is considered for a 3D model only while it lies in the cone that starts at the player and runs ahead as wide as the screen (with a small margin), and is on screen: any other keeps its picture with nothing done for it each frame, and one posed lately takes its pose back as it comes into the cone. Off, to compare: every corpse within the distance, all round the player.");
        Pool.CORPSES.describe("The most the posed corpses hold, about 1.2 MiB a corpse: it bounds how many corpses are 3D models. Raised or lowered, it takes effect at once.");
        Pool.CORPSES.alsoIn(SECTION);
        known = new IdentityHashMap();
        resting = new LinkedHashMap();
        listed = new ArrayList();
        bodies = new CorpsePicks.Bodies();
        counts = new int[6];
    }

    private static final class Corpse {
        final IsoDeadBody body;
        final float random;
        long firstListed;
        long restedAt;
        long restBytes;
        Object look;
        float coverage;
        float distance;
        float progress;
        float share;
        boolean listed;
        boolean eligible;
        boolean picked;
        CorpsePose pose;
        CorpsePose previous;

        Corpse(IsoDeadBody isoDeadBody, long l) {
            this.body = isoDeadBody;
            this.random = Corpses.random(isoDeadBody);
            this.firstListed = l;
        }
    }
}

