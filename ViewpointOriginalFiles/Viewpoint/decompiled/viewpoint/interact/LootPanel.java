/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 *  zombie.core.SpriteRenderer
 *  zombie.core.Translator
 *  zombie.ui.TextManager
 *  zombie.ui.UIFont
 */
package viewpoint.interact;

import java.util.List;
import java.util.Locale;
import viewpoint.interact.InteractActions;
import viewpoint.interact.LootMenu;
import viewpoint.interact.LootRows;
import viewpoint.platform.KeyBind;
import viewpoint.platform.KeyChord;
import viewpoint.platform.Keys;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

public final class LootPanel {
    private static final UIFont FONT = UIFont.Small;
    private static final int GAP = 48;
    private static final int PAD = 8;
    private static final int MIN_WIDTH = 220;
    private static final int MAX_WIDTH = 360;
    private static final int COLUMN = 16;
    private static final String CUT = "...";
    private static final int SHOWN = 10;
    private static final float BACK = 0.6f;
    private static final float LIT = 0.18f;
    private static final float DIM = 0.45f;
    private static final float FILL = 0.45f;
    private static final float[] TITLE = new float[]{1.0f, 0.84f, 0.47f};
    private static final float[] TEXT = new float[]{0.86f, 0.86f, 0.86f};
    private static final float[] HINT = new float[]{0.7f, 0.7f, 0.7f};
    private static final float[] GREEN = new float[]{0.3f, 0.85f, 0.4f};
    private static final int PANEL_ROUND = 3;
    private static final int ROW_ROUND = 5;
    private static boolean failed;
    private static String hint;
    private static List<KeyChord> hintTake;
    private static List<KeyChord> hintAll;
    private static List<KeyChord> hintWindow;
    private static boolean hintPress;
    private static boolean hintAction;
    private static boolean hintLoot;

    public static void draw() {
        if (!LootMenu.panelDue() || failed) {
            return;
        }
        try {
            LootPanel.render();
        }
        catch (RuntimeException runtimeException) {
            failed = true;
            System.out.println("[Viewpoint] loot menu panel off after an error: " + String.valueOf(runtimeException));
        }
    }

    private static void render() {
        int n;
        int n2;
        TextManager textManager = TextManager.instance;
        LootRows lootRows = LootMenu.rows;
        int n3 = textManager.getFontHeight(FONT) + 6;
        int n4 = n3 - 4;
        String string = LootPanel.hint(lootRows);
        int n5 = Math.max(220, textManager.MeasureStringX(FONT, string) + 16);
        for (n2 = n = Math.max(0, Math.min(lootRows.selected - 5, lootRows.rows.size() - 10)); n2 < lootRows.rows.size() && n2 - n < 10; ++n2) {
            n5 = Math.max(n5, LootPanel.rowWidth(textManager, lootRows.rows.get(n2), n4) + 16);
        }
        String string2 = LootMenu.locked() ? "Locked" : null;
        int n6 = n2 - n + (string2 != null ? 1 : 0) + (string.isEmpty() ? 0 : 1);
        int n7 = n6 * n3 + 16;
        int n8 = Core.getInstance().getScreenWidth();
        int n9 = Core.getInstance().getScreenHeight();
        int n10 = Math.min(n8 / 2 + 48, n8 - n5);
        int n11 = Math.max(0, n9 / 2 - n7 / 2);
        LootPanel.rounded(n10, n11, n5, n7, n3 / 3, 0.0f, 0.0f, 0.0f, 0.6f);
        int n12 = n11 + 8;
        for (int i = n; i < n2; ++i) {
            LootPanel.row(textManager, lootRows, i, n10, n12, n5, n4);
            n12 += n3;
        }
        if (string2 != null) {
            textManager.DrawString(FONT, (double)(n10 + 8 + n4 + 8), (double)n12, string2, (double)TEXT[0], (double)TEXT[1], (double)TEXT[2], (double)0.45f);
            n12 += n3;
        }
        textManager.DrawString(FONT, (double)(n10 + 8), (double)n12, string, (double)HINT[0], (double)HINT[1], (double)HINT[2], 1.0);
    }

    private static void row(TextManager textManager, LootRows lootRows, int n, int n2, int n3, int n4, int n5) {
        float f;
        float f2;
        LootRows.Row row = lootRows.rows.get(n);
        if (row.heading != null) {
            textManager.DrawString(FONT, (double)(n2 + 8), (double)n3, LootPanel.label(textManager, row, n5), (double)TITLE[0], (double)TITLE[1], (double)TITLE[2], 1.0);
            return;
        }
        int n6 = n4 - 4;
        int n7 = n5 + 4;
        int n8 = n7 / 5;
        if (n == lootRows.selected) {
            LootPanel.rounded(n2 + 2, n3 - 2, n6, n7, n8, 1.0f, 1.0f, 1.0f, 0.18f);
        }
        if ((f2 = LootPanel.progress(row)) > 0.0f) {
            LootPanel.rounded(n2 + 2, n3 - 2, Math.round((float)n6 * f2), n7, n8, GREEN[0], GREEN[1], GREEN[2], 0.45f);
        }
        if (row.plain()) {
            float f3;
            float f4 = f3 = row.enabled ? 1.0f : 0.45f;
            if (row.icon != null) {
                SpriteRenderer.instance.renderi(row.icon, n2 + 8, n3, n5, n5, 1.0f, 1.0f, 1.0f, f3, null);
            }
            int n9 = n2 + 8 + LootPanel.besides(textManager, row, n5);
            textManager.DrawString(FONT, (double)n9, (double)n3, LootPanel.label(textManager, row, n5), (double)TEXT[0], (double)TEXT[1], (double)TEXT[2], (double)f3);
            return;
        }
        float f5 = f = LootPanel.left(row) > 0 || f2 > 0.0f ? 1.0f : 0.45f;
        if (row.icon != null) {
            SpriteRenderer.instance.renderi(row.icon, n2 + 8, n3, n5, n5, 1.0f, 1.0f, 1.0f, f, null);
        }
        textManager.DrawString(FONT, (double)(n2 + 8 + n5 + 8), (double)n3, LootPanel.label(textManager, row, n5), (double)TEXT[0], (double)TEXT[1], (double)TEXT[2], (double)f);
        String string = LootPanel.weight(row);
        int n10 = n2 + n4 - 8 - textManager.MeasureStringX(FONT, string);
        textManager.DrawString(FONT, (double)n10, (double)n3, string, (double)HINT[0], (double)HINT[1], (double)HINT[2], (double)f);
    }

    private static int rowWidth(TextManager textManager, LootRows.Row row, int n) {
        return LootPanel.besides(textManager, row, n) + textManager.MeasureStringX(FONT, LootPanel.label(textManager, row, n));
    }

    private static int besides(TextManager textManager, LootRows.Row row, int n) {
        if (row.heading != null || row.action >= 0) {
            return 0;
        }
        if (row.plain()) {
            return n + 8;
        }
        return n + 8 + 16 + textManager.MeasureStringX(FONT, LootPanel.weight(row));
    }

    private static float progress(LootRows.Row row) {
        if (row.action >= 0) {
            return InteractActions.progress(row.action);
        }
        float f = 0.0f;
        for (int i = 0; i < row.items.size(); ++i) {
            f = Math.max(f, row.items.get(i).getJobDelta());
        }
        return Math.min(f, 1.0f);
    }

    private static void rounded(int n, int n2, int n3, int n4, int n5, float f, float f2, float f3, float f4) {
        if (n3 <= 0 || n4 <= 0) {
            return;
        }
        int n6 = Math.min(n5, Math.min(n3, n4) / 2);
        SpriteRenderer.instance.renderi(null, n, n2 + n6, n3, n4 - 2 * n6, f, f2, f3, f4, null);
        for (int i = 0; i < n6; ++i) {
            float f5 = (float)(n6 - i) - 0.5f;
            float f6 = (float)n6 - (float)Math.sqrt((float)(n6 * n6) - f5 * f5);
            int n7 = (int)Math.ceil(f6);
            float f7 = (float)n7 - f6;
            LootPanel.strip(n, n2 + i, n3, n7, f7, f, f2, f3, f4);
            LootPanel.strip(n, n2 + n4 - 1 - i, n3, n7, f7, f, f2, f3, f4);
        }
    }

    private static void strip(int n, int n2, int n3, int n4, float f, float f2, float f3, float f4, float f5) {
        SpriteRenderer.instance.renderi(null, n + n4, n2, n3 - 2 * n4, 1, f2, f3, f4, f5, null);
        if (n4 > 0 && f > 0.0f) {
            SpriteRenderer.instance.renderi(null, n + n4 - 1, n2, 1, 1, f2, f3, f4, f5 * f, null);
            SpriteRenderer.instance.renderi(null, n + n3 - n4, n2, 1, 1, f2, f3, f4, f5 * f, null);
        }
    }

    private static int left(LootRows.Row row) {
        return row.items.size() - LootRows.asked(row, LootMenu.asked);
    }

    private static String label(TextManager textManager, LootRows.Row row, int n) {
        int n2 = LootPanel.left(row);
        if (row.label == null || row.labelLeft != n2) {
            Object object = row.items.size() > 1 ? " (" + n2 + ")" : "";
            int n3 = 344 - LootPanel.besides(textManager, row, n) - textManager.MeasureStringX(FONT, (String)object);
            row.labelLeft = n2;
            row.label = LootPanel.fit(textManager, row.heading != null ? row.heading : row.name, n3) + (String)object;
        }
        return row.label;
    }

    private static String fit(TextManager textManager, String string, int n) {
        if (textManager.MeasureStringX(FONT, string) <= n) {
            return string;
        }
        for (int i = string.length() - 1; i > 0; --i) {
            String string2 = string.substring(0, i).stripTrailing() + CUT;
            if (textManager.MeasureStringX(FONT, string2) > n) continue;
            return string2;
        }
        return CUT;
    }

    private static String weight(LootRows.Row row) {
        if (row.weightText == null) {
            row.weightText = String.format(Locale.ROOT, "%.2f", Float.valueOf(row.weight));
        }
        return row.weightText;
    }

    private static String hint(LootRows lootRows) {
        List<KeyChord> list = Keys.LOOT_TAKE.chords();
        List<KeyChord> list2 = Keys.LOOT_TAKE_ALL.chords();
        List<KeyChord> list3 = Keys.LOOT_WINDOW.chords();
        boolean bl = LootMenu.interacts();
        boolean bl2 = lootRows.action() >= 0;
        boolean bl3 = LootMenu.loot();
        if (list != hintTake || list2 != hintAll || list3 != hintWindow || bl != hintPress || bl2 != hintAction || bl3 != hintLoot) {
            hintTake = list;
            hintAll = list2;
            hintWindow = list3;
            hintPress = bl;
            hintAction = bl2;
            hintLoot = bl3;
            String string = bl ? LootPanel.key(Keys.LOOT_TAKE, bl2 ? "IGUI_Controller_Interact" : "ContextMenu_Grab") : (hint = "");
            if (bl3) {
                hint = LootPanel.join(LootPanel.join(hint, LootPanel.key(Keys.LOOT_TAKE_ALL, "IGUI_invpage_Loot_all")), LootPanel.key(Keys.LOOT_WINDOW, "IGUI_Controller_Loot"));
            }
        }
        return hint;
    }

    private static String key(KeyBind keyBind, String string) {
        return keyBind.bound() ? "[" + keyBind.display() + "] " + Translator.getText((String)string, (Object[])new Object[0]) : "";
    }

    private static String join(String string, String string2) {
        return string.isEmpty() || string2.isEmpty() ? string + string2 : string + "   " + string2;
    }

    private LootPanel() {
    }

    static {
        hint = "";
    }
}

