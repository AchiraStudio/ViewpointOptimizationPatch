#version 330
#extension GL_ARB_gpu_shader5 : enable





in vec2 vUv;
in vec3 vNormal;
in vec3 vPos;
#include "constants.glsl"
uniform sampler2D uTextures[MODEL_TEXTURES];
flat in int vTexture;
uniform vec3 uEye;
flat in vec4 vTint;
flat in vec4 vLight;
uniform int uWireframe;
uniform int uModelFilter;
layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gLight;
layout(location = 3) out vec4 gVelocity;
#include "lib/motion.glsl"
#include "lib/hsv.glsl"
#include "lib/lanczos.glsl"
#include "lib/fade.glsl"
void main() {
  fadeDither();
  vec2 size = vec2(textureSize(uTextures[vTexture], 0));
  vec4 c = uModelFilter == FILTER_LANCZOS && magnified(size, dFdx(vUv), dFdy(vUv))
      ? lanczos(uTextures[vTexture], vUv, ivec2(0), ivec2(size) - 1, true) : texture(uTextures[vTexture], vUv);
  float noise = fadeNoise();
  if (uWireframe == 0 && (c.a < 0.01 || c.a < 0.99 && c.a <= noise)) discard;
  vec3 colour = vTint.a != 0.0 ? hueShift(c.rgb, vTint.a) : c.rgb;
  vec3 n = normalize(vNormal);
  if (dot(n, uEye - vPos) < 0.0) n = -n;
  gAlbedo = vec4(colour * vTint.rgb, 0.0);
  gNormal = vec4(n, vLight.a);
  gLight = vLight;
  gVelocity = motion();
  if (uWireframe == 2) {
    gAlbedo.rgb = vec3(0.45, 0.65, 1.0);
    gLight = vec4(1.0);
  }
}
