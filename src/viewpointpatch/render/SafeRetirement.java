package viewpointpatch.render;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.lwjgl.opengl.GL32;
import viewpointpatch.config.SafeMode;
import viewpointpatch.diagnostics.FailureTracker;
import viewpointpatch.diagnostics.PatchLogger;

public final class SafeRetirement {
    private static volatile boolean initialized = false;

    // Reflected fields and methods from viewpoint.render.Retirement
    private static Field retiredField;
    private static Field waitingField;
    private static Field fencedField;
    private static Field fencesField;
    private static Method freePassedMethod;
    private static Method freeMethod;
    private static Method placeMethod;
    private static Method passedMethod;
    private static Method deleteMethod;
    private static Method finishMethod;
    private static Method getStampMethod;
    private static Method getThingMethod;
    private static Method getFenceMethod;
    private static Method getThingsMethod;
    private static Constructor<?> fencedCtor;

    // Adaptive queue backpressure tuning
    private static final int QUEUE_SOFT_CAP = 48;
    private static final int QUEUE_HARD_CAP = 96;
    private static final long WAIT_TIMEOUT_NS = 1_000_000L; // 1 ms wait on oldest fence

    // Diagnostics & stats
    public static long totalBatchesRetired = 0;
    public static long fencesSuccessfullyReclaimed = 0;
    public static long softCapWaitsSatisfied = 0;
    private static int consecutiveFenceErrors = 0;

    private SafeRetirement() {}

    public static synchronized boolean init(Class<?> clazz) {
        if (initialized) {
            return true;
        }
        try {
            retiredField = clazz.getDeclaredField("retired");
            retiredField.setAccessible(true);

            waitingField = clazz.getDeclaredField("waiting");
            waitingField.setAccessible(true);

            fencedField = clazz.getDeclaredField("fenced");
            fencedField.setAccessible(true);

            fencesField = clazz.getDeclaredField("fences");
            fencesField.setAccessible(true);

            freePassedMethod = clazz.getDeclaredMethod("freePassed");
            freePassedMethod.setAccessible(true);

            freeMethod = clazz.getDeclaredMethod("free", ArrayList.class);
            freeMethod.setAccessible(true);

            Class<?> fencesInterface = fencesField.getType();
            placeMethod = fencesInterface.getDeclaredMethod("place");
            placeMethod.setAccessible(true);

            passedMethod = fencesInterface.getDeclaredMethod("passed", Long.TYPE);
            passedMethod.setAccessible(true);

            deleteMethod = fencesInterface.getDeclaredMethod("delete", Long.TYPE);
            deleteMethod.setAccessible(true);

            finishMethod = fencesInterface.getDeclaredMethod("finish");
            finishMethod.setAccessible(true);

            for (Class<?> inner : clazz.getDeclaredClasses()) {
                if (inner.getSimpleName().equals("Retired")) {
                    getStampMethod = inner.getDeclaredMethod("stamp");
                    getStampMethod.setAccessible(true);
                    getThingMethod = inner.getDeclaredMethod("thing");
                    getThingMethod.setAccessible(true);
                } else if (inner.getSimpleName().equals("Fenced")) {
                    getFenceMethod = inner.getDeclaredMethod("fence");
                    getFenceMethod.setAccessible(true);
                    getThingsMethod = inner.getDeclaredMethod("things");
                    getThingsMethod.setAccessible(true);
                    fencedCtor = inner.getDeclaredConstructor(Long.TYPE, ArrayList.class);
                    fencedCtor.setAccessible(true);
                }
            }

            initialized = true;
            PatchLogger.info("SafeRetirement pipeline initialized: strict GPU completion tracking active.");
            return true;
        } catch (Throwable t) {
            FailureTracker.recordFailure("Retirement", t);
            PatchLogger.error("Failed to reflect viewpoint.render.Retirement internals: " + t.getMessage(), t);
            SafeMode.disableRetirement("Reflection initialization failed");
            return false;
        }
    }

    /**
     * Executes safe, adaptive retirement.
     * Returns true if handled completely (original method can be skipped).
     * Returns false to fall back to vanilla Viewpoint execution.
     */
    @SuppressWarnings("unchecked")
    public static boolean execute(long l, Object self) {
        if (!SafeMode.isRetirementOptimized()) {
            return false; // Fall back to vanilla engine retirement
        }

        if (!initialized && !init(self.getClass())) {
            return false; // Fall back to vanilla engine retirement
        }

        try {
            ConcurrentLinkedQueue<?> retired = (ConcurrentLinkedQueue<?>) retiredField.get(self);
            ArrayDeque<Object> waiting = (ArrayDeque<Object>) waitingField.get(self);
            ArrayDeque<Object> fenced = (ArrayDeque<Object>) fencedField.get(self);
            Object fences = fencesField.get(self);

            // 1. Ingest concurrently retired items into waiting queue
            Object item;
            while ((item = retired.poll()) != null) {
                waiting.addLast(item);
            }

            // 2. Collect items whose stamp <= l into current frame batch
            ArrayList<Object> batch = new ArrayList<Object>();
            while (!waiting.isEmpty()) {
                Object peek = waiting.peekFirst();
                long stamp = ((Long) getStampMethod.invoke(peek)).longValue();
                if (stamp <= l) {
                    batch.add(getThingMethod.invoke(waiting.pollFirst()));
                } else {
                    break;
                }
            }

            if (batch.isEmpty()) {
                return true; // Nothing to retire for this frame
            }

            // 3. Proactively free already-passed fences without stalls
            freePassedMethod.invoke(self);

            // 4. Place new fence for the current batch
            long fenceId = ((Long) placeMethod.invoke(fences)).longValue();

            if (fenceId == 0L) {
                consecutiveFenceErrors++;
                PatchLogger.warn("glFenceSync returned 0 (consecutive: " + consecutiveFenceErrors + "). Retaining resources safely without premature free.");
                if (consecutiveFenceErrors >= 3) {
                    SafeMode.disableRetirement("Fence creation returned 0 repeatedly. Driver lacks reliable GL32 sync support.");
                }
                // Do NOT free early. Return false so vanilla retirement executes its safe synchronization.
                return false;
            }

            consecutiveFenceErrors = 0;
            Object fencedObj = fencedCtor.newInstance(fenceId, batch);
            fenced.addLast(fencedObj);
            totalBatchesRetired++;

            // 5. Adaptive queue management & backpressure
            // Check if queue exceeds soft cap (48)
            if (fenced.size() > QUEUE_SOFT_CAP) {
                freePassedMethod.invoke(self);
            }

            // If still above soft cap, wait specifically on the OLDEST fence
            while (fenced.size() > QUEUE_SOFT_CAP) {
                Object oldest = fenced.peekFirst();
                if (oldest == null) break;

                long oldestFence = ((Long) getFenceMethod.invoke(oldest)).longValue();

                // Wait only for the oldest fence (max 1ms)
                int waitResult = GL32.glClientWaitSync(oldestFence, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, WAIT_TIMEOUT_NS);
                if (waitResult == GL32.GL_ALREADY_SIGNALED || waitResult == GL32.GL_CONDITION_SATISFIED) {
                    // Confirmed complete by GPU! Safe to free.
                    fenced.pollFirst();
                    deleteMethod.invoke(fences, oldestFence);
                    ArrayList<?> oldThings = (ArrayList<?>) getThingsMethod.invoke(oldest);
                    freeMethod.invoke(null, oldThings);
                    fencesSuccessfullyReclaimed++;
                    softCapWaitsSatisfied++;
                } else if (waitResult == GL32.GL_WAIT_FAILED) {
                    PatchLogger.warn("glClientWaitSync failed on fence " + oldestFence);
                    break;
                } else {
                    // Timeout: GPU is still actively processing this batch.
                    // DO NOT FREE! Back off and avoid CPU spin.
                    break;
                }
            }

            // Emergency Hard Cap: if backlog reaches hard cap (96), synchronize safely
            if (fenced.size() >= QUEUE_HARD_CAP) {
                PatchLogger.warn("GPU fence backlog reached hard cap (" + fenced.size() + "). Performing safe queue synchronization.");
                finishMethod.invoke(fences); // Engine finish
                while ((item = fenced.pollFirst()) != null) {
                    long f = ((Long) getFenceMethod.invoke(item)).longValue();
                    deleteMethod.invoke(fences, f);
                    ArrayList<?> t = (ArrayList<?>) getThingsMethod.invoke(item);
                    freeMethod.invoke(null, t);
                }
            }

            FailureTracker.recordSuccess("Retirement");
            return true; // Successfully and safely handled; skip vanilla stall
        } catch (Throwable t) {
            FailureTracker.recordFailure("Retirement", t);
            return false; // Safely fall back to original Viewpoint method
        }
    }
}

