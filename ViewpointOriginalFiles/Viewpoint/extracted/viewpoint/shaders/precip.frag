#version 330



#include "lib/precip_look.glsl"
in vec2 vLocal;
in float vFade;
in vec3 vColor;
uniform float uMode, uIntensity;
out vec4 fragColor;
void main() { fragColor = precipLook(uMode, vLocal, vFade, vColor, uIntensity); }
