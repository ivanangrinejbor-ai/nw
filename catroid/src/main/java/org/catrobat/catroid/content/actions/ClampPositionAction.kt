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

class ClampPositionAction : TemporalAction() {

    var scope: Scope? = null
    var minX: Formula? = null
    var maxX: Formula? = null
    var minY: Formula? = null
    var maxY: Formula? = null

    override fun update(percent: Float) {
        val s = scope ?: return
        val look = s.sprite?.look ?: return
        try {
            val mnX = minX?.interpretFloat(s) ?: -540f
            val mxX = maxX?.interpretFloat(s) ?: 540f
            val mnY = minY?.interpretFloat(s) ?: -960f
            val mxY = maxY?.interpretFloat(s) ?: 960f

            val lowerX = minOf(mnX, mxX)
            val upperX = maxOf(mnX, mxX)
            val lowerY = minOf(mnY, mxY)
            val upperY = maxOf(mnY, mxY)

            val curX = look.getXInUserInterfaceDimensionUnit()
            val curY = look.getYInUserInterfaceDimensionUnit()

            val clampedX = curX.coerceIn(lowerX, upperX)
            val clampedY = curY.coerceIn(lowerY, upperY)

            look.setXInUserInterfaceDimensionUnit(clampedX)
            look.setYInUserInterfaceDimensionUnit(clampedY)
        } catch (e: Exception) {
        }
    }
}
