/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GpuScore {
    private static final double LAPTOP = 0.8;
    private static final Pattern NVIDIA = Pattern.compile("\\b(RTX|GTX|GT|MX)\\s*(\\d{3,4})\\s*(TI|SUPER)?");
    private static final Pattern RADEON = Pattern.compile("\\bRX\\s*(\\d{3,4})\\s*(XTX|XT|GRE)?\\s*(M)?\\b");
    private static final Pattern ARC = Pattern.compile("\\bARC\\S*\\s*([AB]\\d{3})\\b");
    private static final Pattern APU = Pattern.compile("\\b(\\d{3})M\\b");
    private static final Map<String, Double> GTX = Map.ofEntries(Map.entry("GTX 750", 0.12), Map.entry("GTX 750 TI", 0.14), Map.entry("GTX 760", 0.17), Map.entry("GTX 770", 0.22), Map.entry("GTX 780", 0.28), Map.entry("GTX 780 TI", 0.33), Map.entry("GTX 950", 0.2), Map.entry("GTX 960", 0.24), Map.entry("GTX 970", 0.36), Map.entry("GTX 980", 0.43), Map.entry("GTX 980 TI", 0.55), Map.entry("GTX 1050", 0.2), Map.entry("GTX 1050 TI", 0.25), Map.entry("GTX 1060", 0.4), Map.entry("GTX 1070", 0.55), Map.entry("GTX 1070 TI", 0.62), Map.entry("GTX 1080", 0.68), Map.entry("GTX 1080 TI", 0.88), Map.entry("GTX 1630", 0.18), Map.entry("GTX 1650", 0.42), Map.entry("GTX 1650 SUPER", 0.55), Map.entry("GTX 1660", 0.6), Map.entry("GTX 1660 SUPER", 0.68), Map.entry("GTX 1660 TI", 0.68), Map.entry("GT 1030", 0.12), Map.entry("RTX 2050", 0.35), Map.entry("RTX 3050", 0.7), Map.entry("RTX 3060 TI", 1.25));
    private static final Map<Integer, Double> RTX_GENERATION = Map.of(20, 0.85, 30, 1.0, 40, 1.2, 50, 1.4);
    private static final Map<Integer, Double> RTX_TIER = Map.of(50, 0.7, 60, 1.0, 70, 1.45, 80, 1.9, 90, 2.4);
    private static final Map<String, Double> RX = Map.ofEntries(Map.entry("460", 0.2), Map.entry("470", 0.36), Map.entry("480", 0.4), Map.entry("550", 0.13), Map.entry("560", 0.2), Map.entry("570", 0.38), Map.entry("580", 0.42), Map.entry("590", 0.47), Map.entry("5500 XT", 0.45), Map.entry("5600 XT", 0.7), Map.entry("5700", 0.8), Map.entry("5700 XT", 0.9), Map.entry("6400", 0.3), Map.entry("6500 XT", 0.35), Map.entry("6600", 0.9), Map.entry("6600 XT", 1.05), Map.entry("6650 XT", 1.1), Map.entry("6700", 1.2), Map.entry("6700 XT", 1.3), Map.entry("6750 XT", 1.4), Map.entry("6800", 1.65), Map.entry("6800 XT", 1.9), Map.entry("6900 XT", 2.05), Map.entry("6950 XT", 2.2), Map.entry("7600", 1.1), Map.entry("7600 XT", 1.15), Map.entry("7700 XT", 1.6), Map.entry("7800 XT", 1.9), Map.entry("7900 GRE", 2.1), Map.entry("7900 XT", 2.5), Map.entry("7900 XTX", 2.9), Map.entry("9060 XT", 1.6), Map.entry("9070", 2.5), Map.entry("9070 XT", 2.8));
    private static final Map<String, Double> INTEL_ARC = Map.of("A310", 0.25, "A380", 0.35, "A580", 0.7, "A750", 0.85, "A770", 0.95, "B570", 0.95, "B580", 1.1);
    private static final Map<String, Double> RADEON_APU = Map.of("660", 0.2, "680", 0.25, "740", 0.18, "760", 0.25, "780", 0.3, "880", 0.35, "890", 0.4);

    static double of(String string) {
        String string2 = GpuScore.normal(string);
        double d = GpuScore.nvidia(string2);
        if (Double.isNaN(d)) {
            d = GpuScore.amd(string2);
        }
        if (Double.isNaN(d)) {
            d = GpuScore.intel(string2);
        }
        boolean bl = string2.contains("LAPTOP") || string2.contains("MAX-Q") || string2.contains("MOBILE");
        return bl ? d * 0.8 : d;
    }

    static boolean integrated(String string) {
        String string2 = GpuScore.normal(string);
        boolean bl = string2.contains("INTEL") && !ARC.matcher(string2).find();
        boolean bl2 = string2.contains("RADEON") && !string2.contains("RX") && (string2.contains("RADEON GRAPHICS") || string2.contains("VEGA ") || APU.matcher(string2).find());
        return bl || bl2;
    }

    private static String normal(String string) {
        return string == null ? "" : string.toUpperCase(Locale.ROOT).replace("(R)", "").replace("(TM)", "").replaceAll("/.*", "").replaceAll("\\s+", " ").trim();
    }

    private static double nvidia(String string) {
        Matcher matcher = NVIDIA.matcher(string);
        if (!matcher.find()) {
            return Double.NaN;
        }
        String string2 = matcher.group(1);
        String string3 = matcher.group(3) == null ? "" : " " + matcher.group(3);
        int n = Integer.parseInt(matcher.group(2));
        Double d = GTX.get(string2 + " " + n + string3);
        if (d != null) {
            return d;
        }
        if (string2.equals("RTX") && n >= 2000) {
            Double d2 = RTX_GENERATION.get(n / 100);
            Double d3 = RTX_TIER.get(n % 100);
            return d2 == null || d3 == null ? Double.NaN : d2 * d3 * (string3.equals(" TI") ? 1.15 : (string3.equals(" SUPER") ? 1.1 : 1.0));
        }
        return string2.equals("MX") ? 0.18 : (string2.equals("GT") ? 0.06 : 0.12);
    }

    private static double amd(String string) {
        Matcher matcher = RADEON.matcher(string);
        if (matcher.find()) {
            String string2 = matcher.group(1) + (String)(matcher.group(2) == null ? "" : " " + matcher.group(2));
            Double d = RX.getOrDefault(string2, RX.get(matcher.group(1)));
            return d == null ? Double.NaN : (matcher.group(3) != null ? d * 0.8 : d);
        }
        if (!string.contains("RADEON")) {
            return Double.NaN;
        }
        Matcher matcher2 = APU.matcher(string);
        if (matcher2.find() && RADEON_APU.containsKey(matcher2.group(1))) {
            return RADEON_APU.get(matcher2.group(1));
        }
        return string.contains("VEGA 56") ? 0.6 : (string.contains("VEGA 64") ? 0.66 : (GpuScore.integrated(string) ? 0.15 : Double.NaN));
    }

    private static double intel(String string) {
        if (!string.contains("INTEL")) {
            return Double.NaN;
        }
        Matcher matcher = ARC.matcher(string);
        if (matcher.find()) {
            return INTEL_ARC.getOrDefault(matcher.group(1), Double.NaN);
        }
        if (string.contains("ARC")) {
            return 0.3;
        }
        return string.contains("IRIS XE") ? 0.14 : (string.contains("IRIS") ? 0.09 : 0.06);
    }

    private GpuScore() {
    }
}

