#version 150

uniform sampler2D Sampler0;      // 512x512 zoltraak_magic_circle.png
uniform float GameTime;
uniform float VortexSpeed;
uniform vec4 CoreColor;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 vertexNormal;

out vec4 fragColor;

void main() {
    // Center UV at (0, 0)
    vec2 centeredUV = texCoord0 - vec2(0.5);
    float radius = length(centeredUV);

    if (radius > 0.5) {
        discard;
    }

    // Convert to Polar Coordinates (r, theta)
    float theta = atan(centeredUV.y, centeredUV.x);

    // Dynamic spiral twirl: inner region spins faster, creating a fluid whirlpool effect
    float speed = (VortexSpeed != 0.0) ? VortexSpeed : 2.5;
    float time = GameTime * 1200.0 * speed;
    float twirl = (1.0 - smoothstep(0.0, 0.5, radius)) * 4.71239; // 1.5 * PI spiral
    float spiralTheta = theta + twirl - time;

    // Convert back to distorted Cartesian coordinates
    vec2 distortedUV = vec2(cos(spiralTheta), sin(spiralTheta)) * radius + vec2(0.5);

    // Sample primary magic circle texture with distorted UV
    vec4 circleTex = texture(Sampler0, distortedUV);

    // Secondary reverse-spin resonance sample for energy interference patterns
    float counterTheta = theta - twirl * 0.5 + time * 0.4;
    vec2 counterUV = vec2(cos(counterTheta), sin(counterTheta)) * radius + vec2(0.5);
    vec4 counterTex = texture(Sampler0, counterUV);

    // Radial energy pulsation
    float pulse = 0.85 + 0.15 * sin(radius * 30.0 - time * 3.0);

    // Blend primary runes with fluid swirl
    vec4 runeColor = mix(circleTex, counterTex, 0.35);

    // Color gradient: Pure white glowing core transitioning to vibrant celestial cyan
    vec3 baseColor = (CoreColor.rgb != vec3(0.0)) ? CoreColor.rgb : vec3(0.0, 0.898, 1.0);
    vec3 energyColor = mix(vec3(1.0, 1.0, 1.0), baseColor, smoothstep(0.05, 0.35, radius));

    // Outer edge soft circular fade
    float edgeAlpha = smoothstep(0.5, 0.44, radius);

    float alpha = runeColor.a * vertexColor.a * edgeAlpha * pulse;
    vec3 emissiveRgb = runeColor.rgb * energyColor * 1.8;

    fragColor = vec4(emissiveRgb, alpha);
}
