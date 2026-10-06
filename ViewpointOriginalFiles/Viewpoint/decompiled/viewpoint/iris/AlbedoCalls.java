/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import viewpoint.iris.GlslDeclarations;
import viewpoint.iris.GlslTokens;

final class AlbedoCalls {
    private static final Map<String, String> REPLACEMENTS = new HashMap<String, String>();
    static final String ATLAS = "vp_albedoTex";
    private final List<GlslTokens.Token> tokens;
    private final Set<String> samplers;
    final List<String> notes = new ArrayList<String>();
    int rewritten;

    private AlbedoCalls(List<GlslTokens.Token> list, Set<String> set) {
        this.tokens = list;
        this.samplers = set;
    }

    static AlbedoCalls rewrite(List<GlslTokens.Token> list, Set<String> set) {
        AlbedoCalls albedoCalls = new AlbedoCalls(list, set);
        if (!set.isEmpty()) {
            albedoCalls.helpers();
            albedoCalls.calls(0, list.size(), set);
            albedoCalls.leftovers();
        }
        return albedoCalls;
    }

    private void calls(int n, int n2, Set<String> set) {
        for (int i = n; i < n2 && i < this.tokens.size(); ++i) {
            int n3;
            List<int[]> list;
            int n4;
            GlslTokens.Token token = this.tokens.get(i);
            if (!token.isName() || (n4 = GlslTokens.next(this.tokens, i + 1)) >= this.tokens.size() || !this.tokens.get(n4).is("(") || (list = this.arguments(n4, n3 = GlslTokens.close(this.tokens, n4))).isEmpty() || !set.contains(this.single(list.get(0)))) continue;
            String string = REPLACEMENTS.get(token.text + "/" + list.size());
            if (string == null) {
                this.tokens.get((int)list.get((int)0)[0]).text = ATLAS;
                continue;
            }
            token.text = string;
            int n5 = GlslTokens.next(this.tokens, list.get(0)[1] + 1);
            for (int j = list.get(0)[0]; j <= n5; ++j) {
                this.tokens.get((int)j).text = "";
            }
            ++this.rewritten;
        }
    }

    private List<int[]> arguments(int n, int n2) {
        ArrayList<int[]> arrayList = new ArrayList<int[]>();
        if (n2 < 0) {
            return arrayList;
        }
        int n3 = -1;
        int n4 = -1;
        int n5 = 0;
        for (int i = n + 1; i < n2; ++i) {
            GlslTokens.Token token = this.tokens.get(i);
            if (!token.significant()) continue;
            if (token.is("(") || token.is("[")) {
                ++n5;
            } else if (token.is(")") || token.is("]")) {
                --n5;
            }
            if (token.is(",") && n5 == 0) {
                arrayList.add(new int[]{n3, n4});
                n3 = -1;
                continue;
            }
            if (n3 < 0) {
                n3 = i;
            }
            n4 = i;
        }
        if (n3 >= 0) {
            arrayList.add(new int[]{n3, n4});
        }
        return arrayList;
    }

    private String single(int[] nArray) {
        return nArray[0] == nArray[1] && nArray[0] >= 0 && this.tokens.get(nArray[0]).isName() ? this.tokens.get((int)nArray[0]).text : "";
    }

    private void helpers() {
        GlslDeclarations glslDeclarations = GlslDeclarations.read(this.tokens);
        HashMap<String, Set> hashMap = new HashMap<String, Set>();
        block0: for (int i = 0; i < this.tokens.size(); ++i) {
            int n2;
            GlslTokens.Token token = this.tokens.get(i);
            if (!token.isName() || glslDeclarations.functions(token.text).isEmpty() || (n2 = GlslTokens.next(this.tokens, i + 1)) >= this.tokens.size() || !this.tokens.get(n2).is("(") || AlbedoCalls.isDefinition(glslDeclarations, i)) continue;
            List<int[]> list = this.arguments(n2, GlslTokens.close(this.tokens, n2));
            for (int j = 0; j < list.size(); ++j) {
                if (!this.samplers.contains(this.single((int[])list.get(j)))) continue;
                hashMap.computeIfAbsent(token.text, string -> new HashSet()).add(j);
                token.text = token.text + "_vp" + j;
                continue block0;
            }
        }
        TreeMap treeMap = new TreeMap(Comparator.reverseOrder());
        for (Map.Entry entry : hashMap.entrySet()) {
            for (GlslDeclarations.Function function : glslDeclarations.functions((String)entry.getKey())) {
                Iterator iterator = ((Set)entry.getValue()).iterator();
                while (iterator.hasNext()) {
                    int n3 = (Integer)iterator.next();
                    if (n3 >= function.params.size()) continue;
                    treeMap.computeIfAbsent(function.close + 1, n -> new ArrayList()).addAll(this.clone(function, n3));
                }
            }
        }
        for (Map.Entry entry : treeMap.entrySet()) {
            this.tokens.add((Integer)entry.getKey(), new GlslTokens.Token(GlslTokens.Kind.SPACE, "\n"));
            this.tokens.addAll((Integer)entry.getKey() + 1, (Collection)entry.getValue());
        }
    }

    private static boolean isDefinition(GlslDeclarations glslDeclarations, int n) {
        for (GlslDeclarations.Function function : glslDeclarations.functions) {
            if (function.nameToken != n) continue;
            return true;
        }
        return false;
    }

    private List<GlslTokens.Token> clone(GlslDeclarations.Function function, int n) {
        Object object;
        for (int i = function.nameToken; i > 0 && !((GlslTokens.Token)(object = this.tokens.get(i - 1))).is(";") && !((GlslTokens.Token)object).is("}") && ((GlslTokens.Token)object).kind != GlslTokens.Kind.DIRECTIVE; --i) {
        }
        object = new ArrayList();
        for (int i = i; i <= function.close; ++i) {
            GlslTokens.Token token = this.tokens.get(i);
            object.add(new GlslTokens.Token(token.kind, (String)(i == function.nameToken ? function.name + "_vp" + n : token.text)));
        }
        object.add(new GlslTokens.Token(GlslTokens.Kind.SPACE, "\n"));
        AlbedoCalls albedoCalls = new AlbedoCalls((List<GlslTokens.Token>)object, Set.of(function.params.get(n)));
        albedoCalls.calls(0, object.size(), Set.of(function.params.get(n)));
        this.rewritten += albedoCalls.rewritten;
        return object;
    }

    private void leftovers() {
        GlslDeclarations glslDeclarations = GlslDeclarations.read(this.tokens);
        for (int i = 0; i < this.tokens.size(); ++i) {
            int n;
            GlslTokens.Token token = this.tokens.get(i);
            if (!token.isName() || !this.samplers.contains(token.text) || !glslDeclarations.insideFunction(i) || (n = GlslTokens.previous(this.tokens, i)) >= 0 && this.tokens.get(n).is(".")) continue;
            token.text = ATLAS;
            this.notes.add("the albedo sampler is read otherwise than by a texture call: it samples the atlas page");
        }
    }

    static {
        for (String string : new String[]{"texture2D", "texture"}) {
            REPLACEMENTS.put(string + "/2", "vp_albedo");
            REPLACEMENTS.put(string + "/3", "vp_albedoBias");
        }
        for (String string : new String[]{"texture2DLod", "textureLod"}) {
            REPLACEMENTS.put(string + "/3", "vp_albedoLod");
        }
        for (String string : new String[]{"textureGrad", "texture2DGrad", "texture2DGradARB"}) {
            REPLACEMENTS.put(string + "/4", "vp_albedoGrad");
        }
        REPLACEMENTS.put("texelFetch/3", "vp_albedoFetch");
    }
}

