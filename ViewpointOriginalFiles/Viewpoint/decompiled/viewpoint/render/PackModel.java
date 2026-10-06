/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import viewpoint.render.PackData;
import viewpoint.render.PackModels;

public final class PackModel {
    public static final int UNLOADED = 0;
    public static final int READING = 1;
    public static final int READ = 2;
    public static final int RESIDENT = 3;
    public static final int FAILED = 4;
    public static final int NO_ROOM = 5;
    public final int index;
    public final File obj;
    private volatile int state;
    private final AtomicInteger holds = new AtomicInteger();
    private volatile long wanted;
    private volatile PackData data;
    private volatile long refused;
    private volatile float[] bounds;
    int meshPage;
    int firstVertex;
    int vertices;
    int firstIndex;
    int indices;
    int artPage;
    int artX;
    int artY;
    int artW;
    int artH;
    int artWidth;
    int artHeight;

    PackModel(int n, File file) {
        this.index = n;
        this.obj = file;
    }

    public int state() {
        return this.state;
    }

    public void wanted(long l) {
        this.wanted = l;
    }

    public long wanted() {
        return this.wanted;
    }

    public void reading() {
        this.state = 1;
    }

    public void read(PackData packData) {
        float[] fArray = packData.vertices();
        float[] fArray2 = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -3.4028235E38f, -3.4028235E38f, -3.4028235E38f};
        for (int i = 0; i < fArray.length; i += 8) {
            for (int j = 0; j < 3; ++j) {
                fArray2[j] = Math.min(fArray2[j], fArray[i + j]);
                fArray2[j + 3] = Math.max(fArray2[j + 3], fArray[i + j]);
            }
        }
        this.bounds = fArray2;
        this.data = packData;
        this.state = 2;
        PackModels.READ.add(this);
    }

    public float[] bounds() {
        return this.bounds;
    }

    public void failed() {
        this.state = 4;
        PackModels.SETTLED.add(this);
    }

    public void retry() {
        this.state = 0;
    }

    public long refused() {
        return this.refused;
    }

    void refused(long l) {
        this.refused = l;
    }

    public void hold() {
        this.holds.incrementAndGet();
    }

    public void release() {
        this.holds.decrementAndGet();
    }

    boolean held() {
        return this.holds.get() > 0;
    }

    void state(int n) {
        this.state = n;
    }

    PackData take() {
        PackData packData = this.data;
        this.data = null;
        return packData;
    }
}

