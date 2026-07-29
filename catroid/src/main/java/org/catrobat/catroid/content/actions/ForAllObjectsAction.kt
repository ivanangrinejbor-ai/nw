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
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException

/**
 * Applies a single visual/motion property to EVERY sprite in the current scene (incl. clones).
 * The value formula is interpreted ONCE using the running sprite's scope, then applied to all
 * objects — matching the "set size/color/… for all objects" request. Value-less variants
 * (SHOW/HIDE) ignore the formula.
 */
class ForAllObjectsAction : TemporalAction() {

    enum class Property { SIZE, TRANSPARENCY, BRIGHTNESS, COLOR, X, Y, DIRECTION, SHOW, HIDE }

    var scope: Scope? = null
    var property: Property = Property.SIZE
    var value: Formula? = null

    override fun update(percent: Float) {
        val project = ProjectManager.getInstance().currentProject ?: return

        val v: Float = try {
            value?.interpretFloat(scope) ?: 0f
        } catch (e: InterpretationException) {
            0f
        }

        // Snapshot to avoid ConcurrentModificationException if clones spawn/despawn mid-loop.
        for (sprite in ArrayList(project.spriteListWithClones)) {
            val look = sprite.look ?: continue
            when (property) {
                Property.SIZE -> look.setSizeInUserInterfaceDimensionUnit(v)
                Property.TRANSPARENCY -> look.setTransparencyInUserInterfaceDimensionUnit(v)
                Property.BRIGHTNESS -> look.setBrightnessInUserInterfaceDimensionUnit(v)
                Property.COLOR -> look.setColorInUserInterfaceDimensionUnit(v)
                Property.X -> look.setPositionInUserInterfaceDimensionUnit(v, look.yInUserInterfaceDimensionUnit)
                Property.Y -> look.setPositionInUserInterfaceDimensionUnit(look.xInUserInterfaceDimensionUnit, v)
                Property.DIRECTION -> look.setMotionDirectionInUserInterfaceDimensionUnit(v)
                Property.SHOW -> look.setLookVisible(true)
                Property.HIDE -> look.setLookVisible(false)
            }
        }
    }
}
