#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform float u_exposure;

void main() {
    vec3 hdrColor = texture2D(u_texture0, v_texCoords).rgb;
    hdrColor *= u_exposure;
    gl_FragColor = vec4(hdrColor, 1.0);
}