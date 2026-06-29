package org.catrobat.catroid.content.bricks

import org.catrobat.catroid.R
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.formulaeditor.Formula

class EnableDynamicReplanningBrick() : FormulaBrick() {
    companion object { @JvmField val serialVersionUID: Long = 1L }

    init { addFields() }
    private fun addFields() {
        addAllowedBrickField(Brick.BrickField.SPRITE_NAME, R.id.brick_dynamic_replan_sprite_edit)
        addAllowedBrickField(Brick.BrickField.ENABLED, R.id.brick_dynamic_replan_enabled_edit)
    }

    constructor(spriteName: String, enabled: String) : this() {
        setFormulaWithBrickField(Brick.BrickField.SPRITE_NAME, Formula(spriteName))
        setFormulaWithBrickField(Brick.BrickField.ENABLED, Formula(enabled))
    }

    override fun getViewResource(): Int = R.layout.brick_dynamic_replanning
    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(sprite.actionFactory.createEnableDynamicReplanningAction(sprite, sequence,
            getFormulaWithBrickField(Brick.BrickField.SPRITE_NAME),
            getFormulaWithBrickField(Brick.BrickField.ENABLED)))
    }
}
