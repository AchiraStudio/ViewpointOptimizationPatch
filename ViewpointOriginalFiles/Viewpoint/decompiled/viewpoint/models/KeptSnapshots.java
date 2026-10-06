/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.skinnedmodel.model.ModelSlotRenderData
 */
package viewpoint.models;

import java.util.IdentityHashMap;
import java.util.Map;
import viewpoint.core.Frame;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;

final class KeptSnapshots {
    private static final Map<ModelSlotRenderData, int[]> frames = new IdentityHashMap<ModelSlotRenderData, int[]>();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void keep(ModelSlotRenderData modelSlotRenderData) {
        Map<ModelSlotRenderData, int[]> map = frames;
        synchronized (map) {
            frames.put(modelSlotRenderData, new int[1]);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void use(Frame frame, ModelSlotRenderData modelSlotRenderData) {
        Map<ModelSlotRenderData, int[]> map = frames;
        synchronized (map) {
            int[] nArray = frames.get(modelSlotRenderData);
            nArray[0] = nArray[0] + 1;
        }
        frame.snapshots.add(modelSlotRenderData);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void replace(ModelSlotRenderData modelSlotRenderData) {
        boolean bl;
        Map<ModelSlotRenderData, int[]> map = frames;
        synchronized (map) {
            int[] nArray = frames.get(modelSlotRenderData);
            boolean bl2 = bl = nArray[0] == 0;
            if (bl) {
                frames.remove(modelSlotRenderData);
            } else {
                nArray[0] = -nArray[0] - 1;
            }
        }
        if (bl) {
            modelSlotRenderData.postRender();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean release(ModelSlotRenderData modelSlotRenderData) {
        Map<ModelSlotRenderData, int[]> map = frames;
        synchronized (map) {
            boolean bl;
            int[] nArray = frames.get(modelSlotRenderData);
            if (nArray == null) {
                return true;
            }
            if (nArray[0] > 0) {
                nArray[0] = nArray[0] - 1;
                return false;
            }
            if (nArray[0] == 0) {
                throw new IllegalStateException("a kept snapshot released more often than drawn");
            }
            nArray[0] = nArray[0] + 1;
            boolean bl2 = bl = nArray[0] == -1;
            if (bl) {
                frames.remove(modelSlotRenderData);
            }
            return bl;
        }
    }

    private KeptSnapshots() {
    }
}

