/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  zombie.core.skinnedmodel.model.IsoObjectModelDrawer
 *  zombie.core.skinnedmodel.model.ItemModelRenderer
 *  zombie.core.skinnedmodel.model.ModelMesh
 *  zombie.core.skinnedmodel.model.WorldItemModelDrawer
 *  zombie.core.textures.Texture
 *  zombie.inventory.InventoryItem
 *  zombie.iso.IsoGridSquare
 *  zombie.scripting.objects.ModelScript
 */
package viewpoint.models;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import viewpoint.core.Frame;
import viewpoint.light.OwnedLight;
import viewpoint.models.ModelCapture;
import viewpoint.render.ModelDraws;
import zombie.core.skinnedmodel.model.IsoObjectModelDrawer;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.scripting.objects.ModelScript;

final class RigidCapture {
    private static boolean reached = true;
    private static final Field OBJECT_MODEL = RigidCapture.field(IsoObjectModelDrawer.class, "model");
    private static final Field OBJECT_SCRIPT = RigidCapture.field(IsoObjectModelDrawer.class, "modelScript");
    private static final Field OBJECT_TEXTURE = RigidCapture.field(IsoObjectModelDrawer.class, "texture");
    private static final Field OBJECT_PALETTE = RigidCapture.field(IsoObjectModelDrawer.class, "matrixPalette");
    private static final Field OBJECT_TRANSFORM = RigidCapture.field(IsoObjectModelDrawer.class, "transform");
    private static final Field[] OBJECT_XYZ = RigidCapture.fields(IsoObjectModelDrawer.class, "x", "y", "z");
    private static final Field[] OBJECT_COLOUR = RigidCapture.fields(IsoObjectModelDrawer.class, "tintR", "tintG", "tintB", "hue");
    private static final Field[] OBJECT_AMBIENT = RigidCapture.fields(IsoObjectModelDrawer.class, "ambientR", "ambientG", "ambientB");
    private static final Field ITEM_RENDERER = RigidCapture.field(WorldItemModelDrawer.class, "renderer");
    private static final Field ITEM_MODEL = RigidCapture.field(ItemModelRenderer.class, "model");
    private static final Field ITEM_PARTS = RigidCapture.field(ItemModelRenderer.class, "weaponParts");
    private static final Field ITEM_TRANSFORM = RigidCapture.field(ItemModelRenderer.class, "transform");
    private static final Field ITEM_CULL = RigidCapture.field(ItemModelRenderer.class, "cullFace");
    private static final Field ITEM_MASK = RigidCapture.field(ItemModelRenderer.class, "tintMask");
    private static final Field[] ITEM_COLOUR = RigidCapture.fields(ItemModelRenderer.class, "tintR", "tintG", "tintB", "hue");
    private static final Field[] ITEM_AMBIENT = RigidCapture.fields(ItemModelRenderer.class, "ambientR", "ambientG", "ambientB");
    private static final Field PART_MODEL = RigidCapture.field(RigidCapture.part(), "model");
    private static final Field PART_TRANSFORM = RigidCapture.field(RigidCapture.part(), "transform");
    private static final Matrix4f place = new Matrix4f();
    private static final Matrix4f model = new Matrix4f();
    private static final float[] colour = new float[4];
    private static final float[] ambient = new float[3];

    static boolean object(Frame frame, IsoObjectModelDrawer isoObjectModelDrawer, IsoGridSquare isoGridSquare) {
        if (!reached) {
            return false;
        }
        try {
            boolean bl;
            Model model = (Model)((Object)OBJECT_MODEL.get(isoObjectModelDrawer));
            FloatBuffer floatBuffer = (FloatBuffer)OBJECT_PALETTE.get(isoObjectModelDrawer);
            if (!ModelCapture.loaded(model) || !model.isStatic && (floatBuffer == null || floatBuffer.limit() < 16)) {
                return false;
            }
            ModelScript modelScript = (ModelScript)OBJECT_SCRIPT.get(isoObjectModelDrawer);
            Texture texture = (Texture)OBJECT_TEXTURE.get(isoObjectModelDrawer);
            Texture texture2 = texture != null ? texture : model.tex;
            boolean bl2 = bl = modelScript != null && modelScript.getShaderName().contains("door");
            int n = bl ? 1028 : (modelScript == null || modelScript.cullFace <= 0 ? 0 : modelScript.cullFace);
            ModelDraws modelDraws = frame.scene.models;
            RigidCapture.placed(frame, OBJECT_XYZ[0].getFloat(isoObjectModelDrawer), OBJECT_XYZ[1].getFloat(isoObjectModelDrawer), OBJECT_XYZ[2].getFloat(isoObjectModelDrawer));
            place.rotateY((float)Math.PI);
            RigidCapture.model.set((Matrix4fc)place).mul((Matrix4fc)((Matrix4f)OBJECT_TRANSFORM.get(isoObjectModelDrawer)));
            int n2 = model.isStatic ? -1 : modelDraws.palette(floatBuffer, floatBuffer.limit() / 16);
            int n3 = modelDraws.add(model.mesh, null, texture2, RigidCapture.model, n2, n2, n, 3);
            RigidCapture.read(isoObjectModelDrawer, OBJECT_COLOUR, colour);
            RigidCapture.read(isoObjectModelDrawer, OBJECT_AMBIENT, ambient);
            modelDraws.material(n3, colour[0], colour[1], colour[2], colour[3]);
            if (bl && texture2 != null) {
                RigidCapture.doorUv(modelDraws, n3, texture2);
            }
            RigidCapture.finish(modelDraws, n3, isoGridSquare, model.mesh, !model.isStatic);
            return true;
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    static boolean item(Frame frame, WorldItemModelDrawer worldItemModelDrawer, IsoGridSquare isoGridSquare, InventoryItem inventoryItem) {
        if (!reached) {
            return false;
        }
        try {
            ItemModelRenderer itemModelRenderer = (ItemModelRenderer)ITEM_RENDERER.get(worldItemModelDrawer);
            Model model = (Model)((Object)ITEM_MODEL.get(itemModelRenderer));
            List list = (List)ITEM_PARTS.get(itemModelRenderer);
            if (!RigidCapture.rigid(model) || !RigidCapture.partsRigid(list)) {
                return false;
            }
            ModelDraws modelDraws = frame.scene.models;
            int n = Math.max(0, ITEM_CULL.getInt(itemModelRenderer));
            boolean bl = ITEM_MASK.get(itemModelRenderer) != null;
            RigidCapture.read(itemModelRenderer, ITEM_COLOUR, colour);
            RigidCapture.read(itemModelRenderer, ITEM_AMBIENT, ambient);
            RigidCapture.placed(frame, itemModelRenderer.x, itemModelRenderer.y, itemModelRenderer.z);
            place.rotateX((float)Math.toRadians(inventoryItem.worldXRotation)).rotateY((float)Math.toRadians(inventoryItem.worldZRotation)).rotateZ((float)Math.toRadians(inventoryItem.worldYRotation));
            RigidCapture.model.set((Matrix4fc)place).mul((Matrix4fc)((Matrix4f)ITEM_TRANSFORM.get(itemModelRenderer)));
            int n2 = modelDraws.add(model.mesh, null, model.tex, RigidCapture.model, -1, -1, n, 3);
            modelDraws.item(n2, itemModelRenderer);
            modelDraws.material(n2, bl ? 1.0f : colour[0], bl ? 1.0f : colour[1], bl ? 1.0f : colour[2], colour[3]);
            RigidCapture.finish(modelDraws, n2, isoGridSquare, model.mesh, false);
            for (int i = 0; list != null && i < list.size(); ++i) {
                Model model2 = (Model)((Object)PART_MODEL.get(list.get(i)));
                RigidCapture.model.set((Matrix4fc)place).mul((Matrix4fc)((Matrix4f)PART_TRANSFORM.get(list.get(i))));
                int n3 = modelDraws.add(model2.mesh, null, model2.tex, RigidCapture.model, -1, -1, n, 3);
                modelDraws.material(n3, bl ? 1.0f : colour[0], bl ? 1.0f : colour[1], bl ? 1.0f : colour[2], colour[3]);
                RigidCapture.finish(modelDraws, n3, isoGridSquare, model2.mesh, false);
            }
            return true;
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean rigid(Model model) {
        return ModelCapture.loaded(model) && model.isStatic;
    }

    private static boolean partsRigid(List<?> list) throws ReflectiveOperationException {
        for (int i = 0; list != null && i < list.size(); ++i) {
            if (RigidCapture.rigid((Model)((Object)PART_MODEL.get(list.get(i))))) continue;
            return false;
        }
        return true;
    }

    private static void placed(Frame frame, float f, float f2, float f3) {
        place.translation(-(f - frame.camX), (f3 - frame.camZ) * 2.4494896f, -(f2 - frame.camY)).scale(-1.5f, 1.5f, 1.5f);
    }

    private static void doorUv(ModelDraws modelDraws, int n, Texture texture) {
        float f = texture.getWidthHW();
        float f2 = texture.getHeightHW();
        float f3 = texture.xStart * f - texture.offsetX;
        float f4 = texture.yStart * f2 - texture.offsetY;
        modelDraws.uv(n, f3 / f, f4 / f2, (float)texture.getWidthOrig() / f, (float)texture.getHeightOrig() / f2);
    }

    private static void finish(ModelDraws modelDraws, int n, IsoGridSquare isoGridSquare, ModelMesh modelMesh, boolean bl) {
        int n2 = isoGridSquare == null ? -1 : OwnedLight.modelLightAt(isoGridSquare.x, isoGridSquare.y, isoGridSquare.z);
        modelDraws.light(n, n2 < 0 ? Math.min(1.0f, ambient[0]) : (float)(n2 & 0xFF) / 255.0f, n2 < 0 ? Math.min(1.0f, ambient[1]) : (float)(n2 >> 8 & 0xFF) / 255.0f, n2 < 0 ? Math.min(1.0f, ambient[2]) : (float)(n2 >> 16 & 0xFF) / 255.0f, isoGridSquare != null && isoGridSquare.isOutside() && !isoGridSquare.haveRoof);
        modelDraws.bounds(n, model, modelMesh.minXyz, modelMesh.maxXyz, bl ? 2.0f : 1.0f);
    }

    private static void read(Object object, Field[] fieldArray, float[] fArray) throws IllegalAccessException {
        for (int i = 0; i < fieldArray.length; ++i) {
            fArray[i] = fieldArray[i].getFloat(object);
        }
    }

    private static Class<?> part() {
        try {
            return Class.forName("zombie.core.skinnedmodel.model.ItemModelRenderer$WeaponPartParams");
        }
        catch (ClassNotFoundException | RuntimeException exception) {
            return null;
        }
    }

    private static Field[] fields(Class<?> clazz, String ... stringArray) {
        Field[] fieldArray = new Field[stringArray.length];
        for (int i = 0; i < stringArray.length; ++i) {
            fieldArray[i] = RigidCapture.field(clazz, stringArray[i]);
        }
        return fieldArray;
    }

    private static Field field(Class<?> clazz, String string) {
        try {
            Field field = clazz.getDeclaredField(string);
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            if (reached) {
                System.out.println("[Viewpoint] models: objects and items cannot be drawn, " + string + ": " + String.valueOf(exception));
            }
            reached = false;
            return null;
        }
    }

    private RigidCapture() {
    }
}

