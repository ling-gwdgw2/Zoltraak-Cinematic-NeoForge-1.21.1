#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 uv;
out vec3 viewPosition;
out float age;
out float energy;
out float span;
out float halfWidth;
void main(){viewPosition=(ModelViewMat*vec4(Position,1)).xyz;gl_Position=ProjMat*ModelViewMat*vec4(Position,1);uv=UV0;age=Color.r*255.0;energy=Color.a;span=max(Color.g*128.0,.01);halfWidth=Color.b*16.0;}
