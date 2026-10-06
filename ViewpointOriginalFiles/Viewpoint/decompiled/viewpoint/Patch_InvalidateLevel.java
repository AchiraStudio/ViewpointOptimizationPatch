/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$Argument
 *  me.zed_0xff.zombie_buddy.Patch$OnEnter
 *  me.zed_0xff.zombie_buddy.Patch$This
 *  zombie.iso.fboRenderChunk.FBORenderLevels
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.iso.fboRenderChunk.FBORenderLevels;

@Patch(className="zombie.iso.fboRenderChunk.FBORenderLevels", methodName="invalidateLevel")
public class Patch_InvalidateLevel {
    @Patch.OnEnter
    public static void enter(@Patch.This FBORenderLevels fBORenderLevels, @Patch.Argument(value=0) int n, @Patch.Argument(value=1) long l) {
        Hooks.levelInvalidated(fBORenderLevels, n, l);
    }
}

