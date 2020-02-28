#version 330 core

layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec3 tangent;
layout (location = 3) in vec2 texCoords;

out vec3 pass_position;
out vec3 pass_normal;
out vec2 pass_texCoords;
out mat4 pass_invMatrix;

uniform vec3 lightPos;
uniform mat4 modelMatrix;
uniform float time;

layout (std140) uniform Matrices
{
	mat4 viewMatrix;
	mat4 projectionMatrix;
};

#include rotationMatrix.glsl

void main() {
	float scaleFactor = 0.3 * sin(2. * time) + 1.;
	mat4 scale;
	scale[0] = vec4(scaleFactor, 0., 0., 0.);
	scale[1] = vec4(0., scaleFactor, 0., 0.);
	scale[2] = vec4(0., 0., scaleFactor, 0.);
	scale[3] = vec4(0., 0., 0., 1.);
	mat4 rotation = rotationMatrix(vec3(0., 1., 0.), 4. * time);

	mat4 transformation = modelMatrix * scale * rotation;
	pass_position = (transformation * vec4(position.xyz, 1.)).xyz;
	pass_texCoords = texCoords;
	pass_normal = normalize(transformation * vec4(normal, 0.0)).xyz;
	pass_invMatrix = transpose(viewMatrix) * inverse(projectionMatrix);
	gl_Position = projectionMatrix * viewMatrix * transformation * vec4(position.xyz, 1.);
}
