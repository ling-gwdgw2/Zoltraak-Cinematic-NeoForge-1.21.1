#version 150
in vec2 uv;
in vec3 viewPosition;
in float age;
in float energy;
in float span;
in float halfWidth;
out vec4 fragColor;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float noise(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);}
float fbm(vec2 p){return noise(p)*.57+noise(p*2.07)*.28+noise(p*4.31)*.15;}
void main(){
 bool volume=uv.y>=2.0;
 float x=uv.x,y=volume?sin((uv.y-2.0)*6.2831853)*.28:uv.y*2.0-1.0;
 vec3 surfaceNormal=normalize(cross(dFdx(viewPosition),dFdy(viewPosition)));
 float facing=abs(dot(surfaceNormal,normalize(-viewPosition)));
 // Never extend the rounded source behind the magic circle.
 if(x<0.0)discard;
 float grain=fbm(vec2(x*24.0-age*.8,y*9.0+age*.21));
 float flow=fbm(vec2(x*31.0-age*.92,y*5.0+grain*1.8));
 float taper=1.0;
 float width=(.255+.105*(1.0-smoothstep(.01,.20,clamp(x,0.0,1.0)))+.035*smoothstep(.55,1.0,clamp(x,0.0,1.0))+.014*(flow-.5)+.004*sin(x*41.0-age*.9))*max(.09,taper);
 float cap=max((x-1.0)*span,0.0)/max(halfWidth,.01);
 float d=length(vec2(cap,y))/max(width,.008);
 if(volume)d=1.065-.74*pow(facing,.65);
 float core=1.0-smoothstep(.87,1.04,d);
 float fiber=pow(noise(vec2(x*240.0-age*4.0,y*52.0+flow*3.0)),5.0);
 vec3 soot=mix(vec3(.008,.004,.014),vec3(.034,.014,.020),grain*grain);
 soot+=vec3(.052,.04,.066)*fiber;
 float seam=pow(max(0.0,sin(y*43.0+flow*11.0+x*16.0-age*.7)),20.0)*.035;
 soot+=vec3(.65,.5,.8)*seam;
 // A broad granular boundary, not a smooth luminous outline. Derivative-sized
 // cells keep individual grains visible both close up and at long range.
 vec2 cellSize=max(fwidth(vec2(x,y))*1.55,vec2(1.0/1600.0,1.0/360.0));
 vec2 dustUV=vec2(x-age*.011,y+age*.008)/cellSize;
 vec2 cell=floor(dustUV),local=fract(dustUV)-.5;
 float seed=hash(cell);
 float dotShape=1.0-smoothstep(.16,.49,length(local+vec2(seed-.5,hash(cell+19.0)-.5)*.35));
 float cluster=noise(vec2(x*211.0-age*2.0,y*49.0));
 float band=smoothstep(.95,1.01,d)*(1.0-smoothstep(1.035,1.16,d));
 float grainMask=smoothstep(.28,.72,seed+cluster*.26)*dotShape;
 float sparkle=band*grainMask;
 float fine=noise(vec2(x*1200.0-age*7.0,y*270.0));
 float grit=band*smoothstep(.54,.82,fine)*.4;
 float rim=exp(-pow((d-1.015)*30.0,2.0));
 float smoke=exp(-max(0.0,d-1.0)*30.0)*(1.0-core)*(.03+.05*flow);
 float chips=clamp(sparkle+grit,0.0,1.0);
 vec3 base=mix(soot,vec3(.21,.10,.34),rim*.65);
 vec3 powder=mix(vec3(.66,.60,.95),vec3(.98,1.0,1.0),smoothstep(.34,.8,seed));
 vec3 color=mix(base,powder,smoothstep(.07,.63,chips));
 if(volume)color+=vec3(.016,.01,.025)*pow(facing,2.0);
 float alpha=max(core,max(smoke+rim*.17,chips*.98))*energy;

 if(!volume)alpha*=1.0-core;
 if(alpha<.003)discard;
 fragColor=vec4(color,clamp(alpha,0.0,1.0));
}
