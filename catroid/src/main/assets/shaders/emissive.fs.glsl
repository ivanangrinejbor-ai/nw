#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_emissiveTexture;
uniform vec4 u_emissiveColorFactor;
uniform float u_emissiveIntensity;

#ifdef textureFlag
varying vec2 v_texCoord0;
#endif

void main() {
    vec4 emissiveColor = u_emissiveColorFactor;
#ifdef textureFlag
    emissiveColor *= texture2D(u_emissiveTexture, v_texCoord0);
#endif

    if (u_emissiveIntensity > 0.0) {
        gl_FragColor = vec4(emissiveColor.rgb * u_emissiveIntensity, 1.0);
    } else {
        discard;
    }
}