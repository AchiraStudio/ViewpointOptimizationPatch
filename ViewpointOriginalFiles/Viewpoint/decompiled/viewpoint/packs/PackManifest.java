/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.packs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

final class PackManifest {
    private static final String MODEL = "model.";
    private static final String BIND = "bind.";
    private static final int BIND_FIELDS = 6;
    final String id;
    final String name;
    final LinkedHashMap<String, String> models;
    final LinkedHashMap<String, Bind> binds;
    final List<String> errors;

    private PackManifest(String string, String string2, LinkedHashMap<String, String> linkedHashMap, LinkedHashMap<String, Bind> linkedHashMap2, List<String> list) {
        this.id = string;
        this.name = string2;
        this.models = linkedHashMap;
        this.binds = linkedHashMap2;
        this.errors = list;
    }

    static PackManifest parse(List<String> list) {
        String string = null;
        String string2 = null;
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        LinkedHashMap<String, Bind> linkedHashMap2 = new LinkedHashMap<String, Bind>();
        ArrayList<String> arrayList = new ArrayList<String>();
        for (int i = 0; i < list.size(); ++i) {
            String string3 = list.get(i).strip();
            int n = string3.indexOf(61);
            if (string3.isEmpty() || string3.startsWith("#")) continue;
            if (n <= 0) {
                arrayList.add("line " + (i + 1) + ": no key=value: " + string3);
                continue;
            }
            String string4 = string3.substring(0, n).strip();
            String string5 = string3.substring(n + 1).strip();
            if (string4.equals("id")) {
                string = string5;
                continue;
            }
            if (string4.equals("name")) {
                string2 = string5;
                continue;
            }
            if (string4.startsWith(MODEL) && string4.length() > MODEL.length() && !string5.isEmpty()) {
                linkedHashMap.put(string4.substring(MODEL.length()), string5);
                continue;
            }
            if (!string4.startsWith(BIND) || string4.length() <= BIND.length()) continue;
            Bind bind = PackManifest.bind(string5);
            if (bind == null) {
                arrayList.add("line " + (i + 1) + ": a bind is model,degrees,x,y,z,scale: " + string3);
                continue;
            }
            linkedHashMap2.put(string4.substring(BIND.length()), bind);
        }
        linkedHashMap2.entrySet().removeIf(entry -> {
            boolean bl;
            boolean bl2 = bl = !linkedHashMap.containsKey(((Bind)entry.getValue()).model());
            if (bl) {
                arrayList.add(BIND + (String)entry.getKey() + ": no model " + ((Bind)entry.getValue()).model());
            }
            return bl;
        });
        return new PackManifest(string, string2, linkedHashMap, linkedHashMap2, arrayList);
    }

    private static Bind bind(String string) {
        String[] stringArray = string.split(",");
        if (stringArray.length != 6 || stringArray[0].isBlank()) {
            return null;
        }
        try {
            float f = Float.parseFloat(stringArray[5].strip());
            return f > 0.0f ? new Bind(stringArray[0].strip(), Float.parseFloat(stringArray[1].strip()), Float.parseFloat(stringArray[2].strip()), Float.parseFloat(stringArray[3].strip()), Float.parseFloat(stringArray[4].strip()), f) : null;
        }
        catch (NumberFormatException numberFormatException) {
            return null;
        }
    }

    record Bind(String model, float degrees, float x, float y, float z, float scale) {
    }
}

