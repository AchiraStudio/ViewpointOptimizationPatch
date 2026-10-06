#version 330



in vec2 vUv;
in vec3 vLocal;
flat in vec4 vRect;
in vec3 vNormal;
in vec3 vPos;
flat in float vFill;
flat in int vFlags;
flat in float vFloorLayer;
uniform sampler2D uTexture;
uniform vec3 uEye;
layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gLight;
#include "lib/fade.glsl"
#include "lib/sample_sprite.glsl"
#include "lib/sample_floor.glsl"
#include "lib/sample_light.glsl"
#include "lib/surface.glsl"
#include "lib/lod.glsl"
#include "lib/glass.glsl"
void main() {
  fadeDither();

  if ((vFlags & FLAG_BAKED_FLOOR) != 0 && fadeNoise() >= floorShare(vFloorLayer)) discard;
  bool solid = glassSolid(vFlags, vPos, vNormal);
  vec4 c = (vFlags & FLAG_BAKED_FLOOR) != 0
      ? sampleFloor(vUv, vFloorLayer)
      : sampleSprite(uTexture, vUv, vRect, vFlags, solid, uGlassRange.w);
  Surface s = resolve(c, vLocal, vNormal, vFill, vFlags, vPos, uEye);
  bool lowFloor = (vFlags & FLAG_BAKED_FLOOR) != 0 && floorLow(vFloorLayer);
  bool water = (vFlags & FLAG_WATER) != 0;
  float material = s.foliage ? 1.0 : water ? 0.75 : vFill > 0.5 ? 0.5 : 0.0;
  gAlbedo = vec4(lodView(s.albedo, lowFloor ? LOD_FLOOR_LOW : LOD_NEAR), material);
  gNormal = vec4(water ? vec3(0.0, 1.0, 0.0) : s.normal, s.light.a);
  float sky = sampleSky(vLocal, s.foliage ? vec3(0.0) : s.normal);
  gLight = vec4(uWireframe == 2 ? vec3(1.0) : s.light.rgb, sky);
}
