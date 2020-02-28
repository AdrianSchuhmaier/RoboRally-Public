#version 330 core

in vec2 pass_texCoords;
in mat4 pass_invMatrix;

layout (location = 0) out vec4 color;
layout (location = 1) out vec4 glow;

layout (std140) uniform Dimensions
{
	vec2 dimensions;
};


float random(vec2 ab)
{
	float f = (cos(dot(ab ,vec2(21.9898,78.233))) * 758.5453);
	return fract(f);
}

float noise(in vec2 xy)
{
	vec2 ij = floor(xy);
	vec2 uv = xy-ij;
	uv = uv*uv*(3.0-2.0*uv);


	float a = random(vec2(ij.x, ij.y ));
	float b = random(vec2(ij.x+1., ij.y));
	float c = random(vec2(ij.x, ij.y+1.));
	float d = random(vec2(ij.x+1., ij.y+1.));
	float k0 = a;
	float k1 = b-a;
	float k2 = c-a;
	float k3 = a-b-c+d;
	float kk = (k0 + k1*uv.x + k2*uv.y + k3*uv.x*uv.y);
	return kk;
}

vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }

float snoise(vec2 v){
  const vec4 C = vec4(0.211324865405187, 0.366025403784439,
           -0.577350269189626, 0.024390243902439);
  vec2 i  = floor(v + dot(v, C.yy) );
  vec2 x0 = v -   i + dot(i, C.xx);
  vec2 i1;
  i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod(i, 289.0);
  vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
  + i.x + vec3(0.0, i1.x, 1.0 ));
  vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy),
    dot(x12.zw,x12.zw)), 0.0);
  m = m*m ;
  m = m*m ;
  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );
  vec3 g;
  g.x  = a0.x  * x0.x  + h.x  * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}

void main()
{
	vec2 uv = 2. * vec2(gl_FragCoord.x / dimensions.x, gl_FragCoord.y / dimensions.y) - vec2(1.);

	vec3 fragViewDir = (pass_invMatrix * vec4(uv, -1., 1.)).xyz;
	fragViewDir = normalize(fragViewDir);

	float m = 2. * sqrt(
				pow(fragViewDir.x, 2.) +
				pow(fragViewDir.y + 1., 2.) +
				pow(fragViewDir.z, 2.));

	color.rgb = vec3( (1.-fragViewDir.y-.1) * (1.-fragViewDir.y) );

    color.rgb += pow(noise(1000*(fragViewDir.xz / m + .5)), 120.0) * 20.0;

    color.a = 1.;

    glow.x = 0.;
    glow.z = 0.;
    glow.a = 1.;
}
