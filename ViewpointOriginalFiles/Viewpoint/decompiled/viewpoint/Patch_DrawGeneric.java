/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$Argument
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.core.textures.TextureDraw;

@Patch(className="zombie.core.SpriteRenderer", methodName="drawGeneric")
public class Patch_DrawGeneric {
    @Patch.OnEnter(skipOn=true)
    public static boolean enter(@Patch.Argument(value=0) TextureDraw.GenericDrawer genericDrawer) {
        return Hooks.skipDrawGeneric(genericDrawer);
    }
}

