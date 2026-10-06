/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import viewpoint.platform.Settings;
import viewpoint.platform.ShaderPack;
import viewpoint.platform.ShaderPacks;

final class Shaders {
    private static final String ROOT = "/viewpoint/shaders/";
    static final String CONSTANTS = "constants.glsl";
    static final Set<String> DRIVERS = Set.of("lib/weather.glsl", "precip.vert");
    private static final Pattern INCLUDE = Pattern.compile("^\\s*#include\\s+\"([^\"]+)\"\\s*$");

    static Source load(String string) {
        return Shaders.load(string, ShaderPacks.active());
    }

    static Source load(String string, ShaderPacks.Active active) {
        ArrayList<String> arrayList = new ArrayList<String>();
        StringBuilder stringBuilder = new StringBuilder(8192);
        boolean[] blArray = new boolean[1];
        Shaders.expand(string, active, arrayList, new ArrayList<String>(), stringBuilder, blArray);
        return new Source(stringBuilder.toString(), arrayList, blArray[0]);
    }

    private static void expand(String string, ShaderPacks.Active active, List<String> list, List<String> list2, StringBuilder stringBuilder, boolean[] blArray) {
        if (list2.contains(string)) {
            throw new IllegalStateException("shader include cycle: " + String.join((CharSequence)" > ", list2) + " > " + string);
        }
        list2.add(string);
        int n = list.size();
        list.add(string);
        String string2 = Shaders.read(string, active, blArray);
        int n2 = 0;
        int n3 = 0;
        while (n3 < string2.length()) {
            int n4 = string2.indexOf(10, n3);
            n4 = n4 < 0 ? string2.length() : n4 + 1;
            String string3 = string2.substring(n3, n4);
            ++n2;
            Matcher matcher = INCLUDE.matcher(string3.stripTrailing());
            if (matcher.matches()) {
                stringBuilder.append("#line 1 ").append(list.size()).append('\n');
                Shaders.expand(matcher.group(1), active, list, list2, stringBuilder, blArray);
                stringBuilder.append("#line ").append(n2 + 1).append(' ').append(n).append('\n');
            } else if (n == 0 && n2 == 1 && string3.startsWith("#version")) {
                stringBuilder.append(string3).append(active.defines).append("#line 2 0\n");
            } else {
                stringBuilder.append(string3);
            }
            n3 = n4;
        }
        list2.remove(list2.size() - 1);
    }

    private static String read(String string, ShaderPacks.Active active, boolean[] blArray) {
        boolean bl = string.equals(CONSTANTS) || DRIVERS.contains(string);
        ShaderPack shaderPack = active.pack;
        while (shaderPack != null && !bl) {
            String string2 = shaderPack.shader(string);
            if (string2 != null) {
                blArray[0] = true;
                return string2;
            }
            shaderPack = shaderPack.base;
        }
        return Shaders.read(string);
    }

    static String read(String string) {
        String string2;
        block11: {
            if (string.equals(CONSTANTS)) {
                return Shaders.constants();
            }
            File file = Shaders.devFile(string);
            if (file != null) {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            }
            InputStream inputStream = Shaders.class.getResourceAsStream(ROOT + string);
            try {
                if (inputStream == null) {
                    throw new IllegalStateException("shader file missing from the jar: " + string);
                }
                string2 = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
                if (inputStream == null) break block11;
            }
            catch (Throwable throwable) {
                try {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException iOException) {
                    throw new IllegalStateException("cannot read shader " + string + ": " + String.valueOf(iOException), iOException);
                }
            }
            inputStream.close();
        }
        return string2;
    }

    private static String constants() {
        return "#ifndef VIEWPOINT_CONSTANTS\n#define VIEWPOINT_CONSTANTS\n// Generated from Java (Shaders.constants): the chunk mesh vertex's flag bits (VertexFormat).\n#define FLAG_SINGLE_SIDED 1\n#define FLAG_DECAL 2\n#define FLAG_SOLID_FLOOR 4\n#define FLAG_COVER 8\n#define FLAG_BAKED_FLOOR 64\n#define FLAG_BILLBOARD 128\n#define FLAG_FACING 512\n#define FLAG_GLASS 1024\n#define FLAG_WATER 2048\n#define FLAG_GRASS 4096\n#define PLANE_SHIFT 16\n#define PLANE_MASK 7\n#define PLANE_BITS 3\n#define PLANT_VERTICES 6\n#define PLANT_TEXELS 7\n#define FLAG_LAYER_SHIFT " + Integer.numberOfTrailingZeros(16) + "\n#define FLAG_ROOTED 256\n// A model pack's instance's slot (VertexFormat.MODEL_*): where its flags, square and model sit.\n#define MODEL_FLAGS 6\n#define MODEL_SQUARE 7\n#define MODEL_INDEX 8\n#define NO_SQUARE 64\n// A dead body's card's texels (VertexFormat.CARD_*).\n#define CARD_TEXELS 7\n// The owned models' textures a group binds, and the slot's shift in the instance's draw index (Gl).\n#define MODEL_TEXTURES 16\n#define MODEL_SLOT_SHIFT 24\n#define ROOT_X_SHIFT 16\n#define ROOT_Y_SHIFT 24\n// A storey's height in scene units (Space.SQRT6).\n#define SQRT6 2.4494896\n// Squares from the eye the far shell is drawn into the G-buffer (Space.SHELL_BAND).\n#define SHELL_BAND 256.0\n// Scene units over its floor a lamp's light starts from (Space.LAMP_HEIGHT).\n#define LAMP_HEIGHT 2.1\n// The least strength of the sun or the moon its maps are drawn for (Space.SUN_LEAST).\n#define SUN_LEAST 0.02\n// Light grids across the light atlas (LightGridFormat), each LIGHT_GRID texels.\n#define LIGHT_COLUMNS 204.0\n#define LIGHT_GRID vec2(20.0, 40.0)\n// Levels of the sky's light lost a square from open sky, in a grid's bottom half (LightGridFormat).\n#define SKY_STEP 16.0\n// The far shell's band's table of grids (render.BandLight): chunks a side, and levels.\n#define BAND_WINDOW 72\n#define BAND_LEVELS 32\n// The far cells' light pages' table (render.CellLightPages): cells a side.\n#define CELL_WINDOW 16\n#endif\n";
    }

    static long modified(String string, ShaderPacks.Active active) {
        Object object = active.pack;
        while (object != null) {
            long l = ((ShaderPack)object).modified(string);
            if (l >= 0L) {
                return l;
            }
            object = ((ShaderPack)object).base;
        }
        object = Shaders.devFile(string);
        return object == null ? 0L : ((File)object).lastModified();
    }

    private static File devFile(String string) {
        String string2;
        String string3 = string2 = Settings.dev ? Settings.shaderDir : null;
        if (string2 == null || string2.isEmpty()) {
            return null;
        }
        File file = new File(string2, string);
        return file.isFile() ? file : null;
    }

    static String explain(String string, List<String> list) {
        Matcher matcher = Pattern.compile("\\b(\\d+)([:(])(\\d+)").matcher(string);
        StringBuilder stringBuilder = new StringBuilder();
        while (matcher.find()) {
            int n = Integer.parseInt(matcher.group(1));
            String string2 = n < list.size() ? list.get(n) : matcher.group(1);
            matcher.appendReplacement(stringBuilder, Matcher.quoteReplacement(string2 + matcher.group(2) + matcher.group(3)));
        }
        matcher.appendTail(stringBuilder);
        return stringBuilder.toString();
    }

    private Shaders() {
    }

    static final class Source {
        final String text;
        final List<String> files;
        final boolean fromPack;

        Source(String string, List<String> list, boolean bl) {
            this.text = string;
            this.files = list;
            this.fromPack = bl;
        }
    }
}

