



#ifndef VIVID_CLOUD_BLOCKS
#define VIVID_CLOUD_BLOCKS
#include "lib/cloud_shape.glsl"

#ifndef OPTION_CLOUD_STYLE
#define OPTION_CLOUD_STYLE 0
#endif
#ifndef OPTION_CLOUD_STYLE_LAYERED
#define OPTION_CLOUD_STYLE_LAYERED 1
#endif


const float BLOCK_FLOOR = 420.0;
const float BLOCK_ROOF = 470.0;
const float BLOCK_WIDTH = 40.0;

vec2 blockGrid(vec3 p) { return (p.xz + uCloudOffset) / BLOCK_WIDTH; }

float blockFull(vec2 cell) {
  vec3 p = vec3((cell.x + 0.5) * BLOCK_WIDTH - uCloudOffset.x, 0.5 * (BLOCK_FLOOR + BLOCK_ROOF), (cell.y + 0.5) * BLOCK_WIDTH - uCloudOffset.y);
  float n = textureLod(uCloudNoise, cloudSpace(p), max(log2(BLOCK_WIDTH / CLOUD_TEXEL), 0.0)).r;
  return step(1.0 - weatherAt(p).r, n);
}
#endif
