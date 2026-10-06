

uniform vec2 uAlphaRange = vec2(0.3, 2.0);















#include "constants.glsl"
#include "lib/lanczos.glsl"
uniform int uWireframe;
uniform int uSpriteFilter;

vec4 sampleSprite(sampler2D tex, vec2 uv, vec4 rect, int flags, bool solid, float emptyAlpha) {
  if (uWireframe != 0) {
    vec4 w = textureLod(tex, uv, 0.0);
    return vec4(uWireframe == 2 || w.a < 0.3 ? vec3(0.8) : w.rgb, 1.0);
  }
  vec2 dx = dFdx(uv), dy = dFdy(uv);
  if (uv.x < rect.x || uv.y < rect.y || uv.x > rect.z || uv.y > rect.w) discard;
  vec2 size = vec2(textureSize(tex, 0));
  ivec2 lo = ivec2(floor(rect.xy * size + 0.5)), hi = max(ivec2(floor(rect.zw * size + 0.5)) - 1, lo);
  vec4 c = uSpriteFilter == FILTER_LANCZOS && magnified(size, dx, dy)
      ? lanczos(tex, uv, lo, hi, false) : textureGrad(tex, uv, dx, dy);
  float own = textureLod(tex, uv, 0.0).a;
  c.rgb = min(c.rgb * min(own / max(c.a, 0.004), 4.0), vec3(1.0));
  c.a = (flags & FLAG_SOLID_FLOOR) != 0 && own >= 0.3 || solid && own >= emptyAlpha ? 1.0 : own;
  if (c.a < uAlphaRange.x || c.a >= uAlphaRange.y) discard;
  return c;
}
vec4 sampleSprite(sampler2D tex, vec2 uv, vec4 rect, int flags) { return sampleSprite(tex, uv, rect, flags, false, 1.0); }
vec4 sampleSprite(sampler2D tex, vec2 uv, vec4 rect) { return sampleSprite(tex, uv, rect, 0); }
