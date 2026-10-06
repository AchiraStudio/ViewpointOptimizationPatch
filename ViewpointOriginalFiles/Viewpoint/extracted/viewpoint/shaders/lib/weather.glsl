


#ifndef VIEWPOINT_WEATHER
#define VIEWPOINT_WEATHER
uniform float uRain;
uniform float uSnow;
uniform vec2 uWind;
uniform float uCloudType;
uniform vec2 uCloudWind;


uniform sampler2D uWeatherMap;
uniform vec2 uWeatherOffset;
uniform float uWeatherSpan;


vec4 weatherAt(vec3 p) { return textureLod(uWeatherMap, (p.xz + uWeatherOffset) / uWeatherSpan + 0.5, 0.0); }



const int ALONG_SIDE = 48;
uniform float uCoverAlong[2 * ALONG_SIDE + 1];
uniform float uTypeAlong[2 * ALONG_SIDE + 1];
uniform float uAlongStep;


uniform vec2 uCloudCarried;


vec2 skyAlong(vec3 p) {
  vec2 downwind = length(uCloudWind) > 1.0e-3 ? normalize(uCloudWind) : vec2(1.0, 0.0);
  float x = clamp(dot(p.xz, downwind) / uAlongStep + float(ALONG_SIDE), 0.0, float(2 * ALONG_SIDE) - 1.0e-3);
  int i = int(x);
  float f = x - float(i);
  return vec2(mix(uCoverAlong[i], uCoverAlong[i + 1], f), mix(uTypeAlong[i], uTypeAlong[i + 1], f));
}






const float RAIN_PERIOD = 600.0;
uniform sampler2D uWeatherNoise;
uniform vec2 uWindOffset;
uniform float uRainClock, uSheets, uGusts;



const float SHEET_TILE = 4096.0, SHEET_FINE_TILE = 1536.0, SHEET_SECONDS = 20.0;
const float GUST_TILE = 2048.0, GUST_SECONDS = 5.0;



const float SHEET_WIDE = 0.7, SHEET_FINE = 0.3, SHEET_GAIN = 1.5, GUST = 0.6;

const vec2 SLICE_STEP = vec2(0.7548777, 0.5698403);


vec4 airNoise(vec2 q, float tile, float seconds) {
  float t = uRainClock / seconds, slices = RAIN_PERIOD / seconds;
  float k = floor(t), f = t - k, w = f * f * (3.0 - 2.0 * f);
  vec2 a = fract(mod(k, slices) * SLICE_STEP), b = fract(mod(k + 1.0, slices) * SLICE_STEP);
  vec4 v = mix(textureLod(uWeatherNoise, q / tile + a, 0.0), textureLod(uWeatherNoise, q / tile + b, 0.0), w);
  return 0.5 + (v - 0.5) / sqrt(w * w + (1.0 - w) * (1.0 - w));
}


float sheetsAt(vec3 p) {
  vec2 q = p.xz + uWindOffset;
  float wide = airNoise(q, SHEET_TILE, SHEET_SECONDS).r, fine = airNoise(q, SHEET_FINE_TILE, SHEET_SECONDS).g;
  float n = (SHEET_WIDE * (wide - 0.5) + SHEET_FINE * (fine - 0.5)) / length(vec2(SHEET_WIDE, SHEET_FINE));
  return 1.0 + uSheets * clamp(SHEET_GAIN * 2.0 * n, -1.0, 1.0);
}

float gustAt(vec3 p) { return uGusts * GUST * 2.0 * (airNoise(p.xz + uWindOffset, GUST_TILE, GUST_SECONDS).b - 0.5); }


uniform float uRainAlong[2 * ALONG_SIDE + 1];

float rainAlong(vec3 p) {
  vec2 downwind = length(uCloudWind) > 1.0e-3 ? normalize(uCloudWind) : vec2(1.0, 0.0);
  float x = clamp(dot(p.xz, downwind) / uAlongStep + float(ALONG_SIDE), 0.0, float(2 * ALONG_SIDE) - 1.0e-3);
  int i = int(x);

  return mix(uRainAlong[i], uRainAlong[i + 1], x - float(i));
}



uniform vec4 uLightning;
#endif
