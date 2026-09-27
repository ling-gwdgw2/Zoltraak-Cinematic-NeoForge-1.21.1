#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 uv;
in vec4 vColor;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec2 p = uv * 2.0 - 1.0;
    float r = length(p);
    if (r > 1.05) discard;

    // Sub-pixel smooth circular boundary
    float circleEdge = 1.0 - smoothstep(0.97, 1.0, r);
    float theta = atan(p.y, p.x);
    float t = GameTime * 2.5;

    // 1. Dispersion & chromatic offset for demonic runes
    vec2 pDir = (r > 0.001) ? (p / r) : vec2(0.0);
    vec2 uv_r = uv - pDir * 0.004;
    vec2 uv_g = uv;
    vec2 uv_b = uv + pDir * 0.004;

    float rawR = texture(Sampler0, uv_r).r;
    float rawG = texture(Sampler0, uv_g).g;
    float rawB = texture(Sampler0, uv_b).b;
    float rawA = texture(Sampler0, uv).a;

    float runeLuma = max(rawR, max(rawG, rawB)) * rawA;

    // 2. High-frequency abyssal pulse & swirling mana distortion
    float radialWave = 0.5 + 0.5 * sin(r * 26.0 - t * 4.5);
    float shimmer = 0.88 + 0.12 * sin(theta * 14.0 + r * 30.0 - t * 5.5);
    float runeEmission = runeLuma * (1.0 + 0.45 * radialWave) * shimmer;

    // 3. Central aperture rim (sinister violet corona)
    float apertureRim = exp(-pow((r - 0.22) * 32.0, 2.0)) * (0.75 + 0.25 * sin(theta * 8.0 - t * 3.5));

    // 4. Sharp cross glint at the eye of the aperture
    float crossH = exp(-abs(p.y) * 55.0) * exp(-abs(p.x) * 5.0);
    float crossV = exp(-abs(p.x) * 55.0) * exp(-abs(p.y) * 5.0);
    float starGlint = (crossH + crossV) * (0.50 + 0.30 * sin(t * 4.5));

    // 5. Abyssal / Black Zoltraak Palette:
    // Obsidian / Deep Violet Base -> Vibrant Demonic Purple -> Pure White-Violet Core
    vec3 abyssalShadow = vec3(0.08, 0.02, 0.15);
    vec3 demonicViolet = vec3(0.68, 0.18, 0.98);
    vec3 whiteViolet    = vec3(0.98, 0.88, 1.0);

    vec3 runeColor = mix(demonicViolet, whiteViolet, clamp(runeEmission * 0.80, 0.0, 1.0));
    vec3 finalRGB = mix(abyssalShadow, runeColor, runeEmission);
    finalRGB += demonicViolet * apertureRim * 1.5;
    finalRGB += whiteViolet * starGlint * 1.6;

    // 6. Alpha calculation
    float totalAlpha = (runeLuma * 0.95 + apertureRim * 0.90 + starGlint * 0.85) * circleEdge * vColor.a * ColorModulator.a;
    if (totalAlpha < 0.008) discard;

    fragColor = vec4(finalRGB * vColor.rgb * ColorModulator.rgb, clamp(totalAlpha, 0.0, 1.0));
}
