/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Exposer$LuaClass
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Exposer;
import viewpoint.packs.ModelPacks;

@Exposer.LuaClass(name="Viewpoint.ModelPacks")
public final class ModelPacksLua {
    public static boolean register(String string, String string2) {
        return ModelPacks.register(string, string2);
    }

    private ModelPacksLua() {
    }
}

