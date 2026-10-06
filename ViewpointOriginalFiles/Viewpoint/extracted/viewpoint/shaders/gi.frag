#version 330












in vec2 vUv;
uniform sampler2D uColor;
uniform sampler2D uNormal;
uniform mat3 uViewRot;
uniform float uGiRadius;
uniform float uGiStrength;
uniform vec3 uOriginFrac;
uniform sampler2D uAlbedo;
uniform sampler2D uRooms;
uniform sampler2D uFlashDepth;
uniform mat4 uFlashUnproject;
uniform mat4 uViewProjection;
uniform float uFloorY;
uniform float uSunBounce;
uniform float uFrameNoise;
out vec4 fragColor;
#include "lib/camera.glsl"
#include "lib/sun.glsl"
#include "lib/flash.glsl"
#include "lib/grids.glsl"
#include "lib/common.glsl"
#include "constants.glsl"
const float PI = 3.14159265, GOLDEN = 2.3999632;

float roomAt(vec3 p) {
  vec2 cell = gridCell(p.xz);
  if (any(lessThan(cell, vec2(0.0))) || any(greaterThanEqual(cell, vec2(uExposureSize)))) return -1.0;
  return floor(texelFetch(uRooms, ivec2(cell), 0).r * 255.0 + 0.5);
}
bool onStorey(vec3 p) { return p.y > uFloorY - 0.3 && p.y < uFloorY + 2.6; }

vec3 albedoAt(vec3 q) {
  vec4 c = uViewProjection * vec4(q, 1.0);
  if (c.w > 0.05) {
    vec2 uv = c.xy / c.w * 0.5 + 0.5;
    if (all(greaterThan(uv, vec2(0.0))) && all(lessThan(uv, vec2(1.0)))
        && abs(linearDepth(texture(uDepth, uv).r) - c.w) < 0.05 + 0.02 * c.w) return texture(uAlbedo, uv).rgb;
  }
  return vec3(0.35);
}


vec3 sunBounce(vec3 p, vec3 n, float room, float jitter, float turn) {
#if !PIPELINE_SHADOWS
  return vec3(0.0);
#endif
  if (uSunBounce <= 0.0 || uSunStrength <= 0.0 || uSunDir.y <= 0.0 || room < 0.5 || room > 254.5 || !onStorey(p)) return vec3(0.0);
  const int K = 10;
  const float R = 3.5;
  vec3 sum = vec3(0.0);
  for (int i = 0; i < K; i++) {
    float r = R * sqrt((float(i) + jitter) / float(K)), a = turn + float(i) * GOLDEN;
    vec3 q = vec3(p.x + cos(a) * r, uFloorY + 0.02, p.z + sin(a) * r);
    vec3 d = q - p;
    float dist2 = dot(d, d), dist = sqrt(dist2);
    float cosP = dot(n, d) / dist, cosQ = -d.y / dist;
    if (cosP <= 0.0 || cosQ <= 0.0 || abs(roomAt(q) - room) > 0.5) continue;
    float lit = sunAt(q + vec3(0.0, 0.05, 0.0));
    if (lit <= 0.0) continue;
    sum += albedoAt(q) * lit * cosP * cosQ / (dist2 + 0.3) * smoothstep(0.2, 0.8, dist);
  }
  return sum * uSunColor * (uSunStrength * uSunBounce * uSunDir.y * R * R / float(K));
}
vec3 flashPoint(vec2 t) {
  vec4 s = uFlashUnproject * vec4(t * 2.0 - 1.0, texture(uFlashDepth, t).r * 2.0 - 1.0, 1.0);
  return s.xyz / s.w;
}



vec3 flashBounce(vec3 p, vec3 n, float room, float jitter, float turn) {
#if !PIPELINE_FLASHLIGHT_SHADOW
  return vec3(0.0);
#endif
  if (uFlashOn < 0.5) return vec3(0.0);
  const int K = 12;
  float A = acos(uFlashCone.x);
  vec3 w = uFlashDir;
  vec3 t1 = normalize(cross(w, abs(w.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0))), t2 = cross(w, t1);
  bool gate = room >= 0.0 && onStorey(p);
  vec2 texStep = 2.0 / vec2(textureSize(uFlashDepth, 0));
  vec3 sum = vec3(0.0);
  for (int i = 0; i < K; i++) {
    float u = (float(i) + jitter) / float(K), a = A * u * u, phi = turn + float(i) * GOLDEN;
    vec3 omega = w * cos(a) + (t1 * cos(phi) + t2 * sin(phi)) * sin(a);
    vec4 c = uFlashMatrix * vec4(uFlashPos + omega, 1.0);
    vec2 t = c.xy / c.w;
    if (texture(uFlashDepth, t).r >= 1.0) continue;
    vec3 q = flashPoint(t);
    vec3 nq = cross(flashPoint(t + vec2(texStep.x, 0.0)) - q, flashPoint(t + vec2(0.0, texStep.y)) - q);
    float len = length(nq);
    nq = len > 1.0e-8 ? nq / len : -omega;
    if (dot(nq, uFlashPos - q) < 0.0) nq = -nq;
    vec3 d = p - q;
    float dist2 = dot(d, d), dist = sqrt(dist2);
    float cosQ = dot(nq, d) / dist, cosP = -dot(n, d) / dist;
    if (cosQ <= 0.0 || cosP <= 0.0) continue;
    if (gate && onStorey(q) && abs(roomAt(q + nq * 0.3) - room) > 0.5) continue;
    float lens = distance(q, uFlashPos);
    float flux = flashBeam(cos(a)) * flashFalloff(lens) * lens * lens * 4.0 * PI * A * u * sin(a) / float(K);
    sum += albedoAt(q) * flux * cosQ * cosP / (PI * (dist2 + 0.2)) * smoothstep(0.2, 0.8, dist);
  }
  return sum * uFlashColor;
}
void main() {
  float depth = texture(uDepth, vUv).r;
  if (depth >= 1.0) { fragColor = vec4(0.0, 0.0, 0.0, 1.0); return; }
  vec3 p = viewPos(vUv);
  vec3 n = normalize(transpose(uViewRot) * texture(uNormal, vUv).xyz);
  if (dot(n, -p) < 0.0) n = -n;
  float dist = -p.z;
  if (dist >= 45.0) { fragColor = vec4(0.0, 0.0, 0.0, 1.0); return; }
  float radius = uGiRadius * (1.0 + dist * 0.04);
  vec2 reach = radius / (dist * uCamera.zw) * 0.5;
  vec3 world = uEye + uViewRot * p + uOriginFrac;
  float turn = ign(floor(world.xz * 25.0) + floor(world.y * 25.0) * vec2(0.37, 0.61)) * 6.2831853;
  float jitter = ign(floor(world.xz * 47.0) + floor(world.y * 31.0) * vec2(0.91, 0.13));
  vec3 indirect = vec3(0.0);
  float occlusion = 0.0;
  for (int i = 0; i < 8; i++) {
    float a = turn + float(i) * 0.7853982;
    vec2 dir = vec2(cos(a), sin(a));
    float horizon = 0.0;
    for (int s = 1; s <= 5; s++) {
      float t = (float(s) - 0.5 + jitter * 0.5) / 5.0;
      t = t * t;
      vec2 uv = vUv + dir * reach * t;
      if (uv.x < 0.0 || uv.y < 0.0 || uv.x > 1.0 || uv.y > 1.0) break;
      vec3 v = viewPos(uv) - p;
      float d = length(v);
      if (d > radius || d < 0.001) continue;
      float e = dot(v / d, n);
      float w = 1.0 - d / radius;
      if (e > horizon) {
        indirect += texture(uColor, uv).rgb * (e - horizon) * w;
        horizon = mix(horizon, e, w);
      }
    }
    occlusion += horizon;
  }
  float ao = clamp(1.0 - occlusion / 8.0 * 1.3, 0.0, 1.0);
  ao = mix(ao, 1.0, smoothstep(25.0, 45.0, dist));

  indirect *= 1.0 - smoothstep(SHELL_BAND * 0.5, SHELL_BAND, dist);

  vec3 pos = uEye + uViewRot * p, sn = uViewRot * n;
  float room = roomAt(pos + sn * 0.3);
  room = room > 254.5 ? 0.0 : room;
  float noise = fract(ign(gl_FragCoord.xy) + uFrameNoise * 0.618034);
  float spin = (fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) + uFrameNoise * 0.381966) * 6.2831853;
  vec3 bounce = sunBounce(pos, sn, room, noise, spin) + flashBounce(pos, sn, room, noise, spin);
  fragColor = vec4(indirect / 8.0 * uGiStrength + bounce, ao);
}
