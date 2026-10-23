#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 uv;
in vec4 vColor;

out vec4 fragColor;

// Helper: rotate 2D vector by angle
vec2 rotateVec(vec2 v, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec2(v.x * c - v.y * s, v.x * s + v.y * c);
}

// Sample texture with prismatic chromatic dispersion
vec4 samplePrismatic(sampler2D tex, vec2 p_rot, vec2 dir, float dispersion) {
    vec2 uv_center = (p_rot + 1.0) * 0.5;
    vec2 uv_r = uv_center - dir * dispersion;
    vec2 uv_g = uv_center;
    vec2 uv_b = uv_center + dir * dispersion;

    float r_ch = texture(tex, clamp(uv_r, 0.0, 1.0)).r;
    float g_ch = texture(tex, clamp(uv_g, 0.0, 1.0)).g;
    float b_ch = texture(tex, clamp(uv_b, 0.0, 1.0)).b;
    float a_ch = texture(tex, clamp(uv_g, 0.0, 1.0)).a;

    return vec4(r_ch, g_ch, b_ch, a_ch);
}

void main() {
    vec2 p = uv * 2.0 - 1.0;
    float r = length(p);
    if (r > 1.015) discard;

    vec2 dir = (r > 0.001) ? (p / r) : vec2(0.0);
    float t = (GameTime > 0.001 ? GameTime : 1.0) * 2.5;

    // 1. Unified Rigid Arcane Disk Rotation
    float rotSpeed = t * 0.045;
    vec2 p_rot = rotateVec(p, rotSpeed);

    // 2. Clockwise Circular Compass Sweep (1:1 Anime Manifestation)
    // vColor.r carries formation progress [0.0 -> 1.0]
    float formation = clamp(vColor.r, 0.0, 1.0);
    float drawMask = 1.0;
    float leadSpark = 0.0;

    if (formation < 0.999) {
        // Clockwise sweep starting from top (0.0 to 1.0)
        float sweepAngle = fract((atan(-p_rot.x, p_rot.y) + 3.14159265) / 6.2831853);
        
        // Outer rune ring (r > 0.60) sweeps clockwise
        float ringMask = smoothstep(sweepAngle - 0.02, sweepAngle, formation);
        
        // Inner petals bloom outward as the outer sweep progresses
        float petalBloom = smoothstep(0.15, 0.90, formation);
        float petalRadiusMask = smoothstep(r / 0.62, (r / 0.62) - 0.08, petalBloom);
        
        // Combine outer sweep with inner bloom
        drawMask = mix(petalRadiusMask, ringMask, smoothstep(0.55, 0.65, r));

        // Blinding leading spark at the tip of the sweep
        float headDiff = abs(sweepAngle - formation);
        leadSpark = exp(-headDiff * headDiff * 1600.0) * smoothstep(0.60, 0.98, r) * 3.5;
    }

    if (drawMask < 0.005 && leadSpark < 0.01) discard;

    // 3. Sample High-Definition Magic Circle Texture
    vec4 tex = samplePrismatic(Sampler0, p_rot, dir, 0.0025);
    float runeLuma = max(tex.r, max(tex.g, tex.b)) * tex.a;

    // 4. Smooth Outward Traveling Mana Wave
    float wave = exp(-pow(fract(r * 2.2 - t * 0.70) * 4.0, 2.0)) * 0.28;

    // 5. Pure Temporal Shimmer (no spatial moire grating)
    float shimmer = 0.94 + 0.06 * sin(t * 3.0);

    // 6. Central Eye Core Glow (subtle, follows texture without washing out center art)
    float coreFocus = exp(-r * r * 42.0) * 0.35 * runeLuma;

    // 7. Render texture artwork directly with dynamic mana energy
    vec3 baseArt = tex.rgb;
    vec3 whiteHotCore = vec3(0.96, 1.00, 1.00);

    vec3 finalColor = baseArt * (1.05 + wave * 0.35) * shimmer;
    finalColor += whiteHotCore * smoothstep(0.65, 0.98, runeLuma) * (0.6 + wave * 0.4);
    finalColor += whiteHotCore * (coreFocus * 0.5 + leadSpark);

    // 8. Anti-Aliased Outer Circular Edge
    float circleCut = 1.0 - smoothstep(0.985, 1.008, r);

    // Alpha calculation
    float totalAlpha = ((runeLuma * drawMask) * 0.98 + coreFocus * 0.5 + leadSpark * 0.9)
                       * circleCut * vColor.a * ColorModulator.a;
    if (totalAlpha < 0.008) discard;

    fragColor = vec4(finalColor * ColorModulator.rgb, clamp(totalAlpha, 0.0, 1.0));
}
