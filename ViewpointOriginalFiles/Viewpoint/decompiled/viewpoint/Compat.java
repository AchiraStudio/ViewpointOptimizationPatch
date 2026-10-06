/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 */
package viewpoint;

import java.lang.invoke.CallSite;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Patch_AimFromReticle;
import viewpoint.Patch_CameraZoom;
import viewpoint.Patch_CheckLights;
import viewpoint.Patch_ContextPick;
import viewpoint.Patch_CullAnimals;
import viewpoint.Patch_DisplayCursor;
import viewpoint.Patch_DrawGeneric;
import viewpoint.Patch_FrameSwap;
import viewpoint.Patch_FramerateUncapped;
import viewpoint.Patch_GameLoaded;
import viewpoint.Patch_GameRender;
import viewpoint.Patch_HeadSquare;
import viewpoint.Patch_InvalidateAll;
import viewpoint.Patch_InvalidateLevel;
import viewpoint.Patch_IsoCell;
import viewpoint.Patch_IsoCursor;
import viewpoint.Patch_IsoReticle;
import viewpoint.Patch_KeyDown;
import viewpoint.Patch_LightingUpdate;
import viewpoint.Patch_LineOfSight;
import viewpoint.Patch_LoadOptions;
import viewpoint.Patch_LockFps;
import viewpoint.Patch_LookAngle;
import viewpoint.Patch_ModelSlotUpdate;
import viewpoint.Patch_MouseUpdate;
import viewpoint.Patch_MoveVector;
import viewpoint.Patch_Movement;
import viewpoint.Patch_PickCorpse;
import viewpoint.Patch_PickDoor;
import viewpoint.Patch_PickHoppable;
import viewpoint.Patch_PickThumpable;
import viewpoint.Patch_PickTree;
import viewpoint.Patch_PickVehicle;
import viewpoint.Patch_PickWindow;
import viewpoint.Patch_PickWindowFrame;
import viewpoint.Patch_PlayStarted;
import viewpoint.Patch_ReticleX;
import viewpoint.Patch_ReticleY;
import viewpoint.Patch_RootMotion;
import viewpoint.Patch_Running;
import viewpoint.Patch_SaveOptions;
import viewpoint.Patch_SimulationLevel;
import viewpoint.Patch_SkeletonPosed;
import viewpoint.Patch_SoundListener;
import viewpoint.Patch_Sprinting;
import viewpoint.Patch_StatsGet;
import viewpoint.Patch_Strafing;
import viewpoint.Patch_SwitchPower;
import viewpoint.Patch_Thunder;
import viewpoint.Patch_UiFrameEnd;
import viewpoint.Patch_ViewDistMax;
import viewpoint.Patch_ViewDistance;
import viewpoint.Patch_VisionCone;
import viewpoint.Patch_WeatherFX;
import viewpoint.Patch_WindowGlass;
import viewpoint.Patch_WindowSmash;
import viewpoint.Patch_WindowSync;
import viewpoint.Patch_WindowToggle;
import viewpoint.Patch_ZombieCull;
import viewpoint.Patch_ZombieScreenX;
import viewpoint.Patch_ZombieScreenY;

public final class Compat {
    public static final int GAME_LOADED = 0;
    public static final int CULL_ANIMALS = 1;
    public static final int DISPLAY_CURSOR = 2;
    public static final int DRAW_GENERIC = 3;
    public static final int INVALIDATE_ALL = 4;
    public static final int INVALIDATE_LEVEL = 5;
    public static final int ISO_CELL = 6;
    public static final int ISO_CURSOR = 7;
    public static final int ISO_RETICLE = 8;
    public static final int LIGHTING_UPDATE = 9;
    public static final int LOOK_ANGLE = 10;
    public static final int MOUSE_UPDATE = 11;
    public static final int MOVEMENT = 12;
    public static final int MOVE_VECTOR = 13;
    public static final int RETICLE_X = 14;
    public static final int RETICLE_Y = 15;
    public static final int RUNNING = 16;
    public static final int SPRINTING = 17;
    public static final int STRAFING = 18;
    public static final int VIEW_DISTANCE = 19;
    public static final int VIEW_DIST_MAX = 20;
    public static final int VISION_CONE = 21;
    public static final int WEATHER_FX = 22;
    public static final int ZOMBIE_SCREEN_X = 23;
    public static final int ZOMBIE_SCREEN_Y = 24;
    public static final int SIMULATION_LEVEL = 25;
    public static final int STATS_GET = 26;
    public static final int ZOMBIE_CULL = 27;
    public static final int HEAD_SQUARE = 28;
    public static final int KEY_DOWN = 29;
    public static final int WINDOW_TOGGLE = 30;
    public static final int WINDOW_SMASH = 31;
    public static final int WINDOW_GLASS = 32;
    public static final int WINDOW_SYNC = 33;
    public static final int FRAME_SWAP = 34;
    public static final int GAME_RENDER = 35;
    public static final int UI_FRAME_END = 36;
    public static final int PLAY_STARTED = 37;
    public static final int LOCK_FPS = 38;
    public static final int FRAMERATE_UNCAPPED = 39;
    public static final int LOAD_OPTIONS = 40;
    public static final int SAVE_OPTIONS = 41;
    public static final int CHECK_LIGHTS = 42;
    public static final int SWITCH_POWER = 43;
    public static final int MODEL_SLOT_UPDATE = 44;
    public static final int LINE_OF_SIGHT = 45;
    public static final int THUNDER = 46;
    public static final int SOUND_LISTENER = 47;
    public static final int CONTEXT_PICK = 48;
    public static final int PICK_DOOR = 49;
    public static final int PICK_WINDOW = 50;
    public static final int PICK_WINDOW_FRAME = 51;
    public static final int PICK_THUMPABLE = 52;
    public static final int PICK_HOPPABLE = 53;
    public static final int PICK_CORPSE = 54;
    public static final int PICK_TREE = 55;
    public static final int PICK_VEHICLE = 56;
    public static final int CAMERA_ZOOM = 57;
    public static final int AIM_FROM_RETICLE = 58;
    public static final int SKELETON_POSED = 59;
    public static final int ROOT_MOTION = 60;
    private static final Class<?>[] PATCHES = new Class[]{Patch_GameLoaded.class, Patch_CullAnimals.class, Patch_DisplayCursor.class, Patch_DrawGeneric.class, Patch_InvalidateAll.class, Patch_InvalidateLevel.class, Patch_IsoCell.class, Patch_IsoCursor.class, Patch_IsoReticle.class, Patch_LightingUpdate.class, Patch_LookAngle.class, Patch_MouseUpdate.class, Patch_Movement.class, Patch_MoveVector.class, Patch_ReticleX.class, Patch_ReticleY.class, Patch_Running.class, Patch_Sprinting.class, Patch_Strafing.class, Patch_ViewDistance.class, Patch_ViewDistMax.class, Patch_VisionCone.class, Patch_WeatherFX.class, Patch_ZombieScreenX.class, Patch_ZombieScreenY.class, Patch_SimulationLevel.class, Patch_StatsGet.class, Patch_ZombieCull.class, Patch_HeadSquare.class, Patch_KeyDown.class, Patch_WindowToggle.class, Patch_WindowSmash.class, Patch_WindowGlass.class, Patch_WindowSync.class, Patch_FrameSwap.class, Patch_GameRender.class, Patch_UiFrameEnd.class, Patch_PlayStarted.class, Patch_LockFps.class, Patch_FramerateUncapped.class, Patch_LoadOptions.class, Patch_SaveOptions.class, Patch_CheckLights.class, Patch_SwitchPower.class, Patch_ModelSlotUpdate.class, Patch_LineOfSight.class, Patch_Thunder.class, Patch_SoundListener.class, Patch_ContextPick.class, Patch_PickDoor.class, Patch_PickWindow.class, Patch_PickWindowFrame.class, Patch_PickThumpable.class, Patch_PickHoppable.class, Patch_PickCorpse.class, Patch_PickTree.class, Patch_PickVehicle.class, Patch_CameraZoom.class, Patch_AimFromReticle.class, Patch_SkeletonPosed.class, Patch_RootMotion.class};
    private static final String[] WHEN = new String[]{"when a game loads", "every frame", "every frame", "every frame", "when a chunk changes", "when a chunk changes", "every frame", "when the game draws its cursor", "while aiming", "every frame", "every frame", "every frame", "every frame on foot", "while moving on foot", "while aiming", "while aiming", "every frame", "every frame", "every frame", "every frame", "every frame", "every frame", "every frame", "with zombies around", "with zombies around", "every frame", "every frame", "every frame", "with zombies out of sight nearby", "every frame", "when a window opens or closes", "when a window is smashed", "when broken glass is taken out of a window", "when another player changes a window (multiplayer)", "every frame", "every frame", "every frame", "when play starts", "every frame", "every frame", "when the game reads its options", "when the game writes its options", "every frame", "every frame with a switched lamp in the loaded chunks", "every frame with a character that has a model", "every frame", "when lightning strikes or thunder rumbles, in a storm", "every frame", "when the mouse moves over the world", "when something in the world is right-clicked", "when something in the world is right-clicked", "when something in the world is right-clicked", "when something in the world is right-clicked", "when something in the world is right-clicked", "when a world menu opens", "when something in the world is right-clicked", "when a vehicle is right-clicked", "every frame", "while aiming a firearm", "every frame", "every frame on foot"};
    private static final String[][] MEMBERS = new String[][]{{"zombie.core.skinnedmodel.model.VertexBufferObject", "handle", "owned models (ModelMeshes)"}, {"zombie.iso.fboRenderChunk.FBORenderChunkManager", "sizeChunkStore", "isometric chunk textures let go in first person (IsoChunkTextures)"}, {"zombie.core.skinnedmodel.model.VertexBufferObject", "vertexFormat", "owned models (ModelMeshes)"}, {"zombie.core.skinnedmodel.model.ModelSlotRenderData", "rendered", "owned models' textures (CharacterTextures.Snapshot)"}, {"zombie.core.skinnedmodel.model.ModelSlotRenderData", "characterOutline", "owned characters' outlines (ModelCapture)"}, {"zombie.core.skinnedmodel.model.ModelSlotRenderData", "outlineColor", "owned characters' outlines (ModelCapture)"}, {"zombie.core.skinnedmodel.model.ModelSlotRenderData", "outlineBehindPlayer", "owned characters' outlines (ModelCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "x", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "y", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "z", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "transform", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "model", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.WorldItemModelDrawer", "renderer", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "modelScript", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "texture", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "matrixPalette", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "tintR", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "tintG", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "tintB", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "hue", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "ambientR", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "ambientG", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.IsoObjectModelDrawer", "ambientB", "owned object models (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "model", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "weaponParts", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "transform", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "cullFace", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "tintMask", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "tintR", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "tintG", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "tintB", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "hue", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "ambientR", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "ambientG", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "ambientB", "owned ground items (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer$WeaponPartParams", "model", "owned ground items' weapon parts (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer$WeaponPartParams", "transform", "owned ground items' weapon parts (RigidCapture)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "checkSmartTexture()", "owned ground items' tint masks, blood and fluid (ItemTextures)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "modelTextureName", "owned ground items' tint masks, blood and fluid (ItemTextures)"}, {"zombie.core.skinnedmodel.model.ItemModelRenderer", "smartTexture", "owned ground items' tint masks, blood and fluid (ItemTextures)"}, {"zombie.core.skinnedmodel.DeadBodyAtlas$BodyTexture", "entry", "corpse images waiting for their offsets (WorldMesher)"}, {"zombie.input.Mouse", "x", "the mouse parked while looking (Controls)"}, {"zombie.input.Mouse", "y", "the mouse parked while looking (Controls)"}, {"zombie.characters.Stats", "stats", "the game's stat lookup without boxing (GameFixes)"}, {"zombie.erosion.ErosionMain", "noiseMain", "the erosion's trees in the far world (Erosion)"}, {"zombie.erosion.ErosionMain", "noiseMoisture", "the erosion's trees in the far world (Erosion)"}, {"zombie.erosion.ErosionMain", "noiseMinerals", "the erosion's trees in the far world (Erosion)"}, {"zombie.erosion.ErosionMain", "soilTable", "the erosion's trees in the far world (Erosion)"}, {"zombie.erosion.utils.Noise2D", "layers", "the erosion's trees in the far world (Erosion)"}, {"zombie.erosion.utils.Noise2D", "perm", "the erosion's trees in the far world (Erosion)"}, {"zombie.erosion.utils.Noise2D$Layer", "freq", "the erosion's trees in the far world (Erosion)"}, {"zombie.erosion.utils.Noise2D$Layer", "amp", "the erosion's trees in the far world (Erosion)"}, {"zombie.erosion.utils.Noise2D$Layer", "p", "the erosion's trees in the far world (Erosion)"}, {"zombie.erosion.categories.NatureTrees", "trees", "the erosion's trees in the far world (Erosion)"}, {"zombie.erosion.categories.NatureTrees", "soilRef", "the erosion's trees in the far world (Erosion)"}, {"zombie.erosion.categories.NatureTrees", "spawnChance", "the erosion's trees in the far world (Erosion)"}, {"zombie.erosion.categories.NatureBush", "soilRef", "the erosion's plants in the far world (ErosionPlants)"}, {"zombie.erosion.categories.NatureBush", "spawnChance", "the erosion's plants in the far world (ErosionPlants)"}, {"zombie.erosion.categories.NaturePlants", "soilRef", "the erosion's plants in the far world (ErosionPlants)"}, {"zombie.erosion.categories.NaturePlants", "spawnChance", "the erosion's plants in the far world (ErosionPlants)"}, {"zombie.erosion.categories.NatureTrees$TreeInit", "tilesetName", "the erosion's trees in the far world (Erosion)"}};
    private static final boolean[] fired = new boolean[PATCHES.length];
    private static boolean checked;
    private static boolean reported;

    static Set<String> patchNames() {
        TreeSet<String> treeSet = new TreeSet<String>();
        for (Class<?> clazz : PATCHES) {
            treeSet.add(clazz.getSimpleName());
        }
        return treeSet;
    }

    public static void hit(int n) {
        if (!fired[n]) {
            Compat.fired[n] = true;
            if (reported) {
                System.out.println("[Viewpoint] patches seen working: " + Compat.seen() + " of " + PATCHES.length + ", now also " + Compat.name(n));
            }
        }
    }

    public static int check() {
        if (checked) {
            return 0;
        }
        checked = true;
        ArrayList<CallSite> arrayList = new ArrayList<CallSite>();
        for (Class<?> clazz : PATCHES) {
            Patch patch = clazz.getAnnotation(Patch.class);
            if (patch == null) {
                arrayList.add((CallSite)((Object)(clazz.getSimpleName() + " (no @Patch)")));
                continue;
            }
            if (Compat.hasMethod(patch.className(), patch.methodName())) continue;
            arrayList.add((CallSite)((Object)(patch.className() + "." + patch.methodName() + " (" + clazz.getSimpleName() + ")")));
        }
        for (Class<?> clazz : MEMBERS) {
            if (Compat.hasMember(clazz[0], clazz[1])) continue;
            arrayList.add((CallSite)((Object)(clazz[0] + "." + clazz[1] + " - " + clazz[2] + " stays off")));
        }
        if (arrayList.isEmpty()) {
            System.out.println("[Viewpoint] compat: all " + PATCHES.length + " patch targets and " + MEMBERS.length + " private members found");
        } else {
            System.out.println("[Viewpoint] compat: the game has changed since the mod was written; missing:");
            for (String string : arrayList) {
                System.out.println("[Viewpoint]   " + string);
            }
        }
        return arrayList.size();
    }

    public static void reportFired() {
        if (reported) {
            return;
        }
        reported = true;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < PATCHES.length; ++i) {
            if (fired[i]) continue;
            stringBuilder.append(stringBuilder.length() == 0 ? "" : ", ").append(Compat.name(i)).append(" (").append(WHEN[i]).append(')');
        }
        System.out.println("[Viewpoint] patches seen working: " + Compat.seen() + " of " + PATCHES.length + (String)(stringBuilder.length() == 0 ? "" : "; not yet: " + String.valueOf(stringBuilder) + ". Each is logged when it first runs; one still missing after its situation came up was not applied."));
    }

    private static int seen() {
        int n = 0;
        for (boolean bl : fired) {
            if (!bl) continue;
            ++n;
        }
        return n;
    }

    private static String name(int n) {
        Patch patch = PATCHES[n].getAnnotation(Patch.class);
        return patch == null ? PATCHES[n].getSimpleName() : patch.className().substring(patch.className().lastIndexOf(46) + 1) + "." + patch.methodName();
    }

    private static boolean hasMethod(String string, String string2) {
        try {
            for (Class<?> clazz = Class.forName(string, false, Compat.class.getClassLoader()); clazz != null; clazz = clazz.getSuperclass()) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (!method.getName().equals(string2)) continue;
                    return true;
                }
            }
        }
        catch (ClassNotFoundException | LinkageError throwable) {
            return false;
        }
        return false;
    }

    private static boolean hasMember(String string, String string2) {
        if (string2.endsWith("()")) {
            return Compat.hasMethod(string, string2.substring(0, string2.length() - 2));
        }
        try {
            for (Class<?> clazz = Class.forName(string, false, Compat.class.getClassLoader()); clazz != null; clazz = clazz.getSuperclass()) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (!field.getName().equals(string2)) continue;
                    return true;
                }
            }
        }
        catch (ClassNotFoundException | LinkageError throwable) {
            return false;
        }
        return false;
    }

    private Compat() {
    }
}

