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

// Sample texture with demonic prismatic chromatic dispersion
vec4 samplePrismatic(sampler2D tex, vec2 p_rot, float r, vec2 dir, float dispersion) {
    vec2 uv_center = (p_rot + 1.0) * 0.5;
    vec2 uv_r = uv_center - dir * (dispersion * 1.2);
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

    // 1. Concentric Demonic Multi-Layer Counter-Rotation:
    // Inner Abyssal Core (r < 0.40): rapid clockwise rotation
    // Middle Dark Arcane Glyph Band (0.40 <= r < 0.76): sinister counter-clockwise rotation
    // Outer Demonic Celestial Ring (r >= 0.76): steady clockwise orbit
    vec2 p_inner = rotateVec(p,  t * 0.35);
    vec2 p_mid   = rotateVec(p, -t * 0.24);
    vec2 p_outer = rotateVec(p,  t * 0.12);

    vec4 tex_inner = samplePrismatic(Sampler0, p_inner, r, dir, 0.008);
    vec4 tex_mid   = samplePrismatic(Sampler0, p_mid,   r, dir, 0.006);
    vec4 tex_outer = samplePrismatic(Sampler0, p_outer, r, dir, 0.004);

    float w_mid   = smoothstep(0.36, 0.46, r);
    float w_outer = smoothstep(0.74, 0.82, r);

    vec4 blendedTex = mix(tex_inner, tex_mid, w_mid);
    blendedTex = mix(blendedTex, tex_outer, w_outer);

    float runeLuma = max(blendedTex.r, max(blendedTex.g, blendedTex.b)) * blendedTex.a;

    // 2. Traveling Abyssal Mana Shockwave Surges
    float wave1 = exp(-pow(fract(r * 2.4 - t * 0.70) * 4.5, 2.0)) * 1.5;
    float wave2 = exp(-pow(fract(r * 3.8 - t * 1.20 + 0.30) * 5.0, 2.0)) * 1.1;
    float manaSurge = wave1 + wave2;

    // 3. High-Frequency Demonic Mana Shimmer along Rune Contours
    float shimmer = 0.84 + 0.16 * sin(theta * 14.0 + r * 28.0 - t * 5.5);
    float runeIntensity = runeLuma * (1.15 + manaSurge * 1.15) * shimmer;

    // 4. Central Demonic Aperture Rim (Violent royal violet corona)
    float apertureRim = exp(-pow((r - 0.22) * 26.0, 2.0)) * (0.85 + 0.25 * sin(theta * 10.0 - t * 4.0));

    // 5. 4-Point Rotating Sinister Needle Star Glint
    vec2 st = rotateVec(p, -t * 0.25);
    float crossH = exp(-abs(st.y) * 45.0) * exp(-abs(st.x) * 4.0);
    float crossV = exp(-abs(st.x) * 45.0) * exp(-abs(st.y) * 4.0);
    float needleStar = (crossH + crossV) * (0.90 + 0.35 * sin(t * 4.2));

    // 6. Central Event-Horizon Dark Void Core (Negative black hole absorbing core surrounded by intense rim)
    float eventHorizon = exp(-r * r * 50.0);
    float coreFocus = exp(-pow((r - 0.08) * 32.0, 2.0)) * 1.6;

    // 7. Concentric Demonic Energy Arcs
    float innerRuneRing = exp(-pow((r - 0.44) * 36.0, 2.0)) * 0.70 * (0.85 + 0.15 * sin(theta * 8.0 - t * 2.5));
    float outerRuneRing = exp(-pow((r - 0.94) * 45.0, 2.0)) * 0.95;

    // 8. Demonic Zoltraak / Qual Color Palette:
    // Abyssal Obsidian Void -> Royal Demonic Violet -> Radiant White-Hot Lavender
    vec3 obsidianVoid   = vec3(0.06, 0.01, 0.12);
    vec3 demonicViolet  = vec3(0.72, 0.12, 1.00);
    vec3 royalPurple    = vec3(0.92, 0.36, 1.00);
    vec3 whiteLavender  = vec3(0.98, 0.90, 1.00);

    vec3 finalColor = obsidianVoid * (runeIntensity + 0.1);
    finalColor += demonicViolet * clamp(runeIntensity * 0.90, 0.0, 1.0);
    finalColor += royalPurple * clamp((runeIntensity - 0.55) * 1.2 + manaSurge * 0.50, 0.0, 1.0);
    finalColor += whiteLavender * clamp((runeIntensity - 0.80) * 1.5, 0.0, 1.0);

    // Add aperture rim, needle star glint, and concentric rings
    finalColor += demonicViolet * apertureRim * 1.55;
    finalColor += whiteLavender * (needleStar + coreFocus) * 1.40;
    finalColor += royalPurple * (innerRuneRing + outerRuneRing);

    // Suppress very center with dark event horizon void
    finalColor = mix(finalColor, obsidianVoid * 0.3, clamp(eventHorizon * 0.90, 0.0, 1.0));

    // 9. Sub-Pixel Anti-Aliased Circular Boundary
    float circleCut = 1.0 - smoothstep(0.96, 1.0, r);

    // Alpha channel computation
    float totalAlpha = (runeLuma * 0.96 + apertureRim * 0.90 + (needleStar + coreFocus) * 0.85) * circleCut * vColor.a * ColorModulator.a;
    if (totalAlpha < 0.008) discard;

    fragColor = vec4(finalColor * vColor.rgb * ColorModulator.rgb, clamp(totalAlpha, 0.0, 1.0));
}
