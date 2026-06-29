/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class TweenPositionAction : TemporalAction() {
    var scope: Scope? = null
    var xDestination: Formula? = null
    var yDestination: Formula? = null
    var duration: Formula? = null
    var easingType: Formula? = null

    private var startX = 0f
    private var startY = 0f
    private var targetX = 0f
    private var targetY = 0f

    override fun begin() {
        val sprite = scope?.sprite ?: return
        val look = sprite.look ?: return

        startX = look.xInUserInterfaceDimensionUnit
        startY = look.yInUserInterfaceDimensionUnit
        targetX = xDestination?.interpretFloat(scope) ?: startX
        targetY = yDestination?.interpretFloat(scope) ?: startY

        val durationSeconds = duration?.interpretFloat(scope) ?: 1f
        setDuration(durationSeconds)

        val easing = easingType?.interpretInteger(scope)?.toInt() ?: 0
        interpolation = getInterpolation(easing)
    }

    override fun update(percent: Float) {
        val sprite = scope?.sprite ?: return
        val look = sprite.look ?: return

        val newX = startX + (targetX - startX) * percent
        val newY = startY + (targetY - startY) * percent

        look.setPositionInUserInterfaceDimensionUnit(newX, newY)
    }

    private fun getInterpolation(type: Int): com.badlogic.gdx.math.Interpolation {
        return when (type) {
            0 -> com.badlogic.gdx.math.Interpolation.linear
            1 -> com.badlogic.gdx.math.Interpolation.sine
            2 -> com.badlogic.gdx.math.Interpolation.sineIn
            3 -> com.badlogic.gdx.math.Interpolation.sineOut
            4 -> com.badlogic.gdx.math.Interpolation.pow2
            5 -> com.badlogic.gdx.math.Interpolation.pow2In
            6 -> com.badlogic.gdx.math.Interpolation.pow2Out
            7 -> com.badlogic.gdx.math.Interpolation.pow3
            8 -> com.badlogic.gdx.math.Interpolation.pow3In
            9 -> com.badlogic.gdx.math.Interpolation.pow3Out
            10 -> com.badlogic.gdx.math.Interpolation.pow4
            11 -> com.badlogic.gdx.math.Interpolation.pow4In
            12 -> com.badlogic.gdx.math.Interpolation.pow4Out
            13 -> com.badlogic.gdx.math.Interpolation.pow5
            14 -> com.badlogic.gdx.math.Interpolation.pow5In
            15 -> com.badlogic.gdx.math.Interpolation.pow5Out
            16 -> com.badlogic.gdx.math.Interpolation.bounce
            17 -> com.badlogic.gdx.math.Interpolation.bounceIn
            18 -> com.badlogic.gdx.math.Interpolation.bounceOut
            19 -> com.badlogic.gdx.math.Interpolation.elastic
            20 -> com.badlogic.gdx.math.Interpolation.elasticIn
            21 -> com.badlogic.gdx.math.Interpolation.elasticOut
            22 -> com.badlogic.gdx.math.Interpolation.swing
            23 -> com.badlogic.gdx.math.Interpolation.swingIn
            24 -> com.badlogic.gdx.math.Interpolation.swingOut
            else -> com.badlogic.gdx.math.Interpolation.linear
        }
    }
}
