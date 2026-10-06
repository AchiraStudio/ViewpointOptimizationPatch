/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Directives {
    private static final Pattern DRAWBUFFERS = Pattern.compile("DRAWBUFFERS\\s*:\\s*([0-9A-Za-z]+)");
    private static final Pattern RENDERTARGETS = Pattern.compile("RENDERTARGETS\\s*:\\s*([0-9 ,]+)");
    private static final Pattern CONST = Pattern.compile("const\\s+(?:int|float|bool|vec2|vec3|vec4|ivec2|ivec3)\\s+(\\w+)\\s*=\\s*([^;]+?)\\s*;");
    private static final Pattern LOCAL_SIZE = Pattern.compile("local_size_([xyz])\\s*=\\s*(\\d+)");
    public final List<Integer> drawBuffers;
    public final Map<String, String> constants;
    public final int[] localSize = new int[]{1, 1, 1};

    private Directives(List<Integer> list, Map<String, String> map) {
        this.drawBuffers = list;
        this.constants = map;
    }

    public static Directives scan(String string) {
        String[] stringArray;
        ArrayList<Integer> arrayList = null;
        Matcher matcher = RENDERTARGETS.matcher(string);
        while (matcher.find()) {
            arrayList = new ArrayList<Integer>();
            for (String string2 : matcher.group(1).split(",")) {
                if (string2.isBlank()) continue;
                arrayList.add(Integer.parseInt(string2.strip()));
            }
        }
        if (arrayList == null) {
            stringArray = DRAWBUFFERS.matcher(string);
            while (stringArray.find()) {
                arrayList = new ArrayList();
                for (char c : stringArray.group(1).toCharArray()) {
                    arrayList.add(Character.isDigit(c) ? c - 48 : Character.toUpperCase(c) - 65 + 10);
                }
            }
        }
        stringArray = new LinkedHashMap();
        Matcher matcher2 = CONST.matcher(string);
        while (matcher2.find()) {
            if (!Directives.isDirective(matcher2.group(1))) continue;
            stringArray.put(matcher2.group(1), matcher2.group(2).strip());
        }
        Directives directives = new Directives(arrayList, (Map<String, String>)stringArray);
        Matcher matcher3 = LOCAL_SIZE.matcher(string);
        while (matcher3.find()) {
            directives.localSize["xyz".indexOf((String)matcher3.group((int)1))] = Integer.parseInt(matcher3.group(2));
        }
        return directives;
    }

    static boolean isDirective(String string) {
        boolean bl;
        block24: {
            block23: {
                block22: {
                    if (string.matches("(colortex|gaux|shadowcolor)\\d+(Format|Clear|ClearColor|MipmapEnabled|Nearest)") || string.matches("(gcolor|gdepth|gnormal|composite|gaux\\d)(Format|Clear|ClearColor|MipmapEnabled)") || string.matches("shadow(tex\\d|color\\d)?(Nearest|Mipmap|MipmapEnabled)") || string.matches("shadowHardwareFiltering\\d?")) break block22;
                    switch (string) {
                        case "shadowMapResolution": 
                        case "shadowDistance": 
                        case "shadowDistanceRenderMul": 
                        case "shadowIntervalSize": 
                        case "generateShadowMipmap": 
                        case "generateShadowColorMipmap": 
                        case "sunPathRotation": 
                        case "ambientOcclusionLevel": 
                        case "noiseTextureResolution": 
                        case "wetnessHalflife": 
                        case "drynessHalflife": 
                        case "eyeBrightnessHalflife": 
                        case "centerDepthHalflife": 
                        case "shadowMapFov": 
                        case "workGroups": 
                        case "workGroupsRender": 
                        case "voxelDistance": {
                            break;
                        }
                        default: {
                            break block23;
                        }
                    }
                }
                bl = true;
                break block24;
            }
            bl = false;
        }
        return bl;
    }
}

