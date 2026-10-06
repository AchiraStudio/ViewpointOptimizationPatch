/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import viewpoint.iris.Macros;

public final class ShadersProperties {
    public final Map<String, List<String>> screens = new LinkedHashMap<String, List<String>>();
    public final Map<String, Integer> screenColumns = new LinkedHashMap<String, Integer>();
    public final Set<String> sliders = new LinkedHashSet<String>();
    public final Map<String, List<String>> profiles = new LinkedHashMap<String, List<String>>();
    public final Map<String, String> blend = new LinkedHashMap<String, String>();
    public final Map<String, String> alphaTest = new LinkedHashMap<String, String>();
    public final Map<String, String> programEnabled = new LinkedHashMap<String, String>();
    public final Map<String, String> sizeBuffer = new LinkedHashMap<String, String>();
    public final Map<String, Boolean> flip = new LinkedHashMap<String, Boolean>();
    public final Map<String, String> textures = new LinkedHashMap<String, String>();
    public final Map<String, String> customTextures = new LinkedHashMap<String, String>();
    public final Map<String, String> images = new LinkedHashMap<String, String>();
    public final Map<Integer, String> bufferObjects = new LinkedHashMap<Integer, String>();
    public final List<CustomUniform> uniforms = new ArrayList<CustomUniform>();
    public final Map<String, String> flags = new LinkedHashMap<String, String>();
    public final Set<String> unknown = new LinkedHashSet<String>();
    private static final Set<String> KNOWN_FLAGS = Set.of("shadow.enabled", "dhShadow.enabled", "shadow.culling", "shadowTerrain", "shadowTranslucent", "shadowEntities", "shadowPlayer", "shadowBlockEntities", "shadowLightBlockEntities", "oldLighting", "oldHandLight", "dynamicHandLight", "separateAo", "clouds", "cloudShadow", "sun", "moon", "stars", "sky", "weather", "weather.particles", "particles.ordering", "particles.before.deferred", "voxelizeLightBlocks", "beacon.beam.depth", "rain.depth", "underwaterOverlay", "vignette", "frustum.culling", "occlusion.culling", "iris.features.required", "iris.features.optional", "allowConcurrentCompute", "supportsColorCorrection", "prepareBeforeShadow", "shadow.ignoreFrustum", "backFace.solid", "backFace.cutout", "backFace.cutoutMipped", "backFace.translucent", "texture.noise", "version.1.12.2", "dimension", "screen.columns");

    private ShadersProperties() {
    }

    public static ShadersProperties parse(String string) {
        ShadersProperties shadersProperties = new ShadersProperties();
        for (String string2 : string.split("\n")) {
            String string3 = Macros.stripComments(string2).strip();
            int n = string3.indexOf(61);
            if (string3.isEmpty() || string3.startsWith("#") || n <= 0) continue;
            shadersProperties.entry(string3.substring(0, n).strip(), string3.substring(n + 1).strip());
        }
        return shadersProperties;
    }

    private void entry(String string, String string2) {
        if (string.equals("screen")) {
            this.screens.put("", ShadersProperties.words(string2));
        } else if (string.equals("screen.columns")) {
            this.screenColumns.put("", ShadersProperties.number(string2, 2));
        } else if (string.startsWith("screen.") && string.endsWith(".columns")) {
            this.screenColumns.put(string.substring(7, string.length() - 8), ShadersProperties.number(string2, 2));
        } else if (string.startsWith("screen.")) {
            this.screens.put(string.substring(7), ShadersProperties.words(string2));
        } else if (string.equals("sliders")) {
            this.sliders.addAll(ShadersProperties.words(string2));
        } else if (string.startsWith("profile.")) {
            this.profiles.put(string.substring(8), ShadersProperties.words(string2));
        } else if (string.startsWith("blend.")) {
            this.blend.put(string.substring(6), string2);
        } else if (string.startsWith("alphaTest.")) {
            this.alphaTest.put(string.substring(10), string2);
        } else if (string.startsWith("program.") && string.endsWith(".enabled")) {
            this.programEnabled.put(string.substring(8, string.length() - 8), string2);
        } else if (string.startsWith("size.buffer.")) {
            this.sizeBuffer.put(string.substring(12), string2);
        } else if (string.startsWith("flip.")) {
            this.flip.put(string.substring(5), string2.equalsIgnoreCase("true"));
        } else if (string.startsWith("texture.") && !string.equals("texture.noise")) {
            this.textures.put(string.substring(8), string2);
        } else if (string.startsWith("customTexture.")) {
            this.customTextures.put(string.substring(14), string2);
        } else if (string.startsWith("image.")) {
            this.images.put(string.substring(6), string2);
        } else if (string.startsWith("bufferObject.")) {
            this.bufferObjects.put(ShadersProperties.number(string.substring(13), -1), string2);
        } else if (string.startsWith("uniform.") || string.startsWith("variable.")) {
            this.uniform(string, string2);
        } else {
            this.flags.put(string, string2);
            if (!(KNOWN_FLAGS.contains(string) || string.startsWith("dimension.") || string.startsWith("scale.") || string.startsWith("indirect."))) {
                this.unknown.add(string);
            }
        }
    }

    private void uniform(String string, String string2) {
        String[] stringArray = string.split("\\.", 3);
        if (stringArray.length < 3) {
            this.unknown.add(string);
            return;
        }
        String string3 = stringArray[2];
        this.uniforms.removeIf(customUniform -> customUniform.name.equals(string3));
        this.uniforms.add(new CustomUniform(stringArray[1], string3, string2, stringArray[0].equals("variable")));
    }

    public String flag(String string, String string2) {
        return this.flags.getOrDefault(string, string2);
    }

    public boolean flag(String string, boolean bl) {
        String string2 = this.flags.get(string);
        return string2 == null ? bl : string2.strip().equalsIgnoreCase("true");
    }

    public Set<String> features(boolean bl) {
        return new LinkedHashSet<String>(ShadersProperties.words(this.flag(bl ? "iris.features.required" : "iris.features.optional", "")));
    }

    public static List<String> words(String string) {
        ArrayList<String> arrayList = new ArrayList<String>(Arrays.asList(string.strip().split("\\s+")));
        arrayList.removeIf(String::isEmpty);
        return arrayList;
    }

    private static int number(String string, int n) {
        try {
            return Integer.parseInt(string.strip());
        }
        catch (NumberFormatException numberFormatException) {
            return n;
        }
    }

    public record CustomUniform(String type, String name, String expression, boolean variable) {
    }
}

