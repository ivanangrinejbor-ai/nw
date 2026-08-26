/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2026 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import java.util.Locale

class CrtScreenShaderAction : TemporalAction() {
    var scope: Scope? = null
    var intensityFormula: Formula? = null

    override fun update(percent: Float) {
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val threeDManager = stageListener.threeDManager ?: return

        val intensity = ((intensityFormula?.interpretFloat(scope) ?: 80f) / 100f).coerceIn(0f, 1f)

        val fragment = CRT_FRAGMENT.replace("__INTENSITY__", String.format(Locale.US, "%.4f", intensity))
        threeDManager.setCustomScreenShader(SCREEN_VERTEX, fragment)
    }

    companion object {
        const val SCREEN_VERTEX = """attribute vec4 a_position;
attribute vec2 a_texCoord0;
varying vec2 v_texCoords;

void main() {
    v_texCoords = a_texCoord0;
    gl_Position = a_position;
}"""

        const val CRT_FRAGMENT = """#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform float u_time;

const float DISTORTION = 0.22;
const float ZOOM = 1.12;
const float CHROMATIC = 0.007;
const float INTENSITY = __INTENSITY__;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 raw = texture2D(u_texture0, v_texCoords).rgb;

    vec2 uv = v_texCoords;
    vec2 cc = uv - vec2(0.5);
    float dist = dot(cc, cc);

    vec2 distortedUV = vec2(0.5) + cc * (1.0 + DISTORTION * dist) / ZOOM;

    vec3 crt;
    if (distortedUV.x < 0.0 || distortedUV.x > 1.0 || distortedUV.y < 0.0 || distortedUV.y > 1.0) {
        crt = vec3(0.0);
    } else {
        vec2 splitOffset = cc * CHROMATIC * dist * 3.0;
        float r = texture2D(u_texture0, distortedUV - splitOffset).r;
        float g = texture2D(u_texture0, distortedUV).g;
        float b = texture2D(u_texture0, distortedUV + splitOffset).b;
        crt = vec3(r, g, b);

        crt.r *= 1.02;
        crt.g *= 1.04;
        crt.b *= 0.93;

        float scanline = sin(distortedUV.y * 650.0 + u_time * 2.5) * 0.05;
        crt -= vec3(scanline);

        float grain = (hash(v_texCoords * vec2(u_time, u_time * 1.7)) - 0.5) * 0.08;
        crt += vec3(grain);

        float vignette = smoothstep(0.35, 0.75, dist);
        crt *= (1.0 - vignette * 0.55);
    }

    gl_FragColor = vec4(mix(raw, crt, INTENSITY), 1.0);
}"""
    }
}
