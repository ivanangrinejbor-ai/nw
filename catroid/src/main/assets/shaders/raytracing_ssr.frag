#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform sampler2D u_depthTexture;
uniform sampler2D u_materialTexture;

uniform mat4 u_projectionMatrix;
uniform mat4 u_invProjectionMatrix;
uniform mat4 u_viewMatrix;
uniform float u_farPlane;

uniform float u_reflectivityMulti;
uniform float u_edgeFade;
uniform float u_thickness;
uniform float u_maxDistance;
uniform float u_stride;
uniform int u_maxSteps;

float getNoise(vec2 coord) {
    return fract(52.9829189 * fract(dot(coord, vec2(0.06711056, 0.00583715))));
}

float getDepth(vec2 uv) {
    vec2 data = texture2D(u_depthTexture, uv).rg;
    return (data.x + data.y / 255.0) * u_farPlane;
}

vec3 getNormal(vec2 uv) {
    vec2 p = texture2D(u_depthTexture, uv).ba * 2.0 - 1.0;
    vec3 n = vec3(p.xy, 1.0 - abs(p.x) - abs(p.y));
    if (n.z < 0.0) n.xy = (1.0 - abs(n.yx)) * vec2(p.x >= 0.0 ? 1.0 : -1.0, p.y >= 0.0 ? 1.0 : -1.0);
    return normalize(mat3(u_viewMatrix) * n);
}

vec3 getViewPos(vec2 uv) {
    float z = getDepth(uv);
    vec4 ndc = vec4(uv * 2.0 - 1.0, 1.0, 1.0);
    vec4 farRay = u_invProjectionMatrix * ndc;
    return (farRay.xyz / farRay.w) * (z / u_farPlane);
}

void main() {
    float matReflectivity = texture2D(u_materialTexture, v_texCoords).r;

    if (matReflectivity <= 0.01) {
        gl_FragColor = texture2D(u_texture0, v_texCoords);
        return;
    }

    vec4 baseColor = texture2D(u_texture0, v_texCoords);
    float depth = getDepth(v_texCoords);

    if (depth >= u_farPlane * 0.98) {
        gl_FragColor = baseColor;
        return;
    }

    vec3 vPos = getViewPos(v_texCoords);
    vec3 vNorm = getNormal(v_texCoords);
    vec3 vDir = normalize(vPos);
    vec3 vRef = normalize(reflect(vDir, vNorm));

    if (vRef.z > 0.0) {
        gl_FragColor = baseColor;
        return;
    }

    float offset = getNoise(gl_FragCoord.xy);
    float currentStepSize = max(u_stride, 0.05);

    vec3 rayPos = vPos + vNorm * 0.05 + vRef * (currentStepSize * offset);

    vec3 hitColor = vec3(0.0);
    float mask = 0.0;

    for(int i = 0; i < 40; i++) {
        if (i >= u_maxSteps) break;

        rayPos += vRef * currentStepSize;

        vec4 proj = u_projectionMatrix * vec4(rayPos, 1.0);
        vec2 hitUV = (proj.xy / proj.w) * 0.5 + 0.5;

        if(hitUV.x < 0.0 || hitUV.x > 1.0 || hitUV.y < 0.0 || hitUV.y > 1.0) break;

        float sceneZ = getDepth(hitUV);
        float rayZ = -rayPos.z;

        if (rayZ > sceneZ) {
            float diff = rayZ - sceneZ;

            if (diff < max(u_thickness, currentStepSize)) {

                vec3 startPos = rayPos - vRef * currentStepSize;
                vec3 endPos = rayPos;
                vec3 midPos;

                for(int j = 0; j < 4; j++) {
                    midPos = mix(startPos, endPos, 0.5);
                    vec4 mProj = u_projectionMatrix * vec4(midPos, 1.0);
                    vec2 mUV = (mProj.xy / mProj.w) * 0.5 + 0.5;

                    if (-midPos.z > getDepth(mUV)) {
                        endPos = midPos;
                    } else {
                        startPos = midPos;
                    }
                }

                vec4 finalProj = u_projectionMatrix * vec4(midPos, 1.0);
                vec2 finalUV = (finalProj.xy / finalProj.w) * 0.5 + 0.5;

                hitColor = texture2D(u_texture0, finalUV).rgb;

                vec2 screenEdge = smoothstep(0.0, u_edgeFade, finalUV) * smoothstep(1.0, 1.0 - u_edgeFade, finalUV);
                mask = screenEdge.x * screenEdge.y;
                mask *= 1.0 - clamp(distance(vPos, midPos) / u_maxDistance, 0.0, 1.0);
                break;
            }
        }
        currentStepSize *= 1.05;
    }

    float fresnel = pow(1.0 - max(dot(vNorm, -vDir), 0.0), 3.0);
    float finalIntensity = mask * fresnel * matReflectivity * u_reflectivityMulti;

    gl_FragColor = vec4(mix(baseColor.rgb, hitColor, finalIntensity), baseColor.a);
}