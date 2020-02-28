#version 330 core

in vec2 pass_texCoords;

uniform sampler2D image;
uniform float strength;

void main() {

	vec2 offset[9];
	offset[0] = vec2(-1., -1.);
	offset[1] = vec2(0., -1.);
	offset[2] = vec2(1., -1.);
	offset[3] = vec2(-1., 0.);
	offset[4] = vec2(0., 0.);
	offset[5] = vec2(1., 0.);
	offset[6] = vec2(-1., 1.);
	offset[7] = vec2(0., 1.);
	offset[8] = vec2(1., 1.);

	vec4 sample[9];

	for (int i = 0; i < 9; i++) {
		sample[i] = texture(image,
				pass_texCoords + offset[i] * (1. / textureSize(image, 0)));
	}

	gl_FragColor = 9 * sample[4];

	for (int i = 0; i < 9; i++) {
		if (i != 4)
			gl_FragColor -= sample[i];
	}

	gl_FragColor.rgb = mix(texture(image, pass_texCoords).rgb, gl_FragColor.rgb,
			strength);
	gl_FragColor.a = 1.;
}

