package viewpointpatch.gpu;

public enum GpuTier {
    SAFE("Safe (Engine Defaults / Unknown Hardware)"),
    LOW_POWER("Low-Power Integrated / Legacy GPU"),
    INTEGRATED("Modern Integrated APU / Iris Xe / Mobile Arc"),
    MAINSTREAM("Mainstream Dedicated GPU"),
    HIGH_END("High-End Enthusiast GPU");

    private final String description;

    GpuTier(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

