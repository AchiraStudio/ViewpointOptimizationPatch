package viewpointpatch;

import java.lang.reflect.Method;
import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.platform.LiveSettings;

@Patch(className = "viewpoint.render.FloorFilter", methodName = "bakeFilter")
public class Patch_FloorFilter {
    public static Method valueMethod;

    public static String getLiveSetting(String key) {
        try {
            if (valueMethod == null) {
                valueMethod = LiveSettings.class.getDeclaredMethod("value", String.class);
                valueMethod.setAccessible(true);
            }
            return (String) valueMethod.invoke(null, key);
        } catch (Throwable t) {
            return null;
        }
    }

    @Patch.OnExit
    public static void onExit(@Patch.Return(readOnly = false) int ret) {
        if (ret == 2) { // 2 = BAKE_LANCZOS
            String filter = getLiveSetting("floors.filter");
            if (filter != null && !"Lanczos".equalsIgnoreCase(filter.trim())) {
                ret = 1; // 1 = BAKE_LINEAR (much faster, no frame hitches)
            }
        }
    }
}
