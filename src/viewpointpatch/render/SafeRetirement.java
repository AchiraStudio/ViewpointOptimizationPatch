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
    public static final int QUEUE_SOFT_CAP = 48;
    public static final int QUEUE_HIGH_CAP = 72;
    public static final int QUEUE_HARD_CAP = 96;

    // Tiered timeouts: 50us under moderate pressure, 200us under high pressure (avoids 1ms CPU stalls)
    private static final long WAIT_MODERATE_NS = 50_000L;  // 50 microseconds
    private static final long WAIT_HIGH_NS = 200_000L;     // 200 microseconds

    // Diagnostics & stats
    public static long totalBatchesRetired = 0;
    public static long fencesSuccessfullyReclaimed = 0;
    public static long softCapWaitsSatisfied = 0;
    public static long totalWaitTimeNs = 0;
    public static int currentQueueDepth = 0;
    public static int peakQueueDepth = 0;
    public static double rollingQueueDepth = 0.0;
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

        ArrayDeque<Object> waiting = null;
        ArrayList<Object> retiredBatch = null;

        try {
            ConcurrentLinkedQueue<?> retired = (ConcurrentLinkedQueue<?>) retiredField.get(self);
            waiting = (ArrayDeque<Object>) waitingField.get(self);
            ArrayDeque<Object> fenced = (ArrayDeque<Object>) fencedField.get(self);
            Object fences = fencesField.get(self);

            // 1. Ingest concurrently retired items into waiting queue
            Object item;
            while ((item = retired.poll()) != null) {
                waiting.addLast(item);
            }

            // 2. Collect Retired objects whose stamp <= l into pending list (retaining wrappers for atomic rollback)
            retiredBatch = new ArrayList<Object>();
            while (!waiting.isEmpty()) {
                Object peek = waiting.peekFirst();
                long stamp = ((Long) getStampMethod.invoke(peek)).longValue();
                if (stamp <= l) {
                    retiredBatch.add(waiting.pollFirst());
                } else {
                    break;
                }
            }

            if (retiredBatch.isEmpty()) {
                return true; // Nothing to retire for this frame
            }

            // 3. Proactively free already-passed fences without stalls
            freePassedMethod.invoke(self);

            // 4. Place new fence for the current batch
            long fenceId = ((Long) placeMethod.invoke(fences)).longValue();

            if (fenceId == 0L) {
                consecutiveFenceErrors++;
                PatchLogger.warn("glFenceSync returned 0 (consecutive: " + consecutiveFenceErrors + "). Restoring batch to waiting queue and delegating to engine sync.");
                if (consecutiveFenceErrors >= 3) {
                    SafeMode.disableRetirement("Fence creation returned 0 repeatedly. Driver lacks reliable GL32 sync support.");
                }
                // ATOMIC RESTORATION: Put all Retired entries back into waiting in exact original order
                for (int i = retiredBatch.size() - 1; i >= 0; i--) {
                    waiting.addFirst(retiredBatch.get(i));
                }
                // Return false so vanilla retirement executes safely with complete queue
                return false;
            }

            // Unwrap things only after fence creation is confirmed non-zero
            ArrayList<Object> batch = new ArrayList<Object>(retiredBatch.size());
            for (Object retObj : retiredBatch) {
                batch.add(getThingMethod.invoke(retObj));
            }

            consecutiveFenceErrors = 0;
            Object fencedObj = fencedCtor.newInstance(fenceId, batch);
            fenced.addLast(fencedObj);
            totalBatchesRetired++;

            // 5. Adaptive queue management & backpressure
            currentQueueDepth = fenced.size();
            if (currentQueueDepth > peakQueueDepth) {
                peakQueueDepth = currentQueueDepth;
            }
            rollingQueueDepth = (rollingQueueDepth * 0.95) + (currentQueueDepth * 0.05);

            // Level 1: If above soft cap (48), drain non-blocking first
            if (fenced.size() > QUEUE_SOFT_CAP) {
                freePassedMethod.invoke(self);
            }

            // Level 2: If still above soft cap, wait only on the OLDEST fence with tiered microsecond budget
            while (fenced.size() > QUEUE_SOFT_CAP) {
                Object oldest = fenced.peekFirst();
                if (oldest == null) break;

                long oldestFence = ((Long) getFenceMethod.invoke(oldest)).longValue();
                long timeoutNs = (fenced.size() > QUEUE_HIGH_CAP) ? WAIT_HIGH_NS : WAIT_MODERATE_NS;

                long startWait = System.nanoTime();
                int waitResult = GL32.glClientWaitSync(oldestFence, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, timeoutNs);
                totalWaitTimeNs += (System.nanoTime() - startWait);

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
                    // Timeout (50-200us): GPU is still actively processing this batch.
                    // DO NOT FREE! Back off cleanly without CPU spin or long stall.
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
            // In case of exception, restore any polled entries back into waiting queue
            if (retiredBatch != null && !retiredBatch.isEmpty() && waiting != null) {
                for (int i = retiredBatch.size() - 1; i >= 0; i--) {
                    waiting.addFirst(retiredBatch.get(i));
                }
            }
            return false; // Safely fall back to original Viewpoint method
        }
    }
}

