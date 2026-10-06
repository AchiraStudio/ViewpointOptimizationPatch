package viewpointpatch.gpu;

public enum GpuVendor {
    NVIDIA,
    AMD,
    INTEL,
    APPLE,
    SOFTWARE,
    UNKNOWN;

    public static GpuVendor fromStrings(String vendor, String renderer) {
        String v = (vendor != null ? vendor.toUpperCase() : "");
        String r = (renderer != null ? renderer.toUpperCase() : "");

        if (r.contains("LLVMPIPE") || r.contains("SOFTWARE") || r.contains("MICROSOFT BASIC RENDER")) {
            return SOFTWARE;
        }
        if (v.contains("NVIDIA")) {
            return NVIDIA;
        }
        if (v.contains("AMD") || v.contains("ATI") || v.contains("ADVANCED MICRO DEVICES")) {
            return AMD;
        }
        if (v.contains("INTEL")) {
            return INTEL;
        }
        if (v.contains("APPLE")) {
            return APPLE;
        }
        return UNKNOWN;
    }
}

