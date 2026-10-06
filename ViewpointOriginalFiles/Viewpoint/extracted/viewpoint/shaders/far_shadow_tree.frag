#version 330



in vec3 vPos;
in vec2 vCorner;
flat in vec2 vLocal;
flat in float vCover;
uniform vec2 uCellWorld;
#include "lib/far_crown.glsl"
#include "lib/far_caster.glsl"
void main() {
  bool trunk = inTrunk(vCorner);
  if (!inCrown(vCorner) && !trunk) discard;
  if (!trunk && !inBranches(vCorner, vLocal, vCover)) discard;
  vec2 world = uCellWorld + vLocal;
  if (withinNear(vPos) || nearBuilt(world) || shellStands(world)) discard;
}
