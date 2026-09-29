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

    // 1. Expanding High-Energy Shockwave Wavefront Ring
    float shockRadius = 0.72;
    float wave = exp(-pow((r - shockRadius) * 14.0, 2.0));

    // 2. Thermal Heat Wave Distortion & Secondary Trailing Ripples
    float ripple = sin(r * 28.0 - t * 14.0) * exp(-pow(r * 2.0, 2.0)) * 0.32;

    // 3. Center Muzzle Plasma Eruption Burst
    float centerBurst = exp(-r * 7.5) * 1.15;

    // 4. Sample Texture Mask
    float texLuma = texture(Sampler0, uv).r;

    // Combined explosive shockwave density
    float density = max(wave * 1.35 + ripple + centerBurst, texLuma * (1.0 + wave * 0.8));

    // 5. Chromatic Shockwave Palette (Theme-aware: Cyan, Amber/Gold, Violet)
    vec3 baseTheme = vertexColor.rgb;
    vec3 whiteHot = vec3(1.0, 0.99, 0.94);

    // Wavefront shifts to white-hot at the sharpest pressure edge
    vec3 color = mix(baseTheme, whiteHot, smoothstep(0.48, 0.92, density));

    // 6. Alpha & Output
    float alpha = density * vertexColor.a * ColorModulator.a;
    color *= ColorModulator.rgb;

    if (alpha <= 0.003) {
        discard;
    }

    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
