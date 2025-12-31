#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture0;
varying vec2 v_texCoords;

uniform float u_threshold;
uniform float u_intensity;
uniform float u_dispersal;
uniform float u_distortion;

const int GHOSTS = 5;

vec3 tex(vec2 uv) {
    vec3 texColor = texture2D(u_texture0, uv).rgb;
    return max(vec3(0.0), texColor - u_threshold);
}

void main() {
    vec2 uv = v_texCoords;

    vec2 ghostVec = (vec2(0.5) - uv) * u_dispersal;

    vec3 result = vec3(0.0);

    for (int i = 0; i < GHOSTS; ++i) {
        vec2 offset = fract(uv + ghostVec * float(i));

        float weight = length(vec2(0.5) - offset) / length(vec2(0.5));
        weight = pow(1.0 - weight, 10.0);

        vec3 ghostColor;
        ghostColor.r = tex(offset + normalize(ghostVec) * u_distortion).r;
        ghostColor.g = tex(offset).g;
        ghostColor.b = tex(offset - normalize(ghostVec) * u_distortion).b;

        result += ghostColor * weight;
    }

    vec4 origColor = texture2D(u_texture0, uv);

    gl_FragColor = vec4(origColor.rgb + result * u_intensity, origColor.a);
}