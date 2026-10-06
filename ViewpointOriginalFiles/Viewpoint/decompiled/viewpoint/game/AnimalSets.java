/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.characters.action.ActionGroup
 *  zombie.characters.animals.AnimalDefinitions
 */
package viewpoint.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.TreeSet;
import viewpoint.platform.LiveSettings;
import zombie.characters.action.ActionGroup;
import zombie.characters.animals.AnimalDefinitions;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;

public final class AnimalSets {
    public static final LiveSettings.Toggle ON = LiveSettings.toggle("game.animalSetsWithGame", "Animals' animations loaded with the game", "Game/Fixes", true);

    public static void load() {
        Object object2;
        if (!ON.get()) {
            return;
        }
        long l = System.nanoTime();
        ArrayList<String> arrayList = new ArrayList<String>();
        for (Object object2 : AnimalDefinitions.getAnimalDefsArray()) {
            arrayList.add(((AnimalDefinitions)object2).animset);
        }
        TreeSet<String> treeSet = AnimalSets.sets(arrayList);
        object2 = treeSet.iterator();
        while (object2.hasNext()) {
            String string = (String)object2.next();
            AnimationSet.GetAnimationSet(string, false);
            ActionGroup.getActionGroup((String)string);
        }
        System.out.printf(Locale.ROOT, "[Viewpoint] animals: %d animation sets and action groups loaded with the game in %.0f ms%n", treeSet.size(), (double)(System.nanoTime() - l) * 1.0E-6);
    }

    static TreeSet<String> sets(Collection<String> collection) {
        TreeSet<String> treeSet = new TreeSet<String>();
        for (String string : collection) {
            if (string == null) continue;
            treeSet.add(string);
        }
        return treeSet;
    }

    private AnimalSets() {
    }
}

