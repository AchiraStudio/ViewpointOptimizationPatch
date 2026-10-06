/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.textures.Texture
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.sprite.IsoSprite
 *  zombie.pathfind.VehiclePoly
 *  zombie.vehicles.BaseVehicle
 */
package viewpoint.interact;

import java.util.Arrays;
import viewpoint.interact.PackBoxes;
import viewpoint.visibility.Edges;
import viewpoint.world.TileMesh;
import viewpoint.world.TileMeshes;
import viewpoint.world.WorldMesher;
import zombie.core.textures.Texture;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.pathfind.VehiclePoly;
import zombie.vehicles.BaseVehicle;

final class LootBoxes {
    private static final float THIN = 0.1f;
    private static final float PLAIN_TALL = 0.81649655f;
    private static final float VEHICLE_TALL = 1.0f;
    private static final float ANIMAL_LONG = 2.0f;
    private static final float ANIMAL_TALL = 3.0f;
    private static final float[] packBox = new float[6];
    private static float[] boxes = new float[144];

    static float[] all() {
        return boxes;
    }

    static void empty(int n) {
        if (boxes.length < (n + 1) * 9) {
            boxes = Arrays.copyOf(boxes, Math.max(boxes.length * 2, (n + 1) * 9));
        }
        int n2 = n * 9;
        for (int i = 0; i < 3; ++i) {
            LootBoxes.boxes[n2 + i] = Float.MAX_VALUE;
            LootBoxes.boxes[n2 + 3 + i] = -3.4028235E38f;
        }
        LootBoxes.boxes[n2 + 6] = 1.0f;
        LootBoxes.boxes[n2 + 7] = 0.0f;
        LootBoxes.boxes[n2 + 8] = -1.0f;
    }

    static void grow(int n, float f, float f2, float f3, float f4, float f5, float f6) {
        int n2 = n * 9;
        LootBoxes.boxes[n2] = Math.min(boxes[n2], f);
        LootBoxes.boxes[n2 + 1] = Math.min(boxes[n2 + 1], f2);
        LootBoxes.boxes[n2 + 2] = Math.min(boxes[n2 + 2], f3);
        LootBoxes.boxes[n2 + 3] = Math.max(boxes[n2 + 3], f4);
        LootBoxes.boxes[n2 + 4] = Math.max(boxes[n2 + 4], f5);
        LootBoxes.boxes[n2 + 5] = Math.max(boxes[n2 + 5], f6);
    }

    static void shape(int n, IsoObject isoObject, IsoGridSquare isoGridSquare) {
        int n2;
        int n3;
        IsoSprite isoSprite = isoObject.getSprite();
        if (PackBoxes.of(isoObject, isoGridSquare, packBox)) {
            LootBoxes.grow(n, packBox[0], packBox[1], packBox[2], packBox[3], packBox[4], packBox[5]);
            LootBoxes.thicken(n);
            return;
        }
        Texture texture = isoSprite == null ? null : isoSprite.getTextureForCurrentFrame(isoObject.getDir(), isoObject);
        TileMesh tileMesh = texture == null ? null : TileMeshes.get(isoSprite, texture, null);
        float f = (float)isoGridSquare.x + 0.5f;
        float f2 = (float)isoGridSquare.y + 0.5f;
        float f3 = (float)isoGridSquare.z * 2.4494896f + WorldMesher.rise(isoObject);
        int n4 = isoSprite == null ? 0 : Edges.ownerKind(isoSprite);
        boolean bl = tileMesh != null && tileMesh.vertCount > 0 && !WorldMesher.hasModel(isoObject);
        int n5 = n * 9;
        if (!bl && n4 == 1) {
            LootBoxes.grow(n, isoGridSquare.x, (float)isoGridSquare.y - 0.1f, f3, (float)isoGridSquare.x + 1.0f, (float)isoGridSquare.y + 0.1f, f3 + 2.4494896f);
            LootBoxes.boxes[n5 + 8] = 1.0f;
            return;
        }
        if (!bl && n4 == 2) {
            LootBoxes.grow(n, (float)isoGridSquare.x - 0.1f, isoGridSquare.y, f3, (float)isoGridSquare.x + 0.1f, (float)isoGridSquare.y + 1.0f, f3 + 2.4494896f);
            LootBoxes.boxes[n5 + 8] = 0.0f;
            return;
        }
        if (tileMesh == null || tileMesh.vertCount == 0) {
            LootBoxes.grow(n, f - 0.5f, f2 - 0.5f, f3, f + 0.5f, f2 + 0.5f, f3 + 0.81649655f);
            return;
        }
        float[] fArray = tileMesh.data;
        for (n3 = 0; n3 < fArray.length; n3 += 8) {
            LootBoxes.grow(n, f + fArray[n3], f2 + fArray[n3 + 2], f3 + fArray[n3 + 1], f + fArray[n3], f2 + fArray[n3 + 2], f3 + fArray[n3 + 1]);
        }
        n3 = boxes[n5 + 3] - boxes[n5] < 0.2f ? 1 : 0;
        int n6 = n2 = boxes[n5 + 4] - boxes[n5 + 1] < 0.2f ? 1 : 0;
        LootBoxes.boxes[n5 + 8] = n3 == n2 ? -1.0f : (n3 != 0 ? 0.0f : 1.0f);
        LootBoxes.thicken(n);
    }

    static void footprint(int n, BaseVehicle baseVehicle) {
        int n2;
        VehiclePoly vehiclePoly = baseVehicle.getPoly();
        float[] fArray = new float[]{vehiclePoly.x1, vehiclePoly.x2, vehiclePoly.x3, vehiclePoly.x4};
        float[] fArray2 = new float[]{vehiclePoly.y1, vehiclePoly.y2, vehiclePoly.y3, vehiclePoly.y4};
        int n3 = 1;
        for (n2 = 2; n2 < 4; ++n2) {
            n3 = LootBoxes.distance(fArray, fArray2, n2) > LootBoxes.distance(fArray, fArray2, n3) ? n2 : n3;
        }
        n2 = n3 == 1 ? 2 : 1;
        int n4 = n3 == 3 ? 2 : 3;
        float f = (float)Math.sqrt(LootBoxes.distance(fArray, fArray2, n2));
        float f2 = (float)Math.sqrt(LootBoxes.distance(fArray, fArray2, n4));
        float f3 = (fArray[0] + fArray[n3]) * 0.5f;
        float f4 = (fArray2[0] + fArray2[n3]) * 0.5f;
        float f5 = (float)Math.atan2(fArray2[n2] - fArray2[0], fArray[n2] - fArray[0]);
        float f6 = baseVehicle.getZ() * 2.4494896f;
        float f7 = baseVehicle.getScript() == null ? 1.0f : Math.max(1.0f, baseVehicle.getScript().getExtents().y);
        LootBoxes.grow(n, f3 - f * 0.5f, f4 - f2 * 0.5f, f6, f3 + f * 0.5f, f4 + f2 * 0.5f, f6 + f7);
        LootBoxes.boxes[n * 9 + 6] = (float)Math.cos(f5);
        LootBoxes.boxes[n * 9 + 7] = (float)Math.sin(f5);
    }

    static void animal(float[] fArray, int n, float f, float f2, float f3, float f4, float f5, float f6) {
        int n2 = n * 9;
        float f7 = 2.0f * f4;
        fArray[n2] = f - f7;
        fArray[n2 + 1] = f2 - f4;
        fArray[n2 + 2] = f3;
        fArray[n2 + 3] = f + f7;
        fArray[n2 + 4] = f2 + f4;
        fArray[n2 + 5] = f3 + 3.0f * f4;
        fArray[n2 + 6] = f5;
        fArray[n2 + 7] = f6;
        fArray[n2 + 8] = -1.0f;
    }

    static void animal(int n, float f, float f2, float f3, float f4, float f5, float f6) {
        LootBoxes.animal(boxes, n, f, f2, f3, f4, f5, f6);
    }

    private static float distance(float[] fArray, float[] fArray2, int n) {
        float f = fArray[n] - fArray[0];
        float f2 = fArray2[n] - fArray2[0];
        return f * f + f2 * f2;
    }

    private static void thicken(int n) {
        int n2 = n * 9;
        for (int i = 0; i < 2; ++i) {
            float f = (boxes[n2 + i] + boxes[n2 + 3 + i]) * 0.5f;
            LootBoxes.boxes[n2 + i] = Math.min(boxes[n2 + i], f - 0.1f);
            LootBoxes.boxes[n2 + 3 + i] = Math.max(boxes[n2 + 3 + i], f + 0.1f);
        }
    }

    private LootBoxes() {
    }
}

