#version 330 core

in mat4 pass_invMatrix;
in vec3 pass_laserDir;
in vec3 pass_normal;
in vec2 pass_texCoords;

layout (location = 0) out vec4 color;
layout (location = 1) out vec4 glow;

layout (std140) uniform Dimensions
{
	vec2 dimensions;
};

uniform sampler2D tex;
uniform vec3 laserColor;
uniform float time;

#include fresnel.glsl
#include fragViewDir.glsl
#include luminance.glsl

void main() {
	vec4 texture = texture(tex,
			vec2(pass_texCoords.x - 7. * time, pass_texCoords.y));
	vec3 fragViewDir = normalize(fragViewDir(pass_invMatrix));
	vec3 viewAngle = normalize(cross(fragViewDir, pass_laserDir));
	vec3 normalAngle = normalize(cross(pass_normal, pass_laserDir));
	float fresnel = dot(viewAngle, normalAngle);

	color.rgb = vec3(1. + 2. * texture.r + 0.5 * texture.g) * laserColor;
	color.a = texture.a;
	glow.x = color.a;
	glow.y = 1. + luminance(color.rgb * (1. + fresnel));
	glow.z = color.a;
	glow.w = 1.;
}
