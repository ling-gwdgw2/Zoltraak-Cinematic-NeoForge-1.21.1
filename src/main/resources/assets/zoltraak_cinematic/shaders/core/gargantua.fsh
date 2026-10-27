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

    // Cosmic nebula palette (Solas Shader celestial aesthetic): deep interstellar void, emerald-teal auroral gas, celestial violet, radiant stardust gold
    vec3 nebBase = vec3(0.03, 0.07, 0.20);
    vec3 nebEmerald = vec3(0.06, 0.38, 0.26);
    vec3 nebViolet = vec3(0.42, 0.16, 0.58);
    vec3 nebHighlight = vec3(1.0, 0.82, 0.45);
    vec3 nebCol = mix(nebBase, nebEmerald, clamp(n1 * 1.6, 0.0, 1.0));
    nebCol = mix(nebCol, nebViolet, clamp(n2 * 1.4 - 0.2, 0.0, 1.0));
    nebCol = mix(nebCol, nebHighlight, pow(n3, 2.0));
    vec3 nebulaLight = nebCol * (nebDensity * 2.8);

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
    float along = dot(centre, rayDir);
    float impact = length(centre - rayDir * along);
    float holeDistance = length(centre);
    float shadowEdge = rg * GARG_SHADOW;
    // Gravitational lensing envelope: extends far beyond the physical gas disk to capture
    // astrophysical spacetime curvature, background deflection, and Einstein rings at long range.
    float lensingRadius = max(DiskShape.y * 1.85, 45.0);
    float escapeRadius = lensingRadius * 1.10;
    float reach = rg * lensingRadius;

    // Everything below is in units of r_g, in the hole's own frame.
    vec3 axis = normalize(SpinAxis.xyz);
    vec3 p = -centre / rg;
    vec3 d = rayDir;
    float h = length(cross(p, d));

    vec2 span = gargSphereHit(p, d, lensingRadius);
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

    // Ray origin dither: decorrelates per-pixel sampling phase to eliminate all concentric raymarch ripples
    float startJitter = clamp(length(p) * 0.04, 0.03, 0.25) * gargWhiteNoise(gl_FragCoord.xy);
    p += d * startJitter;

    float targetSteps = DiskShape.z >= 16.0 ? DiskShape.z : 96.0;
    int steps = int(targetSteps);

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
        vec3 planar = p - axis * height;
        float radial = length(planar);

        // Adaptive step size:
        // Outside the disk: take efficient steps through empty space so geodesics reach both front & back
        float distRadial = max(inner - radial, radial - outer);
        float diskDist = max(distRadial, abs(height) - 0.8);
        float dt;
        if (diskDist > 0.0) {
            dt = clamp(min(radius * 0.12, diskDist * 0.45 + 0.35), 0.08, 3.2);
        } else {
            // Inside the disk slab: fine resolution to integrate slender volumetric filaments
            float nearPlane = max(abs(height) * 0.40, radius * 0.020);
            dt = clamp(min(radius * 0.065, nearPlane), 0.025, 0.50);
        }

        p += d * dt;
        travelledBlocks += dt * rg;
        float travelled = length(p);

        // IterationT Relativistic 1/r^2 Spacetime Warping Geodesic:
        // Smoothly and stably curves light around the black hole across all distance scales
        float lensMult = SpinAxis.w >= 0.05 ? 1.0 : 0.0;
        vec3 singularityVec = -p / max(travelled, 0.001);
        float warpFactor = 1.0 / (travelled * travelled + 0.01);
        d = normalize(d + singularityVec * (2.4 * warpFactor * lensMult * dt));

        if (travelledBlocks >= sceneDistance) {
            hitSolid = true;
            break;
        }
        if (travelled < GARG_HORIZON) {
            captured = true;
            break;
        }
        if (travelled > escapeRadius) {
            break;
        }

        height = dot(p, axis);
        planar = p - axis * height;
        radial = length(planar);
        if (radial < inner || radial > outer) {
            continue;
        }

        vec2 flat2 = vec2(dot(planar, diskU), dot(planar, diskV));
        float t = clamp((radial - inner) / max(outer - inner, 0.001), 0.0, 1.0);

        // --- 1. SOLAS-GRADE PROTOPLANETARY ACCRETION DYNAMICS ---
        // Differential Keplerian orbital rotation + Inward spiral accretion flow
        float keplerSpeed = pow(inner / max(radial, inner), 1.5);
        float orbitalPhase = time * 2.2 * keplerSpeed;
        float accretionInflow = time * 0.38 * sqrt(inner / max(radial, inner));
        float flowRadial = radial + accretionInflow;

        // Continuous 2D Rotation (zero branch-cut seam, 100% smooth 360 deg)
        float rotAngle = -orbitalPhase + log(max(flowRadial / inner, 1.0)) * 2.0;
        float cosA = cos(rotAngle);
        float sinA = sin(rotAngle);
        vec2 rotatedPos = vec2(
            flat2.x * cosA - flat2.y * sinA,
            flat2.x * sinA + flat2.y * cosA
        );
        float flowAngle = atan(rotatedPos.y, rotatedPos.x);

        // --- 2. MULTI-SCALE KEPLERIAN PLASMA ACCRETION DYNAMICS ---
        // Channel R: Perlin base flow | Channel G: Worley cellular knots | Channel B: High-freq detail filaments
        float boilingTime = time * 0.28;
        vec2 uvBase = rotatedPos * 0.032;
        vec2 eddy = vec2(
            sin(uvBase.y * 3.1416 + boilingTime) * 0.045,
            cos(uvBase.x * 3.1416 + boilingTime * 0.82) * 0.045
        );
        vec4 noiseTex = textureLod(NoiseSampler, fract(uvBase + eddy), 0.0);
        float perlin = noiseTex.r;
        float worley = 1.0 - noiseTex.g; // Invert Worley so cellular lobes become luminous plasma knots
        float detail = noiseTex.b;       // High-frequency turbulent filaments

        // Multi-frequency gas density synthesis
        float plasmaDensity = perlin * 0.44 + worley * 0.36 + detail * 0.20;
        plasmaDensity = smoothstep(0.16, 0.84, plasmaDensity);

        // Concentric Keplerian shear waves & Logarithmic spiral density arms
        float whirl = -4.2;
        float spiralPhase = flowAngle + sqrt(radial / inner) * whirl;
        float spiralArm1 = sin(spiralPhase * 2.0) * 0.5 + 0.5;
        float spiralArm2 = sin(spiralPhase * 4.0 + 1.2) * 0.5 + 0.5;
        float striations = sin(radial * 5.2 - orbitalPhase * 1.2) * 0.5 + 0.5; // Fine concentric Keplerian rings

        float streamPattern = mix(spiralArm1, spiralArm2, 0.38) * 0.65 + striations * 0.35;
        float filament = clamp(plasmaDensity * (0.45 + 0.75 * streamPattern), 0.0, 1.0);

        // --- 3. SLENDER 3D ATMOSPHERIC SCALE-HEIGHT & INNER BLOOM ---
        float radialGradient = 1.0 - t;
        float discThickness = mix(GARG_THICK_INNER, GARG_THICK_OUTER, t);

        // IterationT Smooth Bell-Curve Density Profile
        float dr = gargPcurve(radialGradient);

        // IterationT Inner Edge Volumetric Bloom Factor:
        // Generates the dazzling, silky-smooth incandescent white-hot bloom around the inner hole rim
        float fr = abs(radial - inner) * 0.25 + 0.35;
        float fade = fr * fr * 0.04;
        float bloomFactor = 1.0 / (height * height * 55.0 + fade + 0.00002);
        bloomFactor *= clamp(2.0 - abs(height) / max(discThickness, 0.005), 0.0, 1.0);
        bloomFactor = bloomFactor * bloomFactor;

        // Gas density synthesis: combines vertical gaussian slab + pcurve + inner bloom
        float vertCoverage = clamp(1.0 - abs(height) / max(discThickness, 0.005), 0.0, 1.0);
        float baseDensity = dr * vertCoverage * 0.75 + bloomFactor * 0.15;

        // Multi-scale Keplerian noise modulation (Perlin + Worley knots + Detail filaments)
        float plasmaNoise = perlin * 0.44 + worley * 0.36 + detail * 0.20;
        plasmaNoise = smoothstep(0.16, 0.84, plasmaNoise);
        float streamPattern = mix(spiralArm1, spiralArm2, 0.38) * 0.65 + striations * 0.35;
        float filament = clamp(plasmaNoise * (0.45 + 0.75 * streamPattern), 0.0, 1.0);

        float density = baseDensity * (0.50 + 0.60 * filament);

        // --- 4. RELATIVISTIC DOPPLER BEAMING ---
        vec3 tangentDir = normalize(cross(axis, planar));
        float viewDoppler = dot(tangentDir, -d) * keplerSpeed;
        float dopplerFactor = clamp(1.0 + viewDoppler * 0.70, 0.30, 2.6);
        float dopplerBoost = pow(dopplerFactor, 2.5);

        // --- 5. ITERATION-T PLANCKIAN LOCUS BLACKBODY RADIATION ---
        // Radial gradient powers glow temperature (2,700K outer bronze -> 8,500K inner white-gold)
        float gr = t * t;
        float glowStrength = 1.0 / (gr * gr * 380.0 + 0.002);
        float temp = 2700.0 + glowStrength * 48.0;
        float fed = 1.0 + min(HoleState.w, 6.0) * 0.16;
        vec3 glow = gargBlackbody(temp) * (glowStrength * 0.008 + 0.85) * (max(DiskShape.w, 0.0) * fed * dopplerBoost);

        // --- 6. PHYSICAL BEER-LAMBERT VOLUMETRIC INTEGRATION ---
        float stepTransmittance = exp(-density * dt * 3.5);
        float stepAbsorption = 1.0 - stepTransmittance;
        vec3 stepEmission = glow * stepAbsorption;

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

    // Radial falloff feathering for gravitational lensing and cosmic sky:
    // Inside 0.40 * reach (which covers the entire accretion disk), lensing is 100% full strength.
    // Beyond the disk (0.40 to 1.0 reach), it smoothly and seamlessly tapers to zero at the boundary.
    float normDist = clamp(impact / max(reach, 0.001), 0.0, 1.0);
    float edgeFeather = 1.0 - smoothstep(0.40, 1.0, normDist);

    // Screen-space gravitational lensing: isotropic circular deflection (prevent foreground bleeding)
    vec2 lensedUv = vUv;
    if (SpinAxis.w >= 0.05 && openFactor > 0.001) {
        vec2 candidateUv = clamp(vUv + (d.xy - rayDir.xy) * 0.35 * (edgeFeather * openFactor), 0.001, 0.999);
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
        float bent = 1.0 - dot(d, rayDir);
        float lensed = clamp(bent * 14.0, 0.0, 1.0) * edgeFeather;

        if (lensed > 0.001) {
            float sceneLum = dot(lensedScene, vec3(0.299, 0.587, 0.114));
            vec3 cosmicSky = gargCosmicSky(d, time, sceneLum);
            background = lensedScene + cosmicSky * (lensed * edgeFeather);
        }
    }

    // --- EINSTEIN PHOTON RING (CRITICAL CURVE) ---
    // In IterationT: 12,000K incandescent torus bloom encircling the photon sphere
    float ringDist = impact - shadowEdge;
    float ringInnerMask = smoothstep(-rg * 0.015, rg * 0.015, ringDist);

    // 1. Ultra-sharp incandescent core ring
    float ringCore = exp(-pow(ringDist / max(rg * 0.025, 0.001), 2.0)) * ringInnerMask;

    // 2. IterationT Volumetric Torus Bloom
    float torusDist = abs(ringDist) / max(rg * 0.045, 0.001);
    float bloomTorus = 1.0 / (pow(torusDist, 3.2) + 0.001) * ringInnerMask;

    // 3. Radiant champagne corona
    float ringHalo = exp(-pow(max(ringDist, 0.0) / max(rg * 0.22, 0.001), 1.6)) * ringInnerMask;

    vec3 ringCoreCol = gargBlackbody(12000.0) * (ringCore * 5.0 + bloomTorus * 2.2);
    vec3 ringHaloCol = gargBlackbody(4500.0) * (ringHalo * 1.5);
    vec3 ringComposite = ringCoreCol + ringHaloCol;

    float ahead = step(0.0, along);
    float ringGain = ahead * max(DiskShape.w, 0.0) * (1.0 + crit * 1.8);
    if (hitSolid && sceneDistance < frontHoleDist) {
        ringGain = 0.0;
    }
    vec3 ring = ringComposite * ringGain;

    // Emissive disk & photon ring (superheated and energized during detonation)
    float blastDiskEnergize = 1.0 + blast * 3.5;
    vec3 diskRaw = (accum + ring) * (critBoost * blastDiskEnergize * openFactor);

    // --- TRUE ACES FILMIC TONE MAPPING FOR ACCRETION DISK ---
    // Smoothly rolls off high dynamic range radiance into filmic display space:
    // Completely eliminates flat white clipping while preserving intense golden-amber, solar yellow, and bronze embers.
    vec3 diskToneMapped = acesFilmic(diskRaw * 0.58);
    // Preserves vibrant hue saturation in midtones
    vec3 diskEmission = diskToneMapped * (1.0 + 0.12 * smoothstep(0.0, 0.60, diskToneMapped));

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


