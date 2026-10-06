/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import viewpoint.platform.LiveSettings;

final class FarWorkers {
    private static final LiveSettings.Number THREADS = LiveSettings.number("far.workers", "Worker threads", "World/Far world", 1.0f, 8.0f, 1.0f, 2.0f);
    static final AtomicInteger jobs = new AtomicInteger();
    static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(2, 2, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), runnable -> {
        Thread thread = new Thread(runnable, "Viewpoint far world");
        thread.setDaemon(true);
        thread.setPriority(1);
        return thread;
    });

    static void resize() {
        int n = THREADS.getInt();
        if (n > POOL.getMaximumPoolSize()) {
            POOL.setMaximumPoolSize(n);
            POOL.setCorePoolSize(n);
        } else if (n < POOL.getCorePoolSize()) {
            POOL.setCorePoolSize(n);
            POOL.setMaximumPoolSize(n);
        }
    }

    private FarWorkers() {
    }
}

