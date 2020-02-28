vec3 fresnel(float HdotV, vec3 bias) {
	return bias + (1.0 - bias) * pow(1.0 - HdotV, 5.0);
}

vec3 fresnelNB(float HdotV) {
	return fresnel(HdotV, vec3(0.));
}

vec3 fresnel(vec3 viewDir, vec3 norm, vec3 bias) {
	vec3 halfVec = normalize(viewDir + viewDir);
	norm = normalize(norm);
	float HdotV = max(dot(halfVec, norm), 0.0);
	return fresnel(HdotV, bias);
}

vec3 fresnelNB(vec3 viewDir, vec3 norm) {
	return fresnel(viewDir, norm, vec3(0.));
}

float fresnelFactor(float HdotV, float bias) {
	return bias + (1.0 - bias) * pow(1.0 - HdotV, 5.0);
}

float fresnelFactor(vec3 viewDir, vec3 norm, float bias) {
	vec3 halfVec = normalize(viewDir + norm);
	norm = normalize(norm);
	float HdotV = max(dot(halfVec, viewDir), 0.0);
	return fresnelFactor(HdotV, bias);
}

float fresnelFactorNB(vec3 viewDir, vec3 norm) {
	return fresnelFactor(viewDir, norm, 0.0);
}
