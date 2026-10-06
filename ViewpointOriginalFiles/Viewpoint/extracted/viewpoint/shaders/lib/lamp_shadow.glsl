










#ifndef VIEWPOINT_LAMP_SHADOW
#define VIEWPOINT_LAMP_SHADOW
#include "constants.glsl"
#if PIPELINE_LAMP_SHADOWS
uniform int uLampShadowCount;
uniform vec4 uLampShadowSlot[PIPELINE_LAMP_SHADOWS];
uniform float uLampShadowFloor;
uniform sampler2DArrayShadow uLampShadow;
const vec3 LAMP_FORWARD[6] = vec3[6](vec3(1.0, 0.0, 0.0), vec3(-1.0, 0.0, 0.0), vec3(0.0, 1.0, 0.0),
                                     vec3(0.0, -1.0, 0.0), vec3(0.0, 0.0, 1.0), vec3(0.0, 0.0, -1.0));
const vec3 LAMP_UP[6] = vec3[6](vec3(0.0, 1.0, 0.0), vec3(0.0, 1.0, 0.0), vec3(0.0, 0.0, 1.0),
                                vec3(0.0, 0.0, 1.0), vec3(0.0, 1.0, 0.0), vec3(0.0, 1.0, 0.0));

const float LAMP_LEVEL_COST = 3.0;
#endif

float lampShadow(int i, vec3 pos, vec3 n) {
#if PIPELINE_LAMP_SHADOWS
  if (i >= uLampShadowCount) return 1.0;
  vec4 slot = uLampShadowSlot[i];
  vec3 v = pos - uLightPos[i].xyz;
  float m = max(abs(v.x), max(abs(v.y), abs(v.z)));

  v += n * (0.02 + 3.0 * m / float(PIPELINE_LAMP_SHADOW_RESOLUTION));
  vec3 a = abs(v);
  int face = a.x >= a.y && a.x >= a.z ? (v.x > 0.0 ? 0 : 1) : a.y >= a.z ? (v.y > 0.0 ? 2 : 3) : (v.z > 0.0 ? 4 : 5);
  vec3 forward = LAMP_FORWARD[face], right = cross(forward, LAMP_UP[face]), up = cross(right, forward);
  float along = dot(v, forward), near = slot.z, far = slot.w;
  if (along <= near) return 1.0;
  float depth = 0.5 + 0.5 * ((far + near) / (far - near) - 2.0 * far * near / ((far - near) * along));
  vec2 uv = 0.5 + 0.5 * vec2(dot(v, right), dot(v, up)) / along;
  float texel = 1.0 / float(PIPELINE_LAMP_SHADOW_RESOLUTION), lit = 0.0;
  for (int k = 0; k < 4; k++) {
    lit += texture(uLampShadow, vec4(uv + (vec2(k & 1, k >> 1) - 0.5) * texel, slot.x + float(face), depth));
  }
  return mix(1.0, lit * 0.25, slot.y);
#else
  return 1.0;
#endif
}

vec2 lampShadowReach(vec3 pos, vec3 n) {
  vec2 r = vec2(0.0);
#if PIPELINE_LAMP_SHADOWS
  for (int i = 0; i < uLampShadowCount; i++) {
    vec3 d = pos - uLightPos[i].xyz;
    float levels = abs(floor((pos.y - uLightPos[i].y + LAMP_HEIGHT) / SQRT6 + 0.02));
    float cost = length(d.xz) + LAMP_LEVEL_COST * levels, radius = uLightPos[i].w;
    if (cost >= radius) continue;
    vec3 c = uLightColor[i];
    float e = max(c.r, max(c.g, c.b)) * (1.0 - cost / radius);
    r = max(r, vec2(e, e * lampShadow(i, pos, n)));
  }
#endif
  return r;
}

float lampShadowKeep(vec3 light, vec3 pos, vec3 n) {
  vec2 r = lampShadowReach(pos, n);
  float grids = max(light.r, max(light.g, light.b));
  if (r.x <= 0.0 || grids <= 0.0) return 1.0;
  float ratio = r.x / grids;
  float theirs = smoothstep(0.55, 0.85, ratio) * (1.0 - smoothstep(1.25, 1.6, ratio));
#if PIPELINE_LAMP_SHADOWS
  return mix(1.0, max(r.y / r.x, uLampShadowFloor), theirs);
#else
  return 1.0;
#endif
}
vec3 lampShadowed(vec3 light, vec3 pos, vec3 n) {
  return light * lampShadowKeep(light, pos, n);
}
#endif
