/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.world;

import java.util.HashMap;
import viewpoint.platform.Caches;
import viewpoint.render.FloorArt;
import viewpoint.render.FloorPage;

final class FloorPages {
    private static final HashMap<Long, Held> held = new HashMap();
    private static long bytes;

    static void put(int n, int n2, int n3, FloorArt floorArt) {
        if (floorArt == null) {
            FloorPages.remove(n, n2, n3);
            return;
        }
        long l = FloorPage.key(n, n2, n3);
        Held held = FloorPages.held.get(l);
        if (held != null && held.art.sameArt(floorArt)) {
            return;
        }
        if (held == null) {
            held = new Held();
            FloorPages.held.put(l, held);
        }
        bytes += floorArt.bytes() - (held.art == null ? 0L : held.art.bytes());
        held.art = floorArt;
        FloorPages.repage(n, n2, n3);
    }

    static void remove(int n, int n2, int n3) {
        Held held = FloorPages.held.remove(FloorPage.key(n, n2, n3));
        if (held != null) {
            bytes -= held.art.bytes();
            FloorPages.repage(n, n2, n3);
        }
    }

    static FloorPage page(int n, int n2, int n3) {
        Held held = FloorPages.held.get(FloorPage.key(n, n2, n3));
        return held == null ? null : held.page;
    }

    static void clear() {
        held.clear();
        bytes = 0L;
    }

    private static void repage(int n, int n2, int n3) {
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                Held held = FloorPages.held.get(FloorPage.key(n + j, n2 + i, n3));
                if (held == null) continue;
                held.page = new FloorPage(n + j, n2 + i, n3, held.art, FloorPages.neighbours(n + j, n2 + i, n3));
            }
        }
    }

    private static FloorArt[] neighbours(int n, int n2, int n3) {
        FloorArt[] floorArtArray = new FloorArt[FloorPage.NEIGHBOURS.length / 2];
        for (int i = 0; i < floorArtArray.length; ++i) {
            Held held = FloorPages.held.get(FloorPage.key(n + FloorPage.NEIGHBOURS[i * 2], n2 + FloorPage.NEIGHBOURS[i * 2 + 1], n3));
            floorArtArray[i] = held == null ? null : held.art;
        }
        return floorArtArray;
    }

    private FloorPages() {
    }

    static {
        Caches.register(new Caches.Cache(){

            @Override
            public long bytes() {
                return bytes;
            }

            @Override
            public void list(Caches.Survey survey) {
            }

            @Override
            public void evictBefore(long l) {
            }
        });
    }

    private static final class Held {
        FloorArt art;
        FloorPage page;

        private Held() {
        }
    }
}

