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

package org.catrobat.catroid.content.bricks

import org.catrobat.catroid.R
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.formulaeditor.Formula

class TweenPositionBrick : FormulaBrick {
    companion object {
        private const val serialVersionUID = 1L
    }

    constructor() {
        addAllowedBrickField(BrickField.X_DESTINATION, R.id.brick_tween_position_edit_x)
        addAllowedBrickField(BrickField.Y_DESTINATION, R.id.brick_tween_position_edit_y)
        addAllowedBrickField(BrickField.DURATION_IN_SECONDS, R.id.brick_tween_position_edit_duration)
        addAllowedBrickField(BrickField.EASING_TYPE, R.id.brick_tween_position_edit_easing)
    }

    constructor(xDestination: Double, yDestination: Double, duration: Double, easingType: Double) {
        this()
        setFormulaWithBrickField(BrickField.X_DESTINATION, Formula(xDestination))
        setFormulaWithBrickField(BrickField.Y_DESTINATION, Formula(yDestination))
        setFormulaWithBrickField(BrickField.DURATION_IN_SECONDS, Formula(duration))
        setFormulaWithBrickField(BrickField.EASING_TYPE, Formula(easingType))
    }

    override fun getViewResource(): Int = R.layout.brick_tween_position

    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(sprite.actionFactory.createTweenPositionAction(
            sprite, sequence,
            getFormulaWithBrickField(BrickField.X_DESTINATION),
            getFormulaWithBrickField(BrickField.Y_DESTINATION),
            getFormulaWithBrickField(BrickField.DURATION_IN_SECONDS),
            getFormulaWithBrickField(BrickField.EASING_TYPE)
        ))
    }
}
