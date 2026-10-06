/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import viewpoint.iris.Macros;
import viewpoint.iris.ShadersProperties;

public final class IdTable {
    public static final IdTable EMPTY = new IdTable();
    private final Map<String, Integer> ids = new HashMap<String, Integer>();

    private IdTable() {
    }

    public static IdTable parse(String string, String string2) {
        IdTable idTable = new IdTable();
        if (string == null) {
            return idTable;
        }
        String string3 = string2 + ".";
        for (String string4 : string.split("\n")) {
            int n;
            String string5 = Macros.stripComments(string4).strip();
            int n2 = string5.indexOf(61);
            if (!string5.startsWith(string3) || n2 < 0) continue;
            try {
                n = Integer.parseInt(string5.substring(string3.length(), n2).strip());
            }
            catch (NumberFormatException numberFormatException) {
                continue;
            }
            for (String string6 : ShadersProperties.words(string5.substring(n2 + 1))) {
                String string7 = IdTable.plain(string6);
                idTable.ids.putIfAbsent(string7, n);
                int n3 = string7.indexOf(58);
                if (n3 <= 0) continue;
                idTable.ids.putIfAbsent(string7.substring(0, n3), n);
            }
        }
        return idTable;
    }

    public int id(String ... stringArray) {
        for (String string : stringArray) {
            String string2 = IdTable.plain(string);
            Integer n = this.ids.get(string2);
            if (n == null) continue;
            return n;
        }
        return -1;
    }

    public int size() {
        return this.ids.size();
    }

    private static String plain(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        return string2.startsWith("minecraft:") ? string2.substring(10) : string2;
    }
}

