/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.ZomboidFileSystem$PZFolder
 *  zombie.gameStates.ChooseGameInfo
 *  zombie.gameStates.ChooseGameInfo$Mod
 *  zombie.iso.sprite.IsoSprite
 */
package viewpoint.packs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import viewpoint.packs.PackBind;
import viewpoint.packs.PackManifest;
import viewpoint.platform.LiveSettings;
import viewpoint.render.PackModel;
import viewpoint.render.PackModels;
import zombie.ZomboidFileSystem;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.sprite.IsoSprite;

public final class ModelPacks {
    private static final String MEDIA = "media/";
    private static final int ERRORS_SHOWN = 5;
    private static final String LODS = "World/Levels of detail";
    private static final String SECTION = "World/Model packs";
    private static final LiveSettings.Number RING = LiveSettings.number("lod.packRing", "Model packs' models within (chunks)", "World/Levels of detail", 0.0f, 16.0f, 1.0f, 6.0f);
    private static final ArrayList<Pack> packs;
    private static final HashSet<String> seen;
    private static final HashMap<String, LiveSettings.Toggle> switches;
    private static HashMap<String, PackBind> bound;
    private static int generation;

    public static boolean register(String string2, String string3) {
        List<String> list;
        if (string2 == null || string3 == null) {
            return false;
        }
        if (!seen.add(string2 + "\n" + string3)) {
            return ModelPacks.registered(string2, string3);
        }
        File file = ModelPacks.find(string2, string3);
        if (file == null) {
            System.out.println("[Viewpoint] model pack " + string2 + ": no " + string3 + " in its folders");
            return false;
        }
        try {
            list = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        }
        catch (IOException iOException) {
            System.out.println("[Viewpoint] model pack " + string2 + ": " + string3 + " unreadable: " + String.valueOf(iOException));
            return false;
        }
        switches.computeIfAbsent(string2, string -> LiveSettings.toggle("packs." + string, ModelPacks.label(string), SECTION, true));
        ModelPacks.add(ModelPacks.read(string2, string3, file.getParentFile(), PackManifest.parse(list)));
        PackModels.publish();
        return true;
    }

    public static void frame() {
        boolean bl = false;
        for (Pack pack : packs) {
            LiveSettings.Toggle toggle = switches.get(pack.modId);
            boolean bl2 = toggle == null || toggle.get();
            bl |= bl2 != pack.on;
            pack.on = bl2;
        }
        if (bl) {
            ModelPacks.rebind();
        }
    }

    public static int ring() {
        return RING.getInt() > 0 && !bound.isEmpty() ? RING.getInt() : -1;
    }

    public static PackBind bind(IsoSprite isoSprite) {
        return isoSprite.name == null ? null : bound.get(isoSprite.name);
    }

    public static PackBind bind(String string) {
        return bound.get(string);
    }

    public static int generation() {
        return generation;
    }

    public static boolean any() {
        return !bound.isEmpty();
    }

    /*
     * WARNING - void declaration
     */
    static Pack read(String string, String string2, File file, PackManifest packManifest) {
        void var7_13;
        HashMap<String, PackModel> hashMap = new HashMap<String, PackModel>();
        for (Map.Entry<String, String> object2 : packManifest.models.entrySet()) {
            File file2 = new File(file, object2.getValue()).toPath().toAbsolutePath().normalize().toFile();
            hashMap.put(object2.getKey(), PackModels.of(file2));
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (Map.Entry<String, PackManifest.Bind> entry : packManifest.binds.entrySet()) {
            PackManifest.Bind bind = entry.getValue();
            linkedHashMap.put(entry.getKey(), new PackBind((PackModel)hashMap.get(bind.model()), bind.degrees(), bind.x(), bind.y(), bind.z(), bind.scale()));
        }
        Object object = packManifest.id != null ? packManifest.id : string + ":" + string2;
        System.out.println("[Viewpoint] model pack " + (String)object + " (" + string + ", " + string2 + "): " + hashMap.size() + " models, " + linkedHashMap.size() + " sprites" + (String)(packManifest.errors.isEmpty() ? "" : ", " + packManifest.errors.size() + " lines left out"));
        boolean bl = false;
        while (var7_13 < Math.min(5, packManifest.errors.size())) {
            System.out.println("[Viewpoint] model pack " + (String)object + ": " + packManifest.errors.get((int)var7_13));
            ++var7_13;
        }
        return new Pack(string, string2, (String)object, (String)(packManifest.name != null ? packManifest.name : object), linkedHashMap);
    }

    static void add(Pack pack) {
        packs.add(pack);
        ModelPacks.rebind();
    }

    static void rebind() {
        HashMap<String, PackBind> hashMap = new HashMap<String, PackBind>();
        for (Pack pack : packs) {
            if (!pack.on) continue;
            hashMap.putAll(pack.binds);
        }
        bound = hashMap;
        ++generation;
    }

    static void clear() {
        packs.clear();
        seen.clear();
        ModelPacks.rebind();
    }

    private static boolean registered(String string, String string2) {
        for (Pack pack : packs) {
            if (!pack.modId.equals(string) || !pack.manifest.equals(string2)) continue;
            return true;
        }
        return false;
    }

    private static String label(String string) {
        ChooseGameInfo.Mod mod = ChooseGameInfo.getModDetails((String)string);
        return mod != null && mod.getName() != null ? mod.getName() : string;
    }

    private static File find(String string, String string2) {
        ChooseGameInfo.Mod mod = ChooseGameInfo.getModDetails((String)string);
        if (mod == null) {
            return null;
        }
        String string3 = string2.replace('\\', '/');
        string3 = string3.startsWith(MEDIA) ? string3.substring(MEDIA.length()) : string3;
        for (ZomboidFileSystem.PZFolder pZFolder : new ZomboidFileSystem.PZFolder[]{mod.mediaFile.version, mod.mediaFile.common}) {
            File file;
            File file2 = pZFolder == null ? null : pZFolder.canonicalFile;
            File file3 = file = file2 == null ? null : new File(file2, string3);
            if (file == null || !file.isFile()) continue;
            return file;
        }
        return null;
    }

    private ModelPacks() {
    }

    static {
        RING.describe("How many chunks round you draw the model packs' authored models; further out, the sprites' art. 0 draws none.");
        packs = new ArrayList();
        seen = new HashSet();
        switches = new HashMap();
        bound = new HashMap();
    }

    static final class Pack {
        final String modId;
        final String manifest;
        final String id;
        final String name;
        final Map<String, PackBind> binds;
        boolean on = true;

        Pack(String string, String string2, String string3, String string4, Map<String, PackBind> map) {
            this.modId = string;
            this.manifest = string2;
            this.id = string3;
            this.name = string4;
            this.binds = map;
        }
    }
}

