#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec3 worldPos;
in vec3 viewPosition;

out vec4 fragColor;

void main() {
    // 1. Surface normal in view space & camera direction (NaN-safe guarded normalization)
    vec3 vCross = cross(dFdx(viewPosition), dFdy(viewPosition));
    float vLen = length(vCross);
    vec3 viewNormal = vLen > 0.0001 ? vCross / vLen : vec3(0.0, 0.0, 1.0);

    float viewLen = length(viewPosition);
    vec3 viewDir = viewLen > 0.0001 ? -viewPosition / viewLen : vec3(0.0, 0.0, 1.0);

    float facing = clamp(abs(dot(viewNormal, viewDir)), 0.0, 1.0);

    // 2. High-clarity Fresnel rim (crystal clear facing, radiant at grazing edges)
    float fresnel = pow(1.0 - facing, 2.2);
    float rimGlow = pow(1.0 - facing, 4.5);

    // 3. Smooth Harmonic Mana Shimmer (isotropic, no vertical barcode lines)
    float t = GameTime * 2.0;
    float localDist = length(worldPos.xz);
    float pulse1 = sin(worldPos.y * 1.5 - t * 2.0 + localDist * 1.2);
    float pulse2 = cos(localDist * 2.5 - t * 2.8);
    float manaShimmer = 0.5 + 0.5 * (pulse1 * 0.55 + pulse2 * 0.45);

    // Impact overdrive detection: vertexColor.r increases when barrier is struck
    float hitFactor = smoothstep(0.15, 0.85, vertexColor.r);
    float shockwave = sin(length(worldPos) * 6.0 - t * 10.0) * hitFactor;

    // 4. Anime Crystalline Mana Color Palette (Frieren Anime Style)
    // Deep mana base: electric sapphire & translucent cyan
    vec3 deepCyan = vec3(0.05, 0.48, 0.95);
    vec3 baseColor = mix(deepCyan, vertexColor.rgb, 0.70);

    // Fresnel rim: iridescent chromatic dispersion (deep cyan -> bright aqua -> incandescent white-blue)
    vec3 rimColor = mix(vec3(0.15, 0.82, 1.0), vec3(0.92, 0.98, 1.0), fresnel);

    // Combine color layers
    vec3 color = baseColor * (0.90 + 0.20 * manaShimmer);
    // Add brilliant Fresnel rim luminescence
    color += rimColor * (fresnel * 0.75 + rimGlow * 0.55);
    // Add impact shockwave surge
    color += vec3(0.85, 0.96, 1.0) * max(0.0, shockwave * 0.5);

    // 5. Dynamic Alpha calculation:
    // Core of barrier is semi-translucent crystal so player can see incoming spells/threats
    // Edges and bevel facets are luminous and clearly defined
    float alpha = vertexColor.a * (0.30 + 0.70 * fresnel);
    // Add extra opacity on impact hit
    alpha = clamp(alpha + hitFactor * 0.30, 0.0, 1.0);

    alpha *= ColorModulator.a;
    color *= ColorModulator.rgb;

    // GPU-safe single terminal discard at end of shader (derivatives executed uniformly)
    if (alpha <= 0.003) {
        discard;
    }

    fragColor = vec4(color, alpha);
}
