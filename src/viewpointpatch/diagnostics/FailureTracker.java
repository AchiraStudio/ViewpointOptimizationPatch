package viewpointpatch.diagnostics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import viewpointpatch.config.SafeMode;

public final class FailureTracker {
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final ConcurrentHashMap<String, AtomicInteger> errorCounts = new ConcurrentHashMap<String, AtomicInteger>();

    private FailureTracker() {}

    public static void recordSuccess(String subsystem) {
        AtomicInteger count = errorCounts.get(subsystem);
        if (count != null && count.get() > 0) {
            count.set(0);
        }
    }

    public static void recordFailure(String subsystem, Throwable t) {
        AtomicInteger count = errorCounts.computeIfAbsent(subsystem, k -> new AtomicInteger(0));
        int current = count.incrementAndGet();
        PatchLogger.warn("Failure in subsystem [" + subsystem + "] (count: " + current + "/" + MAX_CONSECUTIVE_FAILURES + "): " + (t != null ? t.getMessage() : "Unknown error"), t);

        if (current >= MAX_CONSECUTIVE_FAILURES) {
            if ("Retirement".equalsIgnoreCase(subsystem)) {
                SafeMode.disableRetirement("Exceeded maximum consecutive error threshold (" + MAX_CONSECUTIVE_FAILURES + ")");
            } else {
                SafeMode.triggerGlobalSafeMode("Repeated failures in subsystem [" + subsystem + "]");
            }
        }
    }
}

