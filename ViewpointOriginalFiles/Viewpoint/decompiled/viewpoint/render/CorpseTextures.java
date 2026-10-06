/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.skinnedmodel.model.ModelInstanceTextureCreator
 */
package viewpoint.render;

import viewpoint.render.CharacterTextures;
import zombie.core.skinnedmodel.model.ModelInstanceTextureCreator;

public final class CorpseTextures
implements CharacterTextures.Composited {
    private final ModelInstanceTextureCreator creator;
    private volatile boolean made;

    public CorpseTextures(ModelInstanceTextureCreator modelInstanceTextureCreator) {
        this.creator = modelInstanceTextureCreator;
    }

    public boolean made() {
        return this.made;
    }

    @Override
    public boolean composites() {
        return !this.creator.isRendered();
    }

    @Override
    public void ready() {
        this.creator.render();
        this.made = this.creator.isRendered();
    }
}

