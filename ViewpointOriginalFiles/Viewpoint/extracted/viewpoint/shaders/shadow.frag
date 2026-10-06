#version 330

in vec2 vUv;
flat in vec4 vRect;
flat in float vFoliage;
flat in int vFlags;
uniform sampler2D uTexture;
#include "lib/fade.glsl"
#include "lib/sample_sprite.glsl"
#include "lib/crown_shadow.glsl"
void main() {
  fadeDither();
  if ((vFlags & FLAG_BAKED_FLOOR) != 0) return;
  if (vFoliage > 0.5) crownShadow(uTexture, vUv, vRect);
  else sampleSprite(uTexture, vUv, vRect);
}
