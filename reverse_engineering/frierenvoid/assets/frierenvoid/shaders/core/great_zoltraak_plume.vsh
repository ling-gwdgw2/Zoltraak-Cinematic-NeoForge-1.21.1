#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 uv;
out float age;
out float energy;
out float dark;
out float luminous;
void main(){gl_Position=ProjMat*ModelViewMat*vec4(Position,1);uv=UV0;age=Color.r*255.0;energy=Color.a;dark=Color.g;luminous=Color.b;}
