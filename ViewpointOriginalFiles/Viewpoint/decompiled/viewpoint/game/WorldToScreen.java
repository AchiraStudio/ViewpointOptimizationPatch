/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector4f
 */
package viewpoint.game;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import viewpoint.core.Frame;
import viewpoint.input.Camera;
import viewpoint.input.FreeCam;
import viewpoint.input.ThirdPerson;

final class WorldToScreen {
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f viewProjection = new Matrix4f();
    private final Vector4f clip = new Vector4f();
    private final float[] eye = new float[3];
    private final float[] dir = new float[3];
    private float originX;
    private float originY;
    private float originZ;
    private float width;
    private float height;

    WorldToScreen() {
    }

    boolean set(Frame frame, FreeCam.Place place, float f, float f2) {
        boolean bl = false;
        if (place != null) {
            Camera.at(frame, place, this.eye, this.dir);
        } else {
            Camera.direction(frame.viewYaw, frame.viewPitch, this.dir);
            boolean bl2 = bl = !ThirdPerson.view(frame, frame.viewYaw, frame.viewPitch, this.eye);
            if (bl) {
                Camera.eye(frame, frame.viewYaw, this.eye);
            }
        }
        this.view.setLookAt(this.eye[0], this.eye[1], this.eye[2], this.eye[0] + this.dir[0], this.eye[1] + this.dir[1], this.eye[2] + this.dir[2], 0.0f, 1.0f, 0.0f);
        frame.scene.projection.mul((Matrix4fc)this.view, this.viewProjection);
        this.originX = frame.camX;
        this.originY = frame.camY;
        this.originZ = frame.camZ;
        this.width = f;
        this.height = f2;
        return bl;
    }

    boolean pixel(float f, float f2, float f3, float f4, float[] fArray) {
        this.viewProjection.transform(-(f - this.originX), (f3 - this.originZ) * 2.4494896f + f4, -(f2 - this.originY), 1.0f, this.clip);
        if (this.clip.w < 0.05f) {
            return false;
        }
        fArray[0] = (0.5f + 0.5f * this.clip.x / this.clip.w) * this.width;
        fArray[1] = (0.5f - 0.5f * this.clip.y / this.clip.w) * this.height;
        return true;
    }
}

