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

layout (std140) uniform Matrices
{
	mat4 viewMatrix;
	mat4 projectionMatrix;
};

void main() {
	pass_position = (modelMatrix * vec4(position.xyz, 1.)).xyz;
	pass_texCoords = texCoords;
	pass_normal = normalize(modelMatrix * vec4(normal, 0.0)).xyz;
    pass_invMatrix = transpose(viewMatrix) * inverse(projectionMatrix);
	gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position.xyz, 1.);
}
