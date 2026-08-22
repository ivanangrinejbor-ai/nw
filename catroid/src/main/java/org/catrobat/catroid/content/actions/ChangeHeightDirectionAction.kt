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

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import kotlin.math.cos
import kotlin.math.sin

class ChangeHeightDirectionAction : TemporalAction() {
    companion object {
        const val DIRECTION_UP = 0
        const val DIRECTION_DOWN = 1
        const val DIRECTION_CENTER = 2
    }

    var scope: Scope? = null
    var size: Formula? = null
    var direction: Int = DIRECTION_UP

    override fun update(percent: Float) {
        val currentScope = scope ?: return
        try {
            val deltaPercent = size?.interpretFloat(currentScope) ?: 0f
            val look = currentScope.sprite.look ?: return
            val deltaScale = deltaPercent / 100f
            val oldScaleY = look.scaleY
            val newScaleY = oldScaleY + deltaScale

            look.setHeightV(newScaleY)

            if (direction != DIRECTION_CENTER && look.height > 0) {
                val localDy = when (direction) {
                    DIRECTION_UP -> (look.height * deltaScale) / 2f
                    DIRECTION_DOWN -> -(look.height * deltaScale) / 2f
                    else -> 0f
                }
                val rotRad = Math.toRadians(look.rotation.toDouble())
                val worldDx = (-localDy * sin(rotRad)).toFloat()
                val worldDy = (localDy * cos(rotRad)).toFloat()

                look.setXInUserInterfaceDimensionUnit(look.xInUserInterfaceDimensionUnit + worldDx)
                look.setYInUserInterfaceDimensionUnit(look.yInUserInterfaceDimensionUnit + worldDy)
            }
        } catch (e: InterpretationException) {
            Log.d(javaClass.simpleName, "Formula interpretation failed.", e)
        }
    }
}
