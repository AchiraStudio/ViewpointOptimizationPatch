



#ifndef VIEWPOINT_PRECIP_LOOK
#define VIEWPOINT_PRECIP_LOOK
#include "lib/common.glsl"



const float DROP = 0.003, MIN_PIXELS = 0.7;
const float FLAKE = 0.026, FLAKE_PIXELS = 1.2;
const float SPLASH = 0.065;

float precipBrightness(bool snow, float random) { return snow ? 1.0 : 0.7 + 0.6 * random; }


vec3 precipStreak(vec3 fall) { return fall; }



vec4 precipLook(float mode, vec2 local, float fade, vec3 light, float intensity) {
  float a;
  vec3 color = light;
  if (mode > 1.5) {
    float r = length(local);
    a = (smoothstep(0.3, 0.0, abs(r - 0.75)) * 0.7 + smoothstep(0.3, 0.0, r) * 0.5) * fade;
    color *= 1.25;
  } else if (mode > 0.5) {
    a = smoothstep(1.0, 0.3, length(local)) * 0.85 * fade;
    color *= 1.15;
  } else {
    a = pow(1.0 - abs(local.x), 1.5) * (1.0 - local.y * local.y) * 0.55 * fade;
    color *= 1.35;
  }
  a = min(a * mix(0.7, 1.0, intensity), 1.0);
  return vec4(shoulder(color) * a, a);
}
#endif
