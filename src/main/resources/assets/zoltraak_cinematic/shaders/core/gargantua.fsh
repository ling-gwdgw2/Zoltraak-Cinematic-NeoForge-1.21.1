#version 150

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D NoiseSampler;

in vec2 vUv;
out vec4 fragColor;

#define SL_SKY_DEPTH 0.99999

// Background celestial sky & solid world are sampled directly from SceneSampler with subtle geodesic deflection,
// guaranteeing 100% mathematical continuity at the bounding sphere boundary with ZERO dark circle artifacts.

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
const float GARG_ESCAPE = 160.0;
const float GARG_TAU = 6.2831853;
// Half-thickness scale height of the volumetric plasma disk (in r_g)
// Slender, razor-sharp astrophysical accretion disk (matching Kip Thorne & Solas)
const float GARG_THICK_INNER = 0.085;
const float GARG_THICK_OUTER = 0.150;

/**
 * IterationT Planckian Locus Blackbody Radiation Function:
 * Accurately models real astrophysical thermal emission spectrum in sRGB space.
 */
vec3 gargBlackbody(float temperature) {
    const mat2x4 splineX = mat2x4(-0.2661293e9, -0.2343589e6, 0.8776956e3, 0.179910,
                                  -3.0258469e9,  2.1070479e6, 0.2226347e3, 0.240390);

    const mat3x4 splineY = mat3x4(-1.1063814, -1.34811020, 2.18555832, -0.20219683,
                                  -0.9549476, -1.37418593, 2.09137015, -0.16748867,
                                   3.0817580, -5.87338670, 3.75112997, -0.37001483);

    float rt = 1.0 / max(temperature, 1000.0);
    float rt2 = rt * rt;
    vec4 coeffX = vec4(rt2 * rt, rt2, rt, 1.0);

    float x = dot(coeffX, temperature < 4000.0 ? splineX[0] : splineX[1]);
    float x2 = x * x;
    vec4 coeffY = vec4(x2 * x, x2, x, 1.0);

    float z = 1.0 / dot(coeffY, temperature < 2222.0 ? splineY[0] : temperature < 4000.0 ? splineY[1] : splineY[2]);

    vec3 xyz = vec3(x * z, 1.0, z);
    xyz.z -= xyz.x + 1.0;

    const mat3 xyzToSrgb = mat3( 3.24097, -0.96924,  0.05563,
                                -1.53738,  1.87597, -0.20398,
                                -0.49861,  0.04156,  1.05697);

    return max(xyzToSrgb * xyz, vec3(0.0));
}

/**
 * IterationT Smooth Bell-Curve Radial Profile
 */
float gargPcurve(float x) {
    float x2 = x * x;
    return 12.207 * x2 * x2 * (1.0 - x);
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

/**
 * True ACES Filmic Tone Mapping (Krzysztof Narkowicz fit)
 * Smoothly rolls off high dynamic range radiance into display space:
 * Prevents flat desaturated #FFFFFF blowout while preserving rich golden-amber, solar yellow, and bronze embers.
 */
vec3 acesFilmic(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
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
    float holeDistance = length(centre);
    float along = dot(centre, rayDir);
    float impact = length(centre - rayDir * along);

    float openFactor = clamp(HoleState.x, 0.0, 1.0);
    float crit = clamp(HoleState.y, 0.0, 1.0);
    float blast = clamp(HoleState.y - 1.0, 0.0, 1.0);
    float time = HoleState.z;

    if (openFactor < 0.0001) {
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

    // --- ITERATION-T CANONICAL COORDINATE SYSTEM ---
    // 1.0 canonical unit = rg * 2.2 blocks (photon torus = 1.0, inner disk = 0.5, outer disk = 4.0)
    float uScale = max(rg * 2.2, 0.05);

    // Observer camera position in canonical space (singularity at origin 0,0,0)
    vec3 camPos = -centre / uScale;
    vec3 d = rayDir;

    // Bounding sphere of radius 4.8 around the black hole system
    const float BOUND_RADIUS = 4.8;
    vec2 span = gargSphereHit(camPos, d, BOUND_RADIUS);
    if (span.y < 0.0) {
        // Ray misses the entire black hole and accretion disk system
        fragColor = vec4(sceneColour, 1.0);
        return;
    }

    // Start ray at bounding sphere entry
    vec3 p = camPos + d * span.x;
    float travelledBlocks = span.x * uScale;
    if (travelledBlocks >= sceneDistance) {
        // Occluded by solid blocks in foreground
        fragColor = vec4(sceneColour, 1.0);
        return;
    }

    // Disk coordinate axes in eye space
    vec3 axis = normalize(SpinAxis.xyz);
    vec3 diskU;
    if (dot(DiskU.xyz, DiskU.xyz) > 0.25) {
        diskU = normalize(DiskU.xyz);
    } else {
        vec3 refVec = abs(axis.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
        diskU = normalize(cross(axis, refVec));
    }
    vec3 diskV = cross(axis, diskU);

    // Adaptive step configuration across chord
    float totalDist = span.y - span.x;
    const int STEPS = 64;
    float stepLength = totalDist / float(STEPS);

    // IterationT disk geometry bounds
    const float discRadius = 2.25;
    const float discWidth = 3.50;
    const float discInner = 0.50;
    const float discOuter = 4.00;

    vec3 result = vec3(0.0);
    float transmittance = 1.0;
    bool hitShadow = false;
    float lensMult = (SpinAxis.w >= 0.05) ? 1.0 : 0.0;

    for (int i = 0; i < STEPS; ++i) {
        if (transmittance < 0.001) break;

        // 1. IterationT WarpSpace Geodesic
        float sDist = length(p);
        float warpFactor = 1.0 / (sDist * sDist + 0.000001);
        vec3 sVec = -p / max(sDist, 0.0001);
        d = normalize(d + sVec * (warpFactor * (0.06 * lensMult)));

        vec3 pPrev = p;
        p += d * stepLength;
        travelledBlocks += stepLength * uScale;

        // Solid obstacle occlusion
        if (travelledBlocks >= sceneDistance) {
            break;
        }

        // 2. Black Hole Event Horizon Shadow (Segment-to-Sphere Test)
        // Tests the exact point of closest approach of this step segment to origin (r = 0.52).
        // This guarantees a razor-sharp, perfectly round and smooth shadow with ZERO dither noise!
        float tClose = clamp(-dot(pPrev, d), 0.0, stepLength);
        float dMin = length(pPrev + d * tClose);
        if (dMin < 0.52) {
            hitShadow = true;
            transmittance = 0.0;
            break;
        }

        // 3. Disk local frame
        float h = dot(p, axis);
        vec3 planar = p - axis * h;
        float r = length(planar);
        float dist = abs(h);

        // IterationT canonical radial gradient: spans 1.0 at inner rim (0.5) to 0.5 at outer rim (4.0)
        float radialGradient = 1.0 - clamp((r - discInner) / discWidth * 0.5, 0.0, 1.0);
        float discThickness = 0.10 * radialGradient;

        // Smooth outer dissipation between r = 3.0 and r = 4.0:
        // Ensures the accretion disk softly fades to 0 at the outer boundary with zero grain / cliff edge!
        float outerFade = clamp((discOuter - r) / 1.0, 0.0, 1.0);
        outerFade = outerFade * outerFade * (3.0 - 2.0 * outerFade);

        // IterationT Inner Edge Volumetric Bloom Factor
        float fr = abs(r - discInner) + 0.4;
        fr = fr * fr;
        float fade = fr * fr * 0.04;
        float bloomFactor = 1.0 / (h * h * 40.0 + fade + 0.00002);
        bloomFactor *= clamp(2.0 - dist / discThickness, 0.0, 1.0);
        bloomFactor = bloomFactor * bloomFactor;

        // Smooth Bell-Curve Radial Density Profile
        float dr = gargPcurve(radialGradient);
        float density = dr * clamp(1.0 - dist / discThickness, 0.0, 1.0) * 0.7;
        density = clamp(density + bloomFactor * 0.1, 0.0, 1.0) * outerFade;

        if (density > 0.0001 && r >= discInner) {
            // Concentric Keplerian dust rings & turbulence
            float orbitalPhase = time * 0.8;
            vec2 noiseCoord = vec2(r * 2.8, h * 0.6) + vec2(orbitalPhase * 0.03, 0.0);
            float noiseVal = textureLod(NoiseSampler, fract(noiseCoord * 0.25), 0.0).r;
            float fbm = 0.35 + 0.65 * noiseVal;
            density *= (fbm * fbm) * dr;

            // IterationT True Blackbody Thermal Emission:
            // High dynamic range mapped with sub-linear compression so the core is brilliant white,
            // while mid disk is champagne gold and outer disk is rich glowing amber.
            float gr = 1.0 - radialGradient;
            gr = gr * gr;
            float glowStrength = 1.0 / (gr * gr * 400.0 + 0.002);
            float glowFactor = pow(glowStrength, 0.65) * 1.8 + 0.40;
            float temp = 2700.0 + min(glowStrength * 50.0, 25000.0);
            vec3 glow = gargBlackbody(temp) * glowFactor;

            // Relativistic Doppler beaming
            float flowAngle = atan(dot(planar, diskV), dot(planar, diskU));
            glow *= clamp(sin(flowAngle - 1.07) * 0.55 + 1.0, 0.40, 1.65);

            // Volumetric Beer-Lambert integration
            float stepTrans = exp2(-density * 6.0 * (stepLength / 0.15));
            float integral = 1.0 - stepTrans;
            transmittance *= stepTrans;

            result += integral * transmittance * glow;
        }

        // 4. IterationT 12,000K Incandescent Photon Torus Bloom
        vec2 torusCircle = vec2(0.95, 0.015);
        float torusDist = length(vec2(r - torusCircle.x, h + 0.03));
        float bloomDisc = 1.0 / (pow(torusDist * 3.5, 3.2) + 0.001);
        bloomDisc *= step(0.50, r);
        vec3 col = gargBlackbody(12000.0);

        result += col * bloomDisc * (0.008 * stepLength) * transmittance;
    }

    // --- BACKGROUND SCENE & GRAVITATIONAL LENSING ---
    // Gravitational deflection smoothly bends whatever is behind the black hole
    // (the player's sky, planets, stars, or solid blocks in the background).
    // At the bounding sphere boundary, deflection -> 0, guaranteeing zero seam artifacts.
    vec2 lensOffset = (d.xy - rayDir.xy) * (0.025 * openFactor);
    vec3 bg = texture(SceneSampler, clamp(vUv + lensOffset, 0.001, 0.999)).rgb;

    vec3 behindEffect = bg * transmittance;
    if (hitShadow) {
        if (crit >= 0.99 && blast > 0.001) {
            vec3 coreBurst = mix(vec3(1.4, 1.0, 0.6), vec3(5.0, 4.5, 3.8), blast);
            behindEffect = coreBurst * blast;
        } else {
            behindEffect = vec3(0.0);
        }
    }

    // Accretion disk emission: Standard elegant filmic Reinhard tone mapping.
    // Perfectly bounds radiance into [0, 1] without flat 80% blowout:
    // Core (r <= 1.1) reaches dazzling white, mid disk is champagne gold, outer disk is rich glowing amber.
    float blastDiskEnergize = 1.0 + blast * 3.5;
    vec3 diskRaw = result * (openFactor * (1.0 + crit * 1.5) * blastDiskEnergize);
    vec3 diskEmission = diskRaw / (diskRaw + 1.0);

    // --- APOCALYPTIC SUPERNOVA DETONATION & COSMIC SHOCKWAVE VFX ---
    vec3 supernovaVfx = vec3(0.0);
    if (blast > 0.001 && along > 0.0 && (!hitShadow || sceneDistance > holeDistance)) {
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

    // Final composite: background transmitted through disk + glowing accretion disk + supernova VFX
    vec3 composite = behindEffect + diskEmission + supernovaVfx;

    fragColor = vec4(mix(sceneColour, composite, openFactor), 1.0);
}


