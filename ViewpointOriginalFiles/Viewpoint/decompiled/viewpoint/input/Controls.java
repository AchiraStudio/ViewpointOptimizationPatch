/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 *  zombie.core.physics.BallisticsController
 *  zombie.iso.IsoUtils
 *  zombie.iso.Vector2
 *  zombie.iso.Vector3
 */
package viewpoint.input;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import viewpoint.core.Frame;
import viewpoint.core.View;
import viewpoint.input.ArmsPitch;
import viewpoint.input.CrosshairAim;
import viewpoint.input.EyeMotion;
import viewpoint.input.FreeCam;
import viewpoint.input.Locomotion;
import viewpoint.input.Look;
import viewpoint.input.MoveInput;
import viewpoint.input.SeatedEye;
import viewpoint.input.StrafePace;
import viewpoint.input.ThirdPerson;
import viewpoint.platform.LiveSettings;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.physics.BallisticsController;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.Model;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.iso.Vector3;

public final class Controls {
    private static final String CAMERA = "Controls/Camera";
    private static final LiveSettings.Number HEAD_MOVEMENT = LiveSettings.number("camera.headMovement", "Head movement", "Controls/Camera", 0.0f, 1.0f, 0.01f, 0.0f);
    private static final String MOVEMENT = "Controls/Movement";
    private static final LiveSettings.Choice MODEL = LiveSettings.choice("movement.model", "Walking", "Controls/Movement", new String[]{"Turn to move", "Strafe"}, 0);
    private static final LiveSettings.Number DIRECTION_SMOOTHING = LiveSettings.number("movement.directionSmoothing", "Direction smoothing (s)", "Controls/Movement", 0.0f, 0.15f, 0.01f, 0.05f);
    private static final LiveSettings.Number TURN_SMOOTHING = LiveSettings.number("movement.turnSmoothing", "Body turn smoothing (s)", "Controls/Movement", 0.0f, 0.2f, 0.01f, 0.07f);
    private static final LiveSettings.Number BACKPEDAL_SPEED = LiveSettings.number("movement.backpedalSpeed", "Backpedal speed (of the walk's)", "Controls/Movement", 0.5f, 1.0f, 0.05f, 0.8f);
    private static final float AIM_DISTANCE = 8.0f;
    private static final long TRACE_MS = 1000L;
    private static final float MAX_UPDATE_SECONDS = 0.1f;
    private static final Set<String> STEADY_STATES;
    private static final String WALK_STATE = "movement";
    private static final String STRAFE_STATE = "strafe";
    private static final Set<String> WALK_STATES;
    private static boolean middlePressed;
    private static boolean middleWasDown;
    private static final float EYE_ABOVE_HEAD_BONE = 0.1f;
    private static final float EYE_FALLBACK_HEIGHT = 1.38f;
    private static final MoveInput input;
    private static final Locomotion locomotion;
    private static final StrafePace strafePace;
    private static final Vector2 probe;
    private static boolean inputDue;
    private static float updateSeconds;
    private static long updateNanos;
    private static float bodyAngle;
    private static boolean turned;
    private static boolean moved;
    private static float moveX;
    private static float moveY;
    private static float steerSeconds;
    private static float lastX;
    private static float lastY;
    private static int stillFrames;
    private static final Vector3 headBone;
    private static final EyeMotion eyeMotion;
    private static long eyeNanos;
    private static Field mouseX;
    private static Field mouseY;
    private static boolean pinFailed;
    private static String tracedState;
    private static boolean attacking;
    private static boolean reticleCentred;
    private static long tracedAt;

    static boolean owns(IsoGameCharacter isoGameCharacter) {
        return View.enabled && isoGameCharacter != null && isoGameCharacter == IsoPlayer.players[0] && isoGameCharacter.getVehicle() == null && !isoGameCharacter.isDead();
    }

    public static void moveVector(IsoPlayer isoPlayer, Vector2 vector2) {
        float f;
        moved = false;
        if (!Controls.owns((IsoGameCharacter)((Object)isoPlayer))) {
            return;
        }
        float f2 = vector2.getLength();
        if (inputDue) {
            inputDue = false;
            input.update(updateSeconds, -vector2.y, vector2.x, DIRECTION_SMOOTHING.get());
            locomotion.update(MODEL.get(), input.moving(), input.direction());
            if (locomotion.switched()) {
                f = locomotion.strafing() ? Look.yaw : Look.yaw + input.direction();
                boolean bl = Locomotion.steersAfterSwitch(Controls.animatedAngle(isoPlayer), f);
                float f3 = steerSeconds = bl ? 0.4f : 0.0f;
            }
        }
        if (f2 == 0.0f || !input.moving()) {
            return;
        }
        f = Look.yaw + input.direction();
        moveX = (float)Math.cos(f);
        moveY = (float)Math.sin(f);
        moved = true;
        vector2.set((moveX - moveY) * 0.5f, (moveX + moveY) * 0.5f);
        vector2.setLength(f2);
    }

    public static void beforeMovement(IsoPlayer isoPlayer) {
        turned = false;
        if (!Controls.owns((IsoGameCharacter)((Object)isoPlayer))) {
            return;
        }
        long l = System.nanoTime();
        updateSeconds = updateNanos == 0L ? 0.0f : Math.min(0.1f, (float)(l - updateNanos) * 1.0E-9f);
        updateNanos = l;
        inputDue = true;
        steerSeconds = Math.max(0.0f, steerSeconds - updateSeconds);
        if (isoPlayer.isBlockMovement()) {
            return;
        }
        isoPlayer.getInputMoveVector(probe);
        boolean bl = Math.abs(isoPlayer.getX() - lastX) + Math.abs(isoPlayer.getY() - lastY) < 0.001f;
        lastX = isoPlayer.getX();
        lastY = isoPlayer.getY();
        stillFrames = bl ? stillFrames + 1 : 0;
        float f = CrosshairAim.yaw((IsoGameCharacter)((Object)isoPlayer));
        float f2 = (float)Math.cos(f);
        float f3 = (float)Math.sin(f);
        boolean bl2 = isoPlayer.getForwardDirectionX() * f2 + isoPlayer.getForwardDirectionY() * f3 > 0.9f;
        boolean bl3 = isoPlayer.isStrafing();
        if (moved && !bl3 && !locomotion.backwards()) {
            Controls.turn(isoPlayer, (float)Math.atan2(moveY, moveX));
        } else if (bl2 || stillFrames >= 2 || bl3) {
            Controls.turn(isoPlayer, f);
        }
    }

    private static void turn(IsoPlayer isoPlayer, float f) {
        float f2 = isoPlayer.isAiming() ? 0.0f : TURN_SMOOTHING.get();
        bodyAngle = Locomotion.turn(Controls.animatedAngle(isoPlayer), f, updateSeconds, f2);
        isoPlayer.setTargetAndCurrentDirection((float)Math.cos(bodyAngle), (float)Math.sin(bodyAngle));
        turned = true;
    }

    private static float animatedAngle(IsoPlayer isoPlayer) {
        return isoPlayer.hasAnimationPlayer() && isoPlayer.getAnimationPlayer().isReady() ? isoPlayer.getAnimationPlayer().getAngle() : isoPlayer.getDirectionAngleRadians();
    }

    public static void rootMotion(IsoPlayer isoPlayer, Vector2 vector2, boolean bl) {
        if (!Controls.owns((IsoGameCharacter)((Object)isoPlayer))) {
            return;
        }
        if (bl) {
            Controls.pace(isoPlayer, vector2.getLength());
        }
        if (steerSeconds <= 0.0f || !moved || isoPlayer.isBlockMovement()) {
            return;
        }
        float f = vector2.getLength();
        if (f > 0.0f && WALK_STATES.contains(isoPlayer.getCurrentActionContextStateName())) {
            vector2.set(moveX * f, moveY * f);
        }
    }

    private static void pace(IsoPlayer isoPlayer, float f) {
        boolean bl = moved && !isoPlayer.isBlockMovement() && !isoPlayer.isAiming();
        String string = isoPlayer.getCurrentActionContextStateName();
        boolean bl2 = bl && !locomotion.strafing() && WALK_STATE.equals(string);
        boolean bl3 = bl && locomotion.strafing() && STRAFE_STATE.equals(string);
        strafePace.update(isoPlayer, f, updateSeconds, bl2, bl3, BACKPEDAL_SPEED.get());
    }

    public static void afterMovement(IsoPlayer isoPlayer) {
        if (turned && Controls.owns((IsoGameCharacter)((Object)isoPlayer)) && !isoPlayer.isBlockMovement()) {
            isoPlayer.setTargetAndCurrentDirection((float)Math.cos(bodyAngle), (float)Math.sin(bodyAngle));
        }
    }

    public static boolean strafing(IsoGameCharacter isoGameCharacter, boolean bl) {
        if (bl || !moved || !locomotion.strafing() || !Controls.owns(isoGameCharacter) || isoGameCharacter.isRunning() || isoGameCharacter.isSprinting()) {
            return bl;
        }
        return !"run".equals(isoGameCharacter.getCurrentActionContextStateName());
    }

    public static boolean running(IsoGameCharacter isoGameCharacter, boolean bl) {
        return bl && (!moved || !locomotion.backwards() || !Controls.owns(isoGameCharacter));
    }

    public static float visionCone(IsoGameCharacter isoGameCharacter, float f) {
        return Controls.owns(isoGameCharacter) ? 1.0f : f;
    }

    public static void eye(IsoGameCharacter isoGameCharacter, Frame frame) {
        AnimationPlayer animationPlayer;
        int n;
        if (isoGameCharacter.getVehicle() != null) {
            Controls.put(isoGameCharacter, frame);
            return;
        }
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = 1.38f;
        float f4 = 0.0f;
        if (isoGameCharacter.hasAnimationPlayer() && isoGameCharacter.getAnimationPlayer().isReady() && (n = (animationPlayer = isoGameCharacter.getAnimationPlayer()).getSkinningBoneIndex("Bip01_Head", -1)) >= 0) {
            Model.boneToWorldCoords(isoGameCharacter, n, headBone);
            float f5 = Controls.headBone.x - isoGameCharacter.getX();
            float f6 = Controls.headBone.y - isoGameCharacter.getY();
            float f7 = animationPlayer.getAngle();
            f = -f5;
            f2 = -f6;
            f3 = (Controls.headBone.z - isoGameCharacter.getZ()) * 2.4494896f + 0.1f;
            f4 = f5 * (float)Math.cos(f7) + f6 * (float)Math.sin(f7);
        }
        boolean bl = STEADY_STATES.contains(isoGameCharacter.getCurrentActionContextStateName());
        eyeMotion.update(Controls.eyeSeconds(), f, f2, f3, f4, bl, HEAD_MOVEMENT.get());
        Controls.put(isoGameCharacter, frame);
    }

    public static void eyeAtHead(IsoGameCharacter isoGameCharacter, Frame frame, float f, float f2, float f3) {
        float f4 = isoGameCharacter.getX() - frame.camX;
        float f5 = isoGameCharacter.getY() - frame.camY;
        float f6 = (isoGameCharacter.getZ() - frame.camZ) * 2.4494896f;
        float f7 = SeatedEye.offset(isoGameCharacter.getVehicle().getScript().getFullName(), frame.number);
        float f8 = f2 - f6 + 0.1f + f7;
        eyeMotion.update(Controls.eyeSeconds(), f + f4, f3 + f5, f8, 0.0f, false, 1.0f);
        Controls.put(isoGameCharacter, frame);
    }

    private static float eyeSeconds() {
        long l = System.nanoTime();
        float f = eyeNanos == 0L ? Float.MAX_VALUE : (float)(l - eyeNanos) * 1.0E-9f;
        eyeNanos = l;
        return f;
    }

    private static void put(IsoGameCharacter isoGameCharacter, Frame frame) {
        frame.eyeX = Controls.eyeMotion.offsetX - (isoGameCharacter.getX() - frame.camX);
        frame.eyeY = Controls.eyeMotion.height + (isoGameCharacter.getZ() - frame.camZ) * 2.4494896f;
        frame.eyeZ = Controls.eyeMotion.offsetZ - (isoGameCharacter.getY() - frame.camY);
        frame.eyeLean = Controls.eyeMotion.lean;
    }

    public static void traceState(IsoGameCharacter isoGameCharacter) {
        if (!(isoGameCharacter instanceof IsoPlayer)) {
            return;
        }
        IsoPlayer isoPlayer = (IsoPlayer)((Object)isoGameCharacter);
        String string = isoPlayer.getCurrentActionContextStateName();
        if (!string.equals(tracedState)) {
            tracedState = string;
            System.out.println("[Viewpoint] action state " + string + " moving=" + isoPlayer.isPlayerMoving() + " turning=" + isoPlayer.isTurning() + " strafing=" + isoPlayer.isStrafing() + " aiming=" + isoPlayer.isAiming() + " blocked=" + isoPlayer.isBlockMovement());
        }
    }

    public static float lookAngle(IsoGameCharacter isoGameCharacter, float f) {
        return Controls.owns(isoGameCharacter) ? CrosshairAim.yaw(isoGameCharacter) : f;
    }

    public static int reticle(int n, int n2, boolean bl) {
        IsoPlayer isoPlayer;
        if (reticleCentred && n == 0) {
            return (bl ? IsoCamera.getScreenWidth(0) : IsoCamera.getScreenHeight(0)) / 2;
        }
        IsoPlayer isoPlayer2 = isoPlayer = n == 0 ? IsoPlayer.players[0] : null;
        if (!Controls.owns((IsoGameCharacter)((Object)isoPlayer))) {
            return n2;
        }
        float f = CrosshairAim.yaw((IsoGameCharacter)((Object)isoPlayer));
        float f2 = isoPlayer.getAimOriginPosX() + (float)Math.cos(f) * 8.0f;
        float f3 = isoPlayer.getAimOriginPosY() + (float)Math.sin(f) * 8.0f;
        float f4 = isoPlayer.getAimOriginPosZ();
        float f5 = bl ? IsoUtils.XToScreen((float)f2, (float)f3, (float)f4, (int)0) - IsoCamera.getOffX(n) : IsoUtils.YToScreen((float)f2, (float)f3, (float)f4, (int)0) - IsoCamera.getOffY(n);
        return Math.round(f5 / Core.getInstance().getZoom(n));
    }

    public static boolean skipReticle(boolean bl) {
        IsoPlayer isoPlayer = IsoPlayer.players[0];
        boolean bl2 = isoPlayer != null && isoPlayer.getVehicle() != null;
        reticleCentred = bl && View.enabled && isoPlayer != null && Look.captured && !FreeCam.active && (!ThirdPerson.active || !bl2);
        return View.enabled && !reticleCentred;
    }

    public static void reticleDone() {
        reticleCentred = false;
    }

    public static void aim(IsoPlayer isoPlayer) {
        if (!Controls.owns((IsoGameCharacter)((Object)isoPlayer))) {
            return;
        }
        float f = CrosshairAim.yaw((IsoGameCharacter)((Object)isoPlayer));
        isoPlayer.setForwardDirection((float)Math.cos(f), (float)Math.sin(f));
        isoPlayer.setTargetVerticalAimAngle(0.0f);
        Controls.traceShot(isoPlayer);
    }

    private static void traceShot(IsoPlayer isoPlayer) {
        boolean bl = isoPlayer.isPerformingAttackAnimation() && !attacking;
        attacking = isoPlayer.isPerformingAttackAnimation();
        long l = System.currentTimeMillis();
        BallisticsController ballisticsController = isoPlayer.getBallisticsController();
        if (!bl || ballisticsController == null || l - tracedAt < 1000L) {
            return;
        }
        tracedAt = l;
        Vector3 vector3 = ballisticsController.getMuzzleDirection();
        double d = Math.toDegrees(Math.atan2(vector3.z * 2.4494896f, Math.hypot(vector3.x, vector3.y)));
        System.out.printf("[Viewpoint] shot pitch: aim %.1f, arms %.1f, barrel %.1f%n", Math.toDegrees(CrosshairAim.pitch((IsoGameCharacter)((Object)isoPlayer))), Math.toDegrees(ArmsPitch.pitch()), d);
    }

    public static boolean middlePressed() {
        return middlePressed;
    }

    public static void pinMouse() {
        boolean[] blArray = Mouse.buttonDownStates;
        boolean bl = blArray != null && blArray.length > 2 && blArray[2];
        middlePressed = bl && !middleWasDown;
        middleWasDown = bl;
        if (FreeCam.active && Look.captured) {
            FreeCam.wheel += Mouse.getWheelState();
            if (Mouse.buttonDownStates != null) {
                Arrays.fill(Mouse.buttonDownStates, false);
            }
            if (Mouse.buttonPrevStates != null) {
                Arrays.fill(Mouse.buttonPrevStates, false);
            }
        }
        if (!Look.captured || pinFailed) {
            return;
        }
        try {
            if (mouseX == null) {
                mouseX = Mouse.class.getDeclaredField("x");
                mouseY = Mouse.class.getDeclaredField("y");
                mouseX.setAccessible(true);
                mouseY.setAccessible(true);
            }
            mouseX.setInt(null, Core.getInstance().getScreenWidth() / 2);
            mouseY.setInt(null, Core.getInstance().getScreenHeight() / 2);
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            pinFailed = true;
            System.out.println("[Viewpoint] cannot pin the mouse position: " + String.valueOf(exception));
        }
    }

    private Controls() {
    }

    static {
        HEAD_MOVEMENT.describe("How much the view moves with the head as you stand, walk and turn: 0 keeps it steady (the height still follows a crouch), 1 follows the head's every step and sway.");
        MODEL.describe("Turn to move: the body turns to where it walks, as with a controller, and backpedals facing the view when you walk backwards. Strafe: the body always faces the view, and walks sideways and backwards on the game's strafe steps, at the Backpedal speed.");
        DIRECTION_SMOOTHING.describe("How long the walking direction takes to follow the keys, sweeping between them as a controller's stick does: 0 jumps at once. Turning back the other way is always at once.");
        TURN_SMOOTHING.describe("How long the body takes to turn to where it should face: 0 turns it at once. Aiming always turns it at once.");
        BACKPEDAL_SPEED.describe("How fast the body walks facing the view off forward (backwards with Turn to move, any way but forwards with Strafe), as a share of walking forwards: its steps play faster to keep pace. Never slower than the game's own strafe; aiming keeps the game's.");
        STEADY_STATES = Set.of("idle", WALK_STATE, STRAFE_STATE, "run", "sprint", "aim", "maskingleft", "maskingright", "turning", "turning180", "turningAim180", "turningIdle180", "turningMovement180");
        WALK_STATES = Set.of(WALK_STATE, STRAFE_STATE);
        input = new MoveInput();
        locomotion = new Locomotion();
        strafePace = new StrafePace();
        probe = new Vector2();
        headBone = new Vector3();
        eyeMotion = new EyeMotion();
    }
}

