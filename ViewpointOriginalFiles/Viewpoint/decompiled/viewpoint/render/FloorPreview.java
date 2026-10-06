/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  imgui.ImGui
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL42
 *  org.lwjgl.opengl.GL43
 */
package viewpoint.render;

import imgui.ImGui;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import viewpoint.render.FloorBakes;
import viewpoint.render.FloorPage;

final class FloorPreview {
    private static final float SHOWN = 320.0f;
    private final FloorBakes bakes;
    private int texture;
    private int size;

    FloorPreview(FloorBakes floorBakes) {
        this.bakes = floorBakes;
    }

    void draw() {
        ImGui.textUnformatted((String)this.bakes.figures());
        FloorPage floorPage = this.bakes.nearestPage();
        int n = this.bakes.nearestSlice();
        if (floorPage == null || n < 0) {
            ImGui.textUnformatted((String)"No floor baked in view yet.");
            return;
        }
        boolean bl = (n & 0x100000) != 0;
        int n2 = this.bakes.tierSize(n);
        if (this.texture == 0 || this.size != n2) {
            if (this.texture != 0) {
                GL11.glDeleteTextures((int)this.texture);
            }
            this.texture = GL11.glGenTextures();
            this.size = n2;
            int n3 = GL11.glGetInteger((int)32873);
            GL11.glBindTexture((int)3553, (int)this.texture);
            GL42.glTexStorage2D((int)3553, (int)1, (int)32856, (int)this.size, (int)this.size);
            GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
            GL11.glBindTexture((int)3553, (int)n3);
        }
        GL43.glCopyImageSubData((int)this.bakes.texture(n), (int)35866, (int)0, (int)0, (int)0, (int)FloorBakes.layer(n), (int)this.texture, (int)3553, (int)0, (int)0, (int)0, (int)0, (int)this.size, (int)this.size, (int)1);
        ImGui.textUnformatted((String)String.format("Chunk %d, %d, storey %d: %s, %d texels a side (north at the top)", floorPage.wx, floorPage.wy, floorPage.level, bl ? "distant tier" : "near tier", this.size));
        ImGui.image((int)this.texture, (float)320.0f, (float)320.0f, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f);
    }
}

