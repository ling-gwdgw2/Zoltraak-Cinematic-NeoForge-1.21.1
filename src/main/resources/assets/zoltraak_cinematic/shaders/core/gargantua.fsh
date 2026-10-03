#version 150

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D NoiseSampler;

in vec2 vUv;
out vec4 fragColor;

#define SL_SKY_DEPTH 0.99999

// Volumetric star field: Kali's "Star Nest", from Shadertoy.
#ifndef STAR_NEST_GLSL
#define STAR_NEST_GLSL

#define SN_ITERATIONS 17
#define SN_FORMUPARAM 0.53
#define SN_VOLSTEPS 20
#define SN_STEPSIZE 0.1
#define SN_TILE 0.850
#define SN_BRIGHTNESS 0.0015
#define SN_DARKMATTER 0.300
#define SN_DISTFADING 0.730
#define SN_SATURATION 0.850

vec3 starNest(vec3 dir, float drift) {
    vec3 from = vec3(1.0, 0.5, 0.5) + vec3(drift * 2.0, drift, -2.0);

    float s = 0.1;
    float fade = 1.0;
    vec3 v = vec3(0.0);

    for (int r = 0; r < SN_VOLSTEPS; r++) {
        vec3 p = from + s * dir * 0.5;
        p = abs(vec3(SN_TILE) - mod(p, vec3(SN_TILE * 2.0)));

        float pa = 0.0;
        float a = 0.0;
        for (int i = 0; i < SN_ITERATIONS; i++) {
            p = abs(p) / dot(p, p) - SN_FORMUPARAM;
            a += abs(length(p) - pa);
            pa = length(p);
        }

        float dm = max(0.0, SN_DARKMATTER - a * a * 0.001);
        a *= a * a;
        if (r > 6) {
            fade *= 1.0 - dm;
        }

        v += fade;
        v += vec3(s, s * s, s * s * s * s) * a * SN_BRIGHTNESS * fade;
        fade *= SN_DISTFADING;
        s += SN_STEPSIZE;
    }

    v = mix(vec3(length(v)), v, SN_SATURATION);
    return v * 0.01;
}

#endif

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
// x = opened 0..1, y = criticality 0..1, z = wrapped seconds, w = swallowed lights
uniform vec4 HoleState;
// x = disk inner radius, y = outer radius, both in r_g; z = h/r; w = brightness
uniform vec4 DiskShape;

const int GARG_STEPS_MAX = 112;
const float GARG_HORIZON = 1.8;
const float GARG_SHADOW = 5.196;
const float GARG_ESCAPE = 60.0;
const float GARG_TAU = 6.2831853;
// Half-thickness at the inner and outer edge, in r_g. Slender, sleek proportions matching Interstellar
const float GARG_THICK_INNER = 0.38;
const float GARG_THICK_OUTER = 0.20;

/**
 * Disk colour by radius: pale white-yellow at the inner edge grading to radiant champagne gold,
 * glowing amber and interstellar bronze-orange, matching authentic Gargantua.
 */
vec3 gargDiskColour(float t) {
    vec3 c0 = vec3(1.000, 0.973, 0.902);
    vec3 c1 = vec3(1.000, 0.839, 0.588);
    vec3 c2 = vec3(0.988, 0.667, 0.337);
    vec3 c3 = vec3(0.839, 0.424, 0.149);
    float k = clamp(t, 0.0, 1.0);
    if (k < 0.34) {
        return mix(c0, c1, k / 0.34);
    }
    if (k < 0.67) {
        return mix(c1, c2, (k - 0.34) / 0.33);
    }
    return mix(c2, c3, (k - 0.67) / 0.33);
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

    vec3 diskU = normalize(cross(axis, vec3(0.0, 0.0, 1.0)) + vec3(1.0e-4));
    vec3 diskV = cross(axis, diskU);

    p += d * (gargWhiteNoise(vUv * 512.0) * 0.1);

    int steps = int(mix(48.0, float(GARG_STEPS_MAX),
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
        float nearPlane = max(abs(height) * 0.55, radius * 0.02);
        float dt = clamp(min(radius * 0.12, nearPlane), 0.03, 1.4);

        p += d * dt;
        travelledBlocks += dt * rg;
        float travelled = length(p);
        // Geodesic leapfrog integration
        d = normalize(d + (-1.5 * h * h * p / pow(travelled, 5.0)) * dt);

        if (travelled < GARG_HORIZON) {
            captured = true;
            break;
        }
        if (travelledBlocks >= sceneDistance) {
            if (sceneDistance < frontHoleDist) {
                hitSolid = true;
                break;
            }
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
        float twist = radial * 0.45;
        float spin = time * 0.55;

        float noise = 0.0;
        float weight = 0.0;
        for (int octave = 0; octave < 3; ++octave) {
            float scale = 0.18 * pow(2.0, float(octave));
            float drift = mod(twist + spin * (1.0 + float(octave) * 0.35), GARG_TAU);
            float cs = cos(drift);
            float sn = sin(drift);
            vec2 uv = vec2(flat2.x * cs - flat2.y * sn,
                           flat2.x * sn + flat2.y * cs) * scale;
            float layer = 1.0 / pow(2.0, float(octave));
            noise += layer * dot(texture(NoiseSampler, uv).rgb,
                    vec3(0.299, 0.587, 0.114));
            weight += layer;
        }
        noise /= max(weight, 0.001);

        float t = (radial - inner) / max(outer - inner, 0.001);
        float thickness = mix(GARG_THICK_INNER, GARG_THICK_OUTER, t)
                * mix(0.10, 1.35, noise);
        float coverage = smoothstep(thickness, 0.0, abs(height));
        if (coverage <= 0.002) {
            continue;
        }

        // Seamless inner emission connecting smoothly right to the shadow boundary
        float profile = pow(1.0 - t, 1.4)
                * smoothstep(0.0, 0.015, t) * smoothstep(1.0, 0.90, t);
        float fed = 1.0 + min(HoleState.w, 6.0) * 0.16;
        float grain = 0.35 + 0.65 * noise;
        vec3 emission = gargDiskColour(t) * profile * max(DiskShape.w, 0.0)
                * 9.0 * fed * grain;

        // Front to back accumulation
        // Deliberately small (0.22) so ray builds up over a dozen or more samples,
        // giving deep volumetric structure and preventing opaque shell blowouts.
        float local = clamp(coverage * profile * 0.22, 0.0, 1.0);
        accum += emission * (1.0 - alpha) * local;
        alpha += (1.0 - alpha) * local;
        if (alpha > 0.99) {
            break;
        }
    }

    // The background, read along the ray's new direction.
    // Restores the deep space Interstellar cosmic starfield with smooth radial boundary feathering.
    vec3 background = sceneColour;
    if (!captured && !hitSolid) {
        float normDist = clamp(impact / max(reach, 0.001), 0.0, 1.0);
        float edgeFeather = smoothstep(1.0, 0.45, normDist);
        float bent = 1.0 - dot(d, rayDir);
        float lensed = clamp(bent * 20.0, 0.0, 1.0) * edgeFeather;
        if (lensed > 0.001) {
            vec3 stars = starNest(d, time * 0.01);
            background = mix(sceneColour, stars, lensed);
        }
    }

    // The photon ring: hugging the outer edge of the apparent shadow (critical curve)
    float first = exp(-pow((impact - shadowEdge) / max(rg * 0.14, 0.001), 2.0));
    float second = exp(-pow((impact - shadowEdge * 1.035) / max(rg * 0.08, 0.001), 2.0))
            / 535.0;
    float ahead = step(0.0, along);
    float ringGain = 0.32 * ahead * max(DiskShape.w, 0.0) * (0.30 + 0.70 * alpha)
            * (1.0 + HoleState.y * 1.8);
    if (hitSolid && sceneDistance < frontHoleDist) {
        ringGain = 0.0;
    }
    vec3 ring = gargDiskColour(0.0) * (first + second) * ringGain;

    // Open transition: 0.0 = untouched scene, 1.0 = fully active black hole
    float openFactor = clamp(HoleState.x, 0.0, 1.0);
    float critBoost = 1.0 + HoleState.y * 1.8;

    // Emissive disk & photon ring
    vec3 diskEmission = (accum + ring) * critBoost * openFactor;

    // Background behind the disk
    float blast = clamp(HoleState.w, 0.0, 1.0);
    vec3 behindEffect = sceneColour;
    if (captured || isInsideShadow) {
        // Supernova core detonation at tick 1100: singularity detonates into brilliant white-gold!
        vec3 supernovaCore = mix(vec3(0.0), vec3(3.2, 2.9, 2.4), blast);
        behindEffect = supernovaCore;
    } else if (!hitSolid) {
        behindEffect = background;
    }

    // --- SUPERNOVA DETONATION & EXPANDING COSMIC FIREBALL ---
    vec3 supernovaVfx = vec3(0.0);
    if (blast > 0.001 && along > 0.0 && (!hitSolid || sceneDistance > holeDistance)) {
        // A. Expanding Incandescent Core Fireball: expands rapidly from 1.5 rg up to 6.0 rg
        float coreRadius = rg * (1.5 + (1.0 - blast) * 4.5);
        float coreNorm = impact / max(coreRadius, 0.1);
        float coreShape = exp(-coreNorm * coreNorm * 3.0);
        vec3 coreCol = mix(vec3(1.0, 0.75, 0.4), vec3(1.0, 0.98, 1.0), blast);
        supernovaVfx += coreCol * (coreShape * blast * 6.0);

        // B. Relativistic 3D Shockwave Blast Shell Ring: expands outward from 3.0 rg to 16.0 rg
        float shockRadius = rg * (2.8 + (1.0 - blast) * 13.5);
        float shockDist = abs(impact - shockRadius) / max(rg * 0.7, 0.15);
        float shockShape = exp(-shockDist * shockDist * 5.5);
        vec3 shockCol = mix(vec3(0.5, 0.85, 1.0), vec3(1.0, 0.9, 0.7), blast);
        supernovaVfx += shockCol * (shockShape * blast * 4.5);

        // C. Supernova Cosmic Ray Flash when looking near the blast
        float viewProximity = clamp(1.0 - impact / max(rg * 20.0, 1.0), 0.0, 1.0);
        supernovaVfx += vec3(1.0, 0.96, 0.9) * (blast * blast * viewProximity * 2.2);
    }

    // Smoothly blend the background between the untouched scene and the relativistic distortion
    vec3 blendedBg = mix(sceneColour, behindEffect, openFactor);

    // Final composite: disk emission layered over the background + supernova
    vec3 result = diskEmission + blendedBg * (1.0 - alpha * openFactor) + supernovaVfx;

    fragColor = vec4(result, 1.0);
}


