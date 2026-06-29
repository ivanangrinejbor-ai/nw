package org.catrobat.catroid.content.bricks

import org.catrobat.catroid.R
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.formulaeditor.Formula

class SmoothPathBrick() : FormulaBrick() {
    companion object { @JvmField val serialVersionUID: Long = 1L }

    init { addFields() }
    private fun addFields() {
        addAllowedBrickField(Brick.BrickField.SPRITE_NAME, R.id.brick_smooth_path_sprite_edit)
    }

    constructor(spriteName: String) : this() {
        setFormulaWithBrickField(Brick.BrickField.SPRITE_NAME, Formula(spriteName))
    }

    override fun getViewResource(): Int = R.layout.brick_smooth_path
    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(sprite.actionFactory.createSmoothPathAction(sprite, sequence,
            getFormulaWithBrickField(Brick.BrickField.SPRITE_NAME)))
    }
}
