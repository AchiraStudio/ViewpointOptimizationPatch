#version 330






in vec2 vUv;
in vec3 vLocal;
flat in vec4 vRect;
in vec3 vNormal;
in vec3 vPos;
flat in float vFill;
flat in int vFlags;
flat in vec2 vCellWorld;
flat in float vFloorLayer;
uniform sampler2D uTexture;
uniform sampler2D uMask;
uniform vec3 uMaskGrid;
uniform sampler2D uShellMask;
uniform vec3 uShellGrid;
uniform vec3 uEye;
uniform float uWet;
layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gLight;
#include "lib/sample_sprite.glsl"
#include "lib/sample_floor.glsl"
#include "lib/sample_tree.glsl"
#include "lib/band_light.glsl"

vec4 sampleLight(vec3 local, vec3 n) { return bandLight(vCellWorld + local.xz, local.y, n); }
#include "lib/surface.glsl"
#include "lib/lod.glsl"
#include "lib/fade.glsl"
#include "lib/shell_root.glsl"
#include "lib/wet.glsl"
void main() {
  if (length(vPos - uEye) >= SHELL_BAND) discard;
  fadeDither();
  vec2 chunk = floor((vCellWorld + maskedSquare(vLocal, vFlags)) / 8.0) - uMaskGrid.xy;
  if (all(greaterThanEqual(chunk, vec2(0.0))) && all(lessThan(chunk, vec2(uMaskGrid.z)))
      && builtOver(texelFetch(uMask, ivec2(chunk), 0).r)) discard;
  vec4 c = (vFlags & FLAG_BAKED_FLOOR) != 0 ? sampleFloor(vUv, vFloorLayer)
      : (vFlags & FLAG_BILLBOARD) != 0 ? sampleTree(vUv, vRect) : sampleSprite(uTexture, vUv, vRect, vFlags);
  Surface s = resolve(c, vLocal, vNormal, vFill, vFlags, vPos, uEye);
  vec2 block = floor((vCellWorld + vLocal.xz) / 64.0) - uShellGrid.xy;
  float tier = texelFetch(uShellMask, ivec2(clamp(block, vec2(0.0), vec2(uShellGrid.z - 1.0))), 0).r * 255.0;
  vec3 wet = wetGround(s.albedo, s.normal, s.foliage ? 0.0 : s.light.a, uWet);
  vec3 albedo = lodView(wet, abs(tier - 2.0) < 0.5 ? LOD_SHELL_FULL : LOD_SHELL_LITE);
  gAlbedo = vec4(albedo, s.foliage ? 1.0 : vFill > 0.5 ? 0.5 : 0.0);
  gNormal = vec4(s.normal, s.light.a);
  gLight = vec4(uWireframe == 2 ? vec3(1.0) : s.light.rgb, s.light.a);
}
