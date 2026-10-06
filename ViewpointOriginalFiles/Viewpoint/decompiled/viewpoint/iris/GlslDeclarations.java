/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import viewpoint.iris.GlslTokens;

final class GlslDeclarations {
    static final Set<String> STORAGE = Set.of("in", "out", "varying", "attribute", "uniform", "const", "buffer", "shared");
    static final Set<String> INTERPOLATION = Set.of("flat", "smooth", "noperspective", "centroid", "sample", "invariant");
    private static final Set<String> OTHER_QUALIFIERS = Set.of("highp", "mediump", "lowp", "precise", "coherent", "volatile", "restrict", "readonly", "writeonly");
    final List<Declaration> declarations = new ArrayList<Declaration>();
    final List<Function> functions = new ArrayList<Function>();

    GlslDeclarations() {
    }

    static GlslDeclarations read(List<GlslTokens.Token> list) {
        GlslDeclarations glslDeclarations = new GlslDeclarations();
        int n = GlslTokens.next(list, 0);
        while (n < list.size()) {
            n = glslDeclarations.statement(list, n);
            n = GlslTokens.next(list, n);
        }
        return glslDeclarations;
    }

    private int statement(List<GlslTokens.Token> list, int n) {
        int n2 = n;
        for (int i = n; i < list.size(); ++i) {
            int n3;
            GlslTokens.Token token = list.get(i);
            if (token.kind != GlslTokens.Kind.PUNCT) continue;
            if (token.is(";")) {
                this.declaration(list, n2, i);
                return i + 1;
            }
            if (token.is("{")) {
                n3 = GlslTokens.close(list, i);
                if (n3 < 0) {
                    return list.size();
                }
                this.function(list, n2, i, n3);
                int n4 = GlslTokens.next(list, n3 + 1);
                if (n4 < list.size() && !GlslDeclarations.isFunctionHead(list, n2, i)) {
                    for (int j = n4; j < list.size(); ++j) {
                        if (!list.get(j).is(";")) continue;
                        return j + 1;
                    }
                }
                return n3 + 1;
            }
            if (!token.is("(") && !token.is("[")) continue;
            n3 = GlslTokens.close(list, i);
            i = n3 < 0 ? list.size() : n3;
        }
        return list.size();
    }

    private static boolean isFunctionHead(List<GlslTokens.Token> list, int n, int n2) {
        int n3 = GlslTokens.previous(list, n2);
        return n3 >= n && list.get(n3).is(")");
    }

    private void function(List<GlslTokens.Token> list, int n, int n2, int n3) {
        int n4;
        int n5 = GlslTokens.previous(list, n2);
        if (n5 < n || !list.get(n5).is(")")) {
            return;
        }
        int n6 = -1;
        int n7 = 0;
        for (n4 = n5; n4 >= n; --n4) {
            GlslTokens.Token token = list.get(n4);
            if (token.is(")")) {
                ++n7;
                continue;
            }
            if (!token.is("(") || --n7 != 0) continue;
            n6 = n4;
            break;
        }
        int n8 = n4 = n6 < 0 ? -1 : GlslTokens.previous(list, n6);
        if (n4 < n || !list.get(n4).isName()) {
            return;
        }
        this.functions.add(new Function(list.get((int)n4).text, n4, n2, n3, GlslDeclarations.parameters(list, n6, n5)));
    }

    private static List<String> parameters(List<GlslTokens.Token> list, int n, int n2) {
        ArrayList<String> arrayList = new ArrayList<String>();
        String string = null;
        int n3 = 0;
        for (int i = n + 1; i < n2; ++i) {
            GlslTokens.Token token = list.get(i);
            if (token.is("(") || token.is("[")) {
                ++n3;
                continue;
            }
            if (token.is(")") || token.is("]")) {
                --n3;
                continue;
            }
            if (token.is(",") && n3 == 0) {
                arrayList.add(string);
                string = null;
                continue;
            }
            if (!token.isName() || n3 != 0) continue;
            string = token.text;
        }
        if (string != null && !string.equals("void")) {
            arrayList.add(string);
        }
        return arrayList;
    }

    private void declaration(List<GlslTokens.Token> list, int n, int n2) {
        Declaration declaration = new Declaration(n, n2);
        int n3 = n;
        while (n3 < n2) {
            GlslTokens.Token token = list.get(n3);
            if (!token.significant()) {
                ++n3;
                continue;
            }
            if (token.is("layout")) {
                int n4 = GlslTokens.next(list, n3 + 1);
                int n5 = n4 < n2 && list.get(n4).is("(") ? GlslTokens.close(list, n4) : -1;
                n3 = n5 < 0 ? n3 + 1 : n5 + 1;
                declaration.qualifiers.add("layout");
                continue;
            }
            if (!STORAGE.contains(token.text) && !INTERPOLATION.contains(token.text) && !OTHER_QUALIFIERS.contains(token.text)) break;
            declaration.qualifiers.add(token.text);
            ++n3;
        }
        if (n3 >= n2 || !list.get(n3).isName() || list.get(n3).is("struct") || list.get(n3).is("precision")) {
            return;
        }
        declaration.type = list.get((int)n3).text;
        GlslDeclarations.names(list, GlslTokens.next(list, n3 + 1), n2, declaration);
        if (!declaration.names.isEmpty()) {
            this.declarations.add(declaration);
        }
    }

    private static void names(List<GlslTokens.Token> list, int n, int n2, Declaration declaration) {
        if (n < n2 && list.get(n).is("[")) {
            int n3 = GlslTokens.close(list, n);
            n = GlslTokens.next(list, n3 + 1);
        }
        while (n < n2) {
            if (!list.get(n).isName()) {
                return;
            }
            String string = list.get((int)n).text;
            String string2 = "";
            if ((n = GlslTokens.next(list, n + 1)) < n2 && list.get(n).is("[")) {
                int n4 = GlslTokens.close(list, n);
                string2 = GlslTokens.join(list.subList(n, n4 + 1));
                n = GlslTokens.next(list, n4 + 1);
            }
            declaration.names.add(string);
            declaration.arrays.add(string2);
            if (n < n2 && list.get(n).is("=")) {
                n = GlslDeclarations.skipInitializer(list, n + 1, n2);
            }
            if (n < n2 && list.get(n).is(",")) {
                n = GlslTokens.next(list, n + 1);
                continue;
            }
            return;
        }
    }

    private static int skipInitializer(List<GlslTokens.Token> list, int n, int n2) {
        while (n < n2) {
            GlslTokens.Token token = list.get(n);
            if (token.is("(") || token.is("[") || token.is("{")) {
                n = GlslTokens.close(list, n) + 1;
                continue;
            }
            if (token.is(",")) {
                return n;
            }
            ++n;
        }
        return n2;
    }

    List<Function> functions(String string) {
        ArrayList<Function> arrayList = new ArrayList<Function>();
        for (Function function : this.functions) {
            if (!function.name.equals(string)) continue;
            arrayList.add(function);
        }
        return arrayList;
    }

    Declaration global(String string, Set<String> set) {
        for (Declaration declaration : this.declarations) {
            if (!declaration.names.contains(string)) continue;
            for (String string2 : declaration.qualifiers) {
                if (!set.contains(string2)) continue;
                return declaration;
            }
        }
        return null;
    }

    boolean insideFunction(int n) {
        for (Function function : this.functions) {
            if (n <= function.open || n >= function.close) continue;
            return true;
        }
        return false;
    }

    static final class Function {
        final String name;
        final int nameToken;
        final int open;
        final int close;
        final List<String> params;

        Function(String string, int n, int n2, int n3, List<String> list) {
            this.name = string;
            this.nameToken = n;
            this.open = n2;
            this.close = n3;
            this.params = list;
        }
    }

    static final class Declaration {
        final int first;
        final int last;
        final List<String> qualifiers = new ArrayList<String>();
        String type;
        final List<String> names = new ArrayList<String>();
        final List<String> arrays = new ArrayList<String>();

        Declaration(int n, int n2) {
            this.first = n;
            this.last = n2;
        }

        boolean has(String string) {
            return this.qualifiers.contains(string);
        }

        String interpolation() {
            StringBuilder stringBuilder = new StringBuilder();
            for (String string : this.qualifiers) {
                if (!INTERPOLATION.contains(string)) continue;
                stringBuilder.append(string).append(' ');
            }
            return stringBuilder.toString();
        }
    }
}

