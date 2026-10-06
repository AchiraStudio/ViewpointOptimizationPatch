/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import viewpoint.iris.GlslNamespace;
import viewpoint.iris.IrisAssembler;
import viewpoint.iris.Preprocessor;
import viewpoint.platform.ShaderLibrary;

final class IrisLibrary {
    private final Map<String, String> pieces = new HashMap<String, String>();
    private final Map<String, String> macros = ShaderLibrary.pipelineMacros();

    IrisLibrary() {
    }

    String vertex(IrisAssembler.Source source) {
        return switch (source) {
            default -> throw new IncompatibleClassChangeError();
            case IrisAssembler.Source.MESH -> this.piece("iris/common.glsl") + this.piece("iris/mesh_vertex.glsl");
            case IrisAssembler.Source.MODEL, IrisAssembler.Source.VEHICLE -> this.piece("iris/common.glsl") + this.piece("iris/model_vertex.glsl");
            case IrisAssembler.Source.PLAIN -> this.piece("iris/common.glsl") + this.piece("iris/plain_vertex.glsl");
            case IrisAssembler.Source.WEATHER -> this.piece("iris/common.glsl") + this.piece("iris/weather_vertex.glsl");
            case IrisAssembler.Source.FAR_BOX, IrisAssembler.Source.FAR_TREE -> this.piece("iris/common.glsl") + this.piece("iris/far_vertex.glsl");
            case IrisAssembler.Source.FAR_SHELL -> this.piece("iris/common.glsl") + this.piece("iris/far_shell_vertex.glsl");
            case IrisAssembler.Source.QUAD -> this.piece("iris/common.glsl") + this.piece("iris/quad_vertex.glsl");
        };
    }

    String geometry() {
        return this.piece("iris/common.glsl");
    }

    String fragment(IrisAssembler.Source source) {
        String string = this.piece("iris/common.glsl");
        return string + (switch (source) {
            case IrisAssembler.Source.MESH -> this.piece("iris/mesh_fragment.glsl");
            case IrisAssembler.Source.MODEL -> this.piece("iris/model_fragment.glsl");
            case IrisAssembler.Source.VEHICLE -> this.piece("iris/vehicle_fragment.glsl");
            case IrisAssembler.Source.FAR_BOX, IrisAssembler.Source.FAR_TREE -> this.piece("iris/far_fragment.glsl");
            case IrisAssembler.Source.FAR_SHELL -> this.piece("iris/far_shell_fragment.glsl");
            default -> this.piece("iris/plain_fragment.glsl");
        });
    }

    String composite(boolean bl) {
        return this.piece("iris/common.glsl") + (bl ? this.piece("iris/composite_fragment.glsl") : "");
    }

    private String piece(String string) {
        return this.pieces.computeIfAbsent(string, this::load);
    }

    private String load(String string) {
        try {
            Preprocessor preprocessor = new Preprocessor(ShaderLibrary::read, true, this.macros, Map.of(), Set.of());
            String string2 = preprocessor.run(string).text().replaceAll("(?m)^#line.*$", "");
            return GlslNamespace.prefixed(string2) + "\n";
        }
        catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("the bridge's " + string + ": " + exception.getMessage(), exception);
        }
    }
}

