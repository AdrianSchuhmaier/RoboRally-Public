#version 330 core

layout ( lines ) in;
layout ( triangle_strip, max_vertices = 16) out;

in mat4 invMatrix[];

out mat4 pass_invMatrix;
out vec3 pass_laserDir;
out vec3 pass_normal;
out vec2 pass_texCoords;
out float pass_length;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

vec3 start;
vec3 end;
vec3 up;
vec3 left;

const float size = 0.1;

void createVertex(vec3 position) {
	gl_Position = projectionMatrix * viewMatrix * vec4(position, 1.);
	EmitVertex();
}

void createQuad(vec3 offset, float distance) {
	pass_texCoords = vec2(0., 1.);
	createVertex(start - size * offset);
	pass_texCoords = vec2(0., 0.);
	createVertex(start + size * offset);
	pass_texCoords = vec2(distance, 1.);
	createVertex(end - size * offset);
	pass_texCoords = vec2(distance, 0.);
	createVertex(end + size * offset);
	EndPrimitive();
}

void main() {
	pass_invMatrix = invMatrix[0];

	start = gl_in[0].gl_Position.xyz;
	end = gl_in[1].gl_Position.xyz;
	vec3 diff = end - start;
	float distance = length(diff);
	up = vec3(0., 1., 0.);
	diff = normalize(diff);
	left = cross(diff, up);

	pass_laserDir = diff;
	pass_length = distance;

	createQuad(left, distance);
	createQuad(up, distance);
	createQuad(normalize(left + up), distance);
	createQuad(normalize(left - up), distance);

}
