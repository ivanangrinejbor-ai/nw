#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform sampler2D u_depthTexture;

uniform mat4 u_invProjection;
uniform mat4 u_invView;
uniform vec3 u_cameraPos;
uniform float u_farPlane;

uniform vec3 u_fogColor;
uniform float u_fogDensity;
uniform float u_heightFalloff;
uniform float u_fogHeight;

float getDepth(vec2 uv) {
vec2 data = texture2D(u_depthTexture, uv).rg;
return (data.x + data.y / 255.0) * u_farPlane;
}

void main() {
    vec4 baseColor = texture2D(u_texture0, v_texCoords);
    float linearZ = getDepth(v_texCoords);

    if (linearZ >= u_farPlane * 0.98) {
        gl_FragColor = baseColor;
        return;
    }

    vec4 ndc = vec4(v_texCoords * 2.0 - 1.0, 1.0, 1.0);
    vec4 viewRay = u_invProjection * ndc;
    vec3 vPos = (viewRay.xyz / abs(viewRay.z)) * linearZ;

    vec3 worldPos = (u_invView * vec4(vPos, 1.0)).xyz;

    float dist = distance(u_cameraPos, worldPos);
    vec3 rayDir = normalize(worldPos - u_cameraPos);

    float camH = u_cameraPos.y - u_fogHeight;
    float objH = worldPos.y - u_fogHeight;

    float fogAmount = (u_fogDensity / u_heightFalloff) * exp(-camH * u_heightFalloff) *
                      (1.0 - exp(-dist * rayDir.y * u_heightFalloff)) / rayDir.y;
    if (abs(rayDir.y) < 0.001) {
        fogAmount = u_fogDensity * dist * exp(-camH * u_heightFalloff);
    }

    float fogFactor = 1.0 - exp(-max(fogAmount, 0.0));
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    gl_FragColor = vec4(mix(baseColor.rgb, u_fogColor, fogFactor), baseColor.a);
}