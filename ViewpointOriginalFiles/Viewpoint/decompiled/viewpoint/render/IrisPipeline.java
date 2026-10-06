/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL
 */
package viewpoint.render;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lwjgl.opengl.GL;
import viewpoint.iris.CustomUniforms;
import viewpoint.iris.IrisPack;
import viewpoint.iris.ShadersProperties;
import viewpoint.platform.IrisPacks;
import viewpoint.platform.Pool;
import viewpoint.render.IrisBindings;
import viewpoint.render.IrisFormats;
import viewpoint.render.IrisImages;
import viewpoint.render.IrisPrograms;
import viewpoint.render.IrisShadowTargets;
import viewpoint.render.IrisTargets;
import viewpoint.render.IrisTextures;
import viewpoint.render.IrisUniforms;

final class IrisPipeline {
    private static final Pattern VEC4 = Pattern.compile("vec4\\s*\\(([^)]*)\\)");
    final IrisPack pack;
    final IrisPack.Configured configured;
    final IrisPrograms programs;
    final IrisTargets targets = new IrisTargets();
    final IrisShadowTargets shadows = new IrisShadowTargets();
    final IrisTextures textures = new IrisTextures();
    final IrisImages images = new IrisImages();
    final IrisUniforms uniforms = new IrisUniforms();
    final CustomUniforms custom;
    final IrisBindings bindings;
    final List<String> report = new ArrayList<String>();
    int shadowResolution = 1024;
    float shadowDistance = 128.0f;
    boolean shadowOn;
    float eyeHalflife = 10.0f;
    float wetHalflife = 600.0f;
    float dryHalflife = 200.0f;
    boolean setupDone;

    private IrisPipeline(IrisPack irisPack, IrisPack.Configured configured, IrisPrograms irisPrograms) {
        this.pack = irisPack;
        this.configured = configured;
        this.programs = irisPrograms;
        this.custom = CustomUniforms.parse(configured.properties(), this.report);
        this.bindings = new IrisBindings(this.targets, this.shadows, this.textures, this.images);
    }

    static IrisPipeline create(IrisPack irisPack, Map<String, String> map, Map<String, String> map2) {
        IrisPack.Configured configured;
        try {
            configured = irisPack.configure(map, map2);
        }
        catch (IOException iOException) {
            throw new IllegalStateException(irisPack.id + ": " + iOException.getMessage(), iOException);
        }
        List<String> list = IrisPipeline.features(irisPack, configured.properties());
        IrisPrograms irisPrograms = IrisPrograms.link(irisPack, configured, map2);
        IrisPipeline irisPipeline = new IrisPipeline(irisPack, configured, irisPrograms);
        irisPipeline.report.addAll(list);
        irisPipeline.configure();
        irisPipeline.textures.load(irisPack.source, configured.properties(), irisPipeline.integer("noiseTextureResolution", 256));
        irisPipeline.report.addAll(configured.notes());
        irisPipeline.report.addAll(irisPrograms.notes);
        irisPipeline.report.addAll(irisPipeline.textures.notes);
        return irisPipeline;
    }

    private static List<String> features(IrisPack irisPack, ShadersProperties shadersProperties) {
        if (!GL.getCapabilities().OpenGL40) {
            throw new IllegalStateException(irisPack.id + ": Minecraft shader packs need OpenGL 4.0, which this GPU does not give");
        }
        Set<String> set = IrisPacks.features();
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>(shadersProperties.features(true));
        linkedHashSet.removeAll(set);
        if (!linkedHashSet.isEmpty()) {
            throw new IllegalStateException(irisPack.id + " requires " + String.join((CharSequence)", ", linkedHashSet) + ", which the bridge does not give on this GPU");
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        LinkedHashSet<String> linkedHashSet2 = new LinkedHashSet<String>(shadersProperties.features(false));
        linkedHashSet2.removeAll(set);
        if (!linkedHashSet2.isEmpty()) {
            arrayList.add("runs without " + String.join((CharSequence)", ", linkedHashSet2));
        }
        if (!shadersProperties.unknown.isEmpty()) {
            arrayList.add("directives the bridge does not know: " + String.join((CharSequence)", ", shadersProperties.unknown));
        }
        return arrayList;
    }

    private void configure() {
        Object object;
        Object object2;
        Map<String, String> map = this.programs.constants;
        for (int n : this.programs.buffers) {
            Object object3;
            if (n < 0 || n >= 32) continue;
            object2 = this.targets.buffers[n];
            ((IrisTargets.Buffer)object2).used = true;
            object = IrisPipeline.constant(map, n, "Format");
            if (object != null) {
                object3 = IrisFormats.format((String)object);
                if (object3 == null) {
                    this.report.add("colortex" + n + ": unknown format " + (String)object + " (RGBA8)");
                } else {
                    ((IrisTargets.Buffer)object2).format = object3;
                }
            }
            ((IrisTargets.Buffer)object2).clear = (object3 = IrisPipeline.constant(map, n, "Clear")) == null || !((String)object3).equals("false");
            float[] fArray = IrisPipeline.vec4(IrisPipeline.constant(map, n, "ClearColor"));
            if (fArray != null) {
                ((IrisTargets.Buffer)object2).clearColor = fArray;
            }
            ((IrisTargets.Buffer)object2).mipmap = "true".equals(IrisPipeline.constant(map, n, "MipmapEnabled"));
            this.size((IrisTargets.Buffer)object2, n);
        }
        this.shadowResolution = this.integer("shadowMapResolution", 2048);
        this.shadowDistance = (float)this.number("shadowDistance", 160.0);
        this.eyeHalflife = (float)this.number("eyeBrightnessHalflife", 10.0);
        this.wetHalflife = (float)this.number("wetnessHalflife", 600.0);
        this.dryHalflife = (float)this.number("drynessHalflife", 200.0);
        this.shadowOn = this.programs.draws.containsKey((Object)IrisPrograms.Draw.SHADOW) && this.configured.properties().flag("shadow.enabled", true);
        for (int n : this.programs.shadowColors) {
            if (n < 0 || n >= 8) continue;
            this.shadows.used[n] = true;
            object2 = IrisFormats.format(map.get("shadowcolor" + n + "Format"));
            this.shadows.formats[n] = object2 == null ? this.shadows.formats[n] : object2;
            this.shadows.clear[n] = !"false".equals(map.get("shadowcolor" + n + "Clear"));
            object = IrisPipeline.vec4(map.get("shadowcolor" + n + "ClearColor"));
            this.shadows.clearColors[n] = (float[])(object == null ? (Object)this.shadows.clearColors[n] : object);
            this.shadows.mipmap[n] = "true".equals(map.get("shadowcolor" + n + "MipmapEnabled"));
        }
    }

    private void size(IrisTargets.Buffer buffer, int n) {
        float f;
        float f2;
        String string = this.configured.properties().sizeBuffer.get("colortex" + n);
        String[] stringArray = new String[]{"gcolor", "gdepth", "gnormal", "composite", "gaux1", "gaux2", "gaux3", "gaux4"};
        if (string == null && n < stringArray.length) {
            string = this.configured.properties().sizeBuffer.get(stringArray[n]);
        }
        if (string == null) {
            return;
        }
        String[] stringArray2 = string.strip().split("\\s+");
        if (stringArray2.length < 2) {
            return;
        }
        String string2 = this.configured.macros().getOrDefault(stringArray2[0], stringArray2[0]);
        String string3 = this.configured.macros().getOrDefault(stringArray2[1], stringArray2[1]);
        try {
            f2 = Float.parseFloat(string2);
            f = Float.parseFloat(string3);
        }
        catch (NumberFormatException numberFormatException) {
            throw new IllegalStateException(this.pack.id + ": size.buffer.colortex" + n + " is not a size: " + string, numberFormatException);
        }
        buffer.fixed = !string2.contains(".") && f2 > 1.0f;
        buffer.scaleX = f2;
        buffer.scaleY = f;
    }

    private static String constant(Map<String, String> map, int n, String string) {
        String string2 = map.get("colortex" + n + string);
        String[] stringArray = new String[]{"gcolor", "gdepth", "gnormal", "composite", "gaux1", "gaux2", "gaux3", "gaux4"};
        if (string2 == null && n < stringArray.length) {
            string2 = map.get(stringArray[n] + string);
        }
        if (string2 == null && n >= 4 && n < 8) {
            string2 = map.get("gaux" + (n - 3) + string);
        }
        return string2;
    }

    private static float[] vec4(String string) {
        if (string == null) {
            return null;
        }
        Matcher matcher = VEC4.matcher(string);
        if (!matcher.find()) {
            return null;
        }
        String[] stringArray = matcher.group(1).split(",");
        float[] fArray = new float[4];
        for (int i = 0; i < 4; ++i) {
            String string2 = stringArray[Math.min(i, stringArray.length - 1)].strip().replaceAll("[fF]$", "");
            fArray[i] = Float.parseFloat(string2);
        }
        return fArray;
    }

    int integer(String string, int n) {
        return (int)this.number(string, n);
    }

    double number(String string, double d) {
        String string2 = this.programs.constants.get(string);
        if (string2 == null) {
            return d;
        }
        try {
            return Double.parseDouble(string2.strip().replaceAll("[fF]$", ""));
        }
        catch (NumberFormatException numberFormatException) {
            return d;
        }
    }

    void checkPool() {
        long l = this.targets.bytes + this.shadows.bytes + this.images.bytes + this.textures.bytes;
        Pool.IRIS.use(l);
        if (l > Pool.IRIS.cap()) {
            throw new IllegalStateException(this.pack.id + " needs " + (l >> 20) + " MiB, past the Minecraft shaders' pool's " + (Pool.IRIS.cap() >> 20) + " MiB: screen targets " + (this.targets.bytes >> 20) + " MiB, shadow maps " + (this.shadows.bytes >> 20) + " MiB, images and buffers " + (this.images.bytes >> 20) + " MiB (lower the pack's coloured light or voxel options)");
        }
    }

    void release() {
        Pool.IRIS.use(0L);
        this.programs.delete();
        this.targets.release();
        this.shadows.release();
        this.textures.release();
        this.images.release();
        this.bindings.release();
    }
}

