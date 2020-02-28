#version 330 core

in vec3 pass_position;
in vec3 pass_normal;
in vec2 pass_texCoords;

layout (location = 0) out vec4 color;
layout (location = 1) out vec4 glow;

uniform bool isReflection;

void main() {
	if (isReflection) {
		discard;
	}
	color.rgb = vec3(0.7 * sqrt(abs(pass_position.y)) + 0.3);
	color.a = 1.;
	glow.x = 1.;
	glow.y = 0.;
	glow.z = 0.;
	glow.w = 1.;
}
