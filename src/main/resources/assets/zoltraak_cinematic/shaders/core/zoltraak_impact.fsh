#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 uv;
in vec4 vertexColor;
in vec3 viewPosition;

out vec4 fragColor;

// Procedural hash and noise
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
    vec2 p = uv * 2.0 - 1.0;
    float r = length(p);
    float theta = atan(p.y, p.x);

    // Sub-pixel circular boundary fade preventing quad polygon edge cutoff
    float boundaryFade = 1.0 - smoothstep(0.88, 1.0, r);
    if (boundaryFade <= 0.001) discard;

    // Attributes decoded from vertexColor:
    float progress  = clamp(vertexColor.r, 0.0, 1.0);  // 0.0 (moment of impact) -> 1.0 (end of life)
    bool  isBlack   = vertexColor.g > 0.5;             // Standard Holy Cyan vs Corrupted Black Zoltraak
    bool  isSurface = vertexColor.b > 0.5;             // Surface planar shockwave vs Camera-facing radial burst
    float fade      = vertexColor.a;

    float t = GameTime * 2.8;

    float density = 0.0;
    vec3  color = vec3(0.0);

    if (isSurface) {
        // =====================================================================
        // MODE 1: SURFACE PUNCTURE CRATER & FRACTURE SCORCH (รอยเจาะทะลุและรอยแตกร้าว)
        // =====================================================================
        // Searing puncture crater rim where the laser drilled through the surface
        float holeRim = exp(-pow((r - 0.20) * 14.0, 2.0)) * (1.0 - progress * 0.60) * 2.2;

        // Ground fracture lines & mana scorch cracks radiating from center
        float cracks = pow(max(0.0, sin(theta * 10.0 + fbm(p * 6.0) * 3.5)), 8.0);
        float crackDecay = exp(-r * 2.8) * (1.0 - progress * 0.50) * cracks * 2.0;

        // Central scorch ground glow
        float groundCore = exp(-pow(r * 4.0, 2.0)) * exp(-progress * 4.0) * 2.6;

        density = (holeRim + crackDecay + groundCore) * boundaryFade;

        if (!isBlack) {
            vec3 cCyan = vec3(0.20, 0.90, 1.00);
            vec3 cViolet = vec3(0.50, 0.20, 0.95);
            vec3 cWhite = vec3(1.00, 1.00, 1.00);
            color = mix(cViolet, cCyan, smoothstep(0.15, 0.65, density));
            color = mix(color, cWhite, smoothstep(0.65, 1.30, density));
        } else {
            vec3 cVoid = vec3(0.003, 0.004, 0.008);
            vec3 cSoot = vec3(0.045, 0.035, 0.055);
            vec3 cAsh = vec3(0.35, 0.22, 0.48);
            color = mix(cVoid, cSoot, smoothstep(0.12, 0.60, density));
            color = mix(color, cAsh, smoothstep(0.60, 1.25, density));
        }
    } else {
        // =====================================================================
        // MODE 2: VOLUMETRIC BILLOWING ANIME EXPLOSION (กลุ่มควันและแสงระเบิดอนิเมะ)
        // =====================================================================
        // 1. Initial High-Key Optical Detonation Flash & Diffraction Cross Spikes
        float flashDecay = exp(-progress * 9.0);
        float coreFlash = exp(-pow(r * 5.2, 2.0)) * flashDecay * 3.4;
        float flareH = exp(-abs(p.y) * 26.0) * exp(-abs(p.x) * 1.5) * flashDecay * 1.8;
        float flareV = exp(-abs(p.x) * 26.0) * exp(-abs(p.y) * 1.5) * flashDecay * 1.4;

        // Diagonal 4-point diffraction glint
        vec2 diag = vec2(p.x + p.y, p.x - p.y) * 0.7071;
        float diagGlint = (exp(-abs(diag.x) * 32.0) * exp(-abs(diag.y) * 2.2) +
                           exp(-abs(diag.y) * 32.0) * exp(-abs(diag.x) * 2.2)) * flashDecay * 0.95;

        // 2. Volumetric Billowing Anime Dust / Smoke Cloud (Puffy cel-shaded lobes matching Madhouse animation)
        vec2 smokeUV = p * (2.8 - progress * 0.6) - vec2(0.0, progress * 0.6);
        float smokeNoise = fbm(smokeUV + vec2(t * 0.6, -t * 0.3));
        float smokeRadius = 0.35 + 0.62 * pow(progress, 0.45);
        float smokeShape = (r / max(smokeRadius, 0.01)) + (smokeNoise - 0.5) * 0.45;
        float animeSmoke = (1.0 - smoothstep(0.60, 0.95, smokeShape)) * (1.10 - progress * 0.65);

        // Cel-shaded 2-tone volumetric lighting on the billowing smoke
        float smokeLight = smoothstep(0.38, 0.72, smokeNoise + (0.4 - p.y) * 0.45);

        // 3. Dielectric Plasma Sparks & Welding Eruption Spikes
        float sparkRays = pow(max(0.0, sin(theta * 14.0 + fbm(p * 7.0 + t * 1.5) * 3.5)), 9.0);
        float sparks = sparkRays * exp(-r * 2.5) * (1.0 - progress * 0.7) * 2.2;

        // 4. Floating Flying Mana Embers & Rock Particles
        vec2 emberGrid = (p + vec2(0.0, -progress * 0.3)) * 12.0;
        vec2 cell = floor(emberGrid);
        vec2 cellFract = fract(emberGrid) - 0.5;
        float cellSeed = hash(cell);
        float ember = (1.0 - smoothstep(0.05, 0.30, length(cellFract))) * step(0.60, cellSeed);
        ember *= exp(-r * 1.4) * (1.0 - progress * 0.55) * (0.7 + 0.3 * sin(cellSeed * 50.0 + t * 8.0));

        density = (coreFlash + flareH + flareV + diagGlint + animeSmoke * 1.25 + sparks + ember * 0.8) * boundaryFade;

        if (!isBlack) {
            // Anime Fern Holy Zoltraak:
            // Searing White Core -> Electric Cyan Plasma -> Royal Violet Rim -> Pale Mint-Cyan Smoke
            vec3 cWhite   = vec3(1.00, 1.00, 1.00);
            vec3 cCyan    = vec3(0.20, 0.92, 1.00);
            vec3 cViolet  = vec3(0.48, 0.18, 0.92);
            vec3 cSmokeHi = vec3(0.85, 0.98, 0.95); // Pale anime dust/smoke highlight
            vec3 cSmokeLo = vec3(0.42, 0.65, 0.75); // Shaded dust/smoke contour

            vec3 smokeColor = mix(cSmokeLo, cSmokeHi, smokeLight);
            vec3 plasmaColor = mix(cViolet, cCyan, smoothstep(0.20, 0.70, density));
            plasmaColor = mix(plasmaColor, cWhite, smoothstep(0.70, 1.35, density));

            // Blend plasma energy with billowing dust smoke
            color = mix(smokeColor, plasmaColor, clamp((coreFlash + flareH + flareV + diagGlint + sparks) * 1.2, 0.0, 1.0));
        } else {
            // Anime Qual Black Zoltraak:
            // Pure Obsidian Void -> Charcoal Soot Smoke -> Dark Violet Ash
            vec3 cVoid    = vec3(0.003, 0.004, 0.008);
            vec3 cSootLo  = vec3(0.025, 0.020, 0.035);
            vec3 cSootHi  = vec3(0.075, 0.060, 0.095);
            vec3 cAsh     = vec3(0.38, 0.24, 0.52);

            vec3 sootSmoke = mix(cSootLo, cSootHi, smokeLight);
            color = mix(cVoid, sootSmoke, smoothstep(0.10, 0.55, density));
            color = mix(color, cAsh, smoothstep(0.55, 1.15, density));
        }
    }

    float alpha = clamp(density * fade * ColorModulator.a, 0.0, 1.0);
    color *= ColorModulator.rgb;

    if (alpha <= 0.002) {
        discard;
    }

    fragColor = vec4(color, alpha);
}
