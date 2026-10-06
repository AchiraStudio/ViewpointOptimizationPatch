/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 */
package viewpoint.render;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import viewpoint.platform.GlProgram;

final class IrisProgram {
    static final int UNIT_BASE = 96;
    static final int SCRATCH_UNIT = 95;
    private static final Set<String> LIBRARY_SAMPLERS = Set.of("uLight", "uPlantSlots", "uFloorPages", "uFlashShadow", "uLampShadow", "uPackTable", "uMeshRecords");
    private static final Set<String> ON_UNIT_0 = Set.of("vp_albedoTex", "uTexture");
    final String name;
    final GlProgram gl;
    final List<Integer> drawBuffers;
    final int tintOutput;
    final Map<String, Integer> units = new LinkedHashMap<String, Integer>();
    final Map<String, String> samplerTypes = new LinkedHashMap<String, String>();
    final Map<String, Integer> images = new LinkedHashMap<String, Integer>();
    final Map<String, Uniform> uniforms = new LinkedHashMap<String, Uniform>();

    IrisProgram(String string, GlProgram glProgram, List<Integer> list, int n, Map<String, String> map) {
        this.name = string;
        this.gl = glProgram;
        this.drawBuffers = list;
        this.tintOutput = n;
        this.activeUniforms();
        int n2 = 96;
        int n3 = 0;
        glProgram.use();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Uniform uniform = this.uniforms.get(entry.getKey());
            if (uniform == null) continue;
            if (entry.getValue().contains("image")) {
                this.images.put(entry.getKey(), n3);
                GL20.glUniform1i((int)uniform.location(), (int)n3++);
                continue;
            }
            if (entry.getKey().equals("vp_albedoTex")) {
                this.units.put(entry.getKey(), 0);
                GL20.glUniform1i((int)uniform.location(), (int)0);
                continue;
            }
            if (LIBRARY_SAMPLERS.contains(entry.getKey())) continue;
            this.units.put(entry.getKey(), n2);
            this.samplerTypes.put(entry.getKey(), entry.getValue());
            GL20.glUniform1i((int)uniform.location(), (int)n2++);
        }
        GL20.glUseProgram((int)0);
    }

    private void activeUniforms() {
        int n = this.gl.id();
        int n2 = GL20.glGetProgrami((int)n, (int)35718);
        IntBuffer intBuffer = BufferUtils.createIntBuffer((int)1);
        IntBuffer intBuffer2 = BufferUtils.createIntBuffer((int)1);
        for (int i = 0; i < n2; ++i) {
            String string = GL20.glGetActiveUniform((int)n, (int)i, (int)256, (IntBuffer)intBuffer, (IntBuffer)intBuffer2);
            string = string.endsWith("[0]") ? string.substring(0, string.length() - 3) : string;
            this.uniforms.put(string, new Uniform(GL20.glGetUniformLocation((int)n, (CharSequence)string), intBuffer2.get(0), intBuffer.get(0)));
        }
    }

    void checkUnits() {
        for (Map.Entry<String, Uniform> entry : this.uniforms.entrySet()) {
            Uniform uniform = entry.getValue();
            if (!IrisProgram.isSampler(uniform.type()) || ON_UNIT_0.contains(entry.getKey()) || GL20.glGetUniformi((int)this.gl.id(), (int)uniform.location()) != 0) continue;
            throw new IllegalStateException(this.name + ": the sampler " + entry.getKey() + " has no texture unit");
        }
    }

    private static boolean isSampler(int n) {
        return n >= 35677 && n <= 35684 || n >= 36288 && n <= 36293 || n >= 36297 && n <= 36312 || n >= 36876 && n <= 36879 || n >= 37128 && n <= 37133;
    }

    void use() {
        this.gl.use();
    }

    boolean has(String string) {
        return this.uniforms.containsKey(string);
    }

    void set(Map<String, float[]> map) {
        for (Map.Entry<String, Uniform> entry : this.uniforms.entrySet()) {
            float[] fArray = map.get(entry.getKey());
            if (fArray == null) continue;
            IrisProgram.set(entry.getValue(), fArray);
        }
    }

    void set(String string, float ... fArray) {
        Uniform uniform = this.uniforms.get(string);
        if (uniform != null) {
            IrisProgram.set(uniform, fArray);
        }
    }

    private static void set(Uniform uniform, float[] fArray) {
        int n = uniform.location();
        if (uniform.size() > 1 && (uniform.type() == 5124 || uniform.type() == 5126)) {
            int[] nArray = new int[Math.min(uniform.size(), fArray.length)];
            float[] fArray2 = new float[nArray.length];
            for (int i = 0; i < nArray.length; ++i) {
                nArray[i] = Math.round(fArray[i]);
                fArray2[i] = fArray[i];
            }
            if (uniform.type() == 5124) {
                GL20.glUniform1iv((int)n, (int[])nArray);
            } else {
                GL20.glUniform1fv((int)n, (float[])fArray2);
            }
            return;
        }
        switch (uniform.type()) {
            case 5126: {
                GL20.glUniform1f((int)n, (float)fArray[0]);
                break;
            }
            case 35664: {
                GL20.glUniform2f((int)n, (float)fArray[0], (float)IrisProgram.get(fArray, 1));
                break;
            }
            case 35665: {
                GL20.glUniform3f((int)n, (float)fArray[0], (float)IrisProgram.get(fArray, 1), (float)IrisProgram.get(fArray, 2));
                break;
            }
            case 35666: {
                GL20.glUniform4f((int)n, (float)fArray[0], (float)IrisProgram.get(fArray, 1), (float)IrisProgram.get(fArray, 2), (float)IrisProgram.get(fArray, 3));
                break;
            }
            case 5124: 
            case 35670: {
                GL20.glUniform1i((int)n, (int)Math.round(fArray[0]));
                break;
            }
            case 35667: 
            case 35671: {
                GL20.glUniform2i((int)n, (int)Math.round(fArray[0]), (int)Math.round(IrisProgram.get(fArray, 1)));
                break;
            }
            case 35668: 
            case 35672: {
                GL20.glUniform3i((int)n, (int)Math.round(fArray[0]), (int)Math.round(IrisProgram.get(fArray, 1)), (int)Math.round(IrisProgram.get(fArray, 2)));
                break;
            }
            case 35669: 
            case 35673: {
                GL20.glUniform4i((int)n, (int)Math.round(fArray[0]), (int)Math.round(IrisProgram.get(fArray, 1)), (int)Math.round(IrisProgram.get(fArray, 2)), (int)Math.round(IrisProgram.get(fArray, 3)));
                break;
            }
            case 5125: {
                GL30.glUniform1ui((int)n, (int)Math.round(fArray[0]));
                break;
            }
            case 35676: {
                if (fArray.length < 16) break;
                GL20.glUniformMatrix4fv((int)n, (boolean)false, (float[])fArray);
                break;
            }
            case 35675: {
                if (fArray.length >= 16) {
                    GL20.glUniformMatrix3fv((int)n, (boolean)false, (float[])new float[]{fArray[0], fArray[1], fArray[2], fArray[4], fArray[5], fArray[6], fArray[8], fArray[9], fArray[10]});
                    break;
                }
                if (fArray.length < 9) break;
                GL20.glUniformMatrix3fv((int)n, (boolean)false, (float[])fArray);
                break;
            }
        }
    }

    private static float get(float[] fArray, int n) {
        return n < fArray.length ? fArray[n] : 0.0f;
    }

    List<String> samplers() {
        return new ArrayList<String>(this.units.keySet());
    }

    void delete() {
        this.gl.delete();
    }

    record Uniform(int location, int type, int size) {
    }
}

