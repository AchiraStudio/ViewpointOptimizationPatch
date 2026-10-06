/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL32
 */
package viewpoint.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

public final class Retirement {
    static final int MAX_FENCES = 64;
    private static final Retirement ON_GPU = new Retirement(new GlFences());
    private long building;
    private final ConcurrentLinkedQueue<Retired> retired = new ConcurrentLinkedQueue();
    private final Fences fences;
    private final ArrayDeque<Retired> waiting = new ArrayDeque();
    private final ArrayDeque<Fenced> fenced = new ArrayDeque();
    private boolean fencesFailed;

    public static void building(long l) {
        ON_GPU.stamp(l);
    }

    void stamp(long l) {
        this.building = l;
    }

    public static void retire(Freeable freeable) {
        ON_GPU.letGo(freeable);
    }

    public static void retireDrawing(Freeable freeable) {
        Retirement.ON_GPU.retired.add(new Retired(freeable, Long.MIN_VALUE));
    }

    void letGo(Freeable freeable) {
        if (freeable != null) {
            this.retired.add(new Retired(freeable, this.building));
        }
    }

    Retirement(Fences fences) {
        this.fences = fences;
    }

    public static void collect() {
        ON_GPU.freePassed();
    }

    public static void drawn(long l) {
        ON_GPU.fenceDrawn(l);
    }

    void freePassed() {
        while (!this.fenced.isEmpty() && this.fences.passed(this.fenced.peekFirst().fence())) {
            Fenced fenced = this.fenced.pollFirst();
            this.fences.delete(fenced.fence());
            Retirement.free(fenced.things());
        }
    }

    void fenceDrawn(long l) {
        Object object;
        while ((object = this.retired.poll()) != null) {
            this.waiting.addLast((Retired)object);
        }
        object = new ArrayList();
        while (!this.waiting.isEmpty() && this.waiting.peekFirst().stamp() <= l) {
            ((ArrayList)object).add(this.waiting.pollFirst().thing());
        }
        if (((ArrayList)object).isEmpty()) {
            return;
        }
        long l2 = this.fences.place();
        if (l2 == 0L || this.fenced.size() >= 64) {
            Fenced fenced;
            if (!this.fencesFailed) {
                this.fencesFailed = true;
                System.out.println("[Viewpoint] GPU fences failed: freeing after waiting for the GPU instead");
            }
            this.fences.finish();
            if (l2 != 0L) {
                this.fences.delete(l2);
            }
            while ((fenced = this.fenced.pollFirst()) != null) {
                this.fences.delete(fenced.fence());
                Retirement.free(fenced.things());
            }
            Retirement.free((ArrayList<Freeable>)object);
            return;
        }
        this.fenced.addLast(new Fenced(l2, (ArrayList<Freeable>)object));
    }

    int pending() {
        int n = this.waiting.size() + this.retired.size();
        for (Fenced fenced : this.fenced) {
            n += fenced.things().size();
        }
        return n;
    }

    private static void free(ArrayList<Freeable> arrayList) {
        for (Freeable freeable : arrayList) {
            freeable.free();
        }
    }

    public static interface Freeable {
        public void free();
    }

    private record Retired(Freeable thing, long stamp) {
    }

    static interface Fences {
        public long place();

        public boolean passed(long var1);

        public void delete(long var1);

        public void finish();
    }

    private record Fenced(long fence, ArrayList<Freeable> things) {
    }

    static final class GlFences
    implements Fences {
        GlFences() {
        }

        @Override
        public long place() {
            return GL32.glFenceSync((int)37143, (int)0);
        }

        @Override
        public boolean passed(long l) {
            int n = GL32.glClientWaitSync((long)l, (int)1, (long)0L);
            return n == 37146 || n == 37148;
        }

        @Override
        public void delete(long l) {
            GL32.glDeleteSync((long)l);
        }

        @Override
        public void finish() {
            GL11.glFinish();
        }
    }
}

