/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.objects.IsoDeadBody
 *  zombie.scripting.objects.ModelScript
 */
package viewpoint.models;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import viewpoint.core.Frame;
import viewpoint.light.OwnedLight;
import viewpoint.models.CorpsePose;
import viewpoint.models.ModelCapture;
import viewpoint.render.ModelDraws;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.scripting.objects.ModelScript;

final class CorpseCapture {
    static final float MIDDLE = 0.2f;
    static final float RADIUS = 1.2f;
    private static final float[] WHOLE = new float[]{0.3f, 1.0f, 0.3f};
    private static final float[] DISSOLVING = new float[]{1.0f, 1.0f, 0.2f};
    private static final float[] UNTINTED = new float[]{1.0f, 1.0f, 1.0f};
    private static final Matrix4f place = new Matrix4f();
    private static final Matrix4f model = new Matrix4f();
    private static final Matrix4f attachment = new Matrix4f();
    private static int[] firstBones = new int[8];

    static void draw(Frame frame, IsoDeadBody isoDeadBody, CorpsePose corpsePose, float f, int n, boolean bl) {
        int n2;
        ModelDraws modelDraws = frame.scene.models;
        int n3 = modelDraws.count();
        float[] fArray = !bl ? UNTINTED : (f < 1.0f ? DISSOLVING : WHOLE);
        float f2 = isoDeadBody.getX();
        float f3 = isoDeadBody.getY();
        float f4 = isoDeadBody.getZ();
        place.translation(-(f2 - frame.camX), (f4 - frame.camZ) * 2.4494896f, -(f3 - frame.camY)).scale(-1.5f, 1.5f, 1.5f).rotateY(corpsePose.angle + (float)Math.PI).scale(corpsePose.scale);
        float f5 = place.m30();
        float f6 = place.m31();
        float f7 = place.m32();
        IsoGridSquare isoGridSquare = isoDeadBody.getSquare();
        int n4 = isoGridSquare == null ? -1 : OwnedLight.lightAt(isoGridSquare.x, isoGridSquare.y, isoGridSquare.z);
        boolean bl2 = isoGridSquare != null && isoGridSquare.isOutside() && !isoGridSquare.haveRoof;
        firstBones = firstBones.length >= corpsePose.palettes.length ? firstBones : new int[corpsePose.palettes.length * 2];
        for (n2 = 0; n2 < corpsePose.palettes.length; ++n2) {
            CorpseCapture.firstBones[n2] = modelDraws.palette(corpsePose.palettes[n2], corpsePose.paletteBones[n2]);
        }
        for (n2 = 0; n2 < corpsePose.parts.length; ++n2) {
            ModelInstance modelInstance = corpsePose.parts[n2];
            if (!ModelCapture.loaded(modelInstance.model)) continue;
            int n5 = corpsePose.paletteOf[n2] < 0 ? -1 : firstBones[corpsePose.paletteOf[n2]];
            Matrix4f matrix4f = n5 < 0 ? model.set((Matrix4fc)place).mul((Matrix4fc)attachment.set((Matrix4fc)corpsePose.attachments[n2]).transpose()) : place;
            int n6 = modelDraws.add(modelInstance.model.mesh, modelInstance, modelInstance.model.tex, matrix4f, n5, n5, CorpseCapture.cull(modelInstance), n);
            modelDraws.material(n6, modelInstance.tintR * fArray[0], modelInstance.tintG * fArray[1], modelInstance.tintB * fArray[2], modelInstance.hue);
            modelDraws.light(n6, n4 < 0 ? Math.min(1.0f, corpsePose.ambientR) : (float)(n4 & 0xFF) / 255.0f, n4 < 0 ? Math.min(1.0f, corpsePose.ambientG) : (float)(n4 >> 8 & 0xFF) / 255.0f, n4 < 0 ? Math.min(1.0f, corpsePose.ambientB) : (float)(n4 >> 16 & 0xFF) / 255.0f, bl2);
            modelDraws.bounds(n6, f5, f6 + 0.2f, f7, 1.2f * Math.max(1.0f, corpsePose.scale));
            modelDraws.fade(n6, 0.0f, f);
        }
        if (isoGridSquare != null && isoGridSquare == frame.lootSquare) {
            modelDraws.target(n3);
        }
    }

    private static int cull(ModelInstance modelInstance) {
        ModelScript modelScript = modelInstance.modelScript;
        return modelScript == null || modelScript.cullFace == -1 ? 1028 : modelScript.cullFace;
    }

    private CorpseCapture() {
    }
}

