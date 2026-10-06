/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.models;

import java.util.Arrays;
import viewpoint.render.ModelDraws;

public final class ModelMotion {
    private static ModelDraws last;
    private static Object[] keys;
    private static int[] draws;

    public static void begin(ModelDraws modelDraws) {
        Arrays.fill(keys, null);
        if (last == null || last == modelDraws) {
            return;
        }
        int n = last.count();
        if (n * 2 > keys.length) {
            keys = new Object[Integer.highestOneBit(n * 2) * 2];
            draws = new int[keys.length];
        }
        for (int i = 0; i < n; ++i) {
            int n2;
            Object object = last.key(i);
            if (object == null || keys[n2 = ModelMotion.slot(object)] != null) continue;
            ModelMotion.keys[n2] = object;
            ModelMotion.draws[n2] = i;
        }
    }

    public static void end(ModelDraws modelDraws) {
        last = modelDraws;
    }

    static void follow(ModelDraws modelDraws, int n, Object object, int n2) {
        ModelMotion.follow(modelDraws, n, object, n2, false);
    }

    static void follow(ModelDraws modelDraws, int n, Object object, int n2, boolean bl) {
        modelDraws.key(n, object);
        if (last == null || last == modelDraws) {
            return;
        }
        int n3 = ModelMotion.slot(object);
        if (keys[n3] == object) {
            modelDraws.motion(n, last, draws[n3], n2, bl);
        }
    }

    static int lastPose(ModelDraws modelDraws, Object object, int n) {
        if (last == null || last == modelDraws) {
            return -1;
        }
        int n2 = ModelMotion.slot(object);
        return keys[n2] == object ? modelDraws.lastPose(last, draws[n2], n) : -1;
    }

    private static int slot(Object object) {
        int n = keys.length - 1;
        int n2 = System.identityHashCode(object) * -1640531527 >>> 7 & n;
        while (keys[n2] != null && keys[n2] != object) {
            n2 = n2 + 1 & n;
        }
        return n2;
    }

    private ModelMotion() {
    }

    static {
        keys = new Object[2048];
        draws = new int[2048];
    }
}

