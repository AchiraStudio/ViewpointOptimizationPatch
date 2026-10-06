/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL33
 */
package viewpoint.render;

import org.lwjgl.opengl.GL33;
import viewpoint.platform.Gl;

final class ShadowDepthViews {
    private final int sampler = GL33.glGenSamplers();

    ShadowDepthViews() {
        GL33.glSamplerParameteri((int)this.sampler, (int)34892, (int)0);
        GL33.glSamplerParameteri((int)this.sampler, (int)10241, (int)9728);
        GL33.glSamplerParameteri((int)this.sampler, (int)10240, (int)9728);
        GL33.glSamplerParameteri((int)this.sampler, (int)10242, (int)33071);
        GL33.glSamplerParameteri((int)this.sampler, (int)10243, (int)33071);
    }

    void bind(int[] nArray, boolean bl) {
        for (int i = 0; i < nArray.length; ++i) {
            Gl.bind(Gl.UNIT_SHADOW_DEPTH[i], bl ? nArray[i] : 0);
            GL33.glBindSampler((int)Gl.UNIT_SHADOW_DEPTH[i], (int)(bl ? this.sampler : 0));
        }
    }
}

