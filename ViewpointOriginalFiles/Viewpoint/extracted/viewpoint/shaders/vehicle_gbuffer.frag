#version 330




in vec3 vNormal;
in vec3 vPos;
uniform vec3 uEye;
flat in vec4 vLight;
layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gLight;
layout(location = 3) out vec4 gVelocity;
#include "lib/motion.glsl"
#include "lib/vehicle.glsl"
void main() {
  Vehicle v = vehicle();
  if (v.window > 0.5) discard;
  vec3 n = normalize(vNormal);
  if (dot(n, uEye - vPos) < 0.0) n = -n;
  bool lamp = v.lamp.a > 0.5;
  gAlbedo = vec4(lamp ? v.lamp.rgb : v.colour, lamp ? 0.25 : 0.0);
  gNormal = vec4(n, vLight.a);
  gLight = vLight;
  gVelocity = motion();
}
