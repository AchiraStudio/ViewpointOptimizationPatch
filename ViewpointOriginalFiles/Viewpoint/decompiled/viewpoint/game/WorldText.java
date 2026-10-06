/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.characters.Capability
 *  zombie.chat.ChatElement
 *  zombie.inventory.types.Radio
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoGridSquare
 *  zombie.iso.IsoObject
 *  zombie.iso.objects.IsoRadio
 *  zombie.iso.objects.IsoTelevision
 *  zombie.iso.objects.IsoWaveSignal
 *  zombie.network.GameClient
 *  zombie.radio.devices.DeviceData
 *  zombie.scripting.objects.CharacterTrait
 *  zombie.ui.TextDrawObject
 *  zombie.vehicles.BaseVehicle
 *  zombie.vehicles.VehiclePart
 */
package viewpoint.game;

import java.lang.reflect.Field;
import java.util.ArrayList;
import viewpoint.core.Frame;
import viewpoint.game.WorldToScreen;
import viewpoint.input.FreeCam;
import viewpoint.platform.Tuning;
import viewpoint.world.WorldMesher;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatElement;
import zombie.inventory.types.Radio;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoWaveSignal;
import zombie.network.GameClient;
import zombie.radio.devices.DeviceData;
import zombie.scripting.objects.CharacterTrait;
import zombie.ui.TextDrawObject;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public final class WorldText {
    private static final Field NOTE = WorldText.field("haloNote");
    private static final Field NOTE_TIME = WorldText.field("haloDispTime");
    private static final int NOTE_GAP = 2;
    private static final WorldToScreen screen = new WorldToScreen();
    private static final float[] pixel = new float[2];

    public static void snapshot(Frame frame, IsoCell isoCell, int n) {
        Object object;
        IsoGameCharacter isoGameCharacter;
        float f = IsoCamera.getScreenWidth(n);
        float f2 = IsoCamera.getScreenHeight(n);
        boolean bl = screen.set(frame, FreeCam.active ? FreeCam.place : null, f, f2);
        for (IsoMovingObject isoMovingObject : isoCell.getObjectList()) {
            if (isoMovingObject instanceof IsoGameCharacter) {
                isoGameCharacter = (IsoGameCharacter)isoMovingObject;
                WorldText.character(isoGameCharacter, n, bl, f, f2);
                continue;
            }
            if (!(isoMovingObject instanceof BaseVehicle)) continue;
            object = (BaseVehicle)isoMovingObject;
            WorldText.vehicle(object, n);
        }
        ArrayList arrayList = isoCell.getStaticUpdaterObjectList();
        for (int i = 0; i < arrayList.size(); ++i) {
            object = arrayList.get(i);
            if (!(object instanceof IsoWaveSignal)) continue;
            isoGameCharacter = (IsoWaveSignal)object;
            WorldText.device((IsoWaveSignal)isoGameCharacter, n);
        }
    }

    private static void character(IsoGameCharacter isoGameCharacter, int n, boolean bl, float f, float f2) {
        boolean bl2 = isoGameCharacter == IsoPlayer.getInstance();
        TextDrawObject textDrawObject = bl2 ? WorldText.note(isoGameCharacter) : null;
        ChatElement chatElement = isoGameCharacter.getChatElement();
        if (textDrawObject == null && !chatElement.getHasChatToDisplay()) {
            return;
        }
        if (bl2 && bl) {
            WorldText.pixel[0] = f * 0.5f;
            WorldText.pixel[1] = f2 * (0.5f - Tuning.subtitleRise);
        } else {
            float f3;
            float f4 = f3 = isoGameCharacter.getVehicle() != null ? Tuning.vehicleTextHeight : Tuning.headTextHeight;
            if (!screen.pixel(isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ(), f3, pixel)) {
                return;
            }
        }
        int n2 = (int)pixel[0];
        int n3 = (int)pixel[1];
        if (textDrawObject != null) {
            textDrawObject.AddBatchedDraw((double)n2, (double)(n3 -= textDrawObject.getHeight() + 2), true, WorldText.noteAlpha(isoGameCharacter));
        }
        if (!isoGameCharacter.isInvisible() || isoGameCharacter == IsoCamera.frameState.camCharacter || WorldText.seesStats()) {
            chatElement.renderBatched(n, n2, n3, WorldText.radioUnheard(isoGameCharacter));
        }
    }

    private static void vehicle(BaseVehicle baseVehicle, int n) {
        for (int i = 0; i < baseVehicle.getPartCount(); ++i) {
            VehiclePart vehiclePart = baseVehicle.getPartByIndex(i);
            ChatElement chatElement = vehiclePart.getChatElement();
            if (chatElement == null || !chatElement.getHasChatToDisplay()) continue;
            if (vehiclePart.getDeviceData() != null && !vehiclePart.getDeviceData().getIsTurnedOn()) {
                chatElement.clear(n);
                continue;
            }
            if (!screen.pixel(baseVehicle.getX(), baseVehicle.getY(), baseVehicle.getZ(), Tuning.vehicleTextHeight, pixel)) continue;
            chatElement.renderBatched(n, (int)pixel[0], (int)pixel[1]);
        }
    }

    private static void device(IsoWaveSignal isoWaveSignal, int n) {
        ChatElement chatElement = isoWaveSignal.getChatElement();
        if (!chatElement.getHasChatToDisplay()) {
            return;
        }
        DeviceData deviceData = isoWaveSignal.getDeviceData();
        boolean bl = IsoPlayer.getInstance().hasTrait(CharacterTrait.DEAF);
        IsoGridSquare isoGridSquare = isoWaveSignal.getSquare();
        if (deviceData != null && !deviceData.getIsTurnedOn() || bl && isoWaveSignal instanceof IsoRadio || bl && isoWaveSignal instanceof IsoTelevision && isoGridSquare != null && !isoGridSquare.isSeen(n)) {
            chatElement.clear(n);
            return;
        }
        float f = WorldMesher.rise((IsoObject)isoWaveSignal) + Tuning.deviceTextHeight;
        if (screen.pixel(isoWaveSignal.getX() + 0.5f, isoWaveSignal.getY() + 0.5f, isoWaveSignal.getZ(), f, pixel)) {
            chatElement.renderBatched(n, (int)pixel[0], (int)pixel[1]);
        }
    }

    private static boolean radioUnheard(IsoGameCharacter isoGameCharacter) {
        Radio radio = isoGameCharacter.getEquipedRadio();
        DeviceData deviceData = radio == null ? null : radio.getDeviceData();
        return deviceData != null && (isoGameCharacter != IsoPlayer.getInstance() && deviceData.getHeadphoneType() >= 0 || !deviceData.getIsTurnedOn());
    }

    private static boolean seesStats() {
        IsoPlayer isoPlayer;
        IsoGameCharacter isoGameCharacter;
        return GameClient.client && (isoGameCharacter = IsoCamera.getCameraCharacter()) instanceof IsoPlayer && (isoPlayer = (IsoPlayer)((Object)isoGameCharacter)).getRole() != null && isoPlayer.getRole().hasCapability(Capability.CanSeePlayersStats);
    }

    private static TextDrawObject note(IsoGameCharacter isoGameCharacter) {
        if (NOTE == null || NOTE_TIME == null || isoGameCharacter.getHaloTimerCount() <= 0.0f) {
            return null;
        }
        try {
            return (TextDrawObject)NOTE.get((Object)isoGameCharacter);
        }
        catch (IllegalAccessException illegalAccessException) {
            return null;
        }
    }

    private static float noteAlpha(IsoGameCharacter isoGameCharacter) {
        try {
            return Math.min(1.0f, isoGameCharacter.getHaloTimerCount() / (NOTE_TIME.getFloat((Object)isoGameCharacter) / 4.0f));
        }
        catch (IllegalAccessException illegalAccessException) {
            return 1.0f;
        }
    }

    private static Field field(String string) {
        try {
            Field field = IsoGameCharacter.class.getDeclaredField(string);
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            System.out.println("[Viewpoint] world text: the player's notes cannot be shown: " + String.valueOf(exception));
            return null;
        }
    }

    private WorldText() {
    }
}

