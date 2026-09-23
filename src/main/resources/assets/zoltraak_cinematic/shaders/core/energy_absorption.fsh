#version 150

uniform float GameTime;
uniform float FresnelPower;
uniform vec4 RimColor;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewNormal;
in vec3 viewDirection;

out vec4 fragColor;

void main() {
    vec3 N = normalize(viewNormal);
    vec3 V = normalize(viewDirection);

    // Fresnel incidence angle
    float NdotV = max(dot(N, V), 0.0);
    float power = (FresnelPower > 0.0) ? FresnelPower : 3.2;
    float fresnel = pow(1.0 - NdotV, power);

    // Dark absorption core (event-horizon): Mana is so condensed it devours incoming light
    float absorption = pow(NdotV, 1.8);
    vec3 coreColor = vec3(0.01, 0.03, 0.08) * (1.0 - fresnel);

    // Rim energy emission: Brilliant celestial photon glow
    vec3 rimTint = (RimColor.rgb != vec3(0.0)) ? RimColor.rgb : vec3(0.2, 0.85, 1.0);
    vec3 rimEmission = rimTint * fresnel * 2.5;

    // Atmospheric axial wave shimmer
    float shimmer = 0.9 + 0.1 * sin(texCoord0.y * 50.0 - GameTime * 1200.0 * 5.0);

    vec3 finalColor = (coreColor + rimEmission) * shimmer;
    float alpha = clamp(absorption * 0.4 + fresnel * 1.2, 0.0, 1.0) * vertexColor.a;

    fragColor = vec4(finalColor, alpha);
}
