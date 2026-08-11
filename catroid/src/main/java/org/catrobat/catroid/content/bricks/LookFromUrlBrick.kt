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
package org.catrobat.catroid.content.bricks

import org.catrobat.catroid.R
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.content.bricks.Brick.BrickField
import org.catrobat.catroid.content.bricks.Brick.ResourcesSet
import org.catrobat.catroid.formulaeditor.Formula

class LookFromUrlBrick constructor() : FormulaBrick() {

    constructor(url: String, name: String) : this() {
        setFormulaWithBrickField(BrickField.URL, Formula(url))
        setFormulaWithBrickField(BrickField.NAME, Formula(name))
    }

    constructor(url: Formula, name: Formula) : this() {
        setFormulaWithBrickField(BrickField.URL, url)
        setFormulaWithBrickField(BrickField.NAME, name)
    }

    init {
        addAllowedBrickField(BrickField.URL, R.id.brick_look_from_url_edit)
        addAllowedBrickField(BrickField.NAME, R.id.brick_look_from_url_name_edit)
    }

    override fun getViewResource(): Int = R.layout.brick_look_from_url

    override fun addRequiredResources(requiredResourcesSet: ResourcesSet) {
        requiredResourcesSet.add(Brick.NETWORK_CONNECTION)
        super.addRequiredResources(requiredResourcesSet)
    }

    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(
            sprite.actionFactory.createLookFromUrlAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.URL),
                getFormulaWithBrickField(BrickField.NAME)
            )
        )
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}