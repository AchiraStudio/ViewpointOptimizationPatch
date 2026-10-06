/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL33
 */
package viewpoint.platform;

import java.util.Arrays;
import java.util.function.Supplier;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import viewpoint.platform.Capture;
import viewpoint.platform.GpuParts;
import viewpoint.platform.Hitches;
import viewpoint.platform.Pool;
import viewpoint.platform.ProcessMemory;
import viewpoint.platform.VideoMemory;

public final class Profile {
    public static final int START = 0;
    public static final int FLOORS = 1;
    public static final int SHADOW = 2;
    public static final int GBUFFER = 3;
    public static final int LIGHTING = 4;
    public static final int FAR = 5;
    public static final int OUTLINES = 6;
    public static final int TRANSLUCENT = 7;
    public static final int INDIRECT = 8;
    public static final int WEATHER = 9;
    public static final int VOLUME = 10;
    public static final int BLOOM = 11;
    public static final int POST = 12;
    private static final String[] NAMES = new String[]{"floors", "shadow", "gbuffer", "sky+light", "far", "outlines", "glass", "bounce", "weather", "air", "bloom", "post+taa"};
    private static final int SLOTS = 4;
    private static final int MARKS = 13;
    private static final long LOG_PERIOD_NANOS = 10000000000L;
    private static int[][] queries;
    private static final int[] issued;
    private static int slot;
    private static final double[] gpuMs;
    private static final double[] cpuMs;
    private static final double[] recentGpuMs;
    private static final double[] recentCpuMs;
    private static final double RECENT = 0.02;
    private static final double[] frameCpuMs;
    private static final long[] cpuAt;
    private static int gpuFrames;
    private static int frames;
    private static long lastLog;
    public static int draws;
    public static int meshDraws;
    public static int ownedModels;
    public static int modelRuns;
    public static int paletteBones;
    public static int modelTexturesMade;
    public static int modelTexturesWaiting;
    public static long modelTriangles;
    public static long paletteBytes;
    private static double drawSum;
    private static double meshDrawSum;
    public static volatile int farDrawn;
    public static volatile int farInView;
    public static volatile int shellsInView;
    public static volatile int farTrees;
    public static volatile int farLampCards;
    public static volatile long bandVertices;
    public static volatile long shellVerticesDrawn;
    public static volatile Supplier<String> arenaUsage;
    public static volatile Supplier<String> roomsReport;
    public static volatile Supplier<String> floorsReport;
    public static volatile Supplier<String> shellGroundReport;
    public static volatile Supplier<String> lightSourcesReport;
    public static volatile Supplier<String> ownedLightReport;
    public static volatile Supplier<String> lampShadowsReport;
    public static volatile Supplier<String> farMapsReport;
    public static volatile Supplier<String> cascadeLayersReport;
    public static volatile Supplier<String> packsReport;
    public static volatile Supplier<String> corpsesReport;
    public static volatile Supplier<String> latencyReport;
    private static double cacheMs;
    private static double farMs;
    private static double snapshotMs;
    private static double rebuildSum;
    private static double meshSum;
    private static double buildSum;
    private static double buildMs;
    private static double waitingSum;
    static volatile double lastRenderMs;
    static volatile long lastFrameAt;
    public static volatile int shellVertices;
    public static volatile int shellUploads;
    private static long lastMainFrame;
    private static volatile double mainFrameSum;
    private static volatile double zombieSum;
    private static volatile double askedSum;
    private static volatile double offeredSum;
    private static volatile double freshSum;
    private static volatile double keptSum;
    private static volatile double posedSum;
    private static volatile int mainFrames;
    static volatile double lastMainMs;
    static volatile double lastMainOursMs;
    static volatile long cacheNanos;
    static volatile long farNanos;
    static volatile long snapshotNanos;
    public static volatile String note;
    public static volatile int rebuilds;
    public static volatile int meshes;
    public static volatile int cooking;
    private static volatile int chunkBuilds;
    private static volatile int chunkWaiting;
    private static volatile long chunkBuildNanos;
    private static final int[] reasons;
    private static volatile int unchangedSum;
    private static volatile int signalSum;
    private static volatile int overflowSum;
    public static volatile double recentFarMainMs;

    public static void mark(int n) {
        if (queries == null) {
            for (int[] nArray : queries = new int[4][13]) {
                GL15.glGenQueries((int[])nArray);
            }
        }
        GL33.glQueryCounter((int)queries[slot][n], (int)36392);
        int n2 = slot;
        issued[n2] = issued[n2] | 1 << n;
        long l = System.nanoTime();
        if (n > 0 && cpuAt[n - 1] != 0L) {
            double d = (double)(l - cpuAt[n - 1]) * 1.0E-6;
            int n3 = n - 1;
            cpuMs[n3] = cpuMs[n3] + d;
            int n4 = n - 1;
            frameCpuMs[n4] = frameCpuMs[n4] + d;
            int n5 = n - 1;
            recentCpuMs[n5] = recentCpuMs[n5] + (d - recentCpuMs[n - 1]) * 0.02;
        }
        Profile.cpuAt[n] = l;
    }

    public static void endFrame() {
        long l;
        int n = draws;
        ++frames;
        cacheMs += (double)cacheNanos * 1.0E-6;
        farMs += (double)farNanos * 1.0E-6;
        snapshotMs += (double)snapshotNanos * 1.0E-6;
        rebuildSum += (double)rebuilds;
        meshSum += (double)meshes;
        buildSum += (double)chunkBuilds;
        buildMs += (double)chunkBuildNanos * 1.0E-6;
        waitingSum += (double)chunkWaiting;
        drawSum += (double)draws;
        draws = 0;
        meshDrawSum += (double)meshDraws;
        meshDraws = 0;
        rebuilds = 0;
        Arrays.fill(cpuAt, 0L);
        GpuParts.frameEnded();
        slot = (slot + 1) % 4;
        if (issued[slot] == 8191 && GL15.glGetQueryObjecti((int)queries[slot][12], (int)34919) != 0) {
            l = GL33.glGetQueryObjectui64((int)queries[slot][0], (int)34918);
            for (int i = 1; i < 13; ++i) {
                long l2 = GL33.glGetQueryObjectui64((int)queries[slot][i], (int)34918);
                int n2 = i - 1;
                gpuMs[n2] = gpuMs[n2] + (double)(l2 - l) * 1.0E-6;
                int n3 = i - 1;
                recentGpuMs[n3] = recentGpuMs[n3] + ((double)(l2 - l) * 1.0E-6 - recentGpuMs[i - 1]) * 0.02;
                l = l2;
            }
            ++gpuFrames;
        }
        Profile.issued[Profile.slot] = 0;
        l = System.nanoTime();
        Hitches.render(l, frameCpuMs, NAMES);
        double d = 0.0;
        for (double d2 : frameCpuMs) {
            d += d2;
        }
        lastRenderMs = d;
        lastFrameAt = l;
        Capture.renderFrame(l, frameCpuMs, n);
        Arrays.fill(frameCpuMs, 0.0);
        ProcessMemory.sample(l);
        if (l - lastLog >= 10000000000L) {
            Profile.log(l - lastLog);
            lastLog = l;
        }
    }

    private static void log(long l) {
        double d;
        int n;
        StringBuilder stringBuilder = new StringBuilder(String.format("[Viewpoint] %.0f fps | gpu ms", (double)frames / ((double)l * 1.0E-9)));
        double d2 = 0.0;
        double d3 = 0.0;
        for (n = 0; n < gpuMs.length; ++n) {
            d = gpuFrames == 0 ? 0.0 : gpuMs[n] / (double)gpuFrames;
            d2 += d;
            stringBuilder.append(String.format(" %s %.2f", NAMES[n], d));
            Profile.gpuMs[n] = 0.0;
        }
        stringBuilder.append(String.format(" (%.2f)", d2)).append(GpuParts.report()).append(" | render cpu ms");
        for (n = 0; n < cpuMs.length; ++n) {
            d = cpuMs[n] / (double)Math.max(frames, 1);
            d3 += d;
            stringBuilder.append(String.format(" %s %.2f", NAMES[n], d));
            Profile.cpuMs[n] = 0.0;
        }
        stringBuilder.append(String.format(" (%.2f) | draws/frame %.0f (mesh draws in them %.0f; arena %s)", d3, drawSum / (double)Math.max(frames, 1), meshDrawSum / (double)Math.max(frames, 1), arenaUsage.get()));
        Profile.logRenderer(stringBuilder);
        n = Math.max(mainFrames, 1);
        stringBuilder.append(String.format(" | main thread frame %.2f ms (%.0f fps) | zombies loaded %.0f, asked %.0f, given a model %.0f; characters drawn from fresh snapshots %.0f, kept %.0f, kept in a new pose %.0f", mainFrameSum / (double)n, mainFrameSum > 0.0 ? 1000.0 * (double)n / mainFrameSum : 0.0, zombieSum / (double)n, askedSum / (double)n, offeredSum / (double)n, freshSum / (double)n, keptSum / (double)n, posedSum / (double)n));
        posedSum = 0.0;
        keptSum = 0.0;
        freshSum = 0.0;
        offeredSum = 0.0;
        askedSum = 0.0;
        zombieSum = 0.0;
        mainFrameSum = 0.0;
        mainFrames = 0;
        stringBuilder.append(" | latency ").append(latencyReport.get());
        stringBuilder.append(String.format(" | main ms cache %.2f far %.2f snapshot %.2f | rebuilds/frame %.1f meshes %.0f", cacheMs / (double)frames, farMs / (double)frames, snapshotMs / (double)frames, rebuildSum / (double)frames, meshSum / (double)frames));
        Profile.logChunks(stringBuilder);
        stringBuilder.append(" | rooms ").append(roomsReport.get());
        stringBuilder.append(" | floors ").append(floorsReport.get());
        stringBuilder.append(" | shell ground ").append(shellGroundReport.get());
        stringBuilder.append(" | model packs ").append(packsReport.get());
        stringBuilder.append(" | corpses ").append(corpsesReport.get());
        Profile.logMemory(stringBuilder);
        stringBuilder.append(Hitches.report());
        System.out.println(stringBuilder.append(" | ").append(note));
        gpuFrames = 0;
        frames = 0;
        meshSum = 0.0;
        rebuildSum = 0.0;
        snapshotMs = 0.0;
        farMs = 0.0;
        cacheMs = 0.0;
        meshDrawSum = 0.0;
        drawSum = 0.0;
        waitingSum = 0.0;
        buildMs = 0.0;
        buildSum = 0.0;
    }

    private static void logRenderer(StringBuilder stringBuilder) {
        stringBuilder.append(String.format(" | owned model instances drawn %d (%d multi-draws, %.2fM triangles; palettes %d bones, %.1f MB; textures made %d, waiting %d)", ownedModels, modelRuns, (double)modelTriangles * 1.0E-6, paletteBones, (double)paletteBytes * 1.0E-6, modelTexturesMade, modelTexturesWaiting));
        stringBuilder.append(" | lamp shadows ").append(lampShadowsReport.get());
        stringBuilder.append(" | far maps ").append(farMapsReport.get());
        stringBuilder.append(" | cascade layers ").append(cascadeLayersReport.get());
        stringBuilder.append(String.format(" | far cells in view %d of %d (%.0fk tree cards, %d lamp cards), shell blocks in view %d (%.1fM vertices held; drawn %.1fM in the band, %.1fM past it)", farInView, farDrawn, (double)farTrees / 1000.0, farLampCards, shellsInView, (double)shellVertices / 1000000.0, (double)bandVertices / 1000000.0, (double)shellVerticesDrawn / 1000000.0));
    }

    private static void logMemory(StringBuilder stringBuilder) {
        stringBuilder.append(Pool.report());
        stringBuilder.append(ProcessMemory.report());
        stringBuilder.append(VideoMemory.report());
    }

    private static void logChunks(StringBuilder stringBuilder) {
        stringBuilder.append(String.format(" | builds/frame %.1f (%.2f ms each on the main thread) cooking %d waiting in view %.0f", buildSum / (double)frames, buildSum == 0.0 ? 0.0 : buildMs / buildSum, cooking, waitingSum / (double)frames));
        stringBuilder.append(String.format(" | builds by cause: new %d, game %d, retry %d, model ring %d, model packs %d; unchanged, not cooked %d", reasons[0], reasons[1], reasons[2], reasons[3], reasons[4], unchangedSum));
        stringBuilder.append(String.format(" | signals %d, overflows %d", signalSum, overflowSum));
        stringBuilder.append(" | light sources ").append(lightSourcesReport.get());
        stringBuilder.append(" | owned light ").append(ownedLightReport.get());
        Arrays.fill(reasons, 0);
        overflowSum = 0;
        signalSum = 0;
        unchangedSum = 0;
    }

    public static void mainFrame(long l) {
        if (lastMainFrame != 0L && l - lastMainFrame < 1000000000L) {
            double d = (double)(l - lastMainFrame) * 1.0E-6;
            mainFrameSum += d;
            ++mainFrames;
            lastMainMs = d;
            lastMainOursMs = (double)(cacheNanos + farNanos + snapshotNanos) * 1.0E-6;
            Capture.mainFrame(d, cacheNanos, farNanos, snapshotNanos, chunkBuilds);
            if (d >= 50.0) {
                Hitches.main(d, (double)cacheNanos * 1.0E-6, (double)farNanos * 1.0E-6, (double)snapshotNanos * 1.0E-6, chunkBuilds);
            }
        }
        lastMainFrame = l;
    }

    public static void zombies(int n, int n2, int n3) {
        zombieSum += (double)n;
        askedSum += (double)n2;
        offeredSum += (double)n3;
    }

    public static void snapshots(int n, int n2, int n3) {
        freshSum += (double)n;
        keptSum += (double)n2;
        posedSum += (double)n3;
    }

    public static double recentGpuMs(int n) {
        return recentGpuMs[n - 1];
    }

    public static double recentCpuMs(int n) {
        return recentCpuMs[n - 1];
    }

    public static void cpu(long l, long l2, long l3, int n) {
        cacheNanos = l;
        farNanos = l2;
        recentFarMainMs += ((double)l2 * 1.0E-6 - recentFarMainMs) * 0.02;
        snapshotNanos = l3;
        meshes = n;
    }

    public static void rebuildReason(int n) {
        int n2 = n;
        reasons[n2] = reasons[n2] + 1;
    }

    public static void chunkSignals(int n, boolean bl, int n2) {
        signalSum += n;
        overflowSum += bl ? 1 : 0;
        unchangedSum += n2;
    }

    public static void chunks(int n, long l, int n2) {
        chunkBuilds = n;
        chunkBuildNanos = l;
        chunkWaiting = n2;
    }

    private Profile() {
    }

    static {
        issued = new int[4];
        gpuMs = new double[12];
        cpuMs = new double[12];
        recentGpuMs = new double[12];
        recentCpuMs = new double[12];
        frameCpuMs = new double[12];
        cpuAt = new long[13];
        lastLog = System.nanoTime();
        arenaUsage = () -> "";
        roomsReport = () -> "off";
        floorsReport = () -> "none";
        shellGroundReport = () -> "none";
        lightSourcesReport = () -> "none";
        ownedLightReport = () -> "none";
        lampShadowsReport = () -> "off";
        farMapsReport = () -> "off";
        cascadeLayersReport = () -> "off";
        packsReport = () -> "none";
        corpsesReport = () -> "none";
        latencyReport = () -> "off";
        note = "";
        reasons = new int[5];
    }
}

