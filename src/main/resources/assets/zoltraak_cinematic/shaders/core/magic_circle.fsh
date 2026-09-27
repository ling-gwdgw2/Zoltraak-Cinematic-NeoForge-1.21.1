#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 uv;
in vec4 vColor;

out vec4 fragColor;

void main() {
    vec2 p = uv * 2.0 - 1.0;
    float r = length(p);
    if (r > 1.05) discard;

    // Sub-pixel smooth circular boundary
    float circleEdge = 1.0 - smoothstep(0.97, 1.0, r);
    float theta = atan(p.y, p.x);
    float t = GameTime * 2.5;

    // 1. Chromatic dispersion on rune edge sampling
    vec2 pDir = (r > 0.001) ? (p / r) : vec2(0.0);
    vec2 uv_r = uv - pDir * 0.003;
    vec2 uv_g = uv;
    vec2 uv_b = uv + pDir * 0.003;

    float rawR = texture(Sampler0, uv_r).r;
    float rawG = texture(Sampler0, uv_g).g;
    float rawB = texture(Sampler0, uv_b).b;
    float rawA = texture(Sampler0, uv).a;

    float runeLuma = max(rawR, max(rawG, rawB)) * rawA;

    // 2. High-frequency mana shimmer & traveling wave
    float radialWave = 0.5 + 0.5 * sin(r * 24.0 - t * 4.0);
    float shimmer = 0.90 + 0.10 * sin(theta * 16.0 + r * 28.0 - t * 6.0);
    float runeEmission = runeLuma * (1.0 + 0.50 * radialWave) * shimmer;

    // 3. Crisp aperture corona rim
    float apertureRim = exp(-pow((r - 0.22) * 32.0, 2.0)) * (0.70 + 0.30 * sin(theta * 8.0 - t * 3.5));

    // 4. Delicate needle star glint at aperture center
    float crossH = exp(-abs(p.y) * 55.0) * exp(-abs(p.x) * 5.0);
    float crossV = exp(-abs(p.x) * 55.0) * exp(-abs(p.y) * 5.0);
    float starGlint = (crossH + crossV) * (0.45 + 0.35 * sin(t * 4.5));

    // 5. Vivid Holy Palette: Electric Cyan Sheath with White-Hot Core
    vec3 cyanGlow = vec3(0.12, 0.88, 1.0);
    vec3 whiteHot = vec3(0.96, 0.99, 1.0);
    vec3 runeColor = mix(cyanGlow, whiteHot, clamp(runeEmission * 0.85, 0.0, 1.0));

    vec3 finalRGB = runeColor * runeEmission;
    finalRGB += vec3(0.08, 0.92, 1.0) * apertureRim * 1.3;
    finalRGB += whiteHot * starGlint * 1.4;

    float totalAlpha = (runeLuma * 0.98 + apertureRim * 0.85 + starGlint * 0.80) * circleEdge * vColor.a * ColorModulator.a;
    if (totalAlpha < 0.008) discard;

    fragColor = vec4(finalRGB * vColor.rgb * ColorModulator.rgb, clamp(totalAlpha, 0.0, 1.0));
}
