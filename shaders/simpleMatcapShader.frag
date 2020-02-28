#version 330 core

in vec3 pass_position;
in vec3 pass_normal;
in vec2 pass_texCoords;
in mat4 pass_invMatrix;

layout (location = 0) out vec4 color;
layout (location = 1) out vec4 glow;

layout (std140) uniform Matrices
{
	mat4 viewMatrix;
	mat4 projectionMatrix;
};

layout (std140) uniform Dimensions
{
	vec2 dimensions;
};

uniform sampler2D tex;
uniform sampler2D tex2;

#include fresnel.glsl
#include luminance.glsl

vec2 matcap(vec3 eye, vec3 normal) {
  vec3 reflected = reflect(eye, normal);
  reflected = normalize(reflected);
  reflected = reflected * .5 + .5;
  reflected.y = -reflected.y;
  return reflected.xy;
}

void main()
{
	vec2 uv = 2. * vec2(gl_FragCoord.x / dimensions.x, gl_FragCoord.y / dimensions.y) - vec2(1.);
	vec3 fragViewDir = (pass_invMatrix * vec4(uv, -1., 1.)).xyz;
	fragViewDir = normalize(fragViewDir);

	vec3 cameraNormal = (viewMatrix * vec4(pass_normal, 0.)).xyz;
	vec3 cameraViewDir = (viewMatrix * vec4(fragViewDir, 0.)).xyz;

	color.rgb = texture(tex, matcap(cameraViewDir, cameraNormal)).rgb;
	vec3 altColor = texture(tex2, pass_texCoords).rgb;

	float factor = dot(pass_normal, normalize(vec3(1, 2, 1)));
	factor = factor/2. + .75;

	if (altColor != vec3(1.) && altColor != vec3(0.)) {
		color.rgb = altColor;
	}

	color.rgb *= factor;

	color.a = 1.;
	glow.x = fresnelFactorNB(fragViewDir, pass_normal);
	glow.y = luminance(color.rgb) - 0.9;
	glow.z = 0.;
	glow.w = 1.;
}
