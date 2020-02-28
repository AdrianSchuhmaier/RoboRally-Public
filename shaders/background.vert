#version 330 core

layout (location = 0) in vec2 position;
layout (location = 1) in vec2 texCoords;

layout (std140) uniform Matrices
{
	mat4 viewMatrix;
	mat4 projectionMatrix;
};

out vec2 pass_texCoords;
out mat4 pass_invMatrix;

void main() {
	gl_Position = vec4(position.x, position.y, -1.0, 1.0); 
    pass_texCoords = texCoords;
    pass_invMatrix = transpose(viewMatrix) * inverse(projectionMatrix);
}