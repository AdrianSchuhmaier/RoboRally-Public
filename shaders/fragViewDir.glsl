vec3 fragViewDir(mat4 invMatrix) {
	vec2 uv = 2. * vec2(gl_FragCoord.x / dimensions.x, gl_FragCoord.y / dimensions.y) - vec2(1.);
	return (invMatrix * vec4(uv, -1., 1.)).xyz;
}
