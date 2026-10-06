/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix3f
 *  org.joml.Matrix4f
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.platform;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.PackPass;
import viewpoint.platform.ShaderPacks;
import viewpoint.platform.ShaderWatch;
import viewpoint.platform.Shaders;

public final class GlProgram {
    private static final List<GlProgram> all = new ArrayList<GlProgram>();
    private static final Map<String, GlProgram> passes = new LinkedHashMap<String, GlProgram>();
    private static boolean passesLinked = true;
    public static int undeclared;
    private static int adopted;
    private static final FloatBuffer matrix4;
    private static final FloatBuffer matrix3;
    private static ShaderPacks.Active pack;
    private static String lastError;
    final String name;
    private final String vertexPath;
    private final String fragmentPath;
    private int id;
    private String source = "";
    private boolean fromPack;
    private final HashMap<String, Integer> locations = new HashMap();
    private final LinkedHashMap<String, Integer> samplers = new LinkedHashMap();
    private final HashMap<String, Long> files = new HashMap();

    private GlProgram(String string, String string2, String string3) {
        this.name = string;
        this.vertexPath = string2;
        this.fragmentPath = string3;
    }

    public static GlProgram fromSources(String string2, Sources sources, Map<String, Integer> map, Map<String, Integer> map2, List<String> list) {
        int[] nArray;
        int n = GL20.glCreateProgram();
        for (int n3 : nArray = new int[]{GlProgram.foreign(string2, 35633, sources.vertex(), list), GlProgram.foreign(string2, 36488, sources.control(), list), GlProgram.foreign(string2, 36487, sources.evaluation(), list), GlProgram.foreign(string2, 36313, sources.geometry(), list), GlProgram.foreign(string2, 35632, sources.fragment(), list)}) {
            if (n3 > 0) {
                GL20.glAttachShader((int)n, (int)n3);
                continue;
            }
            if (n3 >= 0) continue;
            GlProgram.deleteAll(n, nArray);
            throw new IllegalStateException(lastError);
        }
        map.forEach((string, n2) -> GL20.glBindAttribLocation((int)n, (int)n2, (CharSequence)string));
        map2.forEach((string, n2) -> GL30.glBindFragDataLocation((int)n, (int)n2, (CharSequence)string));
        String string3 = (sources.vertex() == null ? "" : sources.vertex()) + "\n" + (sources.evaluation() == null ? "" : sources.evaluation()) + "\n" + sources.fragment();
        return GlProgram.linkForeign(string2, n, nArray, string3);
    }

    public static GlProgram compute(String string, String string2, List<String> list) {
        int n = GL20.glCreateProgram();
        int[] nArray = new int[]{GlProgram.foreign(string, 37305, string2, list)};
        if (nArray[0] < 0) {
            GlProgram.deleteAll(n, nArray);
            throw new IllegalStateException(lastError);
        }
        GL20.glAttachShader((int)n, (int)nArray[0]);
        return GlProgram.linkForeign(string, n, nArray, string2);
    }

    private static GlProgram linkForeign(String string, int n, int[] nArray, String string2) {
        GL20.glLinkProgram((int)n);
        if (GL20.glGetProgrami((int)n, (int)35714) == 0) {
            String string3 = GL20.glGetProgramInfoLog((int)n);
            GlProgram.deleteAll(n, nArray);
            lastError = string + " program link failed: " + string3;
            throw new IllegalStateException(lastError);
        }
        for (int n2 : nArray) {
            if (n2 <= 0) continue;
            GL20.glDetachShader((int)n, (int)n2);
            GL20.glDeleteShader((int)n2);
        }
        Object object = new GlProgram(string, null, null);
        ((GlProgram)object).adopt(new Linked(n, string2, Map.of(), true));
        return object;
    }

    private static int foreign(String string, int n, String string2, List<String> list) {
        if (string2 == null) {
            return 0;
        }
        int n2 = GL20.glCreateShader((int)n);
        GL20.glShaderSource((int)n2, (CharSequence)string2);
        GL20.glCompileShader((int)n2);
        if (GL20.glGetShaderi((int)n2, (int)35713) == 0) {
            lastError = string + " shader compile failed: " + Shaders.explain(GL20.glGetShaderInfoLog((int)n2), list);
            GL20.glDeleteShader((int)n2);
            return -1;
        }
        return n2;
    }

    private static void deleteAll(int n, int[] nArray) {
        for (int n2 : nArray) {
            if (n2 <= 0) continue;
            GL20.glDeleteShader((int)n2);
        }
        GL20.glDeleteProgram((int)n);
    }

    public void delete() {
        if (this.id != 0) {
            GL20.glDeleteProgram((int)this.id);
            this.id = 0;
        }
    }

    public int id() {
        return this.id;
    }

    public static GlProgram create(String string, String string2, String string3) {
        GlProgram glProgram = new GlProgram(string, string2, string3);
        Linked linked = glProgram.link(GlProgram.pack());
        if (linked != null) {
            glProgram.adopt(linked);
        }
        all.add(glProgram);
        return glProgram;
    }

    public static ShaderPacks.Active pack() {
        if (pack == null) {
            pack = ShaderPacks.active();
        }
        return pack;
    }

    public static boolean linkPasses() {
        Map<String, GlProgram> map = GlProgram.makePasses(GlProgram.pack());
        boolean bl = passesLinked = map != null;
        if (map != null) {
            passes.putAll(map);
        }
        return passesLinked;
    }

    public static GlProgram pass(String string) {
        return passes.get(string);
    }

    public static String lastError() {
        return lastError;
    }

    public static void forEach(Consumer<GlProgram> consumer) {
        all.forEach(consumer);
        passes.values().forEach(consumer);
    }

    public static int links() {
        return adopted;
    }

    /*
     * WARNING - void declaration
     */
    public static String relinkAll(ShaderPacks.Active active) {
        void var3_6;
        ArrayList<Linked> arrayList = new ArrayList<Linked>();
        for (GlProgram object2 : all) {
            Linked linked = object2.link(active);
            if (linked == null) {
                for (Linked linked2 : arrayList) {
                    GL20.glDeleteProgram((int)linked2.id);
                }
                return object2.name + ": " + lastError;
            }
            arrayList.add(linked);
        }
        Map<String, GlProgram> map = GlProgram.makePasses(active);
        if (map == null) {
            for (Linked linked : arrayList) {
                GL20.glDeleteProgram((int)linked.id);
            }
            return lastError;
        }
        boolean bl = false;
        while (var3_6 < all.size()) {
            GlProgram glProgram = all.get((int)var3_6);
            if (glProgram.id != 0) {
                GL20.glDeleteProgram((int)glProgram.id);
            }
            glProgram.adopt((Linked)arrayList.get((int)var3_6));
            ++var3_6;
        }
        for (GlProgram glProgram : passes.values()) {
            GL20.glDeleteProgram((int)glProgram.id);
        }
        passes.clear();
        passes.putAll(map);
        passesLinked = true;
        pack = active;
        GL20.glUseProgram((int)0);
        return null;
    }

    public static boolean allLinked() {
        boolean bl = passesLinked;
        for (GlProgram glProgram : all) {
            bl &= glProgram.id != 0;
        }
        return bl;
    }

    public void use() {
        GL20.glUseProgram((int)this.id);
    }

    public int location(String string) {
        if (this.id == 0) {
            return -1;
        }
        Integer n = this.locations.get(string);
        if (n == null) {
            n = GL20.glGetUniformLocation((int)this.id, (CharSequence)string);
            this.locations.put(string, n);
            if (n < 0 && !this.fromPack && !this.declares(string)) {
                ++undeclared;
                System.out.println("[Viewpoint] shader program " + this.name + " has no uniform " + string + " (a typo, or a leftover)");
            }
        }
        return n;
    }

    public boolean has(String string) {
        if (this.id == 0) {
            return false;
        }
        Integer n = this.locations.get(string);
        if (n == null) {
            n = GL20.glGetUniformLocation((int)this.id, (CharSequence)string);
            this.locations.put(string, n);
        }
        return n >= 0;
    }

    private boolean declares(String string) {
        return Pattern.compile("\\b" + Pattern.quote(string) + "\\b").matcher(this.source).find();
    }

    public void set(String string, float f) {
        GL20.glUniform1f((int)this.location(string), (float)f);
    }

    public void set(String string, float f, float f2) {
        GL20.glUniform2f((int)this.location(string), (float)f, (float)f2);
    }

    public void set(String string, float f, float f2, float f3) {
        GL20.glUniform3f((int)this.location(string), (float)f, (float)f2, (float)f3);
    }

    public void set(String string, float f, float f2, float f3, float f4) {
        GL20.glUniform4f((int)this.location(string), (float)f, (float)f2, (float)f3, (float)f4);
    }

    public void setInt(String string, int n) {
        GL20.glUniform1i((int)this.location(string), (int)n);
    }

    public void set(String string, Matrix4f matrix4f) {
        GL20.glUniformMatrix4fv((int)this.location(string), (boolean)false, (FloatBuffer)matrix4f.get(matrix4));
    }

    public void set(String string, Matrix3f matrix3f) {
        matrix3.clear();
        matrix3f.get(matrix3);
        GL20.glUniformMatrix3fv((int)this.location(string), (boolean)false, (FloatBuffer)matrix3);
    }

    public void setMatrices(String string, FloatBuffer floatBuffer) {
        GL20.glUniformMatrix4fv((int)this.location(string), (boolean)false, (FloatBuffer)floatBuffer);
    }

    public void setVec4s(String string, FloatBuffer floatBuffer) {
        GL20.glUniform4fv((int)this.location(string), (FloatBuffer)floatBuffer);
    }

    public void setVec3s(String string, FloatBuffer floatBuffer) {
        GL20.glUniform3fv((int)this.location(string), (FloatBuffer)floatBuffer);
    }

    public void setFloats(String string, FloatBuffer floatBuffer) {
        GL20.glUniform1fv((int)this.location(string), (FloatBuffer)floatBuffer);
    }

    public void sampler(String string, int n) {
        this.samplers.put(string, n);
        if (this.id == 0) {
            return;
        }
        GL20.glUseProgram((int)this.id);
        GL20.glUniform1i((int)this.location(string), (int)n);
    }

    public static void reloadChanged() {
        Map<String, Long> map = ShaderWatch.stamps();
        HashSet<String> hashSet = new HashSet<String>();
        ArrayList<GlProgram> arrayList = new ArrayList<GlProgram>(all);
        arrayList.addAll(passes.values());
        for (GlProgram glProgram : arrayList) {
            hashSet.addAll(glProgram.files.keySet());
            if (!glProgram.changed(map)) continue;
            Linked linked = glProgram.link(GlProgram.pack());
            if (linked == null) {
                System.out.println("[Viewpoint] shader reload: " + glProgram.name + " did not compile, the running version stays");
                continue;
            }
            if (glProgram.id != 0) {
                GL20.glDeleteProgram((int)glProgram.id);
            }
            glProgram.adopt(linked);
            System.out.println("[Viewpoint] shader reload: " + glProgram.name);
        }
        ShaderWatch.watch(hashSet);
        GL20.glUseProgram((int)0);
    }

    private boolean changed(Map<String, Long> map) {
        for (Map.Entry<String, Long> entry : this.files.entrySet()) {
            Long l = map.get(entry.getKey());
            if (l == null || l.longValue() == entry.getValue().longValue()) continue;
            return true;
        }
        return false;
    }

    private void adopt(Linked linked) {
        ++adopted;
        this.id = linked.id;
        this.source = linked.source;
        this.fromPack = linked.fromPack;
        this.files.clear();
        this.files.putAll(linked.files);
        this.locations.clear();
        for (Map.Entry<String, Integer> entry : this.samplers.entrySet()) {
            GL20.glUseProgram((int)this.id);
            GL20.glUniform1i((int)this.location(entry.getKey()), (int)entry.getValue());
        }
    }

    private static Map<String, GlProgram> makePasses(ShaderPacks.Active active) {
        LinkedHashMap<String, GlProgram> linkedHashMap = new LinkedHashMap<String, GlProgram>();
        for (PackPass packPass : active.passes) {
            GlProgram glProgram = new GlProgram("pass " + packPass.id, packPass.vertex, packPass.fragment);
            Linked linked = glProgram.link(active);
            if (linked == null) {
                for (GlProgram glProgram2 : linkedHashMap.values()) {
                    GL20.glDeleteProgram((int)glProgram2.id);
                }
                return null;
            }
            glProgram.adopt(linked);
            linkedHashMap.put(packPass.id, glProgram);
        }
        return linkedHashMap;
    }

    private Linked link(ShaderPacks.Active active) {
        Shaders.Source source;
        Shaders.Source source2;
        try {
            source2 = Shaders.load(this.vertexPath, active);
            source = Shaders.load(this.fragmentPath, active);
        }
        catch (IllegalStateException illegalStateException) {
            return GlProgram.failed(this.name + " program: " + illegalStateException.getMessage());
        }
        HashMap<String, Long> hashMap = new HashMap<String, Long>();
        for (String string : source2.files) {
            hashMap.put(string, Shaders.modified(string, active));
        }
        for (String string : source.files) {
            hashMap.put(string, Shaders.modified(string, active));
        }
        int n = this.compile(35633, source2);
        int n2 = this.compile(35632, source);
        if (n == 0 || n2 == 0) {
            GL20.glDeleteShader((int)n);
            GL20.glDeleteShader((int)n2);
            return null;
        }
        int n3 = GL20.glCreateProgram();
        GL20.glAttachShader((int)n3, (int)n);
        GL20.glAttachShader((int)n3, (int)n2);
        GL20.glLinkProgram((int)n3);
        GL20.glDeleteShader((int)n);
        GL20.glDeleteShader((int)n2);
        if (GL20.glGetProgrami((int)n3, (int)35714) == 0) {
            String string = GL20.glGetProgramInfoLog((int)n3);
            GL20.glDeleteProgram((int)n3);
            return GlProgram.failed(this.name + " program link failed: " + string);
        }
        return new Linked(n3, source2.text + "\n" + source.text, hashMap, source2.fromPack || source.fromPack);
    }

    private int compile(int n, Shaders.Source source) {
        int n2 = GL20.glCreateShader((int)n);
        GL20.glShaderSource((int)n2, (CharSequence)source.text);
        GL20.glCompileShader((int)n2);
        if (GL20.glGetShaderi((int)n2, (int)35713) == 0) {
            GlProgram.failed(this.name + " shader compile failed (" + source.files.get(0) + "): " + Shaders.explain(GL20.glGetShaderInfoLog((int)n2), source.files));
            GL20.glDeleteShader((int)n2);
            return 0;
        }
        return n2;
    }

    private static Linked failed(String string) {
        lastError = string;
        System.out.println("[Viewpoint] " + string);
        return null;
    }

    static {
        matrix4 = BufferUtils.createFloatBuffer((int)16);
        matrix3 = BufferUtils.createFloatBuffer((int)9);
        lastError = "";
    }

    public record Sources(String vertex, String control, String evaluation, String geometry, String fragment) {
    }

    private record Linked(int id, String source, Map<String, Long> files, boolean fromPack) {
    }
}

