#version 150

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D NoiseSampler;

in vec2 vUv;
out vec4 fragColor;

#define SL_SKY_DEPTH 0.99999

/**
 * Luminous Interstellar Deep Space: Multi-layered Diamond Starfield & Glowing Cosmic Nebula
 */
vec3 gargCosmicSky(vec3 dir, float time, float sceneLum) {
    // Spherical celestial coordinates
    vec2 skyUv = vec2(atan(dir.z, dir.x) / 6.2831853 + 0.5, asin(clamp(dir.y, -1.0, 1.0)) / 3.14159265 + 0.5);

    // 1. Organic Interstellar Nebula Dust Lanes (using NoiseSampler hardware texture)
    vec2 nebUv = skyUv * vec2(2.4, 1.2) + vec2(time * 0.003, time * 0.001);
    float n1 = dot(textureLod(NoiseSampler, fract(nebUv), 0.0).rgb, vec3(0.333));
    float n2 = dot(textureLod(NoiseSampler, fract(nebUv * 2.3 + vec2(0.35, 0.72)), 0.0).rgb, vec3(0.333));
    float n3 = dot(textureLod(NoiseSampler, fract(nebUv * 5.2 - vec2(0.48, 0.22)), 0.0).rgb, vec3(0.333));
    float nebDensity = pow(clamp(n1 * 0.55 + n2 * 0.32 + n3 * 0.18 - 0.20, 0.0, 1.0), 1.5);

    // Cosmic color palette: deep indigo, celestial violet, radiant stardust gold
    vec3 nebBase = vec3(0.06, 0.12, 0.36);
    vec3 nebMid = vec3(0.46, 0.18, 0.62);
    vec3 nebHighlight = vec3(1.0, 0.82, 0.45);
    vec3 nebCol = mix(nebBase, nebMid, clamp(n2 * 1.6, 0.0, 1.0));
    nebCol = mix(nebCol, nebHighlight, pow(n3, 2.2));
    vec3 nebulaLight = nebCol * (nebDensity * 2.6);

    // 2. Multi-scale Procedural Diamond Starfield
    // A. Dense distant stellar field
    vec3 pStar1 = dir * 140.0;
    vec3 id1 = floor(pStar1);
    vec3 f1 = fract(pStar1) - 0.5;
    float hash1 = fract(sin(dot(id1, vec3(127.1, 311.7, 74.7))) * 43758.5453);
    float star1 = smoothstep(0.965, 1.0, hash1) * exp(-dot(f1, f1) * 75.0);

    // B. Bright sparkling stellar giants with twinkling & spectral colors
    vec3 pStar2 = dir * 42.0;
    vec3 id2 = floor(pStar2);
    vec3 f2 = fract(pStar2) - 0.5;
    float hash2 = fract(sin(dot(id2, vec3(269.5, 183.3, 419.2))) * 37584.2341);
    float star2 = smoothstep(0.975, 1.0, hash2) * exp(-dot(f2, f2) * 55.0);
    float twinkle = 0.72 + 0.28 * sin(time * 2.8 + hash2 * 18.0);
    vec3 starCol2 = (hash2 > 0.99) ? vec3(0.65, 0.88, 1.0) : ((hash2 > 0.982) ? vec3(1.0, 0.88, 0.6) : vec3(1.0));
    vec3 starsLight = vec3(star1 * 1.8) + (starCol2 * star2 * twinkle * 4.2);

    vec3 cosmicVoid = nebulaLight + starsLight;

    // 3. Day / Night Smart Harmonization (No dirty black bruises in daylight!)
    vec3 daySky = nebulaLight * 0.45 + starsLight * 1.5;
    vec3 nightSky = cosmicVoid;
    return mix(nightSky, daySky, clamp(sceneLum * 1.8, 0.0, 1.0));
}

// Gargantua: a black hole whose accretion disk is lensed by its own gravity.
//
// Included by core/gemini_kill_post_gargantua_lens.fsh after
// kill_effect_post_common.glsl, which supplies vUv, fragColor, SceneSampler and
// SL_SKY_DEPTH.
//
// Two things are going on here and they come from different places.
//
// The light bending is integrated as a real geodesic, because the signature image is
// the disk's FAR side bent up over the shadow and down under it, and that is multiple
// imaging rather than distortion. A closed-form deflection cannot produce it -- the
// weak-field alpha = 4M/b diverges logarithmically as b approaches the photon sphere,
// which is exactly where the higher-order images live. Stepping the photon equation
//
//     d2u/dphi2 + u = 3M u^2        (u = 1/r)
//
// in Cartesian leapfrog form gives those crossings for free: a ray passing close
// enough cuts the disk twice or more, and each crossing is one image. Radii follow the
// published Interstellar imagery (James, von Tunzelmann, Franklin & Thorne, Class.
// Quantum Grav. 32, 065001, 2015): the horizon sits at 1.8 r_g for spin a/M = 0.6, the
// shadow an observer sees is sqrt(27) = 5.196 r_g -- 2.6 times the horizon, because a
// black hole always looks bigger than it is -- and the disk starts at the innermost
// stable circular orbit, 3.83 r_g.
//
// The disk itself is volumetric, and that part follows ArcaneVortex's approach, used
// with its author's permission (see CREDITS.txt). The detail worth stealing is that
// noise modulates the disk's local THICKNESS rather than its brightness. Brightness
// noise gives a hard disk with blotches on it; thickness noise gives torn, wispy
// edges, which is what makes it read as gas. An earlier version of this file tested
// for plane crossings instead, which is more correct for an infinitely thin disk and
// looked like a painted ring.

uniform mat4 InverseProjectionMat;
// xyz = hole centre in OpenGL eye space (-Z forward), w = r_g in blocks
uniform vec4 HoleCentre;
// xyz = spin axis in eye space, unit length; w = spin a/M
uniform vec4 SpinAxis;
// x = opened 0..1, y = criticality (0..1) + blast (0..1) -> 0..2, z = wrapped seconds, w = swallowed count (0..10)
uniform vec4 HoleState;
// x = disk inner radius (r_g), y = outer radius (r_g), z = raymarch target steps (>= 16.0), w = brightness
uniform vec4 DiskShape;
// xyz = disk tangent U in eye space, w = unused
uniform vec4 DiskU;

const int GARG_STEPS_MAX = 128;
const float GARG_HORIZON = 1.8;
const float GARG_SHADOW = 5.196;
const float GARG_ESCAPE = 80.0;
const float GARG_TAU = 6.2831853;
// Half-thickness scale height at the inner and outer edge, in r_g.
const float GARG_THICK_INNER = 0.52;
const float GARG_THICK_OUTER = 0.35;

/**
 * True Interstellar Blackbody Palette:
 * White-hot core -> Champagne gold -> Luminous amber -> Fiery bronze-orange -> Deep cosmic ember red
 */
vec3 gargDiskColour(float t) {
    vec3 c0 = vec3(1.000, 0.985, 0.940); // 0.00: Incandescent core
    vec3 c1 = vec3(1.000, 0.860, 0.520); // 0.25: Radiant champagne gold
    vec3 c2 = vec3(0.980, 0.620, 0.220); // 0.50: Glowing solar amber
    vec3 c3 = vec3(0.850, 0.360, 0.080); // 0.75: Interstellar fiery bronze
    vec3 c4 = vec3(0.480, 0.120, 0.025); // 1.00: Smoldering cosmic dust edge

    float k = clamp(t, 0.0, 1.0);
    if (k < 0.25) {
        return mix(c0, c1, k / 0.25);
    }
    if (k < 0.50) {
        return mix(c1, c2, (k - 0.25) / 0.25);
    }
    if (k < 0.75) {
        return mix(c2, c3, (k - 0.50) / 0.25);
    }
    return mix(c3, c4, (k - 0.75) / 0.25);
}

float gargWhiteNoise(vec2 uv) {
    return fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453123);
}

/**
 * Entry and exit distance along a ray for a sphere at the origin, or (-1,-1) on a
 * miss. Entry is clamped to zero so a camera inside the sphere starts where it is.
 */
vec2 gargSphereHit(vec3 origin, vec3 dir, float radius) {
    float b = dot(origin, dir);
    float c = dot(origin, origin) - radius * radius;
    float disc = b * b - c;
    if (disc < 0.0) {
        return vec2(-1.0);
    }
    float root = sqrt(disc);
    return vec2(max(-b - root, 0.0), -b + root);
}


void main() {
    float rg = max(HoleCentre.w, 0.0001);
    vec3 sceneColour = texture(SceneSampler, vUv).rgb;

    vec4 clip = vec4(vUv * 2.0 - 1.0, 1.0, 1.0);
    vec4 eye = InverseProjectionMat * clip;
    if (abs(eye.w) < 0.000001) {
        fragColor = vec4(sceneColour, 1.0);
        return;
    }
    vec3 rayDir = normalize(eye.xyz / eye.w);

    vec3 centre = HoleCentre.xyz;
    float along = dot(centre, rayDir);
    float impact = length(centre - rayDir * along);
    float holeDistance = length(centre);
    float shadowEdge = rg * GARG_SHADOW;
    float reach = rg * DiskShape.y * 1.35;

    // Everything below is in units of r_g, in the hole's own frame.
    vec3 axis = normalize(SpinAxis.xyz);
    vec3 p = -centre / rg;
    vec3 d = rayDir;
    float h = length(cross(p, d));

    vec2 span = gargSphereHit(p, d, DiskShape.y * 1.35);
    if (span.y < 0.0) {
        fragColor = vec4(sceneColour, 1.0);
        return;
    }

    // How far along this ray the solid world is, in blocks. Sky reads as infinity.
    float sceneDistance = 1.0e9;
    float depth = texture(DepthSampler, vUv).r;
    if (depth > 0.0001 && depth < SL_SKY_DEPTH) {
        vec4 sceneClip = vec4(vUv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
        vec4 sceneEye = InverseProjectionMat * sceneClip;
        if (abs(sceneEye.w) > 0.000001 && (sceneEye.z / sceneEye.w) < 0.0) {
            sceneDistance = length(sceneEye.xyz / sceneEye.w);
        }
    }

    p += d * span.x;
    float travelledBlocks = span.x * rg;
    if (travelledBlocks > sceneDistance) {
        fragColor = vec4(sceneColour, 1.0);
        return;
    }

    // Distance along ray where it enters the shadow sphere (if ray aims at the hole)
    bool rayHitsShadowCone = along > 0.0 && impact < shadowEdge;
    float frontHoleDist = rayHitsShadowCone ? (along - sqrt(max(shadowEdge * shadowEdge - impact * impact, 0.0))) : 1.0e9;
    bool isInsideShadow = rayHitsShadowCone && sceneDistance >= frontHoleDist;

    vec3 diskU;
    if (dot(DiskU.xyz, DiskU.xyz) > 0.25) {
        diskU = normalize(DiskU.xyz);
    } else {
        vec3 refVec = abs(axis.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
        diskU = normalize(cross(axis, refVec));
    }
    vec3 diskV = cross(axis, diskU);

    p += d * (gargWhiteNoise(gl_FragCoord.xy) * 0.35);

    float targetSteps = DiskShape.z >= 16.0 ? DiskShape.z : 64.0;
    int steps = int(mix(targetSteps * 0.45, targetSteps,
            clamp(reach / max(holeDistance, 0.001) * 1.6, 0.0, 1.0)));

    vec3 accum = vec3(0.0);
    float alpha = 0.0;
    bool captured = false;
    bool hitSolid = false;
    float time = HoleState.z;
    float inner = DiskShape.x;
    float outer = DiskShape.y;

    for (int i = 0; i < GARG_STEPS_MAX; ++i) {
        if (i >= steps) {
            break;
        }
        float radius = length(p);
        float height = dot(p, axis);
        float nearPlane = max(abs(height) * 0.35, radius * 0.012);
        float dt = clamp(min(radius * 0.10, nearPlane), 0.02, 1.2);

        p += d * dt;
        travelledBlocks += dt * rg;
        float travelled = length(p);
        // Geodesic leapfrog integration (modulated by Gravitational Lensing toggle via SpinAxis.w)
        float lensMult = SpinAxis.w >= 0.05 ? 1.0 : 0.0;
        d = normalize(d + (-1.5 * lensMult * h * h * p / pow(travelled, 5.0)) * dt);

        if (travelledBlocks >= sceneDistance) {
            hitSolid = true;
            break;
        }
        if (travelled < GARG_HORIZON) {
            captured = true;
            break;
        }
        if (travelled > GARG_ESCAPE) {
            break;
        }

        height = dot(p, axis);
        vec3 planar = p - axis * height;
        float radial = length(planar);
        if (radial < inner || radial > outer) {
            continue;
        }

        vec2 flat2 = vec2(dot(planar, diskU), dot(planar, diskV));
        float t = clamp((radial - inner) / max(outer - inner, 0.001), 0.0, 1.0);

        // 1. Keplerian Differential Rotation + Inward Accretion Infall Flow (Cinematic Interstellar Motion)
        float keplerSpeed = pow(inner / max(radial, inner), 1.5);
        // Fast, energetic relativistic inner orbit grading outward
        float orbitalPhase = time * 2.2 * keplerSpeed;
        // Inward accretion flow: gas visibly spirals toward the event horizon
        float accretionInflow = time * 0.40 * sqrt(inner / max(radial, inner));
        float flowRadial = radial + accretionInflow;

        // 2. Continuous 2D Rotated Coordinates (100% Seamless, Zero Branch Cut, No Seam at 360 deg)
        // Rotate flat2 in Cartesian space backwards by orbital phase + logarithmic spiral twist
        float rotAngle = -orbitalPhase + log(max(flowRadial / inner, 1.0)) * 2.0;
        float cosA = cos(rotAngle);
        float sinA = sin(rotAngle);
        vec2 rotatedPos = vec2(
            flat2.x * cosA - flat2.y * sinA,
            flat2.x * sinA + flat2.y * cosA
        );
        float flowAngle = atan(rotatedPos.y, rotatedPos.x);

        // 3. Multi-octave Flowing Gas Plasma & Turbulent Boiling Eddies
        float gasNoise = 0.0;
        float weight = 0.0;
        float boilingTime = time * 0.45;
        for (int octave = 0; octave < 3; ++octave) {
            float freq = pow(2.0, float(octave));
            vec2 uv = rotatedPos * (0.040 * freq);
            // Dynamic fluid convection curls & eddy boiling
            vec2 eddy = vec2(
                sin(uv.y * 3.14159 + uv.x + boilingTime * freq) * 0.07,
                cos(uv.x * 3.14159 - uv.y + boilingTime * freq * 0.8) * 0.07
            );
            float layer = 1.0 / freq;
            float sampleVal = dot(textureLod(NoiseSampler, fract(uv + eddy), 0.0).rgb, vec3(0.299, 0.587, 0.114));
            gasNoise += layer * sampleVal;
            weight += layer;
        }
        float noise = gasNoise / max(weight, 0.001);
        noise = smoothstep(0.18, 0.82, noise);

        // 4. Azimuthal Gas Filaments (Whipping along orbit, perfectly periodic in 2*pi, ZERO comb-teeth artifacts)
        float stream1 = sin(flowAngle * 3.0 + noise * 3.8);
        float stream2 = cos(flowAngle * 5.0 - log(max(flowRadial, 1.0)) * 2.2 + noise * 2.4);
        float filament = clamp(noise * (0.72 + 0.30 * stream1 + 0.18 * stream2), 0.0, 1.0);

        // 5. Gaussian Volumetric Scale-Height Atmosphere: gives thick, soft 3D gaseous glow
        float diskScaleHeight = mix(GARG_THICK_INNER, GARG_THICK_OUTER, t) * (0.65 + 0.35 * filament);
        float zNorm = abs(height) / max(diskScaleHeight, 0.005);
        if (zNorm > 2.8) {
            continue;
        }
        float verticalCoverage = exp(-zNorm * zNorm * 1.8);

        // 6. Relativistic Doppler Beaming (approaching gas amplified & blued, receding gas softened & amber)
        vec3 tangentDir = normalize(cross(axis, planar));
        float viewDoppler = dot(tangentDir, -d) * keplerSpeed;
        float dopplerFactor = clamp(1.0 + viewDoppler * 0.72, 0.28, 2.7);
        float dopplerBoost = pow(dopplerFactor, 2.6);

        // 7. Blackbody Temperature Shift & Radiative Profile
        float radialProfile = pow(1.0 - t, 1.45)
                * smoothstep(0.0, 0.035, t) * smoothstep(1.0, 0.82, t);
        float fed = 1.0 + min(HoleState.w, 6.0) * 0.16;

        float colorTemp = clamp(t - (filament - 0.5) * 0.22 - viewDoppler * 0.28, 0.0, 1.0);
        vec3 baseCol = gargDiskColour(colorTemp);
        vec3 hotCore = vec3(1.0, 0.98, 0.95) * pow(max(0.0, 1.0 - t * 2.2), 2.2) * max(0.0, viewDoppler);

        float intensity = (0.28 + 1.20 * pow(filament, 1.3)) * dopplerBoost;
        vec3 emission = (baseCol + hotCore * 0.45) * radialProfile * max(DiskShape.w, 0.0)
                * 9.0 * fed * intensity;

        // 8. Physical Beer-Lambert Volumetric Integration (Step-Length Invariant: completely eliminates concentric raymarch rings)
        float density = verticalCoverage * radialProfile * (0.22 + 0.48 * filament);
        float stepAbsorption = 1.0 - exp(-density * dt * 2.6);
        vec3 stepEmission = emission * stepAbsorption;
        accum += stepEmission * (1.0 - alpha);
        alpha += (1.0 - alpha) * stepAbsorption;
        if (alpha > 0.985) {
            break;
        }
    }

    // Open transition: 0.0 = untouched scene, 1.0 = fully active black hole
    float openFactor = clamp(HoleState.x, 0.0, 1.0);
    float crit = clamp(HoleState.y, 0.0, 1.0);
    float critBoost = 1.0 + crit * 1.8;
    float blast = clamp(HoleState.y - 1.0, 0.0, 1.0);

    // Screen-space gravitational lensing: prevent foreground bleeding
    vec2 lensedUv = vUv;
    if (SpinAxis.w >= 0.05 && openFactor > 0.001) {
        vec2 candidateUv = clamp(vUv + (d.xy - rayDir.xy) * 0.35 * openFactor, 0.001, 0.999);
        float candidateDepth = texture(DepthSampler, candidateUv).r;
        if (candidateDepth >= SL_SKY_DEPTH) {
            lensedUv = candidateUv;
        } else if (candidateDepth > 0.0001) {
            vec4 candClip = vec4(candidateUv * 2.0 - 1.0, candidateDepth * 2.0 - 1.0, 1.0);
            vec4 candEye = InverseProjectionMat * candClip;
            float candDist = (abs(candEye.w) > 0.000001) ? length(candEye.xyz / candEye.w) : 1.0e9;
            if (candDist >= holeDistance - shadowEdge) {
                lensedUv = candidateUv;
            }
        }
    }
    vec3 lensedScene = texture(SceneSampler, lensedUv).rgb;

    // Check if occluded by blocks in front of the black hole
    bool occludedByForeground = hitSolid && (sceneDistance < frontHoleDist && sceneDistance < holeDistance);

    // The background, read along the ray's deflected direction.
    // Luminous cosmic starfield & interstellar nebula lensed by the black hole.
    vec3 background = lensedScene;
    if (!captured && !hitSolid && SpinAxis.w >= 0.05) {
        float normDist = clamp(impact / max(reach, 0.001), 0.0, 1.0);
        float edgeFeather = smoothstep(1.0, 0.35, normDist);
        float bent = 1.0 - dot(d, rayDir);
        float lensed = clamp(bent * 12.0, 0.0, 1.0) * edgeFeather;

        if (lensed > 0.001) {
            float sceneLum = dot(lensedScene, vec3(0.299, 0.587, 0.114));
            vec3 cosmicSky = gargCosmicSky(d, time, sceneLum);
            background = lensedScene + cosmicSky * (lensed * edgeFeather);
        }
    }

    // The photon ring: hugging the outer edge of the apparent shadow (critical curve)
    float first = exp(-pow((impact - shadowEdge) / max(rg * 0.14, 0.001), 2.0));
    float second = exp(-pow((impact - shadowEdge * 1.035) / max(rg * 0.08, 0.001), 2.0))
            / 535.0;
    float ahead = step(0.0, along);
    float ringGain = 0.32 * ahead * max(DiskShape.w, 0.0) * (0.30 + 0.70 * alpha)
            * (1.0 + crit * 1.8);
    if (hitSolid && sceneDistance < frontHoleDist) {
        ringGain = 0.0;
    }
    vec3 ring = gargDiskColour(0.0) * (first + second) * ringGain;

    // Emissive disk & photon ring (superheated and energized during detonation)
    float blastDiskEnergize = 1.0 + blast * 3.5;
    vec3 diskRaw = (accum + ring) * (critBoost * blastDiskEnergize * openFactor);

    // --- ACES FILMIC TONE MAPPING FOR ACCRETION DISK ---
    // Smoothly compresses high dynamic range radiance into display space:
    // Prevents flat blown-out #FFFFFF clipping, preserving rich solar gold, amber, and bronze embers.
    vec3 diskToneMapped = diskRaw / (diskRaw + vec3(1.18, 1.35, 1.62));
    // Gentle vibrancy lift for filmic glow
    vec3 diskEmission = diskToneMapped * (1.0 + 0.28 * smoothstep(0.0, 0.75, diskToneMapped));

    // Background behind the disk
    vec3 sceneOrCosmic = (sceneDistance < 1.0e8) ? lensedScene : background;
    vec3 behindEffect = sceneColour;

    if (captured || isInsideShadow) {
        if (crit >= 0.99 && blast > 0.001) {
            // Singularity has detonated! Core blazes into brilliant white-gold supernova light
            // and dissolves gracefully into the background scene as spacetime heals.
            vec3 coreBurst = mix(vec3(1.4, 1.0, 0.6), vec3(4.5, 4.0, 3.2), blast);
            behindEffect = mix(sceneOrCosmic, coreBurst, clamp(blast * 1.6 + openFactor * 0.45, 0.0, 1.0));
        } else {
            // Active event horizon shadow
            behindEffect = vec3(0.0);
        }
    } else if (!occludedByForeground) {
        behindEffect = sceneOrCosmic;
    }

    // --- APOCALYPTIC SUPERNOVA DETONATION & COSMIC SHOCKWAVE VFX ---
    vec3 supernovaVfx = vec3(0.0);
    if (blast > 0.001 && along > 0.0 && (!hitSolid || sceneDistance > holeDistance)) {
        float progress = 1.0 - blast; // 0.0 at moment of detonation -> 1.0 as blast expands

        // A. Incandescent Supernova Core Fireball: expands rapidly from 1.2 rg to 7.0 rg
        float coreRadius = rg * (1.2 + progress * 6.5);
        float coreNorm = impact / max(coreRadius, 0.1);
        float coreShape = exp(-coreNorm * coreNorm * 3.5);
        vec3 coreCol = mix(vec3(1.0, 0.65, 0.25), vec3(1.0, 0.98, 1.0), blast);
        supernovaVfx += coreCol * (coreShape * blast * 7.5);

        // B. Primary Relativistic 3D Shockwave Shell with Chromatic Edge:
        float shockRadius = rg * (2.2 + progress * 24.0);
        float shockWidth = rg * (0.45 + progress * 1.1);
        float shockDist = abs(impact - shockRadius) / max(shockWidth, 0.1);
        float shockShape = exp(-shockDist * shockDist * 6.0);
        // Chromatic dispersion: cyan leading front, gold radiant body, violet trailing wake
        vec3 shockLead = vec3(0.4, 0.9, 1.0);
        vec3 shockBody = vec3(1.0, 0.88, 0.6);
        vec3 shockTrail = vec3(0.85, 0.45, 1.0);
        vec3 shockCol = mix(shockTrail, shockBody, clamp(blast * 1.4, 0.0, 1.0));
        if (impact > shockRadius) {
            shockCol = mix(shockBody, shockLead, clamp((impact - shockRadius) / max(shockWidth * 0.8, 0.05), 0.0, 1.0));
        }
        supernovaVfx += shockCol * (shockShape * blast * 6.0);

        // C. Secondary Harmonic Compression Wave (echo shockwave):
        float subShockRadius = shockRadius * 0.62;
        float subDist = abs(impact - subShockRadius) / max(shockWidth * 0.75, 0.1);
        float subShape = exp(-subDist * subDist * 5.0);
        supernovaVfx += vec3(0.95, 0.6, 0.2) * (subShape * blast * 2.8);

        // D. Relativistic Starburst Rays (Godrays piercing through spacetime):
        vec3 rayVec = (rayDir * along - centre) / max(rg, 0.01);
        float rayAngle = atan(dot(rayVec, diskU), dot(rayVec, diskV));
        float rayHarmonics = sin(rayAngle * 14.0 + time * 1.2) * cos(rayAngle * 9.0 - time * 0.8);
        float raySpikes = pow(abs(rayHarmonics), 2.5);
        float rayFalloff = exp(-impact / max(rg * 16.0, 1.0));
        vec3 rayColor = mix(vec3(1.0, 0.85, 0.5), vec3(1.0, 0.98, 1.0), blast);
        supernovaVfx += rayColor * (raySpikes * rayFalloff * blast * 5.0);

        // E. Cosmic Ray Flash on camera proximity
        float viewProximity = clamp(1.0 - impact / max(rg * 25.0, 1.0), 0.0, 1.0);
        supernovaVfx += vec3(1.0, 0.95, 0.88) * (blast * blast * viewProximity * 2.8);
    }

    // Smoothly blend the background between the untouched scene and the relativistic distortion
    vec3 blendedBg = mix(sceneColour, behindEffect, openFactor);

    // Final composite: disk emission layered over the background + supernova
    vec3 result = diskEmission + blendedBg * (1.0 - alpha * openFactor) + supernovaVfx;

    fragColor = vec4(result, 1.0);
}


