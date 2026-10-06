#version 330


#include "constants.glsl"
#include "lib/mesh_vertex.glsl"
uniform mat4 uViewProjection;
uniform vec3 uEye;
out vec2 vUv;
out vec3 vLocal;
flat out float vFill;
flat out int vFlags;
flat out vec4 vRect;
out vec3 vNormal;
out vec3 vPos;
out float vDistance;
flat out vec2 vFade;
flat out vec2 vLightOrigin;
flat out float vFloorLayer;
void main() {
  MeshVertex v = meshVertex(uEye);
  if (v.hidden) { gl_Position = vec4(2.0, 2.0, 2.0, 1.0); return; }
  vUv = v.uv; vRect = v.rect; vNormal = v.normal; vFill = v.fill;
  vFlags = v.flags;
  vFade = v.fade.xy;
  vFloorLayer = v.fade.w;
  vLocal = v.local;
  vLightOrigin = lightOrigin(v, uEye.y);
  vPos = pulled(v, uEye);
  gl_Position = uViewProjection * vec4(vPos, 1.0);
  vDistance = gl_Position.w;
}
