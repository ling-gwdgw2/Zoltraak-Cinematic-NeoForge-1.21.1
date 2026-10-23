#version 150

in vec2 uv;
in float age;
in float energy;
in float dark;
in float luminous;

out vec4 fragColor;

void main() {
    float x = clamp(uv.x, 0.0, 1.0);
    float y = abs(uv.y * 2.0 - 1.0);

    // =========================================================================
    // 1. ANIME-ACCURATE NEEDLE LANCE PROFILE (1:1 Sousou no Frieren)
    // =========================================================================
    // Tail (0.0 -> 0.22): Tapers smoothly and dissolves into space
    // Body (0.22 -> 0.82): Slender supersonic energy needle
    // Tip  (0.82 -> 1.00): Diamond Arrowhead converging to a razor-sharp apex
    float tailFade = smoothstep(0.0, 0.08, x);

    float lanceWidth;
    if (x < 0.08) {
        lanceWidth = mix(0.60, 1.0, smoothstep(0.0, 0.08, x));
    } else if (x < 0.90) {
        lanceWidth = 1.0;
    } else {
        // Diamond Arrowhead: slight expansion at diamond shoulders, then sharp linear taper
        float t = (x - 0.90) / 0.10;
        lanceWidth = (1.0 + 0.12 * sin(t * 3.14159)) * (1.0 - t * 0.95);
    }

    // High-frequency supersonic ionization ripple
    float ripple = 1.0 + 0.04 * sin(x * 65.0 - age * 8.5);
    float effectiveWidth = max(lanceWidth * ripple, 0.005);
    float dist = y / effectiveWidth;

    // =========================================================================
    // 2. RADIAL ENERGY GRADIENT (Razor-Thin Core + Vibrant Ionized Sheath)
    // =========================================================================
    // Searing pure white plasma core: wide incandescent core
    float core = exp(-pow(dist * 2.2, 2.5));

    // Vibrant electric ionized sheath: wraps the core in glowing cyan
    float sheath = exp(-pow(dist * 1.25, 2.0));

    // Outer atmospheric displacement halo: signature royal violet mana rim
    float halo = exp(-pow(dist * 0.70, 1.8)) * 0.45;

    // =========================================================================
    // 3. DIAMOND ARROWHEAD GLINT & ANAMORPHIC TIP SPARKLE
    // =========================================================================
    // 4-pointed diamond star sparkle at the arrowhead (x = 0.96)
    float glintX = abs(x - 0.96) * 22.0;
    float glintY = y * 7.5;
    float diamondGlint = exp(-glintX - glintY) * 1.5;

    // Needle tip flash at the very apex
    float tipApex = smoothstep(0.88, 1.0, x) * exp(-y * 6.0) * 1.3;

    // =========================================================================
    // 4. PALETTE COMPUTATION (Demon-Slaying vs Human-Killing)
    // =========================================================================
    if (dark > 0.5) {
        // =====================================================================
        // ANIME 1:1 CORRUPTED / HUMAN-KILLING ZOLTRAAK (Elder Sage Qual)
        // Solid Pitch-Black Obsidian Void Lance with Charcoal Soot & Ash Dust
        // =====================================================================
        vec3 voidBlack   = vec3(0.003, 0.004, 0.008); // 100% Light-devouring obsidian void
        vec3 sootBlack   = vec3(0.045, 0.038, 0.055); // Dark charcoal soot mantle
        vec3 smokeViolet = vec3(0.20, 0.11, 0.32);   // Muted dark violet-ash rim
        vec3 ashDust     = vec3(0.48, 0.35, 0.65);   // Micro-particle disintegration dust

        // Core void zone: Solid impenetrable black
        float coreMask = 1.0 - smoothstep(0.48, 0.88, dist);

        // Edge smoke & mineral dust boundary
        float rimMask = smoothstep(0.40, 0.82, dist) * (1.0 - smoothstep(0.95, 1.25, dist));
        float dustNoise = sin(x * 95.0 - age * 11.0) * sin(dist * 25.0);
        float dustSparkle = rimMask * smoothstep(0.2, 0.8, dustNoise);

        vec3 lanceColor = mix(voidBlack, sootBlack, clamp(dist * 0.75, 0.0, 1.0));
        lanceColor = mix(lanceColor, smokeViolet, rimMask * 0.65);
        lanceColor = mix(lanceColor, ashDust, dustSparkle * 0.45);

        // Alpha: The core MUST be solid 1.0 so translucent blending blocks the background
        float lanceAlpha = clamp((coreMask * 1.0 + rimMask * 0.55) * tailFade * energy, 0.0, 1.0);

        if (lanceAlpha <= 0.003) {
            discard;
        }

        fragColor = vec4(lanceColor, lanceAlpha);
        return;
    }

    // Theme 0: Demon-Slaying / Fern (1:1 Anime Tri-Tone: White Core -> Cyan Body -> Royal Violet Rim)
    vec3 violetMana   = vec3(0.48, 0.18, 0.92); // Signature Frieren Royal Violet Mana Rim
    vec3 cyanElectric = vec3(0.20, 0.88, 1.00); // Luminous Electric Cyan Sheath
    vec3 whiteCore    = vec3(1.00, 1.00, 1.00); // Incandescent Searing White Core

    // Outer rim starts as vibrant royal violet, inner body shifts to electric cyan
    vec3 holyTheme = mix(violetMana, cyanElectric, clamp(sheath * 1.45 - halo * 0.4, 0.0, 1.0));
    // Core and arrowhead flash searing white
    holyTheme      = mix(holyTheme, whiteCore, clamp(core * 1.35 + diamondGlint * 0.75 + tipApex * 0.65, 0.0, 1.0));
    // Outer atmospheric displacement carries the purple mana haze
    holyTheme     += violetMana * halo * 1.35;

    // =========================================================================
    // 5. ALPHA CALCULATION & DISSOLUTION (Fern Holy Theme)
    // =========================================================================
    float density = (core * 0.98 + sheath * 0.85 + halo * 0.80 + diamondGlint * 0.55 + tipApex * 0.45) * tailFade;
    float alpha = clamp(density * energy, 0.0, 0.95);

    if (alpha <= 0.003) {
        discard;
    }

    fragColor = vec4(holyTheme, alpha);
}