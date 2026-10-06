/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.Map;
import viewpoint.iris.IrisTransform;
import viewpoint.iris.LightmapFlow;

public final class IrisAssembler {
    public static final int LIBRARY_SOURCE = 999;

    private IrisAssembler() {
    }

    public static String vertex(IrisTransform.Stage stage, String string, Source source) {
        StringBuilder stringBuilder = IrisAssembler.header(stage, string, source == Source.QUAD && IrisAssembler.isOld(stage.version));
        IrisAssembler.inputs(stringBuilder, stage);
        stringBuilder.append(stage.code).append("\n#line 1 ").append(999).append("\nvoid main() {\n");
        IrisAssembler.vertexMain(stringBuilder, stage, source);
        return stringBuilder.append("}\n").toString();
    }

    static void inputs(StringBuilder stringBuilder, IrisTransform.Stage stage) {
        for (Map.Entry<String, String> entry : stage.inputs.entrySet()) {
            stringBuilder.append(entry.getValue()).append(' ').append(entry.getKey()).append(";\n");
        }
    }

    static void vertexMain(StringBuilder stringBuilder, IrisTransform.Stage stage, Source source) {
        stringBuilder.append(switch (source.ordinal()) {
            default -> throw new IncompatibleClassChangeError();
            case 0 -> "  vp_fetchMesh();\n";
            case 1, 2 -> "  vp_fetchModel();\n";
            case 3 -> "  vp_fetchPlain();\n";
            case 4 -> "  vp_fetchWeather();\n";
            case 5 -> "  vp_fetchFarBox();\n";
            case 6 -> "  vp_fetchFarTree();\n";
            case 7 -> "  vp_fetchFarShell();\n";
            case 8 -> "  vp_fetchQuad();\n";
        });
        if (source != Source.QUAD) {
            for (Map.Entry<String, String> entry : stage.inputs.entrySet()) {
                stringBuilder.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append('(').append(IrisAssembler.value(entry.getKey())).append(");\n");
            }
        }
        stringBuilder.append(stage.mainRenamed ? "  vp_packMain();\n" : "");
        if (stage.lightmap != null) {
            stringBuilder.append("  vp_lm_").append(stage.lightmap.name()).append(" = ").append(stage.lightmap.name()).append(";\n");
        }
        if (stage.colour != null) {
            String string = stage.colour.name();
            stringBuilder.append("  vp_col_").append(string).append(" = ").append(string).append(";\n");
        }
        if (source != Source.QUAD) {
            stringBuilder.append("  if (vp_hiddenVertex) gl_Position = vec4(2.0, 2.0, 2.0, 1.0);\n");
        }
    }

    public static String fragment(IrisTransform.Stage stage, String string, Source source, int n) {
        String string2;
        StringBuilder stringBuilder = IrisAssembler.header(stage, string, false);
        if (n >= 0 && !stage.fragData) {
            stringBuilder.append("layout(location = ").append(n).append(") out vec4 vp_tintOut;\n");
        }
        stringBuilder.append(stage.code).append("\n#line 1 ").append(999).append("\nvoid main() {\n");
        stringBuilder.append("  vp_prologue();\n");
        LightmapFlow.Found found = stage.lightmap;
        if (found != null) {
            string2 = found.name();
            stringBuilder.append("  ").append(string2).append(" = vp_lm_").append(string2).append(";\n");
            stringBuilder.append("  ").append(string2).append('.').append(found.components()).append(" = clamp(").append(string2).append('.').append(found.components()).append(" + vp_lightmapDelta(), 0.0, 1.0);\n");
        }
        if (stage.colour != null) {
            string2 = stage.colour.name();
            stringBuilder.append("  ").append(string2).append(" = vp_col_").append(string2).append(";\n");
            stringBuilder.append("  ").append(string2).append(".rgb = vp_carriedColour(").append(string2).append(".rgb);\n");
        }
        stringBuilder.append(stage.mainRenamed ? "  vp_packMain();\n" : "");
        if (n >= 0) {
            stringBuilder.append((String)(stage.fragData ? "  gl_FragData[" + n + "] = vp_tintOutput();\n" : "  vp_tintOut = vp_tintOutput();\n"));
        }
        return stringBuilder.append("}\n").toString();
    }

    public static String plain(IrisTransform.Stage stage, String string) {
        StringBuilder stringBuilder = IrisAssembler.header(stage, string, false);
        return stringBuilder.append(stage.code).append('\n').toString();
    }

    static StringBuilder header(IrisTransform.Stage stage, String string, boolean bl) {
        StringBuilder stringBuilder = new StringBuilder(string.length() + stage.code.length() + 1024);
        stringBuilder.append("#version ").append(stage.version).append('\n');
        for (String string2 : stage.extensions) {
            stringBuilder.append(string2).append('\n');
        }
        Object object = string;
        if (IrisAssembler.isOld(stage.version)) {
            object = ((String)object).replace("texture(", "texture2D(");
        }
        if (bl) {
            object = ((String)object).replaceAll("(?m)^in ", "attribute ");
        }
        stringBuilder.append("#line 1 ").append(999).append('\n').append((String)object).append('\n');
        return stringBuilder;
    }

    static boolean isOld(String string) {
        try {
            return Integer.parseInt(string.strip().split("\\s+")[0]) < 130;
        }
        catch (NumberFormatException numberFormatException) {
            return true;
        }
    }

    private static String value(String string) {
        return switch (string) {
            case "mc_Entity" -> "vp_entity";
            case "mc_midTexCoord" -> "vp_midTexCoord";
            case "at_tangent" -> "vp_tangent";
            case "at_midBlock" -> "vp_midBlock";
            case "at_velocity" -> "vec4(vp_velocity, 0.0)";
            case "vaPosition" -> "vp_Vertex";
            case "vaColor" -> "vp_Color";
            case "vaUV0" -> "vp_MultiTexCoord0";
            case "vaUV2" -> "vp_MultiTexCoord1";
            case "vaNormal" -> "vec4(vp_Normal, 0.0)";
            case "dhMaterialId" -> "vp_entity";
            case "mc_chunkFade" -> "vec4(1.0)";
            default -> "vec4(0.0)";
        };
    }

    public static enum Source {
        MESH,
        MODEL,
        VEHICLE,
        PLAIN,
        WEATHER,
        FAR_BOX,
        FAR_TREE,
        FAR_SHELL,
        QUAD;

    }
}

