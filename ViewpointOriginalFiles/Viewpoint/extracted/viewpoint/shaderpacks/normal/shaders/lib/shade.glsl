




#ifndef OPTION_FACE_SHADING
#define OPTION_FACE_SHADING 1.0
#endif
uniform vec3 uFogColor;

vec3 lamps(vec3 pos, vec3 n) {
  return vec3(0.0);
}
uniform int uTorchCount;
uniform vec4 uTorchPos[8];
uniform vec4 uTorchDir[8];
uniform vec3 uTorchColor[8];
vec3 torches(vec3 pos, vec3 n) {
  vec3 sum = vec3(0.0);
  for (int i = 0; i < uTorchCount; i++) {
    vec3 d = pos - uTorchPos[i].xyz;
    float dist = length(d);
    float reach = uTorchPos[i].w;
    if (dist >= reach) continue;
    vec3 l = d / max(dist, 1.0e-4);
    float edge = uTorchDir[i].w;
    float beam = edge <= -1.0 ? 1.0 : smoothstep(edge, mix(edge, 1.0, 0.3), dot(l, uTorchDir[i].xyz));
    float facing = 0.35 + 0.65 * max(dot(n, -l), 0.0);
    sum += uTorchColor[i] * beam * facing * (1.0 - dist / reach);
  }
  return sum;
}

float faceShade(vec3 n) {
  vec3 a = abs(n);
  float side = (a.x * 0.6 + a.z * 0.8) / max(a.x + a.z, 1.0e-4);
  float shade = n.y >= 0.0 ? mix(side, 1.0, n.y) : mix(side, 0.5, -n.y);
  return mix(1.0, shade, OPTION_FACE_SHADING);
}
vec3 shade(vec3 albedo, vec4 light, vec3 n, float lit) {
  return albedo * light.rgb * faceShade(n);
}
