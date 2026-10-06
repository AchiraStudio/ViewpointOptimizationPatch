/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.skinnedmodel.model.ItemModelRenderer
 *  zombie.core.textures.SmartTexture
 *  zombie.core.textures.Texture
 *  zombie.core.textures.TextureCombiner
 */
package viewpoint.render;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.textures.SmartTexture;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureCombiner;

final class ItemTextures {
    private static final Method CHECK = ItemTextures.method();
    private static final Field NAME = ItemTextures.field("modelTextureName");
    private static final Field SMART = ItemTextures.field("smartTexture");
    private final ArrayList<ItemModelRenderer> made = new ArrayList();

    ItemTextures() {
    }

    Texture make(ItemModelRenderer itemModelRenderer) {
        if (CHECK == null || NAME == null || SMART == null) {
            return null;
        }
        try {
            if (!((Boolean)CHECK.invoke((Object)itemModelRenderer, NAME.get(itemModelRenderer))).booleanValue()) {
                return null;
            }
            this.made.add(itemModelRenderer);
            return (Texture)SMART.get(itemModelRenderer);
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    void release() {
        for (int i = 0; i < this.made.size(); ++i) {
            try {
                SmartTexture smartTexture = (SmartTexture)SMART.get(this.made.get(i));
                if (smartTexture.result != null) {
                    TextureCombiner.instance.releaseTexture(smartTexture.result);
                    smartTexture.result = null;
                }
                smartTexture.clear();
                continue;
            }
            catch (ReflectiveOperationException | RuntimeException exception) {
                // empty catch block
            }
        }
        this.made.clear();
    }

    private static Method method() {
        try {
            Method method = ItemModelRenderer.class.getDeclaredMethod("checkSmartTexture", String.class);
            method.setAccessible(true);
            return method;
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            System.out.println("[Viewpoint] models: items keep their plain textures: " + String.valueOf(exception));
            return null;
        }
    }

    private static Field field(String string) {
        try {
            Field field = ItemModelRenderer.class.getDeclaredField(string);
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            System.out.println("[Viewpoint] models: items keep their plain textures: " + String.valueOf(exception));
            return null;
        }
    }
}

