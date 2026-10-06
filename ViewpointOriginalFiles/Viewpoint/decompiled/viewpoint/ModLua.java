/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.Lua.LuaManager
 */
package viewpoint;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import zombie.Lua.LuaManager;

final class ModLua {
    private static final String LOADED = "ViewpointLoot";

    static void load() {
        File file2;
        if (LuaManager.env.rawget((Object)LOADED) != null) {
            return;
        }
        try {
            file2 = ModLua.folder();
        }
        catch (URISyntaxException uRISyntaxException) {
            System.out.println("[Viewpoint] Lua: the game loaded none of the mod's, and its jar has no path: " + String.valueOf(uRISyntaxException));
            return;
        }
        File[] fileArray = file2.listFiles((file, string) -> string.toLowerCase(Locale.ENGLISH).endsWith(".lua"));
        if (fileArray == null || fileArray.length == 0) {
            System.out.println("[Viewpoint] Lua: the game loaded none of the mod's, and " + String.valueOf(file2) + " has none");
            return;
        }
        Arrays.sort(fileArray, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File file3 : fileArray) {
            LuaManager.RunLua((String)file3.getAbsolutePath());
        }
        System.out.println("[Viewpoint] Lua: the game loaded none of the mod's (a server that does not list Viewpoint); ran its " + fileArray.length + " files from " + String.valueOf(file2));
    }

    private static File folder() throws URISyntaxException {
        File file = new File(ModLua.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        File file2 = file.getParentFile().getParentFile().getParentFile();
        return new File(file2, "lua" + File.separator + "client");
    }

    private ModLua() {
    }
}

