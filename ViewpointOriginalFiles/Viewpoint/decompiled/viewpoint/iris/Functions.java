/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.iris;

import java.util.Set;

final class Functions {
    private static final Set<String> NAMES = Set.of("sin", "cos", "tan", "asin", "acos", "atan", "atan2", "torad", "todeg", "min", "max", "clamp", "abs", "floor", "ceil", "exp", "exp2", "frac", "fract", "log", "log2", "pow", "round", "signum", "sign", "sqrt", "fmod", "mod", "between", "equals", "in", "random", "mix", "lerp", "smoothstep", "step", "length", "dot", "normalize", "hsv", "not");

    static boolean known(String string) {
        return NAMES.contains(string);
    }

    static double[] apply(String string, double[][] dArray) {
        double[] dArray2;
        switch (string) {
            case "min": {
                dArray2 = Functions.fold(dArray, true);
                break;
            }
            case "max": {
                dArray2 = Functions.fold(dArray, false);
                break;
            }
            case "clamp": {
                dArray2 = Functions.each3(dArray, (d, d2, d3) -> Math.max(d2, Math.min(d3, d)));
                break;
            }
            case "pow": {
                dArray2 = Functions.each2(dArray, Math::pow);
                break;
            }
            case "atan2": {
                dArray2 = Functions.each2(dArray, Math::atan2);
                break;
            }
            case "fmod": 
            case "mod": {
                dArray2 = Functions.each2(dArray, (d, d2) -> d2 == 0.0 ? 0.0 : d - d2 * Math.floor(d / d2));
                break;
            }
            case "step": {
                dArray2 = Functions.each2(dArray, (d, d2) -> d2 < d ? 0.0 : 1.0);
                break;
            }
            case "mix": 
            case "lerp": {
                dArray2 = Functions.each3(dArray, (d, d2, d3) -> d + (d2 - d) * d3);
                break;
            }
            case "smoothstep": {
                dArray2 = Functions.each3(dArray, (d, d2, d3) -> Functions.smoothstep(d, d2, d3));
                break;
            }
            case "between": {
                dArray2 = Functions.scalar(dArray[0][0] >= dArray[1][0] && dArray[0][0] <= dArray[2][0]);
                break;
            }
            case "equals": {
                dArray2 = Functions.scalar(Math.abs(dArray[0][0] - dArray[1][0]) <= (dArray.length > 2 ? dArray[2][0] : 0.0));
                break;
            }
            case "in": {
                dArray2 = Functions.in(dArray);
                break;
            }
            case "random": {
                double[] dArray3 = new double[1];
                dArray2 = dArray3;
                dArray3[0] = Math.random();
                break;
            }
            case "length": {
                double[] dArray4 = new double[1];
                dArray2 = dArray4;
                dArray4[0] = Math.sqrt(Functions.dot(dArray[0], dArray[0]));
                break;
            }
            case "dot": {
                double[] dArray5 = new double[1];
                dArray2 = dArray5;
                dArray5[0] = Functions.dot(dArray[0], dArray[1]);
                break;
            }
            case "normalize": {
                dArray2 = Functions.normalize(dArray[0]);
                break;
            }
            case "not": {
                dArray2 = Functions.scalar(dArray[0][0] == 0.0);
                break;
            }
            default: {
                dArray2 = Functions.each1(string, dArray[0]);
            }
        }
        return dArray2;
    }

    private static double[] each1(String string, double[] dArray) {
        double[] dArray2 = new double[dArray.length];
        for (int i = 0; i < dArray.length; ++i) {
            double d = dArray[i];
            dArray2[i] = switch (string) {
                case "sin" -> Math.sin(d);
                case "cos" -> Math.cos(d);
                case "tan" -> Math.tan(d);
                case "asin" -> Math.asin(d);
                case "acos" -> Math.acos(d);
                case "atan" -> Math.atan(d);
                case "torad" -> Math.toRadians(d);
                case "todeg" -> Math.toDegrees(d);
                case "abs" -> Math.abs(d);
                case "floor" -> Math.floor(d);
                case "ceil" -> Math.ceil(d);
                case "exp" -> Math.exp(d);
                case "exp2" -> Math.pow(2.0, d);
                case "frac", "fract" -> d - Math.floor(d);
                case "log" -> Math.log(d);
                case "log2" -> Math.log(d) / Math.log(2.0);
                case "round" -> Math.rint(d);
                case "signum", "sign" -> Math.signum(d);
                case "sqrt" -> Math.sqrt(d);
                default -> throw new IllegalArgumentException("unknown function " + string);
            };
        }
        return dArray2;
    }

    private static double[] each2(double[][] dArray, Op2 op2) {
        int n = Math.max(dArray[0].length, dArray[1].length);
        double[] dArray2 = new double[n];
        for (int i = 0; i < n; ++i) {
            dArray2[i] = op2.apply(Functions.at(dArray[0], i), Functions.at(dArray[1], i));
        }
        return dArray2;
    }

    private static double[] each3(double[][] dArray, Op3 op3) {
        int n = Math.max(dArray[0].length, Math.max(dArray[1].length, dArray[2].length));
        double[] dArray2 = new double[n];
        for (int i = 0; i < n; ++i) {
            dArray2[i] = op3.apply(Functions.at(dArray[0], i), Functions.at(dArray[1], i), Functions.at(dArray[2], i));
        }
        return dArray2;
    }

    private static double[] fold(double[][] dArray, boolean bl) {
        double[] dArray2 = (double[])dArray[0].clone();
        for (int i = 1; i < dArray.length; ++i) {
            for (int j = 0; j < dArray2.length; ++j) {
                double d = Functions.at(dArray[i], j);
                dArray2[j] = bl ? Math.min(dArray2[j], d) : Math.max(dArray2[j], d);
            }
        }
        return dArray2;
    }

    private static double[] in(double[][] dArray) {
        for (int i = 1; i < dArray.length; ++i) {
            if (dArray[i][0] != dArray[0][0]) continue;
            return Functions.scalar(true);
        }
        return Functions.scalar(false);
    }

    private static double at(double[] dArray, int n) {
        return dArray[Math.min(n, dArray.length - 1)];
    }

    private static double dot(double[] dArray, double[] dArray2) {
        double d = 0.0;
        for (int i = 0; i < Math.min(dArray.length, dArray2.length); ++i) {
            d += dArray[i] * dArray2[i];
        }
        return d;
    }

    private static double[] normalize(double[] dArray) {
        double d = Math.sqrt(Functions.dot(dArray, dArray));
        double[] dArray2 = (double[])dArray.clone();
        int n = 0;
        while (n < dArray2.length && d > 0.0) {
            int n2 = n++;
            dArray2[n2] = dArray2[n2] / d;
        }
        return dArray2;
    }

    private static double smoothstep(double d, double d2, double d3) {
        double d4 = Math.max(0.0, Math.min(1.0, (d3 - d) / (d2 - d)));
        return d4 * d4 * (3.0 - 2.0 * d4);
    }

    private static double[] scalar(boolean bl) {
        return new double[]{bl ? 1.0 : 0.0};
    }

    private Functions() {
    }

    private static interface Op3 {
        public double apply(double var1, double var3, double var5);
    }

    private static interface Op2 {
        public double apply(double var1, double var3);
    }
}

