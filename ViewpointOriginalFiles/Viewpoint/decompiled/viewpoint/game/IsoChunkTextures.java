/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.fboRenderChunk.FBORenderChunk
 *  zombie.iso.fboRenderChunk.FBORenderChunkManager
 */
package viewpoint.game;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import viewpoint.platform.LiveSettings;
import zombie.core.opengl.RenderThread;
import zombie.core.textures.TextureFBO;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;

public final class IsoChunkTextures {
    public static final LiveSettings.Toggle ON = LiveSettings.toggle("game.releaseIsoChunkTextures", "Isometric chunk textures let go in first person", "Game/Fixes", true);
    private static final int WAIT_FRAMES = 3;
    private static int frames;
    private static Field store;

    public static void update(boolean bl) {
        if (!bl) {
            frames = 0;
            return;
        }
        if (++frames == 3 && ON.get()) {
            IsoChunkTextures.release();
        }
    }

    private static void release() {
        long l = System.nanoTime();
        FBORenderChunkManager fBORenderChunkManager = FBORenderChunkManager.instance;
        fBORenderChunkManager.clearCache();
        fBORenderChunkManager.recycle();
        ArrayList<TextureFBO> arrayList = new ArrayList<TextureFBO>();
        for (ArrayList<FBORenderChunk> arrayList2 : IsoChunkTextures.store(fBORenderChunkManager).values()) {
            for (FBORenderChunk fBORenderChunk : arrayList2) {
                if (fBORenderChunk.fbo == null) continue;
                arrayList.add(fBORenderChunk.fbo);
                fBORenderChunk.fbo = null;
            }
            arrayList2.clear();
        }
        IsoChunkTextures.store(fBORenderChunkManager).clear();
        fBORenderChunkManager.renderChunk = null;
        double d = (double)(System.nanoTime() - l) * 1.0E-6;
        RenderThread.queueInvokeOnRenderContext(() -> {
            long l = System.nanoTime();
            for (TextureFBO textureFBO : arrayList) {
                textureFBO.destroy();
            }
            System.out.println(String.format("[Viewpoint] the isometric view's %d chunk textures let go: %.1f ms on the main thread, %.1f ms on the render thread", arrayList.size(), d, (double)(System.nanoTime() - l) * 1.0E-6));
        });
    }

    private static HashMap<Integer, ArrayList<FBORenderChunk>> store(FBORenderChunkManager fBORenderChunkManager) {
        try {
            if (store == null) {
                store = FBORenderChunkManager.class.getDeclaredField("sizeChunkStore");
                store.setAccessible(true);
            }
            return (HashMap)store.get(fBORenderChunkManager);
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("FBORenderChunkManager.sizeChunkStore could not be read (Compat)", reflectiveOperationException);
        }
    }

    private IsoChunkTextures() {
    }

    static {
        ON.describe("Frees about 16 MB of video memory, and as much of the system's commit, a chunk the isometric view has drawn; that view draws them again when it comes back.");
    }
}

