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
uniform sampler2D uMainDepth;
uniform vec3 uMaskGrid;
uniform sampler2D uShellMask;
uniform vec3 uShellGrid;
uniform vec3 uEye;
uniform vec2 uFogRange;
uniform vec2 uHaze;
uniform float uWet;
uniform int uDebugView;
out vec4 fragColor;
#include "lib/sample_sprite.glsl"
#include "lib/sample_floor.glsl"
#include "lib/sample_tree.glsl"
#include "lib/far_light.glsl"

vec4 sampleLight(vec3 local, vec3 n) { return farLight(vCellWorld + local.xz, local.y, n); }
#include "lib/surface.glsl"
#include "lib/sun.glsl"
#include "lib/shade.glsl"
#include "lib/lod.glsl"
#include "lib/fade.glsl"
#include "lib/shell_root.glsl"
#include "lib/fog.glsl"
#include "lib/fog_amount.glsl"
#include "lib/wet.glsl"
void main() {
  float d = length(vPos - uEye);
  if (d < SHELL_BAND || texelFetch(uMainDepth, ivec2(gl_FragCoord.xy), 0).r < 1.0) discard;
  fadeDither();
  vec2 chunk = floor((vCellWorld + maskedSquare(vLocal, vFlags)) / 8.0) - uMaskGrid.xy;
  if (all(greaterThanEqual(chunk, vec2(0.0))) && all(lessThan(chunk, vec2(uMaskGrid.z)))
      && builtOver(texelFetch(uMask, ivec2(chunk), 0).r)) discard;
  vec4 c = (vFlags & FLAG_BAKED_FLOOR) != 0 ? sampleFloor(vUv, vFloorLayer)
      : (vFlags & FLAG_BILLBOARD) != 0 ? sampleTree(vUv, vRect) : sampleSprite(uTexture, vUv, vRect, vFlags);
  Surface s = resolve(c, vLocal, vNormal, vFill, vFlags, vPos, uEye);
  vec3 n = s.foliage ? vec3(0.0, 1.0, 0.0) : s.normal;
  float lit = sunlit(vPos, n, uEye, s.foliage);
  if (uDebugView == 6) { fragColor = vec4(sunMapsView(vPos, n), 1.0); return; }
  vec3 color = wetGround(shade(s.albedo, s.light, n, lit), s.normal, s.foliage ? 0.0 : s.light.a, uWet);
  float fog = max(fogAt(d, uFogRange), hazeAt(d, uHaze));
  vec2 block = floor((vCellWorld + vLocal.xz) / 64.0) - uShellGrid.xy;
  float tier = texelFetch(uShellMask, ivec2(clamp(block, vec2(0.0), vec2(uShellGrid.z - 1.0))), 0).r * 255.0;
  color = lodView(color, abs(tier - 2.0) < 0.5 ? LOD_SHELL_FULL : LOD_SHELL_LITE);
  vec3 fogged = mix(color, fogColor(uFogColor, (vPos - uEye) / d), fog);
  fragColor = vec4(uLodView == 2 ? color : fogged, 1.0);
}
