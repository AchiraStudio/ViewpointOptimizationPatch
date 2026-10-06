#version 330



in vec2 vUv;
uniform sampler2D uAlbedo;
uniform sampler2D uNormal;
uniform sampler2D uEngineLight;
uniform int uDebugView;
out vec4 fragColor;
#include "lib/camera.glsl"
#include "lib/sun.glsl"
#include "lib/flash.glsl"
#include "lib/shade.glsl"
void main() {
  float depth = texture(uDepth, vUv).r;
  if (depth >= 1.0) discard;
  vec4 albedo = texture(uAlbedo, vUv);
  vec4 normal = texture(uNormal, vUv);
  vec4 engine = texture(uEngineLight, vUv);
  vec4 light = vec4(engine.rgb, normal.a);
  if (uDebugView == 1) { fragColor = vec4(albedo.rgb, 1.0); return; }
  if (uDebugView == 2) { fragColor = vec4(light.rgb, 1.0); return; }

  if (albedo.a > 0.125 && albedo.a < 0.375) { fragColor = vec4(albedo.rgb, 1.0); return; }
  vec3 pos = scenePos(vUv, depth);
  vec3 n = normalize(normal.xyz);
  bool foliage = albedo.a > 0.875;
  float lit = sunlit(pos, n, uEye, foliage);
  if (uDebugView == 6) { fragColor = vec4(sunMapsView(pos, n), 1.0); return; }
  float keep = lampShadowKeep(light.rgb, pos, n);
  if (uDebugView == 5) {

    vec2 reach = lampShadowReach(pos, n);
    fragColor = vec4(keep, keep, reach.x > 0.0 ? reach.y / reach.x : 1.0, 1.0);
    return;
  }
  light.rgb *= keep;


  light.rgb = max(light.rgb, min(lamps(pos, n), light.rgb * 1.35 + 0.02)) + torches(pos, n);
  fragColor = vec4(shade(albedo.rgb, light, n, lit) + albedo.rgb * flashlight(pos, n, foliage), 1.0);
}
