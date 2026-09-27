#version 150
in vec2 uv;
in float age;
in float energy;
in float dark;
in float luminous;
out vec4 fragColor;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float noise(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);}
void main(){
 float x=uv.x,y=abs(uv.y*2.0-1.0);
 float flow=noise(vec2(x*43.0-age*.55,y*9.0));
 float d=y+.055*(flow-.5)+.018*sin(x*180.0-age*1.2);
 float attached=1.0-step(.1,abs(luminous-.5));
 float edgeLight=mix(luminous,smoothstep(.18,.48,x),attached);
 float core=1.0-smoothstep(.68,.80,d);
 float cyan=exp(-pow((d-.78)*12.0,2.0));
 float white=exp(-pow((d-.77)*39.0,2.0));
 vec2 cell=max(fwidth(uv)*1.3,vec2(.0008));
 float grit=step(.56,hash(floor(uv/cell)+floor(age*.4)))*smoothstep(.67,.77,d)*(1.0-smoothstep(.82,.98,d));
 float crack=pow(max(0.0,sin(x*37.0+y*57.0+flow*11.0-age)),18.0)*.18;
 vec3 ink=vec3(.004,.006,.018)+vec3(.015,.035,.10)*flow*flow+vec3(.05,.24,.6)*crack*edgeLight;
 vec3 body=mix(vec3(.92,.98,1),ink,dark);
 vec3 color=mix(body,vec3(.08,.53,1),cyan*edgeLight);
 color=mix(color,vec3(.75,1,1),max(white,grit*.92)*edgeLight);
 float alpha=max(core,max(cyan*.8,grit)*edgeLight)*energy;
 alpha*=mix(1.0,smoothstep(0.0,.15,x),attached);
 if(alpha<.003)discard;
 fragColor=vec4(color,alpha);
}
