/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Translator
 *  zombie.core.textures.Texture
 *  zombie.inventory.InventoryItem
 *  zombie.inventory.ItemContainer
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.objects.IsoWorldInventoryObject
 */
package viewpoint.interact;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import viewpoint.interact.InteractActions;
import viewpoint.interact.LootTargets;
import zombie.characters.IsoPlayer;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoWorldInventoryObject;

final class LootRows {
    private static final Comparator<Row> BY_NAME = Comparator.comparing(row -> row.name, String.CASE_INSENSITIVE_ORDER);
    private static final String EMPTY_ICON = "media/ui/LootableMaps/map_garbage.png";
    private static String empty;
    private static String interact;
    final ArrayList<Row> rows = new ArrayList();
    int selected = -1;
    private final ArrayList<Row> spare = new ArrayList();
    private final HashMap<String, Row> byName = new HashMap();
    private final ArrayList<InventoryItem> ground = new ArrayList();
    private long signature;
    private boolean built;

    LootRows() {
    }

    void clear() {
        this.recycle();
        this.selected = -1;
        this.built = false;
    }

    void refresh(IsoPlayer isoPlayer, LootTargets.Target target) {
        long l = LootRows.signature(target);
        if (this.built && l == this.signature) {
            return;
        }
        String string = this.selected >= 0 && this.selected < this.rows.size() ? this.rows.get((int)this.selected).name : null;
        int n = this.selected;
        this.build(isoPlayer, target);
        this.signature = l;
        this.built = true;
        this.selected = this.select(string, n);
    }

    void move(int n) {
        for (int i = 0; i < Math.abs(n); ++i) {
            int n2 = this.nextSelectable(this.selected, n > 0 ? 1 : -1);
            if (n2 < 0) {
                return;
            }
            this.selected = n2;
        }
    }

    int action() {
        return this.selected >= 0 && this.selected < this.rows.size() ? this.rows.get((int)this.selected).action : -1;
    }

    InventoryItem next(Set<InventoryItem> set) {
        if (this.selected < 0 || this.selected >= this.rows.size()) {
            return null;
        }
        for (InventoryItem inventoryItem : this.rows.get((int)this.selected).items) {
            if (set.contains(inventoryItem)) continue;
            return inventoryItem;
        }
        return null;
    }

    void all(Set<InventoryItem> set, List<InventoryItem> list) {
        for (Row row : this.rows) {
            for (InventoryItem inventoryItem : row.items) {
                if (set.contains(inventoryItem)) continue;
                list.add(inventoryItem);
            }
        }
    }

    static int asked(Row row, Set<InventoryItem> set) {
        int n = 0;
        for (InventoryItem inventoryItem : row.items) {
            n += set.contains(inventoryItem) ? 1 : 0;
        }
        return n;
    }

    static String title(ItemContainer itemContainer) {
        String string;
        String string2 = string = itemContainer.getVehiclePart() == null ? null : Translator.getTextOrNull((String)("IGUI_VehiclePart" + itemContainer.getType()), (Object[])new Object[0]);
        if (string != null) {
            return string;
        }
        if (itemContainer.getCustomName() != null) {
            return itemContainer.getCustomName();
        }
        String string3 = Translator.getTextOrNull((String)("IGUI_ContainerTitle_" + itemContainer.getType()), (Object[])new Object[0]);
        return string3 != null ? string3 : itemContainer.getType();
    }

    static String groundTitle() {
        String string = Translator.getTextOrNull((String)"IGUI_ContainerTitle_floor", (Object[])new Object[0]);
        return string != null ? string : "Ground";
    }

    private void build(IsoPlayer isoPlayer, LootTargets.Target target) {
        this.recycle();
        if (target.kind == 0) {
            this.heading(LootRows.groundTitle());
            this.group(isoPlayer, this.groundItems(target.square));
            this.ground.clear();
        }
        for (ItemContainer itemContainer : target.containers) {
            this.heading(LootRows.title(itemContainer));
            this.group(isoPlayer, itemContainer.getItems());
        }
        this.actions();
    }

    private void actions() {
        if (InteractActions.count() == 0) {
            return;
        }
        if (interact == null) {
            interact = Translator.getText((String)"IGUI_Controller_Interact", (Object[])new Object[0]);
        }
        this.heading(InteractActions.title() != null ? InteractActions.title() : interact);
        for (int i = 0; i < InteractActions.count(); ++i) {
            Row row = this.row();
            row.name = InteractActions.label(i);
            row.action = i;
            row.enabled = InteractActions.enabled(i);
            this.rows.add(row);
        }
    }

    private void heading(String string) {
        Row row = this.row();
        row.heading = string;
        this.rows.add(row);
    }

    private void group(IsoPlayer isoPlayer, List<InventoryItem> list) {
        int n = this.rows.size();
        this.byName.clear();
        for (int i = 0; i < list.size(); ++i) {
            InventoryItem inventoryItem = list.get(i);
            if (inventoryItem == null || inventoryItem.isHidden()) continue;
            String string = inventoryItem.getName(isoPlayer);
            Row row = this.byName.get(string);
            if (row == null) {
                row = this.row();
                row.name = string;
                row.icon = inventoryItem.getTex();
                this.byName.put(string, row);
                this.rows.add(row);
            }
            row.items.add(inventoryItem);
            row.weight += inventoryItem.getUnequippedWeight();
        }
        this.rows.subList(n, this.rows.size()).sort(BY_NAME);
        this.byName.clear();
        if (this.rows.size() == n) {
            if (empty == null) {
                empty = Translator.getText((String)"ContextMenu_Empty", (Object[])new Object[0]);
            }
            Row row = this.row();
            row.name = empty;
            row.icon = Texture.getSharedTexture((String)EMPTY_ICON);
            row.enabled = false;
            this.rows.add(row);
        }
    }

    private List<InventoryItem> groundItems(IsoGridSquare isoGridSquare) {
        this.ground.clear();
        ArrayList arrayList = isoGridSquare.getWorldObjects();
        for (int i = 0; i < arrayList.size(); ++i) {
            if (((IsoWorldInventoryObject)arrayList.get(i)).getItem() == null) continue;
            this.ground.add(((IsoWorldInventoryObject)arrayList.get(i)).getItem());
        }
        return this.ground;
    }

    private static long signature(LootTargets.Target target) {
        long l = (long)target.kind * 31L + (long)InteractActions.version();
        if (target.kind == 0) {
            ArrayList arrayList = target.square.getWorldObjects();
            for (int i = 0; i < arrayList.size(); ++i) {
                InventoryItem inventoryItem = ((IsoWorldInventoryObject)arrayList.get(i)).getItem();
                l = l * 31L + (long)(inventoryItem == null ? 0 : inventoryItem.getID());
            }
            return l * 31L + (long)arrayList.size();
        }
        for (ItemContainer itemContainer : target.containers) {
            ArrayList arrayList = itemContainer.getItems();
            for (int i = 0; i < arrayList.size(); ++i) {
                l = l * 31L + (long)((InventoryItem)arrayList.get(i)).getID();
            }
            l = l * 31L + (long)arrayList.size();
        }
        return l;
    }

    int select(String string, int n) {
        int n2;
        for (n2 = 0; string != null && n2 < this.rows.size(); ++n2) {
            if (!this.rows.get(n2).selectable() || !string.equals(this.rows.get((int)n2).name)) continue;
            return n2;
        }
        n2 = Math.max(0, Math.min(n, this.rows.size() - 1));
        if (n2 < this.rows.size() && this.rows.get(n2).selectable()) {
            return n2;
        }
        int n3 = this.nextSelectable(n2, 1);
        return n3 >= 0 ? n3 : this.nextSelectable(n2, -1);
    }

    private int nextSelectable(int n, int n2) {
        for (int i = n + n2; i >= 0 && i < this.rows.size(); i += n2) {
            if (!this.rows.get(i).selectable()) continue;
            return i;
        }
        return -1;
    }

    private Row row() {
        Row row = this.spare.isEmpty() ? new Row() : this.spare.remove(this.spare.size() - 1);
        row.heading = null;
        row.name = null;
        row.icon = null;
        row.weight = 0.0f;
        row.action = -1;
        row.enabled = true;
        row.items.clear();
        row.label = null;
        row.weightText = null;
        return row;
    }

    private void recycle() {
        for (Row row : this.rows) {
            row.items.clear();
            row.icon = null;
            this.spare.add(row);
        }
        this.rows.clear();
    }

    static final class Row {
        String heading;
        String name;
        Texture icon;
        float weight;
        int action = -1;
        boolean enabled = true;
        final ArrayList<InventoryItem> items = new ArrayList();
        String label;
        String weightText;
        int labelLeft;

        Row() {
        }

        boolean selectable() {
            return this.heading == null && this.enabled;
        }

        boolean plain() {
            return this.heading == null && this.items.isEmpty();
        }
    }
}

