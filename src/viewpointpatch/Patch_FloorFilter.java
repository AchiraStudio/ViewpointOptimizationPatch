package viewpointpatch;

import me.zed_0xff.zombie_buddy.Patch;
import viewpointpatch.render.FloorFilterOptimizer;

@Patch(className = "viewpoint.render.FloorFilter", methodName = "bakeFilter")
public class Patch_FloorFilter {

    @Patch.OnExit
    public static void onExit(@Patch.Return(readOnly = false) int ret) {
        ret = FloorFilterOptimizer.optimizeFilter(ret);
    }
}
