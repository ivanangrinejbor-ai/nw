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

class CreateParticleSystemBrick : FormulaBrick {
    companion object {
        private const val serialVersionUID = 1L
    }

    constructor() {
        addAllowedBrickField(BrickField.PARTICLE_ID, R.id.brick_create_particle_system_edit_id)
        addAllowedBrickField(BrickField.PARTICLE_MAX_COUNT, R.id.brick_create_particle_system_edit_count)
        addAllowedBrickField(BrickField.PARTICLE_LIFETIME, R.id.brick_create_particle_system_edit_lifetime)
        addAllowedBrickField(BrickField.PARTICLE_SPEED, R.id.brick_create_particle_system_edit_speed)
    }

    constructor(id: String, maxCount: Double, lifetime: Double, speed: Double) {
        this()
        setFormulaWithBrickField(BrickField.PARTICLE_ID, Formula(id))
        setFormulaWithBrickField(BrickField.PARTICLE_MAX_COUNT, Formula(maxCount))
        setFormulaWithBrickField(BrickField.PARTICLE_LIFETIME, Formula(lifetime))
        setFormulaWithBrickField(BrickField.PARTICLE_SPEED, Formula(speed))
    }

    override fun getViewResource(): Int = R.layout.brick_create_particle_system

    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(sprite.actionFactory.createParticleSystemAction(
            sprite, sequence,
            getFormulaWithBrickField(BrickField.PARTICLE_ID),
            getFormulaWithBrickField(BrickField.PARTICLE_MAX_COUNT),
            getFormulaWithBrickField(BrickField.PARTICLE_LIFETIME),
            getFormulaWithBrickField(BrickField.PARTICLE_SPEED)
        ))
    }
}
