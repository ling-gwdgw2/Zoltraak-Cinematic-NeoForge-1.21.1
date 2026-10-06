#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 uv;
in vec4 vertexColor;
in vec3 viewPosition;

out vec4 fragColor;

// Procedural hash and noise for lightning arcs and mana sparks
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

    // Quad boundary circular fade preventing harsh polygon edges
    float boundaryFade = 1.0 - smoothstep(0.85, 1.0, r);
    if (boundaryFade <= 0.001) discard;

    // Attributes decoded from vertexColor:
    float progress  = clamp(vertexColor.r, 0.0, 1.0);  // 0.0 (moment of impact) -> 1.0 (end of life)
    bool  isBlack   = vertexColor.g > 0.5;             // Standard Holy Cyan vs Corrupted Black Zoltraak
    bool  isSurface = vertexColor.b > 0.5;             // Surface planar shockwave vs Camera-facing radial burst
    float fade      = vertexColor.a;

    float t = GameTime * 3.0;

    float density = 0.0;
    vec3  color = vec3(0.0);

    // =========================================================================
    // 3D SPHERICAL RELATIVISTIC MANA EXPLOSION (ลูกบอลระเบิดมานาทรงกลม 3D)
    // =========================================================================
    // Relativistic expansion: rapid initial inflation (progress^0.38)
    float sphereR = pow(progress, 0.38) * 0.85;
    float shellThick = 0.080 + 0.035 * progress;
    float innerR = max(0.0, sphereR - shellThick);

    float sphereShell = 0.0;
    float sphereCore = 0.0;
    float surfaceArcs = 0.0;

    if (r < sphereR) {
        // 3D Ray-Sphere intersection depth
        float zOuter = sqrt(max(0.0, sphereR * sphereR - r * r));
        float zInner = (r < innerR) ? sqrt(max(0.0, innerR * innerR - r * r)) : 0.0;

        // Optical Path Length through hollow spherical plasma shell (Limb Brightening / Fresnel Volume)
        float pathLength = (zOuter - zInner) / max(sphereR, 0.01);
        sphereShell = pathLength * 2.3;

        // Normalized 3D normal vector on the front hemisphere surface
        vec3 N = vec3(p.x / max(sphereR, 0.001), p.y / max(sphereR, 0.001), zOuter / max(sphereR, 0.001));

        // 3D Spherical Surface Electrical Filaments crackling over the 3D sphere
        vec2 sphereUV = vec2(atan(N.z, N.x) * 1.5, asin(clamp(N.y, -1.0, 1.0)) * 2.0);
        float surfaceNoise = fbm(sphereUV * 4.0 + vec2(t * 1.2, -t * 0.6));
        surfaceArcs = exp(-pow(abs(surfaceNoise - 0.5) * 14.0, 2.0)) * (1.0 - progress * 0.75) * 1.35;

        // Internal Boiling Plasma Core Radiance
        float interiorGlow = (zOuter / sphereR) * (1.0 - progress) * 0.65;
        sphereCore = interiorGlow + exp(-pow(r / max(0.28 * sphereR, 0.02), 2.0)) * exp(-progress * 4.2) * 2.0;
    }

    // Relativistic Shockwave Leading Compression Front (Sharp edge at outer sphere boundary)
    float shockFront = exp(-pow(abs(r - sphereR) / (0.032 + 0.018 * progress), 2.0)) * (1.0 - progress * 0.55) * 1.6;

    // Optical Core Flash & Anamorphic Glint
    float flashDecay = exp(-progress * 8.5);
    float coreFlash = exp(-pow(r * 5.5, 2.0)) * flashDecay * 2.4;
    float flareH = exp(-abs(p.y) * 24.0) * exp(-abs(p.x) * 1.6) * flashDecay * 1.2;
    float flareV = exp(-abs(p.x) * 24.0) * exp(-abs(p.y) * 1.6) * flashDecay * 0.5;

    // Dielectric High-Voltage Sparks Erupting from the Sphere into surrounding air
    float raySpikes = pow(max(0.0, sin(theta * 14.0 + sin(theta * 5.0) * 2.2 + fbm(p * 8.0) * 2.5)), 12.0);
    float sparkFront = sphereR + 0.12 * progress;
    float sparks = raySpikes * exp(-pow(abs(r - sparkFront) / 0.07, 2.0)) * (1.0 - progress * 0.7) * 1.35;

    // Floating Mana Embers around the spherical blast
    vec2 emberGrid = (p + vec2(0.0, -progress * 0.2)) * 14.0;
    vec2 cell = floor(emberGrid);
    vec2 cellFract = fract(emberGrid) - 0.5;
    float cellSeed = hash(cell);
    float ember = (1.0 - smoothstep(0.05, 0.28, length(cellFract))) * step(0.68, cellSeed);
    ember *= exp(-r * 1.8) * (1.0 - progress * 0.5) * (0.6 + 0.4 * sin(cellSeed * 50.0 + t * 10.0));

    density = (sphereShell + sphereCore + surfaceArcs + shockFront + coreFlash + flareH + flareV + sparks + ember * 0.85) * boundaryFade;

    // Relativistic Chromatic Dispersion Palette
    float rimShift = exp(-pow(abs(r - sphereR) / 0.045, 2.0));

    if (!isBlack) {
        // White Zoltraak: White-hot Core -> Electric Cyan Shell -> Royal Azure Rim
        vec3 cCyan = vec3(0.18, 0.94, 1.00);
        vec3 cAzure = vec3(0.04, 0.42, 0.98);
        vec3 cWhite = vec3(1.00, 1.00, 1.00);
        color = mix(cAzure, cCyan, smoothstep(0.16, 0.65, density));
        color = mix(color, cWhite, smoothstep(0.65, 1.30, density));
        color += vec3(0.06, 0.18, 0.38) * rimShift * 0.45;
    } else {
        // Black Zoltraak: Obsidian Soot -> Vivid Magenta-Violet Shell -> Searing White-Lilac
        vec3 cSoot = vec3(0.04, 0.02, 0.08);
        vec3 cViolet = vec3(0.85, 0.18, 0.98);
        vec3 cWhite = vec3(1.00, 0.92, 1.00);
        color = mix(cSoot, cViolet, smoothstep(0.14, 0.62, density));
        color = mix(color, cWhite, smoothstep(0.62, 1.25, density));
        color += vec3(0.28, 0.03, 0.38) * rimShift * 0.45;
    }

    float alpha = clamp(density * fade * ColorModulator.a, 0.0, 1.0);
    color *= ColorModulator.rgb;

    if (alpha <= 0.002) {
        discard;
    }

    fragColor = vec4(color, alpha);
}
