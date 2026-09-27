#version 150
in vec2 uv;
in float age;
in float energy;
out vec4 fragColor;
void main(){
 float x=uv.x,y=abs(uv.y*2.0-1.0);
 float ripple=sin(x*190.0-age*4.7)*sin(x*59.0+age*2.1);
 float taper=sqrt(clamp((1.0-x)*35.0,0.0,1.0));
 float width=(0.16+0.012*ripple)*max(0.08,taper);
 float core=1.0-smoothstep(width*.7,width,y);
 float edge=exp(-pow(y/max(width*1.8,.01),2.0))*0.65;
 float halo=exp(-y*y*9.0)*.13;
 float streak=pow(max(0.0,sin(x*170.0-age*9.0+y*36.0)),12.0)*exp(-y*7.0)*.11;
 vec3 color=mix(vec3(.38,.64,1.0),vec3(1.0,.99,1.0),core);
 float alpha=(core+edge+halo+streak)*energy*smoothstep(0.0,.006,x)*(1.0-smoothstep(.985,1.0,x));
 fragColor=vec4(color,clamp(alpha,0.0,1.0));
}
