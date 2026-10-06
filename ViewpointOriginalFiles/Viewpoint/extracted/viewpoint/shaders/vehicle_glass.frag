#version 330




in vec3 vNormal;
in vec3 vPos;
uniform vec3 uEye;
flat in vec4 vLight;
uniform float uGlassAlpha;
#include "lib/translucent.glsl"
#include "lib/vehicle.glsl"
#include "lib/sun.glsl"
#include "lib/flash.glsl"
#include "lib/shade.glsl"
void main() {
  Vehicle v = vehicle();
  if (v.window < 0.5 || v.gone > 0.5) discard;
  vec3 n = normalize(vNormal);
  if (dot(n, uEye - vPos) < 0.0) n = -n;
  vec4 light = vLight;
  float lit = sunlit(vPos, n, uEye, false);
#ifdef VIEWPOINT_LAMP_SHADOW
  light.rgb = lampShadowed(light.rgb, vPos, n);
#endif
  light.rgb = max(light.rgb, min(lamps(vPos, n), light.rgb * 1.35 + 0.02)) + torches(vPos, n);
  vec3 colour = shade(v.colour, light, n, lit) + v.colour * flashlight(vPos, n, false);
  translucent(mix(colour, v.lamp.rgb, v.lamp.a), uGlassAlpha, length(vPos - uEye));
}
