/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.characters.CharacterStat
 *  zombie.characters.Stats
 */
package viewpoint.game;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import viewpoint.input.FreeCam;
import viewpoint.platform.BuildPin;
import zombie.characters.CharacterStat;
import zombie.characters.Stats;

public final class GameFixes {
    public static final String SECTION = "Game/Fixes";
    private static final VarHandle STATS = GameFixes.statsMap();
    public static final boolean statsReady = BuildPin.supported();
    private static final boolean[] kept = new boolean[256];

    public static float stat(Stats stats, CharacterStat characterStat) {
        Float f = (Float)STATS.get(stats).get(characterStat);
        return f != null ? f.floatValue() : characterStat.getDefaultValue();
    }

    public static boolean keyDown(int n, boolean bl, boolean bl2) {
        if (bl2 || n < 0 || n >= kept.length) {
            return bl;
        }
        GameFixes.kept[n] = bl && (kept[n] || FreeCam.active);
        return bl && !kept[n];
    }

    private static VarHandle statsMap() {
        try {
            return MethodHandles.privateLookupIn(Stats.class, MethodHandles.lookup()).findVarHandle(Stats.class, "stats", Map.class);
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("[Viewpoint] Stats.stats is not this build's", reflectiveOperationException);
        }
    }

    private GameFixes() {
    }
}

