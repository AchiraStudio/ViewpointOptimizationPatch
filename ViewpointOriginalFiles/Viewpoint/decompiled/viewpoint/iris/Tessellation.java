/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import viewpoint.iris.IrisAssembler;
import viewpoint.iris.IrisTransform;

public final class Tessellation {
    static final float STEP = 1.0f;
    static final float SPREAD = 0.125f;
    static final int MOST = 64;
    private static final Set<String> VARYING = Set.of("aPos", "aUv", "aNormal", "aFill", "aVertex");
    private static final String EXTENSION = "#extension GL_ARB_tessellation_shader : require\n";
    private static final Pattern ATTRIBUTE = Pattern.compile("layout\\s*\\(\\s*location\\s*=\\s*\\d+\\s*\\)\\s*in\\s+(\\w+)\\s+(\\w+)\\s*;");
    private static final Pattern VERTEX_ID = Pattern.compile("\\bgl_VertexID\\b");
    private static final Pattern INSTANCE_ID = Pattern.compile("\\bgl_InstanceID\\b");

    private Tessellation() {
    }

    static List<Attribute> attributes(String string) {
        ArrayList<Attribute> arrayList = new ArrayList<Attribute>();
        Matcher matcher = ATTRIBUTE.matcher(string);
        while (matcher.find()) {
            arrayList.add(new Attribute(matcher.group(1), matcher.group(2)));
        }
        return arrayList;
    }

    public static String vertex(String string, IrisAssembler.Source source) {
        StringBuilder stringBuilder = new StringBuilder("#version 330 compatibility\n").append(string).append('\n');
        for (Attribute attribute : Tessellation.attributes(string)) {
            stringBuilder.append(attribute.declared("out", "_v"));
        }
        stringBuilder.append("out vec3 vp_place_v;\nflat out int vp_hidden_v;\nflat out int vp_vertexId_v;\n").append("flat out int vp_instanceId_v;\nvoid main() {\n  ").append(Tessellation.fetch(source)).append(";\n");
        for (Attribute attribute : Tessellation.attributes(string)) {
            stringBuilder.append("  ").append(attribute.name).append("_v = ").append(attribute.name).append(";\n");
        }
        return stringBuilder.append("  vp_place_v = vp_Vertex.xyz;\n  vp_hidden_v = vp_hiddenVertex ? 1 : 0;\n").append("  vp_vertexId_v = gl_VertexID;\n  vp_instanceId_v = gl_InstanceID;\n").append("  gl_Position = vp_Vertex;\n}\n").toString();
    }

    public static String control(String string) {
        StringBuilder stringBuilder = new StringBuilder("#version 330 core\n").append(EXTENSION);
        stringBuilder.append("layout(vertices = 3) out;\n");
        for (Attribute attribute : Tessellation.attributes(string)) {
            stringBuilder.append(attribute.declared("in", "_v[]")).append(attribute.declared("out", "_c[]"));
        }
        stringBuilder.append("in vec3 vp_place_v[];\nflat in int vp_hidden_v[];\nflat in int vp_vertexId_v[];\n").append("flat in int vp_instanceId_v[];\nflat out int vp_vertexId_c[];\nflat out int vp_instanceId_c[];\n").append("uniform int uPlants;\nuniform int uModels;\n").append("float vp_pieces(vec3 a, vec3 b) {\n").append("  float step = max(").append(1.0f).append(", 0.5 * length(a + b) * ").append(0.125f).append(");\n  return clamp(ceil(distance(a, b) / step), 1.0, ").append(64).append(".0);\n}\nvoid main() {\n");
        for (Attribute attribute : Tessellation.attributes(string)) {
            stringBuilder.append("  ").append(attribute.name).append("_c[gl_InvocationID] = ").append(attribute.name);
            stringBuilder.append("_v[gl_InvocationID];\n");
        }
        return stringBuilder.append("  vp_vertexId_c[gl_InvocationID] = vp_vertexId_v[gl_InvocationID];\n").append("  vp_instanceId_c[gl_InvocationID] = vp_instanceId_v[gl_InvocationID];\n").append("  if (gl_InvocationID != 0) return;\n").append("  float keep = vp_hidden_v[0] != 0 ? 0.0 : 1.0;\n").append("  bool whole = uPlants != 0 || uModels != 0;\n").append("  gl_TessLevelOuter[0] = keep * (whole ? 1.0 : vp_pieces(vp_place_v[1], vp_place_v[2]));\n").append("  gl_TessLevelOuter[1] = keep * (whole ? 1.0 : vp_pieces(vp_place_v[2], vp_place_v[0]));\n").append("  gl_TessLevelOuter[2] = keep * (whole ? 1.0 : vp_pieces(vp_place_v[0], vp_place_v[1]));\n").append("  gl_TessLevelInner[0] = max(gl_TessLevelOuter[0], max(gl_TessLevelOuter[1],").append(" gl_TessLevelOuter[2]));\n}\n").toString();
    }

    public static String evaluation(IrisTransform.Stage stage, String string, IrisAssembler.Source source) {
        List<Attribute> list = Tessellation.attributes(string);
        String string2 = ATTRIBUTE.matcher(string).replaceAll("$1 $2;");
        string2 = INSTANCE_ID.matcher(VERTEX_ID.matcher(string2).replaceAll("vp_vertexId")).replaceAll("vp_instanceId");
        StringBuilder stringBuilder = IrisAssembler.header(stage, "int vp_vertexId;\nint vp_instanceId;\n" + string2, false);
        stringBuilder.insert(stringBuilder.indexOf("\n") + 1, EXTENSION);
        stringBuilder.append("layout(triangles, equal_spacing) in;\n");
        for (Attribute object : list) {
            stringBuilder.append(object.declared("in", "_c[]"));
        }
        stringBuilder.append("flat in int vp_vertexId_c[];\nflat in int vp_instanceId_c[];\n");
        IrisAssembler.inputs(stringBuilder, stage);
        String string3 = VERTEX_ID.matcher(stage.code).replaceAll("vp_vertexId");
        stringBuilder.append(INSTANCE_ID.matcher(string3).replaceAll("vp_instanceId"));
        stringBuilder.append("\n#line 1 ").append(999).append("\nvoid main() {\n");
        stringBuilder.append("  vec3 t = gl_TessCoord;\n");
        for (Attribute attribute : list) {
            String string4 = attribute.name + "_c";
            stringBuilder.append("  ").append(attribute.name).append(" = ");
            stringBuilder.append(attribute.varying() ? "t.x * " + string4 + "[0] + t.y * " + string4 + "[1] + t.z * " + string4 + "[2];\n" : string4 + "[0];\n");
        }
        stringBuilder.append("  vp_vertexId = t.x == 1.0 ? vp_vertexId_c[0] : t.y == 1.0 ? vp_vertexId_c[1]").append(" : t.z == 1.0 ? vp_vertexId_c[2] : -1;\n  vp_instanceId = vp_instanceId_c[0];\n");
        IrisAssembler.vertexMain(stringBuilder, stage, source);
        return stringBuilder.append("}\n").toString();
    }

    private static String fetch(IrisAssembler.Source source) {
        return switch (source) {
            case IrisAssembler.Source.MESH -> "vp_fetchMesh()";
            case IrisAssembler.Source.FAR_BOX -> "vp_fetchFarBox()";
            default -> throw new IllegalArgumentException(String.valueOf((Object)source) + " is not tessellated");
        };
    }

    record Attribute(String type, String name) {
        boolean varying() {
            String string = this.name.startsWith("vp_") ? this.name.substring("vp_".length()) : this.name;
            return VARYING.contains(string);
        }

        String declared(String string, String string2) {
            boolean bl = this.type.startsWith("int") || this.type.startsWith("uint") || this.type.matches("[iu]vec\\d");
            return (bl ? "flat " : "") + string + " " + this.type + " " + this.name + string2 + ";\n";
        }
    }
}

