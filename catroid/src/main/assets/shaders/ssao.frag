#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform sampler2D u_depthTexture;
uniform sampler2D u_noiseTexture;

uniform mat4 u_projectionMatrix;
uniform mat4 u_invProjectionMatrix;
uniform mat4 u_viewMatrix;
uniform float u_farPlane;

uniform vec3 u_kernel[16];
uniform vec2 u_noiseScale;
uniform float u_radius;
uniform float u_intensity;
uniform float u_bias;


float getDepth(vec2 uv) {
vec2 data = texture2D(u_depthTexture, uv).rg;
return (data.x + data.y / 255.0) * u_farPlane;
}


vec3 getNormal(vec2 uv) {
vec2 p = texture2D(u_depthTexture, uv).ba * 2.0 - 1.0;
vec3 v = vec3(p.xy, 1.0 - abs(p.x) - abs(p.y));
if (v.z < 0.0) v.xy = (1.0 - abs(v.yx)) * vec2(p.x >= 0.0 ? 1.0 : -1.0, p.y >= 0.0 ? 1.0 : -1.0);
return normalize(mat3(u_viewMatrix) * v);
}


vec3 getViewPos(vec2 uv) {
float z = getDepth(uv);
vec4 ndc = vec4(uv * 2.0 - 1.0, 1.0, 1.0);
vec4 farRay = u_invProjectionMatrix * ndc;
return (farRay.xyz / farRay.w) * (z / u_farPlane);
}

void main() {
    vec4 baseColor = texture2D(u_texture0, v_texCoords);
    float originDepth = getDepth(v_texCoords);


    if (originDepth >= u_farPlane * 0.95) {
        gl_FragColor = baseColor;
        return;
    }

    vec3 vPos = getViewPos(v_texCoords);
    vec3 vNorm = getNormal(v_texCoords);


    vec3 randomVec = normalize(texture2D(u_noiseTexture, v_texCoords * u_noiseScale).xyz * 2.0 - 1.0);
    vec3 tangent = normalize(randomVec - vNorm * dot(randomVec, vNorm));
    vec3 bitangent = cross(vNorm, tangent);
    mat3 TBN = mat3(tangent, bitangent, vNorm);

    float occlusion = 0.0;
    for (int i = 0; i < 16; i++) {

        vec3 samplePos = vPos + TBN * u_kernel[i] * u_radius;


        vec4 offset = u_projectionMatrix * vec4(samplePos, 1.0);
        vec2 sampleUV = (offset.xy / offset.w) * 0.5 + 0.5;

        float sampleDepth = getDepth(sampleUV);



        float rangeCheck = smoothstep(0.0, 1.0, u_radius / abs(originDepth - sampleDepth));
        if (sampleDepth <= -samplePos.z - u_bias) {
            occlusion += 1.0 * rangeCheck;
        }
    }

    occlusion = 1.0 - (occlusion / 16.0);
    occlusion = clamp(pow(occlusion, u_intensity), 0.0, 1.0);

    gl_FragColor = vec4(baseColor.rgb * occlusion, baseColor.a);
}