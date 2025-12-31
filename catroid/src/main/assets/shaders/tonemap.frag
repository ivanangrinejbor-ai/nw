#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;

vec3 ACESFilmic(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec3 linearColor = texture2D(u_texture0, v_texCoords).rgb;

    vec3 tonemappedColor = ACESFilmic(linearColor);

    vec3 srgbColor = pow(tonemappedColor, vec3(1.0/2.2));

    gl_FragColor = vec4(srgbColor, 1.0);
}