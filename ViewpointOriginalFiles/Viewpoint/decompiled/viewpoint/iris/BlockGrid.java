/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.List;
import java.util.Set;
import viewpoint.iris.GlslTokens;

final class BlockGrid {
    private static final Set<String> BEFORE = Set.of("=", "(", ",", "return");
    private static final Set<String> NOT_AFTER = Set.of(".", "[", "(");

    private BlockGrid() {
    }

    static int unround(List<GlslTokens.Token> list) {
        int n = 0;
        for (int i = 0; i < list.size(); ++i) {
            if (!list.get(i).isName() || !list.get(i).is("floor") || !BlockGrid.unround(list, i)) continue;
            ++n;
        }
        return n;
    }

    private static boolean unround(List<GlslTokens.Token> list, int n) {
        int n2 = GlslTokens.previous(list, n);
        int n3 = GlslTokens.next(list, n + 1);
        if (n2 < 0 || !BEFORE.contains(list.get((int)n2).text) || !BlockGrid.is(list, n3, "(")) {
            return false;
        }
        int n4 = GlslTokens.close(list, n3);
        int n5 = GlslTokens.previous(list, n4);
        int n6 = GlslTokens.previous(list, n5);
        int n7 = GlslTokens.previous(list, n6);
        if (!(n4 >= 0 && n5 >= 0 && BlockGrid.isHalf(list.get(n5)) && BlockGrid.is(list, n6, "+") && n7 >= 0 && list.get(n7).isName() && BlockGrid.is(list, GlslTokens.previous(list, n7), "+"))) {
            return false;
        }
        int n8 = GlslTokens.next(list, n4 + 1);
        int n9 = GlslTokens.next(list, n8 + 1);
        int n10 = GlslTokens.next(list, n9 + 1);
        if (!BlockGrid.is(list, n8, "-") || !BlockGrid.is(list, n9, list.get((int)n7).text) || n10 < list.size() && NOT_AFTER.contains(list.get((int)n10).text)) {
            return false;
        }
        list.get((int)n).text = "";
        list.get((int)n6).text = "";
        list.get((int)n5).text = "";
        return true;
    }

    private static boolean is(List<GlslTokens.Token> list, int n, String string) {
        return n >= 0 && n < list.size() && list.get(n).is(string);
    }

    private static boolean isHalf(GlslTokens.Token token) {
        if (token.kind != GlslTokens.Kind.NUMBER) {
            return false;
        }
        String string = token.text.endsWith("f") || token.text.endsWith("F") ? token.text.substring(0, token.text.length() - 1) : token.text;
        try {
            return Double.parseDouble(string) == 0.5;
        }
        catch (NumberFormatException numberFormatException) {
            return false;
        }
    }
}

