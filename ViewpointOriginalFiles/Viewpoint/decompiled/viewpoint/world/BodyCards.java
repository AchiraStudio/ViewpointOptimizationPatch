/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.objects.IsoDeadBody
 */
package viewpoint.world;

import java.util.ArrayList;
import viewpoint.models.Corpses;
import viewpoint.render.SceneData;
import zombie.core.textures.TextureID;
import zombie.iso.objects.IsoDeadBody;

final class BodyCards {
    private static final ArrayList<IsoDeadBody> gatheredBodies = new ArrayList();
    private static final ArrayList<float[]> gatheredCards = new ArrayList();
    private static final ArrayList<TextureID> gatheredPages = new ArrayList();
    private static final ArrayList<Object> gatheredLooks = new ArrayList();
    private final IsoDeadBody[] bodies;
    private final float[] cards;
    private final TextureID[] pages;
    private final Object[] looks;

    private BodyCards(IsoDeadBody[] isoDeadBodyArray, float[] fArray, TextureID[] textureIDArray, Object[] objectArray) {
        this.bodies = isoDeadBodyArray;
        this.cards = fArray;
        this.pages = textureIDArray;
        this.looks = objectArray;
    }

    static void begin() {
        gatheredBodies.clear();
        gatheredCards.clear();
        gatheredPages.clear();
        gatheredLooks.clear();
    }

    static void add(IsoDeadBody isoDeadBody, float[] fArray, TextureID textureID, Object object) {
        gatheredBodies.add(isoDeadBody);
        gatheredCards.add(fArray);
        gatheredPages.add(textureID);
        gatheredLooks.add(object);
    }

    static BodyCards end() {
        int n = gatheredBodies.size();
        if (n == 0) {
            return null;
        }
        float[] fArray = new float[n * 28];
        for (int i = 0; i < n; ++i) {
            float[] fArray2 = gatheredCards.get(i);
            if (fArray2 == null) continue;
            System.arraycopy(fArray2, 0, fArray, i * 28, 28);
        }
        BodyCards bodyCards = new BodyCards(gatheredBodies.toArray(new IsoDeadBody[0]), fArray, gatheredPages.toArray(new TextureID[0]), gatheredLooks.toArray());
        BodyCards.begin();
        return bodyCards;
    }

    void list(float f, boolean bl) {
        for (int i = 0; i < this.bodies.length; ++i) {
            Corpses.listed(this.bodies[i], this.looks[i], f, bl);
        }
    }

    void into(SceneData sceneData, int n) {
        for (int i = 0; i < this.bodies.length; ++i) {
            if (this.pages[i] == null || !this.bodies[i].getDoRender()) continue;
            sceneData.addCard(n, this.cards, i * 28, this.pages[i], Corpses.share(this.bodies[i]));
        }
    }
}

