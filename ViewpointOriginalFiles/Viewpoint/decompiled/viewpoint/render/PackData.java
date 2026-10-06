/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.render;

public record PackData(float[] vertices, int[] indices, int artWidth, int artHeight, int width, int height, byte[][] levels) {
    public static final int VERTEX_FLOATS = 8;
    public static final int LEVELS = 4;
    public static final int ALIGN = 8;

    public long bytes() {
        long l = 0L;
        for (byte[] byArray : this.levels) {
            l += (long)byArray.length;
        }
        return (long)this.vertices.length * 4L + (long)this.indices.length * 4L + l;
    }
}

