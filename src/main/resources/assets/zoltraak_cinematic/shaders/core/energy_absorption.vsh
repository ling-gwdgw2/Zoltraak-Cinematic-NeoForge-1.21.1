#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;
out vec3 viewNormal;
out vec3 viewDirection;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;
    vertexColor = Color;
    texCoord0 = UV0;

    // View direction in view space (pointing towards camera from vertex)
    viewDirection = -normalize(viewPos.xyz);

    // Transform normal to view space
    mat3 normalMatrix = transpose(inverse(mat3(ModelViewMat)));
    viewNormal = normalize(normalMatrix * Normal);
}
