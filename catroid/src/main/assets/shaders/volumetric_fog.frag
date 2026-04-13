#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform sampler2D u_depthTexture;
uniform sampler2D u_shadowTexture;

uniform mat4 u_invProjViewMatrix;
uniform mat4 u_lightProjView;
uniform vec3 u_cameraPos;
uniform float u_farPlane;

uniform vec3 u_lightDir;
uniform vec3 u_lightColor;

uniform float u_density;
uniform float u_scattering;
uniform float u_maxDistance;
uniform int u_steps;

const float PI = 3.14159265359;

float getNoise(vec2 coord) {
return fract(52.9829189 * fract(dot(coord, vec2(0.06711056, 0.00583715))));
}

float getDepthNorm(vec2 uv) {
vec2 data = texture2D(u_depthTexture, uv).rg;
return data.x + data.y / 255.0;
}

vec3 getWorldPos(vec2 uv) {
float zNorm = getDepthNorm(uv);

vec4 ndc = vec4(uv * 2.0 - 1.0, 1.0, 1.0);
vec4 farRay = u_invProjViewMatrix * ndc;
vec3 worldFarPos = farRay.xyz / farRay.w;

return mix(u_cameraPos, worldFarPos, zNorm);
}

float getShadowDepth(vec2 uv) {
vec4 c = texture2D(u_shadowTexture, uv);
return dot(c, vec4(1.0, 1.0/255.0, 1.0/65025.0, 1.0/16581375.0));
}

float computeScattering(float lightDotView) {
    float g = u_scattering;
    float result = 1.0 - g * g;
    result /= (4.0 * PI * pow(1.0 + g * g - (2.0 * g * lightDotView), 1.5));
    return result;
}

void main() {
    vec4 baseColor = texture2D(u_texture0, v_texCoords);
    vec3 worldPos = getWorldPos(v_texCoords);

    vec3 startPos = u_cameraPos;
    vec3 rayDir = worldPos - startPos;
    float rayLength = length(rayDir);
    rayDir /= rayLength;

    float limit = min(rayLength, u_maxDistance);
    float stepSize = limit / float(u_steps);

    float noise = getNoise(gl_FragCoord.xy);
    vec3 currentPos = startPos + rayDir * (stepSize * noise);

    float fogAccum = 0.0;

    float lightDotView = dot(rayDir, -u_lightDir);
    float phase = computeScattering(lightDotView);

    for(int i = 0; i < 60; i++) {
        if(i >= u_steps) break;
        vec4 lightSpacePos = u_lightProjView * vec4(currentPos, 1.0);
        vec3 projCoords = lightSpacePos.xyz / lightSpacePos.w;
        projCoords = projCoords * 0.5 + 0.5;

        if(projCoords.x >= 0.0 && projCoords.x <= 1.0 &&
           projCoords.y >= 0.0 && projCoords.y <= 1.0 &&
           projCoords.z <= 1.0) {

            float shadowDepth = getShadowDepth(projCoords.xy);

            if(projCoords.z - 0.005 < shadowDepth) {
                fogAccum += 1.0;
            } else {
                fogAccum += 0.05;
            }
        } else {
            fogAccum += 1.0;
        }

        currentPos += rayDir * stepSize;
    }

    fogAccum = (fogAccum / float(u_steps)) * u_density * phase;

    vec3 fogColor = u_lightColor * fogAccum;
    vec3 finalColor = baseColor.rgb + fogColor - (baseColor.rgb * fogColor);

    gl_FragColor = vec4(finalColor, baseColor.a);
}