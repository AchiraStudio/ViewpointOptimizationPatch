/*
 * Decompiled with CFR 0.152.
 */
package viewpoint;

import viewpoint.game.FrameCaps;
import viewpoint.platform.BuildPin;
import viewpoint.platform.HotReload;

public class Main {
    public static void main(String[] stringArray) {
        System.out.println("[Viewpoint] loaded");
        BuildPin.supported();
        if (FrameCaps.ON) {
            FrameCaps.restoreAtStartup();
        }
        HotReload.start();
    }
}

