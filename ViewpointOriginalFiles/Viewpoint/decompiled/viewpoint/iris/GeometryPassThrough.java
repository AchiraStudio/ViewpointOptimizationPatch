/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import viewpoint.iris.GlslTokens;
import viewpoint.iris.IrisAssembler;
import viewpoint.iris.IrisTransform;
import viewpoint.iris.Preprocessor;

public final class GeometryPassThrough {
    private static final String SUFFIX = "_g";
    private static final Pattern OUTPUT = Pattern.compile("(?m)^\\s*(flat\\s+|noperspective\\s+|smooth\\s+)?out\\s+(\\w+)\\s+(vp_\\w+)\\s*;");

    private GeometryPassThrough() {
    }

    public static IrisTransform.Stage stage(Preprocessor.Result result) {
        IrisTransform.Stage stage = IrisTransform.start(result, IrisTransform.Kind.GEOMETRY);
        List<GlslTokens.Token> list = GlslTokens.lex(result.text());
        IrisTransform.renameBuiltins(list, IrisTransform.MATRICES);
        IrisTransform.modernise(list, false);
        IrisTransform.renameBuiltins(list, Map.of("EmitVertex", "vp_emitVertex", "EndPrimitive", "vp_endPrimitive"));
        IrisTransform.renameNewerBuiltins(list, stage);
        IrisTransform.finish(list, stage);
        return stage;
    }

    static List<Varying> outputs(String string) {
        ArrayList<Varying> arrayList = new ArrayList<Varying>();
        Matcher matcher = OUTPUT.matcher(string);
        while (matcher.find()) {
            String string2 = matcher.group(1) == null ? "" : matcher.group(1).strip() + " ";
            arrayList.add(new Varying(string2, matcher.group(2), matcher.group(3)));
        }
        return arrayList;
    }

    public static String renamed(String string) {
        String string2 = string;
        for (Varying varying : GeometryPassThrough.outputs(string)) {
            string2 = string2.replaceAll("\\b" + varying.name + "\\b", varying.name + SUFFIX);
        }
        return string2;
    }

    public static String geometry(IrisTransform.Stage stage, String string, String string2) {
        List<Varying> list = GeometryPassThrough.outputs(string2);
        StringBuilder stringBuilder = IrisAssembler.header(stage, string, false);
        stringBuilder.append("void vp_emitVertex();\nvoid vp_endPrimitive();\n");
        stringBuilder.append(stage.code).append("\n#line 1 ").append(999).append('\n');
        for (Varying varying : list) {
            stringBuilder.append(varying.qualifier).append("in ").append(varying.type).append(' ').append(varying.name + SUFFIX).append("[];\n");
            stringBuilder.append(varying.qualifier).append("out ").append(varying.type).append(' ').append(varying.name).append(";\n");
        }
        stringBuilder.append("int vp_emitted = 0;\nvoid vp_emitVertex() {\n  int k = vp_emitted % gl_in.length();\n");
        for (Varying varying : list) {
            stringBuilder.append("  ").append(varying.name).append(" = ").append(varying.name + SUFFIX).append("[k];\n");
        }
        return stringBuilder.append("  vp_emitted++;\n  EmitVertex();\n}\n").append("void vp_endPrimitive() {\n  vp_emitted = 0;\n  EndPrimitive();\n}\n").toString();
    }

    record Varying(String qualifier, String type, String name) {
    }
}

