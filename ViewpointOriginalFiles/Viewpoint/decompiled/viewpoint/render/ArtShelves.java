/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import java.util.ArrayList;

final class ArtShelves {
    private final int size;
    private final int grain;
    private final ArrayList<Shelf> shelves = new ArrayList();
    private int top;

    ArtShelves(int n, int n2) {
        this.size = n;
        this.grain = n2;
    }

    int[] take(int n, int n2) {
        if (n > this.size || n2 > this.size || n % this.grain != 0 || n2 % this.grain != 0) {
            return null;
        }
        for (Shelf shelf : this.shelves) {
            int n3;
            if (shelf.height < n2 || shelf.height * 3 > n2 * 4 || (n3 = shelf.take(n)) < 0) continue;
            return new int[]{n3, shelf.y};
        }
        if (this.top + n2 > this.size) {
            return null;
        }
        Shelf shelf = new Shelf(this.top, n2, this.size);
        this.shelves.add(shelf);
        this.top += n2;
        return new int[]{shelf.take(n), shelf.y};
    }

    void give(int n, int n2, int n3) {
        for (int i = 0; i < this.shelves.size(); ++i) {
            Shelf shelf = this.shelves.get(i);
            if (shelf.y != n2) continue;
            shelf.give(n, n3);
            while (!this.shelves.isEmpty() && this.shelves.get(this.shelves.size() - 1).empty(this.size)) {
                this.top -= this.shelves.remove((int)(this.shelves.size() - 1)).height;
            }
            return;
        }
        throw new IllegalArgumentException("no shelf at y " + n2);
    }

    boolean empty() {
        return this.shelves.isEmpty();
    }

    private static final class Shelf {
        final int y;
        final int height;
        final ArrayList<int[]> free = new ArrayList();

        Shelf(int n, int n2, int n3) {
            this.y = n;
            this.height = n2;
            this.free.add(new int[]{0, n3});
        }

        int take(int n) {
            for (int i = 0; i < this.free.size(); ++i) {
                int[] nArray = this.free.get(i);
                if (nArray[1] < n) continue;
                int n2 = nArray[0];
                nArray[0] = nArray[0] + n;
                nArray[1] = nArray[1] - n;
                if (nArray[1] == 0) {
                    this.free.remove(i);
                }
                return n2;
            }
            return -1;
        }

        void give(int n, int n2) {
            int n3;
            for (n3 = 0; n3 < this.free.size() && this.free.get(n3)[0] < n; ++n3) {
            }
            this.free.add(n3, new int[]{n, n2});
            if (n3 + 1 < this.free.size() && n + n2 == this.free.get(n3 + 1)[0]) {
                int[] nArray = this.free.get(n3);
                nArray[1] = nArray[1] + this.free.remove(n3 + 1)[1];
            }
            if (n3 > 0 && this.free.get(n3 - 1)[0] + this.free.get(n3 - 1)[1] == n) {
                int[] nArray = this.free.get(n3 - 1);
                nArray[1] = nArray[1] + this.free.remove(n3)[1];
            }
        }

        boolean empty(int n) {
            return this.free.size() == 1 && this.free.get(0)[0] == 0 && this.free.get(0)[1] == n;
        }
    }
}

