#version 150
in vec2 uv;
in float age;
out vec4 fragColor;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float noise(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);}
void main(){
 if(age>=200.){
  vec2 q=(uv-.5)*2.;float r=length(q),a=atan(q.y,q.x),life=1.-smoothstep(200.,224.,age);
  float petals=pow(.5+.5*cos(a*7.+sin(a*3.)*.6),18.);
  float star=exp(-r*7.)*3.+petals*exp(-r*2.8)*1.2;
  float edge=exp(-abs(r-.28)*35.)*.5;
  vec3 c=mix(vec3(.52,.08,1.),vec3(1.,.85,1.),exp(-r*6.));
  fragColor=vec4(c*(star+edge)*life,1.-smoothstep(.7,1.,r));return;
 }
 float growth=smoothstep(24.,118.,age),span=2.8+9.2*growth;
 vec2 p=(uv-.5)*vec2(span*2.,5.2);float r=length(p),a=atan(p.y,p.x),t=age*.035;
 if(r<.99)discard;
 float n=noise(vec2(p.x*6.-t*4.,p.y*18.+sin(p.x*4.-t)*.7));
 float grain=noise(p*95.+vec2(t*9.,0.));
 float eyeWidth=.13+.45*exp(-p.x*p.x*.035);
 float eye=exp(-pow(abs(p.y)/eyeWidth,2.))*exp(-abs(p.x)*.10);
 float filament=exp(-abs(p.y-sin(p.x*2.-t*4.)*.026)*70.)*exp(-abs(p.x)*.085);
 float outer=exp(-abs(p.y)/(.25+eyeWidth))*exp(-abs(p.x)*.55)*.22;
 float rim=exp(-abs(r-1.025)*72.);
 float arc=exp(-abs(r-1.10-(n-.5)*.035)*35.)*(.45+.55*sin(a+t)*sin(a+t));
 vec3 red=vec3(1.,.035,.003),gold=vec3(1.,.58,.008),hot=vec3(1.,.9,.12);
 vec3 c=mix(red,gold,pow(clamp(eye,0.,1.),.65))*eye*(.75+.35*n+.22*grain);
 c+=red*outer+gold*filament*.52+hot*rim*.72+gold*arc*.17;
 float fade=(1.-smoothstep(span*.78,span*.995,abs(p.x)))*(1.-smoothstep(1.8,2.55,abs(p.y)));
 fragColor=vec4(c,fade);
}
