/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import viewpoint.iris.GlslDeclarations;
import viewpoint.iris.GlslTokens;

final class LightmapFlow {
    private LightmapFlow() {
    }

    static Found find(List<GlslTokens.Token> list, GlslDeclarations glslDeclarations, Set<String> set) {
        return LightmapFlow.find(list, glslDeclarations, set, false);
    }

    static Found findColour(List<GlslTokens.Token> list, GlslDeclarations glslDeclarations, Set<String> set) {
        return LightmapFlow.find(list, glslDeclarations, set, true);
    }

    private static Found find(List<GlslTokens.Token> list, GlslDeclarations glslDeclarations, Set<String> set, boolean bl) {
        HashSet<String> hashSet = new HashSet<String>(set);
        HashSet<String> hashSet2 = new HashSet<String>();
        for (boolean bl2 = true; bl2; bl2 |= LightmapFlow.assignments(list, glslDeclarations, hashSet, hashSet2)) {
            bl2 = false;
            for (GlslDeclarations.Function function : glslDeclarations.functions) {
                if (hashSet2.contains(function.name) || !LightmapFlow.reads(list, function.open, function.close, hashSet, hashSet2)) continue;
                hashSet2.add(function.name);
                bl2 = true;
            }
        }
        Object object = null;
        int n = -1;
        for (GlslDeclarations.Declaration declaration : glslDeclarations.declarations) {
            if (!declaration.has("out") && !declaration.has("varying")) continue;
            for (String string : declaration.names) {
                int n2;
                String string2 = LightmapFlow.components(list, glslDeclarations, string, hashSet, hashSet2, declaration.type, bl);
                if (string2 == null || (n2 = bl ? LightmapFlow.colourScore(string) : LightmapFlow.score(string)) <= n) continue;
                n = n2;
                object = new Found(string, declaration.type, string2, declaration.interpolation());
            }
        }
        return object;
    }

    private static String components(List<GlslTokens.Token> list, GlslDeclarations glslDeclarations, String string, Set<String> set, Set<String> set2, String string2, boolean bl) {
        for (int i = 0; i < list.size(); ++i) {
            int n;
            GlslTokens.Token token = list.get(i);
            if (!token.isName() || !token.is(string) || !glslDeclarations.insideFunction(i)) continue;
            int n2 = GlslTokens.next(list, i + 1);
            String string3 = "";
            if (n2 < list.size() && list.get(n2).is(".")) {
                n = GlslTokens.next(list, n2 + 1);
                string3 = list.get((int)n).text;
                n2 = GlslTokens.next(list, n + 1);
            }
            if (n2 >= list.size() || !list.get(n2).is("=") || !LightmapFlow.reads(list, n2 + 1, n = LightmapFlow.statementEnd(list, n2), set, set2)) continue;
            if (bl) {
                boolean bl2;
                boolean bl3 = bl2 = string3.isEmpty() || string3.equals("rgb") || string3.equals("xyz");
                if (!bl2 || !string2.equals("vec3") && !string2.equals("vec4")) continue;
                return "rgb";
            }
            if (string2.equals("vec2") && (string3.isEmpty() || string3.equals("xy"))) {
                return "xy";
            }
            if (!string2.equals("vec4") || string3.length() != 2) continue;
            return string3;
        }
        return null;
    }

    private static boolean assignments(List<GlslTokens.Token> list, GlslDeclarations glslDeclarations, Set<String> set, Set<String> set2) {
        boolean bl = false;
        for (int i = 0; i < list.size(); ++i) {
            if (!list.get(i).is("=") || !glslDeclarations.insideFunction(i)) continue;
            int n = GlslTokens.previous(list, i);
            while (n >= 0 && !list.get(n).isName()) {
                n = GlslTokens.previous(list, n);
            }
            int n2 = n;
            while (n2 >= 2 && list.get(GlslTokens.previous(list, n2)).is(".")) {
                n2 = GlslTokens.previous(list, GlslTokens.previous(list, n2));
            }
            if (n2 < 0 || set.contains(list.get((int)n2).text) || !LightmapFlow.reads(list, i + 1, LightmapFlow.statementEnd(list, i), set, set2)) continue;
            set.add(list.get((int)n2).text);
            bl = true;
        }
        return bl;
    }

    private static int statementEnd(List<GlslTokens.Token> list, int n) {
        int n2 = 0;
        for (int i = n; i < list.size(); ++i) {
            GlslTokens.Token token = list.get(i);
            if (token.is("(") || token.is("[")) {
                ++n2;
                continue;
            }
            if (token.is(")") || token.is("]")) {
                --n2;
                continue;
            }
            if (!token.is(";") && n2 >= 0 && (!token.is(",") || n2 != 0)) continue;
            return i;
        }
        return list.size();
    }

    private static boolean reads(List<GlslTokens.Token> list, int n, int n2, Set<String> set, Set<String> set2) {
        for (int i = n; i < n2 && i < list.size(); ++i) {
            int n3;
            int n4;
            GlslTokens.Token token = list.get(i);
            if (!token.isName() || (n4 = GlslTokens.previous(list, i)) >= 0 && list.get(n4).is(".")) continue;
            if (set.contains(token.text)) {
                return true;
            }
            if (!set2.contains(token.text) || (n3 = GlslTokens.next(list, i + 1)) >= list.size() || !list.get(n3).is("(")) continue;
            return true;
        }
        return false;
    }

    private static int colourScore(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        return string2.contains("glcolor") || string2.equals("color") ? 3 : (string2.contains("col") ? 2 : (string2.contains("tint") ? 1 : 0));
    }

    private static int score(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        return string2.startsWith("lm") || string2.contains("lightmap") ? 3 : (string2.contains("light") ? 2 : (string2.contains("lm") ? 1 : 0));
    }

    record Found(String name, String type, String components, String interpolation) {
    }
}

