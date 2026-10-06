/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.Collection;
import java.util.List;
import viewpoint.iris.GlslDeclarations;
import viewpoint.iris.GlslTokens;

final class GlobalInitializers {
    private GlobalInitializers() {
    }

    static void defer(List<GlslTokens.Token> list) {
        StringBuilder stringBuilder = new StringBuilder();
        for (GlslTokens.Token object2 : list) {
            stringBuilder.append(object2.text);
        }
        List<GlslTokens.Token> list2 = GlslTokens.lex(stringBuilder.toString());
        list.clear();
        list.addAll((Collection<GlslTokens.Token>)list2);
        GlslDeclarations glslDeclarations = GlslDeclarations.read(list);
        GlslDeclarations.Function function = null;
        for (GlslDeclarations.Function function2 : glslDeclarations.functions) {
            function = function2.name.equals("vp_packMain") ? function2 : function;
        }
        if (function == null) {
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        for (GlslDeclarations.Declaration declaration : glslDeclarations.declarations) {
            if (declaration.first >= function.open || glslDeclarations.insideFunction(declaration.first) || GlobalInitializers.constant(list, declaration)) continue;
            stringBuilder2.append(GlobalInitializers.initializers(list, declaration));
        }
        if (!stringBuilder2.isEmpty()) {
            GlslTokens.Token token = list.get(function.open);
            token.text = token.text + String.valueOf(stringBuilder2);
        }
    }

    private static boolean constant(List<GlslTokens.Token> list, GlslDeclarations.Declaration declaration) {
        for (String string : declaration.qualifiers) {
            if (!GlslDeclarations.STORAGE.contains(string)) continue;
            return true;
        }
        for (int i = declaration.first; i <= declaration.last; ++i) {
            int n = GlslTokens.next(list, i + 1);
            if (!list.get(i).is("[") || n > declaration.last || !list.get(n).is("]")) continue;
            return true;
        }
        return false;
    }

    private static String initializers(List<GlslTokens.Token> list, GlslDeclarations.Declaration declaration) {
        StringBuilder stringBuilder = new StringBuilder();
        int n = 0;
        String string = null;
        int n2 = -1;
        for (int i = declaration.first; i <= declaration.last; ++i) {
            boolean bl;
            GlslTokens.Token token = list.get(i);
            boolean bl2 = bl = n == 0;
            n += token.is("(") || token.is("[") || token.is("{") ? 1 : (token.is(")") || token.is("]") || token.is("}") ? -1 : 0);
            if (bl && token.isName() && declaration.names.contains(token.text) && n2 < 0) {
                string = token.text;
                continue;
            }
            if (bl && token.is("=") && n2 < 0) {
                n2 = i;
                continue;
            }
            if (!bl || !token.is(",") && !token.is(";") || n2 < 0) continue;
            StringBuilder stringBuilder2 = new StringBuilder();
            for (int j = n2 + 1; j < i; ++j) {
                stringBuilder2.append(list.get((int)j).text);
                list.get((int)j).text = "";
            }
            list.get((int)n2).text = "";
            stringBuilder.append(' ').append(string).append(" =").append((CharSequence)stringBuilder2).append(';');
            n2 = -1;
        }
        return stringBuilder.toString();
    }
}

