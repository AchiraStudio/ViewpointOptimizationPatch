/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Macros {
    private final Map<String, Macro> table = new HashMap<String, Macro>();

    Macros() {
    }

    void define(String string, List<String> list, String string2) {
        this.table.put(string, new Macro(string, list, string2.strip()));
    }

    void define(String string, String string2) {
        this.define(string, null, string2);
    }

    void undefine(String string) {
        this.table.remove(string);
    }

    boolean defined(String string) {
        return this.table.containsKey(string);
    }

    Macro get(String string) {
        return this.table.get(string);
    }

    Map<String, String> objectLike() {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        for (Macro macro : this.table.values()) {
            if (macro.params != null) continue;
            hashMap.put(macro.name, macro.body);
        }
        return hashMap;
    }

    void defineFrom(String string) {
        int n;
        int n2 = string.length();
        for (n = 0; n < n2 && Character.isWhitespace(string.charAt(n)); ++n) {
        }
        int n3 = n;
        while (n < n2 && Macros.isIdentifierPart(string.charAt(n))) {
            ++n;
        }
        String string2 = string.substring(n3, n);
        if (string2.isEmpty()) {
            return;
        }
        if (n < n2 && string.charAt(n) == '(') {
            int n4 = string.indexOf(41, n);
            if (n4 < 0) {
                return;
            }
            ArrayList<String> arrayList = new ArrayList<String>();
            for (String string3 : string.substring(n + 1, n4).split(",")) {
                if (string3.isBlank()) continue;
                arrayList.add(string3.strip());
            }
            this.define(string2, arrayList, Macros.stripComments(string.substring(n4 + 1)));
            return;
        }
        this.define(string2, null, Macros.stripComments(string.substring(n)));
    }

    String expand(String string) {
        return this.expand(string, new HashSet<String>());
    }

    private String expand(String string, Set<String> set) {
        StringBuilder stringBuilder = new StringBuilder(string.length() + 16);
        int n = string.length();
        int n2 = 0;
        while (n2 < n) {
            int n3;
            char c = string.charAt(n2);
            if (c == '/' && n2 + 1 < n && (string.charAt(n2 + 1) == '/' || string.charAt(n2 + 1) == '*')) {
                n3 = Macros.commentEnd(string, n2);
                stringBuilder.append(string, n2, n3);
                n2 = n3;
                continue;
            }
            if (Macros.isIdentifierStart(c)) {
                n3 = n2;
                while (n2 < n && Macros.isIdentifierPart(string.charAt(n2))) {
                    ++n2;
                }
                n2 = this.identifier(string, n3, n2, set, stringBuilder);
                continue;
            }
            if (Character.isDigit(c) || c == '.' && n2 + 1 < n && Character.isDigit(string.charAt(n2 + 1))) {
                n3 = n2++;
                while (!(n2 >= n || !Macros.isIdentifierPart(string.charAt(n2)) && string.charAt(n2) != '.' && (string.charAt(n2) != '+' && string.charAt(n2) != '-' || string.charAt(n2 - 1) != 'e' && string.charAt(n2 - 1) != 'E'))) {
                    ++n2;
                }
                stringBuilder.append(string, n3, n2);
                continue;
            }
            stringBuilder.append(c);
            ++n2;
        }
        return stringBuilder.toString();
    }

    private int identifier(String string, int n, int n2, Set<String> set, StringBuilder stringBuilder) {
        String string2 = string.substring(n, n2);
        Macro macro = this.table.get(string2);
        if (macro == null || set.contains(string2)) {
            stringBuilder.append(string2);
            return n2;
        }
        if (macro.params == null) {
            set.add(string2);
            stringBuilder.append(this.expand(macro.body, set));
            set.remove(string2);
            return n2;
        }
        int n3 = Macros.skipSpace(string, n2);
        if (n3 >= string.length() || string.charAt(n3) != '(') {
            stringBuilder.append(string2);
            return n2;
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        int n4 = Macros.arguments(string, n3, arrayList);
        if (n4 < 0) {
            stringBuilder.append(string2);
            return n2;
        }
        String string3 = this.substitute(macro, arrayList, set);
        set.add(string2);
        stringBuilder.append(this.expand(string3, set));
        set.remove(string2);
        int n5 = Macros.count(string, n3, n4, '\n');
        for (int i = 0; i < n5; ++i) {
            stringBuilder.append('\n');
        }
        return n4 + 1;
    }

    private static int arguments(String string, int n, List<String> list) {
        int n2 = 0;
        int n3 = n + 1;
        for (int i = n; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c == '/' && i + 1 < string.length() && (string.charAt(i + 1) == '/' || string.charAt(i + 1) == '*')) {
                i = Macros.commentEnd(string, i) - 1;
                continue;
            }
            if (c == '(') {
                ++n2;
                continue;
            }
            if (c == ')') {
                if (--n2 != 0) continue;
                String string2 = string.substring(n3, i);
                if (!list.isEmpty() || !string2.isBlank()) {
                    list.add(string2);
                }
                return i;
            }
            if (c != ',' || n2 != 1) continue;
            list.add(string.substring(n3, i));
            n3 = i + 1;
        }
        return -1;
    }

    private String substitute(Macro macro, List<String> list, Set<String> set) {
        String string = macro.body;
        StringBuilder stringBuilder = new StringBuilder(string.length() + 16);
        int n = string.length();
        int n2 = 0;
        boolean bl = false;
        while (n2 < n) {
            int n3;
            int n4;
            char c = string.charAt(n2);
            if (c == '#' && n2 + 1 < n && string.charAt(n2 + 1) == '#') {
                Macros.trimEnd(stringBuilder);
                n2 = Macros.skipSpace(string, n2 + 2);
                bl = true;
                continue;
            }
            if (c == '#') {
                int n5;
                for (n5 = n4 = Macros.skipSpace(string, n2 + 1); n5 < n && Macros.isIdentifierPart(string.charAt(n5)); ++n5) {
                }
                n3 = macro.params.indexOf(string.substring(n4, n5));
                if (n3 >= 0) {
                    stringBuilder.append('\"').append(n3 < list.size() ? list.get(n3).strip() : "").append('\"');
                    n2 = n5;
                    continue;
                }
            }
            if (Macros.isIdentifierStart(c)) {
                boolean bl2;
                for (n4 = n2; n4 < n && Macros.isIdentifierPart(string.charAt(n4)); ++n4) {
                }
                String string2 = string.substring(n2, n4);
                n3 = macro.params.indexOf(string2);
                boolean bl3 = bl2 = bl || Macros.skipSpace(string, n4) + 1 < n && string.charAt(Macros.skipSpace(string, n4)) == '#' && string.charAt(Macros.skipSpace(string, n4) + 1) == '#';
                if (n3 >= 0) {
                    String string3 = n3 < list.size() ? list.get(n3).strip() : "";
                    stringBuilder.append(bl2 ? string3 : this.expand(string3, set));
                } else {
                    stringBuilder.append(string2);
                }
                bl = false;
                n2 = n4;
                continue;
            }
            bl = false;
            stringBuilder.append(c);
            ++n2;
        }
        return stringBuilder.toString();
    }

    private static void trimEnd(StringBuilder stringBuilder) {
        while (stringBuilder.length() > 0 && Character.isWhitespace(stringBuilder.charAt(stringBuilder.length() - 1))) {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
    }

    static int commentEnd(String string, int n) {
        if (string.charAt(n + 1) == '/') {
            int n2 = string.indexOf(10, n);
            return n2 < 0 ? string.length() : n2;
        }
        int n3 = string.indexOf("*/", n + 2);
        return n3 < 0 ? string.length() : n3 + 2;
    }

    static String stripComments(String string) {
        StringBuilder stringBuilder = new StringBuilder(string.length());
        int n = 0;
        int n2 = string.length();
        while (n < n2) {
            char c = string.charAt(n);
            if (c == '/' && n + 1 < n2 && (string.charAt(n + 1) == '/' || string.charAt(n + 1) == '*')) {
                int n3 = Macros.commentEnd(string, n);
                if (string.charAt(n + 1) == '*') {
                    stringBuilder.append(' ');
                }
                n = n3;
                continue;
            }
            stringBuilder.append(c);
            ++n;
        }
        return stringBuilder.toString();
    }

    private static int skipSpace(String string, int n) {
        while (n < string.length() && Character.isWhitespace(string.charAt(n))) {
            ++n;
        }
        return n;
    }

    private static int count(String string, int n, int n2, char c) {
        int n3 = 0;
        for (int i = n; i < n2; ++i) {
            if (string.charAt(i) != c) continue;
            ++n3;
        }
        return n3;
    }

    static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    record Macro(String name, List<String> params, String body) {
    }
}

