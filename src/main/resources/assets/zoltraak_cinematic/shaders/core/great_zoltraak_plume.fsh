#version 150

in vec2 uv;
in float age;
in float energy;
in float dark;
in float luminous;

out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise2D(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash12(i), hash12(i + vec2(1.0, 0.0)), f.x),
        mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), f.x),
        f.y
    );
}

void main() {
    float x = uv.x;
    float y = abs(uv.y * 2.0 - 1.0);

    // 1. Turbulent Lateral Fluid Waves
    float wave = noise2D(vec2(x * 38.0 - age * 0.65, y * 8.5));
    float dist = y + 0.045 * (wave - 0.5) + 0.015 * sin(x * 160.0 - age * 1.5);

    // 2. Base Attachment and Edge Illumination
    float attached = 1.0 - step(0.1, abs(luminous - 0.5));
    float edgeLight = mix(luminous, smoothstep(0.15, 0.45, x), attached);

    // 3. Dense Plume Core & Luminous Crest
    float core = 1.0 - smoothstep(0.65, 0.82, dist);
    float crestGlow = exp(-pow((dist - 0.78) * 14.0, 2.0));
    float hotRidge = exp(-pow((dist - 0.76) * 36.0, 2.0));

    // 4. Fine Dielectric Sparks & Fluid Breaking
    vec2 cell = max(fwidth(uv) * 1.25, vec2(0.001));
    float spark = step(0.58, hash12(floor(uv / cell) + floor(age * 0.45))) *
                  smoothstep(0.65, 0.76, dist) * (1.0 - smoothstep(0.82, 0.98, dist));

    // 5. Dual Palette Selection (Celestial Cyan vs Demonic Obsidian)
    vec3 lightBody   = vec3(0.92, 0.98, 1.00);
    vec3 cyanGlow    = vec3(0.15, 0.62, 1.00);
    vec3 whiteHot    = vec3(0.85, 1.00, 1.00);

    vec3 darkBody    = vec3(0.010, 0.006, 0.018);
    vec3 violetGlow  = vec3(0.58, 0.18, 0.88);
    vec3 lavenderHot = vec3(0.90, 0.84, 1.00);

    vec3 bodyCol  = mix(lightBody, darkBody, dark);
    vec3 glowCol  = mix(cyanGlow, violetGlow, dark);
    vec3 ridgeCol = mix(whiteHot, lavenderHot, dark);

    vec3 color = mix(bodyCol, glowCol, crestGlow * edgeLight);
    color = mix(color, ridgeCol, max(hotRidge, spark * 0.9) * edgeLight);

    // 6. Alpha Composition
    float alpha = max(core * 0.95, max(crestGlow * 0.85, spark) * edgeLight) * energy;
    alpha *= mix(1.0, smoothstep(0.0, 0.15, x), attached);
    if (alpha < 0.003) discard;

    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
