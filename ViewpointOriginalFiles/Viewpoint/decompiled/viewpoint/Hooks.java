/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  fmod.fmod.SoundListener
 *  zombie.UpdateSchedulerSimulationLevel
 *  zombie.characters.CharacterStat
 *  zombie.characters.Stats
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoObject
 *  zombie.iso.IsoObjectPicker$ClickObject
 *  zombie.iso.Vector2
 *  zombie.iso.fboRenderChunk.FBORenderLevels
 *  zombie.iso.objects.IsoWindow
 *  zombie.iso.sprite.IsoReticle
 *  zombie.vehicles.BaseVehicle
 */
package viewpoint;

import fmod.fmod.SoundListener;
import viewpoint.Compat;
import viewpoint.FP;
import viewpoint.ModLua;
import viewpoint.core.View;
import viewpoint.environment.Lightning;
import viewpoint.game.AnimalSets;
import viewpoint.game.DiagonalWalk;
import viewpoint.game.FrameCaps;
import viewpoint.game.GameFixes;
import viewpoint.game.Hearing;
import viewpoint.game.LightCheck;
import viewpoint.game.SpottedStack;
import viewpoint.input.ArmsPitch;
import viewpoint.input.Controls;
import viewpoint.input.Look;
import viewpoint.interact.LootMenu;
import viewpoint.interact.LootPanel;
import viewpoint.interact.MouseTargets;
import viewpoint.light.Visibility;
import viewpoint.models.CharacterRate;
import viewpoint.models.ModelCull;
import viewpoint.models.Models;
import viewpoint.platform.FrameTimes;
import viewpoint.platform.KeyInput;
import viewpoint.platform.Onboarding;
import viewpoint.platform.PerformanceOverlay;
import viewpoint.platform.SettingsWindow;
import viewpoint.platform.VideoMemory;
import viewpoint.render.Latency;
import viewpoint.world.ChunkCache;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Stats;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCell;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoWindow;
import zombie.iso.sprite.IsoReticle;
import zombie.vehicles.BaseVehicle;

public final class Hooks {
    public static boolean skipIsoCellRender(IsoCell isoCell) {
        Compat.hit(6);
        return FP.renderWorld(isoCell);
    }

    public static boolean skipIsoCursor() {
        Compat.hit(7);
        return View.enabled;
    }

    public static boolean skipIsoReticle(IsoReticle isoReticle) {
        Compat.hit(8);
        return Controls.skipReticle(isoReticle == IsoReticle.getInstance((int)0));
    }

    public static void isoReticleDone() {
        Controls.reticleDone();
    }

    public static boolean skipWeatherFx() {
        Compat.hit(22);
        return View.enabled;
    }

    public static boolean skipDrawGeneric(TextureDraw.GenericDrawer genericDrawer) {
        Compat.hit(3);
        return Models.capture(genericDrawer);
    }

    public static void levelInvalidated(FBORenderLevels fBORenderLevels, int n, long l) {
        Compat.hit(5);
        ChunkCache.invalidated(fBORenderLevels, n, l);
    }

    public static void allLevelsInvalidated(FBORenderLevels fBORenderLevels, long l) {
        Compat.hit(4);
        ChunkCache.invalidated(fBORenderLevels, Integer.MIN_VALUE, l);
    }

    public static void windowToggled(IsoWindow isoWindow) {
        Compat.hit(30);
        ChunkCache.windowChanged(isoWindow);
    }

    public static void windowSmashed(IsoWindow isoWindow) {
        Compat.hit(31);
        ChunkCache.windowChanged(isoWindow);
    }

    public static void windowGlassChanged(IsoWindow isoWindow) {
        Compat.hit(32);
        ChunkCache.windowChanged(isoWindow);
    }

    public static void gameLoaded() {
        Compat.hit(0);
        FP.gameLoaded();
    }

    public static void thunder(int n, int n2, boolean bl) {
        Compat.hit(46);
        Lightning.struck(n, n2, bl);
    }

    public static IsoObjectPicker.ClickObject contextPick(int n, int n2, IsoObjectPicker.ClickObject clickObject) {
        Compat.hit(48);
        return MouseTargets.on() ? MouseTargets.contextPick(n, n2) : clickObject;
    }

    public static IsoObject pickDoor(IsoObject isoObject) {
        return Hooks.picked(49, 0, isoObject);
    }

    public static IsoObject pickWindow(IsoObject isoObject) {
        return Hooks.picked(50, 1, isoObject);
    }

    public static IsoObject pickWindowFrame(IsoObject isoObject) {
        return Hooks.picked(51, 2, isoObject);
    }

    public static IsoObject pickThumpable(IsoObject isoObject) {
        return Hooks.picked(52, 3, isoObject);
    }

    public static IsoObject pickHoppable(IsoObject isoObject) {
        return Hooks.picked(53, 4, isoObject);
    }

    public static IsoObject pickCorpse(IsoObject isoObject) {
        return Hooks.picked(54, 5, isoObject);
    }

    public static IsoObject pickTree(IsoObject isoObject) {
        return Hooks.picked(55, 6, isoObject);
    }

    private static IsoObject picked(int n, int n2, IsoObject isoObject) {
        Compat.hit(n);
        return MouseTargets.on() ? MouseTargets.picked(n2) : isoObject;
    }

    public static BaseVehicle pickVehicle(BaseVehicle baseVehicle) {
        Compat.hit(56);
        return MouseTargets.on() ? MouseTargets.vehicle() : baseVehicle;
    }

    public static void soundListener(SoundListener soundListener) {
        Compat.hit(47);
        Hearing.listen(soundListener);
    }

    public static float cameraZoom(float f) {
        Compat.hit(57);
        return Hearing.cameraZoom(f);
    }

    public static void lightingUpdateStarted() {
        Compat.hit(9);
        Visibility.lightingUpdate = true;
    }

    public static void lightingUpdateEnded() {
        Visibility.lightingUpdate = false;
    }

    public static boolean skipLightCheck() {
        Compat.hit(42);
        return LightCheck.skipCheck();
    }

    public static boolean skipSwitchPower(IsoLightSwitch isoLightSwitch) {
        Compat.hit(43);
        return LightCheck.skipPower(isoLightSwitch);
    }

    public static boolean switchPower(IsoLightSwitch isoLightSwitch, boolean bl) {
        return LightCheck.power(isoLightSwitch, bl);
    }

    public static float viewDistance(float f) {
        Compat.hit(19);
        return Visibility.viewDistance(f);
    }

    public static float viewDistMax(float f) {
        Compat.hit(20);
        return Visibility.viewDistance(f);
    }

    public static boolean skipAnimalCull(IsoWorld isoWorld) {
        Compat.hit(1);
        return ModelCull.cullAnimals(isoWorld);
    }

    public static void modelSlotUpdating(ModelManager.ModelSlot modelSlot) {
        Compat.hit(44);
        CharacterRate.updating(modelSlot);
    }

    public static void modelSlotUpdated() {
        CharacterRate.updated();
    }

    public static void zombieCullStarted() {
        Compat.hit(27);
        ModelCull.zombieCull(true);
    }

    public static void zombieCullEnded() {
        ModelCull.zombieCull(false);
    }

    public static boolean couldSeeHeadSquare(IsoZombie isoZombie, IsoPlayer isoPlayer, boolean bl) {
        Compat.hit(28);
        return ModelCull.headSquareSeen(isoZombie, isoPlayer, bl);
    }

    public static int zombieScreenX(IsoZombie isoZombie, int n, int n2) {
        Compat.hit(23);
        return ModelCull.cullScreenX(isoZombie, n, n2);
    }

    public static int zombieScreenY(int n, int n2) {
        Compat.hit(24);
        return ModelCull.cullScreenY(n, n2);
    }

    public static UpdateSchedulerSimulationLevel simulationLevel(IsoMovingObject isoMovingObject, UpdateSchedulerSimulationLevel updateSchedulerSimulationLevel) {
        Compat.hit(25);
        return ModelCull.simulationLevel(isoMovingObject, updateSchedulerSimulationLevel);
    }

    public static void inputMoveVector(IsoPlayer isoPlayer, Vector2 vector2) {
        Compat.hit(13);
        Controls.moveVector(isoPlayer, vector2);
    }

    public static void movementStarting(IsoPlayer isoPlayer) {
        Compat.hit(12);
        Controls.beforeMovement(isoPlayer);
    }

    public static void movementDone(IsoPlayer isoPlayer) {
        Controls.afterMovement(isoPlayer);
    }

    public static void rootMotion(IsoPlayer isoPlayer, Vector2 vector2, boolean bl) {
        Compat.hit(60);
        Controls.rootMotion(isoPlayer, vector2, bl);
    }

    public static float lookAngle(IsoGameCharacter isoGameCharacter, float f) {
        Compat.hit(10);
        return Controls.lookAngle(isoGameCharacter, f);
    }

    public static float visionCone(IsoGameCharacter isoGameCharacter, float f) {
        Compat.hit(21);
        return Controls.visionCone(isoGameCharacter, f);
    }

    public static boolean running(IsoGameCharacter isoGameCharacter, boolean bl) {
        Compat.hit(16);
        return Controls.running(isoGameCharacter, bl);
    }

    public static boolean sprinting(IsoGameCharacter isoGameCharacter, boolean bl) {
        Compat.hit(17);
        return Controls.running(isoGameCharacter, bl);
    }

    public static boolean strafing(IsoGameCharacter isoGameCharacter, boolean bl) {
        Compat.hit(18);
        return Controls.strafing(isoGameCharacter, bl);
    }

    public static int reticleX(int n, int n2) {
        Compat.hit(14);
        return Controls.reticle(n, n2, true);
    }

    public static int reticleY(int n, int n2) {
        Compat.hit(15);
        return Controls.reticle(n, n2, false);
    }

    public static void aimedFromReticle(IsoPlayer isoPlayer) {
        Compat.hit(58);
        Controls.aim(isoPlayer);
    }

    public static void skeletonPosed(AnimationPlayer animationPlayer) {
        Compat.hit(59);
        ArmsPitch.posed(animationPlayer);
    }

    public static void mouseUpdated() {
        Compat.hit(11);
        Controls.pinMouse();
        SettingsWindow.blockGameMouse();
        LootMenu.wheel();
    }

    public static boolean keyDown(int n, boolean bl) {
        Compat.hit(29);
        boolean bl2 = KeyInput.keyDown(n, bl);
        return LootMenu.keyDown(n, GameFixes.keyDown(n, bl2, KeyInput.reachesGameFlying(n)));
    }

    public static boolean skipStatsGet() {
        Compat.hit(26);
        return GameFixes.statsReady;
    }

    public static float statsGet(Stats stats, CharacterStat characterStat, float f) {
        return GameFixes.statsReady ? GameFixes.stat(stats, characterStat) : f;
    }

    public static void lineOfSightStarting(IsoPlayer isoPlayer) {
        Compat.hit(45);
        SpottedStack.use(isoPlayer);
    }

    public static void playStarting() {
        ModLua.load();
    }

    public static void playStarted() {
        Compat.hit(37);
        AnimalSets.load();
        DiagonalWalk.fix();
        Onboarding.playStarted();
    }

    public static int lockFps(int n) {
        Compat.hit(38);
        return FrameCaps.ON ? FrameCaps.lockNow(n) : n;
    }

    public static boolean framerateUncapped(boolean bl) {
        Compat.hit(39);
        return FrameCaps.ON ? FrameCaps.uncappedNow(bl) : bl;
    }

    public static void optionsLoading() {
        Compat.hit(40);
        if (FrameCaps.ON) {
            FrameCaps.beforeLoadOptions();
        }
    }

    public static void optionsLoaded() {
        if (FrameCaps.ON) {
            FrameCaps.afterLoadOptions();
        }
    }

    public static void optionsSaving() {
        Compat.hit(41);
        if (FrameCaps.ON) {
            FrameCaps.beforeSaveOptions();
        }
    }

    public static void optionsSaved() {
        if (FrameCaps.ON) {
            FrameCaps.afterSaveOptions();
        }
    }

    public static void uiFrameEnding() {
        Compat.hit(36);
        SettingsWindow.update();
        LootPanel.draw();
        PerformanceOverlay.draw();
    }

    public static boolean skipMouseCursorUpdate() {
        Compat.hit(2);
        return Look.onUpdateMouseCursor();
    }

    public static void frameSwapping() {
        SettingsWindow.render();
        Latency.presenting();
    }

    public static void frameSwapped() {
        Compat.hit(34);
        FrameTimes.swapped();
        Look.framePresented();
    }

    public static void gameRenderStarted() {
        Compat.hit(35);
        FrameTimes.renderStarted();
        VideoMemory.sample(System.nanoTime());
    }

    public static void gameRenderEnded() {
        FrameTimes.renderEnded();
    }

    public static void windowSynced(IsoWindow isoWindow) {
        Compat.hit(33);
        ChunkCache.windowChanged(isoWindow);
    }

    private Hooks() {
    }
}

