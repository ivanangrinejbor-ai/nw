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

class WaveShaderAction : TemporalAction() {
    var scope: Scope? = null
    var objectIdFormula: Formula? = null
    var amplitudeFormula: Formula? = null

    override fun update(percent: Float) {
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val threeDManager = stageListener.threeDManager ?: return

        val objId = objectIdFormula?.interpretString(scope) ?: return
        if (objId.isEmpty()) return

        val amplitude = ((amplitudeFormula?.interpretFloat(scope) ?: 30f) / 100f)
            .coerceIn(0f, 1f) * MAX_AMPLITUDE

        threeDManager.setObjectCustomShader(objId, OBJECT_VERTEX, WAVE_FRAGMENT)
        threeDManager.setObjectShaderUniform(objId, "amp", amplitude, 0f, 0f, 1)
    }

    companion object {
        const val MAX_AMPLITUDE = 0.15f

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

        const val WAVE_FRAGMENT = """#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoord;

uniform sampler2D u_diffuseTexture;
uniform float u_time;
uniform float u_amp;

void main() {
    vec2 uv = v_texCoord;
    uv.x += sin(uv.y * 18.0 + u_time * 3.0) * u_amp;
    uv.y += cos(uv.x * 14.0 + u_time * 2.2) * u_amp * 0.6;
    vec4 tex = texture2D(u_diffuseTexture, clamp(uv, 0.0, 1.0));
    gl_FragColor = tex;
}"""
    }
}
