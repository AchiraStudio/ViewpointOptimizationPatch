/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.zed_0xff.zombie_buddy.Patch
 *  me.zed_0xff.zombie_buddy.Patch$Argument
 *  me.zed_0xff.zombie_buddy.Patch$OnExit
 *  me.zed_0xff.zombie_buddy.Patch$Return
 *  zombie.UpdateSchedulerSimulationLevel
 */
package viewpoint;

import me.zed_0xff.zombie_buddy.Patch;
import viewpoint.Hooks;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.iso.IsoMovingObject;

@Patch(className="zombie.MovingObjectUpdateScheduler", methodName="getUpdateSchedulerSimulationLevelForObject")
public class Patch_SimulationLevel {
    @Patch.OnExit
    public static void exit(@Patch.Argument(value=0) IsoMovingObject isoMovingObject, @Patch.Return(readOnly=false) UpdateSchedulerSimulationLevel updateSchedulerSimulationLevel) {
        updateSchedulerSimulationLevel = Hooks.simulationLevel(isoMovingObject, updateSchedulerSimulationLevel);
    }
}

