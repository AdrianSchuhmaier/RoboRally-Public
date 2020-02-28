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
uniform vec3 playerColor;
uniform bool isReflection;
uniform bool isPreview;

#include fresnel.glsl
#include luminance.glsl

vec2 matcap(vec3 eye, vec3 normal) {
	vec3 reflected = reflect(eye, normal);
	reflected = normalize(reflected);
	reflected = reflected * .5 + .5;
	return reflected.xy;
}

void main() {
	float yCut;
	if (isReflection) {
		yCut = 0.;
	} else {
		yCut = -10.;
	}
	if (pass_position.y < yCut) {
		discard;
	}
	vec2 uv = 2.
			* vec2(gl_FragCoord.x / dimensions.x, gl_FragCoord.y / dimensions.y)
			- vec2(1.);
	vec3 fragViewDir = (pass_invMatrix * vec4(uv, -1., 1.)).xyz;
	fragViewDir = normalize(fragViewDir);

	vec3 cameraNormal = (viewMatrix * vec4(pass_normal, 0.)).xyz;
	vec3 cameraViewDir = (viewMatrix * vec4(fragViewDir, 0.)).xyz;

	vec3 matCapColor = texture(tex2, matcap(cameraViewDir, cameraNormal)).rgb;

	float factor = dot(pass_normal, normalize(vec3(1, 2, 1)));
	factor = factor / 2. + 1.;

	matCapColor = sqrt(matCapColor);
	color.rgb = texture(tex, pass_texCoords).rgb;
	if (color.rgb == vec3(1., 0., 0.)) {
		color.rgb = playerColor * factor;
	}
	color.rgb = mix(matCapColor, matCapColor * color.rgb, 0.8);

	if(isPreview){
		color.a = 0.7;
	} else {
		color.a = 1.;
	}
	glow.x = 1.;
	glow.y = luminance(color.rgb) - 0.9;
	glow.z = isPreview ? 1. : 0.;
	glow.w = 1.;
}
