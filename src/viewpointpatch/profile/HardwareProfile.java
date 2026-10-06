package viewpointpatch.profile;

import viewpointpatch.gpu.GpuTier;

public final class HardwareProfile {
    public final GpuTier tier;
    public final int cookThreads;
    public final int cookingMax;
    public final int cellJobs;
    public final int shellJobs;
    public final float cellUploadMb;
    public final float shellUploadMb;
    public final float bakeBudgetMs;
    public final int bakePagesPerFrame;
    public final int defaultFarBlocks;
    public final String defaultFloorFilter;

    public HardwareProfile(
            GpuTier tier,
            int cookThreads,
            int cookingMax,
            int cellJobs,
            int shellJobs,
            float cellUploadMb,
            float shellUploadMb,
            float bakeBudgetMs,
            int bakePagesPerFrame,
            int defaultFarBlocks,
            String defaultFloorFilter) {
        this.tier = tier;
        this.cookThreads = cookThreads;
        this.cookingMax = cookingMax;
        this.cellJobs = cellJobs;
        this.shellJobs = shellJobs;
        this.cellUploadMb = cellUploadMb;
        this.shellUploadMb = shellUploadMb;
        this.bakeBudgetMs = bakeBudgetMs;
        this.bakePagesPerFrame = bakePagesPerFrame;
        this.defaultFarBlocks = defaultFarBlocks;
        this.defaultFloorFilter = defaultFloorFilter;
    }

    public static HardwareProfile forTier(GpuTier tier) {
        switch (tier) {
            case LOW_POWER:
                return new HardwareProfile(
                    GpuTier.LOW_POWER,
                    2, 64, 2, 1, 1.0f, 2.0f, 1.0f, 1, 8, "Trilinear"
                );
            case INTEGRATED:
                return new HardwareProfile(
                    GpuTier.INTEGRATED,
                    3, 96, 3, 2, 1.5f, 3.0f, 1.5f, 2, 12, "Trilinear"
                );
            case MAINSTREAM:
                return new HardwareProfile(
                    GpuTier.MAINSTREAM,
                    4, 128, 4, 3, 2.0f, 4.0f, 2.5f, 2, 24, "Trilinear"
                );
            case HIGH_END:
                return new HardwareProfile(
                    GpuTier.HIGH_END,
                    6, 256, 6, 4, 4.0f, 8.0f, 4.0f, 4, 32, "Trilinear"
                );
            case SAFE:
            default:
                return new HardwareProfile(
                    GpuTier.SAFE,
                    2, 64, 2, 1, 1.0f, 2.0f, 1.5f, 1, 12, "Trilinear"
                );
        }
    }
}

