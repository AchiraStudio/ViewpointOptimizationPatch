





uniform float uFlashOn;
uniform vec3 uFlashPos;
uniform vec3 uFlashDir;
uniform vec3 uFlashColor;
uniform vec3 uFlashCone;
uniform mat4 uFlashMatrix;
uniform float uFlashTexel;
uniform sampler2DShadow uFlashShadow;
float flashBeam(float c) {
  if (c <= uFlashCone.x) return 0.0;
  float a = acos(min(c, 1.0)), hot = uFlashCone.y;
  float rim = (a - hot * 1.15) / (hot * 0.3);
  float spill = smoothstep(uFlashCone.x, mix(uFlashCone.x, 1.0, 0.35), c);
  return exp(-a * a / (hot * hot)) + 0.1 * exp(-rim * rim) + 0.18 * spill;
}
float flashFalloff(float d) {
  float x = d / uFlashCone.z;
  float window = clamp(1.0 - x * x * x * x, 0.0, 1.0);
  return window * window / (d * d + 0.25);
}
float flashShadow(vec3 pos, vec3 n, float d, bool filtered) {
#if !PIPELINE_FLASHLIGHT_SHADOW
  return 1.0;
#else
  vec4 sc = uFlashMatrix * vec4(pos + n * (0.01 + uFlashTexel * d * 2.0), 1.0);
  if (sc.w <= 0.0) return 0.0;
  sc.xyz /= sc.w;
  if (!filtered) return texture(uFlashShadow, sc.xyz);
  vec2 texel = 1.0 / vec2(textureSize(uFlashShadow, 0));
  float lit = 0.0;
  for (int y = -1; y <= 1; y++)
    for (int x = -1; x <= 1; x++)
      lit += texture(uFlashShadow, vec3(sc.xy + vec2(x, y) * texel, sc.z));
  return lit / 9.0;
#endif
}

vec3 flashlight(vec3 pos, vec3 n, bool foliage) {
  if (uFlashOn < 0.5) return vec3(0.0);
  vec3 d = pos - uFlashPos;
  float dist = length(d);
  if (dist >= uFlashCone.z) return vec3(0.0);
  vec3 l = d / max(dist, 1.0e-4);
  float beam = flashBeam(dot(l, uFlashDir));
  float facing = foliage ? 0.6 : dot(n, -l);
  if (beam <= 0.0 || facing <= 0.0) return vec3(0.0);
  return uFlashColor * beam * facing * flashFalloff(dist) * flashShadow(pos, foliage ? -l : n, dist, true);
}
