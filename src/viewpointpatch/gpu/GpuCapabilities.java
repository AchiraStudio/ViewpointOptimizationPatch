package viewpointpatch.gpu;

public final class GpuCapabilities {
    public final GpuVendor vendor;
    public final GpuTier tier;
    public final String rawVendor;
    public final String rawRenderer;
    public final String rawVersion;
    public final int maxTextureSize;
    public final boolean syncSupported;
    public final int vramMb;
    public final boolean isIntegrated;

    public GpuCapabilities(
            GpuVendor vendor,
            GpuTier tier,
            String rawVendor,
            String rawRenderer,
            String rawVersion,
            int maxTextureSize,
            boolean syncSupported,
            int vramMb,
            boolean isIntegrated) {
        this.vendor = vendor;
        this.tier = tier;
        this.rawVendor = rawVendor != null ? rawVendor : "Unknown";
        this.rawRenderer = rawRenderer != null ? rawRenderer : "Unknown";
        this.rawVersion = rawVersion != null ? rawVersion : "Unknown";
        this.maxTextureSize = maxTextureSize;
        this.syncSupported = syncSupported;
        this.vramMb = vramMb;
        this.isIntegrated = isIntegrated;
    }

    @Override
    public String toString() {
        return String.format(
            "GPU[Vendor=%s, Tier=%s, Renderer=\"%s\", GL=\"%s\", MaxTex=%d, Sync=%s, VRAM=%s, Integrated=%s]",
            vendor, tier, rawRenderer, rawVersion, maxTextureSize,
            syncSupported ? "Yes" : "No",
            vramMb > 0 ? (vramMb + "MB") : "Unknown",
            isIntegrated ? "Yes" : "No"
        );
    }
}

