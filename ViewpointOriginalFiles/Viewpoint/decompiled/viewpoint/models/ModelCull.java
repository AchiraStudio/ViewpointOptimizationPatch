/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.UpdateSchedulerSimulationLevel
 *  zombie.ai.states.FakeDeadZombieState
 *  zombie.core.Core
 *  zombie.iso.IsoCell
 *  zombie.iso.IsoGridSquare
 */
package viewpoint.models;

import viewpoint.core.View;
import viewpoint.input.FreeCam;
import viewpoint.input.Look;
import viewpoint.input.ThirdPerson;
import viewpoint.platform.LiveSettings;
import viewpoint.platform.Profile;
import viewpoint.visibility.Rooms;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.ai.State;
import zombie.ai.states.FakeDeadZombieState;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;

public final class ModelCull {
    static volatile float range = 80.0f;
    private static final float MODEL_CLOSE = 12.0f;
    private static final double TURN_MARGIN = Math.toRadians(30.0);
    private static final float SIGHT_FREE = 30.0f;
    private static final float BODY_RADIUS = 0.4f;
    private static final float BODY_LEVELS = 0.8f;
    private static final float[] AWARE = new float[]{30.0f, 60.0f, 80.0f};
    private static final float[] UNAWARE = new float[]{12.0f, 24.0f, 48.0f};
    private static final UpdateSchedulerSimulationLevel[] LEVELS = new UpdateSchedulerSimulationLevel[]{UpdateSchedulerSimulationLevel.HALF, UpdateSchedulerSimulationLevel.QUARTER, UpdateSchedulerSimulationLevel.EIGHTH};
    private static final LiveSettings.Toggle THIN_UNAWARE = LiveSettings.toggle("lod.unawareZombiesThinned", "Zombies without a target updated less often from 12 squares", "World/Levels of detail", true);
    private static boolean zombieCull;
    private static int asked;
    private static int offered;

    public static int cullScreenX(IsoZombie isoZombie, int n, int n2) {
        IsoPlayer isoPlayer = IsoPlayer.players[0];
        if (!View.enabled || n != 0 || isoPlayer == null) {
            return n2;
        }
        return ModelCull.wanted(isoZombie, isoPlayer) ? 0 : -1000;
    }

    public static void zombieCull(boolean bl) {
        zombieCull = bl;
    }

    public static boolean headSquareSeen(IsoZombie isoZombie, IsoPlayer isoPlayer, boolean bl) {
        return bl || View.enabled && zombieCull && isoPlayer == IsoPlayer.players[0] && ModelCull.sightFree(isoZombie, isoPlayer);
    }

    private static boolean sightFree(IsoMovingObject isoMovingObject, IsoPlayer isoPlayer) {
        return isoMovingObject.DistToSquared((IsoMovingObject)((Object)isoPlayer)) <= 900.0f;
    }

    private static boolean wanted(IsoMovingObject isoMovingObject, IsoPlayer isoPlayer) {
        boolean bl;
        float f;
        ++asked;
        FreeCam.Place place = FreeCam.centre();
        float f2 = place != null ? (float)place.x() : isoPlayer.getX() + ThirdPerson.offsetX;
        float f3 = place != null ? (float)place.y() : isoPlayer.getY() + ThirdPerson.offsetY;
        float f4 = isoMovingObject.getX() - f2;
        float f5 = f4 * f4 + (f = isoMovingObject.getY() - f3) * f;
        if (f5 > range * range) {
            return false;
        }
        boolean bl2 = bl = f5 <= 144.0f;
        if (!bl) {
            double d = (double)Core.getInstance().getScreenWidth() / (double)Math.max(1, Core.getInstance().getScreenHeight());
            double d2 = Math.atan(Math.tan((double)View.fovY(ThirdPerson.active) / 2.0) * d) + TURN_MARGIN;
            double d3 = place != null ? (double)place.yaw() : (double)Look.yaw;
            double d4 = Math.sqrt(f5);
            boolean bl3 = bl = ((double)f4 * Math.cos(d3) + (double)f * Math.sin(d3)) / d4 >= Math.cos(Math.min(d2, Math.PI));
        }
        if (bl && !bl2 && ModelCull.roomHidden(isoMovingObject)) {
            return false;
        }
        if (bl) {
            ++offered;
        }
        return bl;
    }

    static boolean roomHidden(IsoMovingObject isoMovingObject) {
        float f = isoMovingObject.getX();
        float f2 = isoMovingObject.getY();
        float f3 = isoMovingObject.getZ();
        return Rooms.hides(f - 0.4f, f2 - 0.4f, f + 0.4f, f2 + 0.4f, (int)Math.floor(f3), (int)Math.floor(f3 + 0.8f));
    }

    public static UpdateSchedulerSimulationLevel simulationLevel(IsoMovingObject isoMovingObject, UpdateSchedulerSimulationLevel updateSchedulerSimulationLevel) {
        Object object;
        IsoPlayer isoPlayer = IsoPlayer.players[0];
        if (!View.enabled || isoPlayer == null || isoMovingObject == isoPlayer || updateSchedulerSimulationLevel == null) {
            return updateSchedulerSimulationLevel;
        }
        float f = isoMovingObject.DistTo((IsoMovingObject)((Object)isoPlayer));
        boolean bl = THIN_UNAWARE.get() && isoMovingObject instanceof IsoZombie && ((IsoZombie)((Object)(object = (IsoZombie)isoMovingObject))).getTarget() == null;
        object = bl ? UNAWARE : AWARE;
        UpdateSchedulerSimulationLevel updateSchedulerSimulationLevel2 = UpdateSchedulerSimulationLevel.FULL;
        for (int i = 0; i < ((Object)object).length && f > object[i]; ++i) {
            updateSchedulerSimulationLevel2 = LEVELS[i];
        }
        if (updateSchedulerSimulationLevel.ordinal() <= updateSchedulerSimulationLevel2.ordinal()) {
            return updateSchedulerSimulationLevel;
        }
        return updateSchedulerSimulationLevel2.max(isoMovingObject.getMinimumSimulationLevel());
    }

    public static void reportCull(IsoCell isoCell) {
        Profile.zombies(isoCell.getZombieList().size(), asked, offered);
        offered = 0;
        asked = 0;
    }

    public static int cullScreenY(int n, int n2) {
        return View.enabled && n == 0 ? 0 : n2;
    }

    public static boolean cullAnimals(IsoWorld isoWorld) {
        IsoPlayer isoPlayer = IsoPlayer.players[0];
        if (!View.enabled || isoPlayer == null) {
            return false;
        }
        for (IsoMovingObject isoMovingObject : isoWorld.getCell().getObjectList()) {
            boolean bl;
            if (!(isoMovingObject instanceof IsoAnimal)) continue;
            IsoAnimal isoAnimal = (IsoAnimal)((Object)isoMovingObject);
            IsoGridSquare isoGridSquare = isoAnimal.getCurrentSquare();
            boolean bl2 = bl = isoGridSquare != null && ModelCull.wanted((IsoMovingObject)((Object)isoAnimal), isoPlayer) && (ModelCull.sightFree((IsoMovingObject)((Object)isoAnimal), isoPlayer) || isoAnimal.getAlpha(0) != 0.0f && isoAnimal.legsSprite.def.alpha != 0.0f || isoGridSquare.isCouldSee(0)) && !isoAnimal.isCurrentState((State)FakeDeadZombieState.instance());
            if (bl) {
                isoAnimal.setSceneCulled(false);
            } else if (isoAnimal.hasActiveModel()) {
                isoAnimal.setSceneCulled(true);
            }
            if (!isoAnimal.hasAnimationPlayer()) continue;
            isoAnimal.getAnimationPlayer().doBlending = bl;
        }
        return true;
    }

    private ModelCull() {
    }
}

