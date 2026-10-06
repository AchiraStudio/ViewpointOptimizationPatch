/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import viewpoint.iris.AlbedoCalls;
import viewpoint.iris.BlockGrid;
import viewpoint.iris.GlobalInitializers;
import viewpoint.iris.GlslDeclarations;
import viewpoint.iris.GlslTokens;
import viewpoint.iris.LampHooks;
import viewpoint.iris.LightmapFlow;
import viewpoint.iris.Preprocessor;

public final class IrisTransform {
    private static final Map<String, String> VERTEX_BUILTINS = Map.ofEntries(Map.entry("gl_Vertex", "vp_Vertex"), Map.entry("gl_Normal", "vp_Normal"), Map.entry("gl_Color", "vp_Color"), Map.entry("gl_MultiTexCoord0", "vp_MultiTexCoord0"), Map.entry("gl_MultiTexCoord1", "vp_MultiTexCoord1"), Map.entry("gl_MultiTexCoord2", "vp_MultiTexCoordZero"), Map.entry("gl_MultiTexCoord3", "vp_MultiTexCoordZero"));
    static final Map<String, String> MATRICES = Map.ofEntries(Map.entry("gl_ModelViewMatrix", "vp_ModelViewMatrix"), Map.entry("gl_ProjectionMatrix", "vp_ProjectionMatrix"), Map.entry("gl_ModelViewProjectionMatrix", "vp_ModelViewProjectionMatrix"), Map.entry("gl_NormalMatrix", "vp_NormalMatrix"), Map.entry("gl_TextureMatrix", "vp_TextureMatrix"), Map.entry("gl_ModelViewMatrixInverse", "vp_ModelViewMatrixInverse"), Map.entry("gl_ProjectionMatrixInverse", "vp_ProjectionMatrixInverse"), Map.entry("gl_Fog", "vp_Fog"));
    public static final Set<String> INPUTS = Set.of("mc_Entity", "mc_midTexCoord", "at_tangent", "at_midBlock", "at_velocity", "mc_chunkFade", "vaPosition", "vaColor", "vaUV0", "vaUV1", "vaUV2", "vaNormal", "chunkOffset", "dhMaterialId");
    private static final Set<String> NEWER_BUILTINS = Set.of("round", "roundEven", "trunc", "sinh", "cosh", "tanh", "asinh", "acosh", "atanh", "isnan", "isinf", "modf", "inverse", "determinant", "texture", "textureLod", "textureSize", "texelFetch", "textureGrad", "textureOffset", "textureProj", "floatBitsToInt", "floatBitsToUint", "intBitsToFloat", "uintBitsToFloat", "packUnorm2x16", "unpackUnorm2x16", "packHalf2x16", "unpackHalf2x16", "fma", "frexp", "ldexp", "bitfieldExtract", "bitfieldInsert", "bitfieldReverse", "bitCount", "findLSB", "findMSB", "textureGather", "textureQueryLod", "dFdxFine", "dFdyFine", "dFdxCoarse", "dFdyCoarse", "fwidthFine", "fwidthCoarse");
    private static final Set<String> ALBEDO = Set.of("tex", "texture", "gtexture", "gcolor", "colortex0");

    private IrisTransform() {
    }

    public static Stage vertex(Preprocessor.Result result, Kind kind) {
        return IrisTransform.vertex(result, kind, false);
    }

    public static Stage vertex(Preprocessor.Result result, Kind kind, boolean bl) {
        Stage stage = IrisTransform.start(result, kind);
        List<GlslTokens.Token> list = GlslTokens.lex(result.text());
        IrisTransform.renameBuiltins(list, VERTEX_BUILTINS);
        IrisTransform.renameBuiltins(list, MATRICES);
        IrisTransform.ftransform(list);
        IrisTransform.modernise(list, true);
        if (kind == Kind.GEOMETRY) {
            GlslDeclarations glslDeclarations = GlslDeclarations.read(list);
            IrisTransform.removeInputs(list, glslDeclarations, stage);
            for (Map.Entry<String, String> entry : Map.of("dhMaterialId", "int", "mc_chunkFade", "float").entrySet()) {
                if (stage.inputs.containsKey(entry.getKey()) || !list.stream().anyMatch(token -> token.is((String)entry.getKey()))) continue;
                stage.inputs.put(entry.getKey(), entry.getValue());
            }
            glslDeclarations = GlslDeclarations.read(list);
            stage.lightmap = LightmapFlow.find(list, glslDeclarations, Set.of("vp_MultiTexCoord1", "vaUV2"));
            if (stage.lightmap != null) {
                IrisTransform.carry(list, glslDeclarations, stage.lightmap, "out");
            }
            if (bl) {
                IrisTransform.colour(list, stage);
                BlockGrid.unround(list);
            }
        }
        IrisTransform.renameNewerBuiltins(list, stage);
        stage.mainRenamed = IrisTransform.renameMain(list);
        IrisTransform.finish(list, stage);
        return stage;
    }

    public static Stage fragment(Preprocessor.Result result, Kind kind, LightmapFlow.Found found) {
        return IrisTransform.fragment(result, kind, found, null);
    }

    public static Stage fragment(Preprocessor.Result result, Kind kind, LightmapFlow.Found found, LightmapFlow.Found found2) {
        Stage stage = IrisTransform.start(result, kind);
        List<GlslTokens.Token> list = GlslTokens.lex(result.text());
        IrisTransform.renameBuiltins(list, MATRICES);
        IrisTransform.modernise(list, false);
        if (kind == Kind.GEOMETRY) {
            IrisTransform.albedo(list, stage);
            GlslDeclarations glslDeclarations = GlslDeclarations.read(list);
            if (found != null && glslDeclarations.global(found.name(), Set.of("in", "varying")) != null) {
                IrisTransform.carry(list, glslDeclarations, found, "in");
                stage.lightmap = found;
            } else if (found != null) {
                stage.notes.add("the lightmap's variable " + found.name() + " is not read by the fragment stage");
            }
            glslDeclarations = GlslDeclarations.read(list);
            if (found2 != null && glslDeclarations.global(found2.name(), Set.of("in", "varying")) != null) {
                IrisTransform.carry(list, glslDeclarations, found2, "in", "vp_col_");
                stage.colour = found2;
            }
        }
        stage.lampTints = LampHooks.tint(list);
        IrisTransform.renameNewerBuiltins(list, stage);
        stage.mainRenamed = kind == Kind.GEOMETRY && IrisTransform.renameMain(list);
        IrisTransform.finish(list, stage);
        return stage;
    }

    private static void colour(List<GlslTokens.Token> list, Stage stage) {
        GlslDeclarations glslDeclarations = GlslDeclarations.read(list);
        LightmapFlow.Found found = LightmapFlow.findColour(list, glslDeclarations, Set.of("vp_Color", "vaColor"));
        if (!(found == null || stage.lightmap != null && found.name().equals(stage.lightmap.name()))) {
            IrisTransform.carry(list, glslDeclarations, found, "out", "vp_col_");
            stage.colour = found;
        }
    }

    public static Stage plain(Preprocessor.Result result, Kind kind) {
        Stage stage = IrisTransform.start(result, kind);
        List<GlslTokens.Token> list = GlslTokens.lex(result.text());
        IrisTransform.renameBuiltins(list, MATRICES);
        IrisTransform.finish(list, stage);
        return stage;
    }

    static Stage start(Preprocessor.Result result, Kind kind) {
        Stage stage = new Stage();
        stage.version = IrisTransform.version(result.version(), kind);
        stage.raised = !stage.version.equals(result.version() == null ? "120" : result.version().strip());
        stage.extensions.addAll(result.extensions());
        return stage;
    }

    static void modernise(List<GlslTokens.Token> list, boolean bl) {
        for (GlslTokens.Token token : list) {
            if (!token.isName()) continue;
            switch (token.text) {
                case "attribute": {
                    token.text = "in";
                    break;
                }
                case "varying": {
                    token.text = bl ? "out" : "in";
                    break;
                }
                case "texelFetch2D": {
                    token.text = "texelFetch";
                    break;
                }
                case "texture2DGradARB": 
                case "texture3DGradARB": {
                    token.text = "textureGrad";
                    break;
                }
                case "texture2DLodARB": 
                case "texture3DLodARB": {
                    token.text = "textureLod";
                    break;
                }
            }
        }
    }

    static String version(String string, Kind kind) {
        int n;
        String string2 = string == null ? "120" : string.strip();
        String[] stringArray = string2.split("\\s+");
        try {
            n = Integer.parseInt(stringArray[0]);
        }
        catch (NumberFormatException numberFormatException) {
            n = 120;
        }
        if (kind != Kind.COMPUTE && n < 330) {
            return "330 compatibility";
        }
        return string2;
    }

    static void renameBuiltins(List<GlslTokens.Token> list, Map<String, String> map) {
        for (GlslTokens.Token token : list) {
            String string;
            if (!token.isName() || (string = map.get(token.text)) == null) continue;
            token.text = string;
        }
    }

    private static void ftransform(List<GlslTokens.Token> list) {
        for (int i = 0; i < list.size(); ++i) {
            int n;
            GlslTokens.Token token = list.get(i);
            if (!token.is("ftransform") || (n = GlslTokens.next(list, i + 1)) >= list.size() || !list.get(n).is("(")) continue;
            int n2 = GlslTokens.close(list, n);
            for (int j = n; j <= n2; ++j) {
                list.get((int)j).text = "";
            }
            token.text = "(vp_ModelViewProjectionMatrix * vp_Vertex)";
        }
    }

    private static void removeInputs(List<GlslTokens.Token> list, GlslDeclarations glslDeclarations, Stage stage) {
        for (GlslDeclarations.Declaration declaration : glslDeclarations.declarations) {
            if (!declaration.has("in") && !declaration.has("attribute") && !declaration.has("uniform")) continue;
            ArrayList<String> arrayList = new ArrayList<String>();
            for (String string : declaration.names) {
                if (INPUTS.contains(string)) {
                    stage.inputs.put(string, declaration.type);
                    continue;
                }
                arrayList.add(string);
            }
            if (arrayList.size() == declaration.names.size()) continue;
            Object object = arrayList.isEmpty() ? "" : IrisTransform.qualifiers(list, declaration) + declaration.type + " " + String.join((CharSequence)", ", arrayList) + ";";
            IrisTransform.blank(list, declaration.first, declaration.last);
            list.get((int)declaration.first).text = object;
        }
    }

    private static void carry(List<GlslTokens.Token> list, GlslDeclarations glslDeclarations, LightmapFlow.Found found, String string) {
        IrisTransform.carry(list, glslDeclarations, found, string, "vp_lm_");
    }

    private static void carry(List<GlslTokens.Token> list, GlslDeclarations glslDeclarations, LightmapFlow.Found found, String string, String string2) {
        GlslDeclarations.Declaration declaration = glslDeclarations.global(found.name(), Set.of("in", "out", "varying"));
        if (declaration == null) {
            return;
        }
        String string3 = IrisTransform.qualifiers(list, declaration);
        ArrayList<CallSite> arrayList = new ArrayList<CallSite>();
        String string4 = "";
        for (int i = 0; i < declaration.names.size(); ++i) {
            if (declaration.names.get(i).equals(found.name())) {
                string4 = declaration.arrays.get(i);
                continue;
            }
            arrayList.add((CallSite)((Object)(declaration.names.get(i) + declaration.arrays.get(i))));
        }
        String string5 = declaration.has("varying") ? "varying" : string;
        StringBuilder stringBuilder = new StringBuilder();
        if (!arrayList.isEmpty()) {
            stringBuilder.append(string3).append(declaration.type).append(' ').append(String.join((CharSequence)", ", arrayList)).append("; ");
        }
        stringBuilder.append(declaration.type).append(' ').append(found.name()).append(string4).append("; ");
        stringBuilder.append(found.interpolation()).append(string5).append(' ').append(declaration.type).append(' ').append(string2).append(found.name()).append(string4).append(';');
        IrisTransform.blank(list, declaration.first, declaration.last);
        list.get((int)declaration.first).text = stringBuilder.toString();
    }

    private static String qualifiers(List<GlslTokens.Token> list, GlslDeclarations.Declaration declaration) {
        GlslTokens.Token token;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = declaration.first; !(i > declaration.last || (token = list.get(i)).is(declaration.type) && token.isName()); ++i) {
            stringBuilder.append(token.text);
        }
        String string = stringBuilder.toString().strip();
        return string.isEmpty() ? "" : string + " ";
    }

    private static void albedo(List<GlslTokens.Token> list, Stage stage) {
        GlslDeclarations glslDeclarations = GlslDeclarations.read(list);
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();
        for (GlslDeclarations.Declaration object : glslDeclarations.declarations) {
            if (!object.has("uniform") || !object.type.equals("sampler2D")) continue;
            for (String string : object.names) {
                if (!ALBEDO.contains(string)) continue;
                linkedHashSet.add(string);
            }
        }
        AlbedoCalls albedoCalls = AlbedoCalls.rewrite(list, linkedHashSet);
        stage.albedoReads = albedoCalls.rewritten;
        stage.notes.addAll(new LinkedHashSet<String>(albedoCalls.notes));
        glslDeclarations = GlslDeclarations.read(list);
        for (GlslDeclarations.Declaration declaration : glslDeclarations.declarations) {
            if (!declaration.has("uniform") || !declaration.type.equals("sampler2D")) continue;
            if (!declaration.names.stream().anyMatch(linkedHashSet::contains)) continue;
            ArrayList<String> arrayList = new ArrayList<String>(declaration.names);
            arrayList.removeAll(linkedHashSet);
            String string = arrayList.isEmpty() ? "" : IrisTransform.qualifiers(list, declaration) + "sampler2D " + String.join((CharSequence)", ", arrayList) + ";";
            IrisTransform.blank(list, declaration.first, declaration.last);
            list.get((int)declaration.first).text = string;
        }
    }

    static void renameNewerBuiltins(List<GlslTokens.Token> list, Stage stage) {
        if (!stage.version.startsWith("330") && !stage.version.startsWith("4")) {
            return;
        }
        GlslDeclarations glslDeclarations = GlslDeclarations.read(list);
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();
        for (GlslDeclarations.Function object : glslDeclarations.functions) {
            if (!NEWER_BUILTINS.contains(object.name)) continue;
            linkedHashSet.add(object.name);
        }
        if (linkedHashSet.isEmpty()) {
            return;
        }
        for (GlslTokens.Token token : list) {
            if (!token.isName() || !linkedHashSet.contains(token.text)) continue;
            token.text = "vp_pack_" + token.text;
        }
        stage.notes.add("functions named as GLSL 1.30's built-ins renamed: " + String.valueOf(linkedHashSet));
    }

    private static boolean renameMain(List<GlslTokens.Token> list) {
        GlslDeclarations glslDeclarations = GlslDeclarations.read(list);
        boolean bl = false;
        for (GlslDeclarations.Function function : glslDeclarations.functions("main")) {
            list.get((int)function.nameToken).text = "vp_packMain";
            bl = true;
        }
        GlobalInitializers.defer(list);
        return bl;
    }

    static void finish(List<GlslTokens.Token> list, Stage stage) {
        GlslDeclarations glslDeclarations = GlslDeclarations.read(list);
        for (GlslDeclarations.Declaration object : glslDeclarations.declarations) {
            if (object.has("uniform")) {
                for (String string : object.names) {
                    stage.uniforms.add(string);
                    if (!object.type.startsWith("sampler") && !object.type.contains("image") && !object.type.startsWith("isampler") && !object.type.startsWith("usampler")) continue;
                    stage.samplers.put(string, object.type);
                }
            }
            if (!object.has("out") || object.has("in")) continue;
            for (String string : object.names) {
                stage.outputs.put(string, IrisTransform.location(list, object));
            }
        }
        for (GlslTokens.Token token : list) {
            if (!token.isName() || !token.is("gl_FragData") && !token.is("gl_FragColor")) continue;
            stage.fragData = true;
        }
        stage.code = GlslTokens.join(list);
    }

    private static int location(List<GlslTokens.Token> list, GlslDeclarations.Declaration declaration) {
        for (int i = declaration.first; i < declaration.last; ++i) {
            if (!list.get(i).is("location")) continue;
            int n = GlslTokens.next(list, i + 1);
            int n2 = GlslTokens.next(list, n + 1);
            try {
                return Integer.parseInt(list.get((int)n2).text);
            }
            catch (NumberFormatException numberFormatException) {
                return -1;
            }
        }
        return -1;
    }

    private static void blank(List<GlslTokens.Token> list, int n, int n2) {
        for (int i = n; i <= n2; ++i) {
            GlslTokens.Token token = list.get(i);
            token.text = token.kind == GlslTokens.Kind.SPACE || token.kind == GlslTokens.Kind.COMMENT ? token.text.replaceAll("[^\n]", "") : "";
        }
    }

    public static enum Kind {
        GEOMETRY,
        COMPOSITE,
        COMPUTE;

    }

    public static final class Stage {
        public String version;
        public String code;
        public final List<String> extensions = new ArrayList<String>();
        public final Map<String, String> inputs = new LinkedHashMap<String, String>();
        public LightmapFlow.Found lightmap;
        public LightmapFlow.Found colour;
        public boolean mainRenamed;
        public boolean fragData;
        public final Map<String, Integer> outputs = new LinkedHashMap<String, Integer>();
        public final Map<String, String> samplers = new LinkedHashMap<String, String>();
        public final Set<String> uniforms = new LinkedHashSet<String>();
        public int albedoReads;
        public int lampTints;
        public boolean raised;
        public final List<String> notes = new ArrayList<String>();
    }
}

