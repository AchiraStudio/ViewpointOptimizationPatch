/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import viewpoint.iris.ShadersProperties;
import viewpoint.iris.UniformExpression;

public final class CustomUniforms {
    private final List<Entry> entries = new ArrayList<Entry>();
    private final Map<String, double[]> values = new HashMap<String, double[]>();
    private final UniformExpression.State state = new UniformExpression.State();

    private CustomUniforms() {
    }

    public static CustomUniforms parse(ShadersProperties shadersProperties, List<String> list) {
        CustomUniforms customUniforms = new CustomUniforms();
        for (ShadersProperties.CustomUniform customUniform : shadersProperties.uniforms) {
            try {
                customUniforms.entries.add(new Entry(customUniform, UniformExpression.parse(customUniform.expression(), (customUniform.variable() ? "variable." : "uniform.") + customUniform.type() + "." + customUniform.name())));
            }
            catch (IllegalArgumentException illegalArgumentException) {
                list.add("custom uniform left out: " + illegalArgumentException.getMessage());
            }
        }
        return customUniforms;
    }

    public void evaluate(UniformExpression.Names names, double d) {
        this.state.frame(d);
        this.values.clear();
        UniformExpression.Names names2 = string -> {
            double[] dArray = this.values.get(string);
            return dArray != null ? dArray : names.value(string);
        };
        for (Entry entry : this.entries) {
            double[] dArray = entry.expression.evaluate(names2, this.state);
            this.values.put(entry.declaration.name(), CustomUniforms.sized(dArray, entry.declaration.type()));
        }
    }

    public List<Entry> uniforms() {
        ArrayList<Entry> arrayList = new ArrayList<Entry>();
        for (Entry entry : this.entries) {
            if (entry.declaration.variable()) continue;
            arrayList.add(entry);
        }
        return arrayList;
    }

    public double[] value(String string) {
        return this.values.get(string);
    }

    private static double[] sized(double[] dArray, String string) {
        int n;
        Object object = string;
        int n2 = -1;
        switch (((String)object).hashCode()) {
            case 3615518: {
                if (!((String)object).equals("vec2")) break;
                n2 = 0;
                break;
            }
            case 100585223: {
                if (!((String)object).equals("ivec2")) break;
                n2 = 1;
                break;
            }
            case 3615519: {
                if (!((String)object).equals("vec3")) break;
                n2 = 2;
                break;
            }
            case 100585224: {
                if (!((String)object).equals("ivec3")) break;
                n2 = 3;
                break;
            }
            case 3615520: {
                if (!((String)object).equals("vec4")) break;
                n2 = 4;
                break;
            }
            case 100585225: {
                if (!((String)object).equals("ivec4")) break;
                n2 = 5;
            }
        }
        switch (n2) {
            case 0: 
            case 1: {
                int n3 = 2;
                break;
            }
            case 2: 
            case 3: {
                int n3 = 3;
                break;
            }
            case 4: 
            case 5: {
                int n3 = 4;
                break;
            }
            default: {
                int n3 = n = 1;
            }
        }
        if (dArray.length == n) {
            return dArray;
        }
        object = new double[n];
        for (n2 = 0; n2 < n; ++n2) {
            object[n2] = dArray[Math.min(n2, dArray.length - 1)];
        }
        return object;
    }

    public record Entry(ShadersProperties.CustomUniform declaration, UniformExpression expression) {
    }
}

