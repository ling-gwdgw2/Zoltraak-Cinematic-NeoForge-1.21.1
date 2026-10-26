#version 150

in vec2 uv;
in vec3 viewPosition;
in float age;
in float energy;
in float span;
in float halfWidth;

out vec4 fragColor;

// Clean analytic noise
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
    bool isVolume = uv.y >= 2.0;
    float x = uv.x;
    float y = isVolume ? sin((uv.y - 2.0) * 6.2831853) * 0.28 : (uv.y * 2.0 - 1.0);

    if (x < 0.0) discard;

    // 1. Surface normal and facing angle for 3D volumetric cylinder
    vec3 normal = normalize(cross(dFdx(viewPosition), dFdy(viewPosition)));
    float facing = abs(dot(normal, normalize(-viewPosition)));

    // 2. Turbulent Abyssal Singularity Wave
    float flow = noise2D(vec2(x * 24.0 - age * 0.9, y * 7.5));
    float pulse = sin(x * 35.0 - age * 2.1);

    // 3. Volumetric Core Radius and Tapering
    float beamRadius = 0.26 + 0.06 * flow + 0.015 * pulse;
    float dist = isVolume ? (1.05 - 0.72 * pow(facing, 0.65)) : (abs(y) / max(beamRadius, 0.01));

    // 4. Pitch-Black Void Core (Event horizon devouring ambient light)
    float voidCore = 1.0 - smoothstep(0.78, 0.98, dist);

    // 5. Demonic Royal Violet Corona & Silver Lavender Discharge
    float rim = exp(-pow((dist - 1.0) * 16.0, 2.0));
    float discharge = pow(noise2D(vec2(x * 150.0 - age * 4.5, y * 32.0)), 6.0) * rim * 1.6;

    // 6. Demonic Spectral Palette: Pitch Black -> Charcoal Violet -> Royal Purple -> Silver Lavender
    vec3 pitchBlackVoid = vec3(0.005, 0.003, 0.009);
    vec3 deepCharcoal   = vec3(0.08, 0.04, 0.12);
    vec3 royalViolet    = vec3(0.55, 0.20, 0.82);
    vec3 silverLavender = vec3(0.88, 0.82, 0.98);

    vec3 color = mix(deepCharcoal, royalViolet, clamp(rim * 1.4, 0.0, 1.0));
    color = mix(color, silverLavender, clamp(discharge * 1.4, 0.0, 1.0));
    color = mix(color, pitchBlackVoid, voidCore);

    if (isVolume) {
        color += deepCharcoal * pow(facing, 2.0);
    }

    // 7. Alpha Composition
    float alpha = max(voidCore * 0.98, rim * 0.88 + discharge) * energy;
    if (!isVolume) {
        alpha *= (1.0 - voidCore * 0.2);
    }
    if (alpha < 0.003) discard;

    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
