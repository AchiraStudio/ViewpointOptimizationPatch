#version 330

in vec2 vUv;
uniform sampler2D uRain;
out vec4 fragColor;
void main() { fragColor = texture(uRain, vUv); }
