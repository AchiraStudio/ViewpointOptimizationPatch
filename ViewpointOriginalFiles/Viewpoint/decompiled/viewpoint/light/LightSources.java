/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoLightSource
 *  zombie.iso.IsoRoomLight
 */
package viewpoint.light;

import java.util.ArrayList;
import java.util.Stack;
import viewpoint.light.LightJobs;
import viewpoint.light.LightMemory;
import viewpoint.light.SourceRegister;
import viewpoint.platform.Profile;
import zombie.iso.IsoCell;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoRoomLight;
import zombie.iso.LightingJNI;

public final class LightSources {
    private static final SourceRegister register = new SourceRegister();
    private static int polled = -1;
    private static int readings;
    private static volatile int lamps;
    private static volatile int lampsOn;
    private static volatile int rooms;
    private static volatile int roomsOn;
    private static volatile int mapLamps;
    private static volatile int mapLampsOn;
    private static volatile int mapRooms;
    private static volatile int mapRoomsOn;
    private static volatile int changes;
    private static volatile int marked;
    private static volatile int rememberedChunks;
    private static volatile int rememberedLamps;
    private static volatile long captureNanos;
    private static int changesReported;
    private static int markedReported;
    private static long captureReported;

    public static void update(IsoCell isoCell, int n) {
        int n2 = LightingJNI.getUpdateCounter(n);
        if (n2 == -1 || n2 == polled) {
            return;
        }
        register.begin(polled == -1 ? null : LightJobs::mark);
        polled = n2;
        ++readings;
        Stack stack = isoCell.getLamppostPositions();
        for (int i = 0; i < stack.size(); ++i) {
            IsoLightSource isoLightSource = (IsoLightSource)stack.get(i);
            if (isoLightSource.id == 0 || isoLightSource.life != -1) continue;
            register.lamp(isoLightSource.id, isoLightSource.x, isoLightSource.y, isoLightSource.z, isoLightSource.radius, LightSources.engine(isoLightSource.r), LightSources.engine(isoLightSource.g), LightSources.engine(isoLightSource.b), LightSources.building(isoLightSource), isoLightSource.active);
        }
        ArrayList arrayList = isoCell.roomLights;
        for (int i = 0; i < arrayList.size(); ++i) {
            IsoRoomLight isoRoomLight = (IsoRoomLight)arrayList.get(i);
            if (isoRoomLight.id == 0 || isoRoomLight.room == null || isoRoomLight.room.def == null) continue;
            long l = isoRoomLight.room.building == null || isoRoomLight.room.building.def == null ? -1L : isoRoomLight.room.building.def.getID();
            register.room(isoRoomLight.id, isoRoomLight.x, isoRoomLight.y, isoRoomLight.z, isoRoomLight.width, isoRoomLight.height, isoRoomLight.room.def.getID(), l, isoRoomLight.active);
        }
        register.finish();
        LightMemory.capture(isoCell);
        LightSources.publish();
    }

    public static void forget() {
        register.clear();
        polled = -1;
    }

    static SourceRegister register() {
        return register;
    }

    static int readings() {
        return readings;
    }

    static float engine(float f) {
        return Math.max(0.0f, Math.min(1.0f, f * 2.0f));
    }

    static long building(IsoLightSource isoLightSource) {
        return isoLightSource.localToBuilding == null || isoLightSource.localToBuilding.def == null ? -1L : isoLightSource.localToBuilding.def.getID();
    }

    static void publish() {
        lamps = register.lamps();
        lampsOn = register.lampsOn();
        rooms = register.rooms();
        roomsOn = register.roomsOn();
        mapLamps = register.mapLamps();
        mapLampsOn = register.mapLampsOn();
        mapRooms = register.mapRooms();
        mapRoomsOn = register.mapRoomsOn();
        changes += register.takeChanges();
        marked += register.takeMarked();
        rememberedChunks = LightMemory.chunks();
        rememberedLamps = LightMemory.lampsHeld();
        captureNanos += LightMemory.takeCaptureNanos();
    }

    private static String report() {
        int n = changes;
        int n2 = marked;
        long l = captureNanos;
        String string = "lamps " + lamps + " (" + lampsOn + " on), room lights " + rooms + " (" + roomsOn + " on), the map's lamps " + mapLamps + " (" + mapLampsOn + " on) and room lights " + mapRooms + " (" + mapRoomsOn + " on), changes " + (n - changesReported) + ", levels marked " + (n2 - markedReported) + String.format(", remembered chunks %d (%d lamps, captured in %.1f ms)", rememberedChunks, rememberedLamps, (double)(l - captureReported) / 1000000.0);
        changesReported = n;
        markedReported = n2;
        captureReported = l;
        return string;
    }

    private LightSources() {
    }

    static {
        Profile.lightSourcesReport = LightSources::report;
    }

    static interface Marks {
        public void mark(int var1, int var2, int var3);
    }
}

