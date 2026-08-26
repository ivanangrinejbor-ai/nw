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

class DissolveShaderAction : TemporalAction() {
    var scope: Scope? = null
    var objectIdFormula: Formula? = null
    var progressFormula: Formula? = null

    override fun update(percent: Float) {
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val threeDManager = stageListener.threeDManager ?: return

        val objId = objectIdFormula?.interpretString(scope) ?: return
        if (objId.isEmpty()) return

        val progress = ((progressFormula?.interpretFloat(scope) ?: 0f) / 100f).coerceIn(0f, 1f)

        threeDManager.setObjectCustomShader(objId, OBJECT_VERTEX, DISSOLVE_FRAGMENT)
        threeDManager.setObjectShaderUniform(objId, "progress", progress, 0f, 0f, 1)
    }

    companion object {
        const val OBJECT_VERTEX = """attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;

varying vec2 v_texCoord;

void main() {
    v_texCoord = a_texCoord0;
    gl_Position = u_projViewTrans * u_worldTrans * vec4(a_position, 1.0);
}"""

        const val DISSOLVE_FRAGMENT = """#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoord;

uniform sampler2D u_diffuseTexture;
uniform float u_time;
uniform float u_progress;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

void main() {
    vec4 tex = texture2D(u_diffuseTexture, v_texCoord);
    float n = noise(v_texCoord * 9.0) * 0.85 + noise(v_texCoord * 27.0) * 0.15;
    float t = clamp(u_progress, 0.0, 1.0);
    if (n < t) discard;
    float edge = smoothstep(t, t + 0.15, n);
    vec3 burnColor = mix(vec3(1.0, 0.9, 0.3), vec3(0.85, 0.12, 0.02), smoothstep(t + 0.02, t + 0.15, n));
    vec3 col = mix(burnColor, tex.rgb, edge);
    gl_FragColor = vec4(col, tex.a);
}"""
    }
}
