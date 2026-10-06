/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.Locale;
import viewpoint.iris.Macros;

final class IfExpression {
    private final String text;
    private int at;
    private static final String[][] LEVELS = new String[][]{{"||"}, {"&&"}, {"|"}, {"^"}, {"&"}, {"==", "!="}, {"<=", ">=", "<", ">"}, {"<<", ">>"}, {"+", "-"}, {"*", "/", "%"}};

    private IfExpression(String string) {
        this.text = string;
    }

    static boolean test(String string, Macros macros) {
        return IfExpression.evaluate(string, macros) != 0.0;
    }

    static double evaluate(String string, Macros macros) {
        String string2 = macros.expand(IfExpression.definedAnswered(Macros.stripComments(string), macros));
        IfExpression ifExpression = new IfExpression(string2);
        double d = ifExpression.ternary();
        ifExpression.space();
        if (ifExpression.at < ifExpression.text.length()) {
            throw new IllegalArgumentException("cannot read the condition '" + string.strip() + "' at '" + ifExpression.text.substring(ifExpression.at).strip() + "'");
        }
        return d;
    }

    private static String definedAnswered(String string, Macros macros) {
        StringBuilder stringBuilder = new StringBuilder(string.length());
        int n = 0;
        int n2 = string.length();
        while (n < n2) {
            if (!(!string.startsWith("defined", n) || n != 0 && Macros.isIdentifierPart(string.charAt(n - 1)) || n + 7 < n2 && Macros.isIdentifierPart(string.charAt(n + 7)))) {
                boolean bl;
                int n3;
                for (n3 = n + 7; n3 < n2 && Character.isWhitespace(string.charAt(n3)); ++n3) {
                }
                boolean bl2 = bl = n3 < n2 && string.charAt(n3) == '(';
                if (bl) {
                    ++n3;
                }
                while (n3 < n2 && Character.isWhitespace(string.charAt(n3))) {
                    ++n3;
                }
                int n4 = n3;
                while (n3 < n2 && Macros.isIdentifierPart(string.charAt(n3))) {
                    ++n3;
                }
                String string2 = string.substring(n4, n3);
                if (bl) {
                    while (n3 < n2 && string.charAt(n3) != ')') {
                        ++n3;
                    }
                    ++n3;
                }
                stringBuilder.append(macros.defined(string2) ? " 1 " : " 0 ");
                n = n3;
                continue;
            }
            stringBuilder.append(string.charAt(n));
            ++n;
        }
        return stringBuilder.toString();
    }

    private double ternary() {
        double d = this.binary(0);
        this.space();
        if (this.peek('?')) {
            ++this.at;
            double d2 = this.ternary();
            this.space();
            this.expect(':');
            double d3 = this.ternary();
            return d != 0.0 ? d2 : d3;
        }
        return d;
    }

    private double binary(int n) {
        if (n == LEVELS.length) {
            return this.unary();
        }
        double d = this.binary(n + 1);
        while (true) {
            this.space();
            String string = this.operator(LEVELS[n]);
            if (string == null) {
                return d;
            }
            double d2 = this.binary(n + 1);
            d = IfExpression.apply(string, d, d2);
        }
    }

    private String operator(String[] stringArray) {
        for (String string : stringArray) {
            if (!this.text.startsWith(string, this.at)) continue;
            if (string.length() == 1 && this.at + 1 < this.text.length()) {
                char c = this.text.charAt(this.at + 1);
                if (string.equals("|") && c == '|' || string.equals("&") && c == '&' || string.equals("<") && (c == '<' || c == '=') || string.equals(">") && (c == '>' || c == '=')) continue;
            }
            this.at += string.length();
            return string;
        }
        return null;
    }

    private static double apply(String string, double d, double d2) {
        return switch (string) {
            case "||" -> {
                if (d != 0.0 || d2 != 0.0) {
                    yield 1.0;
                }
                yield 0.0;
            }
            case "&&" -> {
                if (d != 0.0 && d2 != 0.0) {
                    yield 1.0;
                }
                yield 0.0;
            }
            case "|" -> (long)d | (long)d2;
            case "^" -> (long)d ^ (long)d2;
            case "&" -> (long)d & (long)d2;
            case "==" -> {
                if (d == d2) {
                    yield 1.0;
                }
                yield 0.0;
            }
            case "!=" -> {
                if (d != d2) {
                    yield 1.0;
                }
                yield 0.0;
            }
            case "<=" -> {
                if (d <= d2) {
                    yield 1.0;
                }
                yield 0.0;
            }
            case ">=" -> {
                if (d >= d2) {
                    yield 1.0;
                }
                yield 0.0;
            }
            case "<" -> {
                if (d < d2) {
                    yield 1.0;
                }
                yield 0.0;
            }
            case ">" -> {
                if (d > d2) {
                    yield 1.0;
                }
                yield 0.0;
            }
            case "<<" -> (long)d << (int)d2;
            case ">>" -> (long)d >> (int)d2;
            case "+" -> d + d2;
            case "-" -> d - d2;
            case "*" -> d * d2;
            case "/" -> {
                if (d2 == 0.0) {
                    yield 0.0;
                }
                if (IfExpression.isWhole(d) && IfExpression.isWhole(d2)) {
                    yield (long)d / (long)d2;
                }
                yield d / d2;
            }
            case "%" -> {
                if (d2 == 0.0) {
                    yield 0.0;
                }
                yield d % d2;
            }
            default -> throw new IllegalStateException(string);
        };
    }

    private static boolean isWhole(double d) {
        return d == Math.rint(d);
    }

    private double unary() {
        this.space();
        if (this.peek('!')) {
            ++this.at;
            return this.unary() == 0.0 ? 1.0 : 0.0;
        }
        if (this.peek('-')) {
            ++this.at;
            return -this.unary();
        }
        if (this.peek('+')) {
            ++this.at;
            return this.unary();
        }
        if (this.peek('~')) {
            ++this.at;
            return (long)this.unary() ^ 0xFFFFFFFFFFFFFFFFL;
        }
        return this.primary();
    }

    private double primary() {
        this.space();
        if (this.peek('(')) {
            ++this.at;
            double d = this.ternary();
            this.space();
            this.expect(')');
            return d;
        }
        int n = this.at;
        if (this.at < this.text.length() && (Character.isDigit(this.text.charAt(this.at)) || this.text.charAt(this.at) == '.')) {
            while (!(this.at >= this.text.length() || !Macros.isIdentifierPart(this.text.charAt(this.at)) && this.text.charAt(this.at) != '.' && (this.text.charAt(this.at) != '+' && this.text.charAt(this.at) != '-' || this.text.charAt(this.at - 1) != 'e' && this.text.charAt(this.at - 1) != 'E' || this.text.substring(n, this.at).startsWith("0x")))) {
                ++this.at;
            }
            return IfExpression.number(this.text.substring(n, this.at));
        }
        if (this.at < this.text.length() && Macros.isIdentifierStart(this.text.charAt(this.at))) {
            while (this.at < this.text.length() && Macros.isIdentifierPart(this.text.charAt(this.at))) {
                ++this.at;
            }
            String string = this.text.substring(n, this.at);
            return string.equals("true") ? 1.0 : 0.0;
        }
        throw new IllegalArgumentException("cannot read the condition '" + this.text.strip() + "'");
    }

    private static double number(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        if (string2.startsWith("0x")) {
            return Long.parseLong(string2.substring(2).replaceAll("[ul]+$", ""), 16);
        }
        string2 = string2.replaceAll("(lf|[ulf])+$", "");
        return Double.parseDouble(string2);
    }

    private boolean peek(char c) {
        return this.at < this.text.length() && this.text.charAt(this.at) == c;
    }

    private void expect(char c) {
        if (!this.peek(c)) {
            throw new IllegalArgumentException("expected '" + c + "' in the condition '" + this.text.strip() + "'");
        }
        ++this.at;
    }

    private void space() {
        while (this.at < this.text.length() && Character.isWhitespace(this.text.charAt(this.at))) {
            ++this.at;
        }
    }
}

