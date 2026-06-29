package org.catrobat.catroid.content.bricks

import org.catrobat.catroid.R
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.content.bricks.Brick.BrickField

class TimerBrick : FormulaBrick() {
    companion object {
        @JvmField val serialVersionUID: Long = 1L
    }

    constructor() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_timer_edit_name)
        addAllowedBrickField(BrickField.DURATION_IN_SECONDS, R.id.brick_timer_edit_duration)
        addAllowedBrickField(BrickField.VARIABLE, R.id.brick_timer_edit_variable)
    }

    constructor(name: String, duration: Double, variableName: String) : this() {
        setFormulaWithBrickField(BrickField.NAME, Formula(name))
        setFormulaWithBrickField(BrickField.DURATION_IN_SECONDS, Formula(duration))
        setFormulaWithBrickField(BrickField.VARIABLE, Formula(variableName))
    }

    override fun getViewResource(): Int = R.layout.brick_timer

    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(sprite.actionFactory.createTimerAction(
            sprite, sequence,
            getFormulaWithBrickField(BrickField.NAME),
            getFormulaWithBrickField(BrickField.DURATION_IN_SECONDS),
            getFormulaWithBrickField(BrickField.VARIABLE)
        ))
    }
}
