/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import viewpoint.iris.GlslDeclarations;
import viewpoint.iris.GlslTokens;

public final class SmoothNormals {
    private static final Set<String> FLOAT_TYPES = Set.of("float", "vec2", "vec3", "vec4", "mat2", "mat3", "mat4", "mat2x2", "mat2x3", "mat2x4", "mat3x2", "mat3x3", "mat3x4", "mat4x2", "mat4x3", "mat4x4");
    private static final List<String> FRAME = List.of("normal", "tangent", "tbn");

    private SmoothNormals() {
    }

    public static String unflatten(String string) {
        List<GlslTokens.Token> list = GlslTokens.lex(string);
        boolean bl = false;
        for (GlslDeclarations.Declaration declaration : GlslDeclarations.read(list).declarations) {
            if (!declaration.has("flat") || !FLOAT_TYPES.contains(declaration.type) || !declaration.has("in") && !declaration.has("out") && !declaration.has("varying") || !declaration.names.stream().allMatch(SmoothNormals::isFrame)) continue;
            for (int i = declaration.first; i <= declaration.last; ++i) {
                if (!list.get(i).is("flat")) continue;
                list.get((int)i).text = "";
                bl = true;
            }
        }
        return bl ? GlslTokens.join(list) : string;
    }

    private static boolean isFrame(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        return FRAME.stream().anyMatch(string2::contains);
    }
}

