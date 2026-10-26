#version 150

in vec2 uv;
in float age;
in float energy;
in float span;
in float halfWidth;

out vec4 fragColor;

// Clean analytic hash and smooth noise for demonic particulate dissipation
float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float smoothNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    float x = uv.x;
    float y = abs(uv.y * 2.0 - 1.0);

    // 1. Aerodynamic spearhead taper at beam head and launch fade
    float baseFade = smoothstep(0.0, 0.05, x);
    float tipTaper = smoothstep(1.0, 0.85, x);

    // 2. Turbulent Abyssal Void Flow
    float flow = smoothNoise(vec2(x * 36.0 - age * 1.8, y * 8.0 + age * 0.35));
    float sootSwirl = smoothNoise(vec2(x * 95.0 - age * 3.5, y * 22.0));

    // 3. Demonic Ray Dimensions with Supersonic Wavefront
    float beamRadius = (0.34 + 0.04 * (flow - 0.5)) * max(0.2, tipTaper);
    float coreDist = y / max(beamRadius, 0.008);

    // 4. Pitch-Black Void Core (Event Horizon devouring ambient light)
    float voidCore = 1.0 - smoothstep(0.78, 0.98, coreDist);

    // 5. Crackling Violet Mana Filament & Mineral Disintegration Boundary
    float filament = pow(max(0.0, sin(x * 65.0 - age * 9.0 + y * 28.0 + flow * 4.0)), 12.0) * exp(-y * 4.5);
    float boundarySheath = exp(-pow((coreDist - 0.95) * 8.0, 2.0)) * 0.75;
    float dustSparkle = smoothstep(0.65, 0.92, sootSwirl) * boundarySheath;

    // 6. Demonic Spectral Palette: Pitch Black -> Abyssal Violet -> Royal Purple -> Silver Lavender
    vec3 pitchBlackVoid = vec3(0.005, 0.003, 0.009);
    vec3 deepCharcoal   = vec3(0.08, 0.04, 0.11);
    vec3 royalViolet    = vec3(0.55, 0.20, 0.82);
    vec3 silverLavender = vec3(0.88, 0.82, 0.98);

    vec3 color = mix(deepCharcoal, royalViolet, smoothstep(0.1, 0.8, boundarySheath));
    color = mix(color, silverLavender, clamp(dustSparkle * 1.3 + filament * 0.8, 0.0, 1.0));
    color = mix(color, pitchBlackVoid, voidCore);

    // 7. Alpha composition
    float edgeCut = 1.0 - smoothstep(0.90, 1.0, y);
    float alpha = max(voidCore * 0.98, (boundarySheath + filament + dustSparkle) * 0.92) * energy * baseFade * tipTaper * edgeCut;
    if (alpha < 0.004) discard;

    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
