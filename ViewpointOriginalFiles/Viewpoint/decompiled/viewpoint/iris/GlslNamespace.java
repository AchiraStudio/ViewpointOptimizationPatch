/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.HashSet;
import java.util.List;
import viewpoint.iris.GlslDeclarations;
import viewpoint.iris.GlslTokens;

public final class GlslNamespace {
    public static final String PREFIX = "vp_";

    private GlslNamespace() {
    }

    /*
     * WARNING - void declaration
     */
    public static String prefixed(String string) {
        void var5_14;
        int n;
        void var5_12;
        List<GlslTokens.Token> list = GlslTokens.lex(string);
        GlslDeclarations glslDeclarations = GlslDeclarations.read(list);
        HashSet<String> hashSet = new HashSet<String>();
        for (GlslDeclarations.Function object : glslDeclarations.functions) {
            if (object.name.equals("main")) continue;
            hashSet.add(object.name);
        }
        for (GlslDeclarations.Declaration n2 : glslDeclarations.declarations) {
            if (n2.has("uniform") || glslDeclarations.insideFunction(n2.first)) continue;
            hashSet.addAll(n2.names);
        }
        int n3 = 0;
        while (n3 + 2 < list.size()) {
            int n2;
            if (list.get(n3).is("struct") && (n2 = GlslTokens.next(list, n3 + 1)) < list.size() && list.get(n2).isName()) {
                hashSet.add(list.get((int)n2).text);
            }
            ++n3;
        }
        boolean[] blArray = new boolean[list.size()];
        boolean bl = false;
        while (var5_12 < list.size()) {
            int n4;
            if (list.get((int)var5_12).is("struct") && (n4 = GlslTokens.next(list, GlslTokens.next(list, (int)(var5_12 + true)) + 1)) < list.size() && list.get(n4).is("{")) {
                n = GlslTokens.close(list, n4);
                for (int i = n4; i <= n && i >= 0; ++i) {
                    blArray[i] = true;
                }
            }
            ++var5_12;
        }
        boolean bl2 = false;
        while (var5_14 < list.size()) {
            GlslTokens.Token token = list.get((int)var5_14);
            if (!(!token.isName() || !hashSet.contains(token.text) || token.text.startsWith(PREFIX) || blArray[var5_14] || (n = GlslTokens.previous(list, (int)var5_14)) >= 0 && list.get(n).is("."))) {
                token.text = PREFIX + token.text;
            }
            ++var5_14;
        }
        return GlslTokens.join(list);
    }
}

