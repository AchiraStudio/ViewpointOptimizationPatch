/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import viewpoint.iris.IfExpression;
import viewpoint.iris.Macros;
import viewpoint.iris.PackSource;
import viewpoint.iris.SourceFiles;

public final class Preprocessor {
    private static final Pattern DIRECTIVE = Pattern.compile("^\\s*#\\s*(\\w+)(.*)$", 32);
    private static final Pattern COMMENTED_DEFINE = Pattern.compile("^\\s*//+\\s*#\\s*define\\s+(\\w+)\\s*(//.*)?$");
    private static final Pattern VALUE_DEFINE = Pattern.compile("^(\\s*#\\s*define\\s+(\\w+)\\s+)([^\\s/]+)(.*)$");
    private static final Pattern CONST_OPTION = Pattern.compile("^(\\s*const\\s+\\w+\\s+(\\w+)\\s*=\\s*)([^;]+)(;.*)$");
    private static final Pattern INCLUDE = Pattern.compile("^\\s*[\"<]([^\">]+)[\">]");
    private static final int MAX_DEPTH = 64;
    private final SourceFiles source;
    private final boolean rootIncludes;
    private final Map<String, String> options;
    private final Set<String> toggles;
    private final Macros macros = new Macros();
    private final List<String> files = new ArrayList<String>();
    private final Set<String> extensions = new LinkedHashSet<String>();
    private final List<String> errors = new ArrayList<String>();
    private final StringBuilder out = new StringBuilder(65536);
    private final Deque<String> including = new ArrayDeque<String>();
    private String version;
    private boolean properties;

    public Preprocessor(SourceFiles sourceFiles, Map<String, String> map, Map<String, String> map2, Set<String> set) {
        this(sourceFiles, false, map, map2, set);
    }

    public Preprocessor(SourceFiles sourceFiles, boolean bl, Map<String, String> map, Map<String, String> map2, Set<String> set) {
        this.source = sourceFiles;
        this.rootIncludes = bl;
        this.options = map2;
        this.toggles = set;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            this.macros.define(entry.getKey(), entry.getValue());
        }
    }

    public Result run(String string) throws IOException {
        this.file(PackSource.normalize(string));
        return new Result(this.out.toString(), List.copyOf(this.files), this.version, List.copyOf(this.extensions), List.copyOf(this.errors));
    }

    public String runProperties(String string, String string2) throws IOException {
        this.properties = true;
        this.files.add(string);
        this.lines(string, string2, 0);
        return this.out.toString();
    }

    public boolean test(String string) {
        return IfExpression.test(string, this.macros);
    }

    public Map<String, String> macros() {
        return this.macros.objectLike();
    }

    private void file(String string) throws IOException {
        if (this.including.size() > 64 || this.including.contains(string)) {
            throw new IllegalArgumentException("include cycle: " + String.join((CharSequence)" > ", this.including) + " > " + string);
        }
        String string2 = this.source.read(string);
        if (string2 == null) {
            throw new IllegalArgumentException("missing file " + string + (String)(this.including.isEmpty() ? "" : " (included from " + this.including.peekLast() + ")"));
        }
        this.including.addLast(string);
        int n = this.files.size();
        this.files.add(string);
        this.out.append("#line 1 ").append(n).append('\n');
        this.lines(string, string2, n);
        this.including.removeLast();
    }

    private void lines(String string, String string2, int n) throws IOException {
        String[] stringArray = string2.split("\n", -1);
        ArrayDeque<boolean[]> arrayDeque = new ArrayDeque<boolean[]>();
        StringBuilder stringBuilder = new StringBuilder();
        boolean bl = false;
        for (int i = 0; i < stringArray.length; ++i) {
            Matcher matcher;
            int n2 = i;
            Object object = stringArray[i];
            while (stringArray[i].endsWith("\\") && i + 1 < stringArray.length) {
                object = ((String)object).substring(0, ((String)object).length() - 1) + stringArray[++i];
            }
            if (i > n2) {
                object = ((String)object).replaceAll("\\\\+$", "");
            }
            boolean bl2 = arrayDeque.isEmpty() || ((boolean[])arrayDeque.peek())[0];
            boolean bl3 = bl;
            bl = Preprocessor.endsInComment((String)object, bl);
            Matcher matcher2 = matcher = bl3 ? null : DIRECTIVE.matcher((CharSequence)object);
            if (matcher != null && matcher.matches()) {
                this.flush(stringBuilder);
                this.directive(matcher.group(1), matcher.group(2), arrayDeque, bl2, string);
                if (matcher.group(1).equals("include") && bl2) {
                    this.out.append("#line ").append(i + 2).append(' ').append(n).append('\n');
                    continue;
                }
                this.appendBlankLines(i - n2 + 1);
                continue;
            }
            if (!bl2) {
                this.flush(stringBuilder);
                this.appendBlankLines(i - n2 + 1);
                continue;
            }
            stringBuilder.append(this.optionLine((String)object)).append('\n');
            for (int j = n2; j < i; ++j) {
                stringBuilder.append('\n');
            }
        }
        this.flush(stringBuilder);
        if (!arrayDeque.isEmpty()) {
            this.errors.add(string + ": #if without #endif");
        }
    }

    private void flush(StringBuilder stringBuilder) {
        if (stringBuilder.length() == 0) {
            return;
        }
        this.out.append(this.properties ? stringBuilder : this.macros.expand(stringBuilder.toString()));
        stringBuilder.setLength(0);
    }

    private void appendBlankLines(int n) {
        for (int i = 0; i < n; ++i) {
            this.out.append('\n');
        }
    }

    private void directive(String string, String string2, Deque<boolean[]> deque, boolean bl, String string3) throws IOException {
        switch (string) {
            case "if": 
            case "ifdef": 
            case "ifndef": {
                boolean bl2 = bl && this.condition(string, string2, string3);
                deque.push(new boolean[]{bl2, bl2, bl});
                break;
            }
            case "elif": {
                boolean bl3;
                boolean[] blArray = this.top(deque, string3, string);
                blArray[0] = bl3 = blArray[2] && !blArray[1] && this.condition("if", string2, string3);
                blArray[1] = blArray[1] | bl3;
                break;
            }
            case "else": {
                boolean[] blArray = this.top(deque, string3, string);
                blArray[0] = blArray[2] && !blArray[1];
                blArray[1] = true;
                break;
            }
            case "endif": {
                if (deque.isEmpty()) {
                    this.errors.add(string3 + ": #endif without #if");
                    break;
                }
                deque.pop();
                break;
            }
            default: {
                if (!bl) break;
                this.activeDirective(string, string2, string3);
            }
        }
    }

    private void activeDirective(String string, String string2, String string3) throws IOException {
        switch (string) {
            case "define": {
                this.define(string2);
                break;
            }
            case "undef": {
                this.macros.undefine(string2.strip().split("\\s+")[0]);
                break;
            }
            case "include": {
                this.include(string2, string3);
                break;
            }
            case "version": {
                if (this.version != null) break;
                this.version = Macros.stripComments(string2).strip();
                break;
            }
            case "extension": {
                this.extensions.add("#extension " + Macros.stripComments(string2).strip());
                break;
            }
            case "error": {
                this.errors.add(string3 + ": #error " + string2.strip());
                break;
            }
            case "pragma": {
                this.out.append("#pragma").append(string2);
                break;
            }
        }
    }

    private boolean[] top(Deque<boolean[]> deque, String string, String string2) {
        if (deque.isEmpty()) {
            this.errors.add(string + ": #" + string2 + " without #if");
            deque.push(new boolean[]{false, true, false});
        }
        return deque.peek();
    }

    private boolean condition(String string, String string2, String string3) {
        String string4 = Macros.stripComments(string2).strip();
        try {
            return switch (string) {
                case "ifdef" -> this.macros.defined(string4.split("\\s+")[0]);
                case "ifndef" -> {
                    if (!this.macros.defined(string4.split("\\s+")[0])) {
                        yield true;
                    }
                    yield false;
                }
                default -> IfExpression.test(string4, this.macros);
            };
        }
        catch (IllegalArgumentException illegalArgumentException) {
            this.errors.add(string3 + ": " + illegalArgumentException.getMessage());
            return false;
        }
    }

    private void define(String string) {
        Matcher matcher = VALUE_DEFINE.matcher("#define" + string);
        String string2 = string.strip().split("[\\s(]", 2)[0];
        if (this.toggles.contains(string2) && "false".equals(this.options.get(string2))) {
            return;
        }
        if (matcher.matches() && this.options.containsKey(string2) && !this.toggles.contains(string2)) {
            this.macros.define(string2, this.options.get(string2));
            return;
        }
        this.macros.defineFrom(string);
    }

    private String optionLine(String string) {
        if (!string.contains("define") && !string.contains("const")) {
            return string;
        }
        Matcher matcher = COMMENTED_DEFINE.matcher(string);
        if (matcher.matches() && this.toggles.contains(matcher.group(1))) {
            if ("true".equals(this.options.get(matcher.group(1)))) {
                this.macros.define(matcher.group(1), "");
            }
            return "";
        }
        Matcher matcher2 = CONST_OPTION.matcher(string);
        if (matcher2.matches() && this.options.containsKey(matcher2.group(2)) && !this.toggles.contains(matcher2.group(2))) {
            return matcher2.group(1) + this.options.get(matcher2.group(2)) + matcher2.group(4);
        }
        return string;
    }

    private void include(String string, String string2) throws IOException {
        Matcher matcher = INCLUDE.matcher(string);
        if (!matcher.find()) {
            this.errors.add(string2 + ": cannot read #include" + string);
            return;
        }
        String string3 = matcher.group(1);
        String string4 = string2.contains("/") ? string2.substring(0, string2.lastIndexOf(47) + 1) : "";
        String string5 = PackSource.normalize((String)(string3.startsWith("/") || this.rootIncludes ? string3 : string4 + string3));
        if (string5 == null) {
            throw new IllegalArgumentException(string2 + ": #include " + string3 + " leaves the shaders folder");
        }
        this.file(string5);
    }

    private static boolean endsInComment(String string, boolean bl) {
        int n = 0;
        int n2 = string.length();
        while (n < n2) {
            int n3;
            if (bl) {
                n3 = string.indexOf("*/", n);
                if (n3 < 0) {
                    return true;
                }
                bl = false;
                n = n3 + 2;
                continue;
            }
            n3 = string.indexOf(47, n);
            if (n3 < 0 || n3 + 1 >= n2) {
                return false;
            }
            if (string.charAt(n3 + 1) == '/') {
                return false;
            }
            if (string.charAt(n3 + 1) == '*') {
                bl = true;
                n = n3 + 2;
                continue;
            }
            n = n3 + 1;
        }
        return bl;
    }

    public record Result(String text, List<String> files, String version, List<String> extensions, List<String> errors) {
    }
}

