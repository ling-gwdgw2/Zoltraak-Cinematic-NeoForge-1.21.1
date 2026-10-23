#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 uv;
in vec4 vertexColor;
in vec3 viewPosition;

out vec4 fragColor;

void main() {
    // Normalizes sub-atlas UV [0.50, 0.75] or direct UV [0, 1] into centered [-1, 1]
    vec2 subUV = fract(uv * 4.0);
    vec2 p = subUV * 2.0 - 1.0;
    float r = length(p);
    float t = GameTime * 2.5;

    // 1. High-Velocity Staccato Supersonic Shockwave Ring
    // Sharp, condensed compression wavefront crest matching Fern's rapid fire
    float shockRadius = 0.74;
    float ringDist = abs(r - shockRadius);
    float shockPeak = exp(-pow(ringDist * 28.0, 2.0)) * 1.75;

    // 2. Transonic Condensation Disc & Trailing Mana Echoes
    float condensationDisc = exp(-pow(r * 3.6, 2.0)) * 0.55;
    float microRipples = sin(r * 36.0 - t * 16.0) * exp(-pow(r * 2.5, 2.0)) * 0.22;

    // 3. Center Muzzle Plasma Eruption Pop
    float corePop = exp(-r * 11.5) * 1.85;

    // 4. Sample Texture Mask (Multiply by texture alpha to avoid rectangular edge leakage)
    vec4 texSample = texture(Sampler0, uv);
    float texLuma = texSample.r * texSample.a;

    // Combined staccato shockwave density
    float density = max(shockPeak + condensationDisc + microRipples + corePop, texLuma * (1.1 + shockPeak * 0.9));

    // Smooth sub-pixel circular boundary preventing quad edge cutoffs
    float boundary = 1.0 - smoothstep(0.86, 0.98, r);
    density *= boundary;

    // 5. Chromatic Shockwave Palette (Theme-aware: Cyan or Muted Ash Violet)
    vec3 baseTheme = vertexColor.rgb;
    bool isDarkTheme = (baseTheme.g < 0.35 && baseTheme.r < 0.45);
    vec3 crestColor = isDarkTheme ? vec3(0.28, 0.18, 0.36) : vec3(1.0, 1.0, 1.0);

    // Wavefront crest shifts to peak pressure color
    float crestMix = clamp(shockPeak * 0.85 + corePop * 0.75, 0.0, 1.0);
    vec3 color = mix(baseTheme, crestColor, crestMix);

    // 6. Alpha & Output
    float alpha = density * vertexColor.a * ColorModulator.a;
    color *= ColorModulator.rgb;

    if (alpha <= 0.003) {
        discard;
    }

    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}

