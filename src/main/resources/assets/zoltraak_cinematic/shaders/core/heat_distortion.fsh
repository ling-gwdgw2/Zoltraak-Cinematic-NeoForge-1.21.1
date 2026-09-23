#version 150

uniform sampler2D Sampler0;      // Atlas / noise texture
uniform sampler2D SceneColor;    // Screen background captured by SceneCaptureHandler
uniform float GameTime;
uniform float DistortionStrength;

in vec4 vertexColor;
in vec2 texCoord0;
in vec4 screenPos;
in vec3 vertexNormal;

out vec4 fragColor;

// Procedural multi-frequency sine wave distortion
vec2 calculateDistortionOffset(vec2 uv, float time) {
    float wave1 = sin(uv.y * 35.0 - time * 6.0 + uv.x * 12.0);
    float wave2 = cos(uv.y * 55.0 + time * 9.0 - uv.x * 20.0);
    float wave3 = sin((uv.x + uv.y) * 25.0 - time * 4.0);
    return vec2(wave1 * 0.6 + wave3 * 0.4, wave2 * 0.7 + wave1 * 0.3);
}

void main() {
    // Reconstruct normalized screen UV [0, 1]
    vec2 screenUV = (screenPos.xy / screenPos.w) * 0.5 + 0.5;

    // Edge falloff: Smoothly fade out distortion at cylinder edges to prevent seam artifacts
    float edgeFadeY = smoothstep(0.0, 0.15, texCoord0.y) * smoothstep(1.0, 0.85, texCoord0.y);
    float edgeFadeX = smoothstep(0.0, 0.25, texCoord0.x) * smoothstep(1.0, 0.75, texCoord0.x);
    float fade = edgeFadeX * edgeFadeY * vertexColor.a;

    float strength = (DistortionStrength > 0.0 ? DistortionStrength : 0.035) * fade;

    // Calculate dynamic refraction offset
    vec2 offset = calculateDistortionOffset(texCoord0, GameTime * 1200.0) * strength;
    vec2 refractedUV = clamp(screenUV + offset, 0.001, 0.999);

    // Sample warped background color
    vec4 sceneBg = texture(SceneColor, refractedUV);

    // If scene capture is transparent or unavailable, blend with noise texture
    vec4 noiseSample = texture(Sampler0, texCoord0 + offset * 5.0);

    // Subtle chromatic aberration / optical flare at distortion peaks
    vec3 heatTint = vec3(0.15, 0.75, 1.0) * length(offset) * 12.0;

    vec3 finalRgb = mix(sceneBg.rgb + heatTint, noiseSample.rgb, 0.05);
    fragColor = vec4(finalRgb, fade * 0.85);
}
