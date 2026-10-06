/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.util.Arrays;
import java.util.List;
import viewpoint.light.LightFlood;
import viewpoint.light.LightLayout;
import viewpoint.light.LightLayouts;
import viewpoint.light.LightModel;
import viewpoint.light.LightWork;
import viewpoint.light.SourceRegister;

final class SourceLight {
    static final SourceLight NONE = new SourceLight(0, 0, 0, 0, 0, 0, new int[0]);
    final int x0;
    final int y0;
    final int z0;
    final int width;
    final int height;
    final int levels;
    private final int[] light;

    private SourceLight(int n, int n2, int n3, int n4, int n5, int n6, int[] nArray) {
        this.x0 = n;
        this.y0 = n2;
        this.z0 = n3;
        this.width = n4;
        this.height = n5;
        this.levels = n6;
        this.light = nArray;
    }

    int at(int n, int n2, int n3) {
        int n4 = n - this.x0;
        int n5 = n2 - this.y0;
        int n6 = n3 - this.z0;
        if (n4 < 0 || n5 < 0 || n6 < 0 || n4 >= this.width || n5 >= this.height || n6 >= this.levels) {
            return 0;
        }
        return this.light[(n6 * this.height + n5) * this.width + n4];
    }

    long bytes() {
        return (long)this.light.length * 4L + 64L;
    }

    boolean same(SourceLight sourceLight) {
        return this.x0 == sourceLight.x0 && this.y0 == sourceLight.y0 && this.z0 == sourceLight.z0 && this.width == sourceLight.width && this.height == sourceLight.height && this.levels == sourceLight.levels && Arrays.equals(this.light, sourceLight.light);
    }

    static Box box(SourceRegister.Lamp lamp) {
        int n = lamp.radius() - 1;
        int n2 = LightModel.levelsReached(lamp.radius());
        return new Box(lamp.x() - n, lamp.y() - n, lamp.z() - n2, lamp.x() + n, lamp.y() + n, lamp.z() + n2);
    }

    static Box box(SourceRegister.RoomLight roomLight) {
        int n = 6;
        int n2 = LightModel.levelsReached(7);
        return new Box(roomLight.x() - n, roomLight.y() - n, roomLight.z() - n2, roomLight.x() + roomLight.width() - 1 + n, roomLight.y() + roomLight.height() - 1 + n, roomLight.z() + n2);
    }

    static SourceLight lamp(LightWork lightWork, SourceRegister.Lamp lamp, Box box, List<LightLayouts.Level> list) {
        if (lamp.radius() <= 0) {
            return NONE;
        }
        LightLayout lightLayout = lightWork.layout(box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1(), list);
        int[] nArray = lightWork.starts(1);
        nArray[0] = lightLayout.index(lamp.x(), lamp.y(), lamp.z());
        lightWork.flood.run(nArray, 1, lamp.radius());
        return SourceLight.paint(lightWork, 255.0 * (double)lamp.r(), 255.0 * (double)lamp.g(), 255.0 * (double)lamp.b(), lamp.radius());
    }

    static SourceLight room(LightWork lightWork, SourceRegister.RoomLight roomLight, Box box, List<LightLayouts.Level> list) {
        LightLayout lightLayout = lightWork.layout(box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1(), list);
        int[] nArray = lightWork.starts(roomLight.width() * roomLight.height());
        int n = 0;
        for (int i = roomLight.y(); i < roomLight.y() + roomLight.height(); ++i) {
            for (int j = roomLight.x(); j < roomLight.x() + roomLight.width(); ++j) {
                nArray[n++] = lightLayout.index(j, i, roomLight.z());
            }
        }
        lightWork.flood.run(nArray, n, 7.0);
        return SourceLight.paint(lightWork, 229.0, 216.0, 209.0, 7.0);
    }

    private static SourceLight paint(LightWork lightWork, double d, double d2, double d3, double d4) {
        int n;
        int n2;
        int n3;
        int n4;
        int n5;
        LightLayout lightLayout = lightWork.layout;
        LightFlood lightFlood = lightWork.flood;
        int n6 = Integer.MAX_VALUE;
        int n7 = Integer.MAX_VALUE;
        int n8 = Integer.MAX_VALUE;
        int n9 = Integer.MIN_VALUE;
        int n10 = Integer.MIN_VALUE;
        int n11 = Integer.MIN_VALUE;
        for (n5 = 0; n5 < lightFlood.touchedCount(); ++n5) {
            int n12;
            n4 = lightFlood.touched(n5);
            lightWork.values[n5] = n12 = LightModel.light(lightFlood, n4, d, d2, d3, d4);
            if (n12 == 0) continue;
            n3 = n4 % lightLayout.width;
            n2 = n4 / lightLayout.width % lightLayout.height;
            n = n4 / (lightLayout.width * lightLayout.height);
            n6 = Math.min(n6, n3);
            n7 = Math.min(n7, n2);
            n8 = Math.min(n8, n);
            n9 = Math.max(n9, n3);
            n10 = Math.max(n10, n2);
            n11 = Math.max(n11, n);
        }
        if (n6 > n9) {
            return NONE;
        }
        n5 = n9 - n6 + 1;
        n4 = n10 - n7 + 1;
        int[] nArray = new int[n5 * n4 * (n11 - n8 + 1)];
        for (n3 = 0; n3 < lightFlood.touchedCount(); ++n3) {
            n2 = lightFlood.touched(n3);
            n = n2 % lightLayout.width;
            int n13 = n2 / lightLayout.width % lightLayout.height;
            int n14 = n2 / (lightLayout.width * lightLayout.height);
            if (lightWork.values[n3] == 0) continue;
            nArray[((n14 - n8) * n4 + n13 - n7) * n5 + n - n6] = lightWork.values[n3];
        }
        return new SourceLight(lightLayout.x0 + n6, lightLayout.y0 + n7, lightLayout.z0 + n8, n5, n4, n11 - n8 + 1, nArray);
    }

    record Box(int x0, int y0, int z0, int x1, int y1, int z1) {
    }
}

