#version 330 core

in vec2 pass_texCoords;

layout (std140) uniform Dimensions
{
	vec2 dimensions;
};

uniform sampler2D scene;
uniform sampler2D reflection;
uniform sampler2D effect;
uniform sampler2D glow;

uniform vec2 inverseTextureSize;
uniform bool fxaa;
uniform bool reflect;
uniform float reflectiveness;

#include fxaa.glsl
	
void main() {
	vec4 sceneColor = fxaa ? FXAA(scene, pass_texCoords) : texture(scene, pass_texCoords);
	vec4 reflectionColor = fxaa ? FXAA(reflection, vec2( - pass_texCoords.x, pass_texCoords.y)) : FXAA(reflection, vec2( - pass_texCoords.x, pass_texCoords.y));
	vec4 effectColor = texture(effect, pass_texCoords);
	vec4 glow = texture(glow, pass_texCoords);
	float fresnel = glow.x;
	float effectFac = glow.y;
	float mask = glow.z;

	if (mask == 0. || reflect == false || reflectiveness == 0.) {
		gl_FragColor = sceneColor;
	} else {
	   fresnel = fresnel;
	   float reflectionBias = reflectiveness;
	   gl_FragColor = (1.0 - 0.75 * reflectionBias) * fresnel * sceneColor + reflectionBias * (1. - fresnel) * reflectionColor;
	}
	
//	gl_FragColor.rgb = reflectionColor.aaa;
	gl_FragColor.a = 1.;
	gl_FragColor.rgb = mix(gl_FragColor.rgb, effectColor.rgb, effectColor.a * effectColor.a * effectColor.a);

}
