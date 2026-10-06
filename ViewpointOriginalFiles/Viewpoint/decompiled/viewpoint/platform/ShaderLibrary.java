/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.LinkedHashMap;
import java.util.Map;
import viewpoint.platform.GlProgram;
import viewpoint.platform.Shaders;

public final class ShaderLibrary {
    public static String read(String string) {
        try {
            return Shaders.read(string);
        }
        catch (IllegalStateException illegalStateException) {
            return null;
        }
    }

    public static Map<String, String> pipelineMacros() {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (String string : GlProgram.pack().defines.split("\n")) {
            String[] stringArray = string.strip().split("\\s+", 3);
            if (stringArray.length < 2 || !stringArray[0].equals("#define")) continue;
            linkedHashMap.put(stringArray[1], stringArray.length > 2 ? stringArray[2] : "");
        }
        return linkedHashMap;
    }

    private ShaderLibrary() {
    }
}

