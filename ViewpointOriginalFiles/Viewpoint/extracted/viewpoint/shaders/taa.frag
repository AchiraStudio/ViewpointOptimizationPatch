#version 330










in vec2 vUv;
uniform sampler2D uCurrent;
uniform sampler2D uHistory;
uniform mat4 uUnproject;
uniform mat4 uPrevious;
uniform vec2 uTexel;
uniform float uHistoryValid;
uniform float uHistoryWeight;
uniform sampler2D uFarDepth;
uniform mat4 uFarUnproject;
uniform vec2 uFarRange;
uniform sampler2D uVelocity;
out vec4 fragColor;
#include "lib/camera.glsl"
vec3 toYCoCg(vec3 c) { return vec3(0.25 * c.r + 0.5 * c.g + 0.25 * c.b, 0.5 * c.r - 0.5 * c.b, -0.25 * c.r + 0.5 * c.g - 0.25 * c.b); }
vec3 toRgb(vec3 c) { float t = c.x - c.z; return vec3(t + c.y, c.x + c.z, t - c.y); }
vec3 catmullRom(sampler2D tex, vec2 uv) {
  vec2 size = 1.0 / uTexel;
  vec2 p = uv * size;
  vec2 p1 = floor(p - 0.5) + 0.5;
  vec2 f = p - p1;
  vec2 w0 = f * (-0.5 + f * (1.0 - 0.5 * f));
  vec2 w1 = 1.0 + f * f * (-2.5 + 1.5 * f);
  vec2 w2 = f * (0.5 + f * (2.0 - 1.5 * f));
  vec2 w3 = f * f * (-0.5 + 0.5 * f);
  vec2 w12 = w1 + w2;
  vec2 t0 = (p1 - 1.0) * uTexel, t3 = (p1 + 2.0) * uTexel, t12 = (p1 + w2 / w12) * uTexel;
  vec3 r = texture(tex, vec2(t0.x, t0.y)).rgb * w0.x * w0.y + texture(tex, vec2(t12.x, t0.y)).rgb * w12.x * w0.y
         + texture(tex, vec2(t3.x, t0.y)).rgb * w3.x * w0.y + texture(tex, vec2(t0.x, t12.y)).rgb * w0.x * w12.y
         + texture(tex, vec2(t12.x, t12.y)).rgb * w12.x * w12.y + texture(tex, vec2(t3.x, t12.y)).rgb * w3.x * w12.y
         + texture(tex, vec2(t0.x, t3.y)).rgb * w0.x * w3.y + texture(tex, vec2(t12.x, t3.y)).rgb * w12.x * w3.y
         + texture(tex, vec2(t3.x, t3.y)).rgb * w3.x * w3.y;
  return max(r, vec3(0.0));
}
float farLinear(float z) { z = z * 2.0 - 1.0; return 2.0 * uFarRange.x * uFarRange.y / (uFarRange.y + uFarRange.x - z * (uFarRange.y - uFarRange.x)); }
void main() {
  vec3 current = texture(uCurrent, vUv).rgb;
  float ownDepth = texture(uDepth, vUv).r;
  bool farOn = uFarRange.x > 0.0;
  float ownFar = farOn ? texture(uFarDepth, vUv).r : 1.0;
  bool far = ownDepth >= 1.0 && ownFar < 1.0;
  float distanceNow = far ? farLinear(ownFar) : linearDepth(ownDepth);
  float nearestFar = 1.0;
  vec3 m1 = vec3(0.0), m2 = vec3(0.0);
  vec3 lo = vec3(1.0e9), hi = vec3(-1.0e9);
  float nearest = 1.0;
  vec2 nearestUv = vUv;
  for (int y = -1; y <= 1; y++) {
    for (int x = -1; x <= 1; x++) {
      vec2 uv = vUv + vec2(x, y) * uTexel;
      vec3 c = toYCoCg(texture(uCurrent, uv).rgb);
      m1 += c; m2 += c * c;
      lo = min(lo, c); hi = max(hi, c);
      float d = texture(uDepth, uv).r;
      if (d < nearest) { nearest = d; nearestUv = uv; }
      if (farOn) nearestFar = min(nearestFar, texture(uFarDepth, uv).r);
    }
  }
  if (uHistoryValid < 0.5) { fragColor = vec4(current, distanceNow); return; }




  vec4 scene = nearest < 1.0 || nearestFar >= 1.0 ? uUnproject * vec4(vUv * 2.0 - 1.0, nearest * 2.0 - 1.0, 1.0)
                                                  : uFarUnproject * vec4(vUv * 2.0 - 1.0, nearestFar * 2.0 - 1.0, 1.0);
  vec4 previous = uPrevious * vec4(scene.xyz / scene.w, 1.0);
  vec2 previousUv = previous.xy / previous.w * 0.5 + 0.5;
  float previousDistance = previous.w;
  vec4 motion = texture(uVelocity, nearestUv);
  if (motion.w > 0.5) { previousUv = vUv - motion.xy; previousDistance = motion.z; }
  if (any(lessThan(previousUv, vec2(0.0))) || any(greaterThan(previousUv, vec2(1.0)))) { fragColor = vec4(current, distanceNow); return; }
  vec3 mean = m1 / 9.0;
  vec3 sigma = sqrt(max(m2 / 9.0 - mean * mean, 0.0));
  vec3 boxLo = max(lo, mean - 1.25 * sigma), boxHi = min(hi, mean + 1.25 * sigma);
  vec3 history = toYCoCg(catmullRom(uHistory, previousUv));

  vec3 centre = 0.5 * (boxHi + boxLo), extent = 0.5 * (boxHi - boxLo) + 1.0e-4;
  vec3 v = history - centre;
  vec3 a = abs(v / extent);
  float m = max(a.x, max(a.y, a.z));
  if (m > 1.0) history = centre + v / m;





  ivec2 size = textureSize(uHistory, 0);
  ivec2 h = clamp(ivec2(previousUv * vec2(size)), ivec2(1), size - 2);
  float storedLo = 1.0e9, storedHi = 0.0;
  for (int y = -1; y <= 1; y++) {
    for (int x = -1; x <= 1; x++) {
      float stored = texelFetch(uHistory, h + ivec2(x, y), 0).a;
      storedLo = min(storedLo, stored);
      storedHi = max(storedHi, stored);
    }
  }
  float outside = max(max(storedLo - previousDistance, previousDistance - storedHi), 0.0) / max(previousDistance, 0.1);
  float trust = ownDepth >= 1.0 && !far ? 1.0 : 1.0 - smoothstep(0.03, 0.12, outside);
  float w = uHistoryWeight * trust;
  vec3 c = toYCoCg(current);
  float wc = (1.0 - w) / (1.0 + c.x), wh = w / (1.0 + history.x);
  fragColor = vec4(toRgb((c * wc + history * wh) / max(wc + wh, 1.0e-6)), distanceNow);
}
