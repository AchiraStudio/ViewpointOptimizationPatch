#version 330

in vec2 vUv;
uniform sampler2D uColor;
uniform vec2 uTexel;
uniform float uSharpen;
out vec4 fragColor;
void main() {
  vec3 c = texture(uColor, vUv).rgb;
  vec3 around = texture(uColor, vUv + vec2(uTexel.x, 0.0)).rgb + texture(uColor, vUv - vec2(uTexel.x, 0.0)).rgb
              + texture(uColor, vUv + vec2(0.0, uTexel.y)).rgb + texture(uColor, vUv - vec2(0.0, uTexel.y)).rgb;
  fragColor = vec4(clamp(c + (c * 4.0 - around) * uSharpen, 0.0, 1.0), 1.0);
}
