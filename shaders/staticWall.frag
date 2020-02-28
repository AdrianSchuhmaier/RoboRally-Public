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

#include luminance.glsl

void main()
{
	float factor = dot(pass_normal, normalize(vec3(1, 2, 1)));
	factor = factor/2. + .75;
	color.rgb = vec3(factor);
	color.a = 1.;
	glow.x = 1.;
	glow.y = luminance(color.rgb) - 0.9;
	glow.z = 0.;
	glow.w = 1.;
}
