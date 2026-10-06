/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 */
package viewpoint.render;

import java.util.Arrays;
import org.joml.Matrix4f;
import viewpoint.render.ChunkMeshData;
import viewpoint.render.FarGpu;
import viewpoint.render.FloorPage;
import viewpoint.render.ModelDraws;
import zombie.core.textures.TextureID;

public final class SceneData {
    public static final byte VISIBLE = 1;
    public static final byte CASTS_SHADOW = 2;
    public static final byte NEAR = 4;
    public static final byte CASTS_FAR = 8;
    public static final byte CASTS_MID = 16;
    public static final byte RAIN = 64;
    private static final float WHOLE_HI = 2.0f;
    public final Matrix4f projection = new Matrix4f();
    public double originX;
    public double originY;
    public double originZ;
    public float viewerX;
    public float viewerY;
    public float viewerZ;
    public boolean glassFar;
    public float glassStart;
    public float glassOpaque;
    public float glassStoreys;
    public float shadowReach = 128.0f;
    public float fogR;
    public float fogG;
    public float fogB;
    public float fogStart;
    public float fogEnd;
    public float nearReach;
    public FarGpu.CellDraw[] far = new FarGpu.CellDraw[64];
    public int farCount;
    public static final int FAR_MASK_SIZE = 19;
    public final byte[] farMask = new byte[361];
    public int farMaskX;
    public int farMaskY;
    public int farMaskSize;
    public boolean daylightKnown;
    public float daylightR;
    public float daylightG;
    public float daylightB;
    public float skyR;
    public float skyG;
    public float skyB;
    public FarGpu.BlockDraw[] shells = new FarGpu.BlockDraw[64];
    public int shellCount;
    public static final int MAX_SHELL_BLOCKS = 24;
    public static final int SHELL_GRID = 53;
    public static final int SHELL_MASK_BYTES = 8427;
    public final byte[] shellMask = new byte[8427];
    public int shellMaskX;
    public int shellMaskY;
    public float zenithR;
    public float zenithG;
    public float zenithB;
    public float sunX;
    public float sunY = 1.0f;
    public float sunZ;
    public float sunR;
    public float sunG;
    public float sunB;
    public float sunStrength;
    public float skySunX;
    public float skySunY = 1.0f;
    public float skySunZ;
    public float skyGlow;
    public float skyMoonX;
    public float skyMoonY = -1.0f;
    public float skyMoonZ;
    public float cloudCover;
    public float night;
    public float time;
    public float cloudTime;
    public float overcast;
    public float cloudType = 0.5f;
    public float dayFraction;
    public float sunSine;
    public float eyeSky = 1.0f;
    public float nightFraction = -1.0f;
    public float sunTrueX;
    public float sunTrueY = 1.0f;
    public float sunTrueZ;
    public int worldDay;
    public float warmth = 0.6f;
    public float humidity = 0.5f;
    public float thunder;
    public int moonPhase;
    public float mist;
    public float rain;
    public float snow;
    public float windX;
    public float windZ;
    public float puddles;
    public float wetGround;
    public float desaturation;
    public float cloudWindX;
    public float cloudWindZ;
    public static final int ALONG_SIDE = 48;
    public static final float ALONG_STEP = 512.0f;
    public final float[] coverAlong = new float[97];
    public final float[] typeAlong = new float[97];
    public final float[] rainAlong = new float[97];
    public double cloudCarriedX;
    public double cloudCarriedZ;
    public double windCarriedX;
    public double windCarriedZ;
    public float sheets;
    public float gusts;
    public float lightningX;
    public float lightningY;
    public float lightningZ;
    public float lightning;
    public static final int EXPOSURE_SIZE = 64;
    public static final byte EXPOSURE_PORCH = 100;
    public final byte[] exposure = new byte[4096];
    public float exposureAX;
    public float exposureAY;
    public static final byte ROOM_OUTSIDE = 0;
    public static final byte ROOM_NONE = -1;
    public final byte[] rooms = new byte[4096];
    public final byte[] water = new byte[4096];
    public boolean flashOn;
    public float flashStrength;
    public float flashRange;
    public float flashCos;
    public static final int MAX_LIGHTS = 48;
    public static final int LIGHT_STRIDE = 10;
    public final float[] lights = new float[480];
    public final long[] lightKeys = new long[48];
    public int lightCount;
    public int lampShadowCount;
    public static final int MAX_TORCHES = 8;
    public static final int TORCH_STRIDE = 11;
    public final float[] torches = new float[88];
    public int torchCount;
    public static final int MAX_BLOBS = 32;
    public final float[] blobs = new float[128];
    public int blobCount;
    public final ModelDraws models = new ModelDraws();
    public ChunkMeshData[] meshes = new ChunkMeshData[512];
    public float[] meshOffsets = new float[1536];
    public byte[] meshFlags = new byte[512];
    float[] meshDistance = new float[512];
    public float[] meshFade = new float[1024];
    public int meshCount;
    public int[] meshPlan = new int[512];
    public int[] meshPlanLength = new int[512];
    public long[] meshModelSquares = new long[512];
    public FloorPage[] meshFloor = new FloorPage[512];
    public ChunkMeshData[] meshBelow = new ChunkMeshData[512];
    public int[] meshCardFirst = new int[512];
    public int[] meshCardCount = new int[512];
    public float[] cards = new float[1792];
    public TextureID[] cardPages = new TextureID[64];
    public int cardCount;
    public int[] plan = new int[3072];
    public int planEntries;
    public ChunkMeshData targetMesh;
    public int targetOwner;
    public int targetSquare = -1;
    private long[] order = new long[512];
    private ChunkMeshData[] sortedMeshes = new ChunkMeshData[512];
    private float[] sortedOffsets = new float[1536];
    private byte[] sortedFlags = new byte[512];
    private float[] sortedFade = new float[1024];
    private int[] sortedPlan = new int[1024];
    private long[] sortedModelSquares = new long[512];
    private FloorPage[] sortedFloor = new FloorPage[512];
    private ChunkMeshData[] sortedBelow = new ChunkMeshData[512];
    private int[] sortedCards = new int[1024];

    public float nearEnd() {
        return this.nearReach > 0.0f ? Math.min(this.fogEnd, this.nearReach) : this.fogEnd;
    }

    public void blob(float f, float f2, float f3, float f4) {
        int n = this.blobCount;
        if (this.blobCount == 32) {
            n = this.farthestBlob();
            if (f * f + f2 * f2 + f3 * f3 >= this.distance2(n)) {
                return;
            }
        } else {
            ++this.blobCount;
        }
        this.blobs[n * 4] = f;
        this.blobs[n * 4 + 1] = f2;
        this.blobs[n * 4 + 2] = f3;
        this.blobs[n * 4 + 3] = f4;
    }

    private int farthestBlob() {
        int n = 0;
        for (int i = 1; i < this.blobCount; ++i) {
            if (!(this.distance2(i) > this.distance2(n))) continue;
            n = i;
        }
        return n;
    }

    private float distance2(int n) {
        float f = this.blobs[n * 4];
        float f2 = this.blobs[n * 4 + 1];
        float f3 = this.blobs[n * 4 + 2];
        return f * f + f2 * f2 + f3 * f3;
    }

    public void reset() {
        int n;
        this.models.reset();
        Arrays.fill(this.meshes, 0, this.meshCount, null);
        Arrays.fill(this.meshFloor, 0, this.meshCount, null);
        Arrays.fill(this.meshBelow, 0, this.meshCount, null);
        this.meshCount = 0;
        Arrays.fill(this.cardPages, 0, this.cardCount, null);
        this.cardCount = 0;
        this.planEntries = 0;
        this.targetMesh = null;
        this.targetSquare = -1;
        for (n = 0; n < this.farCount; ++n) {
            this.far[n].mesh = null;
            this.far[n].colours = null;
        }
        this.farCount = 0;
        this.farMaskSize = 0;
        for (n = 0; n < this.shellCount; ++n) {
            this.shells[n].shell = null;
            this.shells[n].inside = null;
        }
        this.shellCount = 0;
        Arrays.fill(this.shellMask, (byte)0);
        this.blobCount = 0;
    }

    public void addMesh(ChunkMeshData chunkMeshData, float f, float f2, float f3, byte by, float f4) {
        this.addMesh(chunkMeshData, f, f2, f3, by, f4, 0.0f, 2.0f);
    }

    public void addMesh(ChunkMeshData chunkMeshData, float f, float f2, float f3, byte by, float f4, float f5, float f6) {
        if (this.meshCount == this.meshes.length) {
            this.meshes = Arrays.copyOf(this.meshes, this.meshCount * 2);
            this.meshOffsets = Arrays.copyOf(this.meshOffsets, this.meshCount * 6);
            this.meshFlags = Arrays.copyOf(this.meshFlags, this.meshCount * 2);
            this.meshDistance = Arrays.copyOf(this.meshDistance, this.meshCount * 2);
            this.meshFade = Arrays.copyOf(this.meshFade, this.meshCount * 4);
            this.meshPlan = Arrays.copyOf(this.meshPlan, this.meshCount * 2);
            this.meshPlanLength = Arrays.copyOf(this.meshPlanLength, this.meshCount * 2);
            this.meshModelSquares = Arrays.copyOf(this.meshModelSquares, this.meshCount * 2);
            this.meshFloor = Arrays.copyOf(this.meshFloor, this.meshCount * 2);
            this.meshBelow = Arrays.copyOf(this.meshBelow, this.meshCount * 2);
            this.meshCardFirst = Arrays.copyOf(this.meshCardFirst, this.meshCount * 2);
            this.meshCardCount = Arrays.copyOf(this.meshCardCount, this.meshCount * 2);
        }
        this.meshCardFirst[this.meshCount] = 0;
        this.meshCardCount[this.meshCount] = 0;
        this.meshPlan[this.meshCount] = -1;
        this.meshPlanLength[this.meshCount] = 0;
        this.meshModelSquares[this.meshCount] = -1L;
        this.meshFloor[this.meshCount] = null;
        this.meshBelow[this.meshCount] = null;
        this.meshFade[this.meshCount * 2] = f5;
        this.meshFade[this.meshCount * 2 + 1] = f6;
        this.meshes[this.meshCount] = chunkMeshData;
        this.meshOffsets[this.meshCount * 3] = f;
        this.meshOffsets[this.meshCount * 3 + 1] = f2;
        this.meshOffsets[this.meshCount * 3 + 2] = f3;
        this.meshFlags[this.meshCount] = by;
        this.meshDistance[this.meshCount] = f4;
        ++this.meshCount;
    }

    public void addCard(int n, float[] fArray, int n2, TextureID textureID, float f) {
        if (this.cardCount == this.cardPages.length) {
            this.cards = Arrays.copyOf(this.cards, this.cards.length * 2);
            this.cardPages = Arrays.copyOf(this.cardPages, this.cardCount * 2);
        }
        if (this.meshCardCount[n] == 0) {
            this.meshCardFirst[n] = this.cardCount;
        }
        int n3 = this.cardCount * 28;
        System.arraycopy(fArray, n2, this.cards, n3, 28);
        this.cards[n3 + 24] = f;
        this.cards[n3 + 24 + 1] = 2.0f;
        this.cardPages[this.cardCount++] = textureID;
        int n4 = n;
        this.meshCardCount[n4] = this.meshCardCount[n4] + 1;
    }

    public int addPlanEntry(int n, int n2, int n3) {
        if (this.planEntries * 3 == this.plan.length) {
            this.plan = Arrays.copyOf(this.plan, this.plan.length * 2);
        }
        this.plan[this.planEntries * 3] = n;
        this.plan[this.planEntries * 3 + 1] = n2;
        this.plan[this.planEntries * 3 + 2] = n3;
        ++this.planEntries;
        return 1;
    }

    public void sortFrontToBack() {
        int n;
        if (this.order.length < this.meshCount) {
            this.order = new long[this.meshes.length];
            this.sortedMeshes = new ChunkMeshData[this.meshes.length];
            this.sortedOffsets = new float[this.meshes.length * 3];
            this.sortedFlags = new byte[this.meshes.length];
            this.sortedFade = new float[this.meshes.length * 2];
            this.sortedPlan = new int[this.meshes.length * 2];
            this.sortedModelSquares = new long[this.meshes.length];
            this.sortedFloor = new FloorPage[this.meshes.length];
            this.sortedBelow = new ChunkMeshData[this.meshes.length];
            this.sortedCards = new int[this.meshes.length * 2];
        }
        for (n = 0; n < this.meshCount; ++n) {
            this.order[n] = (long)Float.floatToIntBits(Math.max(0.0f, this.meshDistance[n])) << 32 | (long)n;
        }
        Arrays.sort(this.order, 0, this.meshCount);
        for (n = 0; n < this.meshCount; ++n) {
            int n2 = (int)this.order[n];
            this.sortedMeshes[n] = this.meshes[n2];
            this.sortedOffsets[n * 3] = this.meshOffsets[n2 * 3];
            this.sortedOffsets[n * 3 + 1] = this.meshOffsets[n2 * 3 + 1];
            this.sortedOffsets[n * 3 + 2] = this.meshOffsets[n2 * 3 + 2];
            this.sortedFlags[n] = this.meshFlags[n2];
            this.sortedFade[n * 2] = this.meshFade[n2 * 2];
            this.sortedFade[n * 2 + 1] = this.meshFade[n2 * 2 + 1];
            this.sortedPlan[n * 2] = this.meshPlan[n2];
            this.sortedPlan[n * 2 + 1] = this.meshPlanLength[n2];
            this.sortedModelSquares[n] = this.meshModelSquares[n2];
            this.sortedFloor[n] = this.meshFloor[n2];
            this.sortedBelow[n] = this.meshBelow[n2];
            this.sortedCards[n * 2] = this.meshCardFirst[n2];
            this.sortedCards[n * 2 + 1] = this.meshCardCount[n2];
        }
        System.arraycopy(this.sortedMeshes, 0, this.meshes, 0, this.meshCount);
        System.arraycopy(this.sortedOffsets, 0, this.meshOffsets, 0, this.meshCount * 3);
        System.arraycopy(this.sortedFlags, 0, this.meshFlags, 0, this.meshCount);
        System.arraycopy(this.sortedFade, 0, this.meshFade, 0, this.meshCount * 2);
        System.arraycopy(this.sortedModelSquares, 0, this.meshModelSquares, 0, this.meshCount);
        System.arraycopy(this.sortedFloor, 0, this.meshFloor, 0, this.meshCount);
        System.arraycopy(this.sortedBelow, 0, this.meshBelow, 0, this.meshCount);
        for (n = 0; n < this.meshCount; ++n) {
            this.meshPlan[n] = this.sortedPlan[n * 2];
            this.meshPlanLength[n] = this.sortedPlan[n * 2 + 1];
            this.meshCardFirst[n] = this.sortedCards[n * 2];
            this.meshCardCount[n] = this.sortedCards[n * 2 + 1];
        }
        Arrays.fill(this.sortedMeshes, 0, this.meshCount, null);
        Arrays.fill(this.sortedFloor, 0, this.meshCount, null);
        Arrays.fill(this.sortedBelow, 0, this.meshCount, null);
    }
}

