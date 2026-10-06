/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import viewpoint.iris.Biomes;
import viewpoint.iris.Functions;
import viewpoint.iris.Macros;

public final class UniformExpression {
    private final Node root;
    private final String text;
    private int at;
    private int smooths;
    private final String owner;
    private static final String[][] LEVELS = new String[][]{{"||"}, {"&&"}, {"==", "!="}, {"<=", ">=", "<", ">"}, {"+", "-"}, {"*", "/", "%"}};

    private UniformExpression(String string, String string2) {
        this.text = string;
        this.owner = string2;
        this.root = this.parseAll();
    }

    public static UniformExpression parse(String string, String string2) {
        return new UniformExpression(string, string2);
    }

    public double[] evaluate(Names names, State state) {
        return this.root.eval(names, state);
    }

    private Node parseAll() {
        Node node = this.ternary();
        this.space();
        if (this.at < this.text.length()) {
            throw this.error("unexpected '" + this.text.substring(this.at) + "'");
        }
        return node;
    }

    private IllegalArgumentException error(String string) {
        return new IllegalArgumentException(this.owner + ": " + string + " in '" + this.text + "'");
    }

    private Node ternary() {
        final Node node = this.binary(0);
        this.space();
        if (this.peek('?')) {
            ++this.at;
            final Node node2 = this.ternary();
            this.space();
            this.expect(':');
            final Node node3 = this.ternary();
            return new Node(this){
                final /* synthetic */ UniformExpression this$0;
                {
                    this.this$0 = uniformExpression;
                }

                @Override
                double[] eval(Names names, State state) {
                    return node.eval(names, state)[0] != 0.0 ? node2.eval(names, state) : node3.eval(names, state);
                }
            };
        }
        return node;
    }

    private Node binary(int n) {
        if (n == LEVELS.length) {
            return this.unary();
        }
        Node node = this.binary(n + 1);
        while (true) {
            this.space();
            String string = null;
            for (String string2 : LEVELS[n]) {
                if (!this.text.startsWith(string2, this.at) || string2.length() == 1 && this.at + 1 < this.text.length() && this.text.charAt(this.at + 1) == '=' && (string2.equals("<") || string2.equals(">"))) continue;
                string = string2;
                break;
            }
            if (string == null) {
                return node;
            }
            this.at += string.length();
            Node object = this.binary(n + 1);
            node = UniformExpression.operation(string, node, object);
        }
    }

    private static Node operation(final String string, final Node node, final Node node2) {
        return new Node(){

            @Override
            double[] eval(Names names, State state) {
                double[] dArray = node.eval(names, state);
                double[] dArray2 = node2.eval(names, state);
                if (string.equals("&&")) {
                    return UniformExpression.scalar(dArray[0] != 0.0 && dArray2[0] != 0.0 ? 1.0 : 0.0);
                }
                if (string.equals("||")) {
                    return UniformExpression.scalar(dArray[0] != 0.0 || dArray2[0] != 0.0 ? 1.0 : 0.0);
                }
                int n = Math.max(dArray.length, dArray2.length);
                double[] dArray3 = new double[n];
                for (int i = 0; i < n; ++i) {
                    dArray3[i] = UniformExpression.apply(string, dArray[Math.min(i, dArray.length - 1)], dArray2[Math.min(i, dArray2.length - 1)]);
                }
                return n > 1 && UniformExpression.isComparison(string) ? UniformExpression.scalar(UniformExpression.all(dArray3)) : dArray3;
            }
        };
    }

    private static boolean isComparison(String string) {
        return string.equals("==") || string.equals("!=") || string.startsWith("<") || string.startsWith(">");
    }

    private static double all(double[] dArray) {
        for (double d : dArray) {
            if (d != 0.0) continue;
            return 0.0;
        }
        return 1.0;
    }

    private static double apply(String string, double d, double d2) {
        return switch (string) {
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
            case "+" -> d + d2;
            case "-" -> d - d2;
            case "*" -> d * d2;
            case "/" -> {
                if (d2 == 0.0) {
                    yield 0.0;
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

    private Node unary() {
        this.space();
        if (this.peek('!')) {
            ++this.at;
            final Node node = this.unary();
            return new Node(this){
                final /* synthetic */ UniformExpression this$0;
                {
                    this.this$0 = uniformExpression;
                }

                @Override
                double[] eval(Names names, State state) {
                    return UniformExpression.scalar(node.eval(names, state)[0] == 0.0 ? 1.0 : 0.0);
                }
            };
        }
        if (this.peek('-')) {
            ++this.at;
            final Node node = this.unary();
            return new Node(this){
                final /* synthetic */ UniformExpression this$0;
                {
                    this.this$0 = uniformExpression;
                }

                @Override
                double[] eval(Names names, State state) {
                    double[] dArray = (double[])node.eval(names, state).clone();
                    for (int i = 0; i < dArray.length; ++i) {
                        dArray[i] = -dArray[i];
                    }
                    return dArray;
                }
            };
        }
        if (this.peek('+')) {
            ++this.at;
            return this.unary();
        }
        return this.postfix(this.primary());
    }

    private Node postfix(Node node) {
        Node node2 = node;
        while (this.peek('.') && this.at + 1 < this.text.length() && !Character.isWhitespace(this.text.charAt(this.at + 1))) {
            int n = ++this.at;
            while (this.at < this.text.length() && Macros.isIdentifierPart(this.text.charAt(this.at))) {
                ++this.at;
            }
            String string = this.text.substring(n, this.at);
            int n2 = -1;
            if (string.matches("\\d+") && this.peek('.')) {
                n2 = Integer.parseInt(string);
                ++this.at;
                n = this.at;
                while (this.at < this.text.length() && Character.isDigit(this.text.charAt(this.at))) {
                    ++this.at;
                }
                string = this.text.substring(n, this.at);
            }
            node2 = this.component(node2, n2, string);
        }
        return node2;
    }

    private Node component(final Node node, int n, String string) {
        int n2;
        int n3 = n2 = n >= 0 ? n * 4 + Integer.parseInt(string) : "xyzwrgbastpq".indexOf(string.charAt(0)) % 4;
        if (string.isEmpty() || n2 < 0) {
            throw this.error("unknown component ." + string);
        }
        final int n4 = n2;
        return new Node(this){
            final /* synthetic */ UniformExpression this$0;
            {
                this.this$0 = uniformExpression;
            }

            @Override
            double[] eval(Names names, State state) {
                double[] dArray = node.eval(names, state);
                return UniformExpression.scalar(n4 < dArray.length ? dArray[n4] : 0.0);
            }
        };
    }

    private Node primary() {
        this.space();
        if (this.peek('(')) {
            ++this.at;
            Node node = this.ternary();
            this.space();
            this.expect(')');
            return node;
        }
        int n = this.at;
        if (this.at < this.text.length() && (Character.isDigit(this.text.charAt(this.at)) || this.text.charAt(this.at) == '.')) {
            while (this.at < this.text.length() && (Character.isDigit(this.text.charAt(this.at)) || this.text.charAt(this.at) == '.' || this.text.charAt(this.at) == 'e' || (this.text.charAt(this.at) == '-' || this.text.charAt(this.at) == '+') && this.text.charAt(this.at - 1) == 'e')) {
                ++this.at;
            }
            double d = Double.parseDouble(this.text.substring(n, this.at));
            if (this.at < this.text.length() && (this.text.charAt(this.at) == 'f' || this.text.charAt(this.at) == 'F')) {
                ++this.at;
            }
            return UniformExpression.constant(d);
        }
        if (this.at < this.text.length() && Macros.isIdentifierStart(this.text.charAt(this.at))) {
            while (this.at < this.text.length() && Macros.isIdentifierPart(this.text.charAt(this.at))) {
                ++this.at;
            }
            String string = this.text.substring(n, this.at);
            this.space();
            if (this.peek('(')) {
                ++this.at;
                return this.function(string.toLowerCase(Locale.ROOT), this.arguments());
            }
            return this.name(string);
        }
        throw this.error("cannot read '" + this.text.substring(this.at) + "'");
    }

    private List<Node> arguments() {
        ArrayList<Node> arrayList = new ArrayList<Node>();
        this.space();
        if (this.peek(')')) {
            ++this.at;
            return arrayList;
        }
        while (true) {
            arrayList.add(this.ternary());
            this.space();
            if (!this.peek(',')) break;
            ++this.at;
        }
        this.expect(')');
        return arrayList;
    }

    private Node name(final String string) {
        switch (string) {
            case "true": {
                return UniformExpression.constant(1.0);
            }
            case "false": {
                return UniformExpression.constant(0.0);
            }
            case "pi": {
                return UniformExpression.constant(Math.PI);
            }
        }
        Object object = Biomes.constant(string);
        if (object != null) {
            return UniformExpression.constant((Double)object);
        }
        return new Node(this){
            final /* synthetic */ UniformExpression this$0;
            {
                this.this$0 = uniformExpression;
            }

            @Override
            double[] eval(Names names, State state) {
                double[] dArray = names.value(string);
                return dArray == null ? UniformExpression.scalar(0.0) : dArray;
            }
        };
    }

    private Node function(String string, List<Node> list) {
        return switch (string) {
            case "if" -> this.ifNode(list);
            case "smooth" -> this.smoothNode(list);
            case "vec2", "vec3", "vec4" -> this.vector(string.charAt(3) - 48, list);
            default -> this.math(string, list);
        };
    }

    private Node ifNode(final List<Node> list) {
        return new Node(this){
            final /* synthetic */ UniformExpression this$0;
            {
                this.this$0 = uniformExpression;
            }

            @Override
            double[] eval(Names names, State state) {
                int n = 0;
                while (n + 1 < list.size()) {
                    if (((Node)list.get(n)).eval(names, state)[0] != 0.0) {
                        return ((Node)list.get(n + 1)).eval(names, state);
                    }
                    n += 2;
                }
                return n < list.size() ? ((Node)list.get(n)).eval(names, state) : UniformExpression.scalar(0.0);
            }
        };
    }

    private Node smoothNode(List<Node> list) {
        final String string = this.owner + "#" + this.smooths++;
        final List<Node> list2 = list.size() == 4 ? list.subList(1, list.size()) : list;
        return new Node(this){
            final /* synthetic */ UniformExpression this$0;
            {
                this.this$0 = uniformExpression;
            }

            @Override
            double[] eval(Names names, State state) {
                double d = ((Node)list2.get(0)).eval(names, state)[0];
                double d2 = list2.size() > 1 ? ((Node)list2.get(1)).eval(names, state)[0] : 1.0;
                double d3 = list2.size() > 2 ? ((Node)list2.get(2)).eval(names, state)[0] : d2;
                Double d4 = state.eased.get(string);
                if (d4 == null) {
                    state.eased.put(string, d);
                    return UniformExpression.scalar(d);
                }
                double d5 = d > d4 ? d2 : d3;
                double d6 = d5 <= 0.0 ? 1.0 : 1.0 - Math.exp(-state.frameSeconds * 3.0 / d5);
                double d7 = d4 + (d - d4) * d6;
                state.eased.put(string, d7);
                return UniformExpression.scalar(d7);
            }
        };
    }

    private Node vector(final int n, final List<Node> list) {
        return new Node(this){
            final /* synthetic */ UniformExpression this$0;
            {
                this.this$0 = uniformExpression;
            }

            @Override
            double[] eval(Names names, State state) {
                double[] dArray = new double[n];
                int n2 = 0;
                for (Node node : list) {
                    for (double d : node.eval(names, state)) {
                        if (n2 >= n) continue;
                        dArray[n2++] = d;
                    }
                }
                while (n2 < n && n2 > 0) {
                    dArray[n2] = dArray[n2 - 1];
                    ++n2;
                }
                return dArray;
            }
        };
    }

    private Node math(final String string, final List<Node> list) {
        if (!Functions.known(string)) {
            throw this.error("unknown function " + string + "()");
        }
        return new Node(this){
            final /* synthetic */ UniformExpression this$0;
            {
                this.this$0 = uniformExpression;
            }

            @Override
            double[] eval(Names names, State state) {
                double[][] dArrayArray = new double[list.size()][];
                for (int i = 0; i < dArrayArray.length; ++i) {
                    dArrayArray[i] = ((Node)list.get(i)).eval(names, state);
                }
                return Functions.apply(string, dArrayArray);
            }
        };
    }

    private static Node constant(double d) {
        final double[] dArray = UniformExpression.scalar(d);
        return new Node(){

            @Override
            double[] eval(Names names, State state) {
                return dArray;
            }
        };
    }

    static double[] scalar(double d) {
        return new double[]{d};
    }

    private boolean peek(char c) {
        return this.at < this.text.length() && this.text.charAt(this.at) == c;
    }

    private void expect(char c) {
        if (!this.peek(c)) {
            throw this.error("expected '" + c + "'");
        }
        ++this.at;
    }

    private void space() {
        while (this.at < this.text.length() && Character.isWhitespace(this.text.charAt(this.at))) {
            ++this.at;
        }
    }

    private static abstract class Node {
        private Node() {
        }

        abstract double[] eval(Names var1, State var2);
    }

    public static interface Names {
        public double[] value(String var1);
    }

    public static final class State {
        final Map<String, Double> eased = new HashMap<String, Double>();
        double frameSeconds;

        public void frame(double d) {
            this.frameSeconds = d;
        }
    }
}

