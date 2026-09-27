#version 150
uniform sampler2D SceneSampler;
uniform vec2 Viewport;
in vec2 uv;
in float age;
out vec4 fragColor;
void main(){
 vec2 p=(uv-.5)*5.2;
 float r=length(p);
 if(r<.98 || r>2.55)discard;
 vec2 screen=gl_FragCoord.xy/Viewport;
 // Derivatives convert orbital coordinates into actual framebuffer pixels at any FOV.
 vec2 pixelScale=vec2(1.0/max(abs(dFdx(p.x)),.00001),1.0/max(abs(dFdy(p.y)),.00001))/Viewport;
 float fall=1.0-smoothstep(1.05,2.5,r);
 float bending=.14*fall/max(r-.75,.2);
 vec2 offset=normalize(p)*pixelScale*bending;
 // Uneven travelling waves evoke the warped clothing and air around the hand.
 offset+=pixelScale*vec2(sin(p.y*14.0-age*.24),cos(p.x*11.0+age*.18))*.035*fall;
 float chroma=.025*fall;
 vec2 lo=vec2(.001),hi=vec2(.999);
 vec3 col;
 col.r=texture(SceneSampler,clamp(screen-offset*(1.0+chroma),lo,hi)).r;
 col.g=texture(SceneSampler,clamp(screen-offset,lo,hi)).g;
 col.b=texture(SceneSampler,clamp(screen-offset*(1.0-chroma),lo,hi)).b;
 // A narrow gravitational shadow makes the luminous photon ring readable in daylight.
 col*=1.0-.24*exp(-abs(r-1.02)*9.0);
 fragColor=vec4(col,fall);
}
