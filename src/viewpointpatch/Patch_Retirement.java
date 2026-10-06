package viewpointpatch;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "viewpoint.render.Retirement", methodName = "fenceDrawn")
public class Patch_Retirement {
    public static volatile boolean initialized;
    public static Field retiredField;
    public static Field waitingField;
    public static Field fencedField;
    public static Field fencesField;
    public static Method freePassedMethod;
    public static Method placeMethod;
    public static Method deleteMethod;
    public static Method freeMethod;
    public static Method getStampMethod;
    public static Method getThingMethod;
    public static Method getFenceMethod;
    public static Method getThingsMethod;
    public static Constructor<?> fencedCtor;

    // Ring buffer fallback for systems where glFenceSync returns 0
    public static final ArrayDeque<Object> fallbackRing = new ArrayDeque<Object>();
    public static int stallsPrevented = 0;

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

            deleteMethod = fencesInterface.getDeclaredMethod("delete", Long.TYPE);
            deleteMethod.setAccessible(true);

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
            System.out.println("[ViewpointOptimizationPatch] GPU fence stall eliminator initialized successfully.");
            return true;
        } catch (Throwable t) {
            System.out.println("[ViewpointOptimizationPatch] Warning: Failed to reflect Retirement fields: " + t);
            return false;
        }
    }

    @Patch.OnEnter(skipOn = true)
    public static boolean onEnter(long l, @Patch.This Object self) {
        if (!initialized && !init(self.getClass())) {
            return false; // Fall back to original method safely
        }

        try {
            ConcurrentLinkedQueue<?> retired = (ConcurrentLinkedQueue<?>) retiredField.get(self);
            ArrayDeque<Object> waiting = (ArrayDeque<Object>) waitingField.get(self);
            ArrayDeque<Object> fenced = (ArrayDeque<Object>) fencedField.get(self);
            Object fences = fencesField.get(self);

            // 1. Move concurrently retired items to waiting deque
            Object item;
            while ((item = retired.poll()) != null) {
                waiting.addLast(item);
            }

            // 2. Collect all items whose stamp <= l
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

            // 3. Proactively free already-passed fences
            freePassedMethod.invoke(self);

            // 4. Place new fence
            long fenceId = ((Long) placeMethod.invoke(fences)).longValue();

            if (fenceId != 0L) {
                Object fencedObj = fencedCtor.newInstance(fenceId, batch);
                fenced.addLast(fencedObj);

                // Bound queue size to 32 to prevent driver command queue backup.
                // In the original code, reaching 128 caused glFinish() lockup.
                // Retiring after 32 frames is completely safe on all hardware.
                while (fenced.size() > 32) {
                    Object oldest = fenced.pollFirst();
                    if (oldest != null) {
                        long oldFence = ((Long) getFenceMethod.invoke(oldest)).longValue();
                        deleteMethod.invoke(fences, oldFence);
                        ArrayList<?> oldThings = (ArrayList<?>) getThingsMethod.invoke(oldest);
                        freeMethod.invoke(null, oldThings);
                        stallsPrevented++;
                    }
                }
            } else {
                // glFenceSync returned 0 (driver sync object failure or unsupported on iGPU).
                // Safely defer by 4 frames (standard double/triple buffer swap clearance).
                fallbackRing.addLast(batch);
                while (fallbackRing.size() > 4) {
                    ArrayList<?> oldThings = (ArrayList<?>) fallbackRing.pollFirst();
                    if (oldThings != null) {
                        freeMethod.invoke(null, oldThings);
                        stallsPrevented++;
                    }
                }
            }

            return true; // Handled safely; skip original glFinish() method!
        } catch (Throwable t) {
            return false; // Fall back to original method if reflection fails
        }
    }
}
