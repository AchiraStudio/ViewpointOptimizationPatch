/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import viewpoint.platform.Settings;

public final class Threads {
    private static volatile Thread game;
    private static volatile Thread render;
    private static volatile boolean reported;

    public static void game() {
        if (Settings.dev) {
            game = Threads.check(game, "game");
        }
    }

    public static void render() {
        if (Settings.dev) {
            render = Threads.check(render, "render");
        }
    }

    private static Thread check(Thread thread, String string) {
        Thread thread2 = Thread.currentThread();
        if (thread == null) {
            return thread2;
        }
        if (thread != thread2 && !reported) {
            reported = true;
            System.out.println("[Viewpoint] " + string + "-thread code ran on " + thread2.getName() + " (its thread is " + thread.getName() + "):");
            new Throwable().printStackTrace(System.out);
        }
        return thread;
    }

    private Threads() {
    }
}

