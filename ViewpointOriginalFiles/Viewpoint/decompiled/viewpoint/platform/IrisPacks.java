/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GLCapabilities
 */
package viewpoint.platform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import viewpoint.iris.IrisMacros;
import viewpoint.iris.IrisPack;

public final class IrisPacks {
    private static final Map<String, IrisPack> packs = new LinkedHashMap<String, IrisPack>();
    private static final Map<String, String> broken = new LinkedHashMap<String, String>();
    private static volatile Active active;
    private static volatile Active drawn;
    private static volatile Failure failure;
    private static volatile List<String> report;
    private static int generation;
    private static Map<String, String> standard;
    private static Set<String> features;

    static boolean discover(File file) {
        LinkedHashMap<String, IrisPack> linkedHashMap = new LinkedHashMap<String, IrisPack>(packs);
        packs.clear();
        broken.clear();
        Object[] objectArray = file == null ? null : file.listFiles();
        Object[] objectArray2 = objectArray == null ? new File[]{} : objectArray;
        Arrays.sort(objectArray2);
        for (Object object : objectArray2) {
            boolean bl;
            boolean bl2 = bl = ((File)object).isFile() && ((File)object).getName().toLowerCase(Locale.ROOT).endsWith(".zip");
            if (!bl && !new File((File)object, "shaders").isDirectory()) continue;
            IrisPack irisPack = linkedHashMap.values().stream().filter(arg_0 -> IrisPacks.lambda$discover$0((File)object, arg_0)).findFirst().orElse(null);
            if (irisPack != null) {
                packs.put(irisPack.id, irisPack);
                continue;
            }
            try {
                IrisPack irisPack2 = IrisPack.load((File)object, IrisPacks.discoveryMacros());
                packs.put(irisPack2.id, irisPack2);
            }
            catch (IOException | RuntimeException exception) {
                broken.put(((File)object).getName(), exception.getMessage());
                System.out.println("[Viewpoint] Minecraft shader pack " + ((File)object).getName() + " left out: " + exception.getMessage());
            }
        }
        if (!packs.isEmpty()) {
            System.out.println("[Viewpoint] Minecraft shader packs: " + String.join((CharSequence)", ", packs.keySet()));
        }
        return !linkedHashMap.keySet().equals(packs.keySet());
    }

    static List<IrisPack> all() {
        return new ArrayList<IrisPack>(packs.values());
    }

    static IrisPack find(String string) {
        return packs.get(string);
    }

    static Map<String, String> leftOut() {
        return new LinkedHashMap<String, String>(broken);
    }

    public static void use(IrisPack irisPack, Map<String, String> map) {
        Active active = IrisPacks.active;
        if (irisPack == null) {
            IrisPacks.active = null;
            return;
        }
        if (active != null && active.pack == irisPack && active.values.equals(map)) {
            return;
        }
        IrisPacks.active = new Active(irisPack, Map.copyOf(map), ++generation);
    }

    public static Active active() {
        return active;
    }

    public static Active drawn() {
        return drawn;
    }

    public static Failure failure() {
        return failure;
    }

    public static void linked(Active active, List<String> list) {
        drawn = active;
        failure = null;
        report = List.copyOf(list);
    }

    public static List<String> report() {
        return report;
    }

    public static void failed(Active active, String string) {
        drawn = null;
        failure = new Failure(active.pack().id, active.generation(), string);
        System.out.println("[Viewpoint] Minecraft shader pack " + active.pack().id + " cannot be drawn: " + string);
    }

    public static Map<String, String> standard() {
        if (standard == null) {
            GLCapabilities gLCapabilities = GL.getCapabilities();
            int n = gLCapabilities.OpenGL46 ? 460 : (gLCapabilities.OpenGL45 ? 450 : (gLCapabilities.OpenGL44 ? 440 : (gLCapabilities.OpenGL43 ? 430 : (gLCapabilities.OpenGL42 ? 420 : (gLCapabilities.OpenGL41 ? 410 : (gLCapabilities.OpenGL40 ? 400 : 330))))));
            features = IrisPacks.features(gLCapabilities);
            standard = IrisMacros.standard(n, GL11.glGetString((int)7936), GL11.glGetString((int)7937), features, true);
        }
        return standard;
    }

    public static Set<String> features() {
        IrisPacks.standard();
        return features;
    }

    private static Set<String> features(GLCapabilities gLCapabilities) {
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>(List.of("PER_BUFFER_BLENDING", "FADE_VARIABLE", "REVERSED_CULLING", "HIGHER_SHADOWCOLOR", "SEPARATE_HARDWARE_SAMPLERS"));
        if (gLCapabilities.OpenGL43) {
            linkedHashSet.addAll(List.of("COMPUTE_SHADERS", "CUSTOM_IMAGES", "SSBO"));
        }
        return linkedHashSet;
    }

    private static Map<String, String> discoveryMacros() {
        return IrisMacros.standard(460, "", "", Set.of("PER_BUFFER_BLENDING", "FADE_VARIABLE", "COMPUTE_SHADERS", "CUSTOM_IMAGES", "SSBO"), true);
    }

    private IrisPacks() {
    }

    private static /* synthetic */ boolean lambda$discover$0(File file, IrisPack irisPack) {
        return irisPack.file.equals(file);
    }

    static {
        report = List.of();
    }

    public record Active(IrisPack pack, Map<String, String> values, int generation) {
    }

    public record Failure(String packId, int generation, String why) {
    }
}

