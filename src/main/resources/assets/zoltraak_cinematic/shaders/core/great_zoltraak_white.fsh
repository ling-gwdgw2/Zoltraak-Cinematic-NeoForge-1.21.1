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

    // 1. Surface normal and Fresnel glancing angle for 3D volumetric cylinder
    vec3 normal = normalize(cross(dFdx(viewPosition), dFdy(viewPosition)));
    float facing = abs(dot(normal, normalize(-viewPosition)));

    // 2. Longitudinal Catastrophic Plasma Flow & Turbulence
    float flow = noise2D(vec2(x * 22.0 - age * 1.2, y * 7.0));
    float pulse = sin(x * 38.0 - age * 2.5);

    // 3. Volumetric Core Radius and Tapering
    float beamRadius = 0.26 + 0.06 * flow + 0.015 * pulse;
    float dist = isVolume ? (1.05 - 0.72 * pow(facing, 0.65)) : (abs(y) / max(beamRadius, 0.01));

    // 4. White-Hot Incandescent Core
    float core = 1.0 - smoothstep(0.82, 1.02, dist);

    // 5. Electric Cyan Ionized Boundary & Glancing Rim Flash
    float rim = exp(-pow((dist - 1.0) * 18.0, 2.0));
    float arcDischarge = pow(noise2D(vec2(x * 160.0 - age * 5.0, y * 35.0)), 6.0) * rim * 1.5;

    // 6. Spectral Palette: White-Hot Core -> Electric Cyan -> Royal Arcane Blue
    vec3 deepAzure    = vec3(0.15, 0.45, 0.95);
    vec3 electricCyan = vec3(0.52, 0.86, 1.00);
    vec3 whiteCore    = vec3(0.98, 1.00, 1.00);

    vec3 color = mix(deepAzure, electricCyan, clamp(rim * 1.5, 0.0, 1.0));
    color = mix(color, whiteCore, clamp(core, 0.0, 1.0));
    color += electricCyan * arcDischarge;

    if (isVolume) {
        color *= mix(vec3(0.65, 0.85, 1.0), vec3(1.0), pow(facing, 0.45));
    }

    // 7. Alpha Composition
    float alpha = max(core, rim * 0.85 + arcDischarge) * energy;
    if (!isVolume) {
        alpha *= (1.0 - core * 0.2);
    }
    if (alpha < 0.003) discard;

    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
