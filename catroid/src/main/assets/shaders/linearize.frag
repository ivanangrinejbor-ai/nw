#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;

void main() {
    vec3 srgbColor = texture2D(u_texture0, v_texCoords).rgb;

    vec3 linearColor = pow(srgbColor, vec3(2.2));

    gl_FragColor = vec4(linearColor, 1.0);
}