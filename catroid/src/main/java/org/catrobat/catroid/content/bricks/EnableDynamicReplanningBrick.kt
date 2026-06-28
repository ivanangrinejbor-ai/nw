package org.catrobat.catroid.content.bricks

import org.catrobat.catroid.R
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.formulaeditor.Formula

class EnableDynamicReplanningBrick : FormulaBrick() {
    private static final long serialVersionUID = 1L

    constructor() {
        addAllowedBrickField(BrickField.SPRITE_NAME, R.id.brick_dynamic_replan_sprite_edit)
        addAllowedBrickField(BrickField.ENABLED, R.id.brick_dynamic_replan_enabled_edit)
    }

    constructor(spriteName: String, enabled: String) : this() {
        setFormulaWithBrickField(BrickField.SPRITE_NAME, Formula(spriteName))
        setFormulaWithBrickField(BrickField.ENABLED, Formula(enabled))
    }

    override fun getViewResource(): Int = R.layout.brick_dynamic_replanning

    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(sprite.actionFactory.createEnableDynamicReplanningAction(
            sprite, sequence, 
            getFormulaWithBrickField(BrickField.SPRITE_NAME),
            getFormulaWithBrickField(BrickField.ENABLED)))
    }
}
