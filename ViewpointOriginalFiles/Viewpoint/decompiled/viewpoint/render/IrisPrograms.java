/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import viewpoint.iris.Directives;
import viewpoint.iris.GeometryPassThrough;
import viewpoint.iris.IrisAssembler;
import viewpoint.iris.IrisPack;
import viewpoint.iris.IrisTransform;
import viewpoint.iris.Preprocessor;
import viewpoint.iris.ProgramSet;
import viewpoint.iris.SmoothNormals;
import viewpoint.iris.Tessellation;
import viewpoint.platform.GlProgram;
import viewpoint.render.FlashShadow;
import viewpoint.render.IrisBindings;
import viewpoint.render.IrisLibrary;
import viewpoint.render.IrisProgram;
import viewpoint.render.LampShadows;
import viewpoint.render.VehiclePass;

final class IrisPrograms {
    static volatile File sources;
    private static final Pattern IVEC3;
    private static final Pattern VEC2;
    private static final int MAX_DRAW_BUFFERS = 8;
    final Map<Draw, IrisProgram> draws = new EnumMap<Draw, IrisProgram>(Draw.class);
    final Map<ProgramSet.Stage, List<Pass>> passes = new EnumMap<ProgramSet.Stage, List<Pass>>(ProgramSet.Stage.class);
    final Map<String, String> constants = new LinkedHashMap<String, String>();
    final Set<Integer> buffers = new LinkedHashSet<Integer>();
    final Set<Integer> shadowColors = new LinkedHashSet<Integer>();
    final List<String> notes = new ArrayList<String>();
    private final IrisPack pack;
    private final IrisPack.Configured configured;
    private final Map<String, String> standard;
    private final Map<String, String> forced;
    private final IrisLibrary library = new IrisLibrary();

    private IrisPrograms(IrisPack irisPack, IrisPack.Configured configured, Map<String, String> map) {
        this.pack = irisPack;
        this.configured = configured;
        this.standard = map;
        this.forced = irisPack.clock ? Map.of("sunPathRotation", "-26.0") : Map.of();
    }

    static IrisPrograms link(IrisPack irisPack, IrisPack.Configured configured, Map<String, String> map) {
        IrisPrograms irisPrograms = new IrisPrograms(irisPack, configured, map);
        try {
            Object object;
            for (Draw enum_ : Draw.values()) {
                object = configured.programs().geometry(enum_.program);
                if (object == null) continue;
                irisPrograms.draws.put(enum_, irisPrograms.geometry(enum_, (ProgramSet.Program)object));
            }
            for (Enum enum_ : ProgramSet.Stage.values()) {
                object = new ArrayList();
                for (ProgramSet.Program program : configured.programs().passes((ProgramSet.Stage)enum_)) {
                    object.add(irisPrograms.pass(program));
                }
                irisPrograms.passes.put((ProgramSet.Stage)enum_, (List<Pass>)object);
            }
        }
        catch (IOException | IllegalArgumentException exception) {
            irisPrograms.delete();
            throw new IllegalStateException(irisPack.id + ": " + exception.getMessage(), exception);
        }
        catch (IllegalStateException illegalStateException) {
            irisPrograms.delete();
            throw illegalStateException;
        }
        return irisPrograms;
    }

    private IrisProgram geometry(Draw draw, ProgramSet.Program program) throws IOException {
        Preprocessor.Result result = this.pack.preprocess(this.configured, program.vertex(), this.standard, this.forced);
        Preprocessor.Result result2 = this.pack.preprocess(this.configured, program.fragment(), this.standard, this.forced);
        IrisPrograms.errors(program.name(), result, result2);
        boolean bl = draw.source.name().startsWith("FAR_");
        IrisTransform.Stage stage = IrisTransform.vertex(result, IrisTransform.Kind.GEOMETRY, bl);
        IrisTransform.Stage stage2 = IrisTransform.fragment(result2, IrisTransform.Kind.GEOMETRY, stage.lightmap, stage.colour);
        if (IrisPrograms.smooth(draw)) {
            stage.code = SmoothNormals.unflatten(stage.code);
            stage2.code = SmoothNormals.unflatten(stage2.code);
        }
        List<Integer> list = IrisPrograms.drawBuffers(result2.text(), stage2);
        boolean bl2 = draw == Draw.SHADOW || draw == Draw.SHADOW_MODELS || draw == Draw.DH_SHADOW;
        (bl2 ? this.shadowColors : this.buffers).addAll(list);
        this.directives(result.text(), result2.text());
        int n = IrisPrograms.tintOutput(draw, list, stage2);
        String string = IrisAssembler.fragment(stage2, this.library.fragment(draw.source), draw.source, n);
        this.notes(program.name(), stage, stage2);
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>(stage.samplers);
        linkedHashMap.putAll(stage2.samplers);
        linkedHashMap.put("vp_albedoTex", "sampler2D");
        GlProgram.Sources sources = this.stages(draw, program, stage, string, linkedHashMap);
        GlProgram glProgram = IrisPrograms.link(this.pack.id + " " + program.name(), sources, Map.of(), IrisPrograms.outputs(stage2), result2.files());
        IrisProgram irisProgram = new IrisProgram(program.name(), glProgram, list, n, linkedHashMap);
        glProgram.sampler("uLight", 5);
        glProgram.sampler("uPalettes", 31);
        glProgram.sampler("uDraws", 40);
        glProgram.sampler("uRainMap", 24);
        glProgram.sampler("uTop", 25);
        glProgram.sampler("uSide", 26);
        glProgram.sampler("uMask", 27);
        glProgram.sampler("uTrees", 43);
        glProgram.sampler("uKinds", 44);
        glProgram.sampler("uShellMask", 28);
        glProgram.sampler("uTexture", 0);
        glProgram.sampler("uFloorPages", 41);
        glProgram.sampler("uTreeAtlas", 42);
        glProgram.sampler("uPlantSlots", 48);
        glProgram.sampler("uPackTable", 88);
        glProgram.sampler("uMeshRecords", 89);
        glProgram.sampler("uCardTexels", 90);
        for (int i = 0; draw.source == IrisAssembler.Source.MODEL && i < 16; ++i) {
            glProgram.sampler("uTextures[" + i + "]", 64 + i);
        }
        if (draw.source == IrisAssembler.Source.VEHICLE) {
            VehiclePass.samplers(glProgram);
        }
        FlashShadow.sampler(glProgram);
        LampShadows.sampler(glProgram);
        irisProgram.checkUnits();
        this.referenced(linkedHashMap);
        return irisProgram;
    }

    private Pass pass(ProgramSet.Program program) throws IOException {
        Object object;
        List<Integer> list;
        Object object2;
        Object object3;
        ArrayList<float[]> arrayList;
        Object object4;
        Object object5;
        IrisProgram irisProgram = null;
        if (program.fragment() != null) {
            object5 = program.vertex();
            object4 = this.pack.preprocess(this.configured, program.fragment(), this.standard, this.forced);
            arrayList = object5 == null ? null : this.pack.preprocess(this.configured, (String)object5, this.standard, this.forced);
            IrisPrograms.errors(program.name(), arrayList, (Preprocessor.Result)object4);
            IrisTransform.Stage stage = IrisTransform.fragment((Preprocessor.Result)object4, IrisTransform.Kind.COMPOSITE, null);
            String string = IrisAssembler.plain(stage, this.library.composite(stage.lampTints > 0));
            object3 = null;
            object2 = new LinkedHashMap<String, String>(stage.samplers);
            if (stage.lampTints > 0) {
                object2.put("vp_tintTex", "sampler2D");
            }
            if (arrayList != null) {
                list = IrisTransform.vertex(arrayList, IrisTransform.Kind.COMPOSITE);
                object2.putAll(((IrisTransform.Stage)((Object)list)).samplers);
                object3 = IrisAssembler.vertex(list, this.library.vertex(IrisAssembler.Source.QUAD), IrisAssembler.Source.QUAD);
                this.directives(((Preprocessor.Result)((Object)arrayList)).text(), null);
            }
            list = IrisPrograms.drawBuffers(((Preprocessor.Result)object4).text(), stage);
            this.buffers.addAll(list);
            this.directives(((Preprocessor.Result)object4).text(), null);
            object = IrisPrograms.link(this.pack.id + " " + program.name(), new GlProgram.Sources((String)object3, null, null, null, string), Map.of("vp_aQuad", 0), IrisPrograms.outputs(stage), ((Preprocessor.Result)object4).files());
            irisProgram = new IrisProgram(program.name(), (GlProgram)object, list, -1, (Map<String, String>)object2);
            irisProgram.checkUnits();
            this.referenced((Map<String, String>)object2);
        }
        object5 = new ArrayList();
        object4 = new ArrayList();
        arrayList = new ArrayList<float[]>();
        for (String string : program.computes()) {
            object3 = this.pack.preprocess(this.configured, string, this.standard, this.forced);
            IrisPrograms.errors(string, (Preprocessor.Result)object3, null);
            object2 = IrisTransform.plain((Preprocessor.Result)object3, IrisTransform.Kind.COMPUTE);
            list = GlProgram.compute(this.pack.id + " " + string, IrisAssembler.plain((IrisTransform.Stage)object2, ""), ((Preprocessor.Result)object3).files());
            object = new IrisProgram(string, (GlProgram)((Object)list), List.of(), -1, ((IrisTransform.Stage)object2).samplers);
            ((IrisProgram)object).checkUnits();
            object5.add(object);
            Directives directives = Directives.scan(((Preprocessor.Result)object3).text());
            object4.add(IrisPrograms.groups(directives));
            arrayList.add(IrisPrograms.scale(directives));
            this.referenced(((IrisTransform.Stage)object2).samplers);
        }
        return new Pass(program.name(), irisProgram, (List<IrisProgram>)object5, (List<int[]>)object4, arrayList);
    }

    private static int tintOutput(Draw draw, List<Integer> list, IrisTransform.Stage stage) {
        int n = Math.max(list.size(), stage.outputs.size());
        for (int n2 : stage.outputs.values()) {
            n = Math.max(n, n2 + 1);
        }
        return draw.tint && n < 8 ? n : -1;
    }

    private static int[] groups(Directives directives) {
        int[] nArray;
        Matcher matcher;
        String string = directives.constants.get("workGroups");
        if (string != null && (matcher = IVEC3.matcher(string)).find()) {
            return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))};
        }
        if (directives.constants.containsKey("workGroupsRender")) {
            nArray = null;
        } else {
            int[] nArray2 = new int[3];
            nArray2[0] = 1;
            nArray2[1] = 1;
            nArray = nArray2;
            nArray2[2] = 1;
        }
        return nArray;
    }

    private static float[] scale(Directives directives) {
        Matcher matcher;
        String string = directives.constants.get("workGroupsRender");
        float f = 1.0f;
        float f2 = 1.0f;
        if (string != null && (matcher = VEC2.matcher(string)).find()) {
            f = Float.parseFloat(matcher.group(1));
            f2 = Float.parseFloat(matcher.group(2));
        }
        return new float[]{f / (float)directives.localSize[0], f2 / (float)directives.localSize[1]};
    }

    private static List<Integer> drawBuffers(String string, IrisTransform.Stage stage) {
        Directives directives = Directives.scan(string);
        if (directives.drawBuffers != null) {
            return List.copyOf(directives.drawBuffers);
        }
        int n = stage.fragData ? IrisPrograms.highestFragData(stage.code) + 1 : stage.outputs.size();
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (int i = 0; i < Math.max(n, 1); ++i) {
            arrayList.add(i);
        }
        return arrayList;
    }

    private static int highestFragData(String string) {
        Matcher matcher = Pattern.compile("gl_FragData\\s*\\[\\s*(\\d+)\\s*\\]").matcher(string);
        int n = 0;
        while (matcher.find()) {
            n = Math.max(n, Integer.parseInt(matcher.group(1)));
        }
        return n;
    }

    private static Map<String, Integer> outputs(IrisTransform.Stage stage) {
        LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<String, Integer>();
        int n = 0;
        for (Map.Entry<String, Integer> entry : stage.outputs.entrySet()) {
            if (entry.getValue() < 0) {
                Matcher matcher;
                linkedHashMap.put(entry.getKey(), (matcher = Pattern.compile("outColor(\\d)").matcher(entry.getKey())).matches() ? Integer.parseInt(matcher.group(1)) : n);
            }
            ++n;
        }
        return linkedHashMap;
    }

    private void directives(String string, String string2) {
        for (String string3 : new String[]{string, string2}) {
            if (string3 == null) continue;
            Directives.scan((String)string3).constants.forEach(this.constants::putIfAbsent);
        }
    }

    private void referenced(Map<String, String> map) {
        for (String string : map.keySet()) {
            int n;
            int n2 = IrisBindings.colortex(string);
            if (n2 >= 0) {
                this.buffers.add(n2);
            }
            if ((n = IrisBindings.shadowcolor(string)) < 0) continue;
            this.shadowColors.add(n);
        }
    }

    private GlProgram.Sources stages(Draw draw, ProgramSet.Program program, IrisTransform.Stage stage, String string, Map<String, String> map) throws IOException {
        String string2;
        String string3 = this.library.vertex(draw.source);
        boolean bl = draw.tessellated();
        String string4 = bl ? Tessellation.vertex(string3, draw.source) : IrisAssembler.vertex(stage, string3, draw.source);
        String string5 = bl ? Tessellation.control(string3) : null;
        String string6 = string2 = bl ? Tessellation.evaluation(stage, string3, draw.source) : null;
        if (program.geometry() == null) {
            return new GlProgram.Sources(string4, string5, string2, null, string);
        }
        Preprocessor.Result result = this.pack.preprocess(this.configured, program.geometry(), this.standard, this.forced);
        IrisPrograms.errors(program.name(), result, null);
        IrisTransform.Stage stage2 = GeometryPassThrough.stage(result);
        if (IrisPrograms.smooth(draw)) {
            stage2.code = SmoothNormals.unflatten(stage2.code);
        }
        map.putAll(stage2.samplers);
        String string7 = GeometryPassThrough.geometry(stage2, this.library.geometry(), bl ? string2 : string4);
        return bl ? new GlProgram.Sources(string4, string5, GeometryPassThrough.renamed(string2), string7, string) : new GlProgram.Sources(GeometryPassThrough.renamed(string4), null, null, string7, string);
    }

    private static boolean smooth(Draw draw) {
        return draw.source == IrisAssembler.Source.MODEL || draw.source == IrisAssembler.Source.VEHICLE;
    }

    private static GlProgram link(String string, GlProgram.Sources sources, Map<String, Integer> map, Map<String, Integer> map2, List<String> list) {
        if (IrisPrograms.sources != null) {
            String[] stringArray = new String[]{sources.vertex(), sources.control(), sources.evaluation(), sources.geometry(), sources.fragment()};
            String[] stringArray2 = new String[]{"vs", "tcs", "tes", "gs", "fs"};
            for (int i = 0; i < stringArray.length; ++i) {
                if (stringArray[i] == null) continue;
                try {
                    Files.writeString(IrisPrograms.sources.toPath().resolve(string.replace(' ', '.') + "." + stringArray2[i]), (CharSequence)stringArray[i], new OpenOption[0]);
                    continue;
                }
                catch (IOException iOException) {
                    throw new UncheckedIOException(iOException);
                }
            }
        }
        return GlProgram.fromSources(string, sources, map, map2, list);
    }

    private static void errors(String string, Preprocessor.Result result, Preprocessor.Result result2) {
        ArrayList<String> arrayList = new ArrayList<String>();
        if (result != null) {
            arrayList.addAll(result.errors());
        }
        if (result2 != null) {
            arrayList.addAll(result2.errors());
        }
        if (!arrayList.isEmpty()) {
            throw new IllegalArgumentException(string + ": " + String.join((CharSequence)"; ", arrayList));
        }
    }

    private void notes(String string, IrisTransform.Stage stage, IrisTransform.Stage stage2) {
        for (String string2 : stage.notes) {
            this.notes.add(string + ": " + string2);
        }
        for (String string2 : stage2.notes) {
            this.notes.add(string + ": " + string2);
        }
    }

    void delete() {
        for (IrisProgram object : this.draws.values()) {
            object.delete();
        }
        for (List list : this.passes.values()) {
            for (Pass pass : list) {
                if (pass.program() != null) {
                    pass.program().delete();
                }
                pass.computes().forEach(IrisProgram::delete);
            }
        }
    }

    static {
        IVEC3 = Pattern.compile("ivec3\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");
        VEC2 = Pattern.compile("vec2\\s*\\(\\s*([\\d.]+)\\s*,\\s*([\\d.]+)\\s*\\)");
    }

    static enum Draw {
        TERRAIN("gbuffers_terrain", IrisAssembler.Source.MESH, true),
        WATER("gbuffers_water", IrisAssembler.Source.MESH, false),
        SKY("gbuffers_skybasic", IrisAssembler.Source.PLAIN, false),
        SKY_TEXTURED("gbuffers_skytextured", IrisAssembler.Source.PLAIN, false),
        ENTITIES("gbuffers_entities", IrisAssembler.Source.MODEL, true),
        VEHICLES("gbuffers_entities", IrisAssembler.Source.VEHICLE, true),
        BLOCKS("gbuffers_block", IrisAssembler.Source.MODEL, true),
        VEHICLE_GLASS("gbuffers_entities_translucent", IrisAssembler.Source.VEHICLE, false),
        WEATHER("gbuffers_weather", IrisAssembler.Source.WEATHER, false),
        LIGHTNING("gbuffers_lightning", IrisAssembler.Source.PLAIN, false),
        DH_TERRAIN("dh_terrain", IrisAssembler.Source.FAR_BOX, false),
        DH_TREES("dh_terrain", IrisAssembler.Source.FAR_TREE, false),
        DH_SHELL("dh_terrain", IrisAssembler.Source.FAR_SHELL, false),
        DH_SHADOW("dh_shadow", IrisAssembler.Source.FAR_BOX, false),
        SHADOW("shadow", IrisAssembler.Source.MESH, false),
        SHADOW_MODELS("shadow", IrisAssembler.Source.MODEL, false);

        final String program;
        final IrisAssembler.Source source;
        final boolean tint;

        boolean tessellated() {
            return this == SHADOW || this == DH_SHADOW || this == TERRAIN || this == WATER;
        }

        private Draw(String string2, IrisAssembler.Source source, boolean bl) {
            this.program = string2;
            this.source = source;
            this.tint = bl;
        }
    }

    record Pass(String name, IrisProgram program, List<IrisProgram> computes, List<int[]> groups, List<float[]> scales) {
    }
}

