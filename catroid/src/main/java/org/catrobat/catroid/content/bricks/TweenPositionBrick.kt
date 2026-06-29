package org.catrobat.catroid.content.bricks

import org.catrobat.catroid.R
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.formulaeditor.Formula

class TweenPositionBrick() : FormulaBrick() {
    companion object { @JvmField val serialVersionUID: Long = 1L }

    init { addFields() }
    private fun addFields() {
        addAllowedBrickField(Brick.BrickField.X_DESTINATION, R.id.brick_tween_position_edit_x)
        addAllowedBrickField(Brick.BrickField.Y_DESTINATION, R.id.brick_tween_position_edit_y)
        addAllowedBrickField(Brick.BrickField.DURATION_IN_SECONDS, R.id.brick_tween_position_edit_duration)
        addAllowedBrickField(Brick.BrickField.EASING_TYPE, R.id.brick_tween_position_edit_easing)
    }

    constructor(xDestination: Double, yDestination: Double, duration: Double, easingType: Double) : this() {
        setFormulaWithBrickField(Brick.BrickField.X_DESTINATION, Formula(xDestination))
        setFormulaWithBrickField(Brick.BrickField.Y_DESTINATION, Formula(yDestination))
        setFormulaWithBrickField(Brick.BrickField.DURATION_IN_SECONDS, Formula(duration))
        setFormulaWithBrickField(Brick.BrickField.EASING_TYPE, Formula(easingType))
    }

    override fun getViewResource(): Int = R.layout.brick_tween_position
    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(sprite.actionFactory.createTweenPositionAction(sprite, sequence,
            getFormulaWithBrickField(Brick.BrickField.X_DESTINATION),
            getFormulaWithBrickField(Brick.BrickField.Y_DESTINATION),
            getFormulaWithBrickField(Brick.BrickField.DURATION_IN_SECONDS),
            getFormulaWithBrickField(Brick.BrickField.EASING_TYPE)))
    }
}
