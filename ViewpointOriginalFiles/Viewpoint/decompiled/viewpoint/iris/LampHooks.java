/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.List;
import java.util.Set;
import viewpoint.iris.GlslDeclarations;
import viewpoint.iris.GlslTokens;

final class LampHooks {
    static final Set<String> NAMES = Set.of("blocklightCol", "blockLightCol", "blocklight_color");
    static final Set<String> FIRST_ARGUMENT = Set.of("doBlockLightLighting");

    private LampHooks() {
    }

    static int tint(List<GlslTokens.Token> list) {
        GlslDeclarations glslDeclarations = GlslDeclarations.read(list);
        int n = 0;
        for (int i = 0; i < list.size(); ++i) {
            GlslTokens.Token token = list.get(i);
            if (!token.isName() || !glslDeclarations.insideFunction(i)) continue;
            if (NAMES.contains(token.text) && LampHooks.isRead(list, i)) {
                token.text = "vp_lampTint(" + token.text + ")";
                ++n;
                continue;
            }
            if (!FIRST_ARGUMENT.contains(token.text) || LampHooks.isDefinition(glslDeclarations, i)) continue;
            n += LampHooks.firstArgument(list, i);
        }
        return n;
    }

    private static boolean isRead(List<GlslTokens.Token> list, int n) {
        int n2 = GlslTokens.previous(list, n);
        int n3 = GlslTokens.next(list, n + 1);
        if (n2 >= 0 && (list.get(n2).is(".") || list.get(n2).isName())) {
            return false;
        }
        return n3 >= list.size() || !list.get(n3).is("=") && !list.get(n3).is("(");
    }

    private static int firstArgument(List<GlslTokens.Token> list, int n) {
        int n2 = GlslTokens.next(list, n + 1);
        if (n2 >= list.size() || !list.get(n2).is("(")) {
            return 0;
        }
        int n3 = GlslTokens.close(list, n2);
        int n4 = 0;
        for (int i = n2 + 1; i < n3; ++i) {
            GlslTokens.Token token = list.get(i);
            if (token.is("(") || token.is("[")) {
                ++n4;
                continue;
            }
            if (token.is(")") || token.is("]")) {
                --n4;
                continue;
            }
            if (!token.is(",") || n4 != 0) continue;
            int n5 = GlslTokens.next(list, n2 + 1);
            list.get((int)n5).text = "vp_lampTint(" + list.get((int)n5).text;
            token.text = ")" + token.text;
            return 1;
        }
        return 0;
    }

    private static boolean isDefinition(GlslDeclarations glslDeclarations, int n) {
        for (GlslDeclarations.Function function : glslDeclarations.functions) {
            if (function.nameToken != n) continue;
            return true;
        }
        return false;
    }
}

