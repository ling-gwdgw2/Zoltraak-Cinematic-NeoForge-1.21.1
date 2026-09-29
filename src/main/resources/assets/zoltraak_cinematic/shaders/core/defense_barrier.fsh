#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec3 worldPos;
in vec3 viewPosition;

out vec4 fragColor;

// --- Hexagonal Grid Distance Function ---
// Returns distance to nearest hexagon edge (0 at boundary, ~0.5 at center)
float hexEdgeDist(vec2 p) {
    p = abs(p);
    float d = max(dot(p, vec2(0.5, 0.8660254)), p.x);
    return 0.5 - d;
}

float hexCellGrid(vec2 uv, float scale, float lineWidth) {
    vec2 p = uv * scale;
    vec2 s = vec2(1.0, 1.7320508);
    vec2 h = s * 0.5;
    vec2 a = mod(p, s) - h;
    vec2 b = mod(p - h, s) - h;
    vec2 g = dot(a, a) < dot(b, b) ? a : b;
    float edge = hexEdgeDist(g);
    // Anti-aliased line width using screen-space derivative
    float w = max(fwidth(edge) * 1.5, lineWidth);
    return 1.0 - smoothstep(0.0, w, edge);
}

// Triplanar hexagonal micro-mesh projection
float getTriplanarHex(vec3 pos, vec3 norm, float scale, float lineWidth) {
    vec3 w = pow(abs(norm), vec3(4.0));
    float sum = w.x + w.y + w.z;
    if (sum < 0.0001) return 0.0;
    w /= sum;

    float h1 = hexCellGrid(pos.yz, scale, lineWidth);
    float h2 = hexCellGrid(pos.zx, scale, lineWidth);
    float h3 = hexCellGrid(pos.xy, scale, lineWidth);

    return w.x * h1 + w.y * h2 + w.z * h3;
}

void main() {
    if (vertexColor.a <= 0.003) {
        discard;
    }

    // 1. Surface normal in view space & camera direction
    vec3 viewNormal = normalize(cross(dFdx(viewPosition), dFdy(viewPosition)));
    vec3 viewDir = normalize(-viewPosition);
    float facing = abs(dot(viewNormal, viewDir));

    // 2. Fresnel edge glow (intense at silhouette edges, translucent in center)
    float fresnel = pow(1.0 - facing, 2.5);
    float rimGlow = pow(1.0 - facing, 5.0);

    // 3. World surface normal for triplanar projection
    vec3 worldNormal = normalize(cross(dFdx(worldPos), dFdy(worldPos)));

    // 4. Procedural Hexagonal Micro-Mesh (scale 2.8 gives crisp ~0.35m micro-tiles)
    float microHex = getTriplanarHex(worldPos, worldNormal, 2.8, 0.038);

    // 5. Mana Shimmer & Wave Propagation
    float t = GameTime * 2.5;
    // Harmonic vertical mana flow
    float verticalWave = sin(worldPos.y * 3.2 - t * 3.5);
    // Subtle cross-interference ripple
    float radialRipple = sin(length(worldPos.xz) * 4.0 - t * 4.0);
    float shimmer = 0.5 + 0.5 * (verticalWave * 0.6 + radialRipple * 0.4);

    // Impact overdrive detection: vertexColor.r increases when barrier is struck
    float hitFactor = smoothstep(0.15, 0.90, vertexColor.r);
    float shockwave = sin(length(worldPos) * 8.0 - t * 12.0) * hitFactor;

    // 6. Chromatic Energy Color Palette (Frieren Anime Style)
    // Deep mana base: electric sapphire & translucent cyan
    vec3 deepCyan = vec3(0.04, 0.45, 0.95);
    // Active plate color from CPU vertex (includes pulse tiles & base tint)
    vec3 baseColor = mix(deepCyan, vertexColor.rgb, 0.75);

    // Micro-hex lines: glowing neon aqua
    vec3 gridColor = vec3(0.35, 0.92, 1.0);

    // Fresnel rim: iridescent chromatic dispersion (deep cyan -> incandescent white-blue)
    vec3 rimColor = mix(vec3(0.12, 0.78, 1.0), vec3(0.92, 0.98, 1.0), fresnel);

    // Combine color layers
    vec3 color = baseColor * (0.85 + 0.25 * shimmer);
    // Add micro-mesh glow (stronger on angled edges)
    color += gridColor * microHex * (0.40 + 0.60 * fresnel);
    // Add brilliant Fresnel rim luminescence
    color += rimColor * (fresnel * 0.70 + rimGlow * 0.50);
    // Add impact shockwave surge
    color += vec3(0.8, 0.95, 1.0) * max(0.0, shockwave * 0.4);

    // 7. Dynamic Alpha calculation:
    // Core of barrier is semi-translucent so player can see incoming spells/threats
    // Edges and micro-hex lines are luminous and clearly defined
    float alpha = vertexColor.a * (0.28 + 0.72 * fresnel + 0.35 * microHex);
    // Add extra opacity on impact hit
    alpha = clamp(alpha + hitFactor * 0.25, 0.0, 1.0);

    alpha *= ColorModulator.a;
    color *= ColorModulator.rgb;

    if (alpha <= 0.003) {
        discard;
    }

    fragColor = vec4(color, alpha);
}
