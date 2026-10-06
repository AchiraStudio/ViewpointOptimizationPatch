/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.world;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import viewpoint.packs.PackLoads;
import viewpoint.render.PackModel;
import viewpoint.render.PackModels;
import viewpoint.world.ChunkCache;

final class ModelWaits {
    private static final IdentityHashMap<PackModel, ArrayList<ChunkCache.Level>> byModel = new IdentityHashMap();
    private static final IdentityHashMap<ChunkCache.Level, ArrayList<PackModel>> byLevel = new IdentityHashMap();
    private static int lastRoom;

    static void gathered(ChunkCache.Level level, List<PackModel> list) {
        if (list == null) {
            byLevel.remove(level);
            return;
        }
        byLevel.put(level, new ArrayList<PackModel>(list));
        for (PackModel packModel2 : list) {
            ArrayList arrayList = byModel.computeIfAbsent(packModel2, packModel -> new ArrayList());
            if (arrayList.contains(level)) continue;
            arrayList.add(level);
        }
    }

    static void frame() {
        PackModel packModel;
        PackLoads.frame();
        ModelWaits.roomAgain();
        while ((packModel = PackModels.settled()) != null) {
            ArrayList<ChunkCache.Level> arrayList = byModel.remove(packModel);
            for (int i = 0; arrayList != null && i < arrayList.size(); ++i) {
                ChunkCache.Level level = arrayList.get(i);
                ArrayList<PackModel> arrayList2 = byLevel.get(level);
                if (arrayList2 == null || level.evicted) {
                    byLevel.remove(level);
                    continue;
                }
                arrayList2.remove(packModel);
                if (ModelWaits.onTheirWay(arrayList2)) continue;
                byLevel.remove(level);
                ChunkCache.mark(level, 4);
            }
        }
    }

    private static void roomAgain() {
        int n = PackModels.roomAgain();
        if (n == lastRoom) {
            return;
        }
        lastRoom = n;
        Iterator<Map.Entry<ChunkCache.Level, ArrayList<PackModel>>> iterator = byLevel.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ChunkCache.Level, ArrayList<PackModel>> entry = iterator.next();
            ChunkCache.Level level = entry.getKey();
            boolean bl = false;
            for (PackModel packModel : entry.getValue()) {
                bl |= packModel.state() == 5;
            }
            if (bl || level.evicted) {
                iterator.remove();
            }
            if (!bl || level.evicted) continue;
            ChunkCache.mark(level, 4);
        }
    }

    static void clear() {
        byModel.clear();
        byLevel.clear();
        PackLoads.clear();
    }

    private static boolean onTheirWay(List<PackModel> list) {
        for (PackModel packModel : list) {
            int n = packModel.state();
            if (n != 1 && n != 2) continue;
            return true;
        }
        return false;
    }

    private ModelWaits() {
    }
}

