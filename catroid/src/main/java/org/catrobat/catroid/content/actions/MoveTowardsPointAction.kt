/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
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
import kotlin.math.sqrt

class MoveTowardsPointAction : TemporalAction() {

    var scope: Scope? = null
    var targetX: Formula? = null
    var targetY: Formula? = null
    var steps: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val sprite = s.sprite ?: return
        val look = sprite.look ?: return
        try {
            val tx = targetX?.interpretFloat(s) ?: 0f
            val ty = targetY?.interpretFloat(s) ?: 0f
            val stepVal = steps?.interpretFloat(s) ?: 0f
            val curX = look.getXInUserInterfaceDimensionUnit()
            val curY = look.getYInUserInterfaceDimensionUnit()
            val dx = tx - curX
            val dy = ty - curY
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dist <= stepVal || dist < 0.0001f) {
                look.setXInUserInterfaceDimensionUnit(tx)
                look.setYInUserInterfaceDimensionUnit(ty)
            } else {
                val ratio = stepVal / dist
                look.changePositionInInterfaceDimensionUnit(dx * ratio, dy * ratio)
            }
            sprite.movedByStepsBrick = true
        } catch (e: Exception) {
        }
    }
}
