#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;

uniform sampler2D u_sourceTexture;
uniform sampler2D u_depthTexture;

uniform mat4 u_inverseProjectionView;

uniform int u_fogEnabled;
uniform int u_fogType;
uniform vec3 u_fogColor;
uniform float u_fogDensity;
uniform float u_fogStartDistance;
uniform float u_fogEndDistance;
uniform float u_fogHeightFalloff;
uniform vec3 u_cameraPosition;

vec3 getWorldPos(float depth, vec2 texCoords) {
    vec4 pos = vec4(texCoords * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    pos = u_inverseProjectionView * pos;
    return pos.xyz / pos.w;
}

float calculateFogFactor(vec3 worldPos, vec3 cameraPos) {
    float fogCoord = length(worldPos - cameraPos);
    float distanceFactor = 0.0;

    if (u_fogType == 1) { // LINEAR
        distanceFactor = smoothstep(u_fogStartDistance, u_fogEndDistance, fogCoord);
    } else if (u_fogType == 2) { // EXPONENTIAL
        distanceFactor = 1.0 - exp(-fogCoord * u_fogDensity);
    } else if (u_fogType == 3) { // EXPONENTIAL SQUARED
        float term = fogCoord * u_fogDensity;
        distanceFactor = 1.0 - exp(-(term * term));
    }

    float heightFactor = exp(-worldPos.y * u_fogHeightFalloff);
    return 1.0 - ((1.0 - distanceFactor) * heightFactor);
}

void main() {
    vec4 color = texture2D(u_sourceTexture, v_texCoords);
    float depth = texture2D(u_depthTexture, v_texCoords).r;

    if (u_fogEnabled == 1) {
        vec3 worldPos = getWorldPos(depth, v_texCoords);
        float fogFactor = calculateFogFactor(worldPos, u_cameraPosition);
        color.rgb = mix(color.rgb, u_fogColor, fogFactor);
    }

    gl_FragColor = color;
}