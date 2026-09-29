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
vec4 samplePrismatic(sampler2D tex, vec2 p_rot, float r, vec2 dir, float dispersion) {
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
    if (r > 1.02) discard;

    vec2 dir = (r > 0.001) ? (p / r) : vec2(0.0);
    float theta = atan(p.y, p.x);
    float t = (GameTime > 0.001 ? GameTime : 1.0) * 2.5;

    // 1. Concentric Multi-Layer Counter-Rotation:
    // Inner Elven Core (r < 0.40): clockwise rotation
    // Middle Arcane Glyph Band (0.40 <= r < 0.76): counter-clockwise rotation
    // Outer Celestial Ring (r >= 0.76): steady clockwise orbit
    vec2 p_inner = rotateVec(p,  t * 0.32);
    vec2 p_mid   = rotateVec(p, -t * 0.22);
    vec2 p_outer = rotateVec(p,  t * 0.10);

    vec4 tex_inner = samplePrismatic(Sampler0, p_inner, r, dir, 0.007);
    vec4 tex_mid   = samplePrismatic(Sampler0, p_mid,   r, dir, 0.005);
    vec4 tex_outer = samplePrismatic(Sampler0, p_outer, r, dir, 0.003);

    float w_mid   = smoothstep(0.36, 0.46, r);
    float w_outer = smoothstep(0.74, 0.82, r);

    vec4 blendedTex = mix(tex_inner, tex_mid, w_mid);
    blendedTex = mix(blendedTex, tex_outer, w_outer);

    float runeLuma = max(blendedTex.r, max(blendedTex.g, blendedTex.b)) * blendedTex.a;

    // 2. Traveling Mana Shockwave Surges (Outward radial energy wavefronts)
    float wave1 = exp(-pow(fract(r * 2.2 - t * 0.65) * 4.5, 2.0)) * 1.5;
    float wave2 = exp(-pow(fract(r * 3.6 - t * 1.15 + 0.35) * 5.0, 2.0)) * 1.0;
    float manaSurge = wave1 + wave2;

    // 3. High-Frequency Mana Shimmer along Rune Contours
    float shimmer = 0.86 + 0.14 * sin(theta * 14.0 + r * 26.0 - t * 5.2);
    float runeIntensity = runeLuma * (1.15 + manaSurge * 1.10) * shimmer;

    // 4. Central Blazing Optical Focus Aperture Rim
    float apertureRim = exp(-pow((r - 0.22) * 26.0, 2.0)) * (0.85 + 0.25 * sin(theta * 10.0 - t * 3.8));

    // 5. 4-Point Rotating Holy Needle Star Glint (Diffraction Spikes at focal center)
    vec2 st = rotateVec(p, t * 0.25);
    float crossH = exp(-abs(st.y) * 45.0) * exp(-abs(st.x) * 4.0);
    float crossV = exp(-abs(st.x) * 45.0) * exp(-abs(st.y) * 4.0);
    float needleStar = (crossH + crossV) * (0.85 + 0.35 * sin(t * 4.0));

    // 6. Central Searing White-Hot Mana Core
    float coreFocus = exp(-r * r * 42.0) * 1.6;

    // 7. Concentric Holy Energy Arcs (Procedural boundary rings)
    float innerRuneRing = exp(-pow((r - 0.44) * 36.0, 2.0)) * 0.65 * (0.85 + 0.15 * sin(theta * 8.0 + t * 2.0));
    float outerRuneRing = exp(-pow((r - 0.94) * 45.0, 2.0)) * 0.90;

    // 8. Holy Frieren / Fern Color Palette:
    // Deep Azure Sheath -> Electric Cyan Radiance -> Searing White-Hot Solar Core
    vec3 deepAzure     = vec3(0.02, 0.42, 0.98);
    vec3 electricCyan  = vec3(0.12, 0.90, 1.00);
    vec3 whiteHotCore  = vec3(0.98, 1.00, 1.00);

    vec3 finalColor = deepAzure * runeIntensity;
    finalColor += (electricCyan - deepAzure) * clamp(runeIntensity * 0.85, 0.0, 1.0);
    finalColor += whiteHotCore * clamp((runeIntensity - 0.65) * 1.3 + manaSurge * 0.45, 0.0, 1.0);

    // Add aperture rim, needle star glint, and concentric rings
    finalColor += electricCyan * apertureRim * 1.45;
    finalColor += whiteHotCore * (needleStar + coreFocus) * 1.50;
    finalColor += electricCyan * (innerRuneRing + outerRuneRing);

    // 9. Sub-Pixel Anti-Aliased Circular Boundary
    float circleCut = 1.0 - smoothstep(0.96, 1.0, r);

    // Alpha channel computation (Preserves crisp visibility without squaring dimness)
    float totalAlpha = (runeLuma * 0.96 + apertureRim * 0.85 + (needleStar + coreFocus) * 0.80) * circleCut * vColor.a * ColorModulator.a;
    if (totalAlpha < 0.008) discard;

    fragColor = vec4(finalColor * vColor.rgb * ColorModulator.rgb, clamp(totalAlpha, 0.0, 1.0));
}
