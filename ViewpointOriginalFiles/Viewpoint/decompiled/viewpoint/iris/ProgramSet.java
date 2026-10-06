/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import viewpoint.iris.Macros;
import viewpoint.iris.PackSource;
import viewpoint.iris.ShadersProperties;

public final class ProgramSet {
    private static final Map<String, String> FALLBACK;
    private static final String[] GEOMETRY;
    public final String folder;
    private final Map<String, Program> geometry = new LinkedHashMap<String, Program>();
    private final Map<Stage, List<Program>> passes = new LinkedHashMap<Stage, List<Program>>();

    private ProgramSet(String string) {
        this.folder = string;
    }

    public static ProgramSet find(PackSource packSource, String string, Predicate<String> predicate) {
        Object object;
        ProgramSet programSet = new ProgramSet(string);
        for (String string2 : GEOMETRY) {
            object = programSet.program(packSource, string2, predicate);
            if (object == null || !((Program)object).drawable()) continue;
            programSet.geometry.put(string2, (Program)object);
        }
        for (Stage stage : Stage.values()) {
            object = new ArrayList();
            int n = stage == Stage.FINAL ? 1 : 100;
            for (int i = 0; i < n; ++i) {
                Object object2 = i == 0 ? stage.prefix : stage.prefix + i;
                Program program = programSet.program(packSource, (String)object2, predicate);
                if (program == null || program.fragment == null && program.computes.isEmpty()) continue;
                object.add(program);
            }
            programSet.passes.put(stage, List.copyOf(object));
        }
        return programSet;
    }

    public static String overworld(PackSource packSource, String string) {
        if (string != null) {
            for (String string2 : string.split("\n")) {
                List<String> list;
                String string3 = Macros.stripComments(string2).strip();
                int n = string3.indexOf(61);
                if (!string3.startsWith("dimension.") || n < 0 || !(list = ShadersProperties.words(string3.substring(n + 1))).contains("minecraft:overworld") && !list.contains("overworld") && !list.contains("*")) continue;
                return string3.substring(10, n).strip() + "/";
            }
        }
        for (String string4 : packSource.paths()) {
            if (!string4.startsWith("world0/") || !string4.endsWith(".fsh")) continue;
            return "world0/";
        }
        return "";
    }

    private Program program(PackSource packSource, String string, Predicate<String> predicate) {
        String string2 = this.folder + string;
        if (!predicate.test(string)) {
            return null;
        }
        String string3 = ProgramSet.file(packSource, string2 + ".vsh");
        String string4 = ProgramSet.file(packSource, string2 + ".fsh");
        String string5 = ProgramSet.file(packSource, string2 + ".gsh");
        ArrayList<String> arrayList = new ArrayList<String>();
        String string6 = ProgramSet.file(packSource, string2 + ".csh");
        if (string6 != null) {
            arrayList.add(string6);
        }
        for (char c = 'a'; c <= 'z'; c = (char)(c + '\u0001')) {
            String string7 = ProgramSet.file(packSource, string2 + "_" + c + ".csh");
            if (string7 == null) continue;
            arrayList.add(string7);
        }
        if (string3 == null && string4 == null && arrayList.isEmpty()) {
            return null;
        }
        return new Program(string, string3, string5, string4, List.copyOf(arrayList));
    }

    private static String file(PackSource packSource, String string) {
        return packSource.exists(string) ? string : null;
    }

    public Program geometry(String string) {
        String string2 = string;
        while (string2 != null) {
            Program program = this.geometry.get(string2);
            if (program != null) {
                return program;
            }
            string2 = FALLBACK.get(string2);
        }
        return null;
    }

    public Map<String, Program> geometryPrograms() {
        return this.geometry;
    }

    public List<Program> passes(Stage stage) {
        return this.passes.getOrDefault((Object)stage, List.of());
    }

    static {
        String[][] stringArrayArray;
        FALLBACK = new LinkedHashMap<String, String>();
        for (String[] stringArray : stringArrayArray = new String[][]{{"gbuffers_line", "gbuffers_basic"}, {"gbuffers_textured", "gbuffers_basic"}, {"gbuffers_textured_lit", "gbuffers_textured"}, {"gbuffers_skybasic", "gbuffers_basic"}, {"gbuffers_skytextured", "gbuffers_textured"}, {"gbuffers_clouds", "gbuffers_textured"}, {"gbuffers_terrain", "gbuffers_textured_lit"}, {"gbuffers_terrain_solid", "gbuffers_terrain"}, {"gbuffers_terrain_cutout", "gbuffers_terrain"}, {"gbuffers_damagedblock", "gbuffers_terrain"}, {"gbuffers_block", "gbuffers_terrain"}, {"gbuffers_block_translucent", "gbuffers_block"}, {"gbuffers_beaconbeam", "gbuffers_textured"}, {"gbuffers_entities", "gbuffers_textured_lit"}, {"gbuffers_entities_translucent", "gbuffers_entities"}, {"gbuffers_entities_glowing", "gbuffers_entities"}, {"gbuffers_armor_glint", "gbuffers_textured"}, {"gbuffers_spidereyes", "gbuffers_textured"}, {"gbuffers_hand", "gbuffers_textured_lit"}, {"gbuffers_hand_water", "gbuffers_hand"}, {"gbuffers_weather", "gbuffers_textured_lit"}, {"gbuffers_water", "gbuffers_terrain"}, {"gbuffers_particles", "gbuffers_textured_lit"}, {"gbuffers_particles_translucent", "gbuffers_particles"}, {"gbuffers_lightning", "gbuffers_entities"}, {"shadow_solid", "shadow"}, {"shadow_cutout", "shadow"}, {"shadow_water", "shadow"}, {"shadow_entities", "shadow"}, {"shadow_block", "shadow"}, {"shadow_lightning", "shadow_entities"}, {"dh_water", "dh_terrain"}}) {
            FALLBACK.put(stringArray[0], stringArray[1]);
        }
        GEOMETRY = new String[]{"gbuffers_basic", "gbuffers_line", "gbuffers_textured", "gbuffers_textured_lit", "gbuffers_skybasic", "gbuffers_skytextured", "gbuffers_clouds", "gbuffers_terrain", "gbuffers_terrain_solid", "gbuffers_terrain_cutout", "gbuffers_damagedblock", "gbuffers_block", "gbuffers_block_translucent", "gbuffers_beaconbeam", "gbuffers_entities", "gbuffers_entities_translucent", "gbuffers_entities_glowing", "gbuffers_armor_glint", "gbuffers_spidereyes", "gbuffers_hand", "gbuffers_hand_water", "gbuffers_weather", "gbuffers_water", "gbuffers_particles", "gbuffers_particles_translucent", "gbuffers_lightning", "shadow", "shadow_solid", "shadow_cutout", "shadow_water", "shadow_entities", "shadow_block", "shadow_lightning", "dh_terrain", "dh_water", "dh_shadow"};
    }

    public record Program(String name, String vertex, String geometry, String fragment, List<String> computes) {
        public boolean drawable() {
            return this.vertex != null && this.fragment != null;
        }
    }

    public static enum Stage {
        SETUP("setup"),
        BEGIN("begin"),
        SHADOWCOMP("shadowcomp"),
        PREPARE("prepare"),
        DEFERRED("deferred"),
        COMPOSITE("composite"),
        FINAL("final");

        public final String prefix;

        private Stage(String string2) {
            this.prefix = string2;
        }
    }
}

