/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  fmod.fmod.SoundListener
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$This
 */
package viewpoint;

import fmod.fmod.SoundListener;
import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;

@Patch(className="fmod.fmod.SoundListener", methodName="tick")
public class Patch_SoundListener {
    @Patch.OnExit
    public static void exit(@Patch.This SoundListener soundListener) {
        Hooks.soundListener(soundListener);
    }
}

