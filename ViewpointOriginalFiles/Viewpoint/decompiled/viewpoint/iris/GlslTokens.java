/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.ArrayList;
import java.util.List;
import viewpoint.iris.Macros;

final class GlslTokens {
    private static final String[] OPERATORS = new String[]{"<<=", ">>=", "++", "--", "+=", "-=", "*=", "/=", "%=", "&=", "^=", "|=", "&&", "||", "^^", "==", "!=", "<=", ">=", "<<", ">>"};

    static List<Token> lex(String string) {
        ArrayList<Token> arrayList = new ArrayList<Token>(string.length() / 3);
        int n = string.length();
        int n2 = 0;
        boolean bl = true;
        while (n2 < n) {
            char c = string.charAt(n2);
            int n3 = n2;
            if (c == '#' && bl) {
                int n4 = string.indexOf(10, n2);
                n4 = n4 < 0 ? n : n4;
                arrayList.add(new Token(Kind.DIRECTIVE, string.substring(n2, n4)));
                n2 = n4;
                continue;
            }
            if (Character.isWhitespace(c)) {
                while (n2 < n && Character.isWhitespace(string.charAt(n2))) {
                    if (string.charAt(n2) == '\n') {
                        bl = true;
                    }
                    ++n2;
                }
                arrayList.add(new Token(Kind.SPACE, string.substring(n3, n2)));
                continue;
            }
            bl = false;
            if (c == '/' && n2 + 1 < n && (string.charAt(n2 + 1) == '/' || string.charAt(n2 + 1) == '*')) {
                n2 = Macros.commentEnd(string, n2);
                arrayList.add(new Token(Kind.COMMENT, string.substring(n3, n2)));
                continue;
            }
            if (Macros.isIdentifierStart(c)) {
                while (n2 < n && Macros.isIdentifierPart(string.charAt(n2))) {
                    ++n2;
                }
                arrayList.add(new Token(Kind.NAME, string.substring(n3, n2)));
                continue;
            }
            if (Character.isDigit(c) || c == '.' && n2 + 1 < n && Character.isDigit(string.charAt(n2 + 1))) {
                ++n2;
                while (!(n2 >= n || !Macros.isIdentifierPart(string.charAt(n2)) && string.charAt(n2) != '.' && (string.charAt(n2) != '+' && string.charAt(n2) != '-' || string.charAt(n2 - 1) != 'e' && string.charAt(n2 - 1) != 'E' || string.startsWith("0x", n3)))) {
                    ++n2;
                }
                arrayList.add(new Token(Kind.NUMBER, string.substring(n3, n2)));
                continue;
            }
            String string2 = GlslTokens.operator(string, n2);
            n2 += string2.length();
            arrayList.add(new Token(Kind.PUNCT, string2));
        }
        return arrayList;
    }

    private static String operator(String string, int n) {
        for (String string2 : OPERATORS) {
            if (!string.startsWith(string2, n)) continue;
            return string2;
        }
        return String.valueOf(string.charAt(n));
    }

    static String join(List<Token> list) {
        StringBuilder stringBuilder = new StringBuilder(list.size() * 4);
        for (Token token : list) {
            stringBuilder.append(token.text);
        }
        return stringBuilder.toString();
    }

    static int next(List<Token> list, int n) {
        while (n < list.size() && !list.get(n).significant()) {
            ++n;
        }
        return n;
    }

    static int previous(List<Token> list, int n) {
        --n;
        while (n >= 0 && !list.get(n).significant()) {
            --n;
        }
        return n;
    }

    static int close(List<Token> list, int n) {
        String string = list.get((int)n).text;
        String string2 = string.equals("(") ? ")" : (string.equals("[") ? "]" : "}");
        int n2 = 0;
        for (int i = n; i < list.size(); ++i) {
            Token token = list.get(i);
            if (token.kind != Kind.PUNCT) continue;
            if (token.is(string)) {
                ++n2;
                continue;
            }
            if (!token.is(string2) || --n2 != 0) continue;
            return i;
        }
        return -1;
    }

    private GlslTokens() {
    }

    static final class Token {
        final Kind kind;
        String text;

        Token(Kind kind, String string) {
            this.kind = kind;
            this.text = string;
        }

        boolean is(String string) {
            return this.text.equals(string);
        }

        boolean isName() {
            return this.kind == Kind.NAME;
        }

        boolean significant() {
            return this.kind != Kind.SPACE && this.kind != Kind.COMMENT && this.kind != Kind.DIRECTIVE;
        }

        public String toString() {
            return this.text;
        }
    }

    static enum Kind {
        NAME,
        NUMBER,
        PUNCT,
        SPACE,
        COMMENT,
        DIRECTIVE;

    }
}

