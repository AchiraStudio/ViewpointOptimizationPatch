/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.ZomboidFileSystem
 *  zombie.core.Color
 *  zombie.core.opengl.RenderSettings
 *  zombie.core.opengl.RenderSettings$PlayerRenderSettings
 *  zombie.core.textures.ColorInfo
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoGridSquare$ILighting
 *  zombie.iso.IsoGridSquare$ResultLight
 *  zombie.iso.IsoLightSource
 *  zombie.iso.IsoObject
 *  zombie.iso.IsoRoomLight
 *  zombie.iso.SpriteDetails.IsoObjectType
 *  zombie.iso.objects.IsoCurtain
 *  zombie.iso.objects.IsoDoor
 *  zombie.iso.objects.IsoThumpable
 *  zombie.iso.objects.IsoWindow
 *  zombie.iso.weather.ClimateManager
 */
package viewpoint.light;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import viewpoint.light.Flashlight;
import viewpoint.light.GridTexels;
import viewpoint.light.LayoutCapture;
import viewpoint.light.LightRecording;
import viewpoint.light.OwnedLight;
import viewpoint.platform.SettingsWindow;
import viewpoint.visibility.Edges;
import zombie.GameTime;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.core.Color;
import zombie.core.opengl.RenderSettings;
import zombie.core.textures.ColorInfo;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoObject;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.weather.ClimateManager;

public final class LightRecorder {
    private static final String FOLDER = "viewpoint-recordings";
    private static final int RING = 5;
    private static final SettingsWindow.Button RECORD = SettingsWindow.button("Debug/Profiling", "Record light", "The engine's light around you, with everything it was given, saved to Zomboid/viewpoint-recordings to fit the light model. Look around first: the engine lights only what you see.");

    public static void poll(IsoCell isoCell, int n, float f, float f2, float f3) {
        if (RECORD.take()) {
            LightRecorder.record(isoCell, n, f, f2, f3);
        }
    }

    private static void record(IsoCell isoCell, int n, float f, float f2, float f3) {
        try {
            List<LightRecording.Square> list = LightRecorder.squares(isoCell, n, f, f2);
            List<LightRecording.Lamp> list2 = LightRecorder.lamps(isoCell);
            List<LightRecording.RoomLight> list3 = LightRecorder.rooms(isoCell);
            List<LightRecording.Torch> list4 = LightRecorder.torches();
            Path path = new File(ZomboidFileSystem.instance.getCacheDir(), FOLDER).toPath();
            Files.createDirectories(path, new FileAttribute[0]);
            Path path2 = path.resolve("light-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".lightrec");
            LightRecording.write(path2, new LightRecording.Recording(f, f2, f3, LightRecorder.world(n), list2, list3, list4, list));
            int n2 = LightRecorder.inView(list);
            System.out.println("[Viewpoint] light recording: " + list.size() + " squares (" + n2 + " in view), " + list2.size() + " lamps, " + list3.size() + " room lights, " + list4.size() + " torches, at hour " + String.format("%.2f", Float.valueOf(GameTime.getInstance().getTimeOfDay())) + " into " + String.valueOf(path2));
            RECORD.status("saved " + String.valueOf(path2.getFileName()) + ": " + list.size() + " squares, " + n2 + " in view");
        }
        catch (IOException | RuntimeException exception) {
            System.out.println("[Viewpoint] light recording failed: " + String.valueOf(exception));
            RECORD.status("failed: " + exception.getMessage());
        }
    }

    private static List<LightRecording.Square> squares(IsoCell isoCell, int n, float f, float f2) {
        IsoChunkMap isoChunkMap = isoCell.getChunkMap(n);
        int n2 = Math.floorDiv((int)Math.floor(f), 8);
        int n3 = Math.floorDiv((int)Math.floor(f2), 8);
        ArrayList<LightRecording.Square> arrayList = new ArrayList<LightRecording.Square>();
        for (int i = n3 - 5; i <= n3 + 5; ++i) {
            for (int j = n2 - 5; j <= n2 + 5; ++j) {
                IsoChunk isoChunk = isoChunkMap.getChunkForGridSquare(j * 8, i * 8);
                if (isoChunk == null || !isoChunk.loaded || isoChunk.wx != j || isoChunk.wy != i) continue;
                for (int k = isoChunk.minLevel; k <= isoChunk.maxLevel; ++k) {
                    for (int i2 = 0; i2 < 8; ++i2) {
                        for (int i3 = 0; i3 < 8; ++i3) {
                            IsoGridSquare isoGridSquare = isoChunk.getGridSquare(i3, i2, k);
                            if (isoGridSquare == null) continue;
                            arrayList.add(LightRecorder.square(isoGridSquare, n));
                        }
                    }
                }
            }
        }
        return arrayList;
    }

    private static LightRecording.Square square(IsoGridSquare isoGridSquare, int n) {
        int n2;
        IsoGridSquare.ILighting iLighting = isoGridSquare.lighting[n];
        ColorInfo colorInfo = iLighting == null ? null : iLighting.lightInfo();
        int[] nArray = new int[8];
        LightRecording.Reached[] reachedArray = new LightRecording.Reached[]{};
        if (iLighting != null) {
            for (n2 = 0; n2 < nArray.length; ++n2) {
                nArray[n2] = iLighting.lightverts(n2);
            }
            reachedArray = LightRecorder.reached(iLighting);
        }
        n2 = isoGridSquare.getX();
        int n3 = isoGridSquare.getY();
        int n4 = isoGridSquare.getZ();
        return new LightRecording.Square(n2, n3, n4, LightRecorder.flags(isoGridSquare, iLighting), LayoutCapture.passes(isoGridSquare), isoGridSquare.getBuildingDef() == null ? -1L : isoGridSquare.getBuildingDef().getID(), isoGridSquare.getRoom() == null ? -1L : isoGridSquare.getRoomID(), isoGridSquare.lightLevel, colorInfo == null ? 0 : GridTexels.pack(colorInfo.r, colorInfo.g, colorInfo.b), iLighting == null ? 0.0f : iLighting.darkMulti(), iLighting == null ? 0.0f : iLighting.targetDarkMulti(), nArray, reachedArray, OwnedLight.lightAt(n2, n3, n4));
    }

    private static int recorded(int n) {
        return n == 4 ? 2 : n;
    }

    private static int flags(IsoGridSquare isoGridSquare, IsoGridSquare.ILighting iLighting) {
        int n = (isoGridSquare.isOutside() ? 1 : 0) | (isoGridSquare.haveRoof ? 2 : 0) | (isoGridSquare.getOpenAir() ? 4 : 0) | (isoGridSquare.isSolidFloor() || isoGridSquare.TreatAsSolidFloor() ? 8 : 0) | (isoGridSquare.HasStairs() ? 16 : 0) | (LightRecorder.elevated(isoGridSquare) ? 32 : 0) | LightRecorder.recorded(Edges.edge(isoGridSquare, true)) << 20 | LightRecorder.recorded(Edges.edge(isoGridSquare, false)) << 22 | (LightRecorder.doorOpen(isoGridSquare.getDoor(Edges.facing(true))) ? 512 : 0) | (LightRecorder.doorOpen(isoGridSquare.getDoor(Edges.facing(false))) ? 1024 : 0);
        if (iLighting != null) {
            n |= (iLighting.bSeen() ? 64 : 0) | (iLighting.bCanSee() ? 128 : 0) | (iLighting.bCouldSee() ? 256 : 0);
        }
        for (IsoObject isoObject : isoGridSquare.getSpecialObjects()) {
            IsoCurtain isoCurtain;
            if (isoObject instanceof IsoWindow) {
                IsoWindow isoWindow = (IsoWindow)isoObject;
                boolean bl = isoWindow.isNorth();
                n |= bl ? 2048 : 4096;
                n |= !isoWindow.IsOpen() ? 0 : (bl ? 8192 : 16384);
                continue;
            }
            if (!(isoObject instanceof IsoCurtain) || (isoCurtain = (IsoCurtain)isoObject).IsOpen()) continue;
            n |= LightRecorder.curtain(isoCurtain.getType());
        }
        return n;
    }

    private static boolean elevated(IsoGridSquare isoGridSquare) {
        return isoGridSquare.has(IsoObjectType.stairsTN) || isoGridSquare.has(IsoObjectType.stairsMN) || isoGridSquare.has(IsoObjectType.stairsTW) || isoGridSquare.has(IsoObjectType.stairsMW);
    }

    private static boolean doorOpen(IsoObject isoObject) {
        IsoThumpable isoThumpable;
        IsoDoor isoDoor;
        return isoObject instanceof IsoDoor && (isoDoor = (IsoDoor)isoObject).IsOpen() || isoObject instanceof IsoThumpable && (isoThumpable = (IsoThumpable)isoObject).isDoor() && isoThumpable.IsOpen();
    }

    private static int curtain(IsoObjectType isoObjectType) {
        return isoObjectType == IsoObjectType.curtainW ? 32768 : (isoObjectType == IsoObjectType.curtainN ? 65536 : (isoObjectType == IsoObjectType.curtainE ? 131072 : (isoObjectType == IsoObjectType.curtainS ? 262144 : 0)));
    }

    private static LightRecording.Reached[] reached(IsoGridSquare.ILighting iLighting) {
        LightRecording.Reached[] reachedArray = new LightRecording.Reached[iLighting.resultLightCount()];
        for (int i = 0; i < reachedArray.length; ++i) {
            IsoGridSquare.ResultLight resultLight = iLighting.getResultLight(i);
            reachedArray[i] = new LightRecording.Reached(resultLight.id, resultLight.x, resultLight.y, resultLight.z, resultLight.radius, resultLight.r, resultLight.g, resultLight.b, resultLight.flags);
        }
        return reachedArray;
    }

    private static List<LightRecording.Lamp> lamps(IsoCell isoCell) {
        ArrayList<LightRecording.Lamp> arrayList = new ArrayList<LightRecording.Lamp>();
        for (IsoLightSource isoLightSource : isoCell.getLamppostPositions()) {
            boolean bl = !isoLightSource.switches.isEmpty() && ((IsoLightSwitch)((Object)isoLightSource.switches.get((int)0))).streetLight;
            long l = isoLightSource.localToBuilding == null || isoLightSource.localToBuilding.def == null ? -1L : isoLightSource.localToBuilding.def.getID();
            arrayList.add(new LightRecording.Lamp(isoLightSource.id, isoLightSource.x, isoLightSource.y, isoLightSource.z, isoLightSource.radius, isoLightSource.r, isoLightSource.g, isoLightSource.b, isoLightSource.active, isoLightSource.life, isoLightSource.hydroPowered, l, bl, isoLightSource.switches.size()));
        }
        return arrayList;
    }

    private static List<LightRecording.RoomLight> rooms(IsoCell isoCell) {
        ArrayList<LightRecording.RoomLight> arrayList = new ArrayList<LightRecording.RoomLight>();
        for (IsoRoomLight isoRoomLight : isoCell.roomLights) {
            long l = isoRoomLight.room == null || isoRoomLight.room.def == null ? -1L : isoRoomLight.room.def.getID();
            long l2 = isoRoomLight.room == null || isoRoomLight.room.building == null || isoRoomLight.room.building.def == null ? -1L : isoRoomLight.room.building.def.getID();
            arrayList.add(new LightRecording.RoomLight(isoRoomLight.id, isoRoomLight.x, isoRoomLight.y, isoRoomLight.z, isoRoomLight.width, isoRoomLight.height, isoRoomLight.active, l, l2));
        }
        return arrayList;
    }

    private static List<LightRecording.Torch> torches() {
        ArrayList<IsoGameCharacter.TorchInfo> arrayList = new ArrayList<IsoGameCharacter.TorchInfo>();
        LightingJNI.getTorches(arrayList);
        ArrayList<LightRecording.Torch> arrayList2 = new ArrayList<LightRecording.Torch>(arrayList.size());
        for (IsoGameCharacter.TorchInfo torchInfo : arrayList) {
            arrayList2.add(new LightRecording.Torch(torchInfo.id, torchInfo.x, torchInfo.y, torchInfo.z, torchInfo.r, torchInfo.g, torchInfo.b, torchInfo.angleX, torchInfo.angleY, torchInfo.dist, torchInfo.strength, torchInfo.cone, torchInfo.dot, torchInfo.focusing));
        }
        return arrayList2;
    }

    private static Map<String, Float> world(int n) {
        LinkedHashMap<String, Float> linkedHashMap = new LinkedHashMap<String, Float>();
        GameTime gameTime = GameTime.getInstance();
        RenderSettings.PlayerRenderSettings playerRenderSettings = RenderSettings.getInstance().getPlayerSettings(n);
        ClimateManager climateManager = ClimateManager.getInstance();
        linkedHashMap.put("player", Float.valueOf(n));
        linkedHashMap.put("timeOfDay", Float.valueOf(gameTime.getTimeOfDay()));
        linkedHashMap.put("night", Float.valueOf(gameTime.getNight()));
        linkedHashMap.put("skyLightLevel", Float.valueOf(gameTime.getSkyLightLevel()));
        linkedHashMap.put("ambientForPlayer", Float.valueOf(RenderSettings.getInstance().getAmbientForPlayer(n)));
        linkedHashMap.put("settings.ambient", Float.valueOf(playerRenderSettings.getAmbient()));
        linkedHashMap.put("settings.night", Float.valueOf(playerRenderSettings.getNight()));
        linkedHashMap.put("settings.viewDistance", Float.valueOf(playerRenderSettings.getViewDistance()));
        linkedHashMap.put("settings.rmod", Float.valueOf(playerRenderSettings.getRmod()));
        linkedHashMap.put("settings.gmod", Float.valueOf(playerRenderSettings.getGmod()));
        linkedHashMap.put("settings.bmod", Float.valueOf(playerRenderSettings.getBmod()));
        linkedHashMap.put("climate.ambient", Float.valueOf(climateManager.getAmbient()));
        linkedHashMap.put("climate.dayLightStrength", Float.valueOf(climateManager.getDayLightStrength()));
        linkedHashMap.put("climate.nightStrength", Float.valueOf(climateManager.getNightStrength()));
        linkedHashMap.put("climate.globalLightIntensity", Float.valueOf(climateManager.getGlobalLightIntensity()));
        LightRecorder.colour(linkedHashMap, "climate.exterior", climateManager.getGlobalLight().getExterior());
        LightRecorder.colour(linkedHashMap, "climate.interior", climateManager.getGlobalLight().getInterior());
        linkedHashMap.put("hydroPower", Float.valueOf(IsoWorld.instance.isHydroPowerOn() ? 1.0f : 0.0f));
        linkedHashMap.put("flashlight", Float.valueOf(Flashlight.active ? 1.0f : 0.0f));
        return linkedHashMap;
    }

    private static void colour(Map<String, Float> map, String string, Color color) {
        map.put(string + ".r", Float.valueOf(color.r));
        map.put(string + ".g", Float.valueOf(color.g));
        map.put(string + ".b", Float.valueOf(color.b));
        map.put(string + ".a", Float.valueOf(color.a));
    }

    private static int inView(List<LightRecording.Square> list) {
        int n = 0;
        for (LightRecording.Square square : list) {
            if ((square.flags() & 0x80) == 0) continue;
            ++n;
        }
        return n;
    }

    private LightRecorder() {
    }
}

