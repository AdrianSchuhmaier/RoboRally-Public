#version 330 core

in vec3 pass_position;
in vec3 pass_normal;
in vec2 pass_texCoords;
in mat4 pass_invMatrix;

layout (location = 0) out vec4 color;
layout (location = 1) out vec4 glow;

layout (std140) uniform Dimensions
{
	vec2 dimensions;
};

uniform sampler2D tex;

#include fresnel.glsl
#include fragViewDir.glsl
#include luminance.glsl

void main()
{
	vec3 fragViewDir = normalize(fragViewDir(pass_invMatrix));
	
	color = texture(tex, pass_texCoords);
	float a = color.a;

	color.a = 1.;
	glow.x = color.a * fresnelFactor(fragViewDir, pass_normal, .5);
	glow.z = 1.;
	glow.w = 1.;
}
