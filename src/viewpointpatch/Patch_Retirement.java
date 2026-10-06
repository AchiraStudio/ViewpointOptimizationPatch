package viewpointpatch;

import me.zed_0xff.zombie_buddy.Patch;
import viewpointpatch.render.SafeRetirement;

@Patch(className = "viewpoint.render.Retirement", methodName = "fenceDrawn")
public class Patch_Retirement {

    @Patch.OnEnter(skipOn = true)
    public static boolean onEnter(long l, @Patch.This Object self) {
        return SafeRetirement.execute(l, self);
    }
}
