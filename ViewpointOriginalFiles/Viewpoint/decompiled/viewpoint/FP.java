/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.SpriteRenderer
 *  zombie.iso.IsoCell
 */
package viewpoint;

import viewpoint.Compat;
import viewpoint.SceneDrawer;
import viewpoint.Screenshot;
import viewpoint.core.Frame;
import viewpoint.core.View;
import viewpoint.environment.Atmosphere;
import viewpoint.environment.Weather;
import viewpoint.far.BlockLight;
import viewpoint.far.CellLight;
import viewpoint.far.FarWorld;
import viewpoint.game.IsoChunkTextures;
import viewpoint.game.LightCheck;
import viewpoint.game.Teleport;
import viewpoint.game.WorldText;
import viewpoint.input.Controls;
import viewpoint.input.CrosshairAim;
import viewpoint.input.FreeCam;
import viewpoint.input.Look;
import viewpoint.input.ThirdPerson;
import viewpoint.interact.LootMenu;
import viewpoint.interact.MouseTargets;
import viewpoint.light.Daylight;
import viewpoint.light.Flashlight;
import viewpoint.light.LightRecorder;
import viewpoint.light.LightSources;
import viewpoint.light.Lights;
import viewpoint.light.Torches;
import viewpoint.models.CharacterRate;
import viewpoint.models.Characters;
import viewpoint.models.Corpses;
import viewpoint.models.ModelCull;
import viewpoint.models.ModelMotion;
import viewpoint.models.Models;
import viewpoint.platform.BuildPin;
import viewpoint.platform.Caches;
import viewpoint.platform.HotReload;
import viewpoint.platform.Keys;
import viewpoint.platform.Onboarding;
import viewpoint.platform.Profile;
import viewpoint.platform.Settings;
import viewpoint.platform.SettingsWindow;
import viewpoint.platform.Threads;
import viewpoint.render.Latency;
import viewpoint.render.LodView;
import viewpoint.render.Retirement;
import viewpoint.render.WorldRenderer;
import viewpoint.visibility.Glass;
import viewpoint.visibility.Rooms;
import viewpoint.world.ChunkCache;
import viewpoint.world.MeshRecorder;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.core.SpriteRenderer;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunkMap;
import zombie.ui.UIManager;

public final class FP {
    private static boolean cursorMode;
    private static boolean settingsShown;
    private static boolean paused;
    private static boolean cursorBeforePause;
    private static IsoCell lastCell;
    private static long enabledAt;
    private static long frameNumber;
    private static final Frame[] frames;
    private static final SceneDrawer[] drawers;
    private static int reloads;

    public static boolean renderWorld(IsoCell isoCell) {
        int n = IsoCamera.frameState.playerIndex;
        if (n != 0) {
            return false;
        }
        Threads.game();
        Retirement.building(++frameNumber);
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        FP.devKeys();
        FP.toggle(isoGameCharacter);
        if (isoCell != lastCell) {
            lastCell = isoCell;
            FP.resetCaches();
        }
        if (reloads != HotReload.generation) {
            reloads = HotReload.generation;
            FP.resetCaches();
            FarWorld.clear();
            System.out.println("[Viewpoint] hot reload: the caches start over");
        }
        if (isoGameCharacter != null) {
            LightRecorder.poll(isoCell, n, isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ());
            Teleport.poll(isoCell, isoGameCharacter);
        }
        IsoChunkTextures.update(View.enabled && isoGameCharacter != null);
        if (!View.enabled || isoGameCharacter == null) {
            Look.wantCapture = false;
            MouseTargets.frame(false);
            CrosshairAim.off();
            CharacterRate.clear();
            return false;
        }
        if (enabledAt != 0L && System.nanoTime() - enabledAt > 10000000000L) {
            enabledAt = 0L;
            Compat.reportFired();
        }
        FP.cursorMode();
        ThirdPerson.poll();
        FreeCam.keys();
        if (Keys.SCREENSHOT.pressed()) {
            Screenshot.requested = true;
        }
        Look.wantCapture = !cursorMode && !isoGameCharacter.isDead() && !UIManager.isModalVisible();
        MouseTargets.frame(!Look.captured);
        int n2 = SpriteRenderer.instance.getMainStateIndex();
        Frame frame = frames[n2];
        try {
            FP.snapshot(frame, isoCell, isoGameCharacter, n);
        }
        catch (Throwable throwable) {
            View.enabled = false;
            Look.wantCapture = false;
            Characters.release(frame);
            Corpses.release(frame);
            System.out.println("[Viewpoint] disabled after error while building the scene:");
            throwable.printStackTrace(System.out);
            return false;
        }
        SpriteRenderer.instance.drawGeneric((TextureDraw.GenericDrawer)drawers[n2]);
        return true;
    }

    private static void cursorMode() {
        if (Controls.middlePressed()) {
            boolean bl = cursorMode = !cursorMode;
        }
        if (LootMenu.freeCursor()) {
            cursorMode = true;
        }
        if (LootMenu.lookAgain()) {
            cursorMode = false;
        }
        if (SettingsWindow.shown() && !settingsShown) {
            cursorMode = true;
        }
        settingsShown = SettingsWindow.shown();
        boolean bl = GameTime.isGamePaused();
        if (bl && !paused) {
            cursorBeforePause = cursorMode;
            cursorMode = true;
        } else if (!bl && paused) {
            cursorMode = cursorBeforePause;
        }
        paused = bl;
    }

    private static void devKeys() {
        if (Keys.LOD_VIEW.pressed()) {
            LodView.flip();
        }
        if (Keys.DEBUG_VIEW.pressed()) {
            WorldRenderer.debugView = (WorldRenderer.debugView + 1) % 7;
            System.out.println("[Viewpoint] debug view " + WorldRenderer.debugView + " (0 normal, 1 art only, 2 light only, 3 volumetric light x4, 4 bounce light x4, 5 lamp shadows, 6 the sun's maps)");
        }
        if (Keys.ISOLATE.pressed()) {
            WorldRenderer.isolate = (WorldRenderer.isolate + 1) % WorldRenderer.ISOLATE_NAMES.length;
            System.out.println("[Viewpoint] isolate: " + WorldRenderer.ISOLATE_NAMES[WorldRenderer.isolate]);
        }
        if (Keys.ROOMS.pressed()) {
            Rooms.cycle();
        }
    }

    private static void toggle(IsoGameCharacter isoGameCharacter) {
        boolean bl = Keys.FIRST_PERSON.pressed();
        if (bl && Onboarding.blocks()) {
            Onboarding.show();
            return;
        }
        if ((bl |= Onboarding.takeEnter() && !View.enabled) && !BuildPin.supported()) {
            BuildPin.reportRefused();
        } else if (bl && isoGameCharacter != null && !SceneDrawer.failed) {
            View.enabled = !View.enabled;
            cursorMode = false;
            FP.resetCaches();
            Look.yaw = isoGameCharacter.getDirectionAngleRadians();
            Look.pitch = 0.0f;
            if (View.enabled) {
                Compat.check();
                enabledAt = System.nanoTime();
            }
        }
    }

    private static void snapshot(Frame frame, IsoCell isoCell, IsoGameCharacter isoGameCharacter, int n) {
        frame.number = frameNumber;
        frame.scene.reset();
        frame.modelObjects.clear();
        frame.modelItems.clear();
        Characters.release(frame);
        Corpses.release(frame);
        Models.release(frame);
        float f = (float)IsoCamera.getScreenWidth(n) / (float)IsoCamera.getScreenHeight(n);
        frame.scene.projection.setPerspective(View.fovY(ThirdPerson.active), f, 0.05f, 400.0f);
        FreeCam.Place place = FreeCam.centre();
        frame.onCamera = place != null;
        frame.camX = frame.onCamera ? (float)place.x() : isoGameCharacter.getX();
        frame.camY = frame.onCamera ? (float)place.y() : isoGameCharacter.getY();
        frame.camZ = frame.onCamera ? (float)place.z() : isoGameCharacter.getZ();
        frame.viewYaw = frame.onCamera ? place.yaw() : Look.yaw;
        frame.viewPitch = frame.onCamera ? place.pitch() : Look.pitch;
        frame.scene.originX = -frame.camX;
        frame.scene.originY = frame.camZ * 2.4494896f;
        frame.scene.originZ = -frame.camY;
        Controls.eye(isoGameCharacter, frame);
        ThirdPerson.capture(frame, isoCell, isoGameCharacter);
        CrosshairAim.snapshot(frame, isoCell, isoGameCharacter);
        Glass.snapshot(frame);
        Controls.traceState(isoGameCharacter);
        Weather.snapshot(frame, isoCell);
        Atmosphere.apply(frame.scene);
        Flashlight.snapshot(frame, isoGameCharacter);
        long l = System.nanoTime();
        Profile.mainFrame(l);
        ModelCull.reportCull(isoCell);
        ChunkCache.update(frame, isoCell, n);
        MeshRecorder.poll(isoCell, n, frame.camX, frame.camY, frame.camZ);
        LootMenu.snapshot(frame, isoGameCharacter);
        long l2 = System.nanoTime();
        FarWorld.update(frame);
        BlockLight.update(frame);
        CellLight.update(frame);
        long l3 = System.nanoTime();
        ModelMotion.begin(frame.scene.models);
        Characters.snapshot(frame, isoCell, isoGameCharacter);
        Characters.outlinesShown(isoCell, n);
        Models.snapshot(frame, n);
        Corpses.capture(frame);
        ModelMotion.end(frame.scene.models);
        Lights.snapshot(frame, isoCell);
        Torches.snapshot(frame, isoGameCharacter, n);
        Daylight.snapshot(frame.scene);
        WorldText.snapshot(frame, isoCell, n);
        Caches.balance();
        frame.takenNanos = System.nanoTime();
        Latency.snapshotTaken(frame.number);
        Profile.cpu(l2 - l, l3 - l2, frame.takenNanos - l3, frame.scene.meshCount);
    }

    public static void gameLoaded() {
        if (!BuildPin.supported()) {
            return;
        }
        Settings.load();
        FarWorld.clear();
        Atmosphere.gameLoaded();
        System.out.println("[Viewpoint] chunk grid " + IsoChunkMap.chunkGridWidth + " x " + IsoChunkMap.chunkGridWidth + " (the game's own; the near world reaches " + View.radiusChunks() * 8 + " squares)");
    }

    private static void resetCaches() {
        ChunkCache.clear();
        LightSources.forget();
        LightCheck.clear();
        CharacterRate.clear();
        Corpses.forget();
    }

    private FP() {
    }

    static {
        frames = new Frame[]{new Frame(), new Frame(), new Frame()};
        drawers = new SceneDrawer[]{new SceneDrawer(frames[0]), new SceneDrawer(frames[1]), new SceneDrawer(frames[2])};
    }
}

