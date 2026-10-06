#version 330





#include "constants.glsl"
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUv;
layout(location = 2) in vec4 aRect;
layout(location = 3) in vec3 aNormal;
layout(location = 4) in float aFill;
layout(location = 5) in int aFlags;
layout(location = 6) in vec4 aOrigin;
layout(location = 7) in vec4 aCell;
uniform mat4 uViewProjection;
uniform vec3 uEye;

uniform bool uCasting;
out vec2 vUv;
out vec3 vLocal;
flat out float vFill;
flat out int vFlags;
flat out vec4 vRect;
out vec3 vNormal;
out vec3 vPos;
flat out vec2 vCellWorld;
flat out float vFloorLayer;
flat out vec2 vFade;
void main() {
  vCellWorld = aCell.xy;
  vFloorLayer = aOrigin.w;
  vFade = aCell.zw;
  bool billboard = (aFlags & (FLAG_BILLBOARD | FLAG_FACING)) != 0;
  vUv = aUv; vRect = aRect; vNormal = aNormal; vFill = billboard ? 0.0 : aFill; vFlags = aFlags;
  vLocal = vec3(-aPos.x, aPos.y, -aPos.z);
  vPos = aPos + aOrigin.xyz;
  if (billboard) {
    vec2 toEye = uEye.xz - vPos.xz;
    vPos.xz += normalize(vec2(toEye.y, -toEye.x) + vec2(1.0e-6, 0.0)) * aFill;
  }
  bool floorTile = (aFlags & FLAG_SOLID_FLOOR) != 0;
  bool artFace = (aFlags & (FLAG_SINGLE_SIDED | FLAG_COVER)) == FLAG_SINGLE_SIDED;
  float pull = float((aFlags & FLAG_DECAL) != 0) + 3.0 * float((aFlags & FLAG_COVER) != 0)
             + 2.0 * float((aFlags >> FLAG_LAYER_SHIFT) & 3) + 0.5 * float(floorTile) + 0.25 * float(artFace);
  if (uCasting || !floorTile && abs(aNormal.y) > 0.9 && uEye.y < vPos.y) pull = 0.0;
  if (pull > 0.0) {
    vec3 toEye = uEye - vPos;
    float d = length(toEye);
    vPos += toEye / max(d, 0.001) * min((0.001 + d * d * 6.0e-6) * pull, 0.5 * d);
  }
  gl_Position = uViewProjection * vec4(vPos, 1.0);
}
