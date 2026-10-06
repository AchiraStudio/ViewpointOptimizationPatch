/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoObject
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.sprite.IsoSprite
 *  zombie.iso.sprite.IsoSpriteInstance
 */
package viewpoint.world;

import java.util.ArrayList;
import viewpoint.packs.ModelPacks;
import viewpoint.packs.PackBind;
import viewpoint.packs.PackLoads;
import viewpoint.render.PackModel;
import viewpoint.visibility.Edges;
import viewpoint.visibility.Owner;
import viewpoint.world.Recipe;
import zombie.core.properties.PropertyContainer;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;

final class PackGather {
    private static Recipe recipe;
    private static boolean on;
    private static long now;

    static void begin(Recipe recipe, boolean bl) {
        PackGather.recipe = recipe;
        on = bl && ModelPacks.any();
        now = System.nanoTime();
    }

    static void end() {
        recipe = null;
    }

    static boolean object(IsoObject isoObject, IsoSprite isoSprite, float f, float f2, float f3, int n) {
        PackBind packBind;
        PackBind packBind2 = packBind = on ? ModelPacks.bind(isoSprite) : null;
        if (packBind == null || !PackGather.ready(packBind)) {
            return false;
        }
        PackGather.place(packBind, isoSprite, f, f2, f3, n);
        ArrayList arrayList = isoObject.getAttachedAnimSprite();
        for (int i = 0; arrayList != null && i < arrayList.size(); ++i) {
            IsoSpriteInstance isoSpriteInstance = (IsoSpriteInstance)arrayList.get(i);
            PackGather.part(isoSpriteInstance == null ? null : isoSpriteInstance.parentSprite, f, f2, f3, n);
        }
        PackGather.part(isoObject.getOverlaySprite(), f, f2, f3, n);
        return true;
    }

    static boolean part(IsoSprite isoSprite, float f, float f2, float f3, int n) {
        PackBind packBind;
        PackBind packBind2 = packBind = on && isoSprite != null ? ModelPacks.bind(isoSprite) : null;
        if (packBind == null || !PackGather.ready(packBind)) {
            return false;
        }
        PackGather.place(packBind, isoSprite, f, f2, f3, n);
        return true;
    }

    private static boolean ready(PackBind packBind) {
        PackModel packModel = packBind.model;
        if (PackLoads.want(packModel, now)) {
            return true;
        }
        if (packModel.state() != 4) {
            if (PackGather.recipe.waiting == null) {
                PackGather.recipe.waiting = new ArrayList();
            }
            if (!PackGather.recipe.waiting.contains(packModel)) {
                PackGather.recipe.waiting.add(packModel);
            }
        }
        return false;
    }

    private static void place(PackBind packBind, IsoSprite isoSprite, float f, float f2, float f3, int n) {
        PropertyContainer propertyContainer = isoSprite.getProperties();
        boolean bl = propertyContainer.has(IsoFlagType.attachedN) || propertyContainer.has(IsoFlagType.attachedW) || propertyContainer.has(IsoFlagType.attachedS) || propertyContainer.has(IsoFlagType.attachedE) || propertyContainer.has(IsoFlagType.attachedNW) || propertyContainer.has(IsoFlagType.attachedSE);
        Recipe.model(recipe, packBind, PackBind.turns(isoSprite), f, f2, f3, n, bl ? 2 : 0, Owner.of(n, Edges.ownerKind(isoSprite)));
    }

    private PackGather() {
    }
}

