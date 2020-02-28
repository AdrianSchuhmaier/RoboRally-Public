#version 330 core

layout (location = 0) in vec3 position;

out mat4 invMatrix;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

void main() {
	gl_Position = vec4(position.x, position.y, position.z, 1.0f);
    invMatrix = transpose(viewMatrix) * inverse(projectionMatrix);
}
