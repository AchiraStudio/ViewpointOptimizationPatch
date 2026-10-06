#version 330




in vec2 vUv;
uniform sampler2D uColor;
uniform sampler2D uAlbedo;
uniform sampler2D uGi;
uniform sampler2D uVolume;
uniform sampler2D uBloom;
uniform vec2 uGiSize;
uniform float uDesaturation;
uniform int uDebugView;
uniform float uNight;
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
    color = color * mix(vec3(0.74, 0.75, 0.78), vec3(1.0), gi.a) + texture(uAlbedo, vUv).rgb * gi.rgb;
  }
  color = color * volume.a + volume.rgb;
  if (depth < 1.0) {
    float fog = max(fogAt(linearDepth(depth), uFogRange), hazeAt(distance(pos, uEye), uHaze));
    color = mix(color, fogColor(uFogColor, normalize(pos - uEye)), fog);
  }
  if (uBloomStrength > 0.0) color += texture(uBloom, vUv).rgb * uBloomStrength;


  color = shoulder(max(color, vec3(0.0)) * uExposure);
  color += (ign(gl_FragCoord.xy + 17.0) - 0.5) / 255.0;
  fragColor = vec4(color, 1.0);
}
