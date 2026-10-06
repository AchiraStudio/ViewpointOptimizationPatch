/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.skinnedmodel.model.ModelInstanceRenderData
 *  zombie.core.skinnedmodel.model.ModelInstanceTextureInitializer
 *  zombie.core.skinnedmodel.model.ModelSlotRenderData
 */
package viewpoint.render;

import java.lang.reflect.Field;
import java.util.Arrays;
import viewpoint.platform.Profile;
import viewpoint.render.ModelDraws;
import viewpoint.render.ModelMeshes;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.ModelInstanceTextureInitializer;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;

final class CharacterTextures {
    private static final float ALWAYS = 6.0f;
    private static final int PER_FRAME = 4;
    private long[] waiting = new long[64];
    private int waitingCount;

    CharacterTextures() {
    }

    void ready(ModelDraws modelDraws) {
        int n;
        this.waiting = this.waiting.length >= modelDraws.slotCount ? this.waiting : new long[modelDraws.slotCount * 2];
        this.waitingCount = 0;
        int n2 = 0;
        for (n = 0; n < modelDraws.slotCount; ++n) {
            Composited composited = modelDraws.slots[n];
            boolean bl = composited.composites();
            if (!bl || modelDraws.slotDistance[n] <= 6.0f) {
                n2 += bl ? 1 : 0;
                composited.ready();
                continue;
            }
            this.waiting[this.waitingCount++] = (long)Float.floatToIntBits(modelDraws.slotDistance[n]) << 32 | (long)n;
        }
        Arrays.sort(this.waiting, 0, this.waitingCount);
        n = Math.min(4, this.waitingCount);
        for (int i = 0; i < n; ++i) {
            modelDraws.slots[(int)this.waiting[i]].ready();
        }
        Profile.modelTexturesMade = n2 + n;
        Profile.modelTexturesWaiting = this.waitingCount - n;
    }

    void leaveOut(ModelDraws modelDraws, ModelMeshes.Held[] heldArray) {
        for (int i = Math.min(4, this.waitingCount); i < this.waitingCount; ++i) {
            int n = (int)this.waiting[i];
            int n2 = modelDraws.slotFirst[n];
            Arrays.fill(heldArray, n2, n2 + modelDraws.slotDraws[n], null);
        }
    }

    static interface Composited {
        public boolean composites();

        public void ready();
    }

    static final class Snapshot
    implements Composited {
        private static final Field RENDERED = Snapshot.rendered();
        private final ModelSlotRenderData data;

        Snapshot(ModelSlotRenderData modelSlotRenderData) {
            this.data = modelSlotRenderData;
        }

        @Override
        public boolean composites() {
            if (this.data.textureCreator != null && !this.data.textureCreator.isRendered()) {
                return true;
            }
            for (int i = 0; i < this.data.modelData.size(); ++i) {
                ModelInstanceRenderData modelInstanceRenderData = (ModelInstanceRenderData)this.data.modelData.get(i);
                ModelInstanceTextureInitializer modelInstanceTextureInitializer = modelInstanceRenderData.modelInstance.getTextureInitializer();
                if (modelInstanceTextureInitializer == null || modelInstanceTextureInitializer.isRendered()) continue;
                return true;
            }
            return false;
        }

        @Override
        public void ready() {
            boolean bl = this.data.checkReady();
            if (RENDERED != null) {
                try {
                    RENDERED.setBoolean(this.data, bl);
                }
                catch (ReflectiveOperationException | RuntimeException exception) {
                    // empty catch block
                }
            }
        }

        private static Field rendered() {
            try {
                Field field = ModelSlotRenderData.class.getDeclaredField("rendered");
                field.setAccessible(true);
                return field;
            }
            catch (ReflectiveOperationException | RuntimeException exception) {
                System.out.println("[Viewpoint] models: rendered cannot be set: " + String.valueOf(exception));
                return null;
            }
        }
    }
}

