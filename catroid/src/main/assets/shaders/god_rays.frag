#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform sampler2D u_depthTexture;

uniform vec2 u_sunPos;
uniform float u_farPlane;


uniform float u_exposure;
uniform float u_decay;
uniform float u_density;
uniform float u_weight;


float getDepth(vec2 uv) {
    vec2 data = texture2D(u_depthTexture, vec2(uv.x, uv.y)).rg;
    return (data.x + data.y / 255.0) * u_farPlane;
}

void main() {
    vec2 uv = v_texCoords;
    vec2 deltaTextCoord = vec2(uv - u_sunPos);


    const int SAMPLES = 32;
    deltaTextCoord *= 1.0 / float(SAMPLES) * u_density;

    vec3 color = texture2D(u_texture0, uv).rgb;
    float illuminationDecay = 1.0;


    for (int i = 0; i < SAMPLES; i++) {
        uv -= deltaTextCoord;


        vec3 sampleColor = texture2D(u_texture0, uv).rgb;



        float d = getDepth(uv);
        if (d < u_farPlane * 0.95) {
            sampleColor = vec3(0.0);
        }


        sampleColor *= illuminationDecay * u_weight;
        color += sampleColor;
        illuminationDecay *= u_decay;
    }

    gl_FragColor = vec4(color * u_exposure, 1.0);
}