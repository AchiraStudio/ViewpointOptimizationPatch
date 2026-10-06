




#include "lib/cloud_blocks.glsl"


const int SHADOW_SAMPLES = 3;
const float SHADOW_FOOTPRINT = 24.0;
const float SHADOW_EXTINCTION = 0.007;

const float SHADOW_FLOOR = 0.3;
const float BLOCK_SHADOW_EDGE = 0.08;
float cloudShadow(vec3 pos) {
#if !PIPELINE_CLOUDS
  return 1.0;
#else
  if (uSunDir.y <= 0.0) return 1.0;
#if OPTION_CLOUD_STYLE == OPTION_CLOUD_STYLE_LAYERED
  vec3 at = pos + uSunDir * ((0.5 * (BLOCK_FLOOR + BLOCK_ROOF) - pos.y) / uSunDir.y);
  vec2 g = blockGrid(at) - 0.5;
  vec2 cell = floor(g), f = smoothstep(0.5 - BLOCK_SHADOW_EDGE, 0.5 + BLOCK_SHADOW_EDGE, fract(g));
  float full = mix(mix(blockFull(cell), blockFull(cell + vec2(1.0, 0.0)), f.x),
                   mix(blockFull(cell + vec2(0.0, 1.0)), blockFull(cell + vec2(1.0, 1.0)), f.x), f.y);
  return mix(1.0, SHADOW_FLOOR, full);
#else
  vec2 layer = cloudLayer();
  float span = (layer.y - layer.x) / float(SHADOW_SAMPLES), density = 0.0;
  for (int i = 0; i < SHADOW_SAMPLES; i++) {
    float h = layer.x + (float(i) + 0.5) * span;
    density += cloudShape(pos + uSunDir * ((h - pos.y) / uSunDir.y), SHADOW_FOOTPRINT, false);
  }
  return mix(SHADOW_FLOOR, 1.0, exp(-density * span / uSunDir.y * SHADOW_EXTINCTION));
#endif
#endif
}
