#version 330 core

in vec2 pass_texCoords;

uniform sampler2D screenTexture;

void main()
{ 
    gl_FragColor = texture(screenTexture, pass_texCoords);
}