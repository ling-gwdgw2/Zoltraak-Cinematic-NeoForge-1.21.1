#version 150

in vec2 uv;
in float age;
in float energy;

out vec4 fragColor;

void main() {
    float x = uv.x;
    float y = abs(uv.y * 2.0 - 1.0);

    // 1. Aerodynamic tip taper and smooth base launch fade
    float baseFade = smoothstep(0.0, 0.04, x);
    float tipTaper = smoothstep(1.0, 0.86, x);

    // 2. High-Frequency Supersonic Longitudinal Ionization Wave
    float waveA = sin(x * 110.0 - age * 11.5);
    float waveB = cos(x * 42.0 + age * 4.8);
    float ionization = (waveA * 0.6 + waveB * 0.4) * 0.025;

    // 3. Searing Super-Gaussian Plasma Core (1:1 Anime Pure White Light)
    float coreRadius = (0.16 + ionization) * max(0.2, tipTaper);
    float core = exp(-pow(y / max(coreRadius, 0.005), 4.0));

    // 4. Electric Cyan Ionized Vapor Sheath
    float sheathRadius = (0.42 + ionization * 1.5) * max(0.3, tipTaper);
    float sheath = exp(-pow(y / max(sheathRadius, 0.01), 2.0)) * 0.68;

    // 5. Atmospheric Plasma Glow & High-Velocity Discharge Filaments
    float aura = exp(-y * 3.8) * 0.22;
    float filament = pow(max(0.0, sin(x * 75.0 - age * 13.0 + y * 24.0)), 14.0) * exp(-y * 5.5) * 0.24;

    // 6. Spectral Color Ramp: Searing White -> Electric Cyan -> Deep Arcane Blue
    vec3 deepBlue     = vec3(0.18, 0.48, 0.95);
    vec3 electricCyan = vec3(0.55, 0.88, 1.00);
    vec3 whiteHotCore = vec3(0.98, 1.00, 1.00);

    vec3 color = mix(deepBlue, electricCyan, clamp(sheath * 1.4, 0.0, 1.0));
    color = mix(color, whiteHotCore, clamp(core, 0.0, 1.0));
    color += electricCyan * filament;

    // 7. Edge cutoff and alpha composition
    float edgeCut = 1.0 - smoothstep(0.85, 1.0, y);
    float alpha = (core * 1.1 + sheath + aura + filament) * energy * baseFade * tipTaper * edgeCut;
    if (alpha < 0.004) discard;

    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
