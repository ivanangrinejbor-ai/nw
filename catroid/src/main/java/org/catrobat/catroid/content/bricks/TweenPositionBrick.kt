package org.catrobat.catroid.content.bricks

import org.catrobat.catroid.R
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.content.bricks.Brick.BrickField

class TweenPositionBrick : FormulaBrick() {
    companion object {
        @JvmField val serialVersionUID: Long = 1L
    }

    constructor() {
        addAllowedBrickField(BrickField.X_DESTINATION, R.id.brick_tween_position_edit_x)
        addAllowedBrickField(BrickField.Y_DESTINATION, R.id.brick_tween_position_edit_y)
        addAllowedBrickField(BrickField.DURATION_IN_SECONDS, R.id.brick_tween_position_edit_duration)
        addAllowedBrickField(BrickField.EASING_TYPE, R.id.brick_tween_position_edit_easing)
    }

    constructor(xDestination: Double, yDestination: Double, duration: Double, easingType: Double) : this() {
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
