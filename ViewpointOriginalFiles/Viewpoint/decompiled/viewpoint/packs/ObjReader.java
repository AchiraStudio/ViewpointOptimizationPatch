/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.packs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

final class ObjReader {
    private static final int KEY_BITS = 21;
    private static final float CORNER_SLACK = 0.02f;
    private static final float CORNER_FILL = 0.7f;

    ObjReader() {
    }

    static Mesh parse(List<String> list) {
        Floats floats = new Floats();
        Floats floats2 = new Floats();
        Floats floats3 = new Floats();
        Floats floats4 = new Floats();
        int[] nArray = new int[64];
        int n = 0;
        HashMap<Long, Integer> hashMap = new HashMap<Long, Integer>();
        String string = null;
        for (int i = 0; i < list.size(); ++i) {
            String string2 = list.get(i).strip();
            if (string2.startsWith("v ")) {
                floats.add(ObjReader.numbers(string2, 3));
                continue;
            }
            if (string2.startsWith("vt ")) {
                floats2.add(ObjReader.numbers(string2, 2));
                continue;
            }
            if (string2.startsWith("vn ")) {
                floats3.add(ObjReader.numbers(string2, 3));
                continue;
            }
            if (string2.startsWith("mtllib ")) {
                string = string2.substring(7).strip();
                continue;
            }
            if (!string2.startsWith("f ")) continue;
            int[] nArray2 = ObjReader.face(string2, i, floats, floats2, floats3, floats4, hashMap);
            if (n + (nArray2.length - 2) * 3 > nArray.length) {
                nArray = Arrays.copyOf(nArray, Math.max(nArray.length * 2, n + (nArray2.length - 2) * 3));
            }
            int n2 = 1;
            while (n2 + 1 < nArray2.length) {
                nArray[n++] = nArray2[0];
                nArray[n++] = nArray2[n2 + 1];
                nArray[n++] = nArray2[n2];
                ++n2;
            }
        }
        float[] fArray = floats4.toArray();
        ObjReader.centreCornerLaid(fArray);
        return new Mesh(fArray, Arrays.copyOf(nArray, n), string);
    }

    static void centreCornerLaid(float[] fArray) {
        int n;
        float f = Float.MAX_VALUE;
        float f2 = -3.4028235E38f;
        float f3 = Float.MAX_VALUE;
        float f4 = -3.4028235E38f;
        for (n = 0; n < fArray.length; n += 8) {
            f = Math.min(f, fArray[n]);
            f2 = Math.max(f2, fArray[n]);
            f3 = Math.min(f3, fArray[n + 2]);
            f4 = Math.max(f4, fArray[n + 2]);
        }
        n = f2 <= 0.02f && f4 <= 0.02f && f >= -1.02f && f3 >= -1.02f && f2 - f > 0.7f && f4 - f3 > 0.7f ? 1 : 0;
        for (int i = 0; n != 0 && i < fArray.length; i += 8) {
            int n2 = i;
            fArray[n2] = fArray[n2] + 0.5f;
            int n3 = i + 2;
            fArray[n3] = fArray[n3] + 0.5f;
        }
    }

    static String texture(List<String> list) {
        for (String string : list) {
            String string2 = string.strip();
            if (!string2.startsWith("map_Kd ")) continue;
            return string2.substring(7).strip();
        }
        return null;
    }

    private static int[] face(String string, int n, Floats floats, Floats floats2, Floats floats3, Floats floats4, HashMap<Long, Integer> hashMap) {
        String[] stringArray = string.substring(2).strip().split("\\s+");
        if (stringArray.length < 3) {
            throw new IllegalArgumentException("line " + (n + 1) + ": a face of fewer than three corners");
        }
        int[] nArray = new int[stringArray.length];
        int[] nArray2 = new int[stringArray.length];
        int[] nArray3 = new int[stringArray.length];
        for (int i = 0; i < stringArray.length; ++i) {
            String[] stringArray2 = stringArray[i].split("/", -1);
            nArray[i] = ObjReader.index(stringArray2[0], floats.size() / 3, n);
            nArray2[i] = stringArray2.length > 1 && !stringArray2[1].isEmpty() ? ObjReader.index(stringArray2[1], floats2.size() / 2, n) : -1;
            nArray3[i] = stringArray2.length > 2 && !stringArray2[2].isEmpty() ? ObjReader.index(stringArray2[2], floats3.size() / 3, n) : -1;
        }
        float[] fArray = null;
        for (int i = 0; i < stringArray.length && fArray == null; ++i) {
            fArray = nArray3[i] < 0 ? ObjReader.flatNormal(floats, nArray) : null;
        }
        int[] nArray4 = new int[stringArray.length];
        for (int i = 0; i < stringArray.length; ++i) {
            Integer n2;
            long l = fArray != null ? -1L : ((long)nArray[i] << 21 | (long)nArray2[i] + 1L) << 21 | (long)nArray3[i] + 1L;
            Integer n3 = n2 = l < 0L ? null : hashMap.get(l);
            if (n2 == null) {
                n2 = floats4.size() / 8;
                ObjReader.corner(floats, floats2, floats3, floats4, nArray[i], nArray2[i], nArray3[i], fArray);
                if (l >= 0L) {
                    hashMap.put(l, n2);
                }
            }
            nArray4[i] = n2;
        }
        return nArray4;
    }

    private static void corner(Floats floats, Floats floats2, Floats floats3, Floats floats4, int n, int n2, int n3, float[] fArray) {
        float[] fArray2 = floats.data;
        float[] fArray3 = floats2.data;
        float[] fArray4 = floats3.data;
        float f = fArray != null ? fArray[0] : fArray4[n3 * 3];
        float f2 = fArray != null ? fArray[1] : fArray4[n3 * 3 + 1];
        float f3 = fArray != null ? fArray[2] : fArray4[n3 * 3 + 2];
        floats4.add(-fArray2[n * 3], fArray2[n * 3 + 2], -fArray2[n * 3 + 1]);
        floats4.add(-f, f3, -f2);
        floats4.add(n2 < 0 ? 0.0f : fArray3[n2 * 2], n2 < 0 ? 0.0f : 1.0f - fArray3[n2 * 2 + 1]);
    }

    private static float[] flatNormal(Floats floats, int[] nArray) {
        float[] fArray;
        float[] fArray2 = floats.data;
        float f = fArray2[nArray[1] * 3 + 1] - fArray2[nArray[0] * 3 + 1];
        float f2 = fArray2[nArray[2] * 3 + 2] - fArray2[nArray[0] * 3 + 2];
        float f3 = fArray2[nArray[1] * 3 + 2] - fArray2[nArray[0] * 3 + 2];
        float f4 = fArray2[nArray[2] * 3 + 1] - fArray2[nArray[0] * 3 + 1];
        float f5 = f * f2 - f3 * f4;
        float f6 = fArray2[nArray[2] * 3] - fArray2[nArray[0] * 3];
        float f7 = fArray2[nArray[1] * 3] - fArray2[nArray[0] * 3];
        float f8 = f3 * f6 - f7 * f2;
        float f9 = f7 * f4 - f * f6;
        float f10 = (float)Math.sqrt(f5 * f5 + f8 * f8 + f9 * f9);
        if (f10 > 0.0f) {
            float[] fArray3 = new float[3];
            fArray3[0] = f5 / f10;
            fArray3[1] = f8 / f10;
            fArray = fArray3;
            fArray3[2] = f9 / f10;
        } else {
            float[] fArray4 = new float[3];
            fArray4[0] = 0.0f;
            fArray4[1] = 0.0f;
            fArray = fArray4;
            fArray4[2] = 1.0f;
        }
        return fArray;
    }

    private static int index(String string, int n, int n2) {
        int n3;
        int n4;
        try {
            n4 = Integer.parseInt(string);
        }
        catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("line " + (n2 + 1) + ": not an index: " + string);
        }
        int n5 = n3 = n4 < 0 ? n + n4 : n4 - 1;
        if (n3 < 0 || n3 >= n) {
            throw new IllegalArgumentException("line " + (n2 + 1) + ": no element " + string);
        }
        return n3;
    }

    private static float[] numbers(String string, int n) {
        String[] stringArray = string.split("\\s+");
        float[] fArray = new float[n];
        for (int i = 0; i < n && i + 1 < stringArray.length; ++i) {
            fArray[i] = Float.parseFloat(stringArray[i + 1]);
        }
        return fArray;
    }

    private static final class Floats {
        float[] data = new float[256];
        int size;

        private Floats() {
        }

        void add(float ... fArray) {
            if (this.size + fArray.length > this.data.length) {
                this.data = Arrays.copyOf(this.data, Math.max(this.data.length * 2, this.size + fArray.length));
            }
            System.arraycopy(fArray, 0, this.data, this.size, fArray.length);
            this.size += fArray.length;
        }

        int size() {
            return this.size;
        }

        float[] toArray() {
            return Arrays.copyOf(this.data, this.size);
        }
    }

    record Mesh(float[] vertices, int[] indices, String mtllib) {
    }
}

