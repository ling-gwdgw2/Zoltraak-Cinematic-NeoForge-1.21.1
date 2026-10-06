#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec3 worldPos;
in vec3 viewPosition;

out vec4 fragColor;

// Procedural hash and noise for dielectric mana micro-arcs and crystalline lattice turbulence
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    return noise(p) * 0.55 + noise(p * 2.15) * 0.30 + noise(p * 4.41) * 0.15;
}

void main() {
    // 1. Surface normal in view space & camera direction (NaN-safe guarded normalization)
    vec3 vCross = cross(dFdx(viewPosition), dFdy(viewPosition));
    float vLen = length(vCross);
    vec3 viewNormal = vLen > 0.0001 ? vCross / vLen : vec3(0.0, 0.0, 1.0);

    float viewLen = length(viewPosition);
    vec3 viewDir = viewLen > 0.0001 ? -viewPosition / viewLen : vec3(0.0, 0.0, 1.0);

    float facing = clamp(abs(dot(viewNormal, viewDir)), 0.0, 1.0);

    // 2. High-clarity Chromatic Dispersion Fresnel rim (crystal clear facing, radiant at grazing edges)
    // Red, green, and blue disperse at slightly different refraction powers simulating mana prism optics
    float fresnelR = pow(1.0 - facing, 2.7);
    float fresnelG = pow(1.0 - facing, 2.3);
    float fresnelB = pow(1.0 - facing, 1.9);
    float rimGlow = pow(1.0 - facing, 4.5);

    // 3. Smooth Harmonic Mana Shimmer & Procedural Dielectric Micro-Arcs
    float t = GameTime * 2.5;
    float localDist = length(worldPos.xz);
    float pulse1 = sin(worldPos.y * 1.5 - t * 2.0 + localDist * 1.2);
    float pulse2 = cos(localDist * 2.5 - t * 2.8);
    float manaShimmer = 0.5 + 0.5 * (pulse1 * 0.55 + pulse2 * 0.45);

    // Micro dielectric mana filaments dancing across barrier tiles
    vec2 arcCoord = worldPos.xy * 2.8 + vec2(t * 0.6, -t * 0.4);
    float arcNoise = fbm(arcCoord);
    float dielectricArcs = pow(max(0.0, arcNoise - 0.58) * 2.38, 3.0) * 1.8;

    // 4. Mathematical Hexagonal Lattice Resonance (คลื่นสั่นพ้องโครงข่ายหกเหลี่ยมเมื่อถูกโจมตี)
    // vertexColor.r encodes hit glow intensity from renderer (1.0 on hit, decaying smoothly)
    float hitFactor = smoothstep(0.12, 0.85, vertexColor.r);
    
    // Concentric resonant shockwave rings along the barrier surface
    float shockRadius = length(worldPos);
    float resonanceWave = sin(shockRadius * 8.0 - t * 14.0) * hitFactor;
    float resonanceFringe = cos(shockRadius * 15.0 - t * 22.0) * hitFactor;
    float impactShock = max(0.0, resonanceWave * 0.65 + resonanceFringe * 0.35);

    // High-frequency dielectric sparks at the impact zone
    float impactSparks = pow(noise(worldPos.xz * 5.0 + vec2(t * 4.0, -t * 3.0)), 3.5) * hitFactor * 3.5;

    // 5. Anime Crystalline Mana Color Palette (Frieren Defensive Magic)
    // Base mana: electric sapphire & translucent deep cyan
    vec3 deepCyan = vec3(0.04, 0.46, 0.95);
    vec3 baseColor = mix(deepCyan, vertexColor.rgb, 0.65);

    // Chromatic dispersion rim (deep cyan -> bright aqua turquoise -> incandescent pure white)
    vec3 rimColor = vec3(0.10 + 0.85 * fresnelR, 0.75 + 0.23 * fresnelG, 1.0);

    // Combine color layers
    vec3 color = baseColor * (0.85 + 0.25 * manaShimmer);
    color += rimColor * (fresnelB * 0.80 + rimGlow * 0.60);
    // Add procedural dielectric filaments
    color += vec3(0.60, 0.92, 1.0) * dielectricArcs;
    // Add mathematical impact lattice resonance & sparks
    color += vec3(0.85, 0.97, 1.0) * impactShock;
    color += vec3(1.0, 1.0, 1.0) * impactSparks;

    // 6. Dynamic Alpha calculation:
    // Core of barrier is semi-translucent crystal so player can see incoming threats clearly
    // Edges, bevel facets, and resonance lines are luminous and crisply defined
    float alpha = vertexColor.a * (0.28 + 0.72 * fresnelB);
    alpha = clamp(alpha + hitFactor * 0.35 + dielectricArcs * 0.25 + impactShock * 0.30, 0.0, 1.0);

    alpha *= ColorModulator.a;
    color *= ColorModulator.rgb;

    // GPU-safe single terminal discard at end of shader
    if (alpha <= 0.003) {
        discard;
    }

    fragColor = vec4(color, alpha);
}
