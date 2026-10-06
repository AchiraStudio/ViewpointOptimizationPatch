#version 330





in vec2 vUv;
uniform sampler2D uColor;
uniform sampler2D uAlbedo;
uniform sampler2D uGi;
uniform sampler2D uVolume;
uniform sampler2D uBloom;
uniform vec2 uGiSize;
uniform int uDebugView;
uniform float uGiOn;
uniform float uVolumeOn;
uniform float uBloomStrength;
uniform float uExposure;
uniform vec3 uFogColor;
uniform vec2 uFogRange;
uniform vec2 uHaze;
out vec4 fragColor;
#include "lib/camera.glsl"
#include "lib/common.glsl"
#include "lib/fog.glsl"
#include "lib/fog_amount.glsl"
#ifndef OPTION_EXPOSURE
#define OPTION_EXPOSURE 1.0
#endif
#ifndef OPTION_CONTRAST
#define OPTION_CONTRAST 1.25
#endif
#ifndef OPTION_SATURATION
#define OPTION_SATURATION 1.05
#endif
#ifndef OPTION_VIBRANCE
#define OPTION_VIBRANCE 0.25
#endif
const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

const vec3 OCCLUDED_DARK = vec3(0.74, 0.75, 0.78);
const vec3 OCCLUDED_BRIGHT = vec3(0.6, 0.62, 0.66);

const float BLOOM_LEVELS = 5.0;

const float BLOOM_HAZE = 0.5;

const float TONE_SHOULDER = 0.9;
const float TONE_WHITE = 2.0;
const float TONE_MID_IN = 0.18;
const float TONE_MID_OUT = 0.19;

const float DARK_KEEP = 0.5;
const float DARK_END = 0.15;

const float DARK_DESATURATION = 0.35;
const float DARK_COLOUR_END = 0.06;
const float WHITE_DRIFT = 0.5;
const float WHITE_START = 0.9;


void upsample(float depth, out vec4 gi, out vec4 volume) {
  float d0 = linearDepth(depth);
  vec2 texel = 1.0 / uGiSize;
  vec2 base = (floor(vUv * uGiSize - 0.5) + 0.5) * texel;
  gi = vec4(0.0); volume = vec4(0.0);
  float weight = 0.0;
  for (int y = 0; y < 2; y++) {
    for (int x = 0; x < 2; x++) {
      vec2 uv = base + vec2(x, y) * texel;
      float d = linearDepth(texture(uDepth, uv).r);
      float w = 1.0 / (1.0 + abs(d - d0) * 8.0 / max(d0, 0.5));
      gi += texture(uGi, uv) * w;
      volume += texture(uVolume, uv) * w;
      weight += w;
    }
  }
  gi /= max(weight, 0.0001);
  volume /= max(weight, 0.0001);
}

vec3 toneCurve(vec3 x) {
  float a = OPTION_CONTRAST, ad = OPTION_CONTRAST * TONE_SHOULDER;
  float midA = pow(TONE_MID_IN, a), midAd = pow(TONE_MID_IN, ad);
  float whiteA = pow(TONE_WHITE, a), whiteAd = pow(TONE_WHITE, ad);
  float span = (whiteAd - midAd) * TONE_MID_OUT;
  float b = (whiteA * TONE_MID_OUT - midA) / span;
  float c = (whiteAd * midA - whiteA * midAd * TONE_MID_OUT) / span;
  x = min(x, vec3(TONE_WHITE));
  return pow(x, vec3(a)) / (pow(x, vec3(ad)) * b + c);
}
vec3 grade(vec3 color) {
  vec3 x = max(color * uExposure * OPTION_EXPOSURE, vec3(0.0));
  vec3 toned = toneCurve(x);
  vec3 linear = x * (TONE_MID_OUT / TONE_MID_IN);
  color = mix(toned, linear, DARK_KEEP * (1.0 - smoothstep(0.0, DARK_END, x)));
  float luma = dot(color, LUMA);
  color = mix(vec3(luma), color, 1.0 - DARK_DESATURATION * (1.0 - smoothstep(0.0, DARK_COLOUR_END, luma)));
  color = mix(color, vec3(luma), WHITE_DRIFT * smoothstep(WHITE_START, 1.0, luma));
  color = mix(vec3(luma), color, OPTION_SATURATION);
  float top = max(color.r, max(color.g, color.b));
  float chroma = top > 1.0e-4 ? (top - min(color.r, min(color.g, color.b))) / top : 0.0;
  color = mix(vec3(luma), color, 1.0 + OPTION_VIBRANCE * (1.0 - chroma));
  return max(color, vec3(0.0));
}
void main() {
  vec3 color = texture(uColor, vUv).rgb;
  if (uDebugView == 1 || uDebugView == 2 || uDebugView == 6) { fragColor = vec4(color, 1.0); return; }
  float depth = texture(uDepth, vUv).r;
  vec4 gi = vec4(0.0, 0.0, 0.0, 1.0), volume = vec4(0.0, 0.0, 0.0, 1.0);
#if PIPELINE_INDIRECT || PIPELINE_VOLUMETRICS
  upsample(depth, gi, volume);
#endif
  if (uDebugView == 3) { fragColor = vec4(volume.rgb * 4.0, 1.0); return; }
  if (uDebugView == 4) { fragColor = vec4(gi.rgb * 4.0, 1.0); return; }

  if (any(isnan(color)) || any(isinf(color))) color = vec3(0.0);
  if (any(isnan(gi)) || any(isinf(gi))) gi = vec4(0.0, 0.0, 0.0, 1.0);
  if (any(isnan(volume)) || any(isinf(volume))) volume = vec4(0.0, 0.0, 0.0, 1.0);
  gi = mix(vec4(0.0, 0.0, 0.0, 1.0), gi, uGiOn);
  volume = mix(vec4(0.0, 0.0, 0.0, 1.0), volume, uVolumeOn);
  vec3 pos = scenePos(vUv, depth);
  if (depth < 1.0) {
    vec3 occluded = mix(OCCLUDED_DARK, OCCLUDED_BRIGHT, smoothstep(0.1, 0.8, dot(color, LUMA)));
    color = color * mix(occluded, vec3(1.0), gi.a) + texture(uAlbedo, vUv).rgb * gi.rgb;
  }
  color = color * volume.a + volume.rgb;
  if (depth < 1.0) {
    float fog = max(fogAt(linearDepth(depth), uFogRange), hazeAt(distance(pos, uEye), uHaze));
    color = mix(color, fogColor(uFogColor, normalize(pos - uEye)), fog);
  }

  if (uBloomStrength > 0.0) color = mix(color, texture(uBloom, vUv).rgb / BLOOM_LEVELS, uBloomStrength * BLOOM_HAZE);
  color = grade(color);
  color += (ign(gl_FragCoord.xy + 17.0) - 0.5) / 255.0;
  fragColor = vec4(color, 1.0);
}
