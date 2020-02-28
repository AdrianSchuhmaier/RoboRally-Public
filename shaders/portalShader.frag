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
uniform sampler2D depth;
uniform float time;

const float totalAnimationTime = 1.;
const float size = 0.12;

#include fragViewDir.glsl

vec2 ParallaxMapping(vec2 texCoords, vec3 viewDir) {
	float height = texture(depth, texCoords).r;
	vec2 p = viewDir.xz / viewDir.y * (size * height);
	return texCoords - p;
}

void main() {
	vec3 fragViewDir = normalize(fragViewDir(pass_invMatrix));
	vec2 texCoords = ParallaxMapping(pass_texCoords, fragViewDir);
	texCoords = texCoords - floor(texCoords);
	// animation texCoords
	int frame = int(mod(time, totalAnimationTime) / totalAnimationTime * 16);
	texCoords = texCoords / 4.;
	texCoords.x += (frame / 4.);
	texCoords.y += (frame / 4) / 4.;

	color = texture(tex, texCoords);
	//color.a = 1.;
	glow.x = color.a;
	glow.y = 1.;
	glow.z = 0.;
	glow.w = 1.;
}
