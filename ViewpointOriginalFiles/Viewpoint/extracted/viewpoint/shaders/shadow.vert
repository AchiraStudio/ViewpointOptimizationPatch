#version 330




#include "lib/plant.glsl"
#include "lib/pack_model.glsl"
#include "lib/corpse_card.glsl"
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUv;
layout(location = 2) in vec4 aRect;
layout(location = 3) in vec3 aNormal;
layout(location = 5) in int aFlags;
layout(location = 6) in vec4 aMesh;
layout(location = 7) in vec4 aMeshFade;
uniform mat4 uViewProjection;
uniform vec3 uEye;


uniform float uOwnFixture;
out vec2 vUv;
flat out vec4 vRect;
flat out vec2 vFade;
flat out float vFoliage;
flat out int vFlags;
void main() {
  vec3 pos = aPos, offset = aMesh.xyz;
  vUv = aUv; vRect = aRect; vFade = aMeshFade.xy; vFlags = aFlags;
  vFoliage = dot(aNormal, aNormal) < 0.6 ? 1.0 : 0.0;
  if (uPlants != 0) {
    PlantVertex plant = plantVertex(aMesh.xyz, uEye);
    pos = plant.pos; vUv = plant.uv; vRect = plant.rect; vFlags = plant.flags; vFoliage = 1.0;
  }
  if (uModels != 0) {
    ModelVertex model = modelVertex(aPos, aUv, aNormal);
    bool own = uOwnFixture > 0.0 && length((model.foot + model.mesh.xyz).xz - uEye.xz) < uOwnFixture;
    if (model.hidden || own) { gl_Position = vec4(2.0, 2.0, 2.0, 1.0); return; }
    pos = model.pos; vUv = model.uv; vRect = model.rect; vFlags = model.flags; vFoliage = 0.0;
    offset = model.mesh.xyz; vFade = model.fade.xy;
  }
  if (uCards != 0) {
    CardVertex card = cardVertex();
    pos = card.pos; vUv = card.uv; vRect = card.rect; vFlags = card.flags; vFoliage = 0.0;
    offset = card.mesh.xyz; vFade = card.fade.xy;
  }
  gl_Position = uViewProjection * vec4(pos + offset, 1.0);
}
