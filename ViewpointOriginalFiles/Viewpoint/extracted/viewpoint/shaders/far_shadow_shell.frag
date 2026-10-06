#version 330




in vec2 vUv;
in vec3 vLocal;
flat in vec4 vRect;
flat in int vFlags;
in vec3 vNormal;
flat in vec2 vCellWorld;
uniform sampler2D uTexture;
uniform sampler2D uTreeAtlas;
#include "lib/sample_sprite.glsl"
#include "lib/crown_shadow.glsl"
#include "lib/shell_root.glsl"
#include "lib/far_caster.glsl"
void main() {
  if (nearBuilt(vCellWorld + maskedSquare(vLocal, vFlags))) discard;
  if ((vFlags & FLAG_BILLBOARD) != 0) crownShadow(uTreeAtlas, vUv, vRect);
  else if (dot(vNormal, vNormal) < 0.6) crownShadow(uTexture, vUv, vRect);
  else sampleSprite(uTexture, vUv, vRect, vFlags);
}
