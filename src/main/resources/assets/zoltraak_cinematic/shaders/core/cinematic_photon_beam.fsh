#version 150

in vec2 uv;
in float age;
in float energy;
in float span;
in float halfWidth;

out vec4 fragColor;

void main() {
    float x = uv.x;
    float y = abs(uv.y * 2.0 - 1.0);

    // World-space coordinates in blocks / meters:
    float distBase = x * span;            // Distance from magic circle plane (meters)
    float distTip  = (1.0 - x) * span;     // Distance from beam tip wavefront (meters)
    float distY    = y * halfWidth;       // Radial distance from beam centerline (meters)

    // 1. Conical Arcane Muzzle Flare (where beam erupts from magic circle runes):
    // Smoothly expands in the first 2.5 meters from the magic circle plane to seamlessly envelope the runes
    float muzzleFlare = exp(-distBase / 1.6) * 0.95;
    float beamScale = 1.0 + muzzleFlare;
    float scaledDistY = distY / beamScale;

    // Smooth base transition starting precisely at magic circle plane (0.00 to 0.06m)
    float baseFade = smoothstep(0.0, 0.06, distBase);

    // 2. Aerodynamic Supersonic Bow Shock Dome Cap (1:1 Anime Wavefront):
    // Smoothly rounds the entire beam wavefront (core, rim, and aura) into an aerodynamic plasma dome!
    // Eliminates the flat guillotine pipe cut and completely prevents any white nub protrusion!
    float capRadius = min(halfWidth * beamScale, max(0.4, span * 0.45));
    float dxDome = max(0.0, capRadius - distTip);
    float dyDome = scaledDistY;
    float domeDistance = sqrt(dxDome * dxDome + dyDome * dyDome);
    float headCap = 1.0 - smoothstep(capRadius * 0.80, capRadius, domeDistance);

    // 3. High-Frequency Supersonic Longitudinal Ionization Wave (Anime kinetic mana pulses)
    float waveA = sin(distBase * 3.5 - age * 15.0);
    float waveB = cos(distBase * 1.4 + age * 6.5);
    float ionization = (waveA * 0.6 + waveB * 0.4) * (0.018 * halfWidth);

    // 4. Searing Super-Gaussian Plasma Core (1:1 Anime Pure White Light)
    // Steep drop-off power 6 into the electric cyan band
    float coreRadius = (0.32 * halfWidth + ionization);
    float core = exp(-pow(scaledDistY / max(coreRadius, 0.005), 6.0));

    // 5. Electric Cyan Ionized Energy Rim (~32% to 54% beam radius)
    // Hugs the white core with intense neon cyan luminescence
    float rimCenter = 0.44 * halfWidth;
    float rimWidth  = 0.16 * halfWidth;
    float rimDist   = abs(scaledDistY - rimCenter) / max(rimWidth, 0.01);
    float rim       = exp(-rimDist * rimDist * 3.8);

    // 6. Deep Royal Indigo / Midnight Ultramarine Outer Sheath (50% to 92% beam radius)
    // High-saturation blue/indigo radiance envelope (Zero-Red prevents purple drift)
    float sheathDist = max(0.0, scaledDistY - 0.36 * halfWidth);
    float sheath = exp(-pow(sheathDist / max(0.40 * halfWidth, 0.01), 2.0));

    // 7. High-velocity longitudinal discharge filaments
    float filament = pow(max(0.0, sin(distBase * 2.8 - age * 16.0 + (scaledDistY / max(halfWidth, 0.01)) * 16.0)), 10.0)
                   * smoothstep(0.28 * halfWidth, 0.48 * halfWidth, scaledDistY)
                   * exp(-scaledDistY * (2.2 / max(halfWidth, 0.01))) * 0.40;

    // 8. Exact 1:1 Anime Additive Color Palette:
    // - Overbright White Core (>1.0 to trigger intense HDR bloom)
    // - Intense Electric Cyan
    // - High-saturation Royal Cobalt / Ultramarine Blue
    vec3 whiteHotCore  = vec3(1.40, 1.40, 1.40); // Overbright pure white photon core
    vec3 electricCyan  = vec3(0.20, 0.92, 1.50); // Intense glowing cyan
    vec3 royalIndigo   = vec3(0.06, 0.30, 2.00); // High-saturation royal indigo/ultramarine
    vec3 deepArcane    = vec3(0.03, 0.14, 1.25); // Deep cobalt aura

    // Composite emissive layers
    vec3 outerGlow = mix(deepArcane, royalIndigo, clamp(sheath * 1.3, 0.0, 1.0));
    vec3 color = mix(outerGlow, electricCyan, clamp(rim * 1.5 + filament, 0.0, 1.0));
    color = mix(color, whiteHotCore, clamp(core * 1.30, 0.0, 1.0));

    // 9. Soft outer boundary edge falloff
    float edgeCut = 1.0 - smoothstep(halfWidth * 0.85, halfWidth, scaledDistY);
    float alpha = (core * 1.40 + rim * 1.20 + sheath * 0.95 + filament)
                * energy * baseFade * headCap * edgeCut;

    if (alpha < 0.003) discard;

    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
