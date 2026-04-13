#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform sampler2D u_depthTexture;

uniform float u_farPlane;
uniform float u_aspectRatio;

uniform float u_focusDistance;
uniform float u_focusRange;
uniform float u_transition;
uniform float u_blurSize;

float getDepth(vec2 uv) {
vec2 data = texture2D(u_depthTexture, vec2(uv.x, uv.y)).rg;
return (data.x + data.y / 255.0) * u_farPlane;
}

float getCoC(float depth) {
    float dist = abs(depth - u_focusDistance);
    return clamp((dist - u_focusRange) / u_transition, 0.0, 1.0);
}

void main() {
    vec4 baseColor = texture2D(u_texture0, v_texCoords);
    float centerDepth = getDepth(v_texCoords);
    float centerCoC = getCoC(centerDepth);

    if (centerCoC < 0.01) {
        gl_FragColor = baseColor;
        return;
    }

    vec3 accColor = baseColor.rgb;
    float totalWeight = 1.0;

    const float GOLDEN_ANGLE = 2.39996;
    const float SAMPLES = 16.0;

    for (float i = 1.0; i <= SAMPLES; i++) {
        float r = sqrt(i / SAMPLES);
        float theta = i * GOLDEN_ANGLE;

        vec2 offset = vec2(cos(theta), sin(theta) * u_aspectRatio) * r * u_blurSize * centerCoC;
        vec2 sampleUV = v_texCoords + offset;

        if (sampleUV.x >= 0.0 && sampleUV.x <= 1.0 && sampleUV.y >= 0.0 && sampleUV.y <= 1.0) {
            float sampleDepth = getDepth(sampleUV);
            float sampleCoC = getCoC(sampleDepth);

            float weight = (sampleDepth < centerDepth) ? 1.0 : sampleCoC;

            accColor += texture2D(u_texture0, sampleUV).rgb * weight;
            totalWeight += weight;
        }
    }

    gl_FragColor = vec4(accColor / totalWeight, baseColor.a);
}